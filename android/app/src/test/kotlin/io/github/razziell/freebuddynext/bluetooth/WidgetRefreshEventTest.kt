package io.github.razziell.freebuddynext.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRefreshEventTest {
    private val acl = WidgetRefreshEvent(1, 1, true, "headset-a", 10_000, "acl_connected")
    private val a2dp = acl.next(true, "headset-a", 10_200, "a2dp_connected")

    @Test
    fun pairedEventsKeepFreshnessBoundaryAndDoNotReplaceRunningWork() {
        assertEquals(2L, a2dp.generation)
        assertEquals(acl.cycle, a2dp.cycle)
        assertEquals(acl.sinceMillis, a2dp.sinceMillis)
        assertFalse(a2dp.requiresReplacement(acl, retryPending = false))
    }

    @Test
    fun reversedProfileEventOrderAlsoCoalesces() {
        val first = acl.copy(reason = "a2dp_connected")
        assertEquals(first.cycle, first.next(true, "headset-a", 10_200, "acl_connected").cycle)
    }

    @Test
    fun disconnectAndReconnectReplaceObsoleteCycle() {
        val off = a2dp.next(false, "headset-a", 10_300, "a2dp_disconnected")
        val on = off.next(true, "headset-a", 10_400, "acl_connected")
        assertTrue(off.requiresReplacement(a2dp, false))
        assertTrue(on.requiresReplacement(off, false))
        assertEquals(10_400L, on.sinceMillis)
    }

    @Test
    fun anotherDeviceCannotBeCoalesced() {
        val other = acl.next(true, "headset-b", 10_200, "acl_connected")
        assertTrue(other.requiresReplacement(acl, false))
    }

    @Test
    fun missingDisconnectDoesNotMergeALaterConnection() {
        val later = a2dp.next(true, "headset-a", 13_001, "acl_connected")
        assertTrue(later.requiresReplacement(a2dp, false))
        assertEquals(13_001L, later.sinceMillis)
    }

    @Test
    fun clockRollbackStartsANewCycle() {
        assertTrue(acl.next(true, "headset-a", 9_999, "a2dp_connected")
            .requiresReplacement(acl, false))
    }

    @Test
    fun eventDuringBackoffBypassesOldDelayedRetry() {
        assertTrue(a2dp.requiresReplacement(acl, retryPending = true))
        assertTrue(requireNotNull(acl.completion(acl, retry = true, freshReports = false)).backoff)
        val completion = requireNotNull(acl.completion(a2dp, retry = true, freshReports = false))
        assertFalse(completion.backoff)
        assertNull(completion.completedGeneration) // queued tail must run
    }

    @Test
    fun freshReportsCoverThePairedEventAtCompletionBoundary() {
        val completion = requireNotNull(acl.completion(a2dp, retry = false, freshReports = true))
        assertEquals(a2dp.generation, completion.completedGeneration)
    }

    @Test
    fun idleTimeoutDoesNotConsumeAnA2dpEventArrivingAtCompletionBoundary() {
        val completion = requireNotNull(acl.completion(a2dp, retry = false, freshReports = false))
        assertEquals(acl.generation, completion.completedGeneration)
        assertTrue(requireNotNull(completion.completedGeneration) < a2dp.generation)
    }

    @Test
    fun oldWorkerCannotAcknowledgeOrBackoffANewCycle() {
        val off = a2dp.next(false, "headset-a", 10_300, "a2dp_disconnected")
        assertNull(acl.completion(off, retry = false, freshReports = true))
        assertNull(acl.completion(off, retry = true, freshReports = false))
    }
}
