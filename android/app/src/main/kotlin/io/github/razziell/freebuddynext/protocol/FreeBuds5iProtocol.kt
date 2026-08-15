package io.github.razziell.freebuddynext.protocol

object FreeBuds5iProtocol : HeadphonesProtocol {
    override val id = "freebuds_5i"
    override val displayName = "HUAWEI FreeBuds 5i"
    override val bluetoothNamePrefixes = setOf("HUAWEI FreeBuds 5i")
    override val sppUuid: java.util.UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    override val availableColors = listOf(DeviceColor.BLACK, DeviceColor.WHITE, DeviceColor.BLUE)
    override val capabilities = HeadphonesCapabilities(
        ancLevels = true,
        dynamicAnc = true,
        doubleTap = true,
        tripleTap = true,
        splitHold = true,
        swipe = true,
        autoPause = true,
        lowLatency = true,
        ldac = true,
        equalizer = true,
        customEqualizer = false,
        dualConnect = true,
        dualConnectAutoConnect = true,
        deviceInfo = true,
    )
    override fun detectColor(info: DeviceInfo?): DeviceColor? = info?.detect5iColor()

    override val initialRequests = listOf(
        batteryStateRequest(), ancStateRequest(), autoPauseStateRequest(),
        doubleTapStateRequest(), tripleTapStateRequest(), holdStateRequest(),
        holdModesStateRequest(), swipeStateRequest(), lowLatencyStateRequest(),
        equalizerStateRequest(), ldacStateRequest(), dualConnectStateRequest(),
        deviceInfoRequest(), dualConnectEnumeration(),
    )

    /** MBB 1/7: read device info TLVs 0..31 (model, sub-model, firmware, serials). */
    fun deviceInfoRequest() = MbbCommand(1, 7, (0..31).associateWith { byteArrayOf() })

    override fun ancMode(mode: AncMode) = MbbCommand(43, 4, mapOf(1 to intBytes(mode.code, if (mode == AncMode.OFF) 0 else 255)))
    override fun ancLevel(mode: AncMode, level: AncLevel) = MbbCommand(43, 4, mapOf(1 to intBytes(mode.code, level.code)))
    override fun doubleTap(left: DoubleTap?, right: DoubleTap?) =
        MbbCommand(1, 31, buildMap { left?.let { put(1, intBytes(it.code)) }; right?.let { put(2, intBytes(it.code)) } })
    override fun tripleTap(left: TripleTap?, right: TripleTap?) =
        MbbCommand(1, 37, buildMap { left?.let { put(1, intBytes(it.code)) }; right?.let { put(2, intBytes(it.code)) } })
    override fun hold(value: Hold) = MbbCommand(
        43,
        22,
        mapOf(1 to intBytes(value.code), 2 to intBytes(value.code)),
    )
    override fun holdModes(modes: Set<AncMode>): MbbCommand {
        val value = when (modes) {
            setOf(AncMode.OFF, AncMode.NOISE_CANCELLING) -> 1
            AncMode.entries.toSet() -> 2
            setOf(AncMode.NOISE_CANCELLING, AncMode.TRANSPARENCY) -> 3
            setOf(AncMode.OFF, AncMode.TRANSPARENCY) -> 4
            else -> 2
        }
        return MbbCommand(43, 24, mapOf(1 to intBytes(value), 2 to intBytes(value)))
    }
    override fun swipe(value: Swipe) = MbbCommand(
        43,
        30,
        mapOf(1 to intBytes(value.code), 2 to intBytes(value.code)),
    )
    override fun autoPause(enabled: Boolean) = MbbCommand(43, 16, mapOf(1 to intBytes(enabled.asCode())))
    override fun lowLatency(enabled: Boolean) = MbbCommand(43, 108, mapOf(1 to intBytes(enabled.asCode())))
    override fun equalizer(value: EqPreset) = MbbCommand(43, 73, mapOf(1 to intBytes(value.code)))
    override fun ldac(enabled: Boolean) = MbbCommand(43, 162, mapOf(1 to intBytes(enabled.asCode())))
    override fun dualConnect(enabled: Boolean) = MbbCommand(43, 46, mapOf(1 to intBytes(enabled.asCode())))
    override fun dualConnectEnumeration() = MbbCommand(43, 49, mapOf(1 to byteArrayOf()))
    override fun dualConnectPreferred(device: DualConnectDevice, enabled: Boolean) = MbbCommand(
        43,
        50,
        mapOf(1 to if (enabled) device.address.toMacBytes() else ByteArray(6)),
    )
    override fun dualConnectAction(device: DualConnectDevice, action: Int) =
        MbbCommand(43, 51, mapOf(action to device.address.toMacBytes()))

    private fun String.toMacBytes(): ByteArray = split(':').map { it.toInt(16).toByte() }.toByteArray()
    private fun Boolean.asCode() = if (this) 1 else 0

