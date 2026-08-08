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

data class DualConnectDevice(
    val name: String,
    val address: String,
    val autoConnect: Boolean,
    val preferred: Boolean,
    val connectionState: DualConnectionState,
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
    val name: String = "FreeBuds 5i",
    val address: String = "",
    val battery: BatteryLevels = BatteryLevels(),
    val ancMode: AncMode? = null,
    val ancLevel: AncLevel? = null,
    val settings: FreeBudsSettings = FreeBudsSettings(),
    val dualConnectEnabled: Boolean? = null,
    val dualConnectDevices: List<DualConnectDevice> = emptyList(),
)
