package io.github.razziell.freebuddynext.companion

import java.util.Locale

internal fun companionAddress(value: String?): String? = value?.uppercase(Locale.ROOT)
    ?.takeIf { it.matches(Regex("(?:[0-9A-F]{2}:){5}[0-9A-F]{2}")) }

/** BLE disappearance is not a Classic Bluetooth disconnect (and vice versa). */
internal class CompanionPresence {
    enum class Source { LEGACY, BLUETOOTH, BLE }
    private val devices = mutableMapOf<String, MutableSet<Source>>()
    val anyPresent: Boolean get() = devices.isNotEmpty()

    fun update(address: String, source: Source, present: Boolean) {
        val key = companionAddress(address) ?: return
        if (present) devices.getOrPut(key) { mutableSetOf() }.add(source)
        else devices[key]?.let { sources ->
            sources.remove(source)
            if (sources.isEmpty()) devices.remove(key)
        }
    }

    fun retain(enabled: Set<String>) {
        val canonical = enabled.mapNotNull(::companionAddress).toSet()
        devices.keys.retainAll(canonical)
    }
}
