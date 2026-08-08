package com.lastgimbus.the.freebuddy.ui

import android.content.Intent
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastgimbus.the.freebuddy.BuildConfig
import com.lastgimbus.the.freebuddy.R
import com.lastgimbus.the.freebuddy.data.ThemeStore
import kotlinx.coroutines.launch
import com.lastgimbus.the.freebuddy.bluetooth.ConnectionState
import com.lastgimbus.the.freebuddy.bluetooth.HeadphonesRepository
import com.lastgimbus.the.freebuddy.protocol.AncLevel
import com.lastgimbus.the.freebuddy.protocol.AncMode
import com.lastgimbus.the.freebuddy.protocol.BatteryLevel
import com.lastgimbus.the.freebuddy.protocol.BatteryLevels
import com.lastgimbus.the.freebuddy.protocol.DoubleTap
import com.lastgimbus.the.freebuddy.protocol.DualConnectDevice
import com.lastgimbus.the.freebuddy.protocol.DeviceColor
import com.lastgimbus.the.freebuddy.protocol.DeviceInfo
import com.lastgimbus.the.freebuddy.protocol.DualConnectionState
import com.lastgimbus.the.freebuddy.protocol.EqPreset
import com.lastgimbus.the.freebuddy.protocol.detectColor
import com.lastgimbus.the.freebuddy.protocol.FreeBudsSettings
import com.lastgimbus.the.freebuddy.protocol.HeadphonesSnapshot
import com.lastgimbus.the.freebuddy.protocol.Hold
import com.lastgimbus.the.freebuddy.protocol.Swipe
import com.lastgimbus.the.freebuddy.protocol.TripleTap

private enum class Screen { HOME, HEADPHONES_SETTINGS, ABOUT }


private val AppCardShape = RoundedCornerShape(16.dp)
private val LocalShowEmojis = staticCompositionLocalOf { true }
private val EmojiRegex = Regex(
    """[\x{1F000}-\x{1FAFF}\x{2100}-\x{214F}\x{2300}-\x{23FF}\x{2600}-\x{27BF}\uFE0E\uFE0F\u200D]+""",
)

private fun String.withoutEmojis(): String =
    replace(EmojiRegex, "").replace(Regex(" {2,}"), " ").trim()

@Composable
private fun stringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    val value = if (formatArgs.isEmpty()) {
        androidx.compose.ui.res.stringResource(id)
    } else {
        androidx.compose.ui.res.stringResource(id, *formatArgs)
    }
    return if (LocalShowEmojis.current) value else value.withoutEmojis()
}

@Composable
fun FreeBuddyApp(repository: HeadphonesRepository, requestBluetoothPermission: () -> Unit) {
    val context = LocalContext.current
    val showEmojis by ThemeStore.showEmojis(context)
        .collectAsStateWithLifecycle(initialValue = true)
    CompositionLocalProvider(LocalShowEmojis provides showEmojis) {
        FreeBuddyTheme {
            var screen by remember { mutableStateOf(Screen.HOME) }
            val state by repository.state.collectAsStateWithLifecycle()
            when (screen) {
                Screen.HOME -> HomeScreen(state, repository, requestBluetoothPermission, { screen = Screen.HEADPHONES_SETTINGS }, { screen = Screen.ABOUT })
                Screen.HEADPHONES_SETTINGS -> HeadphonesSettingsScreen(state, repository) { screen = Screen.HOME }
                Screen.ABOUT -> AboutScreen { screen = Screen.HOME }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: ConnectionState,
    repository: HeadphonesRepository,
    requestPermission: () -> Unit,
    openHeadphonesSettings: () -> Unit,
    openAbout: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.app_name)) }, actions = {
            IconButton(onClick = openAbout) { Icon(Icons.Default.Settings, stringResource(R.string.about)) }
        })
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (state) {
                ConnectionState.PermissionRequired -> StatusContent(R.string.page_home_no_permission, R.string.page_home_no_permission_desc, R.string.grant_access, requestPermission)
                ConnectionState.BluetoothDisabled -> StatusContent(R.string.bluetooth_disabled, R.string.bluetooth_disabled_desc, R.string.open_bluetooth_settings) { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                ConnectionState.NoSupportedDevices -> StatusContent(R.string.not_paired, R.string.not_paired_desc, R.string.open_bluetooth_settings) { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                is ConnectionState.Disconnected -> StatusContent(R.string.disconnected, R.string.disconnected_desc, R.string.open_bluetooth_settings) { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                is ConnectionState.Connecting -> StatusContent(R.string.connecting, R.string.connecting_desc)
                is ConnectionState.Failed -> StatusContent(R.string.connection_failed, R.string.connection_failed_desc, R.string.retry) { repository.reconnect() }
                is ConnectionState.Connected -> ConnectedContent(state.snapshot, repository, openHeadphonesSettings)
            }
        }
    }
}

