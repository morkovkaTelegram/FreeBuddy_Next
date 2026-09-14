package io.github.razziell.freebuddynext.bluetooth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.await
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Persists the latest event before enqueueing. A same-cycle tail closes the worker
 * completion race without cancelling its Bluetooth operation. New cycles and events
 * arriving during backoff REPLACE, so a delayed retry cannot block a real connection.
 * All access is serialized, including the retry/completion decision.
 */
internal object WidgetRefreshScheduler {
    private const val FILE = "widget_refresh_events"
    private const val WORK_NAME = "bluetooth-widget-refresh"
    private val mutex = Mutex()
    private val arrivalOrder = AtomicLong()
    private var lastAcceptedOrder = 0L

    // Capture on the receiver thread, before dispatching to IO; coroutines may start out of order.
    fun nextArrivalOrder(): Long = arrivalOrder.incrementAndGet()

    suspend fun enqueue(context: Context, connecting: Boolean, address: String?, reason: String,
        order: Long, receivedAtMillis: Long) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (order <= lastAcceptedOrder) return@withLock
                val prefs = preferences(context)
                val previous = readEvent(prefs)
                val now = receivedAtMillis
                val event = previous?.next(connecting, address, now, reason)
                    ?: WidgetRefreshEvent(1, 1, connecting, address, now, reason)
                val replace = event.requiresReplacement(previous, prefs.getBoolean("retry_pending", false))
                val editor = prefs.edit()
                    .putLong("generation", event.generation)
                    .putLong("cycle", event.cycle)
                    .putBoolean("connecting", event.connecting)
                    .putString("address", event.address)
                    .putLong("since", event.sinceMillis)
                    .putString("reason", event.reason)
                check(editor.commit()) { "Could not persist Bluetooth event" }
                val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                    .apply {
                        // Older Android requires an FGS for expedited work.
                        if (Build.VERSION.SDK_INT >= 31) {
                            setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        }
                    }.build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME,
                    if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request,
                ).await()
                lastAcceptedOrder = order
                BackgroundDiagnostics.record(context,
                    "event g=${event.generation} cycle=${event.cycle} $reason ${if (replace) "replace" else "coalesce"}")
            }
        }

    /** A tail whose generation was already covered performs no Bluetooth work. */
    suspend fun pending(context: Context): WidgetRefreshEvent? = mutex.withLock {
        val prefs = preferences(context)
        val event = readEvent(prefs) ?: return@withLock null
        if (prefs.getLong("completed_generation", 0) >= event.generation) return@withLock null
        prefs.edit().putBoolean("retry_pending", false).apply()
        event
    }

    /** Return true only when this still-current cycle should use WorkManager backoff. */
    suspend fun finish(context: Context, event: WidgetRefreshEvent, retry: Boolean,
        freshReports: Boolean = false): Boolean = mutex.withLock {
        val prefs = preferences(context)
        val latest = readEvent(prefs) ?: return@withLock false
        val completion = event.completion(latest, retry, freshReports) ?: return@withLock false
        val editor = prefs.edit().putBoolean("retry_pending", completion.backoff)
        completion.completedGeneration?.let { editor.putLong("completed_generation", it) }
        check(editor.commit())
        completion.backoff
    }

    private fun preferences(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun readEvent(prefs: SharedPreferences): WidgetRefreshEvent? {
        val generation = prefs.getLong("generation", 0)
        if (generation == 0L) return null
        return WidgetRefreshEvent(
            generation, prefs.getLong("cycle", generation), prefs.getBoolean("connecting", true),
            prefs.getString("address", null), prefs.getLong("since", 0),
            prefs.getString("reason", "unknown").orEmpty(),
        )
    }
}
