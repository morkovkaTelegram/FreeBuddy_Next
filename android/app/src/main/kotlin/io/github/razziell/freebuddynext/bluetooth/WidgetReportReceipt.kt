package io.github.razziell.freebuddynext.bluetooth

/** Freshness is a received report, not a change in its values or a cached Connected snapshot. */
internal data class WidgetReportReceipt(
    val session: Long = 0,
    val battery: Long = 0,
    val anc: Long = 0,
    val batteryAtMillis: Long = 0,
    val ancAtMillis: Long = 0,
) {
    fun nextSession() = WidgetReportReceipt(session = session + 1)

    fun hasFreshBattery(sinceMillis: Long, nowMillis: Long): Boolean =
        battery > 0 && isFresh(batteryAtMillis, sinceMillis, nowMillis)

    fun hasFreshAnc(sinceMillis: Long, nowMillis: Long): Boolean =
        anc > 0 && isFresh(ancAtMillis, sinceMillis, nowMillis)

    private fun isFresh(at: Long, since: Long, now: Long): Boolean =
        at >= since && now - at in 0..5_000L

    fun completes(previous: WidgetReportReceipt, needsBattery: Boolean, needsAnc: Boolean): Boolean =
        session == previous.session &&
            (!needsBattery || battery > previous.battery) &&
            (!needsAnc || anc > previous.anc)
}
