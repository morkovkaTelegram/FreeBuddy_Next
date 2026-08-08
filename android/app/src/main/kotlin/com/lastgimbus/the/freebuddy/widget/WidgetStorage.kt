package com.lastgimbus.the.freebuddy.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.lastgimbus.the.freebuddy.protocol.BatteryLevel
import com.lastgimbus.the.freebuddy.protocol.BatteryLevels

object WidgetStorage {
    private const val FILE = "freebuddy_widget"
    const val LEFT = "left"
    const val RIGHT = "right"
    const val CASE = "case"
    private const val OPACITY_PREFIX = "opacity_"
    const val DEFAULT_OPACITY = 100

    // Keys for the per-instance Glance state. Pushing values through Glance
    // state (instead of reading prefs in provideGlance) is what makes updates
    // instant: a live composition session only recomposes when its state
    // changes, so values captured before provideContent would stay stale
    // until the session times out.
    val LEFT_KEY = intPreferencesKey(LEFT)
    val RIGHT_KEY = intPreferencesKey(RIGHT)
    val CASE_KEY = intPreferencesKey(CASE)
    val OPACITY_KEY = intPreferencesKey("opacity")

    /** Background opacity for one widget instance, percent 0..100. */
    fun readOpacity(context: Context, appWidgetId: Int): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(OPACITY_PREFIX + appWidgetId, DEFAULT_OPACITY)
            .coerceIn(0, 100)

    fun writeOpacity(context: Context, appWidgetId: Int, opacity: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(OPACITY_PREFIX + appWidgetId, opacity.coerceIn(0, 100))
            .apply()
    }

    fun removeOpacity(context: Context, appWidgetIds: IntArray) {
        val editor = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach { editor.remove(OPACITY_PREFIX + it) }
        editor.apply()
    }

    suspend fun saveAndUpdate(context: Context, battery: BatteryLevels) {
        val left = BatteryLevel.validOrNull(battery.left) ?: -1
        val right = BatteryLevel.validOrNull(battery.right) ?: -1
        val case = BatteryLevel.validOrNull(battery.case) ?: -1
        // Buds re-send battery packets with unchanged values; skip the whole
        // bitmap -> RemoteViews -> Binder round-trip when nothing changed.
        if (read(context) == Triple(left, right, case)) return
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(LEFT, left)
            .putInt(RIGHT, right)
            .putInt(CASE, case)
            .apply()
        updateAll(context)
    }

    /** Pushes current battery + per-instance opacity into each widget's
     * Glance state and triggers an update; state changes recompose live
     * sessions immediately. */
    suspend fun updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val (left, right, case) = read(context)
        suspend fun push(widget: GlanceAppWidget, cls: Class<out GlanceAppWidget>) {
            manager.getGlanceIds(cls).forEach { id ->
                val appWidgetId = runCatching { manager.getAppWidgetId(id) }.getOrDefault(0)
                val opacity = readOpacity(context, appWidgetId)
                updateAppWidgetState(context, id) { prefs ->
                    prefs[LEFT_KEY] = left
                    prefs[RIGHT_KEY] = right
                    prefs[CASE_KEY] = case
                    prefs[OPACITY_KEY] = opacity
                }
                widget.update(context, id)
            }
        }
        push(BatteryWidget(), BatteryWidget::class.java)
        push(BatteryRowsWidget(), BatteryRowsWidget::class.java)
    }

    suspend fun hasAnyWidget(context: Context): Boolean {
        val manager = GlanceAppWidgetManager(context)
        return manager.getGlanceIds(BatteryWidget::class.java).isNotEmpty() ||
            manager.getGlanceIds(BatteryRowsWidget::class.java).isNotEmpty()
    }

    fun read(context: Context): Triple<Int, Int, Int> {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return Triple(
            BatteryLevel.validOrNull(prefs.getInt(LEFT, -1)) ?: -1,
            BatteryLevel.validOrNull(prefs.getInt(RIGHT, -1)) ?: -1,
            BatteryLevel.validOrNull(prefs.getInt(CASE, -1)) ?: -1,
        )
    }
}
