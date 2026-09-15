package io.github.razziell.freebuddynext.bluetooth

import android.content.Context
import android.app.ActivityManager
import android.os.Build
import android.os.Process
import io.github.razziell.freebuddynext.BuildConfig
import android.util.Log
import java.time.Instant

/** Small persistent lifecycle trace, deliberately excluding addresses, names and MBB payloads. */
object BackgroundDiagnostics {
    private const val FILE = "background_diagnostics"
    private const val KEY = "events"
    private const val LIMIT = 64

    fun recordProcessStart(context: Context) {
        record(context, "process start pid=${Process.myPid()} api=${Build.VERSION.SDK_INT} version=${BuildConfig.VERSION_CODE}")
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                context.getSystemService(ActivityManager::class.java)
                    .getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull()?.let {
                        record(context, "previous exit time=${Instant.ofEpochMilli(it.timestamp)} reason=${it.reason} status=${it.status} importance=${it.importance}")
                    }
            } catch (error: Exception) {
                record(context, "previous exit unavailable=${error.javaClass.simpleName}")
            }
        }
    }

    @Synchronized
    fun record(context: Context, event: String) {
        val line = "${Instant.now()} $event"
        Log.i("FreeBuddyBackground", event)
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val lines = prefs.getString(KEY, "").orEmpty().lineSequence()
            .filter(String::isNotBlank).toList().takeLast(LIMIT - 1) + line
        prefs.edit().putString(KEY, lines.joinToString("\n")).apply()
    }

    @Synchronized
    fun read(context: Context): List<String> =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, "").orEmpty().lines().filter(String::isNotBlank)
}
