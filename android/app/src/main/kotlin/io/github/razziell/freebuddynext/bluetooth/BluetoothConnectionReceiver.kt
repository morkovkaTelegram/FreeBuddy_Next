package io.github.razziell.freebuddynext.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothCodecStatus
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.razziell.freebuddynext.FreeBuddyApplication
import io.github.razziell.freebuddynext.protocol.HeadphonesProtocolRegistry

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
        if (intent?.action != BluetoothDevice.ACTION_ACL_CONNECTED) return
        // Skip process wake-ups for unrelated devices (watches, cars, speakers).
        val name = runCatching { getDevice(intent)?.name }.getOrNull()
        if (name != null && HeadphonesProtocolRegistry.findByBluetoothName(name) == null) return
        // Never block BroadcastReceiver/main thread: all work starts on Dispatchers.IO.
        (context.applicationContext as? FreeBuddyApplication)
            ?.headphonesRepository
            ?.refreshAsync()
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
