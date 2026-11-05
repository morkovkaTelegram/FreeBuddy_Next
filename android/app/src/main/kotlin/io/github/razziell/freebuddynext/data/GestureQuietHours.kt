package io.github.razziell.freebuddynext.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.razziell.freebuddynext.FreeBuddyApplication
import io.github.razziell.freebuddynext.protocol.DoubleTap
import io.github.razziell.freebuddynext.protocol.Hold
import io.github.razziell.freebuddynext.protocol.SplitHoldAction
import io.github.razziell.freebuddynext.protocol.Swipe
import io.github.razziell.freebuddynext.protocol.TripleTap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

private val Context.gestureQuietDataStore by preferencesDataStore(name = "gesture_quiet_hours")

/**
 * Quiet hours for all gesture controls.
 *
 * When enabled and the current local time falls inside [startMinutes, endMinutes)
 * (range may wrap midnight, e.g. 23:30 → 06:40), the app forces double/triple tap,
 * hold and swipe to NONE on the headphones. Preferred actions chosen by the user
 * are stored separately and restored when the window ends or the feature is disabled.
 */
object GestureQuietHours {
    private val ENABLED = booleanPreferencesKey("enabled")
    private val START_MINUTES = intPreferencesKey("start_minutes")
    private val END_MINUTES = intPreferencesKey("end_minutes")

    private val PREF_DOUBLE_LEFT = stringPreferencesKey("pref_double_left")
    private val PREF_DOUBLE_RIGHT = stringPreferencesKey("pref_double_right")
    private val PREF_TRIPLE_LEFT = stringPreferencesKey("pref_triple_left")
    private val PREF_TRIPLE_RIGHT = stringPreferencesKey("pref_triple_right")
    private val PREF_HOLD = stringPreferencesKey("pref_hold")
    private val PREF_SWIPE = stringPreferencesKey("pref_swipe")
    private val PREF_SPLIT_HOLD_LEFT = stringPreferencesKey("pref_split_hold_left")
    private val PREF_SPLIT_HOLD_RIGHT = stringPreferencesKey("pref_split_hold_right")

    /** Default window: 23:30 – 06:40 */
    const val DEFAULT_START_MINUTES = 23 * 60 + 30
    const val DEFAULT_END_MINUTES = 6 * 60 + 40

    private const val ACTION_BOUNDARY = "io.github.razziell.freebuddynext.GESTURE_QUIET_BOUNDARY"
    private const val REQ_START = 4101
    private const val REQ_END = 4102

    data class Config(
        val enabled: Boolean = false,
        val startMinutes: Int = DEFAULT_START_MINUTES,
        val endMinutes: Int = DEFAULT_END_MINUTES,
    )

    data class PreferredGestures(
        val doubleLeft: DoubleTap? = null,
        val doubleRight: DoubleTap? = null,
        val tripleLeft: TripleTap? = null,
        val tripleRight: TripleTap? = null,
        val hold: Hold? = null,
        val swipe: Swipe? = null,
        val splitHoldLeft: SplitHoldAction? = null,
        val splitHoldRight: SplitHoldAction? = null,
    )

