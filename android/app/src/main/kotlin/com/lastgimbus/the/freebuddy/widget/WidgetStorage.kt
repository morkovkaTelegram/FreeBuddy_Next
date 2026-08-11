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
    // Legacy single-opacity value is retained as a migration source for
    // widgets configured before split transparency was introduced.
    private const val LEGACY_OPACITY_PREFIX = "opacity_"
    private const val BACKGROUND_OPACITY_PREFIX = "background_opacity_"
    private const val ELEMENT_OPACITY_PREFIX = "element_opacity_"
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
    val BACKGROUND_OPACITY_KEY = intPreferencesKey("background_opacity")
    val ELEMENT_OPACITY_KEY = intPreferencesKey("element_opacity")
    val LABEL_MODE_KEY = intPreferencesKey("label_mode")

    /** Outer widget container opacity for one instance, percent 0..100. */
    fun readBackgroundOpacity(context: Context, appWidgetId: Int): Int =
        readSplitOpacity(context, BACKGROUND_OPACITY_PREFIX, appWidgetId)

    /** Progress bars and ANC button opacity, percent 0..100. */
    fun readElementOpacity(context: Context, appWidgetId: Int): Int =
        readSplitOpacity(context, ELEMENT_OPACITY_PREFIX, appWidgetId)

    private fun readSplitOpacity(context: Context, prefix: String, appWidgetId: Int): Int {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = prefix + appWidgetId
        // Existing widgets preserve their exact appearance: the old single
        // value seeds both new controls until the user saves them separately.
        return if (prefs.contains(key)) {
            prefs.getInt(key, DEFAULT_OPACITY)
        } else {
            prefs.getInt(LEGACY_OPACITY_PREFIX + appWidgetId, DEFAULT_OPACITY)
        }.coerceIn(0, 100)
    }

    fun writeOpacities(
        context: Context,
        appWidgetId: Int,
        backgroundOpacity: Int,
        elementOpacity: Int,
    ) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(BACKGROUND_OPACITY_PREFIX + appWidgetId, backgroundOpacity.coerceIn(0, 100))
            .putInt(ELEMENT_OPACITY_PREFIX + appWidgetId, elementOpacity.coerceIn(0, 100))
            .remove(LEGACY_OPACITY_PREFIX + appWidgetId)
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
            editor.remove(LEGACY_OPACITY_PREFIX + it)
            editor.remove(BACKGROUND_OPACITY_PREFIX + it)
            editor.remove(ELEMENT_OPACITY_PREFIX + it)
            editor.remove(LABEL_MODE_PREFIX + it)
        }
        editor.apply()
    }

    /** Persists the ANC state; returns true when something actually changed. */
    fun saveAnc(context: Context, mode: AncMode?, connected: Boolean): Boolean {
        val modeCode = mode?.code ?: -1
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.getInt(ANC_MODE, -1) == modeCode &&
            prefs.getBoolean(ANC_CONNECTED, false) == connected
        ) return false
        prefs.edit()
            .putInt(ANC_MODE, modeCode)
            .putBoolean(ANC_CONNECTED, connected)
            .apply()
        return true
    }

    suspend fun saveAncAndUpdate(context: Context, mode: AncMode?, connected: Boolean) {
        if (saveAnc(context, mode, connected)) updateAll(context)
    }

    fun readAnc(context: Context): Pair<Int, Boolean> {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return prefs.getInt(ANC_MODE, -1) to prefs.getBoolean(ANC_CONNECTED, false)
    }

    /** Persists battery levels; returns true when something actually changed. */
    fun saveBattery(context: Context, battery: BatteryLevels): Boolean {
        val left = BatteryLevel.validOrNull(battery.left) ?: -1
        val right = BatteryLevel.validOrNull(battery.right) ?: -1
        val case = BatteryLevel.validOrNull(battery.case) ?: -1
        // Buds re-send battery packets with unchanged values; skip the whole
        // bitmap -> RemoteViews -> Binder round-trip when nothing changed.
        if (read(context) == Triple(left, right, case)) return false
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(LEFT, left)
            .putInt(RIGHT, right)
            .putInt(CASE, case)
            .apply()
        return true
    }

    suspend fun saveAndUpdate(context: Context, battery: BatteryLevels) {
        if (saveBattery(context, battery)) updateAll(context)
    }

    /** Pushes current battery + per-instance appearance into each widget's
     * Glance state and triggers an update; state changes recompose live
     * sessions immediately. */
    suspend fun updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val (left, right, case) = read(context)
        val (ancMode, ancConnected) = readAnc(context)
        suspend fun push(widget: GlanceAppWidget, cls: Class<out GlanceAppWidget>) {
            manager.getGlanceIds(cls).forEach { id ->
                val appWidgetId = runCatching { manager.getAppWidgetId(id) }.getOrDefault(0)
                val backgroundOpacity = readBackgroundOpacity(context, appWidgetId)
                val elementOpacity = readElementOpacity(context, appWidgetId)
                val labelMode = readLabelMode(context, appWidgetId)
                updateAppWidgetState(context, id) { prefs ->
                    prefs[LEFT_KEY] = left
                    prefs[RIGHT_KEY] = right
                    prefs[CASE_KEY] = case
                    prefs[ANC_MODE_KEY] = ancMode
                    prefs[ANC_CONNECTED_KEY] = if (ancConnected) 1 else 0
                    prefs[BACKGROUND_OPACITY_KEY] = backgroundOpacity
                    prefs[ELEMENT_OPACITY_KEY] = elementOpacity
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
