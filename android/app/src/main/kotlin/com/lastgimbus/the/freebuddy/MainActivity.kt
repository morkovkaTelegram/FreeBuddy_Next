package com.lastgimbus.the.freebuddy

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.lastgimbus.the.freebuddy.ui.FreeBuddyApp
import com.lastgimbus.the.freebuddy.widget.WidgetStorage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { (application as FreeBuddyApplication).headphonesRepository.refreshAsync() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            lifecycleScope.launch { WidgetStorage.updateAll(applicationContext) }
        }
        enableEdgeToEdge()
        setContent {
            FreeBuddyApp(
                repository = (application as FreeBuddyApplication).headphonesRepository,
                requestBluetoothPermission = {
                    if (Build.VERSION.SDK_INT >= 31) permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                },
            )
        }
    }
}
