package io.github.razziell.freebuddynext.bluetooth

import android.content.Context
import android.util.Log
import java.time.Instant

/** Small persistent lifecycle trace, deliberately excluding addresses, names and MBB payloads. */
object BackgroundDiagnostics {
    private const val FILE = "background_diagnostics"
    private const val KEY = "events"
    private const val LIMIT = 64

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
