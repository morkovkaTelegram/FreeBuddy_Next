package io.github.razziell.freebuddynext.protocol

import java.util.UUID

private val FreeBuds5iDualConnect = HuaweiDualConnectModule(supportsAutoConnect = true)

object FreeBuds5iProtocol : HeadphonesProtocol {
    override val id = "freebuds_5i"
    override val displayName = "HUAWEI FreeBuds 5i"
    override val bluetoothNamePrefixes = setOf(displayName)
    override val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    override val availableColors = listOf(DeviceColor.BLACK, DeviceColor.WHITE, DeviceColor.BLUE)
    override val modules: List<ProtocolModule> = listOf(
        HuaweiBatteryModule,
        HuaweiAdvancedAncModule,
        HuaweiAutoPauseModule,
        HuaweiDoubleTapModule,
        HuaweiTripleTapModule,
        HuaweiSplitHoldModule,
        HuaweiSwipeModule,
        HuaweiLowLatencyModule,
        HuaweiBuiltInEqualizerModule,
        HuaweiLdacModule,
        FreeBuds5iDualConnect,
        HuaweiFindEarbudsModule,
        HuaweiDeviceInfoModule,
    )

    override fun detectColor(info: DeviceInfo?) = info?.detect5iColor()
}