@Composable
private fun StatusContent(@StringRes title: Int, @StringRes description: Int, @StringRes action: Int? = null, onClick: () -> Unit = {}) {
    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(stringResource(description), textAlign = TextAlign.Center)
        action?.let { Button(onClick = onClick) { Text(stringResource(it)) } }
    }
}

@Composable
private fun ConnectedContent(snapshot: HeadphonesSnapshot, repository: HeadphonesRepository, openSettings: () -> Unit) {
    val context = LocalContext.current
    val forceColor by ThemeStore.forceHeadphonesColor(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val storedColor by ThemeStore.headphonesColor(context)
        .collectAsStateWithLifecycle(initialValue = DeviceColor.BLACK.name)
    val selectedColor = storedColor.toDeviceColor()

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.connected),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    snapshot.name,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
        item {
            var showInfo by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(headphonesImage(snapshot.deviceInfo, forceColor, selectedColor)),
                    contentDescription = snapshot.name,
                    modifier = Modifier
                        .size(width = 260.dp, height = 180.dp)
                        .clickable { showInfo = true },
                    contentScale = ContentScale.Fit,
                )
            }
            if (showInfo) DeviceInfoDialog(snapshot, onDismiss = { showInfo = false })
        }
        item { BatteryCard(snapshot.battery) }
        item { AncCard(snapshot, repository) }
        item { DualConnectCard(snapshot, repository) }
        item { Button(onClick = openSettings, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.headphones_settings)) } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@DrawableRes
private fun headphonesImage(
    info: DeviceInfo?,
    forceColor: Boolean,
    selectedColor: DeviceColor,
): Int = when (if (forceColor) selectedColor else info?.detectColor()) {
    DeviceColor.WHITE -> R.drawable.freebuds_5i_w
    DeviceColor.BLUE -> R.drawable.freebuds_5i_bl
    DeviceColor.BLACK, null -> R.drawable.freebuds_5i_b
}

