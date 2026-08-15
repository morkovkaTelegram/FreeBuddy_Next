package io.github.razziell.freebuddynext.protocol

object HuaweiCustomEqualizerModule : CustomEqualizerModule {
    override val firstPresetId = 100
    override val lastPresetId = 102
    override val bandCount = 10
    override val minimum = -60
    override val maximum = 60
    override val step = 5
    override val maxNameBytes = 24

    override val initialRequests get() = listOf(stateRequest())
    override fun selectPreset(value: EqPreset) = MbbCommand(43, 73, mapOf(1 to intBytes(value.code)))
    override fun stateRequest() = MbbCommand(43, 74, (1..8).associateWith { byteArrayOf() })
    override fun savePreset(preset: CustomEqualizerPreset) = command(preset, action = 1)
    override fun deletePreset(preset: CustomEqualizerPreset) = command(preset, action = 2)

    private fun command(preset: CustomEqualizerPreset, action: Int): MbbCommand {
        require(preset.id in firstPresetId..lastPresetId)
        require(preset.bands.size == bandCount)
        require(preset.bands.all { it in minimum..maximum && it % step == 0 })
        val name = preset.name.trim().toByteArray(Charsets.UTF_8)
        require(name.isNotEmpty() && name.size <= maxNameBytes)
        return MbbCommand(
            43, 73, linkedMapOf(
                1 to intBytes(preset.id),
                2 to intBytes(preset.bands.size),
                3 to preset.bands.map(Int::toByte).toByteArray(),
                4 to name,
                5 to intBytes(action),
            ),
        )
    }

    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand): HeadphonesSnapshot {
        if (command.serviceId != 43 || command.commandId != 74) return snapshot
        val active = command.args[2]?.singleOrNull()?.toInt()?.and(0xFF)
        val builtIn = (command.args[3] ?: byteArrayOf()).map { it.toInt() and 0xFF }
        val custom = (command.args[8] ?: byteArrayOf()).parseCustomPresets()
        val previous = snapshot.equalizerDiagnostics
        val parsed = EqualizerDiagnostics(
            activePresetId = active,
            builtInPresetIds = builtIn,
            customPresets = custom,
            rawFields = command.args.toSortedMap().mapValues { (_, value) -> "0x${value.toHex()}" },
        )
        return snapshot.copy(
            settings = snapshot.settings.copy(
                eqPreset = EqPreset.entries.firstOrNull { it.code == active },
            ),
            equalizerDiagnostics = parsed.copy(
                activePresetId = parsed.activePresetId ?: previous?.activePresetId,
                builtInPresetIds = parsed.builtInPresetIds.ifEmpty { previous?.builtInPresetIds.orEmpty() },
                customPresets = parsed.customPresets.ifEmpty { previous?.customPresets.orEmpty() },
                rawFields = previous?.rawFields.orEmpty() + parsed.rawFields,
            ),
        )
    }

    private fun ByteArray.parseCustomPresets(): List<CustomEqualizerPreset> {
        val bytes = this
        return buildList {
            var offset = 0
            while (offset + PRESET_SIZE <= bytes.size) {
                val block = bytes.copyOfRange(offset, offset + PRESET_SIZE)
            val count = (block[1].toInt() and 0xFF).coerceAtMost(PRESET_SIZE - 2)
            val end = 2 + count
            val bands = block.copyOfRange(2, end).map(Byte::toInt)
            val nameEnd = (end until block.size).firstOrNull { block[it] == 0.toByte() } ?: block.size
            val name = block.copyOfRange(end, nameEnd).toString(Charsets.UTF_8)
                .takeIf(String::isNotBlank) ?: "preset_${block[0].toInt() and 0xFF}"
            add(CustomEqualizerPreset(block[0].toInt() and 0xFF, name, bands))
                offset += PRESET_SIZE
            }
        }
    }

    private const val PRESET_SIZE = 36
}