    fun config(context: Context): Flow<Config> =
        context.gestureQuietDataStore.data.map { prefs ->
            Config(
                enabled = prefs[ENABLED] ?: false,
                startMinutes = (prefs[START_MINUTES] ?: DEFAULT_START_MINUTES).coerceIn(0, 24 * 60 - 1),
                endMinutes = (prefs[END_MINUTES] ?: DEFAULT_END_MINUTES).coerceIn(0, 24 * 60 - 1),
            )
        }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.gestureQuietDataStore.edit { it[ENABLED] = enabled }
        scheduleAlarms(context)
    }

    suspend fun setRange(context: Context, startMinutes: Int, endMinutes: Int) {
        context.gestureQuietDataStore.edit {
            it[START_MINUTES] = startMinutes.coerceIn(0, 24 * 60 - 1)
            it[END_MINUTES] = endMinutes.coerceIn(0, 24 * 60 - 1)
        }
        scheduleAlarms(context)
    }

    fun preferred(context: Context): Flow<PreferredGestures> =
        context.gestureQuietDataStore.data.map { prefs ->
            PreferredGestures(
                doubleLeft = prefs[PREF_DOUBLE_LEFT]?.let { runCatching { DoubleTap.valueOf(it) }.getOrNull() },
                doubleRight = prefs[PREF_DOUBLE_RIGHT]?.let { runCatching { DoubleTap.valueOf(it) }.getOrNull() },
                tripleLeft = prefs[PREF_TRIPLE_LEFT]?.let { runCatching { TripleTap.valueOf(it) }.getOrNull() },
                tripleRight = prefs[PREF_TRIPLE_RIGHT]?.let { runCatching { TripleTap.valueOf(it) }.getOrNull() },
                hold = prefs[PREF_HOLD]?.let { runCatching { Hold.valueOf(it) }.getOrNull() },
                swipe = prefs[PREF_SWIPE]?.let { runCatching { Swipe.valueOf(it) }.getOrNull() },
                splitHoldLeft = prefs[PREF_SPLIT_HOLD_LEFT]?.let { runCatching { SplitHoldAction.valueOf(it) }.getOrNull() },
                splitHoldRight = prefs[PREF_SPLIT_HOLD_RIGHT]?.let { runCatching { SplitHoldAction.valueOf(it) }.getOrNull() },
            )
        }

    suspend fun savePreferredDouble(
        context: Context,
        left: DoubleTap? = null,
        right: DoubleTap? = null,
    ) {
        context.gestureQuietDataStore.edit { prefs ->
            left?.let { prefs[PREF_DOUBLE_LEFT] = it.name }
            right?.let { prefs[PREF_DOUBLE_RIGHT] = it.name }
        }
    }

    suspend fun savePreferredTriple(
        context: Context,
        left: TripleTap? = null,
        right: TripleTap? = null,
    ) {
        context.gestureQuietDataStore.edit { prefs ->
            left?.let { prefs[PREF_TRIPLE_LEFT] = it.name }
            right?.let { prefs[PREF_TRIPLE_RIGHT] = it.name }
        }
    }

    suspend fun savePreferredHold(context: Context, value: Hold) {
        context.gestureQuietDataStore.edit { it[PREF_HOLD] = value.name }
    }

    suspend fun savePreferredSwipe(context: Context, value: Swipe) {
        context.gestureQuietDataStore.edit { it[PREF_SWIPE] = value.name }
    }

    suspend fun savePreferredSplitHold(
        context: Context,
        left: SplitHoldAction? = null,
        right: SplitHoldAction? = null,
    ) {
        context.gestureQuietDataStore.edit { prefs ->
            left?.let { prefs[PREF_SPLIT_HOLD_LEFT] = it.name }
            right?.let { prefs[PREF_SPLIT_HOLD_RIGHT] = it.name }
        }
    }

    /** Seed preferred values from the device once, ignoring NONE. */
    suspend fun ensurePreferredFromDevice(
        context: Context,
        doubleLeft: DoubleTap?,
        doubleRight: DoubleTap?,
        tripleLeft: TripleTap?,
        tripleRight: TripleTap?,
        hold: Hold? = null,
        swipe: Swipe? = null,
        splitHoldLeft: SplitHoldAction? = null,
        splitHoldRight: SplitHoldAction? = null,
    ) {
        context.gestureQuietDataStore.edit { prefs ->
            if (prefs[PREF_DOUBLE_LEFT] == null && doubleLeft != null && doubleLeft != DoubleTap.NONE) {
                prefs[PREF_DOUBLE_LEFT] = doubleLeft.name
            }
            if (prefs[PREF_DOUBLE_RIGHT] == null && doubleRight != null && doubleRight != DoubleTap.NONE) {
                prefs[PREF_DOUBLE_RIGHT] = doubleRight.name
            }
            if (prefs[PREF_TRIPLE_LEFT] == null && tripleLeft != null && tripleLeft != TripleTap.NONE) {
                prefs[PREF_TRIPLE_LEFT] = tripleLeft.name
            }
            if (prefs[PREF_TRIPLE_RIGHT] == null && tripleRight != null && tripleRight != TripleTap.NONE) {
                prefs[PREF_TRIPLE_RIGHT] = tripleRight.name
            }
            if (prefs[PREF_HOLD] == null && hold != null && hold != Hold.NONE) {
                prefs[PREF_HOLD] = hold.name
            }
            if (prefs[PREF_SWIPE] == null && swipe != null && swipe != Swipe.NONE) {
                prefs[PREF_SWIPE] = swipe.name
            }
            if (prefs[PREF_SPLIT_HOLD_LEFT] == null && splitHoldLeft != null && splitHoldLeft != SplitHoldAction.NONE) {
                prefs[PREF_SPLIT_HOLD_LEFT] = splitHoldLeft.name
            }
            if (prefs[PREF_SPLIT_HOLD_RIGHT] == null && splitHoldRight != null && splitHoldRight != SplitHoldAction.NONE) {
                prefs[PREF_SPLIT_HOLD_RIGHT] = splitHoldRight.name
            }
        }
    }

    fun isActive(config: Config, nowMinutes: Int = currentMinutesOfDay()): Boolean {
        if (!config.enabled) return false
        val start = config.startMinutes
        val end = config.endMinutes
        return if (start == end) {
            true
        } else if (start < end) {
            nowMinutes in start until end
        } else {
            nowMinutes >= start || nowMinutes < end
        }
    }

    suspend fun isActiveNow(context: Context): Boolean =
        isActive(config(context).first())

    fun currentMinutesOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun formatMinutes(minutes: Int): String {
        val h = (minutes / 60).coerceIn(0, 23)
        val m = (minutes % 60).coerceIn(0, 59)
        return "%02d:%02d".format(h, m)
    }

    suspend fun scheduleAlarms(context: Context) {
        val app = context.applicationContext
        val cfg = config(app).first()
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val startPi = boundaryPendingIntent(app, REQ_START)
        val endPi = boundaryPendingIntent(app, REQ_END)
        am.cancel(startPi)
        am.cancel(endPi)
        if (!cfg.enabled) return
        scheduleNext(am, cfg.startMinutes, startPi)
        scheduleNext(am, cfg.endMinutes, endPi)
    }

    private fun scheduleNext(am: AlarmManager, minutesOfDay: Int, pi: PendingIntent) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            if (timeInMillis <= System.currentTimeMillis() + 2_000) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } else {
            @Suppress("DEPRECATION")
            am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    private fun boundaryPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, GestureQuietHoursReceiver::class.java).setAction(ACTION_BOUNDARY)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}

/** Applies quiet-hours boundaries and re-arms the next alarm. */
class GestureQuietHoursReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                GestureQuietHours.scheduleAlarms(context)
                val app = context.applicationContext as? FreeBuddyApplication
                app?.headphonesRepository?.applyGestureQuietHours()
            } finally {
                pending.finish()
            }
        }
    }
}