@Composable
private fun DeviceInfoDialog(snapshot: HeadphonesSnapshot, onDismiss: () -> Unit) {
    val info = snapshot.deviceInfo
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.device_info_title)) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DeviceInfoRow(R.string.device_info_model, info?.model ?: snapshot.name)
                DeviceInfoRow(R.string.device_info_submodel, info?.subModel)
                DeviceInfoRow(R.string.device_info_firmware, info?.firmware)
                DeviceInfoRow(R.string.device_info_hardware, info?.hardwareVersion)
                DeviceInfoRow(R.string.device_info_serial, info?.serialNumber)
                DeviceInfoRow(R.string.device_info_left_serial, info?.leftSerial)
                DeviceInfoRow(R.string.device_info_right_serial, info?.rightSerial)
                if (!info?.unknownFields.isNullOrEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Text(
                        stringResource(R.string.device_info_unknown_fields),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    info?.unknownFields?.forEach { (id, value) ->
                        DeviceInfoRow(
                            stringResource(R.string.device_info_tlv_field, id),
                            value,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        shape = AppCardShape,
    )
}

@Composable
private fun DeviceInfoRow(@StringRes label: Int, value: String?) =
    DeviceInfoRow(stringResource(label), value)

@Composable
private fun DeviceInfoRow(label: String, value: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label,
            modifier = Modifier.weight(0.8f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value ?: "—",
            modifier = Modifier.weight(1.6f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DualConnectCard(snapshot: HeadphonesSnapshot, repository: HeadphonesRepository) {
    var expanded by remember { mutableStateOf(false) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    val selectedDevice = snapshot.dualConnectDevices.firstOrNull { it.address == selectedAddress }

    LaunchedEffect(expanded) {
        if (expanded) repository.refreshDualConnectDevices()
    }

    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = snapshot.dualConnectEnabled != null) { expanded = !expanded }
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.dual_connect),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
                Switch(
                    checked = snapshot.dualConnectEnabled ?: false,
                    onCheckedChange = repository::setDualConnect,
                    enabled = snapshot.dualConnectEnabled != null,
                )
                IconButton(onClick = { expanded = !expanded }, enabled = snapshot.dualConnectEnabled != null) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        stringResource(if (expanded) R.string.collapse else R.string.expand),
                    )
                }
            }
            if (expanded) {
                HorizontalDivider()
                if (snapshot.dualConnectDevices.isEmpty()) {
                    Text(
                        stringResource(R.string.dual_connect_no_devices),
                        Modifier.fillMaxWidth().padding(20.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        snapshot.dualConnectDevices.forEach { device ->
                            DualDeviceCard(device) { selectedAddress = device.address }
                        }
                    }
                }
            }
        }
    }

    selectedDevice?.let { device ->
        DualDeviceSheet(
            device = device,
            repository = repository,
            onDismiss = { selectedAddress = null },
        )
    }
}

@Composable
private fun DualDeviceCard(device: DualConnectDevice, onClick: () -> Unit) {
    val stateText = when (device.connectionState) {
        DualConnectionState.PLAYING -> R.string.dual_connect_playing
        DualConnectionState.CONNECTED -> R.string.dual_connect_connected
        DualConnectionState.DISCONNECTED -> R.string.dual_connect_disconnected
    }
    val icon = when (device.connectionState) {
        DualConnectionState.PLAYING -> Icons.Default.BluetoothAudio
        DualConnectionState.CONNECTED -> Icons.Default.Bluetooth
        DualConnectionState.DISCONNECTED -> Icons.Default.BluetoothDisabled
    }
    Card(Modifier.width(112.dp).clickable(onClick = onClick), shape = AppCardShape) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Text(device.name, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(stateText),
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DualDeviceSheet(
    device: DualConnectDevice,
    repository: HeadphonesRepository,
    onDismiss: () -> Unit,
) {
    var confirmUnpair by remember(device.address) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(device.name, style = MaterialTheme.typography.headlineSmall)
            Text(device.address, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingSwitch(
                stringResource(R.string.dual_connect_auto_connect),
                device.autoConnect,
                true,
            ) { repository.setDualDeviceAutoConnect(device, it) }
            SettingSwitch(
                stringResource(R.string.dual_connect_preferred),
                device.preferred,
                true,
            ) { repository.setDualDevicePreferred(device, it) }
            Button(
                onClick = {
                    repository.setDualDeviceConnected(
                        device,
                        device.connectionState == DualConnectionState.DISCONNECTED,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (device.connectionState == DualConnectionState.DISCONNECTED) {
                            R.string.dual_connect_connect
                        } else {
                            R.string.dual_connect_disconnect
                        },
                    ),
                )
            }
            TextButton(onClick = { confirmUnpair = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, null)
                Text(stringResource(R.string.dual_connect_unpair), Modifier.padding(start = 8.dp))
            }
        }
    }
    if (confirmUnpair) {
        AlertDialog(
            onDismissRequest = { confirmUnpair = false },
            title = { Text(stringResource(R.string.dual_connect_unpair)) },
            text = { Text(stringResource(R.string.dual_connect_unpair_confirmation, device.name)) },
            dismissButton = {
                TextButton(onClick = { confirmUnpair = false }) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                Button(onClick = {
                    repository.unpairDualDevice(device)
                    confirmUnpair = false
                    onDismiss()
                }) { Text(stringResource(R.string.dual_connect_unpair)) }
            },
        )
    }
}

@Composable
private fun BatteryCard(battery: BatteryLevels) {
    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BatteryRow(R.drawable.left_earbud, R.string.left_bud_short, battery.left, battery.chargingLeft)
            BatteryRow(R.drawable.right_earbud, R.string.right_bud_short, battery.right, battery.chargingRight)
            BatteryRow(R.drawable.earbuds_case, R.string.case_label, battery.case, battery.chargingCase)
        }
    }
}

