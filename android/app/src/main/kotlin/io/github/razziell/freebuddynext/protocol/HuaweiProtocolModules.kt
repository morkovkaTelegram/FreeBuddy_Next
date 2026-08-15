package io.github.razziell.freebuddynext.protocol

private fun Boolean.asCode() = if (this) 1 else 0
private fun MbbCommand.arg(id: Int) = args[id]?.firstUOrNull()

object HuaweiDeviceInfoModule : DeviceInfoModule {
    override val initialRequests = listOf(
        MbbCommand(1, 7, (0..31).associateWith { byteArrayOf() }),
    )

    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand): HeadphonesSnapshot {
        if (command.serviceId != 1 || command.commandId != 7) return snapshot
        fun text(id: Int): String? = command.args[id]
            ?.takeIf(ByteArray::isNotEmpty)
            ?.toString(Charsets.UTF_8)
            ?.trim { it <= ' ' || it == '\u0000' }
            ?.takeIf { value -> value.isNotEmpty() && value.all { it.code in 32..126 } }
        fun hex(bytes: ByteArray) = bytes.toHex()
        fun hex(id: Int) = command.args[id]?.takeIf(ByteArray::isNotEmpty)?.let(::hex)
        fun diagnosticValue(bytes: ByteArray): String {
            if (bytes.all { (it.toInt() and 0xFF) in 32..126 }) return bytes.toString(Charsets.US_ASCII)
            val printable = buildString {
                bytes.forEach { byte ->
                    (byte.toInt() and 0xFF).takeIf { it in 32..126 }?.let { append(it.toChar()) }
                }
            }.trim().replace("\\", "\\\\").replace("\"", "\\\"")
            val raw = "0x${hex(bytes)}"
            return if (printable.isEmpty()) raw else "$raw (\"$printable\")"
        }

        var left: String? = null
        var right: String? = null
        text(24)?.takeIf { it.startsWith("L-") && ',' in it }?.split(',')?.let {
            left = it.getOrNull(0)?.removePrefix("L-")
            right = it.getOrNull(1)?.removePrefix("R-")
        }
        val known = setOf(3, 7, 9, 10, 15, 24)
        val unknown = command.args.asSequence()
            .filter { (id, value) -> id !in known && value.isNotEmpty() }
            .sortedBy(Map.Entry<Int, ByteArray>::key)
            .associate { (id, value) -> id to diagnosticValue(value) }
        val previous = snapshot.deviceInfo ?: DeviceInfo()
        val info = DeviceInfo(
            model = text(15) ?: previous.model,
            subModel = text(10) ?: hex(10) ?: previous.subModel,
            firmware = text(7) ?: previous.firmware,
            serialNumber = text(9) ?: previous.serialNumber,
            hardwareVersion = text(3) ?: previous.hardwareVersion,
            leftSerial = left ?: previous.leftSerial,
            rightSerial = right ?: previous.rightSerial,
            unknownFields = previous.unknownFields + unknown,
        )
        return if (info == DeviceInfo()) snapshot else snapshot.copy(deviceInfo = info)
    }
}

object HuaweiBatteryModule : BatteryModule {
    override val initialRequests = listOf(
        MbbCommand(1, 8, mapOf(1 to byteArrayOf(), 2 to byteArrayOf(), 3 to byteArrayOf())),
    )

    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand): HeadphonesSnapshot {
        if (command.serviceId != 1 || command.commandId !in setOf(8, 39)) return snapshot
        val level = command.args[2] ?: return snapshot
        val charging = command.args[3] ?: return snapshot
        if (level.size < 3 || charging.size < 3) return snapshot
        return snapshot.copy(
            battery = BatteryLevels(
                BatteryLevel.fromProtocol(level[0]),
                BatteryLevel.fromProtocol(level[1]),
                BatteryLevel.fromProtocol(level[2]),
                charging[0].toInt() == 1,
                charging[1].toInt() == 1,
                charging[2].toInt() == 1,
            ),
        )
    }
}

object HuaweiAdvancedAncModule : AncModule {
    override val supportsLevels = true
    override val supportsDynamicLevel = true
    override val initialRequests get() = listOf(stateRequest())
    override fun setMode(mode: AncMode) =
        MbbCommand(43, 4, mapOf(1 to intBytes(mode.code, if (mode == AncMode.OFF) 0 else 255)))
    override fun setLevel(mode: AncMode, level: AncLevel) =
        MbbCommand(43, 4, mapOf(1 to intBytes(mode.code, level.code)))
    override fun stateRequest() = MbbCommand(43, 42, mapOf(1 to byteArrayOf(), 2 to byteArrayOf()))
    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand): HeadphonesSnapshot {
        if (command.serviceId != 43 || command.commandId != 42) return snapshot
        val values = command.args[1] ?: return snapshot
        if (values.size < 2) return snapshot
        return snapshot.copy(
            ancLevel = AncLevel.entries.firstOrNull { it.code == values[0].toInt().and(0xFF) },
            ancMode = AncMode.entries.firstOrNull { it.code == values[1].toInt().and(0xFF) },
        )
    }
}

