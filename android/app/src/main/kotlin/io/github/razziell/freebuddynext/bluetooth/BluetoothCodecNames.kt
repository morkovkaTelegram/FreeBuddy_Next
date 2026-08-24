package io.github.razziell.freebuddynext.bluetooth

import android.bluetooth.BluetoothCodecStatus
import android.os.Build

/**
 * Maps [android.bluetooth.BluetoothCodecConfig] source codec type constants to
 * user-visible names. The constants are stable AOSP values (public SDK since
 * API 33, identical hidden values on 28..32).
 */
object BluetoothCodecNames {
    const val CODEC_CONFIG_CHANGED_ACTION =
        "android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED"
    const val CACHE_PREFERENCES = "active_a2dp_codecs"

    fun nameFor(codecType: Int): String? = when (codecType) {
        0 -> "SBC"
        1 -> "AAC"
        2 -> "aptX"
        3 -> "aptX HD"
        4 -> "LDAC"
        5 -> "LC3"
        6 -> "Opus"
        else -> null
    }

    @Suppress("DEPRECATION")
    fun nameFrom(status: BluetoothCodecStatus?): String? {
        status ?: return null
        val (type, description) = if (Build.VERSION.SDK_INT >= 33) {
            val config = status.codecConfig ?: return null
            config.codecType to config.toString()
        } else {
            // BluetoothCodecStatus/Config exist on older Android versions but
            // their accessors were hidden from the SDK until API 33.
            val config = runCatching {
                status.javaClass.getMethod("getCodecConfig").invoke(status)
            }.getOrNull() ?: return null
            val codecType = runCatching {
                config.javaClass.getMethod("getCodecType").invoke(config) as? Int
            }.getOrNull() ?: return null
            codecType to config.toString()
        }
        return nameFor(type)
            ?: description
                .substringAfter("codecName:", "")
                .substringBefore(',')
                .trim()
                .takeIf { it.isNotEmpty() }
    }
}
