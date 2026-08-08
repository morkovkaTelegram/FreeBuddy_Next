package com.lastgimbus.the.freebuddy.protocol

object FreeBuds5iCommands {
    val initialRequests = listOf(
        MbbCommand(1, 8), MbbCommand(43, 42), MbbCommand(43, 17),
        MbbCommand(1, 32), MbbCommand(1, 38), MbbCommand(43, 23),
        MbbCommand(43, 25), MbbCommand(43, 31), MbbCommand(43, 108, mapOf(2 to byteArrayOf())),
        MbbCommand(43, 74), MbbCommand(43, 163), MbbCommand(43, 47),
        dualConnectEnumeration(),
    )

    fun ancMode(mode: AncMode) = MbbCommand(43, 4, mapOf(1 to intBytes(mode.code, if (mode == AncMode.OFF) 0 else 255)))
    fun ancLevel(mode: AncMode, level: AncLevel) = MbbCommand(43, 4, mapOf(1 to intBytes(mode.code, level.code)))
    fun doubleTap(left: DoubleTap? = null, right: DoubleTap? = null) =
        MbbCommand(1, 31, buildMap { left?.let { put(1, intBytes(it.code)) }; right?.let { put(2, intBytes(it.code)) } })
    fun tripleTap(left: TripleTap? = null, right: TripleTap? = null) =
        MbbCommand(1, 37, buildMap { left?.let { put(1, intBytes(it.code)) }; right?.let { put(2, intBytes(it.code)) } })
    fun hold(value: Hold) = MbbCommand(43, 22, mapOf(1 to intBytes(value.code)))
    fun holdModes(modes: Set<AncMode>): MbbCommand {
        val value = when (modes) {
            setOf(AncMode.OFF, AncMode.NOISE_CANCELLING) -> 1
            AncMode.entries.toSet() -> 2
            setOf(AncMode.NOISE_CANCELLING, AncMode.TRANSPARENCY) -> 3
            setOf(AncMode.OFF, AncMode.TRANSPARENCY) -> 4
            else -> 2
        }
        return MbbCommand(43, 24, mapOf(1 to intBytes(value), 2 to intBytes(value)))
    }
    fun swipe(value: Swipe) = MbbCommand(43, 30, mapOf(1 to intBytes(value.code)))
    fun autoPause(enabled: Boolean) = MbbCommand(43, 16, mapOf(1 to intBytes(enabled.asCode())))
    fun lowLatency(enabled: Boolean) = MbbCommand(43, 108, mapOf(1 to intBytes(enabled.asCode())))
    fun equalizer(value: EqPreset) = MbbCommand(43, 73, mapOf(1 to intBytes(value.code)))
    fun ldac(enabled: Boolean) = MbbCommand(43, 162, mapOf(1 to intBytes(enabled.asCode())))
    fun dualConnect(enabled: Boolean) = MbbCommand(43, 46, mapOf(1 to intBytes(enabled.asCode())))
    fun dualConnectEnumeration() = MbbCommand(43, 49, mapOf(1 to byteArrayOf()))
    fun dualConnectPreferred(device: DualConnectDevice, enabled: Boolean) = MbbCommand(
        43,
        50,
        mapOf(1 to if (enabled) device.address.toMacBytes() else ByteArray(6)),
    )
    fun dualConnectAction(device: DualConnectDevice, action: Int) =
        MbbCommand(43, 51, mapOf(action to device.address.toMacBytes()))

    private fun String.toMacBytes(): ByteArray = split(':').map { it.toInt(16).toByte() }.toByteArray()
    private fun Boolean.asCode() = if (this) 1 else 0
}

