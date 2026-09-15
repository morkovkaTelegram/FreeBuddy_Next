package io.github.razziell.freebuddynext.companion

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.IntentSender
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.razziell.freebuddynext.FreeBuddyApplication
import io.github.razziell.freebuddynext.bluetooth.BackgroundDiagnostics
import io.github.razziell.freebuddynext.protocol.HeadphonesProtocolRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class CompanionDeviceRow(val address: String, val name: String, val associated: Boolean, val enabled: Boolean)
internal data class CompanionSetupState(
    val supported: Boolean = true,
    val permission: Boolean = true,
    val bluetooth: Boolean = true,
    val devices: List<CompanionDeviceRow> = emptyList(),
    val busy: Boolean = false,
    val error: Boolean = false,
)

/** Callbacks outlive Activity rotation; only the Activity launches the system confirmation UI. */
class CompanionSetupModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val app = application as FreeBuddyApplication
    private val operation = Mutex()
    private val mutableState = MutableStateFlow(CompanionSetupState())
    internal val state = mutableState.asStateFlow()
    private val mutableSender = MutableStateFlow<IntentSender?>(null)
    internal val sender = mutableSender.asStateFlow()
    private var cleared = false
    private var target: String?
        get() = saved["association_target"]
        set(value) { saved["association_target"] = value }

    @SuppressLint("MissingPermission")
    private suspend fun snapshot(): CompanionSetupState = withContext(Dispatchers.IO) {
        if (!CompanionAccess.supported(app) || Build.VERSION.SDK_INT < 31) return@withContext CompanionSetupState(supported = false)
        if (!CompanionAccess.hasPermission(app)) return@withContext CompanionSetupState(permission = false)
        val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
        val links = CompanionAccess.links(app)
        val enabled = CompanionAccess.enabled(app)
        val paired = adapter?.bondedDevices.orEmpty().filter {
            HeadphonesProtocolRegistry.findByBluetoothName(it.name) != null
        }.associate { it.address.uppercase() to (it.name ?: it.address) }
        val rows = (paired.keys + links.map { it.address }).sorted().map { address ->
            CompanionDeviceRow(address, paired[address] ?: address,
                associated = links.any { it.address == address }, enabled = address in enabled)
        }
        CompanionSetupState(bluetooth = adapter?.isEnabled == true, devices = rows)
    }

    fun reload() = viewModelScope.launch {
        operation.withLock {
            try {
                val updated = snapshot()
                mutableState.value = updated.copy(busy = mutableState.value.busy, error = mutableState.value.error)
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { failed(error) }
        }
    }

    fun enable(address: String) {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(busy = true, error = false)
        viewModelScope.launch {
            operation.withLock {
                try {
                    check(CompanionAccess.supported(app) && CompanionAccess.hasPermission(app))
                    val current = snapshot()
                    val row = current.devices.first { it.address == address }
                    if (row.associated) {
                        CompanionAccess.enable(app, address)
                        app.headphonesRepository.refreshAsync()
                        mutableState.value = snapshot()
                    } else {
                        check(current.bluetooth)
                        target = address
                        if (Build.VERSION.SDK_INT >= 31) associate(address)
                    }
                } catch (cancel: CancellationException) { throw cancel }
                catch (error: Exception) { failed(error) }
            }
        }
    }

    @Suppress("DEPRECATION")
    @RequiresApi(31)
    private fun associate(address: String) {
        val request = AssociationRequest.Builder().setSingleDevice(true)
            .addDeviceFilter(BluetoothDeviceFilter.Builder().setAddress(address).build()).build()
        val callback = object : CompanionDeviceManager.Callback() {
            override fun onDeviceFound(intentSender: IntentSender) { if (!cleared) mutableSender.value = intentSender }
            override fun onAssociationPending(intentSender: IntentSender) = onDeviceFound(intentSender)
            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                if (!cleared && Build.VERSION.SDK_INT >= 33) {
                    if (companionAddress(associationInfo.deviceMacAddress?.toString()) == target) complete()
                    else if (target != null) failed(IllegalStateException("Unexpected association"))
                }
            }
            override fun onFailure(error: CharSequence?) {
                if (!cleared) failed(IllegalStateException("Association request failed"))
            }
        }
        val manager = app.getSystemService(CompanionDeviceManager::class.java)
        BackgroundDiagnostics.record(app, "companion association request")
        if (Build.VERSION.SDK_INT >= 33) manager.associate(request, app.mainExecutor, callback)
        else manager.associate(request, callback, Handler(Looper.getMainLooper()))
    }

    fun senderLaunched() { mutableSender.value = null }
    fun launchFailed() { failed(IllegalStateException("System confirmation unavailable")) }

    fun associationResult(result: Int) {
        if (result == Activity.RESULT_OK) complete()
        else if (target != null) {
            target = null
            mutableState.value = mutableState.value.copy(busy = false)
            BackgroundDiagnostics.record(app, "companion association cancelled")
            reload()
        }
    }

    private fun complete() = viewModelScope.launch {
        operation.withLock {
            val address = target ?: return@withLock
            try {
                CompanionAccess.enable(app, address)
                target = null
                app.headphonesRepository.refreshAsync()
                mutableState.value = snapshot()
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { failed(error) }
        }
    }

    fun remove(address: String) {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(busy = true, error = false)
        viewModelScope.launch {
            operation.withLock {
                try {
                    CompanionAccess.remove(app, address)
                    mutableState.value = snapshot()
                } catch (cancel: CancellationException) { throw cancel }
                catch (error: Exception) { failed(error) }
            }
        }
    }

    private fun failed(error: Exception) {
        target = null
        mutableSender.value = null
        mutableState.value = mutableState.value.copy(busy = false, error = true)
        BackgroundDiagnostics.record(app, "companion setup error=${error.javaClass.simpleName}")
    }

    override fun onCleared() { cleared = true; super.onCleared() }
}
