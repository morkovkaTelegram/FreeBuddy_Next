package com.lastgimbus.the.freebuddy

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dev.fluttercommunity.workmanager.BackgroundWorker

class BluetoothDeviceConnectedReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BtDevConnReceiver"
        private const val TASK_ID_ROUTINE_UPDATE = "freebuddy.routine_update"
        private const val UNIQUE_IMMEDIATE_WORK = "freebuddy.immediate_routine_update"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE,
                BluetoothDevice::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "BLUETOOTH_CONNECT permission is not granted")
            return
        }

        val bluetoothClass = device.bluetoothClass ?: return
        if (bluetoothClass.majorDeviceClass != BluetoothClass.Device.Major.AUDIO_VIDEO) {
            return
        }

        val request = OneTimeWorkRequest.Builder(BackgroundWorker::class.java)
            .setInputData(
                Data.Builder()
                    .putString(BackgroundWorker.DART_TASK_KEY, TASK_ID_ROUTINE_UPDATE)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_WORK,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
