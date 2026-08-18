package io.github.razziell.freebuddynext.protocol

import java.util.UUID

/** HUAWEI FreeBuds SE 4 ANC (Fuji / model id 000169). */
object FreeBudsSe4AncProtocol : HeadphonesProtocol {
    override val id = "freebuds_se_4_anc"
    override val displayName = "HUAWEI FreeBuds SE 4 ANC"
    override val bluetoothNamePrefixes = setOf(displayName)
    override val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    override val availableColors = listOf(DeviceColor.BLACK, DeviceColor.WHITE, DeviceColor.BLUE)
    override val modules: List<ProtocolModule> = listOf(
        HuaweiBatteryModule,
        HuaweiAdvancedAncModule,
        HuaweiDoubleTapModule,
        HuaweiTripleTapModule,
        HuaweiSe4HoldModule,
        HuaweiCustomEqualizerModule,
        HuaweiLowLatencyModule,
        HuaweiDeviceInfoModule,
    )

    /** Empirically verified color code from physical SE 4 ANC units. */
    override fun detectColor(info: DeviceInfo?): DeviceColor? = when (info?.unknownFields?.get(25)?.uppercase()) {
        "0X02" -> DeviceColor.BLACK
        else -> null
    }
}
