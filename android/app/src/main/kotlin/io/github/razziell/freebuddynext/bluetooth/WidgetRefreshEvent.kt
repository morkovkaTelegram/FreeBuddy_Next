package io.github.razziell.freebuddynext.bluetooth

/** A generation identifies an event; a cycle groups the nearby ACL/A2DP pair. */
internal data class WidgetRefreshEvent(
    val generation: Long,
    val cycle: Long,
    val connecting: Boolean,
    val address: String?,
    val sinceMillis: Long,
    val reason: String,
) {
    fun next(connecting: Boolean, address: String?, nowMillis: Long, reason: String): WidgetRefreshEvent {
        val nextGeneration = generation + 1
        // Do not group different headsets, direction changes, or a later reconnect
        // whose disconnect broadcast was missed. The window is anchored, not sliding.
        val sameCycle = this.connecting == connecting && this.address == address &&
            nowMillis - sinceMillis in 0..COALESCE_WINDOW_MS
        return WidgetRefreshEvent(
            generation = nextGeneration,
            cycle = if (sameCycle) cycle else nextGeneration,
            connecting = connecting,
            address = address,
            sinceMillis = if (sameCycle) sinceMillis else nowMillis,
            reason = reason,
        )
    }

    fun requiresReplacement(previous: WidgetRefreshEvent?, retryPending: Boolean): Boolean =
        previous?.cycle != cycle || retryPending

    fun completion(latest: WidgetRefreshEvent, retry: Boolean, freshReports: Boolean): WidgetRefreshCompletion? {
        if (latest.cycle != cycle) return null
        return if (retry) {
            WidgetRefreshCompletion(backoff = latest.generation == generation)
        } else {
            // IDLE must not consume an event that arrived at the timeout boundary.
            WidgetRefreshCompletion(completedGeneration = if (freshReports) latest.generation else generation)
        }
    }

    companion object {
        private const val COALESCE_WINDOW_MS = 3_000L
    }
}

internal data class WidgetRefreshCompletion(
    val backoff: Boolean = false,
    val completedGeneration: Long? = null,
)
