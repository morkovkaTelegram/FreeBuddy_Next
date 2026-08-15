package io.github.razziell.freebuddynext

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import io.github.razziell.freebuddynext.ui.FreeBuddyApp
import io.github.razziell.freebuddynext.widget.WidgetStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { (application as FreeBuddyApplication).headphonesRepository.refreshAsync() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            // Off the main thread: updateAll reads SharedPreferences and builds RemoteViews.
            lifecycleScope.launch(Dispatchers.Default) { WidgetStorage.updateAll(applicationContext) }
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