fun HeadphonesSnapshot.apply(command: MbbCommand): HeadphonesSnapshot {
    fun arg(id: Int) = command.args[id]?.firstUOrNull()
    var settings = settings
    return when {
        command.serviceId == 1 && command.commandId in setOf(8, 39) -> {
            val level = command.args[2] ?: return this
            val charging = command.args[3] ?: return this
            if (level.size < 3 || charging.size < 3) return this
            copy(battery = BatteryLevels(
                BatteryLevel.fromProtocol(level[0]), BatteryLevel.fromProtocol(level[1]), BatteryLevel.fromProtocol(level[2]),
                charging[0].toInt() == 1, charging[1].toInt() == 1, charging[2].toInt() == 1,
            ))
        }
        command.serviceId == 43 && command.commandId == 42 -> {
            val values = command.args[1] ?: return this
            if (values.size < 2) return this
            copy(
                ancLevel = AncLevel.entries.firstOrNull { it.code == values[0].toInt().and(0xFF) },
                ancMode = AncMode.entries.firstOrNull { it.code == values[1].toInt().and(0xFF) },
            )
        }
        command.serviceId == 43 && command.commandId == 17 -> copy(settings = settings.copy(autoPause = arg(1) == 1))
        command.serviceId == 1 && command.commandId == 32 -> copy(settings = settings.copy(
            doubleTapLeft = DoubleTap.entries.firstOrNull { it.code == arg(1) },
            doubleTapRight = DoubleTap.entries.firstOrNull { it.code == arg(2) },
        ))
        command.serviceId == 1 && command.commandId == 38 -> copy(settings = settings.copy(
            tripleTapLeft = TripleTap.entries.firstOrNull { it.code == arg(1) },
            tripleTapRight = TripleTap.entries.firstOrNull { it.code == arg(2) },
        ))
        command.serviceId == 43 && command.commandId == 23 -> copy(settings = settings.copy(
            hold = Hold.entries.firstOrNull { it.code == arg(1) },
        ))
        command.serviceId == 43 && command.commandId == 25 -> copy(settings = settings.copy(holdModes = when (arg(1)) {
            1 -> setOf(AncMode.OFF, AncMode.NOISE_CANCELLING)
            2 -> AncMode.entries.toSet()
            3 -> setOf(AncMode.NOISE_CANCELLING, AncMode.TRANSPARENCY)
            4 -> setOf(AncMode.OFF, AncMode.TRANSPARENCY)
            else -> null
        }))
        command.serviceId == 43 && command.commandId == 31 -> copy(settings = settings.copy(
            swipe = Swipe.entries.firstOrNull { it.code == arg(1) },
        ))
        command.serviceId == 43 && command.commandId == 108 -> copy(settings = settings.copy(lowLatency = arg(2) == 1))
        command.serviceId == 43 && command.commandId == 74 -> copy(settings = settings.copy(
            eqPreset = EqPreset.entries.firstOrNull { it.code == arg(2) },
        ))
        command.serviceId == 43 && command.commandId == 163 -> copy(settings = settings.copy(ldac = arg(2) == 1))
        command.serviceId == 43 && command.commandId == 47 -> copy(dualConnectEnabled = arg(1) == 1)
        command.serviceId == 43 && command.commandId == 49 -> {
            val rawMac = command.args[4] ?: return this
            val rawName = command.args[9] ?: return this
            if (rawMac.size != 6) return this
            val address = rawMac.joinToString(":") { "%02X".format(it.toInt().and(0xFF)) }
            val rawConnection = arg(5) ?: 0
            val device = DualConnectDevice(
                name = rawName.toString(Charsets.UTF_8).trimEnd('\u0000'),
                address = address,
                autoConnect = arg(8) == 1,
                preferred = arg(7) == 1,
                connectionState = when {
                    rawConnection == 9 -> DualConnectionState.PLAYING
                    rawConnection > 0 -> DualConnectionState.CONNECTED
                    else -> DualConnectionState.DISCONNECTED
                },
            )
            copy(dualConnectDevices = dualConnectDevices.filterNot { it.address == address } + device)
        }
        else -> this
    }
}

