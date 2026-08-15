package io.github.razziell.freebuddynext.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Refreshes already placed widgets after wallpaper or locale changes.
 * ACTION_CONFIGURATION_CHANGED is not deliverable to manifest receivers,
 * so it is intentionally not listed here; dark-theme switches are covered
 * by values-night resources on the next regular widget update.
 */
class WidgetAppearanceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                // Skip all work when the user has no widgets on any home screen.
                if (WidgetStorage.hasAnyWidget(context.applicationContext)) {
                    WidgetStorage.updateAll(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_WALLPAPER_CHANGED,
        )
    }
}
