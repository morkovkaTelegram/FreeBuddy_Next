package io.github.razziell.freebuddynext.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPublicationPlanTest {
    @Test
    fun batteryOnlyDoesNotRedrawAnc() {
        val plan = WidgetPublicationPlan.create(11, 10, 11, 9, 1)
        assertTrue(plan.battery)
        assertFalse(plan.anc)
    }

    @Test
    fun ancOnlyDoesNotRedrawBatteryProviders() {
        val plan = WidgetPublicationPlan.create(11, 10, 9, 11, 1)
        assertFalse(plan.battery)
        assertTrue(plan.anc)
    }

    @Test
    fun appearanceChangeRefreshesEveryProvider() {
        val plan = WidgetPublicationPlan.create(11, 10, 9, 9, 11)
        assertTrue(plan.battery)
        assertTrue(plan.anc)
    }

    @Test
    fun adjacentReportsCanBePublishedTogether() {
        val plan = WidgetPublicationPlan.create(12, 10, 11, 12, 1)
        assertTrue(plan.battery)
        assertTrue(plan.anc)
    }

    @Test
    fun acknowledgedRevisionHasNoWork() {
        val plan = WidgetPublicationPlan.create(12, 12, 11, 12, 1)
        assertFalse(plan.battery)
        assertFalse(plan.anc)
    }

    @Test
    fun interruptedPublicationKeepsBothGroupsDirty() {
        // Even if one provider succeeded, global acknowledgement advances only when all do.
        val retry = WidgetPublicationPlan.create(12, 10, 11, 12, 1)
        assertTrue(retry.battery)
        assertTrue(retry.anc)
    }

    @Test
    fun updateArrivingDuringPublicationRemainsDirty() {
        val first = WidgetPublicationPlan.create(11, 10, 11, 9, 1)
        val next = WidgetPublicationPlan.create(12, first.revision, 11, 12, 1)
        assertFalse(next.battery)
        assertTrue(next.anc)
    }

    @Test
    fun pendingLegacyRevisionRepairsAllProviders() {
        // Missing appearance revision is treated as the current revision once, during migration.
        val plan = WidgetPublicationPlan.create(12, 10, 0, 0, 12)
        assertTrue(plan.battery)
        assertTrue(plan.anc)
    }
}
