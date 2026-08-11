package com.lastgimbus.the.freebuddy.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val device = FreeBuds5iProtocol.reduce(HeadphonesSnapshot(), command).dualConnectDevices.single()
        assertEquals("Phone", device.name)
        assertEquals("01:23:45:67:89:AB", device.address)
        assertEquals(DualConnectionState.PLAYING, device.connectionState)
        assertEquals(true, device.autoConnect)
        assertEquals(true, device.preferred)
    }

    @Test fun deviceInfoResponseIsParsed() {
        val snapshot = FreeBuds5iProtocol.reduce(
            HeadphonesSnapshot(),
            MbbCommand(
                1,
                7,
                mapOf(
                    3 to "REV1".toByteArray(),
                    7 to "2.1.0.202".toByteArray(),
                    9 to "CASE123".toByteArray(),
                    10 to "Nebula Black".toByteArray(),
                    15 to "HUAWEI FreeBuds 5i".toByteArray(),
                    24 to "L-LEFT123,R-RIGHT456".toByteArray(),
                    2 to byteArrayOf(0x01, 0x45),
                    18 to "COLOR-CANDIDATE".toByteArray(),
                    30 to byteArrayOf(0x01, 0xFF.toByte(), 0x20),
                ),
            ),
        )
        assertEquals("HUAWEI FreeBuds 5i", snapshot.deviceInfo?.model)
        assertEquals("Nebula Black", snapshot.deviceInfo?.subModel)
        assertEquals("2.1.0.202", snapshot.deviceInfo?.firmware)
        assertEquals("CASE123", snapshot.deviceInfo?.serialNumber)
        assertEquals("LEFT123", snapshot.deviceInfo?.leftSerial)
        assertEquals("RIGHT456", snapshot.deviceInfo?.rightSerial)
        assertEquals("0x0145 (\"E\")", snapshot.deviceInfo?.unknownFields?.get(2))
        assertEquals("COLOR-CANDIDATE", snapshot.deviceInfo?.unknownFields?.get(18))
        assertEquals("0x01FF20", snapshot.deviceInfo?.unknownFields?.get(30))
        assertEquals(DeviceColor.BLACK, snapshot.deviceInfo?.detect5iColor())
    }

    @Test fun realFreeBuds5iVariantTlvIdentifiesConfirmedColors() {
        val commonSubModel = "BTFT0014-000145"
        val commonHardware = "HL1OTEM2_VA"
        assertEquals(
            DeviceColor.BLACK,
            DeviceInfo(
                subModel = commonSubModel,
                hardwareVersion = commonHardware,
                unknownFields = mapOf(25 to "0x0C"),
            ).detect5iColor(),
        )
        assertEquals(
            DeviceColor.WHITE,
            DeviceInfo(
                subModel = commonSubModel,
                hardwareVersion = commonHardware,
                unknownFields = mapOf(25 to "0x01"),
            ).detect5iColor(),
        )
        assertEquals(
            DeviceColor.BLUE,
            DeviceInfo(
                subModel = commonSubModel,
                hardwareVersion = commonHardware,
                unknownFields = mapOf(25 to "0x07"),
            ).detect5iColor(),
        )
        assertNull(DeviceInfo(subModel = commonSubModel, hardwareVersion = commonHardware).detect5iColor())
    }

    @Test fun productionDateIsDecodedForCaseAndEarbuds() {
        assertEquals(ProductionDate(2025, 1), "5PUTQ25122006052".productionDate(SerialComponent.CASE))
        assertEquals(ProductionDate(2025, 1), "TQ9084251A000328".productionDate(SerialComponent.EARBUD))
        assertEquals(ProductionDate(2022, 12), "TQ908422C6021664".productionDate(SerialComponent.EARBUD))
        assertEquals(ProductionDate(2024, 12), "2NGTQ24C06012946".productionDate(SerialComponent.CASE))
        assertEquals(ProductionDate(2024, 11), "TQ487024BW006547".productionDate(SerialComponent.EARBUD))
        assertNull("invalid".productionDate(SerialComponent.EARBUD))
    }

    @Test fun partialDeviceInfoResponsesAreMergedWithoutLosingVariant() {
        val withVariant = FreeBuds5iProtocol.reduce(
            HeadphonesSnapshot(),
            MbbCommand(1, 7, mapOf(25 to byteArrayOf(0x01))),
        )
        val completed = FreeBuds5iProtocol.reduce(
            withVariant,
            MbbCommand(1, 7, mapOf(3 to "HL1OTEM2_VA".toByteArray(), 15 to "BTFT0014".toByteArray())),
        )
        assertEquals("0x01", completed.deviceInfo?.unknownFields?.get(25))
        assertEquals(DeviceColor.WHITE, completed.deviceInfo?.detect5iColor())
    }

    @Test fun protocolRegistryMatchesOnlyVerifiedModels() {
        assertEquals(FreeBuds5iProtocol, HeadphonesProtocolRegistry.findByBluetoothName("HUAWEI FreeBuds 5i"))
        assertEquals(FreeBuds6iProtocol, HeadphonesProtocolRegistry.findByBluetoothName("HUAWEI FreeBuds 6i"))
        assertNull(HeadphonesProtocolRegistry.findByBluetoothName("Random headset"))
    }

    @Test fun freeBuds5iCapabilitiesAreExplicit() {
        val features = FreeBuds5iProtocol.capabilities
        assertEquals(true, features.ancLevels)
        assertEquals(true, features.dynamicAnc)
        assertEquals(true, features.dualConnectAutoConnect)
        assertEquals(false, features.customEqualizer)
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
        val battery = FreeBuds5iProtocol.reduce(HeadphonesSnapshot(), command).battery
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
    }

    @Test fun freeBuds6iProfileDeclaresModelDifferences() {
        val features = FreeBuds6iProtocol.capabilities
        assertEquals(true, features.customEqualizer)
        assertEquals(true, features.dualConnect)
        assertEquals(false, features.dualConnectAutoConnect)
        assertEquals(
            listOf(DeviceColor.BLACK, DeviceColor.WHITE, DeviceColor.PURPLE),
            FreeBuds6iProtocol.availableColors,
        )
        assertEquals(
            DeviceColor.BLACK,
            FreeBuds6iProtocol.detectColor(DeviceInfo(unknownFields = mapOf(25 to "0x02"))),
        )
        assertNull(FreeBuds6iProtocol.detectColor(DeviceInfo(unknownFields = mapOf(25 to "0x01"))))
    }

    @Test fun realBlackFreeBuds6iDeviceInfoDetectsBlackColor() {
        val response = MbbCommand.parse(
            "5A0092000107020201530308484C324F52434D5207204861726D6F6E794F5320362E302E302E323932284630303148303033433930290910324E47545132344330363031323934360A0F42544654303031392D3030303135330F08425446543030313918254C2D545134383730323442573030363534372C522D5451343837333234424B3032303030351901021B065E2A00F78A3093C5".hexBytes(),
        )
        val snapshot = FreeBuds6iProtocol.reduce(HeadphonesSnapshot(), response)
        assertEquals("BTFT0019", snapshot.deviceInfo?.model)
        assertEquals("BTFT0019-000153", snapshot.deviceInfo?.subModel)
        assertEquals("0x02", snapshot.deviceInfo?.unknownFields?.get(25))
        assertEquals(DeviceColor.BLACK, FreeBuds6iProtocol.detectColor(snapshot.deviceInfo))
    }

    @Test fun freeBuds6iEqualizerReadRequestsAllKnownFields() {
        val request = FreeBuds6iProtocol.equalizerStateRequest()
        assertEquals(43, request.serviceId)
        assertEquals(74, request.commandId)
        assertEquals((1..8).toSet(), request.args.keys)
        assertEquals(true, request.args.values.all { it.isEmpty() })
    }

    @Test fun freeBuds6iEqualizerDiagnosticsParseBuiltInAndCustomPresets() {
        val custom = ByteArray(36)
        custom[0] = 100.toByte()
        custom[1] = 10
        intArrayOf(-5, 0, 5, 10, 15, 20, 15, 10, 5, 0).forEachIndexed { index, value ->
            custom[2 + index] = value.toByte()
        }
        "My EQ".toByteArray().copyInto(custom, 12)
        val command = MbbCommand(
            43,
            74,
            mapOf(2 to byteArrayOf(100.toByte()), 3 to byteArrayOf(1, 2, 3, 9), 8 to custom),
        )
        val diagnostics = FreeBuds6iProtocol.parseEqualizerDiagnostics(command)
        assertEquals(100, diagnostics.activePresetId)
        assertEquals(listOf(1, 2, 3, 9), diagnostics.builtInPresetIds)
        assertEquals("My EQ", diagnostics.customPresets.single().name)
        assertEquals(listOf(-5, 0, 5, 10, 15, 20, 15, 10, 5, 0), diagnostics.customPresets.single().bands)
        assertEquals("0x01020309", diagnostics.rawFields[3])
    }

    @Test fun diagnosticDumpContainsPacketAndTlvBytes() {
        val line = MbbCommand(43, 74, mapOf(2 to byteArrayOf(1), 8 to byteArrayOf(0xFF.toByte())))
            .toDiagnosticString()
        assertEquals(true, line.startsWith("43/74 packet=5A"))
        assertEquals(true, "tlv[2=01 8=FF]" in line)
    }

    @Test fun holdRequestsAndResponsesMatchOpenFreebudsTraffic() {
        assertEquals(
            "5A0007002B170100020030A7",
            FreeBuds5iProtocol.holdStateRequest().toPayload().toHex(),
        )
        assertEquals(
            "5A0007002B1901000200FF0F",
            FreeBuds5iProtocol.holdModesStateRequest().toPayload().toHex(),
        )

        val response = MbbCommand.parse(
            "5A0015002B19010102020102030A0102030405060708090A7D45".hexBytes(),
        )
        val settings = FreeBuds5iProtocol.reduce(HeadphonesSnapshot(), response).settings
        assertEquals(AncMode.entries.toSet(), settings.holdModes)
    }

    @Test fun holdModeWriteTargetsBothEarbuds() {
        val command = FreeBuds5iProtocol.holdModes(
            setOf(AncMode.OFF, AncMode.TRANSPARENCY),
        )
        assertArrayEquals(byteArrayOf(4), command.args[1])
        assertArrayEquals(byteArrayOf(4), command.args[2])

        val base = FreeBuds5iProtocol.hold(Hold.CYCLE_ANC)
        assertArrayEquals(byteArrayOf(10), base.args[1])
        assertArrayEquals(byteArrayOf(10), base.args[2])
    }

    @Test fun commonReadRequestsMatchOpenfreebudsTraffic() {
        val initialByCommand = FreeBuds5iProtocol.initialRequests.associateBy {
            it.serviceId to it.commandId
        }
        assertEquals(
            "5A0009000108010002000300FBB9",
            initialByCommand.getValue(1 to 8).toPayload().toHex(),
        )
        assertEquals(
            "5A0005002B110100772A",
            FreeBuds5iProtocol.autoPauseStateRequest().toPayload().toHex(),
        )
        assertEquals(
            "5A000700012001000200E897",
            FreeBuds5iProtocol.doubleTapStateRequest().toPayload().toHex(),
        )
        assertEquals(
            "5A0007002B1F01000200328A",
            FreeBuds5iProtocol.swipeStateRequest().toPayload().toHex(),
        )
        assertEquals(
            "5A0005002B6C0200B820",
            FreeBuds5iProtocol.lowLatencyStateRequest().toPayload().toHex(),
        )
    }

    @Test fun commonWritesMatchOpenfreebudsTraffic() {
        assertEquals(
            "5A0006002B10010101A956",
            FreeBuds5iProtocol.autoPause(true).toPayload().toHex(),
        )
        assertEquals(
            "5A000600011F01010203C1",
            FreeBuds5iProtocol.doubleTap(left = DoubleTap.NEXT, right = null).toPayload().toHex(),
        )
        assertEquals(
            "5A0009002B1E0101FF0201FF9D9B",
            FreeBuds5iProtocol.swipe(Swipe.NONE).toPayload().toHex(),
        )
        assertEquals(
            "5A0006002B6C010101A411",
            FreeBuds5iProtocol.lowLatency(true).toPayload().toHex(),
        )
    }

    @Test fun allStructuredReadsDeclareExpectedTlvFields() {
        assertEquals(setOf(1, 2), FreeBuds5iProtocol.ancStateRequest().args.keys)
        assertEquals(setOf(1, 2), FreeBuds5iProtocol.tripleTapStateRequest().args.keys)
        assertEquals((1..8).toSet(), FreeBuds5iProtocol.equalizerStateRequest().args.keys)
        assertEquals(setOf(1), FreeBuds5iProtocol.dualConnectStateRequest().args.keys)
        assertEquals(true, FreeBuds5iProtocol.ancStateRequest().args.values.all { it.isEmpty() })
        assertEquals(true, FreeBuds5iProtocol.equalizerStateRequest().args.values.all { it.isEmpty() })
    }

    @Test fun freeBuds6iCustomEqualizerSaveUsesVerifiedPayload() {
        val command = FreeBuds6iProtocol.customEqualizerSave(
            CustomEqualizerPreset(
                id = 101,
                name = "My sound effect 2",
                bands = List(10) { 60 },
            ),
        )
        assertEquals(43, command.serviceId)
        assertEquals(73, command.commandId)
        assertArrayEquals(intBytes(101), command.args[1])
        assertArrayEquals(intBytes(10), command.args[2])
        assertArrayEquals(ByteArray(10) { 60 }, command.args[3])
        assertArrayEquals("My sound effect 2".toByteArray(), command.args[4])
        assertArrayEquals(intBytes(1), command.args[5])
    }

    @Test fun freeBuds6iCustomEqualizerDeleteUsesActionTwoAndSignedBands() {
        val command = FreeBuds6iProtocol.customEqualizerDelete(
            CustomEqualizerPreset(
                id = 102,
                name = "My sound effect 3",
                bands = List(10) { -60 },
            ),
        )
        assertArrayEquals(ByteArray(10) { (-60).toByte() }, command.args[3])
        assertArrayEquals(intBytes(2), command.args[5])
    }

    @Test(expected = IllegalArgumentException::class)
    fun freeBuds6iCustomEqualizerRejectsOutOfRangeBand() {
        FreeBuds6iProtocol.customEqualizerSave(
            CustomEqualizerPreset(100, "Invalid", List(10) { 65 }),
        )
    }

}

private fun String.hexBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
