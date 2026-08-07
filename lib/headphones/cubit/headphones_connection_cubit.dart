import 'dart:async';
import 'dart:isolate';
import 'dart:ui';

import 'package:app_settings/app_settings.dart';
import 'package:collection/collection.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:stream_channel/stream_channel.dart';
import 'package:the_last_bluetooth/the_last_bluetooth.dart';

import '../../logger.dart';
import 'headphones_cubit_objects.dart';
import 'model_matching.dart';

class HeadphonesConnectionCubit extends Cubit<HeadphonesConnectionState> {
  final TheLastBluetooth _bluetooth;
  StreamChannel<Uint8List>? _connection;
  BluetoothDevice? _activeDevice;
  bool _isConnecting = false;
  bool _isClosing = false;
  Timer? _pendingConnect;
  static const _bluetoothProfileSettleDelay = Duration(milliseconds: 1500);
  StreamSubscription? _btEnabledStream;
  StreamSubscription? _devStream;
  final Map<BluetoothDevice, StreamSubscription> _watchedKnownDevices = {};
  static const connectTries = 3;
  static const killOtherCubitTimeout = Duration(seconds: 3);
  static const _killUrself = "kill urself";

  /// indicating all "killing other cubits" is done and we can call _init()
  bool _warCrimesFinished = false;

  // I needed a way to tell (from background task) if app is currently running.
  // First idea was to then ask the cubit for some info (about battery etc)
  // But, looking at how fucked up this Port communication is, I will just
  // register/deregister this port name, and if background detects it, it just
  // skips 🤷
  //
  // UDPATE: LOOKS LIKE I CAN'T JUST REGEISTER, BECAUSE BLOC SUCKS ASS
  // I WILL HAVE FULL BLOWN PING HERE NOW
  // I could just implement whole fucking http server altoghether -_-
  // ...
  // Actaully............
  // I would probably need something like this for Linux Desktop anyway...
  // This might not be such a bad idea........
  // ...
  // Stop.
  static const pingReceivePortName = 'pingHeadphonesCubitPort';
  final _pingReceivePort = ReceivePort('dummyHeadphonesCubitPort');
  StreamSubscription? _pingReceivePortSS;

  /// returns true if no port, and false if timeout
  static Future<bool> _checkUntilNoPort(Duration timeout) async {
    // do this one time already bc stream will start only after first period
    noPort() =>
        IsolateNameServer.lookupPortByName(
            HeadphonesConnectionCubit.pingReceivePortName) ==
        null;
    if (noPort()) return true;
    return await Stream.periodic(
      Duration(milliseconds: 50),
      (_) => noPort(),
    ).firstWhere((e) => e).timeout(timeout, onTimeout: () => false);
  }

  // This is so fucking embarrassing......
  // Race conditions??? FUCK YES
  /// Reason this exists vs just checking port is to make sure cubit is
  /// **running**, and not just... exising hang up somewhere, or forgot
  /// to de-register the port
  static Future<bool> cubitAlreadyRunningSomewhere(
      {Duration responseTimeout = const Duration(seconds: 1)}) async {
    final ping = IsolateNameServer.lookupPortByName(
        HeadphonesConnectionCubit.pingReceivePortName);
    if (ping == null) return false;
    final pong = ReceivePort(); // this is not right naming, i know
    try {
      ping.send(pong.sendPort);
      return await pong.first.timeout(responseTimeout, onTimeout: () => false)
          as bool;
    } finally {
      pong.close();
    }
  }

  /// returns true if cubit was killed or didn't exist, and false if it didn't
  /// kill itself when asked
  //(that is, port is still present - it **may** happen that it just forgot
  // to close it, but we have no way to know)
  static Future<bool> killOtherCubit() async {
    final ping = IsolateNameServer.lookupPortByName(
        HeadphonesConnectionCubit.pingReceivePortName);
    if (ping == null) {
      loggI.e("No cubit to kill :( (this probably means you're using "
          "this function WRONG, or something WEIRD happened)");
      return true;
    }
    ping.send(_killUrself);
    // wait until second cubit will deregister
    if (await _checkUntilNoPort(killOtherCubitTimeout)) {
      return true;
    } else {
      loggI.e("Cubit didn't kill itself as nicely asked :(");
      return false;
    }
  }

  // todo: make this professional
  static const sppUuid = "00001101-0000-1000-8000-00805f9b34fb";

  Future<void> connect() async {
    _pendingConnect?.cancel();
    _pendingConnect = null;
    if (_isClosing || isClosed || _isConnecting || _connection != null) return;
    final connected = _watchedKnownDevices.keys
        .firstWhereOrNull((dev) => dev.isConnected.valueOrNull ?? false);
    if (connected == null) return;
    final model = matchModel(connected);
    if (model != null) await _connect(connected, model);
  }

