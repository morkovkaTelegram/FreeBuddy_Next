package io.github.razziell.freebuddynext.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BluetoothCodecNamesTest {

    @Test
    fun mapsKnownAospCodecTypes() {
        assertEquals("SBC", BluetoothCodecNames.nameFor(0))
        assertEquals("AAC", BluetoothCodecNames.nameFor(1))
        assertEquals("aptX", BluetoothCodecNames.nameFor(2))
        assertEquals("aptX HD", BluetoothCodecNames.nameFor(3))
        assertEquals("LDAC", BluetoothCodecNames.nameFor(4))
        assertEquals("LC3", BluetoothCodecNames.nameFor(5))
        assertEquals("Opus", BluetoothCodecNames.nameFor(6))
    }

    @Test
    fun unknownTypeHidesBadge() {
        assertNull(BluetoothCodecNames.nameFor(-1))
        assertNull(BluetoothCodecNames.nameFor(7))
        assertNull(BluetoothCodecNames.nameFor(1000))
    }
}
