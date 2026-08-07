// ignore_for_file: constant_identifier_names
import 'dart:async';
import 'package:collection/collection.dart';
import 'package:flutter/services.dart';
import 'package:jni/jni.dart';
import 'package:rxdart/rxdart.dart';
import 'package:stream_channel/stream_channel.dart';
import 'package:the_last_bluetooth/src/android_bluetooth.g.dart' as jni;

import 'src/android_enums.dart' as enums;
import 'src/bluetooth_device.dart';
import 'src/ctrl_bluetooth_device.dart';
import 'src/utils.dart';

export 'src/bluetooth_device.dart';

class TheLastBluetooth {
  TheLastBluetooth._();

  static final TheLastBluetooth _instance = TheLastBluetooth._();

  late jni.BluetoothManager _manager;
  late jni.BluetoothAdapter _adapter;

  // Our streams for user:
  final _isEnabledCtrl = BehaviorSubject<bool>();
  final _pairedDevicesCtrl = BehaviorSubject<Set<CtrlBluetoothDevice>>();

  static const _eventsChannel = EventChannel(
    'the_last_bluetooth/events',
  );

  StreamSubscription<Object?>? _eventsSubscription;

  CtrlBluetoothDevice? _getPairedDevice(String? mac) {
    if (mac == null) return null;
    return _pairedDevicesCtrl.valueOrNull
        ?.firstWhereOrNull((device) => device.mac == mac);
  }

  void _refreshPairedDevices() {
    final previous = _pairedDevicesCtrl.valueOrNull ?? <CtrlBluetoothDevice>{};
    final refreshed = _adapter
        .getBondedDevices()!
        .map(
            (device) => CtrlBluetoothDevice.fromAndroidBluetoothDevice(device!))
        .toSet();
    final refreshedMacs = refreshed.map((device) => device.mac).toSet();
    for (final device in previous) {
      if (!refreshedMacs.contains(device.mac)) device.close();
    }
    _pairedDevicesCtrl.add(refreshed);
  }

  void _handleBroadcast(Object? rawEvent) {
    if (rawEvent is! Map) return;
    final event = Map<Object?, Object?>.from(rawEvent);
    final action = event['action'] as String?;
    final mac = event['address'] as String?;
    final device = _getPairedDevice(mac);

    switch (action) {
      case enums.BluetoothAdapter.ACTION_STATE_CHANGED:
        _isEnabledCtrl.addDistinct(
          event['adapterState'] == jni.BluetoothAdapter.STATE_ON,
        );
      case enums.BluetoothDevice.ACTION_BOND_STATE_CHANGED:
        // Rebuild outside Android's BroadcastReceiver callback. This also
        // handles devices added/removed while the app is alive.
        _refreshPairedDevices();
      case enums.BluetoothDevice.ACTION_ACL_CONNECTED:
        device?.isConnectedCtrl.addDistinct(true);
      case enums.BluetoothDevice.ACTION_ACL_DISCONNECTED:
        device?.isConnectedCtrl.addDistinct(false);
      case enums.BluetoothDevice.ACTION_NAME_CHANGED:
        final name = event['name'] as String?;
        if (name != null) device?.nameCtrl.addDistinct(name);
      case enums.BluetoothDevice.ACTION_ALIAS_CHANGED:
        final alias = event['alias'] as String?;
        if (alias != null) device?.aliasCtrl.addDistinct(alias);
      case enums.BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED:
        final battery = event['batteryLevel'] as int? ?? -1;
        if (battery >= 0) device?.batteryLevelCtrl.addDistinct(battery);
      case enums.BluetoothDevice.ACTION_UUID:
        final uuids = (event['uuids'] as List?)
            ?.whereType<String>()
            .map((uuid) => uuid.toLowerCase())
            .toSet();
        if (device != null &&
            uuids != null &&
            !device.uuidsCompleter.isCompleted) {
          device.uuidsCompleter.complete(uuids);
        }
    }
  }

  static TheLastBluetooth get instance => _instance;