  /// Android reports ACL_CONNECTED before all Bluetooth profiles are ready.
  /// Opening RFCOMM immediately at that point may block inside BluetoothSocket
  /// on some devices. Debounce duplicate broadcasts and let the profile settle.
  void _scheduleConnect() {
    if (_isClosing || isClosed || _connection != null) return;
    _pendingConnect?.cancel();
    _pendingConnect = Timer(_bluetoothProfileSettleDelay, () {
      _pendingConnect = null;
      unawaited(connect());
    });
  }

  Future<void> _connect(BluetoothDevice dev, MatchedModel model) async {
    if (_isClosing || isClosed || _isConnecting || _connection != null) return;

    _isConnecting = true;
    _activeDevice = dev;
    final placeholder = model.placeholder;
    StreamChannel<Uint8List>? connection;

    if (!isClosed) emit(HeadphonesConnecting(placeholder));
    try {
      for (var i = 0; i < connectTries; i++) {
        if (_isClosing || isClosed || !(dev.isConnected.valueOrNull ?? false)) {
          return;
        }
        try {
          connection = await _bluetooth.connectRfcomm(dev, sppUuid);
          break;
        } catch (_) {
          loggI.w('Error when connecting socket: ${i + 1}/$connectTries tries');
          if (!(dev.isConnected.valueOrNull ?? false) ||
              i + 1 >= connectTries) {
            rethrow;
          }
          await Future<void>.delayed(const Duration(milliseconds: 750));
        }
      }

      if (connection == null || _isClosing || isClosed) return;
      _connection = connection;
      emit(HeadphonesConnectedOpen(model.builder(connection, dev)));
      await connection.stream.listen((_) {}).asFuture<void>();
    } catch (error, stackTrace) {
      loggI.e(
        'Error while connecting to socket',
        error: error,
        stackTrace: stackTrace,
      );
    } finally {
      await connection?.sink.close();
      if (identical(_connection, connection)) _connection = null;
      if (_activeDevice == dev) _activeDevice = null;
      _isConnecting = false;
    }

    if (_isClosing ||
        isClosed ||
        !(_bluetooth.isEnabled.valueOrNull ?? false)) {
      return;
    }

    final deviceStillConnected = dev.isConnected.valueOrNull ?? false;
    emit(
      deviceStillConnected
          ? HeadphonesConnectedClosed(placeholder)
          : HeadphonesDisconnected(placeholder),
    );
    if (!deviceStillConnected) await connect();
  }

  Future<void> _pairedDevicesHandle(Iterable<BluetoothDevice> devices) async {
    if (_isClosing || isClosed) return;
    if (!(_bluetooth.isEnabled.valueOrNull ?? false)) {
      emit(const HeadphonesBluetoothDisabled());
      return;
    }

    final knownHeadphones = devices
        .map((dev) => (device: dev, match: matchModel(dev)))
        .where((entry) => entry.match != null)
        .toList(growable: false);
    final knownDevices = knownHeadphones.map((entry) => entry.device).toSet();

    for (final dev in _watchedKnownDevices.keys.toList()) {
      if (!knownDevices.contains(dev)) {
        final subscription = _watchedKnownDevices.remove(dev);
        await subscription?.cancel();
        if (_activeDevice == dev) await _connection?.sink.close();
      }
    }

    if (knownHeadphones.isEmpty) {
      await _connection?.sink.close();
      if (!isClosed) emit(const HeadphonesNotPaired());
      return;
    }

    for (final hp in knownHeadphones) {
      if (_watchedKnownDevices.containsKey(hp.device)) continue;
      var receivedInitialState = false;
      _watchedKnownDevices[hp.device] = hp.device.isConnected.distinct().listen(
        (connected) async {
          if (_isClosing || isClosed) return;
          final isInitialState = !receivedInitialState;
          receivedInitialState = true;
          if (connected) {
            // An already connected device can be opened immediately on startup.
            // A newly connected device needs time for Android profiles to settle.
            if (isInitialState) {
              unawaited(connect());
            } else {
              _scheduleConnect();
            }
            return;
          }
          _pendingConnect?.cancel();
          _pendingConnect = null;
          // An unrelated paired device must never affect the active socket.
          if (_activeDevice != hp.device) return;

          // Cancel a pending native connect/read on its IO worker. Never await
          // BluetoothSocket.close from this broadcast callback.
          unawaited(_bluetooth.cancelRfcomm());

          // Do not await BluetoothSocket.close from this broadcast callback.
          // Some Android Bluetooth stacks block there after ACL_DISCONNECTED,
          // freezing the Flutter UI. Publish the state immediately; the socket
          // read loop will observe the physical disconnect and clean itself up.
          _pendingConnect?.cancel();
          _pendingConnect = null;
          if (!isClosed) {
            emit(HeadphonesDisconnected(hp.match!.placeholder));
          }
        },
        onError: (Object error, StackTrace stackTrace) {
          loggI.e(
            'Bluetooth device state stream failed',
            error: error,
            stackTrace: stackTrace,
          );
        },
      );
    }

    await connect();

    // A supported device is paired, but none of them is currently connected.
    // Do not retain the initial HeadphonesNotPaired state in that case.
    if (!_isClosing &&
        !isClosed &&
        !_isConnecting &&
        _connection == null &&
        !knownDevices.any((dev) => dev.isConnected.valueOrNull ?? false)) {
      emit(HeadphonesDisconnected(knownHeadphones.first.match!.placeholder));
    }
  }

