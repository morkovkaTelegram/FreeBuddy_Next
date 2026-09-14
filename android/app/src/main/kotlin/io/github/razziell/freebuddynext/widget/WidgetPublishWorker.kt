package io.github.razziell.freebuddynext.widget

import android.content.Context
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import io.github.razziell.freebuddynext.bluetooth.BackgroundDiagnostics
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Publishes persisted state without opening a new Bluetooth connection itself. */
class WidgetPublishWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        if (WidgetStorage.hasPendingPublication(applicationContext)) {
            delay(75) // Batch the connection's adjacent battery and ANC reports.
            WidgetStorage.publishAll(applicationContext)
        }
        Result.success()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        BackgroundDiagnostics.record(applicationContext, "publish error=${error.javaClass.simpleName}")
        // The dirty revision remains after exhaustion; another report or process start repairs it.
        // Do not cancel already appended updates when this attempt exhausts its retries.
        if (runAttemptCount < 2) Result.retry() else Result.success()
    }

    companion object {
        suspend fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetPublishWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .apply {
                    if (Build.VERSION.SDK_INT >= 31) {
                        setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    }
                }.build()
            // Do not KEEP: an event arriving just as a running publisher finishes would
            // otherwise be lost. Do not REPLACE: let an in-flight Glance update finish.
            WorkManager.getInstance(context).enqueueUniqueWork(
                "widget-state-publication", ExistingWorkPolicy.APPEND_OR_REPLACE, request,
            ).await()
        }
    }
}
