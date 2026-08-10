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


    override fun customEqualizerSave(preset: CustomEqualizerPreset): MbbCommand =
        customEqualizerCommand(preset, action = CUSTOM_EQ_ACTION_SAVE)

    override fun customEqualizerDelete(preset: CustomEqualizerPreset): MbbCommand =
        customEqualizerCommand(preset, action = CUSTOM_EQ_ACTION_DELETE)

    private fun customEqualizerCommand(
        preset: CustomEqualizerPreset,
        action: Int,
    ): MbbCommand {
        require(preset.id in CUSTOM_EQ_FIRST_ID..CUSTOM_EQ_LAST_ID) {
            "Custom equalizer ID must be in $CUSTOM_EQ_FIRST_ID..$CUSTOM_EQ_LAST_ID"
        }
        require(preset.bands.size == CUSTOM_EQ_BAND_COUNT) {
            "Custom equalizer must contain $CUSTOM_EQ_BAND_COUNT bands"
        }
        require(preset.bands.all { it in CUSTOM_EQ_MIN..CUSTOM_EQ_MAX && it % CUSTOM_EQ_STEP == 0 }) {
            "Custom equalizer bands must be in $CUSTOM_EQ_MIN..$CUSTOM_EQ_MAX with step $CUSTOM_EQ_STEP"
        }
        val nameBytes = preset.name.trim().toByteArray(Charsets.UTF_8)
        require(nameBytes.isNotEmpty() && nameBytes.size <= CUSTOM_EQ_NAME_MAX_BYTES) {
            "Custom equalizer name must occupy 1..$CUSTOM_EQ_NAME_MAX_BYTES UTF-8 bytes"
        }
        return MbbCommand(
            43,
            73,
            linkedMapOf(
                1 to intBytes(preset.id),
                2 to intBytes(preset.bands.size),
                3 to preset.bands.map(Int::toByte).toByteArray(),
                4 to nameBytes,
                5 to intBytes(action),
            ),
        )
    }

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

    /**
     * Confirmed on a real black FreeBuds 6i pair:
     * model BTFT0019, submodel BTFT0019-000153, TLV 25 = 0x02.
     *
     * White and purple codes remain unknown, so only the verified black code
     * is mapped automatically. The textual fallback is retained for firmware
     * variants that may expose a human-readable color.
     */
    override fun detectColor(info: DeviceInfo?): DeviceColor? {
        when (info?.unknownFields?.get(25)?.uppercase()) {
            "0X02" -> return DeviceColor.BLACK
        }
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

    const val CUSTOM_EQ_FIRST_ID = 100
    const val CUSTOM_EQ_LAST_ID = 102
    const val CUSTOM_EQ_BAND_COUNT = 10
    const val CUSTOM_EQ_MIN = -60
    const val CUSTOM_EQ_MAX = 60
    const val CUSTOM_EQ_STEP = 5
    const val CUSTOM_EQ_NAME_MAX_BYTES = 24
    private const val CUSTOM_EQ_ACTION_SAVE = 1
    private const val CUSTOM_EQ_ACTION_DELETE = 2
    private const val CUSTOM_PRESET_SIZE = 36
}