  void init() {
    // TODO: maybe don't require permission to init plugin but gradually, huh??
    // TODO: DEVICES OFTEN "BLINK" ABOUT THEIR CONNECTION

    // this is some init stuff - maybe move this to manual init() dispose() ?
    final ctx = jni.Context.fromReference(Jni.getCachedApplicationContext());

    _manager = ctx
        .getSystemService(jni.Context.BLUETOOTH_SERVICE)!
        .as(jni.BluetoothManager.type, releaseOriginal: true);
    _adapter = _manager.getAdapter()!;

    // emit devices when enabled, clear when disabled
    _isEnabledCtrl.listen((event) {
      if (event) {
        _pairedDevicesCtrl.add(
          _adapter
              .getBondedDevices()!
              .map(
                (dev) => CtrlBluetoothDevice.fromAndroidBluetoothDevice(dev!),
              )
              .toSet(),
        );
      } else {
        _pairedDevicesCtrl.valueOrNull?.forEach((dev) => dev.close());
        _pairedDevicesCtrl.add(<CtrlBluetoothDevice>{});
      }
    });
    // this will also nicely trigger listener above :)
    _isEnabledCtrl.add(_adapter.isEnabled());

    // Native EventChannel receiver never waits synchronously for Dart from
    // Android's main thread (unlike the former dart_jni interface proxy).
    _eventsSubscription?.cancel();
    _eventsSubscription = _eventsChannel.receiveBroadcastStream().listen(
      _handleBroadcast,
      onError: (Object _, StackTrace __) {
        // Bluetooth/permission changes are reflected by the public streams.
      },
    );
  }

  static const _rfcommChannel = MethodChannel(
    'the_last_bluetooth/rfcomm',
  );

  /// Cancels an in-progress connect or closes the active native socket.
  /// The operation itself runs on a native IO worker.
  Future<void> cancelRfcomm() async {
    try {
      await _rfcommChannel.invokeMethod<void>('close');
    } catch (_) {
      // The Android stack may already have removed the socket.
    }
  }

  /// Opens an RFCOMM connection without blocking Flutter's UI isolate.
  /// Connect, blocking reads, writes and close run on native IO workers.
  Future<StreamChannel<Uint8List>> connectRfcomm(
    BluetoothDevice device,
    String serviceUuid, {
    bool force = false,
  }) async {
    final ourDev =
        _pairedDevicesCtrl.value.firstWhereOrNull((d) => d.mac == device.mac);
    if (ourDev == null || !(ourDev.isConnected.valueOrNull ?? false)) {
      throw StateError('Bluetooth device is not connected');
    }

    final toDevice = StreamController<Uint8List>();
    final fromDevice = StreamController<Uint8List>.broadcast();
    var isClosing = false;
    StreamSubscription<Uint8List>? writer;

    Future<void> closeEverything({bool notifyNative = true}) async {
      if (isClosing) return;
      isClosing = true;
      await writer?.cancel();
      if (notifyNative) {
        try {
          await _rfcommChannel.invokeMethod<void>('close');
        } catch (_) {}
      }
      if (!toDevice.isClosed) await toDevice.close();
      if (!fromDevice.isClosed) await fromDevice.close();
      _rfcommChannel.setMethodCallHandler(null);
    }

    _rfcommChannel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'data':
          if (!fromDevice.isClosed) {
            fromDevice.add(Uint8List.fromList(call.arguments as Uint8List));
          }
        case 'closed':
          await closeEverything(notifyNative: false);
      }
    });

    try {
      await _rfcommChannel.invokeMethod<void>('connect', <String, Object>{
        'address': ourDev.mac,
        'uuid': serviceUuid,
        'force': force,
      }).timeout(const Duration(seconds: 10));
    } catch (_) {
      await cancelRfcomm();
      _rfcommChannel.setMethodCallHandler(null);
      if (!toDevice.isClosed) await toDevice.close();
      if (!fromDevice.isClosed) await fromDevice.close();
      rethrow;
    }

    writer = toDevice.stream.listen(
      (bytes) async {
        try {
          await _rfcommChannel.invokeMethod<void>('write', bytes);
        } catch (error, stackTrace) {
          if (!fromDevice.isClosed) fromDevice.addError(error, stackTrace);
          await closeEverything(notifyNative: false);
        }
      },
      onDone: closeEverything,
      onError: (Object _, StackTrace __) => closeEverything(),
    );

    return StreamChannel(fromDevice.stream, toDevice.sink);
  }

  // maybetodo: Make this real ??
  ValueStream<bool> get isAvailable => Stream.value(true).shareValue();

  ValueStream<bool> get isEnabled => _isEnabledCtrl.stream;

  ValueStream<Set<BluetoothDevice>> get pairedDevices =>
      _pairedDevicesCtrl.stream;
}
