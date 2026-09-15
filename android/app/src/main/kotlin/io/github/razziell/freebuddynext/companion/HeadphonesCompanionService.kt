package io.github.razziell.freebuddynext.companion

import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.companion.DevicePresenceEvent
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.razziell.freebuddynext.FreeBuddyApplication
import io.github.razziell.freebuddynext.bluetooth.BackgroundDiagnostics
import io.github.razziell.freebuddynext.bluetooth.WidgetRefreshScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** System-bound, not started/foreground. CDM owns binding and process priority. */
@RequiresApi(31)
class HeadphonesCompanionService : CompanionDeviceService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val presence = CompanionPresence()
    private var recovery: Job? = null
    private val repository get() = (application as FreeBuddyApplication).headphonesRepository

    override fun onCreate() {
        super.onCreate()
        BackgroundDiagnostics.record(this, "companion service create")
    }

    @Suppress("DEPRECATION")
    override fun onDeviceAppeared(address: String) =
        received(address, CompanionPresence.Source.LEGACY, true, "legacy_appeared")

    @Suppress("DEPRECATION")
    override fun onDeviceDisappeared(address: String) =
        received(address, CompanionPresence.Source.LEGACY, false, "legacy_disappeared")

    @RequiresApi(33)
    override fun onDeviceAppeared(associationInfo: AssociationInfo) =
        received(associationInfo.deviceMacAddress?.toString(), CompanionPresence.Source.LEGACY, true, "appeared")

    @RequiresApi(33)
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) =
        received(associationInfo.deviceMacAddress?.toString(), CompanionPresence.Source.LEGACY, false, "disappeared")

    @RequiresApi(36)
    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        try {
            val address = CompanionAccess.links(this).firstOrNull { it.id == event.associationId }?.address ?: return
            when (event.event) {
                DevicePresenceEvent.EVENT_BT_CONNECTED ->
                    received(address, CompanionPresence.Source.BLUETOOTH, true, "bt_connected")
                DevicePresenceEvent.EVENT_BT_DISCONNECTED ->
                    received(address, CompanionPresence.Source.BLUETOOTH, false, "bt_disconnected")
                DevicePresenceEvent.EVENT_BLE_APPEARED ->
                    received(address, CompanionPresence.Source.BLE, true, "ble_appeared")
                DevicePresenceEvent.EVENT_BLE_DISAPPEARED ->
                    received(address, CompanionPresence.Source.BLE, false, "ble_disappeared")
            }
        } catch (error: Exception) {
            BackgroundDiagnostics.record(this, "companion callback error=${error.javaClass.simpleName}")
        }
    }

    private fun received(rawAddress: String?, source: CompanionPresence.Source, appeared: Boolean, reason: String) {
        val address = companionAddress(rawAddress) ?: return
        if (address !in CompanionAccess.enabled(this)) return
        presence.update(address, source, appeared)
        BackgroundDiagnostics.record(this, "companion presence $reason")
        val order = WidgetRefreshScheduler.nextArrivalOrder()
        val receivedAt = System.currentTimeMillis()
        scope.launch {
            try {
                WidgetRefreshScheduler.enqueue(applicationContext, appeared, address,
                    "companion_$reason", order, receivedAt)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                BackgroundDiagnostics.record(applicationContext, "companion enqueue error=${error.javaClass.simpleName}")
            }
        }
        // Never treat presence as proof that audio is ready: repository verifies A2DP.
        repository.refreshAsync()
        if (presence.anyPresent && recovery?.isActive != true) {
            recovery = scope.launch {
                while (isActive) {
                    delay(15_000)
                    presence.retain(CompanionAccess.enabled(applicationContext))
                    if (!presence.anyPresent || !CompanionAccess.hasPermission(applicationContext)) break
                    try {
                        // Serial, awaited reconciliation. An established session sends
                        // no periodic MBB reads; this only repairs a failed/lost socket.
                        repository.reconcileCompanionConnection()
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (error: Exception) {
                        BackgroundDiagnostics.record(applicationContext, "companion recovery error=${error.javaClass.simpleName}")
                    }
                }
            }
        } else if (!presence.anyPresent) {
            recovery?.cancel()
            recovery = null
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        BackgroundDiagnostics.record(this, "companion service unbind")
        recovery?.cancel()
        recovery = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        BackgroundDiagnostics.record(this, "companion service destroy")
        scope.cancel()
        // Do not close repository here: Activity/Worker may still be using it.
        super.onDestroy()
    }
}
