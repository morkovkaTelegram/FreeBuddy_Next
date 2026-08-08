package com.lastgimbus.the.freebuddy.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.lastgimbus.the.freebuddy.protocol.AncMode
import com.lastgimbus.the.freebuddy.protocol.BatteryLevel
import com.lastgimbus.the.freebuddy.protocol.BatteryLevels

object WidgetStorage {
    private const val FILE = "freebuddy_widget"
    const val LEFT = "left"
    const val RIGHT = "right"
    const val CASE = "case"
    private const val ANC_MODE = "anc_mode"
    private const val ANC_CONNECTED = "anc_connected"
    private const val OPACITY_PREFIX = "opacity_"
    private const val LABEL_MODE_PREFIX = "label_mode_"
    const val DEFAULT_OPACITY = 100
    const val LABELS_FULL = 0
    const val LABELS_SHORT = 1
    const val LABELS_HIDDEN = 2

    // Keys for the per-instance Glance state. Pushing values through Glance
    // state (instead of reading prefs in provideGlance) is what makes updates
    // instant: a live composition session only recomposes when its state
    // changes, so values captured before provideContent would stay stale
    // until the session times out.
    val LEFT_KEY = intPreferencesKey(LEFT)
    val RIGHT_KEY = intPreferencesKey(RIGHT)
    val CASE_KEY = intPreferencesKey(CASE)
    val ANC_MODE_KEY = intPreferencesKey(ANC_MODE)
    val ANC_CONNECTED_KEY = intPreferencesKey(ANC_CONNECTED)
    val OPACITY_KEY = intPreferencesKey("opacity")
    val LABEL_MODE_KEY = intPreferencesKey("label_mode")

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

    fun readLabelMode(context: Context, appWidgetId: Int): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(LABEL_MODE_PREFIX + appWidgetId, LABELS_FULL)
            .coerceIn(LABELS_FULL, LABELS_HIDDEN)

    fun writeLabelMode(context: Context, appWidgetId: Int, mode: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(LABEL_MODE_PREFIX + appWidgetId, mode.coerceIn(LABELS_FULL, LABELS_HIDDEN))
            .apply()
    }

    fun removeOpacity(context: Context, appWidgetIds: IntArray) {
        val editor = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach {
            editor.remove(OPACITY_PREFIX + it)
            editor.remove(LABEL_MODE_PREFIX + it)
        }
        editor.apply()
    }

    suspend fun saveAncAndUpdate(context: Context, mode: AncMode?, connected: Boolean) {
        val modeCode = mode?.code ?: -1
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.getInt(ANC_MODE, -1) == modeCode &&
            prefs.getBoolean(ANC_CONNECTED, false) == connected
        ) return
        prefs.edit()
            .putInt(ANC_MODE, modeCode)
            .putBoolean(ANC_CONNECTED, connected)
            .apply()
        updateAll(context)
    }

    fun readAnc(context: Context): Pair<Int, Boolean> {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return prefs.getInt(ANC_MODE, -1) to prefs.getBoolean(ANC_CONNECTED, false)
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
        val (ancMode, ancConnected) = readAnc(context)
        suspend fun push(widget: GlanceAppWidget, cls: Class<out GlanceAppWidget>) {
            manager.getGlanceIds(cls).forEach { id ->
                val appWidgetId = runCatching { manager.getAppWidgetId(id) }.getOrDefault(0)
                val opacity = readOpacity(context, appWidgetId)
                val labelMode = readLabelMode(context, appWidgetId)
                updateAppWidgetState(context, id) { prefs ->
                    prefs[LEFT_KEY] = left
                    prefs[RIGHT_KEY] = right
                    prefs[CASE_KEY] = case
                    prefs[ANC_MODE_KEY] = ancMode
                    prefs[ANC_CONNECTED_KEY] = if (ancConnected) 1 else 0
                    prefs[OPACITY_KEY] = opacity
                    prefs[LABEL_MODE_KEY] = labelMode
                }
                widget.update(context, id)
            }
        }
        push(BatteryWidget(), BatteryWidget::class.java)
        push(BatteryRowsWidget(), BatteryRowsWidget::class.java)
        push(BatteryCompactWidget(), BatteryCompactWidget::class.java)
        push(AncWidget(), AncWidget::class.java)
    }

    suspend fun hasAnyWidget(context: Context): Boolean {
        val manager = GlanceAppWidgetManager(context)
        return manager.getGlanceIds(BatteryWidget::class.java).isNotEmpty() ||
            manager.getGlanceIds(BatteryRowsWidget::class.java).isNotEmpty() ||
            manager.getGlanceIds(BatteryCompactWidget::class.java).isNotEmpty() ||
            manager.getGlanceIds(AncWidget::class.java).isNotEmpty()
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
