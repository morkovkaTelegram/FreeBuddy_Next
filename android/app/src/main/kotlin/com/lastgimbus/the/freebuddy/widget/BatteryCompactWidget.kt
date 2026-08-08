package com.lastgimbus.the.freebuddy.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lastgimbus.the.freebuddy.MainActivity
import com.lastgimbus.the.freebuddy.R
import com.lastgimbus.the.freebuddy.protocol.BatteryLevel

/** Compact 3x1 battery widget: icon and level only, without labels. */
class BatteryCompactWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (left, right, case) = WidgetStorage.read(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(0)
        val initialBackgroundOpacity = WidgetStorage.readBackgroundOpacity(context, appWidgetId)
        val initialElementOpacity = WidgetStorage.readElementOpacity(context, appWidgetId)
        provideContent {
            val prefs = currentState<Preferences>()
            Content(
                context = context,
                left = prefs[WidgetStorage.LEFT_KEY] ?: left,
                right = prefs[WidgetStorage.RIGHT_KEY] ?: right,
                case = prefs[WidgetStorage.CASE_KEY] ?: case,
                backgroundOpacity = prefs[WidgetStorage.BACKGROUND_OPACITY_KEY] ?: initialBackgroundOpacity,
                elementOpacity = prefs[WidgetStorage.ELEMENT_OPACITY_KEY] ?: initialElementOpacity,
            )
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun Content(
        context: Context,
        left: Int,
        right: Int,
        case: Int,
        backgroundOpacity: Int,
        elementOpacity: Int,
    ) {
        val outerBackground = ColorProvider(
            androidx.compose.ui.graphics.Color(
                WidgetProgressBitmap.withOpacity(
                    context.getColor(R.color.battery_widget_bar_background), backgroundOpacity,
                ),
            ),
        )
        val style = TextStyle(
            color = ColorProvider(R.color.battery_widget_text_color),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
        Row(
            GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .background(outerBackground)
                .cornerRadius(18.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            CompactCell(context, R.drawable.left_earbud, R.string.widget_left_earbud, left, elementOpacity, style, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(4.dp))
            CompactCell(context, R.drawable.right_earbud, R.string.widget_right_earbud, right, elementOpacity, style, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(4.dp))
            CompactCell(context, R.drawable.earbuds_case, R.string.widget_case, case, elementOpacity, style, GlanceModifier.defaultWeight())
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun CompactCell(
        context: Context,
        icon: Int,
        label: Int,
        level: Int,
        elementOpacity: Int,
        style: TextStyle,
        modifier: GlanceModifier,
    ) {
        val normalized = BatteryLevel.validOrNull(level)
        val batteryText = normalized?.let { context.getString(R.string.widget_battery_level, it) }
            ?: context.getString(R.string.widget_battery_unknown)
        val innerBackground = ColorProvider(
            androidx.compose.ui.graphics.Color(
                WidgetProgressBitmap.withOpacity(
                    context.getColor(R.color.battery_widget_empty_color), elementOpacity,
                ),
            ),
        )
        val progress = WidgetProgressBitmap.horizontal(
            level = normalized,
            filled = WidgetProgressBitmap.withOpacity(
                context.getColor(R.color.battery_widget_bar_color), elementOpacity,
            ),
            // The empty part is provided by the rounded cell background below.
            // Keeping it outside the bitmap makes the inner container visible
            // even in very short RemoteViews used by the compact widget.
            empty = android.graphics.Color.TRANSPARENT,
        )
        Box(
            modifier.fillMaxHeight()
                .background(innerBackground)
                .cornerRadius(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(progress),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Row(
                GlanceModifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 3.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(icon),
                    contentDescription = context.getString(
                        R.string.widget_device_icon,
                        context.getString(label),
                        batteryText,
                    ),
                    modifier = GlanceModifier.size(22.dp),
                    colorFilter = ColorFilter.tint(ColorProvider(R.color.battery_widget_text_color)),
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = batteryText,
                    style = style,
                    maxLines = 1,
                    modifier = GlanceModifier.width(42.dp),
                )
            }
        }
    }
}
