package com.lastgimbus.the.freebuddy.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.lastgimbus.the.freebuddy.R
import com.lastgimbus.the.freebuddy.ui.FreeBuddyTheme
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
        val initialTransparency = 100 - WidgetStorage.readOpacity(this, appWidgetId)
        val providerClass = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)
            ?.provider?.className
        val isAncWidget = providerClass == AncWidgetReceiver::class.java.name
        val isCompactWidget = providerClass == BatteryCompactWidgetReceiver::class.java.name
        setContent {
            FreeBuddyTheme {
                ConfigureScreen(
                    initialTransparency = initialTransparency,
                    isAncWidget = isAncWidget,
                    isCompactWidget = isCompactWidget,
                    onCancel = { finish() },
                    onSave = { transparency -> save(opacity = 100 - transparency) },
                )
            }
        }
    }

    private fun save(opacity: Int) {
        WidgetStorage.writeOpacity(this, appWidgetId, opacity)
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
    initialTransparency: Int,
    isAncWidget: Boolean,
    isCompactWidget: Boolean,
    onCancel: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var transparency by remember { mutableIntStateOf(initialTransparency.coerceIn(0, 100)) }
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.widget_transparency_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            when {
                isAncWidget -> AncWidgetPreview(transparency = transparency)
                isCompactWidget -> CompactWidgetPreview(transparency = transparency)
                else -> WidgetPreview(transparency = transparency)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.widget_battery_level, transparency),
                style = MaterialTheme.typography.headlineMedium,
            )
            Slider(
                value = transparency.toFloat(),
                onValueChange = {
                    transparency = ((it / 5f).roundToInt() * 5).coerceIn(0, 100)
                },
                valueRange = 0f..100f,
                steps = 19,
                modifier = Modifier.widthIn(max = 400.dp),
            )
            Text(
                stringResource(R.string.widget_transparency_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Row {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(16.dp))
                Button(onClick = { onSave(transparency) }) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun CompactWidgetPreview(transparency: Int) {
    val alpha = (100 - transparency) / 100f
    val outer = colorResource(R.color.battery_widget_bar_background).copy(alpha = alpha)
    val filled = colorResource(R.color.battery_widget_bar_color).copy(alpha = alpha)
    val empty = colorResource(R.color.battery_widget_empty_color).copy(alpha = alpha)
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
private fun AncWidgetPreview(transparency: Int) {
    val alpha = (100 - transparency) / 100f
    val outer = colorResource(R.color.battery_widget_bar_background).copy(alpha = alpha)
    val selected = colorResource(R.color.battery_widget_bar_color).copy(alpha = alpha)
    val unselected = colorResource(R.color.battery_widget_empty_color).copy(alpha = alpha)
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
private fun WidgetPreview(transparency: Int) {
    val opacity = 100 - transparency
    val alpha = opacity / 100f
    val outer = colorResource(R.color.battery_widget_bar_background).copy(alpha = alpha)
    val filled = colorResource(R.color.battery_widget_bar_color).copy(alpha = alpha)
    val empty = colorResource(R.color.battery_widget_empty_color).copy(alpha = alpha)
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
            PreviewBar(R.drawable.left_earbud, stringResource(R.string.widget_left_earbud), 60, filled, empty, text)
            PreviewBar(R.drawable.right_earbud, stringResource(R.string.widget_right_earbud), 85, filled, empty, text)
            PreviewBar(R.drawable.earbuds_case, stringResource(R.string.widget_case), 100, filled, empty, text)
        }
    }
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
            Spacer(Modifier.width(10.dp))
            Text(label, color = text, style = MaterialTheme.typography.bodyMedium)
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
