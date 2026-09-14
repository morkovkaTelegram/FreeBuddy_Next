package io.github.razziell.freebuddynext.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothCodecStatus
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.razziell.freebuddynext.FreeBuddyApplication
import io.github.razziell.freebuddynext.protocol.HeadphonesProtocolRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BluetoothConnectionReceiver : BroadcastReceiver() {
    // Reading device.name is guarded with runCatching; the repository re-checks
    // the permission before doing any real Bluetooth work anyway.
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == BluetoothCodecNames.CODEC_CONFIG_CHANGED_ACTION) {
            val device = getDevice(intent) ?: return
            val codec = BluetoothCodecNames.nameFrom(getCodecStatus(intent)) ?: return
            context.getSharedPreferences(
                BluetoothCodecNames.CACHE_PREFERENCES,
                Context.MODE_PRIVATE,
            ).edit().putString(device.address, codec).apply()
            (context.applicationContext as? FreeBuddyApplication)
                ?.headphonesRepository
                ?.refreshCodecFromCache(device.address)
            return
        }
        val event = when (intent?.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> "acl_connected"
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> "acl_disconnected"
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> when (
                intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
            ) {
                BluetoothProfile.STATE_CONNECTED -> "a2dp_connected"
                BluetoothProfile.STATE_DISCONNECTED -> "a2dp_disconnected"
                else -> return
            }
            else -> return
        }
        // The process is already awake at this point; only skip unnecessary work,
        // not the wake-up itself. A missing name must not suppress a valid event.
        val device = intent?.let(::getDevice)
        val name = runCatching { device?.name }.getOrNull()
        if (name != null && HeadphonesProtocolRegistry.findByBluetoothName(name) == null) return
        val appContext = context.applicationContext
        val address = runCatching { device?.address }.getOrNull()
        val order = WidgetRefreshScheduler.nextArrivalOrder()
        val receivedAt = System.currentTimeMillis()
        val connecting = event == "acl_connected" || event == "a2dp_connected"
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Only persist the job here. Waiting for RFCOMM (up to 10 s), A2DP
                // and MBB in a receiver would exceed its short execution window.
                WidgetRefreshScheduler.enqueue(appContext, connecting, address, event, order, receivedAt)
                // Application.start() and the dynamic receiver already maintain the
                // live repository. Do not launch a third competing refresh here.
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                BackgroundDiagnostics.record(appContext, "enqueue error=${error.javaClass.simpleName}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getCodecStatus(intent: Intent): BluetoothCodecStatus? =
        if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(
                BluetoothCodecStatus.EXTRA_CODEC_STATUS,
                BluetoothCodecStatus::class.java,
            )
        } else {
            intent.getParcelableExtra(BluetoothCodecStatus.EXTRA_CODEC_STATUS)
        }

    @Suppress("DEPRECATION")
    private fun getDevice(intent: Intent): BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }
}
