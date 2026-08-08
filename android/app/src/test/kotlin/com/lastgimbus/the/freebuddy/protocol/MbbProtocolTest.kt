package com.lastgimbus.the.freebuddy.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MbbProtocolTest {
    @Test fun commandRoundTripPreservesIdsAndArguments() {
        val original = MbbCommand(43, 4, mapOf(1 to intBytes(1, 255), 7 to intBytes(0x12)))
        val parsed = MbbCommand.parse(original.toPayload())
        assertEquals(original.serviceId, parsed.serviceId)
        assertEquals(original.commandId, parsed.commandId)
        assertArrayEquals(original.args[1], parsed.args[1])
        assertArrayEquals(original.args[7], parsed.args[7])
    }

    @Test fun parserHandlesFragmentedAndCombinedPackets() {
        val first = MbbCommand(1, 8).toPayload()
        val second = MbbCommand(43, 42, mapOf(1 to intBytes(2, 1))).toPayload()
        val parser = MbbStreamParser()
        assertEquals(0, parser.append(first.copyOfRange(0, 4)).size)
        val result = parser.append(first.copyOfRange(4, first.size) + second)
        assertEquals(listOf(8, 42), result.map { it.commandId })
    }

    @Test fun dualConnectEnumerationUpdatesDeviceList() {
        val command = MbbCommand(
            43,
            49,
            mapOf(
                4 to byteArrayOf(0x01, 0x23, 0x45, 0x67, 0x89.toByte(), 0xAB.toByte()),
                5 to intBytes(9),
                7 to intBytes(1),
                8 to intBytes(1),
                9 to "Phone".toByteArray(),
            ),
        )
        val device = HeadphonesSnapshot().apply(command).dualConnectDevices.single()
        assertEquals("Phone", device.name)
        assertEquals("01:23:45:67:89:AB", device.address)
        assertEquals(DualConnectionState.PLAYING, device.connectionState)
        assertEquals(true, device.autoConnect)
        assertEquals(true, device.preferred)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parserRejectsInvalidChecksum() {
        val payload = MbbCommand(1, 8).toPayload()
        payload[payload.lastIndex] = (payload.last() + 1).toByte()
        MbbCommand.parse(payload)
    }
    @Test fun batteryParserAcceptsOnlyProtocolRange() {
        val command = MbbCommand(
            1,
            8,
            mapOf(
                2 to byteArrayOf(0, 100, 101),
                3 to byteArrayOf(0, 1, 0),
            ),
        )
        val battery = HeadphonesSnapshot().apply(command).battery
        assertEquals(null, battery.left)
        assertEquals(100, battery.right)
        assertEquals(null, battery.case)
        assertEquals(true, battery.chargingRight)
    }

    @Test fun batteryLevelRejectsCorruptUnsignedByte() {
        assertEquals(null, BatteryLevel.fromProtocol(0xFF.toByte()))
        assertEquals(null, BatteryLevel.validOrNull(-1))
        assertEquals(null, BatteryLevel.validOrNull(101))
        assertEquals(1, BatteryLevel.validOrNull(1))
        assertEquals(100, BatteryLevel.validOrNull(100))
        assertEquals(0, BatteryLevel.segments(null))
        assertEquals(0, BatteryLevel.segments(0))
        assertEquals(16, BatteryLevel.segments(80))
        assertEquals(19, BatteryLevel.segments(95))
        assertEquals(20, BatteryLevel.segments(100))
    }

}