@Composable
private fun BatteryRow(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    level: Int?,
    charging: Boolean,
) {
    val validLevel = BatteryLevel.validOrNull(level)
    val fraction = BatteryLevel.fraction(validLevel)
    val shape = RoundedCornerShape(20.dp)
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
        }
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(30.dp),
            )
            Text(
                stringResource(label),
                Modifier.padding(start = 14.dp).weight(1f),
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Text(
                if (validLevel == null) "—" else "$validLevel%${if (charging) " ⚡" else ""}",
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun AncCard(snapshot: HeadphonesSnapshot, repository: HeadphonesRepository) {
    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.anc_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            if (snapshot.ancMode == AncMode.NOISE_CANCELLING) {
                AncLevelMenu(snapshot.ancLevel, repository::setAncLevel)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AncModeButton(AncMode.NOISE_CANCELLING, snapshot.ancMode, R.drawable.ic_anc_noise_control) {
                    repository.setAncMode(AncMode.NOISE_CANCELLING)
                }
                AncModeButton(AncMode.OFF, snapshot.ancMode, R.drawable.ic_anc_off) {
                    repository.setAncMode(AncMode.OFF)
                }
                AncModeButton(AncMode.TRANSPARENCY, snapshot.ancMode, R.drawable.ic_anc_transparency) {
                    repository.setAncMode(AncMode.TRANSPARENCY)
                }
            }
        }
    }
}

@Composable
private fun AncModeButton(mode: AncMode, selectedMode: AncMode?, @DrawableRes icon: Int, onClick: () -> Unit) {
    val content: @Composable () -> Unit = {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(mode.label()),
            modifier = Modifier.size(38.dp),
        )
    }
    if (mode == selectedMode) {
        FilledIconButton(onClick = onClick, modifier = Modifier.size(64.dp), content = content)
    } else {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(64.dp), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AncLevelMenu(selected: AncLevel?, choose: (AncLevel) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = selected?.let { stringResource(it.label()) }.orEmpty()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.anc_level_title)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = AppCardShape,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = AppCardShape,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AncLevel.entries.forEach { level ->
                val isSelected = level == selected
                DropdownMenuItem(
                    text = { Text(stringResource(level.label())) },
                    trailingIcon = {
                        if (isSelected) Icon(Icons.Default.Check, null)
                    },
                    onClick = {
                        expanded = false
                        choose(level)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeadphonesSettingsScreen(state: ConnectionState, repository: HeadphonesRepository, back: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.headphones_settings)) }, navigationIcon = { IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        val snapshot = (state as? ConnectionState.Connected)?.snapshot
        if (snapshot == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(stringResource(R.string.disconnected)) }
            return@Scaffold
        }
        val s = snapshot.settings
        LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { EnumDropdown(R.string.equalizer, EqPreset.entries, s.eqPreset, { it.eqLabel() }, repository::setEq) }
            item { CardSwitch(stringResource(R.string.auto_pause), s.autoPause ?: false, s.autoPause != null, stringResource(R.string.auto_pause_desc), repository::setAutoPause) }
            item { CardSwitch(stringResource(R.string.low_latency), s.lowLatency ?: false, s.lowLatency != null, stringResource(R.string.low_latency_desc), repository::setLowLatency) }
            item { CardSwitch(stringResource(R.string.ldac), s.ldac ?: false, s.ldac != null, stringResource(R.string.ldac_desc), repository::setLdac) }
            item { Text(stringResource(R.string.gestures), style = MaterialTheme.typography.titleLarge) }
            item { GesturePair(R.string.double_tap, DoubleTap.entries, s.doubleTapLeft, s.doubleTapRight, { it.doubleTapLabel() }, { repository.setDoubleTap(left = it) }, { repository.setDoubleTap(right = it) }) }
            item { GesturePair(R.string.triple_tap, TripleTap.entries, s.tripleTapLeft, s.tripleTapRight, { it.tripleTapLabel() }, { repository.setTripleTap(left = it) }, { repository.setTripleTap(right = it) }) }
            item { EnumDropdown(R.string.hold, Hold.entries, s.hold, { it.holdLabel() }, repository::setHold) }
            item { HoldModesCard(s, repository) }
            item { EnumDropdown(R.string.swipe, Swipe.entries, s.swipe, { it.swipeLabel() }, repository::setSwipe) }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun <T> GesturePair(@StringRes title: Int, values: List<T>, left: T?, right: T?, label: @Composable (T) -> String, setLeft: (T) -> Unit, setRight: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GestureColumn(R.string.left_bud_short, values, left, label, setLeft, Modifier.weight(1f))
            GestureColumn(R.string.right_bud_short, values, right, label, setRight, Modifier.weight(1f))
        }
    }
}

