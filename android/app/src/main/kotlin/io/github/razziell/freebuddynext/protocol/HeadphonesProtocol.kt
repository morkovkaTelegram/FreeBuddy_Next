package io.github.razziell.freebuddynext.protocol

import java.util.UUID

/** A self-contained protocol feature that owns its reads and packet reducer. */
interface ProtocolModule {
    val initialRequests: List<MbbCommand>
    fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand): HeadphonesSnapshot = snapshot
}

interface DeviceInfoModule : ProtocolModule
interface BatteryModule : ProtocolModule

interface AncModule : ProtocolModule {
    val supportsLevels: Boolean
    val supportsDynamicLevel: Boolean
    fun setMode(mode: AncMode): MbbCommand
    fun setLevel(mode: AncMode, level: AncLevel): MbbCommand
    fun stateRequest(): MbbCommand
}

interface AutoPauseModule : ProtocolModule {
    fun setEnabled(enabled: Boolean): MbbCommand
    fun stateRequest(): MbbCommand
}

interface DoubleTapModule : ProtocolModule {
    fun set(left: DoubleTap? = null, right: DoubleTap? = null): MbbCommand
    fun stateRequest(): MbbCommand
}

interface TripleTapModule : ProtocolModule {
    fun set(left: TripleTap? = null, right: TripleTap? = null): MbbCommand
    fun stateRequest(): MbbCommand
}

interface HoldModule : ProtocolModule {
    fun setAction(value: Hold): MbbCommand
    fun setModes(modes: Set<AncMode>): MbbCommand
    fun actionStateRequest(): MbbCommand
    fun modesStateRequest(): MbbCommand
}

interface SwipeModule : ProtocolModule {
    fun set(value: Swipe): MbbCommand
    fun stateRequest(): MbbCommand
}

interface LowLatencyModule : ProtocolModule {
    fun setEnabled(enabled: Boolean): MbbCommand
    fun stateRequest(): MbbCommand
}

interface LdacModule : ProtocolModule {
    fun setEnabled(enabled: Boolean): MbbCommand
    fun stateRequest(): MbbCommand
}

interface EqualizerModule : ProtocolModule {
    fun selectPreset(value: EqPreset): MbbCommand
    fun stateRequest(): MbbCommand
}

interface CustomEqualizerModule : EqualizerModule {
    val firstPresetId: Int
    val lastPresetId: Int
    val bandCount: Int
    val minimum: Int
    val maximum: Int
    val step: Int
    val maxNameBytes: Int
    fun savePreset(preset: CustomEqualizerPreset): MbbCommand
    fun deletePreset(preset: CustomEqualizerPreset): MbbCommand
}

interface DualConnectModule : ProtocolModule {
    val supportsAutoConnect: Boolean
    fun setEnabled(enabled: Boolean): MbbCommand
    fun stateRequest(): MbbCommand
    fun enumerate(): MbbCommand
    fun setPreferred(device: DualConnectDevice, enabled: Boolean): MbbCommand
    fun action(device: DualConnectDevice, action: Int): MbbCommand
    fun isChangeEvent(command: MbbCommand): Boolean
}

/** Model identity plus an explicit composition of supported feature modules. */
interface HeadphonesProtocol {
    val id: String
    val displayName: String
    val bluetoothNamePrefixes: Set<String>
    val sppUuid: UUID
    val availableColors: List<DeviceColor>
    val modules: List<ProtocolModule>

    fun detectColor(info: DeviceInfo?): DeviceColor?

    fun matchesBluetoothName(name: String?): Boolean =
        name != null && bluetoothNamePrefixes.any(name::startsWith)

    val initialRequests: List<MbbCommand>
        get() = modules.flatMap(ProtocolModule::initialRequests)

    fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand): HeadphonesSnapshot =
        modules.fold(snapshot) { current, module -> module.reduce(current, command) }
}

inline fun <reified T : ProtocolModule> HeadphonesProtocol.module(): T? =
    modules.filterIsInstance<T>().firstOrNull()

fun HeadphonesProtocol.isDualConnectChangeEvent(command: MbbCommand): Boolean =
    module<DualConnectModule>()?.isChangeEvent(command) == true

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
