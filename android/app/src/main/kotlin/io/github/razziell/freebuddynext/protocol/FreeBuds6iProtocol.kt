package io.github.razziell.freebuddynext.protocol

import java.util.UUID

private val FreeBuds6iDualConnect = HuaweiDualConnectModule(supportsAutoConnect = false)

object FreeBuds6iProtocol : HeadphonesProtocol {
    override val id = "freebuds_6i"
    override val displayName = "HUAWEI FreeBuds 6i"
    override val bluetoothNamePrefixes = setOf(displayName)
    override val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    override val availableColors = listOf(DeviceColor.BLACK, DeviceColor.WHITE, DeviceColor.PURPLE)
    override val modules: List<ProtocolModule> = listOf(
        HuaweiBatteryModule,
        HuaweiAdvancedAncModule,
        HuaweiAutoPauseModule,
        HuaweiDoubleTapModule,
        HuaweiTripleTapModule,
        HuaweiSplitHoldModule,
        HuaweiSwipeModule,
        HuaweiLowLatencyModule,
        HuaweiCustomEqualizerModule,
        HuaweiLdacModule,
        FreeBuds6iDualConnect,
        HuaweiDeviceInfoModule,
    )

    override fun detectColor(info: DeviceInfo?): DeviceColor? {
        when (info?.unknownFields?.get(25)?.uppercase()) {
            "0X01" -> return DeviceColor.WHITE
            "0X02" -> return DeviceColor.BLACK
        }
        val text = listOfNotNull(info?.subModel, info?.hardwareVersion).joinToString(" ").uppercase()
        return when {
            "PURPLE" in text || "VIOLET" in text -> DeviceColor.PURPLE
            "WHITE" in text -> DeviceColor.WHITE
            "BLACK" in text -> DeviceColor.BLACK
            else -> null
        }
    }
}