    override fun reduce(snapshot: HeadphonesSnapshot, command: MbbCommand): HeadphonesSnapshot = snapshot.apply5i(command)

    override fun isDualConnectChangeEvent(command: MbbCommand): Boolean =
        command.serviceId == 43 && command.commandId == 54

    private fun batteryStateRequest() = MbbCommand(
        1,
        8,
        mapOf(1 to byteArrayOf(), 2 to byteArrayOf(), 3 to byteArrayOf()),
    )
    override fun ancStateRequest() = MbbCommand(
        43,
        42,
        mapOf(1 to byteArrayOf(), 2 to byteArrayOf()),
    )
    override fun equalizerStateRequest() =
        MbbCommand(43, 74, (1..8).associateWith { byteArrayOf() })
    override fun autoPauseStateRequest() = MbbCommand(43, 17, mapOf(1 to byteArrayOf()))
    override fun lowLatencyStateRequest() = MbbCommand(43, 108, mapOf(2 to byteArrayOf()))
    override fun ldacStateRequest() = MbbCommand(43, 163)
    override fun dualConnectStateRequest() = MbbCommand(43, 47, mapOf(1 to byteArrayOf()))
    override fun doubleTapStateRequest() = MbbCommand(
        1,
        32,
        mapOf(1 to byteArrayOf(), 2 to byteArrayOf()),
    )
    override fun tripleTapStateRequest() = MbbCommand(
        1,
        38,
        mapOf(1 to byteArrayOf(), 2 to byteArrayOf()),
    )
    override fun holdStateRequest() = MbbCommand(
        43,
        23,
        mapOf(1 to byteArrayOf(), 2 to byteArrayOf()),
    )
    override fun holdModesStateRequest() = MbbCommand(
        43,
        25,
        mapOf(1 to byteArrayOf(), 2 to byteArrayOf()),
    )
    override fun swipeStateRequest() = MbbCommand(
        43,
        31,
        mapOf(1 to byteArrayOf(), 2 to byteArrayOf()),
    )
}

private fun HeadphonesSnapshot.apply5i(command: MbbCommand): HeadphonesSnapshot {
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
            hold = Hold.entries.firstOrNull { it.code == (arg(1) ?: arg(2)) },
        ))
        command.serviceId == 43 && command.commandId == 25 -> copy(settings = settings.copy(holdModes = when (arg(1) ?: arg(2)) {
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
        command.serviceId == 1 && command.commandId == 7 -> {
            fun text(id: Int): String? = command.args[id]
                ?.takeIf { it.isNotEmpty() }
                ?.toString(Charsets.UTF_8)
                ?.trim { c -> c <= ' ' || c == '\u0000' }
                ?.takeIf { it.isNotEmpty() && it.all { c -> c.code in 32..126 } }

            var left: String? = null
            var right: String? = null
            text(24)?.takeIf { it.startsWith("L-") && it.contains(',') }?.let { pair ->
                pair.split(',').let {
                    left = it.getOrNull(0)?.removePrefix("L-")
                    right = it.getOrNull(1)?.removePrefix("R-")
                }
            }
            fun hex(bytes: ByteArray): String =
                bytes.joinToString("") { "%02X".format(it.toInt().and(0xFF)) }
            fun hex(id: Int): String? = command.args[id]
                ?.takeIf { it.isNotEmpty() }
                ?.let(::hex)
            fun diagnosticValue(bytes: ByteArray): String {
                val allPrintableAscii = bytes.all { byte -> (byte.toInt() and 0xFF) in 32..126 }
                if (allPrintableAscii) return bytes.toString(Charsets.US_ASCII)

                val printablePart = buildString {
                    bytes.forEach { byte ->
                        val value = byte.toInt() and 0xFF
                        if (value in 32..126) append(value.toChar())
                    }
                }
                    .trim()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")

                val rawHex = "0x${hex(bytes)}"
                return if (printablePart.isEmpty()) rawHex else "$rawHex (\"$printablePart\")"
            }
            val knownFields = setOf(3, 7, 9, 10, 15, 24)
            val unknownFields = command.args
                .asSequence()
                .filter { (id, value) -> id !in knownFields && value.isNotEmpty() }
                .sortedBy { it.key }
                .associate { (id, value) -> id to diagnosticValue(value) }
            val previous = deviceInfo ?: DeviceInfo()
            val info = DeviceInfo(
                model = text(15) ?: previous.model,
                subModel = text(10) ?: hex(10) ?: previous.subModel,
                firmware = text(7) ?: previous.firmware,
                serialNumber = text(9) ?: previous.serialNumber,
                hardwareVersion = text(3) ?: previous.hardwareVersion,
                leftSerial = left ?: previous.leftSerial,
                rightSerial = right ?: previous.rightSerial,
                unknownFields = previous.unknownFields + unknownFields,
            )
            if (info == DeviceInfo()) this else copy(deviceInfo = info)
        }
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

