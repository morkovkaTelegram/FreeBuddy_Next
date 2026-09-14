package io.github.razziell.freebuddynext.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetReportReceiptTest {
    @Test
    fun cachedReportsDoNotCompleteRefresh() {
        val baseline = WidgetReportReceipt(session = 7, battery = 3, anc = 4)
        assertFalse(baseline.completes(baseline, needsBattery = true, needsAnc = true))
    }

    @Test
    fun waitsForBothReportsInEitherOrder() {
        val baseline = WidgetReportReceipt(session = 7, battery = 3, anc = 4)
        assertFalse(baseline.copy(battery = 4).completes(baseline, true, true))
        assertFalse(baseline.copy(anc = 5).completes(baseline, true, true))
        assertTrue(baseline.copy(battery = 4, anc = 5).completes(baseline, true, true))
    }

    @Test
    fun unchangedValuesStillCountAsFreshReports() {
        // Receipt counters advance on a valid report, independently of snapshot equality.
        val baseline = WidgetReportReceipt(session = 7, battery = 3, anc = 4)
        assertTrue(WidgetReportReceipt(7, 4, 5).completes(baseline, true, true))
    }

    @Test
    fun anotherConnectionCannotCompleteOldRequest() {
        val baseline = WidgetReportReceipt(session = 7)
        assertFalse(WidgetReportReceipt(8, 10, 10).completes(baseline, true, true))
        assertFalse(WidgetReportReceipt(6, 10, 10).completes(baseline, true, true))
    }

    @Test
    fun reconnectAndDisconnectInvalidatePreviousReports() {
        val baseline = WidgetReportReceipt(session = 7, battery = 3, anc = 4)
        assertEquals(WidgetReportReceipt(session = 8), baseline.nextSession())
        assertFalse(baseline.nextSession().completes(baseline, true, true))
    }

    @Test
    fun initialReportsAfterConnectionEventAreReusable() {
        val reports = WidgetReportReceipt(7, 1, 1, batteryAtMillis = 10_100, ancAtMillis = 10_200)
        assertTrue(reports.hasFreshBattery(sinceMillis = 10_000, nowMillis = 10_300))
        assertTrue(reports.hasFreshAnc(sinceMillis = 10_000, nowMillis = 10_300))
    }

    @Test
    fun reportsFromBeforeEventAreNotReused() {
        val reports = WidgetReportReceipt(7, 1, 1, batteryAtMillis = 9_900, ancAtMillis = 9_950)
        assertFalse(reports.hasFreshBattery(10_000, 10_300))
        assertFalse(reports.hasFreshAnc(10_000, 10_300))
    }

    @Test
    fun aDelayedWorkerMustRequestNewReports() {
        val reports = WidgetReportReceipt(7, 1, 1, batteryAtMillis = 10_100, ancAtMillis = 10_200)
        assertFalse(reports.hasFreshBattery(10_000, 20_000))
        assertFalse(reports.hasFreshAnc(10_000, 20_000))
    }

    @Test
    fun requestOnlyTheMissingReport() {
        val reports = WidgetReportReceipt(7, 1, 0, batteryAtMillis = 10_100)
        assertTrue(reports.hasFreshBattery(10_000, 10_300))
        assertFalse(reports.hasFreshAnc(10_000, 10_300))
        assertTrue(reports.copy(anc = 1, ancAtMillis = 10_400).completes(reports, false, true))
    }

    @Test
    fun newSessionDropsReportTimestamps() {
        val reports = WidgetReportReceipt(7, 1, 1, batteryAtMillis = 10_100, ancAtMillis = 10_200).nextSession()
        assertFalse(reports.hasFreshBattery(10_000, 10_300))
        assertFalse(reports.hasFreshAnc(10_000, 10_300))
    }

    @Test
    fun clockRollbackDoesNotReuseFutureDatedReports() {
        val reports = WidgetReportReceipt(7, 1, 1, batteryAtMillis = 10_100, ancAtMillis = 10_200)
        assertFalse(reports.hasFreshBattery(9_000, 9_500))
        assertFalse(reports.hasFreshAnc(9_000, 9_500))
        // Explicit reads still use counters, so a clock adjustment cannot prevent completion.
        assertTrue(reports.copy(battery = 2, anc = 2).completes(reports, true, true))
    }

    @Test
    fun waitsOnlyForSupportedModules() {
        val baseline = WidgetReportReceipt(session = 7)
        assertTrue(baseline.copy(battery = 1).completes(baseline, true, false))
        assertTrue(baseline.copy(anc = 1).completes(baseline, false, true))
        assertTrue(baseline.completes(baseline, false, false))
    }
}
