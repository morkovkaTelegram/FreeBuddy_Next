package com.lastgimbus.the.freebuddy.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.lastgimbus.the.freebuddy.protocol.AncLevel
import com.lastgimbus.the.freebuddy.protocol.AncMode
import com.lastgimbus.the.freebuddy.protocol.DoubleTap
import com.lastgimbus.the.freebuddy.protocol.DualConnectDevice
import com.lastgimbus.the.freebuddy.protocol.EqPreset
import com.lastgimbus.the.freebuddy.protocol.HeadphonesProtocol
import com.lastgimbus.the.freebuddy.protocol.HeadphonesProtocolRegistry
import com.lastgimbus.the.freebuddy.protocol.HeadphonesSnapshot
import com.lastgimbus.the.freebuddy.protocol.Hold
import com.lastgimbus.the.freebuddy.protocol.MbbCommand
import com.lastgimbus.the.freebuddy.protocol.Swipe
import com.lastgimbus.the.freebuddy.protocol.TripleTap
import com.lastgimbus.the.freebuddy.widget.WidgetStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ConnectionState {
    data object PermissionRequired : ConnectionState
    data object BluetoothDisabled : ConnectionState
    data object NoSupportedDevices : ConnectionState
    data class Disconnected(val devices: List<String>) : ConnectionState
    data class Connecting(val deviceName: String) : ConnectionState
    data class Connected(val snapshot: HeadphonesSnapshot) : ConnectionState
    data class Failed(val deviceName: String, val message: String?) : ConnectionState
}

