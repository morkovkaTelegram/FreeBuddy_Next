package com.lastgimbus.the.freebuddy.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lastgimbus.the.freebuddy.FreeBuddyApplication

class BluetoothConnectionReceiver : BroadcastReceiver() {
    // Reading device.name is guarded with runCatching; the repository re-checks
    // the permission before doing any real Bluetooth work anyway.
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != BluetoothDevice.ACTION_ACL_CONNECTED) return
        // Skip process wake-ups for unrelated devices (watches, cars, speakers).
        val name = runCatching { getDevice(intent)?.name }.getOrNull()
        if (name != null && !name.startsWith("HUAWEI FreeBuds 5i")) return
        // Never block BroadcastReceiver/main thread: all work starts on Dispatchers.IO.
        (context.applicationContext as? FreeBuddyApplication)
            ?.headphonesRepository
            ?.refreshAsync()
    }

    @Suppress("DEPRECATION")
    private fun getDevice(intent: Intent): BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }
}
