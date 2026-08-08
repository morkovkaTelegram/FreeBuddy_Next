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
import com.lastgimbus.the.freebuddy.protocol.FreeBuds5iCommands
import com.lastgimbus.the.freebuddy.protocol.HeadphonesSnapshot
import com.lastgimbus.the.freebuddy.protocol.Hold
import com.lastgimbus.the.freebuddy.protocol.MbbCommand
import com.lastgimbus.the.freebuddy.protocol.Swipe
import com.lastgimbus.the.freebuddy.protocol.TripleTap
import com.lastgimbus.the.freebuddy.protocol.apply
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
                    BluetoothDevice.ACTION_ACL_CONNECTED -> if (device?.isSupported() == true) {
                        delay(1200)
                        connectIfAvailable(device)
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                        if (device?.isSupported() == true) handlePossibleDisconnect(device)
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        val a2dpState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                        when {
                            // Buds placed in the case: A2DP drops immediately even
                            // though our RFCOMM socket still pins the ACL link.
                            a2dpState == BluetoothProfile.STATE_DISCONNECTED &&
                                device?.isSupported() == true -> handlePossibleDisconnect(device)
                            a2dpState == BluetoothProfile.STATE_CONNECTED &&
                                device?.isSupported() == true -> {
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
                    val updated = current.apply(command)
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
                if (command.serviceId == 43 && command.commandId == 54) {
                    send(MbbCommand(43, 47))
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
        val paired = bt.bondedDevices.filter { it.isSupported() }.sortedBy { it.address }
        if (paired.isEmpty()) {
            disconnectActive()
            _state.value = ConnectionState.NoSupportedDevices
            return
        }
        val connected = a2dp?.connectedDevices.orEmpty().filter { it.isSupported() }
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
                    client.connect(device)
                    activeDevice = device
                    _state.value = ConnectionState.Connected(
                        HeadphonesSnapshot(name = name, address = device.address)
                    )
                    WidgetStorage.saveAncAndUpdate(context, mode = null, connected = true)
                    // MBB pushes updates on its own (battery 1/39, ANC 43/42,
                    // dual-connect 43/54), so no periodic polling is needed.
                    FreeBuds5iCommands.initialRequests.forEach {
                        client.send(it)
                        delay(35)
                    }
                    startLivenessWatch(device)
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    client.close()
                    activeDevice = null
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
        adapter?.bondedDevices.orEmpty().filter { it.isSupported() }
            .sortedBy { it.address }.map { it.displayName() }
    }.getOrDefault(emptyList())

    private suspend fun disconnectActive() {
        livenessJob?.cancel()
        livenessJob = null
        connectJob?.cancel()
        connectJob = null
        activeDevice = null
        client.close()
        WidgetStorage.saveAncAndUpdate(context, mode = null, connected = false)
    }

    fun setAncMode(mode: AncMode): Boolean {
        if (_state.value !is ConnectionState.Connected) return false
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            connected.copy(snapshot = connected.snapshot.copy(ancMode = mode))
        }
        scope.launch { WidgetStorage.saveAncAndUpdate(context, mode, connected = true) }
        send(FreeBuds5iCommands.ancMode(mode))
        return true
    }
    fun setAncLevel(level: AncLevel) {
        val mode = (_state.value as? ConnectionState.Connected)?.snapshot?.ancMode ?: AncMode.NOISE_CANCELLING
        send(FreeBuds5iCommands.ancLevel(mode, level))
    }
    fun setEq(value: EqPreset) = sendAndRefresh(FreeBuds5iCommands.equalizer(value), MbbCommand(43, 74))
    fun setAutoPause(value: Boolean) = sendAndRefresh(FreeBuds5iCommands.autoPause(value), MbbCommand(43, 17))
    fun setLowLatency(value: Boolean) = sendAndRefresh(FreeBuds5iCommands.lowLatency(value), MbbCommand(43, 108, mapOf(2 to byteArrayOf())))
    fun setLdac(value: Boolean) = sendAndRefresh(FreeBuds5iCommands.ldac(value), MbbCommand(43, 163))
    fun setDualConnect(value: Boolean) = sendAndRefresh(FreeBuds5iCommands.dualConnect(value), MbbCommand(43, 47))
    fun refreshDualConnectDevices() {
        if (_state.value !is ConnectionState.Connected) return
        // Entries are merged by MAC as 43/49 responses arrive, so the current
        // list stays on screen instead of flashing an empty state.
        send(FreeBuds5iCommands.dualConnectEnumeration())
    }
    fun setDualDeviceConnected(device: DualConnectDevice, connected: Boolean) =
        sendAndRefreshDual(FreeBuds5iCommands.dualConnectAction(device, if (connected) 1 else 2))
    fun setDualDeviceAutoConnect(device: DualConnectDevice, enabled: Boolean) =
        sendAndRefreshDual(FreeBuds5iCommands.dualConnectAction(device, if (enabled) 4 else 5))
    fun setDualDevicePreferred(device: DualConnectDevice, enabled: Boolean) =
        sendAndRefreshDual(FreeBuds5iCommands.dualConnectPreferred(device, enabled))
    fun unpairDualDevice(device: DualConnectDevice) {
        // Remove locally right away: enumeration only merges entries by MAC
        // and would otherwise keep the unpaired device on screen.
        _state.update { state ->
            val connected = state as? ConnectionState.Connected ?: return@update state
            connected.copy(
                snapshot = connected.snapshot.copy(
                    dualConnectDevices = connected.snapshot.dualConnectDevices
                        .filterNot { it.address == device.address },
                ),
            )
        }
        sendAndRefreshDual(FreeBuds5iCommands.dualConnectAction(device, 3))
    }
    fun setDoubleTap(left: DoubleTap? = null, right: DoubleTap? = null) = sendAndRefresh(FreeBuds5iCommands.doubleTap(left, right), MbbCommand(1, 32))
    fun setTripleTap(left: TripleTap? = null, right: TripleTap? = null) = sendAndRefresh(FreeBuds5iCommands.tripleTap(left, right), MbbCommand(1, 38))
    fun setHold(value: Hold) = sendAndRefresh(FreeBuds5iCommands.hold(value), MbbCommand(43, 23))
    fun setHoldModes(value: Set<AncMode>) = sendAndRefresh(FreeBuds5iCommands.holdModes(value), MbbCommand(43, 25))
    fun setSwipe(value: Swipe) = sendAndRefresh(FreeBuds5iCommands.swipe(value), MbbCommand(43, 31))

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

    private fun BluetoothDevice.isSupported(): Boolean =
        name?.startsWith("HUAWEI FreeBuds 5i") == true
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
