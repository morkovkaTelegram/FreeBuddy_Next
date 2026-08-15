package io.github.razziell.freebuddynext.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeStoreTest {
    @Test
    fun `different physical sets use isolated serial scopes`() {
        val white = serialScope("freebuds_5i", "5PUTQ25709003227")
        val black = serialScope("freebuds_5i", "5PUTQ25122006052")

        assertNotEquals(white, black)
        assertNotEquals(
            headphonesColorPreferenceName(white),
            headphonesColorPreferenceName(black),
        )
    }

    @Test
    fun `same case serial has same scope regardless of mac`() {
        assertEquals(
            serialScope("freebuds_5i", "5PUTQ25709003227"),
            serialScope("freebuds_5i", "5PUTQ25709003227"),
        )
        assertNotEquals(
            macScope("freebuds_5i", "C8:BC:9C:AF:9B:79"),
            macScope("freebuds_5i", "AA:BB:CC:DD:EE:FF"),
        )
    }

    @Test
    fun `models are isolated even for equal serial text`() {
        assertNotEquals(
            serialScope("freebuds_5i", "SERIAL-1"),
            serialScope("freebuds_6i", "SERIAL-1"),
        )
    }

    @Test
    fun `mac to serial mapping key is stable and sanitized`() {
        assertEquals(
            "headphones_serial_freebuds_5i_c8_bc_9c_af_9b_79",
            macSerialPreferenceName("freebuds_5i", "C8:BC:9C:AF:9B:79"),
        )
    }
}
