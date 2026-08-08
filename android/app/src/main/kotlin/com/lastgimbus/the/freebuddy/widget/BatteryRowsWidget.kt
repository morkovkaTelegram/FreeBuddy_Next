package com.lastgimbus.the.freebuddy.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lastgimbus.the.freebuddy.MainActivity
import com.lastgimbus.the.freebuddy.protocol.BatteryLevel
import com.lastgimbus.the.freebuddy.R

/** Battery widget with three horizontal rows stacked vertically. */
class BatteryRowsWidget : GlanceAppWidget() {
    // Progress is encoded in bitmaps and the layout does not read LocalSize,
    // so a single RemoteViews per instance is enough (Exact would rebuild the
    // content for every resize step for no benefit).
    override val sizeMode: SizeMode = SizeMode.Single
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Seed initial state so a freshly placed widget renders without
        // waiting for the next battery packet.
        val (left, right, case) = WidgetStorage.read(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(0)
        val initialOpacity = WidgetStorage.readOpacity(context, appWidgetId)
        val initialLabelMode = WidgetStorage.readLabelMode(context, appWidgetId)
        provideContent {
            // currentState makes the session recompose whenever
            // WidgetStorage.updateAll pushes new values - battery and
            // opacity changes apply immediately instead of on the next
            // full-update event.
            val prefs = currentState<Preferences>()
            Content(
                context,
                prefs[WidgetStorage.LEFT_KEY] ?: left,
                prefs[WidgetStorage.RIGHT_KEY] ?: right,
                prefs[WidgetStorage.CASE_KEY] ?: case,
                prefs[WidgetStorage.OPACITY_KEY] ?: initialOpacity,
                prefs[WidgetStorage.LABEL_MODE_KEY] ?: initialLabelMode,
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
        opacity: Int,
        labelMode: Int,
    ) {
        val textColor = ColorProvider(R.color.battery_widget_text_color)
        val labelStyle = TextStyle(
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start,
        )
        val levelStyle = TextStyle(
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )

        val outerBackground = if (opacity >= 100) {
            ColorProvider(R.color.battery_widget_bar_background)
        } else {
            // Resolved color is required to multiply alpha; theme switches are
            // covered by launcher-driven updates and WidgetAppearanceReceiver.
            ColorProvider(
                androidx.compose.ui.graphics.Color(
                    WidgetProgressBitmap.withOpacity(
                        context.getColor(R.color.battery_widget_bar_background), opacity,
                    ),
                ),
            )
        }
        Column(
            GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .background(outerBackground)
                .cornerRadius(18.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(6.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            BatteryRow(context, R.drawable.left_earbud, R.string.widget_left_earbud, left, labelStyle, levelStyle, opacity, labelMode, "L", GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.size(4.dp))
            BatteryRow(context, R.drawable.right_earbud, R.string.widget_right_earbud, right, labelStyle, levelStyle, opacity, labelMode, "R", GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.size(4.dp))
            BatteryRow(context, R.drawable.earbuds_case, R.string.widget_case, case, labelStyle, levelStyle, opacity, labelMode, null, GlanceModifier.defaultWeight())
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun BatteryRow(
        context: Context,
        icon: Int,
        label: Int,
        level: Int,
        labelStyle: TextStyle,
        levelStyle: TextStyle,
        opacity: Int,
        labelMode: Int,
        shortLabel: String?,
        modifier: GlanceModifier,
    ) {
        val normalized = BatteryLevel.validOrNull(level)
        val batteryText = normalized?.let {
            context.getString(R.string.widget_battery_level, it)
        } ?: context.getString(R.string.widget_battery_unknown)
        val visibleLabel = WidgetLabels.visibleLabel(context, labelMode, label, shortLabel)
        // Both the filled and empty parts fade with the user-chosen
        // transparency; text and icons stay fully opaque for readability.
        val progressBitmap = WidgetProgressBitmap.horizontal(
            level = normalized,
            filled = WidgetProgressBitmap.withOpacity(
                context.getColor(R.color.battery_widget_bar_color), opacity,
            ),
            empty = WidgetProgressBitmap.withOpacity(
                context.getColor(R.color.battery_widget_empty_color), opacity,
            ),
        )
        Box(
            modifier.fillMaxWidth().cornerRadius(14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Image(
                provider = ImageProvider(progressBitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Row(
                GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(icon),
                    contentDescription = context.getString(
                        R.string.widget_device_icon,
                        context.getString(label),
                        batteryText,
                    ),
                    modifier = GlanceModifier.size(28.dp),
                    colorFilter = ColorFilter.tint(ColorProvider(R.color.battery_widget_text_color)),
                )
                if (visibleLabel.isNotEmpty()) {
                    Spacer(GlanceModifier.width(10.dp))
                    Text(visibleLabel, style = labelStyle, maxLines = 1)
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = batteryText,
                    style = levelStyle,
                    maxLines = 1,
                    modifier = GlanceModifier.width(56.dp),
                )
            }
        }
    }
}
