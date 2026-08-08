package com.lastgimbus.the.freebuddy.protocol

import java.io.ByteArrayOutputStream

data class MbbCommand(
    val serviceId: Int,
    val commandId: Int,
    val args: Map<Int, ByteArray> = emptyMap(),
) {
    fun toPayload(): ByteArray {
        val data = ByteArrayOutputStream()
        args.forEach { (key, value) ->
            require(key in 0..255 && value.size <= 255)
            data.write(key)
            data.write(value.size)
            data.write(value)
        }
        val dataBytes = data.toByteArray()
        val body = byteArrayOf(
            0x5A, 0x00, (dataBytes.size + 3).toByte(), 0x00,
            serviceId.toByte(), commandId.toByte(),
        ) + dataBytes
        val crc = crc16Xmodem(body)
        return body + byteArrayOf((crc ushr 8).toByte(), crc.toByte())
    }

    companion object {
        fun parse(payload: ByteArray): MbbCommand {
            require(payload.size >= 8) { "MBB packet is too short" }
            require(payload[0].u() == 0x5A && payload[1].u() == 0 && payload[3].u() == 0) {
                "Invalid MBB header"
            }
            require(payload.size == payload[2].u() + 5) { "Invalid MBB length" }
            val expected = crc16Xmodem(payload.copyOf(payload.size - 2))
            val actual = (payload[payload.lastIndex - 1].u() shl 8) or payload.last().u()
            require(expected == actual) { "Invalid MBB checksum" }

            val args = linkedMapOf<Int, ByteArray>()
            var offset = 6
            val end = payload.size - 2
            while (offset < end) {
                require(offset + 2 <= end) { "Truncated MBB argument" }
                val id = payload[offset++].u()
                val length = payload[offset++].u()
                require(offset + length <= end) { "Invalid MBB argument length" }
                args[id] = payload.copyOfRange(offset, offset + length)
                offset += length
            }
            return MbbCommand(payload[4].u(), payload[5].u(), args)
        }
    }
}

class MbbStreamParser {
    private var pending = ByteArray(0)

    fun append(bytes: ByteArray): List<MbbCommand> {
        pending += bytes
        val result = mutableListOf<MbbCommand>()
        while (pending.size >= 3) {
            val magicIndex = pending.indexOfFirst { it.u() == 0x5A }
            if (magicIndex < 0) {
                pending = ByteArray(0)
                break
            }
            if (magicIndex > 0) pending = pending.copyOfRange(magicIndex, pending.size)
            if (pending.size < 3) break
            val packetLength = pending[2].u() + 5
            if (packetLength !in 8..260) {
                pending = pending.copyOfRange(1, pending.size)
                continue
            }
            if (pending.size < packetLength) break
            val packet = pending.copyOfRange(0, packetLength)
            pending = pending.copyOfRange(packetLength, pending.size)
            runCatching { MbbCommand.parse(packet) }
                .onSuccess { if (!(it.serviceId == 10 && it.commandId == 13)) result += it }
        }
        return result
    }

    fun clear() { pending = ByteArray(0) }
}

private fun Byte.u() = toInt() and 0xFF
fun ByteArray.firstUOrNull() = firstOrNull()?.toInt()?.and(0xFF)
fun intBytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

fun crc16Xmodem(data: ByteArray): Int {
    var crc = 0
    for (byte in data) {
        crc = crc xor (byte.u() shl 8)
        repeat(8) {
            crc = if ((crc and 0x8000) != 0) (crc shl 1) xor 0x1021 else crc shl 1
            crc = crc and 0xFFFF
        }
    }
    return crc
}
