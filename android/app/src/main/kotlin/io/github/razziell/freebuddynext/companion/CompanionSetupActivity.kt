package io.github.razziell.freebuddynext.companion

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.razziell.freebuddynext.ui.appStringResource as stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.razziell.freebuddynext.R
import io.github.razziell.freebuddynext.ui.FreeBuddyTheme
import io.github.razziell.freebuddynext.widget.WidgetActionButton
import kotlinx.coroutines.launch

class CompanionSetupActivity : ComponentActivity() {
    private val model by lazy { ViewModelProvider(this)[CompanionSetupModel::class.java] }
    private val confirmation = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        model.associationResult(it.resultCode)
    }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { model.reload() }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.sender.collect { sender ->
                    if (sender != null) {
                        try {
                            confirmation.launch(IntentSenderRequest.Builder(sender).build())
                            model.senderLaunched()
                        } catch (error: Exception) { model.launchFailed() }
                    }
                }
            }
        }
        setContent {
            FreeBuddyTheme {
                val state by model.state.collectAsStateWithLifecycle()
                var removing by rememberSaveable { mutableStateOf<String?>(null) }
                Scaffold(topBar = {
                    TopAppBar(title = { Text(stringResource(R.string.companion_title)) }, navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    })
                }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.companion_description))
                        Text(stringResource(R.string.companion_limits), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!state.supported) Text(stringResource(R.string.companion_unavailable))
                        else if (!state.permission) {
                            WidgetActionButton(onClick = {
                                if (Build.VERSION.SDK_INT >= 31) permission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            }) { Text(stringResource(R.string.companion_permission)) }
                        } else {
                            if (!state.bluetooth) Text(stringResource(R.string.companion_bluetooth_off))
                            if (state.devices.isEmpty()) Text(stringResource(R.string.companion_empty))
                            state.devices.forEach { device ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(device.name, style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.semantics { heading() })
                                        if (device.name != device.address) Text(device.address, style = MaterialTheme.typography.bodySmall)
                                        Text(stringResource(if (device.enabled && device.associated) R.string.companion_enabled
                                            else R.string.companion_disabled))
                                        // An existing association may be retried if OEM observation failed.
                                        WidgetActionButton(onClick = { model.enable(device.address) },
                                            enabled = !state.busy && state.bluetooth) {
                                            Text(stringResource(if (device.enabled) R.string.companion_retry else R.string.companion_enable))
                                        }
                                        if (device.associated) WidgetActionButton(
                                            onClick = { removing = device.address }, enabled = !state.busy,
                                        ) { Text(stringResource(R.string.companion_remove)) }
                                    }
                                }
                            }
                        }
                        if (state.busy) {
                            CircularProgressIndicator()
                            Text(stringResource(R.string.companion_waiting), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        }
                        if (state.error) Text(stringResource(R.string.companion_error),
                            color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        WidgetActionButton(onClick = { model.reload() }, enabled = !state.busy) {
                            Text(stringResource(R.string.companion_refresh))
                        }
                    }
                }
                removing?.let { address ->
                    AlertDialog(onDismissRequest = { removing = null },
                        title = { Text(stringResource(R.string.companion_remove)) },
                        text = { Text(stringResource(R.string.companion_remove_hint)) },
                        confirmButton = {
                            WidgetActionButton(onClick = { removing = null; model.remove(address) }) {
                                Text(stringResource(android.R.string.ok))
                            }
                        }, dismissButton = {
                            WidgetActionButton(onClick = { removing = null }) { Text(stringResource(R.string.cancel)) }
                        })
                }
            }
        }
    }

    override fun onResume() { super.onResume(); model.reload() }
}
