package com.lastgimbus.the.freebuddy.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import com.lastgimbus.the.freebuddy.FreeBuddyApplication
import com.lastgimbus.the.freebuddy.MainActivity
import com.lastgimbus.the.freebuddy.R
import com.lastgimbus.the.freebuddy.bluetooth.ConnectionState
import com.lastgimbus.the.freebuddy.protocol.AncMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Compact ANC controller: noise cancellation, off, and transparency. */
class AncWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (initialMode, initialConnected) = WidgetStorage.readAnc(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(0)
        val initialOpacity = WidgetStorage.readOpacity(context, appWidgetId)
        provideContent {
            val prefs = currentState<Preferences>()
            Content(
                context = context,
                modeCode = prefs[WidgetStorage.ANC_MODE_KEY] ?: initialMode,
                connected = (prefs[WidgetStorage.ANC_CONNECTED_KEY]
                    ?: if (initialConnected) 1 else 0) == 1,
                opacity = prefs[WidgetStorage.OPACITY_KEY] ?: initialOpacity,
            )
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun Content(context: Context, modeCode: Int, connected: Boolean, opacity: Int) {
        val outer = color(context, R.color.battery_widget_bar_background, opacity)
        Row(
            modifier = GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .background(outer)
                .cornerRadius(18.dp)
                .padding(6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            ModeButton(context, AncMode.NOISE_CANCELLING,
                R.string.anc_noise, modeCode, connected, opacity, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(5.dp))
            ModeButton(context, AncMode.OFF,
                R.string.anc_off, modeCode, connected, opacity, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(5.dp))
            ModeButton(context, AncMode.TRANSPARENCY,
                R.string.anc_transparency, modeCode, connected, opacity, GlanceModifier.defaultWeight())
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun ModeButton(
        context: Context,
        mode: AncMode,
        label: Int,
        selectedCode: Int,
        connected: Boolean,
        opacity: Int,
        modifier: GlanceModifier,
    ) {
        val selected = connected && selectedCode == mode.code
        val background = color(
            context,
            if (selected) R.color.battery_widget_bar_color else R.color.battery_widget_empty_color,
            opacity,
        )
        val description = if (connected) {
            context.getString(label)
        } else {
            context.getString(R.string.widget_anc_disconnected, context.getString(label))
        }
        Box(
            modifier = modifier.fillMaxHeight()
                .background(background)
                .cornerRadius(14.dp)
                .clickable(
                    if (connected) {
                        actionRunCallback<SetAncModeAction>(
                            actionParametersOf(AncModeKey to mode.code),
                        )
                    } else {
                        actionStartActivity<MainActivity>()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(mode.iconResource()),
                contentDescription = description,
                modifier = GlanceModifier.size(36.dp),
                colorFilter = ColorFilter.tint(
                    ColorProvider(R.color.battery_widget_text_color),
                ),
            )
        }
    }

    private fun color(context: Context, resource: Int, opacity: Int): ColorProvider =
        ColorProvider(
            Color(
                WidgetProgressBitmap.withOpacity(
                    context.getColor(resource),
                    opacity,
                ),
            ),
        )
}

private fun AncMode.iconResource(): Int = when (this) {
    AncMode.NOISE_CANCELLING -> R.drawable.ic_anc_noise_control
    AncMode.OFF -> R.drawable.ic_anc_off
    AncMode.TRANSPARENCY -> R.drawable.ic_anc_transparency
}

private val AncModeKey = ActionParameters.Key<Int>("anc_mode")

class SetAncModeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val mode = AncMode.entries.firstOrNull { it.code == parameters[AncModeKey] } ?: return
        val app = context.applicationContext as? FreeBuddyApplication ?: return
        val repository = app.headphonesRepository
        repository.refreshAsync()
        val connected = repository.state.value is ConnectionState.Connected ||
            withTimeoutOrNull(6_000) {
                repository.state.first { it is ConnectionState.Connected }
            } != null
        if (!connected || !repository.setAncMode(mode)) {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
