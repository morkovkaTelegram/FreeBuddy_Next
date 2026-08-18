package io.github.razziell.freebuddynext.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EqualizerPresetFileTest {
    private val module = HuaweiCustomEqualizerModule

    @Test
    fun `round trip keeps names and bands and assigns portable slots`() {
        val source = listOf(
            CustomEqualizerPreset(102, "Rock", List(10) { it * 5 - 20 }),
            CustomEqualizerPreset(100, "Мой звук", List(10) { 0 }),
        )
        val decoded = EqualizerPresetFileCodec.decode(
            EqualizerPresetFileCodec.encode("freebuds_6i", source),
            module,
        )
        assertEquals("freebuds_6i", decoded.model)
        assertEquals(listOf(100, 101), decoded.presets.map { it.id })
        assertEquals(source.map { it.name }, decoded.presets.map { it.name })
        assertEquals(source.map { it.bands }, decoded.presets.map { it.bands })
    }

    @Test
    fun `rejects incompatible model`() {
        val json = """{"format":"${EqualizerPresetFileCodec.FORMAT}","version":1,"model":"freebuds_5i","presets":[{"name":"A","bands":[0,0,0,0,0,0,0,0,0,0]}]}"""
        assertThrows(IllegalArgumentException::class.java) { EqualizerPresetFileCodec.decode(json, module) }
    }

    @Test
    fun `rejects invalid band count and value`() {
        val short = """{"format":"${EqualizerPresetFileCodec.FORMAT}","version":1,"model":"freebuds_6i","presets":[{"name":"A","bands":[0]}]}"""
        val invalid = """{"format":"${EqualizerPresetFileCodec.FORMAT}","version":1,"model":"freebuds_6i","presets":[{"name":"A","bands":[61,0,0,0,0,0,0,0,0,0]}]}"""
        assertThrows(IllegalArgumentException::class.java) { EqualizerPresetFileCodec.decode(short, module) }
        assertThrows(IllegalArgumentException::class.java) { EqualizerPresetFileCodec.decode(invalid, module) }
    }

    @Test
    fun `rejects empty file and too many presets`() {
        val empty = """{"format":"${EqualizerPresetFileCodec.FORMAT}","version":1,"model":"freebuds_6i","presets":[]}"""
        val one = """{"name":"A","bands":[0,0,0,0,0,0,0,0,0,0]}"""
        val many = """{"format":"${EqualizerPresetFileCodec.FORMAT}","version":1,"model":"freebuds_6i","presets":[$one,$one,$one,$one]}"""
        assertThrows(IllegalArgumentException::class.java) { EqualizerPresetFileCodec.decode(empty, module) }
        assertThrows(IllegalArgumentException::class.java) { EqualizerPresetFileCodec.decode(many, module) }
    }

    @Test
    fun `rejects name longer than UTF8 limit`() {
        val json = EqualizerPresetFileCodec.encode(
            "freebuds_6i",
            listOf(CustomEqualizerPreset(100, "Очень длинное название", List(10) { 0 })),
        )
        assertThrows(IllegalArgumentException::class.java) { EqualizerPresetFileCodec.decode(json, module) }
    }
}