@SuppressLint("MissingPermission")
class HeadphonesRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = RfcommClient()
    private val connectionMutex = Mutex()
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected(emptyList()))
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var a2dp: BluetoothA2dp? = null
    private var profileProxyRequested = false
    private var activeDevice: BluetoothDevice? = null
    private var activeProtocol: HeadphonesProtocol? = null
    private var connectJob: Job? = null
    private var livenessJob: Job? = null
    private var receiverRegistered = false

    private val adapter: BluetoothAdapter?
        get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    private val eventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val device = getDevice(intent)
            scope.launch {
                when (action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> refresh()
                    BluetoothDevice.ACTION_ACL_CONNECTED -> if (device?.protocol() != null) {
                        delay(1200)
                        connectIfAvailable(device)
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                        if (device?.protocol() != null) handlePossibleDisconnect(device)
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        val a2dpState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                        when {
                            // Buds placed in the case: A2DP drops immediately even
                            // though our RFCOMM socket still pins the ACL link.
                            a2dpState == BluetoothProfile.STATE_DISCONNECTED &&
                                device?.protocol() != null -> handlePossibleDisconnect(device)
                            a2dpState == BluetoothProfile.STATE_CONNECTED &&
                                device?.protocol() != null -> {
                                delay(1200)
                                connectIfAvailable(device)
                            }
                        }
                    }
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> refresh()
                }
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.A2DP) {
                a2dp = proxy as? BluetoothA2dp
                profileProxyRequested = false
                scope.launch { refresh() }
            }
        }
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                a2dp = null
                profileProxyRequested = false
                scope.launch { disconnectActive(); refresh() }
            }
        }
    }

    init {
        scope.launch {
            client.commands.collect { command ->
                var batteryChanged = false
                var ancChanged = false
                _state.update { state ->
                    val current = (state as? ConnectionState.Connected)?.snapshot ?: return@update state
                    val protocol = activeProtocol ?: return@update state
                    val updated = protocol.reduce(current, command)
                    if (updated == current) return@update state
                    batteryChanged = updated.battery != current.battery
                    ancChanged = updated.ancMode != current.ancMode
                    ConnectionState.Connected(updated)
                }
                if (batteryChanged) {
                    ((_state.value as? ConnectionState.Connected)?.snapshot?.battery)?.let {
                        WidgetStorage.saveAndUpdate(context, it)
                    }
                }
                if (ancChanged) {
                    val mode = (_state.value as? ConnectionState.Connected)?.snapshot?.ancMode
                    WidgetStorage.saveAncAndUpdate(context, mode, connected = true)
                }
                activeProtocol?.takeIf { it.isDualConnectChangeEvent(command) }?.let { protocol ->
                    send(protocol.dualConnectStateRequest())
                    refreshDualConnectDevices()
                }
            }
        }
        // The RFCOMM stream died on its own (no ACL broadcast is guaranteed in
        // that case) - drop the stale Connected state and re-evaluate.
        scope.launch {
            client.connectionLost.collect {
                disconnectActive()
                _state.value = ConnectionState.Disconnected(pairedSupportedNames())
                // The A2DP profile state lags the RFCOMM death by ~50-100ms
                // (seen in field logs); refreshing right away would read a
                // stale "connected" list and re-attach to buds in the case.
                delay(1000)
                refresh()
            }
        }
    }

    fun start() {
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                context,
                eventReceiver,
                IntentFilter().apply {
                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                    addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                    addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                    addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                    addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                },
                // Bluetooth stack runs as its own uid on modern Android; its protected
                // system broadcasts are NOT delivered to non-exported receivers.
                ContextCompat.RECEIVER_EXPORTED,
            )
            receiverRegistered = true
        }
        ensureProfileProxy()
        scope.launch { refresh() }
    }

    fun refreshAsync() { scope.launch { refresh() } }
    fun reconnect() { scope.launch { activeDevice?.let { connect(it, force = true) } ?: refresh() } }

    private suspend fun refresh() {
        if (!hasPermission()) {
            _state.value = ConnectionState.PermissionRequired
            return
        }
        val bt = adapter
        if (bt == null || !bt.isEnabled) {
            disconnectActive()
            _state.value = ConnectionState.BluetoothDisabled
            return
        }
        ensureProfileProxy()
        val paired = bt.bondedDevices.filter { it.protocol() != null }.sortedBy { it.address }
        if (paired.isEmpty()) {
            disconnectActive()
            _state.value = ConnectionState.NoSupportedDevices
            return
        }
        val connected = a2dp?.connectedDevices.orEmpty().filter { it.protocol() != null }
        val selected = connected.firstOrNull { it.sameAs(activeDevice) } ?: connected.firstOrNull()
        if (selected != null) connectIfAvailable(selected)
        else if (_state.value !is ConnectionState.Connecting) {
            disconnectActive()
            _state.value = ConnectionState.Disconnected(paired.map { it.displayName() })
        }
    }

    private suspend fun connectIfAvailable(device: BluetoothDevice) {
        if ((_state.value as? ConnectionState.Connected)?.snapshot?.address == device.address) return
        connect(device, force = false)
    }

    private suspend fun connect(device: BluetoothDevice, force: Boolean) {
        val protocol = device.protocol() ?: return
        connectionMutex.withLock {
            if (!force && connectJob?.isActive == true) return
            if (!force && activeDevice.sameAs(device) && _state.value is ConnectionState.Connected) return
            connectJob?.cancel()
            connectJob = scope.launch {
                val name = device.displayName()
                ensureProfileProxy()
                val proxy = a2dp
                if (proxy != null && proxy.getConnectionState(device) != BluetoothProfile.STATE_CONNECTED) {
                    // Buds in the case keep accepting RFCOMM; A2DP is the only
                    // signal that they are actually out and in use.
                    if (_state.value !is ConnectionState.Connected) {
                        _state.value = ConnectionState.Disconnected(pairedSupportedNames())
                    }
                    return@launch
                }
                _state.value = ConnectionState.Connecting(name)
                runCatching {
                    client.connect(device, protocol.sppUuid)
                    activeDevice = device
                    activeProtocol = protocol
                    _state.value = ConnectionState.Connected(
                        HeadphonesSnapshot(
                            protocolId = protocol.id,
                            name = name,
                            address = device.address,
                            capabilities = protocol.capabilities,
                        )
                    )
                    WidgetStorage.saveAncAndUpdate(context, mode = null, connected = true)
                    // MBB pushes updates on its own (battery 1/39, ANC 43/42,
                    // dual-connect 43/54), so no periodic polling is needed.
                    protocol.initialRequests.forEach {
                        client.send(it)
                        delay(35)
                    }
                    startLivenessWatch(device)
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    client.close()
                    activeDevice = null
                    activeProtocol = null
                    if (a2dp?.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                        _state.value = ConnectionState.Failed(name, error.message)
                    } else {
                        refresh()
                    }
                }
            }
        }
    }

    /**
     * ACL/A2DP broadcasts are not reliably delivered on every device, and our
     * own RFCOMM socket keeps the ACL link alive, so the buds going into the
     * case can pass silently. Poll the A2DP proxy instead:
     * getConnectionState() is a purely local binder call (no radio, the buds
     * are not woken), so this is battery-free unlike an MBB command watchdog.
     */
    private fun startLivenessWatch(device: BluetoothDevice) {
        livenessJob?.cancel()
        livenessJob = scope.launch {
            while (true) {
                delay(4000)
                if (_state.value !is ConnectionState.Connected) continue
                ensureProfileProxy()
                val proxy = a2dp
                if (proxy == null) continue
                val st = proxy.getConnectionState(device)
                if (st != BluetoothProfile.STATE_CONNECTED) {
                    disconnectActive()
                    refresh()
                    break
                }
            }
        }
    }

    private suspend fun handlePossibleDisconnect(device: BluetoothDevice) {
        // activeDevice may already be nulled by a concurrent teardown (seen in
        // field logs), so a disconnect of a supported device is handled even
        // when it no longer matches.
        val active = activeDevice
        if (active == null || device.sameAs(active)) {
            disconnectActive()
            refresh()
        }
    }

    private fun pairedSupportedNames(): List<String> = runCatching {
        adapter?.bondedDevices.orEmpty().filter { it.protocol() != null }
            .sortedBy { it.address }.map { it.displayName() }
    }.getOrDefault(emptyList())

    private suspend fun disconnectActive() {
        livenessJob?.cancel()
        livenessJob = null
        connectJob?.cancel()
        connectJob = null
        activeDevice = null
        activeProtocol = null
        client.close()
        WidgetStorage.saveAncAndUpdate(context, mode = null, connected = false)
    }

    fun setAncMode(mode: AncMode): Boolean {
        val protocol = activeProtocol ?: return false
        if (_state.value !is ConnectionState.Connected) return false
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            connected.copy(snapshot = connected.snapshot.copy(ancMode = mode))
        }
        scope.launch { WidgetStorage.saveAncAndUpdate(context, mode, connected = true) }
        send(protocol.ancMode(mode))
        return true
    }

    fun setAncLevel(level: AncLevel) = withProtocol { protocol ->
        val mode = (_state.value as? ConnectionState.Connected)?.snapshot?.ancMode
            ?: AncMode.NOISE_CANCELLING
        send(protocol.ancLevel(mode, level))
    }

    fun setEq(value: EqPreset) = withProtocol { sendAndRefresh(it.equalizer(value), it.equalizerStateRequest()) }
    fun setAutoPause(value: Boolean) = withProtocol { sendAndRefresh(it.autoPause(value), it.autoPauseStateRequest()) }
    fun setLowLatency(value: Boolean) = withProtocol { sendAndRefresh(it.lowLatency(value), it.lowLatencyStateRequest()) }
    fun setLdac(value: Boolean) = withProtocol { sendAndRefresh(it.ldac(value), it.ldacStateRequest()) }
    fun setDualConnect(value: Boolean) = withProtocol { sendAndRefresh(it.dualConnect(value), it.dualConnectStateRequest()) }

    fun refreshDualConnectDevices() = withProtocol { protocol ->
        if (_state.value is ConnectionState.Connected) send(protocol.dualConnectEnumeration())
    }

    fun setDualDeviceConnected(device: DualConnectDevice, connected: Boolean) = withProtocol {
        sendAndRefreshDual(it.dualConnectAction(device, if (connected) 1 else 2))
    }
    fun setDualDeviceAutoConnect(device: DualConnectDevice, enabled: Boolean) = withProtocol {
        if (it.capabilities.dualConnectAutoConnect) {
            sendAndRefreshDual(it.dualConnectAction(device, if (enabled) 4 else 5))
        }
    }
    fun setDualDevicePreferred(device: DualConnectDevice, enabled: Boolean) = withProtocol {
        sendAndRefreshDual(it.dualConnectPreferred(device, enabled))
    }
    fun unpairDualDevice(device: DualConnectDevice) {
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            connected.copy(
                snapshot = connected.snapshot.copy(
                    dualConnectDevices = connected.snapshot.dualConnectDevices
                        .filterNot { it.address == device.address },
                ),
            )
        }
        withProtocol { sendAndRefreshDual(it.dualConnectAction(device, 3)) }
    }
    fun setDoubleTap(left: DoubleTap? = null, right: DoubleTap? = null) = withProtocol {
        sendAndRefresh(it.doubleTap(left, right), it.doubleTapStateRequest())
    }
    fun setTripleTap(left: TripleTap? = null, right: TripleTap? = null) = withProtocol {
        sendAndRefresh(it.tripleTap(left, right), it.tripleTapStateRequest())
    }
    fun setHold(value: Hold) = withProtocol { sendAndRefresh(it.hold(value), it.holdStateRequest()) }
    fun setHoldModes(value: Set<AncMode>) = withProtocol {
        sendAndRefresh(it.holdModes(value), it.holdModesStateRequest())
    }
    fun setSwipe(value: Swipe) = withProtocol { sendAndRefresh(it.swipe(value), it.swipeStateRequest()) }

    private inline fun withProtocol(block: (HeadphonesProtocol) -> Unit) {
        activeProtocol?.let(block)
    }

    private fun send(command: MbbCommand) { scope.launch { runCatching { client.send(command) } } }
    private fun sendAndRefreshDual(command: MbbCommand) {
        scope.launch {
            runCatching {
                client.send(command)
                delay(500)
                refreshDualConnectDevices()
            }
        }
    }
    private fun sendAndRefresh(command: MbbCommand, refresh: MbbCommand) {
        scope.launch {
            runCatching {
                client.send(command)
                delay(250)
                client.send(refresh)
            }
        }
    }


    private fun ensureProfileProxy() {
        if (!hasPermission() || a2dp != null || profileProxyRequested) return
        profileProxyRequested = adapter?.getProfileProxy(
            context,
            profileListener,
            BluetoothProfile.A2DP,
        ) == true
    }

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun BluetoothDevice.protocol(): HeadphonesProtocol? =
        HeadphonesProtocolRegistry.findByBluetoothName(name)

    private fun BluetoothDevice.displayName(): String {
        val deviceAlias = if (Build.VERSION.SDK_INT >= 30) alias else null
        return deviceAlias?.takeIf { it.isNotBlank() } ?: name ?: "FreeBuds"
    }
    private fun BluetoothDevice?.sameAs(other: BluetoothDevice?) = this != null && other != null && address == other.address

    @Suppress("DEPRECATION")
    private fun getDevice(intent: Intent): BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
}
