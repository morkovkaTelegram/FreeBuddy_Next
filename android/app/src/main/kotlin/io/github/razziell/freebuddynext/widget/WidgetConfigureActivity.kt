package io.github.razziell.freebuddynext.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.github.razziell.freebuddynext.R
import io.github.razziell.freebuddynext.ui.FreeBuddyTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Per-instance widget configuration: launched by the system when a widget is
 * added (android:configure) and on long-press -> reconfigure
 * (android:widgetFeatures="reconfigurable").
 */
class WidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Cancelled by default so the launcher discards the widget if the
        // user backs out of the very first configuration.
        setResult(RESULT_CANCELED, resultIntent())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val initialBackgroundTransparency =
            100 - WidgetStorage.readBackgroundOpacity(this, appWidgetId)
        val initialElementTransparency =
            100 - WidgetStorage.readElementOpacity(this, appWidgetId)
        val initialLabelMode = WidgetStorage.readLabelMode(this, appWidgetId)
        val providerClass = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)
            ?.provider?.className
        val isAncWidget = providerClass == AncWidgetReceiver::class.java.name
        val isCompactWidget = providerClass == BatteryCompactWidgetReceiver::class.java.name
        val isColumnsWidget = providerClass == BatteryWidgetReceiver::class.java.name
        setContent {
            FreeBuddyTheme {
                ConfigureScreen(
                    initialBackgroundTransparency = initialBackgroundTransparency,
                    initialElementTransparency = initialElementTransparency,
                    initialLabelMode = initialLabelMode,
                    isAncWidget = isAncWidget,
                    isCompactWidget = isCompactWidget,
                    isColumnsWidget = isColumnsWidget,
                    onCancel = { finish() },
                    onSave = { backgroundTransparency, elementTransparency, labelMode ->
                        save(
                            backgroundOpacity = 100 - backgroundTransparency,
                            elementOpacity = 100 - elementTransparency,
                            labelMode = labelMode,
                        )
                    },
                )
            }
        }
    }

    private fun save(backgroundOpacity: Int, elementOpacity: Int, labelMode: Int) {
        WidgetStorage.writeOpacities(
            this,
            appWidgetId,
            backgroundOpacity,
            elementOpacity,
        )
        WidgetStorage.writeLabelMode(this, appWidgetId, labelMode)
        lifecycleScope.launch {
            WidgetStorage.updateAll(this@WidgetConfigureActivity)
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent(): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

@Composable
private fun ConfigureScreen(
    initialBackgroundTransparency: Int,
    initialElementTransparency: Int,
    initialLabelMode: Int,
    isAncWidget: Boolean,
    isCompactWidget: Boolean,
    isColumnsWidget: Boolean,
    onCancel: () -> Unit,
    onSave: (Int, Int, Int) -> Unit,
) {
    var backgroundTransparency by remember {
        mutableIntStateOf(initialBackgroundTransparency.coerceIn(0, 100))
    }
    var elementTransparency by remember {
        mutableIntStateOf(initialElementTransparency.coerceIn(0, 100))
    }
    var labelMode by remember { mutableIntStateOf(initialLabelMode) }
    val supportsLabels = !isAncWidget && !isCompactWidget
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.widget_transparency_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            when {
                isAncWidget -> AncWidgetPreview(backgroundTransparency, elementTransparency)
                isCompactWidget -> CompactWidgetPreview(backgroundTransparency, elementTransparency)
                isColumnsWidget -> ColumnsWidgetPreview(
                    backgroundTransparency = backgroundTransparency,
                    elementTransparency = elementTransparency,
                    labelMode = labelMode,
                )
                else -> RowsWidgetPreview(backgroundTransparency, elementTransparency, labelMode)
            }
            Spacer(Modifier.height(20.dp))
            TransparencyControl(
                label = stringResource(R.string.widget_background_transparency),
                transparency = backgroundTransparency,
                onTransparencyChange = { backgroundTransparency = it },
            )
            Spacer(Modifier.height(16.dp))
            TransparencyControl(
                label = stringResource(R.string.widget_elements_transparency),
                transparency = elementTransparency,
                onTransparencyChange = { elementTransparency = it },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.widget_transparency_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (supportsLabels) {
                Spacer(Modifier.height(20.dp))
                WidgetLabelModeMenu(
                    selected = labelMode,
                    onSelected = { labelMode = it },
                )
            }
            Spacer(Modifier.height(32.dp))
            Row {
                WidgetActionButton(onClick = onCancel, filled = false) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(16.dp))
                WidgetActionButton(
                    onClick = { onSave(backgroundTransparency, elementTransparency, labelMode) },
                    filled = true,
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun TransparencyControl(
    label: String,
    transparency: Int,
    onTransparencyChange: (Int) -> Unit,
) {
    Row(
        Modifier.widthIn(max = 400.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.widget_battery_level, transparency),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
    val transparencyState = stringResource(R.string.widget_battery_level, transparency)
    Slider(
        value = transparency.toFloat(),
        onValueChange = {
            onTransparencyChange(((it / 5f).roundToInt() * 5).coerceIn(0, 100))
        },
        valueRange = 0f..100f,
        steps = 19,
        modifier = Modifier
            .widthIn(max = 400.dp)
            .semantics {
                contentDescription = label
                stateDescription = transparencyState
            },
    )
}

@Composable
private fun CompactWidgetPreview(backgroundTransparency: Int, elementTransparency: Int) {
    val backgroundAlpha = (100 - backgroundTransparency) / 100f
    val elementAlpha = (100 - elementTransparency) / 100f
    val outer = colorResource(R.color.battery_widget_bar_background).copy(alpha = backgroundAlpha)
    val filled = colorResource(R.color.battery_widget_bar_color).copy(alpha = elementAlpha)
    val empty = colorResource(R.color.battery_widget_empty_color).copy(alpha = elementAlpha)
    val text = colorResource(R.color.battery_widget_text_color)
    Box(
        Modifier.widthIn(max = 340.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)),
    ) {
        Checkerboard(Modifier.matchParentSize())
        Row(
            Modifier.fillMaxWidth().background(outer, RoundedCornerShape(20.dp)).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CompactPreviewCell(R.drawable.left_earbud, 60, filled, empty, text, Modifier.weight(1f))
            CompactPreviewCell(R.drawable.right_earbud, 85, filled, empty, text, Modifier.weight(1f))
            CompactPreviewCell(R.drawable.earbuds_case, 100, filled, empty, text, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompactPreviewCell(
    @DrawableRes icon: Int,
    level: Int?,
    filled: Color,
    empty: Color,
    text: Color,
    modifier: Modifier,
) {
    Box(modifier.height(58.dp).clip(RoundedCornerShape(14.dp)).background(empty)) {
        if (level != null) {
            Box(Modifier.fillMaxWidth(level / 100f).fillMaxHeight().background(filled))
        }
        Row(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), text)
            Spacer(Modifier.weight(1f))
            Text(
                level?.let { stringResource(R.string.widget_battery_level, it) }
                    ?: stringResource(R.string.widget_battery_unknown),
                color = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AncWidgetPreview(backgroundTransparency: Int, elementTransparency: Int) {
    val backgroundAlpha = (100 - backgroundTransparency) / 100f
    val elementAlpha = (100 - elementTransparency) / 100f
    val outer = colorResource(R.color.battery_widget_bar_background).copy(alpha = backgroundAlpha)
    val selected = colorResource(R.color.battery_widget_bar_color).copy(alpha = elementAlpha)
    val unselected = colorResource(R.color.battery_widget_empty_color).copy(alpha = elementAlpha)
    val text = colorResource(R.color.battery_widget_text_color)

    Box(
        Modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
    ) {
        Checkerboard(Modifier.matchParentSize())
        Row(
            Modifier
                .fillMaxWidth()
                .background(outer, RoundedCornerShape(20.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AncPreviewButton(
                R.drawable.ic_anc_noise_control,
                selected,
                text,
                Modifier.weight(1f),
            )
            AncPreviewButton(
                R.drawable.ic_anc_off,
                unselected,
                text,
                Modifier.weight(1f),
            )
            AncPreviewButton(
                R.drawable.ic_anc_transparency,
                unselected,
                text,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AncPreviewButton(
    @DrawableRes icon: Int,
    background: Color,
    tint: Color,
    modifier: Modifier,
) {
    Box(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(38.dp),
        )
    }
}

/**
 * Live preview of the "rows" widget on a checkerboard backdrop so the
 * chosen transparency is immediately visible. Uses the exact same widget
 * color resources and the same alpha math as the real widgets.
 */
@Composable
private fun ColumnsWidgetPreview(
    backgroundTransparency: Int,
    elementTransparency: Int,
    labelMode: Int,
) {
    val backgroundAlpha = (100 - backgroundTransparency) / 100f
    val elementAlpha = (100 - elementTransparency) / 100f
    val outer = colorResource(R.color.battery_widget_bar_background).copy(alpha = backgroundAlpha)
    val filled = colorResource(R.color.battery_widget_bar_color).copy(alpha = elementAlpha)
    val empty = colorResource(R.color.battery_widget_empty_color).copy(alpha = elementAlpha)
    val text = colorResource(R.color.battery_widget_text_color)

    Box(
        Modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp)),
    ) {
        Checkerboard(Modifier.matchParentSize())
        Row(
            Modifier
                .fillMaxSize()
                .background(outer, RoundedCornerShape(20.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PreviewColumn(
                R.drawable.left_earbud,
                previewLabel(labelMode, stringResource(R.string.widget_left_earbud), "L"),
                60, filled, empty, text, Modifier.weight(1f),
            )
            PreviewColumn(
                R.drawable.right_earbud,
                previewLabel(labelMode, stringResource(R.string.widget_right_earbud), "R"),
                85, filled, empty, text, Modifier.weight(1f),
            )
            PreviewColumn(
                R.drawable.earbuds_case,
                previewLabel(labelMode, stringResource(R.string.widget_case), null),
                100, filled, empty, text, Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PreviewColumn(
    @DrawableRes icon: Int,
    label: String,
    level: Int,
    filled: Color,
    empty: Color,
    text: Color,
    modifier: Modifier,
) {
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(empty),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(level / 100f)
                .align(Alignment.BottomCenter)
                .background(filled),
        )
        Column(
            Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(icon), text)
            if (label.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    color = text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
            Text(
                stringResource(R.string.widget_battery_level, level),
                color = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RowsWidgetPreview(
    backgroundTransparency: Int,
    elementTransparency: Int,
    labelMode: Int,
) {
    val backgroundAlpha = (100 - backgroundTransparency) / 100f
    val elementAlpha = (100 - elementTransparency) / 100f
    val outer = colorResource(R.color.battery_widget_bar_background).copy(alpha = backgroundAlpha)
    val filled = colorResource(R.color.battery_widget_bar_color).copy(alpha = elementAlpha)
    val empty = colorResource(R.color.battery_widget_empty_color).copy(alpha = elementAlpha)
    val text = colorResource(R.color.battery_widget_text_color) // always opaque

    Box(
        Modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
    ) {
        Checkerboard(Modifier.matchParentSize())
        Column(
            Modifier
                .fillMaxWidth()
                .background(outer, RoundedCornerShape(20.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PreviewBar(
                R.drawable.left_earbud,
                previewLabel(labelMode, stringResource(R.string.widget_left_earbud), "L"),
                60, filled, empty, text,
            )
            PreviewBar(
                R.drawable.right_earbud,
                previewLabel(labelMode, stringResource(R.string.widget_right_earbud), "R"),
                85, filled, empty, text,
            )
            PreviewBar(
                R.drawable.earbuds_case,
                previewLabel(labelMode, stringResource(R.string.widget_case), null),
                100, filled, empty, text,
            )
        }
    }
}

private fun previewLabel(mode: Int, full: String, short: String?): String = when (mode) {
    WidgetStorage.LABELS_SHORT -> short.orEmpty()
    WidgetStorage.LABELS_HIDDEN -> ""
    else -> full
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetActionButton(
    onClick: () -> Unit,
    filled: Boolean,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var pending by remember { mutableStateOf(false) }
    val delayedClick: () -> Unit = {
        if (!pending) {
            pending = true
            scope.launch {
                try {
                    kotlinx.coroutines.delay(120)
                    onClick()
                } finally {
                    pending = false
                }
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(interaction) {
        interaction.interactions.collect { event ->
            if (event is androidx.compose.foundation.interaction.PressInteraction.Press) {
                scale.animateTo(0.9f, androidx.compose.animation.core.tween(90))
                scale.animateTo(
                    1f,
                    androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                    ),
                )
            }
        }
    }
    val modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
    if (filled) {
        Button(
            onClick = delayedClick,
            modifier = modifier,
            interactionSource = interaction,
            content = content,
        )
    } else {
        OutlinedButton(
            onClick = delayedClick,
            modifier = modifier,
            interactionSource = interaction,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetLabelModeMenu(selected: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        WidgetStorage.LABELS_FULL to R.string.widget_labels_full,
        WidgetStorage.LABELS_SHORT to R.string.widget_labels_short,
        WidgetStorage.LABELS_HIDDEN to R.string.widget_labels_hidden,
    )
    val selectedText = stringResource(
        options.firstOrNull { it.first == selected }?.second ?: R.string.widget_labels_full,
    )
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.widget_labels_title)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
        ) {
            options.forEach { (mode, label) ->
                val isSelected = mode == selected
                WidgetExpressiveDropdownMenuItem(
                    selected = isSelected,
                    text = { Text(stringResource(label)) },
                    trailingIcon = {
                        if (isSelected) {
                            androidx.compose.material3.Icon(Icons.Default.Check, null)
                        }
                    },
                    onClick = {
                        onSelected(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Matches the main app's Expressive menu row and keeps feedback visible before dismissal. */
@Composable
private fun WidgetExpressiveDropdownMenuItem(
    selected: Boolean,
    text: @Composable () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var pending by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(interaction) {
        interaction.interactions.collect { event ->
            if (event is androidx.compose.foundation.interaction.PressInteraction.Press) {
                scale.animateTo(0.97f, androidx.compose.animation.core.tween(75))
                scale.animateTo(
                    1f,
                    androidx.compose.animation.core.spring(
                        dampingRatio = 0.86f,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                    ),
                )
            }
        }
    }

    DropdownMenuItem(
        text = text,
        trailingIcon = trailingIcon,
        interactionSource = interaction,
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    Modifier
                },
            )
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
        onClick = {
            if (!pending) {
                pending = true
                scope.launch {
                    try {
                        kotlinx.coroutines.delay(120)
                        onClick()
                    } finally {
                        pending = false
                    }
                }
            }
        },
    )
}

@Composable
private fun PreviewBar(
    iconRes: Int,
    label: String,
    level: Int,
    filled: Color,
    empty: Color,
    text: Color,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(empty),
    ) {
        Box(
            Modifier
                .fillMaxWidth(level / 100f)
                .fillMaxHeight()
                .background(filled),
        )
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(iconRes), tint = text)
            if (label.isNotEmpty()) {
                Spacer(Modifier.width(10.dp))
                Text(label, color = text, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.widget_battery_level, level),
                color = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun Icon(painter: Painter, tint: Color) {
    androidx.compose.material3.Icon(
        painter = painter,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(20.dp),
    )
}

/** Classic checkerboard used to visualise transparency. */
@Composable
private fun Checkerboard(modifier: Modifier) {
    val light = Color(0xFFCCCCCC)
    val dark = Color(0xFF999999)
    Canvas(modifier) {
        val cell = 12.dp.toPx()
        var y = 0f
        var rowIndex = 0
        while (y < size.height) {
            var x = 0f
            var colIndex = rowIndex % 2
            while (x < size.width) {
                drawRect(
                    color = if (colIndex % 2 == 0) light else dark,
                    topLeft = Offset(x, y),
                    size = Size(cell, cell),
                )
                x += cell
                colIndex++
            }
            y += cell
            rowIndex++
        }
    }
}
