package com.lastgimbus.the.lastbluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Native Bluetooth transport and event bridge.
 *
 * BluetoothSocket operations run on IO workers. BroadcastReceiver never calls
 * Dart through dart_jni: doing so synchronously blocks Android's main thread
 * until Dart answers and causes an input-dispatch ANR during ACL events.
 */
class TheLastBluetoothPlugin : FlutterPlugin, MethodChannel.MethodCallHandler,
    EventChannel.StreamHandler {
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var applicationContext: Context
    private val ioExecutor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val closing = AtomicBoolean(false)
    private val lock = Any()

    @Volatile
    private var socket: BluetoothSocket? = null

    @Volatile
    private var eventSink: EventChannel.EventSink? = null

    private var receiverRegistered = false

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            val event = HashMap<String, Any?>()
            event["action"] = action

            try {
                @Suppress("DEPRECATION")
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                if (device != null) {
                    event["address"] = device.address
                    event["name"] = device.name
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        event["alias"] = device.alias
                    }
                    event["uuids"] = device.uuids?.map { it.uuid.toString().lowercase() }
                }

                event["adapterState"] = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                event["bondState"] = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                event["batteryLevel"] = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
            } catch (_: SecurityException) {
                // Permission may be revoked while the process is alive. Emit the
                // action anyway so Dart can refresh permission/device state.
            }

            // EventChannel enqueues the event for Flutter and returns. Unlike a
            // dart_jni interface proxy it never waits synchronously for Dart.
            eventSink?.success(event)
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, RFCOMM_CHANNEL)
        channel.setMethodCallHandler(this)
        eventChannel = EventChannel(binding.binaryMessenger, EVENTS_CHANNEL)
        eventChannel.setStreamHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        unregisterBluetoothReceiver()
        eventSink = null
        eventChannel.setStreamHandler(null)
        channel.setMethodCallHandler(null)
        closeSocket(notifyDart = false)
        ioExecutor.shutdownNow()
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        eventSink = events
        registerBluetoothReceiver()
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
        unregisterBluetoothReceiver()
    }

    private fun registerBluetoothReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            addAction(BluetoothDevice.ACTION_ALIAS_CHANGED)
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
            addAction(BluetoothDevice.ACTION_UUID)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationContext.registerReceiver(
                bluetoothReceiver,
                filter,
                Context.RECEIVER_EXPORTED,
            )
        } else {
            @Suppress("DEPRECATION")
            applicationContext.registerReceiver(bluetoothReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun unregisterBluetoothReceiver() {
        if (!receiverRegistered) return
        try {
            applicationContext.unregisterReceiver(bluetoothReceiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered by Android during process teardown.
        }
        receiverRegistered = false
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "connect" -> connect(call, result)
            "write" -> write(call, result)
            "close" -> ioExecutor.execute {
                closeSocket(notifyDart = false)
                success(result, null)
            }
            else -> result.notImplemented()
        }
    }

    private fun connect(call: MethodCall, result: MethodChannel.Result) {
        val address = call.argument<String>("address")
        val uuid = call.argument<String>("uuid")
        if (address == null || uuid == null) {
            result.error("invalid_arguments", "Bluetooth address and UUID are required", null)
            return
        }

        ioExecutor.execute {
            closeSocket(notifyDart = false)
            closing.set(false)
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                    ?: throw IOException("Bluetooth adapter is unavailable")
                val newSocket = adapter.getRemoteDevice(address)
                    .createRfcommSocketToServiceRecord(UUID.fromString(uuid))
                synchronized(lock) { socket = newSocket }
                newSocket.connect()
                success(result, null)
                startReader(newSocket)
            } catch (error: Throwable) {
                closeSocket(notifyDart = false)
                failure(result, "connect_failed", error)
            }
        }
    }

    private fun write(call: MethodCall, result: MethodChannel.Result) {
        val bytes = call.arguments as? ByteArray
        if (bytes == null) {
            result.error("invalid_arguments", "Byte array is required", null)
            return
        }
        ioExecutor.execute {
            try {
                val current = socket ?: throw IOException("Bluetooth socket is closed")
                synchronized(current) {
                    current.outputStream.write(bytes)
                    current.outputStream.flush()
                }
                success(result, null)
            } catch (error: Throwable) {
                closeSocket(notifyDart = true)
                failure(result, "write_failed", error)
            }
        }
    }

    private fun startReader(connectedSocket: BluetoothSocket) {
        ioExecutor.execute {
            val buffer = ByteArray(1024)
            try {
                while (!closing.get() && connectedSocket === socket) {
                    val count = connectedSocket.inputStream.read(buffer)
                    if (count < 0) break
                    if (count > 0) invokeDart("data", buffer.copyOf(count))
                }
            } catch (_: Throwable) {
                // Physical disconnect normally ends read with IOException.
            } finally {
                if (connectedSocket === socket) closeSocket(notifyDart = true)
            }
        }
    }

    private fun closeSocket(notifyDart: Boolean) {
        if (!closing.compareAndSet(false, true) && socket == null) return
        val old = synchronized(lock) {
            val current = socket
            socket = null
            current
        }
        try {
            old?.close()
        } catch (_: Throwable) {
        }
        if (notifyDart && old != null) invokeDart("closed", null)
    }

    private fun invokeDart(method: String, arguments: Any?) {
        mainHandler.post {
            if (::channel.isInitialized) channel.invokeMethod(method, arguments)
        }
    }

    private fun success(result: MethodChannel.Result, value: Any?) {
        mainHandler.post { result.success(value) }
    }

    private fun failure(result: MethodChannel.Result, code: String, error: Throwable) {
        mainHandler.post { result.error(code, error.message ?: error.javaClass.simpleName, null) }
    }

    companion object {
        private const val RFCOMM_CHANNEL = "the_last_bluetooth/rfcomm"
        private const val EVENTS_CHANNEL = "the_last_bluetooth/events"
        private const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL =
            "android.bluetooth.device.extra.BATTERY_LEVEL"
    }
}
