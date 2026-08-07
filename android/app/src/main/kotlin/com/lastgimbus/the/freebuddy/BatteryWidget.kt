package com.lastgimbus.the.freebuddy

import HomeWidgetGlanceState
import HomeWidgetGlanceStateDefinition
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDefaults
import androidx.glance.unit.ColorProvider
import kotlin.math.max


class BatteryWidget : GlanceAppWidget() {
    companion object {
        private val SMALL_SQUARE = DpSize(60.dp, 60.dp)
        private val SMALL_HORIZONTAL_RECTANGLE = DpSize(200.dp, 60.dp)
        private val HORIZONTAL_RECTANGLE = DpSize(300.dp, 60.dp)
        private val VERTICAL_RECTANGLE = DpSize(80.dp, 160.dp)
        private val BIG_SQUARE = DpSize(180.dp, 180.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(
            SMALL_SQUARE,
            SMALL_HORIZONTAL_RECTANGLE,
            VERTICAL_RECTANGLE,
            HORIZONTAL_RECTANGLE,
            BIG_SQUARE,
        )
    )


    override val stateDefinition: GlanceStateDefinition<*>
        get() = HomeWidgetGlanceStateDefinition()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // TODO in future: Charging, but only when updates will be more "live"
        // currently there is so much lag between updates that it doesn't make sense :/
        // but it will if we'll have foreground service or something
        provideContent {
            GlanceContent(context, currentState())
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun GlanceContent(context: Context, currentState: HomeWidgetGlanceState) {
        GlanceTheme {
            val sp = currentState.preferences
            val left = sp.getInt("left", -1)
            val right = sp.getInt("right", -1)
            val case = sp.getInt("case", -1)
            val size = LocalSize.current
            val barColor = ColorProvider(R.color.battery_widget_bar_color)
            val barBackground = ColorProvider(R.color.battery_widget_bar_background)
            val textStyle = TextDefaults.defaultTextStyle.copy(
                color = ColorProvider(R.color.battery_widget_text_color),
                fontWeight = FontWeight.Medium, fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )

            @Composable
            fun BatteryBox(
                passGlanceModifier: GlanceModifier,
                icon: ImageProvider,
                level: Int,
                label: String,
                availableWidth: Dp = size.width
            ) {
                Box(
                    // this must be passed  here because for some reason the .defaultWeight() is context aware??
                    modifier = passGlanceModifier, contentAlignment = Alignment.Center
                ) {
                    LinearProgressIndicator(
                        modifier = GlanceModifier.cornerRadius(R.dimen.batteryWidgetInnerRadius).fillMaxSize(),
                        progress = (level / 100f).coerceIn(0f, 1f),
                        color = barColor,
                        backgroundColor = barBackground
                    )
                    Row(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Box(modifier = GlanceModifier.padding(R.dimen.batteryWidgetPadding)) {
                            Image(
                                icon,
                                context.getString(R.string.widget_device_icon, label),
                                modifier = GlanceModifier.size(30.dp),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                            )
                        }
                        if (availableWidth > 96.dp) Text(
                            text = label,
                            style = textStyle,
                            maxLines = 1
                        )
                        if (availableWidth >= SMALL_SQUARE.width) {
                            Spacer(modifier = GlanceModifier.size(0.dp).defaultWeight())
                            Box(
                                modifier = GlanceModifier.padding(R.dimen.batteryWidgetPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (level >= 0) {
                                        context.getString(
                                            R.string.widget_battery_level,
                                            level.coerceIn(0, 100)
                                        )
                                    } else {
                                        context.getString(R.string.widget_battery_unknown)
                                    },
                                    style = textStyle,
                                    modifier = GlanceModifier.width(52.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
                    .clickable(actionStartActivity(activity = MainActivity::class.java))
                    .background(GlanceTheme.colors.background).cornerRadius(R.dimen.batteryWidgetBackgroundRadius)
                    .padding(R.dimen.batteryWidgetPadding)
            ) {
                if (size.height < VERTICAL_RECTANGLE.height) {
                    Row(modifier = GlanceModifier.fillMaxSize()) {
                        // this must be passed  here because for some reason the .defaultWeight() is context aware??
                        val mod = GlanceModifier.defaultWeight().fillMaxHeight()
                        if (size.width <= SMALL_SQUARE.width) {
                            BatteryBox(mod, ImageProvider(R.drawable.earbuds), max(left, right), context.getString(R.string.widget_buds))
                        } else {
                            val avail = (size.width / 3) - (8.dp * 2)
                            BatteryBox(mod, ImageProvider(R.drawable.left_earbud), left, context.getString(R.string.widget_left_earbud), avail)
                            Spacer(modifier = GlanceModifier.size(R.dimen.batteryWidgetSquaresSpacerSize))
                            BatteryBox(mod, ImageProvider(R.drawable.right_earbud), right, context.getString(R.string.widget_right_earbud), avail)
                            Spacer(modifier = GlanceModifier.size(R.dimen.batteryWidgetSquaresSpacerSize))
                            BatteryBox(mod, ImageProvider(R.drawable.earbuds_case), case, context.getString(R.string.widget_case), avail)
                        }
                    }
                } else {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        // this must be passed  here because for some reason the .defaultWeight() is context aware??
                        val mod = GlanceModifier.defaultWeight() // no .fillMaxHeight()!
                        BatteryBox(mod, ImageProvider(R.drawable.left_earbud), left, context.getString(R.string.widget_left_earbud))
                        Spacer(modifier = GlanceModifier.size(R.dimen.batteryWidgetLinesSpacerSize))
                        BatteryBox(mod, ImageProvider(R.drawable.right_earbud), right, context.getString(R.string.widget_right_earbud))
                        Spacer(modifier = GlanceModifier.size(R.dimen.batteryWidgetLinesSpacerSize))
                        BatteryBox(mod, ImageProvider(R.drawable.earbuds_case), case, context.getString(R.string.widget_case))
                    }
                }
            }
        }
    }

}