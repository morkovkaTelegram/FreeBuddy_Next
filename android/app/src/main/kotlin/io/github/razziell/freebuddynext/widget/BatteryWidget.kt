package io.github.razziell.freebuddynext.widget

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
import io.github.razziell.freebuddynext.MainActivity
import io.github.razziell.freebuddynext.protocol.BatteryLevel
import io.github.razziell.freebuddynext.R

class BatteryWidget : GlanceAppWidget() {
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
        val initialBackgroundOpacity = WidgetStorage.readBackgroundOpacity(context, appWidgetId)
        val initialElementOpacity = WidgetStorage.readElementOpacity(context, appWidgetId)
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
                prefs[WidgetStorage.BACKGROUND_OPACITY_KEY] ?: initialBackgroundOpacity,
                prefs[WidgetStorage.ELEMENT_OPACITY_KEY] ?: initialElementOpacity,
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
        backgroundOpacity: Int,
        elementOpacity: Int,
        labelMode: Int,
    ) {
        val style = TextStyle(
            color = ColorProvider(R.color.battery_widget_text_color),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        val outerBackground = if (backgroundOpacity >= 100) {
            ColorProvider(R.color.battery_widget_bar_background)
        } else {
            // Resolved color is required to multiply alpha; theme switches are
            // covered by launcher-driven updates and WidgetAppearanceReceiver.
            ColorProvider(
                androidx.compose.ui.graphics.Color(
                    WidgetProgressBitmap.withOpacity(
                        context.getColor(R.color.battery_widget_bar_background), backgroundOpacity,
                    ),
                ),
            )
        }
        Row(
            GlanceModifier.fillMaxSize().appWidgetBackground()
                .background(outerBackground)
                .cornerRadius(18.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            BatteryCell(context, R.drawable.left_earbud, R.string.widget_left_earbud, left, style, elementOpacity, labelMode, "L", GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(4.dp))
            BatteryCell(context, R.drawable.right_earbud, R.string.widget_right_earbud, right, style, elementOpacity, labelMode, "R", GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(4.dp))
            BatteryCell(context, R.drawable.earbuds_case, R.string.widget_case, case, style, elementOpacity, labelMode, null, GlanceModifier.defaultWeight())
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun BatteryCell(
        context: Context,
        icon: Int,
        label: Int,
        level: Int,
        style: TextStyle,
        elementOpacity: Int,
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
        val progressBitmap = WidgetProgressBitmap.vertical(
            level = normalized,
            filled = WidgetProgressBitmap.withOpacity(
                context.getColor(R.color.battery_widget_bar_color), elementOpacity,
            ),
            empty = WidgetProgressBitmap.withOpacity(
                context.getColor(R.color.battery_widget_empty_color), elementOpacity,
            ),
        )
        Box(
            modifier.fillMaxHeight().cornerRadius(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(progressBitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Column(
                GlanceModifier.fillMaxSize().padding(4.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(icon),
                    contentDescription = context.getString(
                        R.string.widget_device_icon,
                        context.getString(label),
                        batteryText,
                    ),
                    modifier = GlanceModifier.size(26.dp),
                    colorFilter = ColorFilter.tint(ColorProvider(R.color.battery_widget_text_color)),
                )
                if (visibleLabel.isNotEmpty()) {
                    Text(visibleLabel, style = style, maxLines = 1)
                }
                Text(
                    batteryText,
                    style = style,
                    maxLines = 1,
                    modifier = GlanceModifier.width(52.dp),
                )
            }
        }
    }
}