  HeadphonesConnectionCubit({required TheLastBluetooth bluetooth})
      : _bluetooth = bluetooth,
        super(const HeadphonesNotPaired()) {
    final rolex = Stopwatch()..start();
    _initInit().then(
      (_) => loggI.d("_initInit() took ${rolex.elapsedMilliseconds}ms"),
    );
  }

  /// THIS should be called ONLY ONCE
  // maybetodo: i'm wondering if to make this public, and not internally called
  // in constructor, but this would complicate di...
  Future<void> _initInit() async {
    // TODO: Some *tests* for this nightmare...
    if (await cubitAlreadyRunningSomewhere()) {
      loggI.w("Found already running cubit while init() - "
          "will wait $killOtherCubitTimeout and then kill it");
      if (await _checkUntilNoPort(killOtherCubitTimeout)) {
        loggI.i("Gone already, no need for war crimes 😇");
      } else {
        loggI.i("Killing other cubit...");
        // ### Important thoughts ###
        //
        // I think it's a good way to stop background tasks, because when cubit
        // closes, it closes all it's streams, thus all tasks waiting for
        // battery values or other shits will throw error/timeout that will be
        // try-catch'ed and nicely finish 👌
        //
        // But, leaving those notes here, in case, some day, they don't.
        if (!await killOtherCubit()) {
          loggI.f("Failed to kill other cubit 😵... well, anyway...");
        }
      }
    }

    if (_isClosing || isClosed) return;

    // And *NOW* we can talk...
    // TODO: I mean, actually, it still sometimes hangs... but I think that's
    // a deep un-fixable-for-now fault of jni :///
    IsolateNameServer.removePortNameMapping(pingReceivePortName);
    IsolateNameServer.registerPortWithName(
        _pingReceivePort.sendPort, pingReceivePortName);
    _pingReceivePortSS = _pingReceivePort.listen((message) {
      // ping back
      if (message is SendPort) message.send(true);
      // kill urself
      if (message == _killUrself) {
        loggI.w("Killing myself bc other cubit asked to 😖");
        close();
      }
    });
    _warCrimesFinished = true;

    return _init();
  }

  Future<void> _init() async {
    if (_isClosing || isClosed) return;
    if (_btEnabledStream != null) {
      loggI.w("_init() was already done and finished, but got called"
          "again. Weird.");
      return;
    }
    loggI.d("Starting init...");
    // last check in case it's a call from requestPermission()
    // this would prob mean user went through all permission asking stuff, and
    // _initInit() is stilllll running... 😵‍💫
    if (!_warCrimesFinished) {
      loggI.w("_init() called but _initInit() not finished 😵‍💫 - "
          "this isn't good, but we may survive this...");
      return;
    }
    // it's down here to be sure that we do have device connected so
    if (!await Permission.bluetoothConnect.isGranted) {
      if (_isClosing || isClosed) return;
      emit(const HeadphonesNoPermission());
      return;
    }
    // TODO: Some day, debug if (underneath?) this is somehow same instance
    // as the background one, and why does it crash/hang when we init it twice
    // ...but that's for another day xdddd
    if (_isClosing || isClosed) return;
    _bluetooth.init();
    _btEnabledStream = _bluetooth.isEnabled.listen((enabled) {
      if (!enabled && !_isClosing && !isClosed) {
        emit(const HeadphonesBluetoothDisabled());
      }
    });
    // logic of connect() is so universal we can use it on every change
    _devStream = _bluetooth.pairedDevices.listen(_pairedDevicesHandle);
  }

  // TODO:
  Future<bool> enableBluetooth() async => false;

  Future<void> openBluetoothSettings() => AppSettings.openAppSettings(
      type: AppSettingsType.bluetooth, asAnotherTask: true);

  Future<void> requestPermission() async {
    await Permission.bluetoothConnect.request();
    await _init();
  }

  @override
  Future<void> close() async {
    if (_isClosing || isClosed) return;
    _isClosing = true;
    _pendingConnect?.cancel();
    _pendingConnect = null;
    unawaited(_bluetooth.cancelRfcomm());

    await _connection?.sink.close();
    _connection = null;
    _activeDevice = null;
    await _btEnabledStream?.cancel();
    await _devStream?.cancel();
    for (final sub in _watchedKnownDevices.values) {
      await sub.cancel();
    }
    _watchedKnownDevices.clear();
    await _pingReceivePortSS?.cancel();
    _pingReceivePort.close();
    IsolateNameServer.removePortNameMapping(pingReceivePortName);
    await super.close();
  }
}