@Composable
private fun <T> GestureColumn(@StringRes title: Int, values: List<T>, selected: T?, label: @Composable (T) -> String, choose: (T) -> Unit, modifier: Modifier) {
    Card(modifier, shape = AppCardShape) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(title), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            values.forEach { value ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label(value), Modifier.weight(1f), maxLines = 2)
                    RadioButton(selected == value, { choose(value) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(@StringRes title: Int, values: List<T>, selected: T?, label: @Composable (T) -> String, choose: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = selected?.let { label(it) }.orEmpty()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(title)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = AppCardShape,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = AppCardShape,
            modifier = Modifier.fillMaxWidth(),
        ) {
            values.forEach { value ->
                val isSelected = value == selected
                DropdownMenuItem(
                    text = { Text(label(value)) },
                    trailingIcon = {
                        if (isSelected) Icon(Icons.Default.Check, null)
                    },
                    onClick = {
                        expanded = false
                        choose(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun HoldModesCard(settings: FreeBudsSettings, repository: HeadphonesRepository) {
    val current = settings.holdModes ?: emptySet()
    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.hold_modes), style = MaterialTheme.typography.titleMedium)
            Column {
                AncMode.entries.forEach { mode ->
                    SettingSwitch(stringResource(mode.label()), mode in current, settings.holdModes != null, stringResource(mode.description())) { enabled ->
                        val changed = if (enabled) current + mode else current - mode
                        if (changed.size >= 2) repository.setHoldModes(changed)
                    }
                }
            }
        }
    }
}

@Composable
private fun CardSwitch(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    description: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Card(Modifier.fillMaxWidth(), shape = AppCardShape) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            SettingSwitch(label, checked, enabled, description, onChange)
        }
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    description: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked, onChange, enabled = enabled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(back: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by ThemeStore.themeMode(context)
        .collectAsStateWithLifecycle(initialValue = ThemeStore.ThemeMode.SYSTEM)
    // Reuse the value already collected at the application UI root. Starting a second
    // DataStore collection here with `true` caused the switch to flash on every entry.
    val showEmojis = LocalShowEmojis.current
    val forceHeadphonesColor by ThemeStore.forceHeadphonesColor(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val storedHeadphonesColor by ThemeStore.headphonesColor(context)
        .collectAsStateWithLifecycle(initialValue = DeviceColor.BLACK.name)
    val selectedHeadphonesColor = storedHeadphonesColor.toDeviceColor()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.about)) }, navigationIcon = { IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.appearance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                Modifier.fillMaxWidth().clickable { showThemeDialog = true }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.theme), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(themeMode.label()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth()
                    .clickable { scope.launch { ThemeStore.setShowEmojis(context, !showEmojis) } }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.show_emojis), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.show_emojis_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = showEmojis,
                    onCheckedChange = { show ->
                        scope.launch { ThemeStore.setShowEmojis(context, show) }
                    },
                )
            }
            Row(
                Modifier.fillMaxWidth()
                    .clickable {
                        scope.launch {
                            ThemeStore.setForceHeadphonesColor(context, !forceHeadphonesColor)
                        }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.force_headphones_color),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.force_headphones_color_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = forceHeadphonesColor,
                    onCheckedChange = { force ->
                        scope.launch { ThemeStore.setForceHeadphonesColor(context, force) }
                    },
                )
            }
            if (forceHeadphonesColor) {
                EnumDropdown(
                    title = R.string.headphones_color,
                    values = DeviceColor.entries,
                    selected = selectedHeadphonesColor,
                    label = { stringResource(it.label()) },
                    choose = { color ->
                        scope.launch { ThemeStore.setHeadphonesColor(context, color.name) }
                    },
                )
            }
            HorizontalDivider()
            Text("FreeBuddy", style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.about_text))
            Text(stringResource(R.string.version_text, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"))
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme)) },
            text = {
                Column {
                    ThemeStore.ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    scope.launch { ThemeStore.setThemeMode(context, mode) }
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = themeMode == mode, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(mode.label()))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton({ showThemeDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private fun String.toDeviceColor(): DeviceColor =
    runCatching { DeviceColor.valueOf(this) }.getOrDefault(DeviceColor.BLACK)

private fun DeviceColor.label() = when (this) {
    DeviceColor.BLACK -> R.string.headphones_color_black
    DeviceColor.WHITE -> R.string.headphones_color_white
    DeviceColor.BLUE -> R.string.headphones_color_blue
}

private fun ThemeStore.ThemeMode.label() = when (this) {
    ThemeStore.ThemeMode.SYSTEM -> R.string.theme_system
    ThemeStore.ThemeMode.LIGHT -> R.string.theme_light
    ThemeStore.ThemeMode.DARK -> R.string.theme_dark
}

@Composable private fun AncMode.label() = when (this) { AncMode.OFF -> R.string.anc_off; AncMode.NOISE_CANCELLING -> R.string.anc_noise; AncMode.TRANSPARENCY -> R.string.anc_transparency }
@Composable private fun AncMode.description() = when (this) { AncMode.OFF -> R.string.anc_off_desc; AncMode.NOISE_CANCELLING -> R.string.anc_noise_desc; AncMode.TRANSPARENCY -> R.string.anc_transparency_desc }
@Composable private fun AncLevel.label() = when (this) { AncLevel.NORMAL -> R.string.anc_normal; AncLevel.COMFORT -> R.string.anc_comfort; AncLevel.ULTRA -> R.string.anc_ultra; AncLevel.DYNAMIC -> R.string.anc_dynamic }
@Composable private fun EqPreset.eqLabel() = stringResource(when (this) { EqPreset.DEFAULT -> R.string.eq_default; EqPreset.BASS -> R.string.eq_bass; EqPreset.TREBLE -> R.string.eq_treble; EqPreset.VOICES -> R.string.eq_voices })
@Composable private fun DoubleTap.doubleTapLabel() = stringResource(when (this) { DoubleTap.NONE -> R.string.action_none; DoubleTap.ASSISTANT -> R.string.action_assistant; DoubleTap.PLAY_PAUSE -> R.string.action_play_pause; DoubleTap.NEXT -> R.string.action_next; DoubleTap.PREVIOUS -> R.string.action_previous })
@Composable private fun TripleTap.tripleTapLabel() = stringResource(when (this) { TripleTap.NONE -> R.string.action_none; TripleTap.NEXT -> R.string.action_next; TripleTap.PREVIOUS -> R.string.action_previous })
@Composable private fun Hold.holdLabel() = stringResource(if (this == Hold.NONE) R.string.action_none else R.string.action_cycle_anc)
@Composable private fun Swipe.swipeLabel() = stringResource(if (this == Swipe.NONE) R.string.action_none else R.string.action_volume)
