package io.github.razziell.freebuddynext.bluetooth

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.razziell.freebuddynext.FreeBuddyApplication
import io.github.razziell.freebuddynext.widget.WidgetStorage
import kotlinx.coroutines.CancellationException

/** Bounded, event-driven refresh. Not a permanent Bluetooth service or periodic radio poll. */
class WidgetRefreshWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        var event: WidgetRefreshEvent? = null
        try {
            val pending = WidgetRefreshScheduler.pending(context) ?: return Result.success()
            event = pending
            if (!WidgetStorage.hasAnyWidget(context)) {
                WidgetRefreshScheduler.finish(context, pending, retry = false)
                return Result.success()
            }
            BackgroundDiagnostics.record(context,
                "refresh start g=${pending.generation} cycle=${pending.cycle} reason=${pending.reason} attempt=$runAttemptCount")
            val repository = (context as FreeBuddyApplication).headphonesRepository
            val outcome = repository.refreshForWidgets(pending)
            // An acknowledged revision needs no second publication. Failed/interrupted
            // submissions remain dirty even when the fresh battery values are identical.
            WidgetStorage.publishAll(context)
            val retry = WidgetRefreshScheduler.finish(context, pending,
                retry = outcome == WidgetRefreshOutcome.RETRY && runAttemptCount < MAX_RETRIES,
                freshReports = outcome == WidgetRefreshOutcome.UPDATED)
            BackgroundDiagnostics.record(context,
                "refresh end g=${pending.generation} outcome=$outcome backoff=$retry")
            return if (retry) Result.retry() else Result.success()
        } catch (cancel: CancellationException) {
            BackgroundDiagnostics.record(context, "refresh cancelled g=${event?.generation}")
            throw cancel
        } catch (error: Exception) {
            BackgroundDiagnostics.record(context, "refresh error g=${event?.generation} type=${error.javaClass.simpleName}")
            val pending = event ?: return Result.failure()
            return if (WidgetRefreshScheduler.finish(context, pending, retry = runAttemptCount < MAX_RETRIES)) {
                Result.retry()
            } else {
                // A failed prerequisite would cancel an already appended newer event.
                // The trace records the error; leave the successor free to repair it.
                Result.success()
            }
        }
    }

    private companion object {
        const val MAX_RETRIES = 2
    }
}

enum class WidgetRefreshOutcome { UPDATED, IDLE, RETRY }
