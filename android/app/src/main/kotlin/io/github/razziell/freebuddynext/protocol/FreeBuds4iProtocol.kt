package io.github.razziell.freebuddynext.protocol

import java.util.UUID

/**
 * HUAWEI FreeBuds 4i profile built only from features verified by OpenFreebuds.
 * TLV 25 value 0x01 is confirmed by a real white FreeBuds 4i pair.
 */

private fun detect4iColor(info: DeviceInfo?): DeviceColor? = when (
    info?.unknownFields?.get(25)?.uppercase()
) {
    "0X01" -> DeviceColor.WHITE
    else -> null
}

object FreeBuds4iProtocol : HeadphonesProtocol {
    override val id = "freebuds_4i"
    override val displayName = "HUAWEI FreeBuds 4i"
    override val bluetoothNamePrefixes = setOf(displayName)
    override val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    override val availableColors = listOf(DeviceColor.BLACK, DeviceColor.WHITE, DeviceColor.RED)
    override val modules: List<ProtocolModule> = listOf(
        HuaweiBatteryModule,
        HuaweiBasicAncModule,
        HuaweiAutoPauseModule,
        HuaweiDoubleTapModule,
        Huawei4iHoldModule,
        HuaweiDeviceInfoModule,
    )

    override fun detectColor(info: DeviceInfo?): DeviceColor? = detect4iColor(info)
}