object HuaweiAutoPauseModule : AutoPauseModule {
    override val initialRequests get() = listOf(stateRequest())
    override fun setEnabled(enabled: Boolean) = MbbCommand(43, 16, mapOf(1 to intBytes(enabled.asCode())))
    override fun stateRequest() = MbbCommand(43, 17, mapOf(1 to byteArrayOf()))
    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand) =
        if (command.serviceId == 43 && command.commandId == 17) {
            snapshot.copy(settings = snapshot.settings.copy(autoPause = command.arg(1) == 1))
        } else snapshot
}

object HuaweiDoubleTapModule : DoubleTapModule {
    override val initialRequests get() = listOf(stateRequest())
    override fun set(left: DoubleTap?, right: DoubleTap?) = MbbCommand(
        1, 31, buildMap {
            left?.let { put(1, intBytes(it.code)) }
            right?.let { put(2, intBytes(it.code)) }
        },
    )
    override fun stateRequest() = MbbCommand(1, 32, mapOf(1 to byteArrayOf(), 2 to byteArrayOf()))
    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand) =
        if (command.serviceId == 1 && command.commandId == 32) {
            snapshot.copy(settings = snapshot.settings.copy(
                doubleTapLeft = DoubleTap.entries.firstOrNull { it.code == command.arg(1) },
                doubleTapRight = DoubleTap.entries.firstOrNull { it.code == command.arg(2) },
            ))
        } else snapshot
}

object HuaweiTripleTapModule : TripleTapModule {
    override val initialRequests get() = listOf(stateRequest())
    override fun set(left: TripleTap?, right: TripleTap?) = MbbCommand(
        1, 37, buildMap {
            left?.let { put(1, intBytes(it.code)) }
            right?.let { put(2, intBytes(it.code)) }
        },
    )
    override fun stateRequest() = MbbCommand(1, 38, mapOf(1 to byteArrayOf(), 2 to byteArrayOf()))
    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand) =
        if (command.serviceId == 1 && command.commandId == 38) {
            snapshot.copy(settings = snapshot.settings.copy(
                tripleTapLeft = TripleTap.entries.firstOrNull { it.code == command.arg(1) },
                tripleTapRight = TripleTap.entries.firstOrNull { it.code == command.arg(2) },
            ))
        } else snapshot
}

object HuaweiSplitHoldModule : HoldModule {
    override val initialRequests get() = listOf(actionStateRequest(), modesStateRequest())
    override fun setAction(value: Hold) = MbbCommand(
        43, 22, mapOf(1 to intBytes(value.code), 2 to intBytes(value.code)),
    )
    override fun setModes(modes: Set<AncMode>): MbbCommand {
        val value = when (modes) {
            setOf(AncMode.OFF, AncMode.NOISE_CANCELLING) -> 1
            AncMode.entries.toSet() -> 2
            setOf(AncMode.NOISE_CANCELLING, AncMode.TRANSPARENCY) -> 3
            setOf(AncMode.OFF, AncMode.TRANSPARENCY) -> 4
            else -> 2
        }
        return MbbCommand(43, 24, mapOf(1 to intBytes(value), 2 to intBytes(value)))
    }
    override fun actionStateRequest() = MbbCommand(43, 23, mapOf(1 to byteArrayOf(), 2 to byteArrayOf()))
    override fun modesStateRequest() = MbbCommand(43, 25, mapOf(1 to byteArrayOf(), 2 to byteArrayOf()))
    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand): HeadphonesSnapshot = when {
        command.serviceId == 43 && command.commandId == 23 -> snapshot.copy(
            settings = snapshot.settings.copy(
                hold = Hold.entries.firstOrNull { it.code == (command.arg(1) ?: command.arg(2)) },
            ),
        )
        command.serviceId == 43 && command.commandId == 25 -> snapshot.copy(
            settings = snapshot.settings.copy(holdModes = when (command.arg(1) ?: command.arg(2)) {
                1 -> setOf(AncMode.OFF, AncMode.NOISE_CANCELLING)
                2 -> AncMode.entries.toSet()
                3 -> setOf(AncMode.NOISE_CANCELLING, AncMode.TRANSPARENCY)
                4 -> setOf(AncMode.OFF, AncMode.TRANSPARENCY)
                else -> null
            }),
        )
        else -> snapshot
    }
}

