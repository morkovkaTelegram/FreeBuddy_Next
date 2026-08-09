package com.lastgimbus.the.freebuddy.protocol

/**
 * HUAWEI FreeBuds 6i profile.
 *
 * OpenFreebuds marks this model as tested and uses the same Huawei MBB
 * handlers as 5i for the features exposed by FreeBuddy. Model-specific
 * differences are declared through capabilities: 6i supports custom EQ,
 * while Dual Connect does not expose per-device auto-connect.
 */
object FreeBuds6iProtocol : HeadphonesProtocol by FreeBuds5iProtocol {
    override val id = "freebuds_6i"
    override val displayName = "HUAWEI FreeBuds 6i"
    override val bluetoothNamePrefixes = setOf("HUAWEI FreeBuds 6i")
    override val availableColors = listOf(
        DeviceColor.BLACK,
        DeviceColor.WHITE,
        DeviceColor.PURPLE,
    )
    override fun matchesBluetoothName(name: String?): Boolean =
        name != null && bluetoothNamePrefixes.any(name::startsWith)

    override val capabilities = FreeBuds5iProtocol.capabilities.copy(
        customEqualizer = true,
        dualConnectAutoConnect = false,
    )

    // Explicitly include the full 43/74 read used by OpenFreebuds. It is also
    // present in the delegated 5i request list, but keeping it here documents
    // that 6i consumes fields 3 and 8 for custom-preset diagnostics.
    override val initialRequests = FreeBuds5iProtocol.initialRequests
        .filterNot { it.serviceId == 43 && it.commandId == 74 } + equalizerStateRequest()

    /** OpenFreebuds reads all known equalizer TLVs 1..8. */
    override fun equalizerStateRequest() =
        MbbCommand(43, 74, (1..8).associateWith { byteArrayOf() })

    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand): HeadphonesSnapshot {
        val common = FreeBuds5iProtocol.reduce(snapshot, command)
        return if (command.serviceId == 43 && command.commandId == 74) {
            val parsed = parseEqualizerDiagnostics(command)
            val previous = snapshot.equalizerDiagnostics
            common.copy(
                equalizerDiagnostics = parsed.copy(
                    activePresetId = parsed.activePresetId ?: previous?.activePresetId,
                    builtInPresetIds = parsed.builtInPresetIds.ifEmpty {
                        previous?.builtInPresetIds.orEmpty()
                    },
                    customPresets = parsed.customPresets.ifEmpty {
                        previous?.customPresets.orEmpty()
                    },
                    rawFields = previous?.rawFields.orEmpty() + parsed.rawFields,
                ),
            )
        } else {
            common
        }
    }

    // 6i color TLVs have not yet been confirmed on real hardware. Never apply
    // the undocumented 5i TLV-25 mapping to this model.
    override fun detectColor(info: DeviceInfo?): DeviceColor? {
        val text = listOfNotNull(info?.subModel, info?.hardwareVersion)
            .joinToString(" ")
            .uppercase()
        return when {
            "PURPLE" in text || "VIOLET" in text -> DeviceColor.PURPLE
            "WHITE" in text -> DeviceColor.WHITE
            "BLACK" in text -> DeviceColor.BLACK
            else -> null
        }
    }

    internal fun parseEqualizerDiagnostics(command: MbbCommand): EqualizerDiagnostics {
        val active = command.args[2]?.singleOrNull()?.toInt()?.and(0xFF)
        val builtIn = (command.args[3] ?: byteArrayOf()).map { it.toInt() and 0xFF }
        val custom = (command.args[8] ?: byteArrayOf()).parseCustomPresets()
        return EqualizerDiagnostics(
            activePresetId = active,
            builtInPresetIds = builtIn,
            customPresets = custom,
            rawFields = command.args.toSortedMap().mapValues { (_, value) -> "0x${value.toHex()}" },
        )
    }

    private fun ByteArray.parseCustomPresets(): List<CustomEqualizerPreset> {
        val bytes = this
        return buildList {
            var offset = 0
            while (offset + CUSTOM_PRESET_SIZE <= bytes.size) {
                val block = bytes.copyOfRange(offset, offset + CUSTOM_PRESET_SIZE)
                val bandCount = (block[1].toInt() and 0xFF).coerceAtMost(CUSTOM_PRESET_SIZE - 2)
                val bandEnd = 2 + bandCount
                val bands = block.copyOfRange(2, bandEnd).map { it.toInt() }
                val nameEnd = (bandEnd until block.size).firstOrNull { block[it] == 0.toByte() }
                    ?: block.size
                val name = block.copyOfRange(bandEnd, nameEnd).toString(Charsets.UTF_8)
                    .takeIf { it.isNotBlank() }
                    ?: "preset_${block[0].toInt() and 0xFF}"
                add(CustomEqualizerPreset(block[0].toInt() and 0xFF, name, bands))
                offset += CUSTOM_PRESET_SIZE
            }
        }
    }

    private const val CUSTOM_PRESET_SIZE = 36
}
