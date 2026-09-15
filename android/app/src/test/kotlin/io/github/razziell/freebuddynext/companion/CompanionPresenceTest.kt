package io.github.razziell.freebuddynext.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionPresenceTest {
    private val first = "00:11:22:33:44:55"
    private val second = "AA:BB:CC:DD:EE:FF"

    @Test fun startsAbsent() { assertFalse(CompanionPresence().anyPresent) }

    @Test fun canonicalizesAddress() {
        assertEquals(second, companionAddress("aa:bb:cc:dd:ee:ff"))
    }

    @Test fun rejectsInvalidAddresses() {
        for (value in listOf(null, "", "headphones", "AA:BB:CC:DD:EE", "GG:11:22:33:44:55"))
            assertNull(companionAddress(value))
    }

    @Test fun classicConnectAndDisconnect() {
        val state = CompanionPresence()
        state.update(first, CompanionPresence.Source.BLUETOOTH, true)
        assertTrue(state.anyPresent)
        state.update(first, CompanionPresence.Source.BLUETOOTH, false)
        assertFalse(state.anyPresent)
    }

    @Test fun bleDisappearanceDoesNotClearClassicConnection() {
        val state = CompanionPresence()
        state.update(first, CompanionPresence.Source.BLUETOOTH, true)
        state.update(first, CompanionPresence.Source.BLE, true)
        state.update(first, CompanionPresence.Source.BLE, false)
        assertTrue(state.anyPresent)
    }

    @Test fun classicDisconnectDoesNotClearBlePresence() {
        val state = CompanionPresence()
        state.update(first, CompanionPresence.Source.BLE, true)
        state.update(first, CompanionPresence.Source.BLUETOOTH, true)
        state.update(first, CompanionPresence.Source.BLUETOOTH, false)
        assertTrue(state.anyPresent)
    }

    @Test fun duplicateAppearanceDoesNotRequireTwoDisappearances() {
        val state = CompanionPresence()
        repeat(2) { state.update(first, CompanionPresence.Source.LEGACY, true) }
        state.update(first, CompanionPresence.Source.LEGACY, false)
        assertFalse(state.anyPresent)
    }

    @Test fun removingOneDeviceLeavesOtherPresent() {
        val state = CompanionPresence()
        state.update(first, CompanionPresence.Source.LEGACY, true)
        state.update(second, CompanionPresence.Source.LEGACY, true)
        state.update(first, CompanionPresence.Source.LEGACY, false)
        assertTrue(state.anyPresent)
        state.update(second.lowercase(), CompanionPresence.Source.LEGACY, false)
        assertFalse(state.anyPresent)
    }

    @Test fun revokedAssociationIsDropped() {
        val state = CompanionPresence()
        state.update(first, CompanionPresence.Source.LEGACY, true)
        state.retain(emptySet())
        assertFalse(state.anyPresent)
    }

    @Test fun enabledAddressesAreComparedCaseInsensitively() {
        val state = CompanionPresence()
        state.update(second, CompanionPresence.Source.BLUETOOTH, true)
        state.retain(setOf(second.lowercase()))
        assertTrue(state.anyPresent)
    }

    @Test fun unknownDisappearanceIsHarmless() {
        val state = CompanionPresence()
        state.update(first, CompanionPresence.Source.LEGACY, false)
        state.update("bad", CompanionPresence.Source.LEGACY, true)
        assertFalse(state.anyPresent)
    }
}