object HuaweiSwipeModule : SwipeModule {
    override val initialRequests get() = listOf(stateRequest())
    override fun set(value: Swipe) = MbbCommand(
        43, 30, mapOf(1 to intBytes(value.code), 2 to intBytes(value.code)),
    )
    override fun stateRequest() = MbbCommand(43, 31, mapOf(1 to byteArrayOf(), 2 to byteArrayOf()))
    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand) =
        if (command.serviceId == 43 && command.commandId == 31) {
            snapshot.copy(settings = snapshot.settings.copy(
                swipe = Swipe.entries.firstOrNull { it.code == command.arg(1) },
            ))
        } else snapshot
}

object HuaweiLowLatencyModule : LowLatencyModule {
    override val initialRequests get() = listOf(stateRequest())
    override fun setEnabled(enabled: Boolean) = MbbCommand(43, 108, mapOf(1 to intBytes(enabled.asCode())))
    override fun stateRequest() = MbbCommand(43, 108, mapOf(2 to byteArrayOf()))
    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand) =
        if (command.serviceId == 43 && command.commandId == 108) {
            snapshot.copy(settings = snapshot.settings.copy(lowLatency = command.arg(2) == 1))
        } else snapshot
}

object HuaweiLdacModule : LdacModule {
    override val initialRequests get() = listOf(stateRequest())
    override fun setEnabled(enabled: Boolean) = MbbCommand(43, 162, mapOf(1 to intBytes(enabled.asCode())))
    override fun stateRequest() = MbbCommand(43, 163)
    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand) =
        if (command.serviceId == 43 && command.commandId == 163) {
            snapshot.copy(settings = snapshot.settings.copy(ldac = command.arg(2) == 1))
        } else snapshot
}

object HuaweiBuiltInEqualizerModule : EqualizerModule {
    override val initialRequests get() = listOf(stateRequest())
    override fun selectPreset(value: EqPreset) = MbbCommand(43, 73, mapOf(1 to intBytes(value.code)))
    override fun stateRequest() = MbbCommand(43, 74, (1..8).associateWith { byteArrayOf() })
    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand) =
        if (command.serviceId == 43 && command.commandId == 74) {
            snapshot.copy(settings = snapshot.settings.copy(
                eqPreset = EqPreset.entries.firstOrNull { it.code == command.arg(2) },
            ))
        } else snapshot
}

class HuaweiDualConnectModule(
    override val supportsAutoConnect: Boolean,
) : DualConnectModule {
    override val initialRequests get() = listOf(stateRequest(), enumerate())
    override fun setEnabled(enabled: Boolean) = MbbCommand(43, 46, mapOf(1 to intBytes(enabled.asCode())))
    override fun stateRequest() = MbbCommand(43, 47, mapOf(1 to byteArrayOf()))
    override fun enumerate() = MbbCommand(43, 49, mapOf(1 to byteArrayOf()))
    override fun setPreferred(device: DualConnectDevice, enabled: Boolean) = MbbCommand(
        43, 50, mapOf(1 to if (enabled) device.address.toMacBytes() else ByteArray(6)),
    )
    override fun action(device: DualConnectDevice, action: Int) =
        MbbCommand(43, 51, mapOf(action to device.address.toMacBytes()))
    override fun isChangeEvent(command: MbbCommand) = command.serviceId == 43 && command.commandId == 54
    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand): HeadphonesSnapshot {
        return when {
        command.serviceId == 43 && command.commandId == 47 ->
            snapshot.copy(dualConnectEnabled = command.arg(1) == 1)
        command.serviceId == 43 && command.commandId == 49 -> {
            val rawMac = command.args[4] ?: return snapshot
            val rawName = command.args[9] ?: return snapshot
            if (rawMac.size != 6) return snapshot
            val address = rawMac.joinToString(":") { "%02X".format(it.toInt().and(0xFF)) }
            val rawConnection = command.arg(5) ?: 0
            val device = DualConnectDevice(
                name = rawName.toString(Charsets.UTF_8).trimEnd('\u0000'),
                address = address,
                autoConnect = command.arg(8) == 1,
                preferred = command.arg(7) == 1,
                connectionState = when {
                    rawConnection == 9 -> DualConnectionState.PLAYING
                    rawConnection > 0 -> DualConnectionState.CONNECTED
                    else -> DualConnectionState.DISCONNECTED
                },
            )
            snapshot.copy(dualConnectDevices = snapshot.dualConnectDevices.filterNot { it.address == address } + device)
        }
        else -> snapshot
    }
    }

    private fun String.toMacBytes() = split(':').map { it.toInt(16).toByte() }.toByteArray()
}
