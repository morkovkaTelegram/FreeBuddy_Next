package com.lastgimbus.the.freebuddy.protocol

data class BatteryLevels(
    val left: Int? = null,
    val right: Int? = null,
    val case: Int? = null,
    val chargingLeft: Boolean = false,
    val chargingRight: Boolean = false,
    val chargingCase: Boolean = false,
)

enum class AncMode(val code: Int) { OFF(0), NOISE_CANCELLING(1), TRANSPARENCY(2) }
enum class AncLevel(val code: Int) { NORMAL(0), COMFORT(1), ULTRA(2), DYNAMIC(3) }
enum class DoubleTap(val code: Int) {
    NONE(255), ASSISTANT(0), PLAY_PAUSE(1), NEXT(2), PREVIOUS(7)
}
enum class TripleTap(val code: Int) { NONE(255), NEXT(2), PREVIOUS(7) }
enum class Hold(val code: Int) { NONE(255), CYCLE_ANC(10) }
enum class Swipe(val code: Int) { NONE(255), VOLUME(0) }
enum class EqPreset(val code: Int) { DEFAULT(1), BASS(2), TREBLE(3), VOICES(9) }
enum class DualConnectionState { DISCONNECTED, CONNECTED, PLAYING }

data class ProductionDate(val year: Int, val month: Int)

enum class SerialComponent { CASE, EARBUD }

/**
 * Best-effort manufacturing date encoded in Huawei serial numbers.
 * Case serials use positions 5..6 for year and 7 for month; earbud serials
 * use positions 6..7 and 8 respectively (zero-based indexes).
 */
fun String.productionDate(component: SerialComponent): ProductionDate? {
    val yearStart = if (component == SerialComponent.CASE) 5 else 6
    val monthIndex = yearStart + 2
    if (length <= monthIndex) return null
    val year = substring(yearStart, yearStart + 2).toIntOrNull() ?: return null
    val month = when (this[monthIndex].uppercaseChar()) {
        in '1'..'9' -> this[monthIndex].digitToInt()
        'A' -> 10
        'B' -> 11
        'C' -> 12
        else -> return null
    }
    return ProductionDate(year = 2000 + year, month = month)
}

data class DeviceInfo(
    val model: String? = null,
    val subModel: String? = null,
    val firmware: String? = null,
    val serialNumber: String? = null,
    val hardwareVersion: String? = null,
    val leftSerial: String? = null,
    val rightSerial: String? = null,
    /** Unrecognised non-empty MBB 1/7 TLVs, rendered as printable text or uppercase HEX. */
    val unknownFields: Map<Int, String> = emptyMap(),
)

/** Retail colors supported by model profiles. */
enum class DeviceColor { BLACK, WHITE, BLUE, PURPLE }

/**
 * Best-effort color detection from MBB 1/7 device information.
 *
 * Confirmed retail samples use TLV 25 as the variant identifier:
 * 0x01 = Ceramic White, 0x07 = Isle Blue, 0x0C = Nebula Black. TLV 10
 * (sub-model) and TLV 3
 * (hardware version) can be identical across colors, so TLV 25 has priority.
 * Textual names are retained as a fallback for future firmware variants.
 */
fun DeviceInfo.detect5iColor(): DeviceColor? {
    val variant = unknownFields[25]?.uppercase()
    val sub = subModel?.uppercase()
    return when {
        variant == "0X01" -> DeviceColor.WHITE
        variant == "0X07" -> DeviceColor.BLUE
        variant == "0X0C" -> DeviceColor.BLACK
        sub?.contains("BLACK") == true -> DeviceColor.BLACK
        sub?.contains("WHITE") == true -> DeviceColor.WHITE
        sub?.contains("BLUE") == true -> DeviceColor.BLUE
        else -> null
    }
}

data class DualConnectDevice(
    val name: String,
    val address: String,
    val autoConnect: Boolean,
    val preferred: Boolean,
    val connectionState: DualConnectionState,
)

data class CustomEqualizerPreset(
    val id: Int,
    val name: String,
    val bands: List<Int>,
)

data class EqualizerDiagnostics(
    val activePresetId: Int? = null,
    val builtInPresetIds: List<Int> = emptyList(),
    val customPresets: List<CustomEqualizerPreset> = emptyList(),
    val rawFields: Map<Int, String> = emptyMap(),
)

data class FreeBudsSettings(
    val doubleTapLeft: DoubleTap? = null,
    val doubleTapRight: DoubleTap? = null,
    val tripleTapLeft: TripleTap? = null,
    val tripleTapRight: TripleTap? = null,
    val hold: Hold? = null,
    val holdModes: Set<AncMode>? = null,
    val swipe: Swipe? = null,
    val autoPause: Boolean? = null,
    val lowLatency: Boolean? = null,
    val eqPreset: EqPreset? = null,
    val ldac: Boolean? = null,
)

data class HeadphonesSnapshot(
    val protocolId: String = "",
    val name: String = "FreeBuds",
    val address: String = "",
    val capabilities: HeadphonesCapabilities = HeadphonesCapabilities(),
    val battery: BatteryLevels = BatteryLevels(),
    val ancMode: AncMode? = null,
    val ancLevel: AncLevel? = null,
    val settings: FreeBudsSettings = FreeBudsSettings(),
    val dualConnectEnabled: Boolean? = null,
    val dualConnectDevices: List<DualConnectDevice> = emptyList(),
    val deviceInfo: DeviceInfo? = null,
    /** FreeBuds 6i MBB 43/74 diagnostics; null for models without custom EQ. */
    val equalizerDiagnostics: EqualizerDiagnostics? = null,
)
