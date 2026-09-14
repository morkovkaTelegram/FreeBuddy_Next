package io.github.razziell.freebuddynext.protocol

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStateReportTest {
    @Test
    fun batteryReadAndPushAcceptUnchangedAndUnknownLevels() {
        for (commandId in listOf(8, 39)) {
            assertTrue(HuaweiBatteryModule.isStateReport(MbbCommand(1, commandId, mapOf(
                2 to byteArrayOf(60, 85, 100), 3 to byteArrayOf(0, 0, 0),
            ))))
            assertTrue(HuaweiBatteryModule.isStateReport(MbbCommand(1, commandId, mapOf(
                2 to byteArrayOf(0, 0, 0), 3 to byteArrayOf(0, 0, 0),
            ))))
        }
    }

    @Test
    fun emptyRequestsTruncatedAndUnrelatedPacketsAreNotBatteryReports() {
        assertFalse(HuaweiBatteryModule.isStateReport(HuaweiBatteryModule.initialRequests.single()))
        assertFalse(HuaweiBatteryModule.isStateReport(MbbCommand(1, 8, mapOf(
            2 to byteArrayOf(60, 85), 3 to byteArrayOf(0, 0, 0),
        ))))
        assertFalse(HuaweiBatteryModule.isStateReport(MbbCommand(1, 8, mapOf(
            2 to byteArrayOf(60, 85, 100),
        ))))
        assertFalse(HuaweiBatteryModule.isStateReport(MbbCommand(43, 8, mapOf(
            2 to byteArrayOf(60, 85, 100), 3 to byteArrayOf(0, 0, 0),
        ))))
    }

    @Test
    fun bothAncModulesRecognizeAllValidModes() {
        for (module in listOf(HuaweiBasicAncModule, HuaweiAdvancedAncModule)) {
            for (mode in AncMode.entries) {
                assertTrue(module.isStateReport(MbbCommand(43, 42, mapOf(
                    1 to byteArrayOf(0, mode.code.toByte()),
                ))))
            }
            assertFalse(module.isStateReport(module.stateRequest()))
            assertFalse(module.isStateReport(MbbCommand(43, 42, mapOf(1 to byteArrayOf(0)))))
            assertFalse(module.isStateReport(MbbCommand(43, 42, mapOf(1 to byteArrayOf(0, 99)))))
            assertFalse(module.isStateReport(MbbCommand(43, 4, mapOf(1 to byteArrayOf(0, 1)))))
        }
    }

    @Test
    fun everySupportedProfileProvidesWidgetReportModules() {
        for (id in listOf("freebuds_4i", "freebuds_5i", "freebuds_6i", "freebuds_se_4_anc")) {
            val protocol = requireNotNull(HeadphonesProtocolRegistry.findById(id))
            assertTrue(protocol.module<BatteryModule>() != null)
            assertTrue(protocol.module<AncModule>() != null)
        }
    }
}
