package io.github.razziell.freebuddynext.protocol

import java.util.UUID

/** Features that may differ between Huawei models. */
data class HeadphonesCapabilities(
    val ancLevels: Boolean = false,
    val dynamicAnc: Boolean = false,
    val doubleTap: Boolean = false,
    val tripleTap: Boolean = false,
    val splitHold: Boolean = false,
    val swipe: Boolean = false,
    val autoPause: Boolean = false,
    val lowLatency: Boolean = false,
    val ldac: Boolean = false,
    val equalizer: Boolean = false,
    val customEqualizer: Boolean = false,
    val dualConnect: Boolean = false,
    val dualConnectAutoConnect: Boolean = false,
    val deviceInfo: Boolean = false,
)

/**
 * Model-specific Huawei MBB protocol profile.
 *
 * A new model must be implemented here and explicitly registered only after
 * its command set has been verified on real hardware. The repository never
 * sends commands from one model profile to another.
 */
interface HeadphonesProtocol {
    val id: String
    val displayName: String
    val bluetoothNamePrefixes: Set<String>
    val sppUuid: UUID
    val capabilities: HeadphonesCapabilities
    val availableColors: List<DeviceColor>
    val initialRequests: List<MbbCommand>

    /** Model-specific color detection; must not reuse another model's undocumented TLVs. */
    fun detectColor(info: DeviceInfo?): DeviceColor?

    fun matchesBluetoothName(name: String?): Boolean =
        name != null && bluetoothNamePrefixes.any(name::startsWith)

    fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand): HeadphonesSnapshot
    fun isDualConnectChangeEvent(command: MbbCommand): Boolean

    fun ancMode(mode: AncMode): MbbCommand
    fun ancLevel(mode: AncMode, level: AncLevel): MbbCommand
    fun equalizer(value: EqPreset): MbbCommand
    fun customEqualizerSave(preset: CustomEqualizerPreset): MbbCommand =
        error("Custom equalizer is not supported by $displayName")
    fun customEqualizerDelete(preset: CustomEqualizerPreset): MbbCommand =
        error("Custom equalizer is not supported by $displayName")
    fun autoPause(enabled: Boolean): MbbCommand
    fun lowLatency(enabled: Boolean): MbbCommand
    fun ldac(enabled: Boolean): MbbCommand
    fun dualConnect(enabled: Boolean): MbbCommand
    fun dualConnectEnumeration(): MbbCommand
    fun dualConnectPreferred(device: DualConnectDevice, enabled: Boolean): MbbCommand
    fun dualConnectAction(device: DualConnectDevice, action: Int): MbbCommand
    fun doubleTap(left: DoubleTap? = null, right: DoubleTap? = null): MbbCommand
    fun tripleTap(left: TripleTap? = null, right: TripleTap? = null): MbbCommand
    fun hold(value: Hold): MbbCommand
    fun holdModes(modes: Set<AncMode>): MbbCommand
    fun swipe(value: Swipe): MbbCommand

    fun ancStateRequest(): MbbCommand
    fun equalizerStateRequest(): MbbCommand
    fun autoPauseStateRequest(): MbbCommand
    fun lowLatencyStateRequest(): MbbCommand
    fun ldacStateRequest(): MbbCommand
    fun dualConnectStateRequest(): MbbCommand
    fun doubleTapStateRequest(): MbbCommand
    fun tripleTapStateRequest(): MbbCommand
    fun holdStateRequest(): MbbCommand
    fun holdModesStateRequest(): MbbCommand
    fun swipeStateRequest(): MbbCommand
}

/** Single source of truth for models that are safe to control. */
object HeadphonesProtocolRegistry {
    private val verifiedProfiles: List<HeadphonesProtocol> = listOf(
        FreeBuds5iProtocol,
        FreeBuds6iProtocol,
    )

    fun findByBluetoothName(name: String?): HeadphonesProtocol? =
        verifiedProfiles.firstOrNull { it.matchesBluetoothName(name) }

    fun findById(id: String): HeadphonesProtocol? =
        verifiedProfiles.firstOrNull { it.id == id }

    fun supportedDisplayNames(): List<String> = verifiedProfiles.map { it.displayName }

    fun defaultProfile(): HeadphonesProtocol = verifiedProfiles.first()
}
