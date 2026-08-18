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
            "freebuds_6i",
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
        assertThrows(IllegalArgumentException::class.java) { EqualizerPresetFileCodec.decode(json, "freebuds_6i", module) }
    }

    @Test
    fun `rejects invalid band count and value`() {
        val short = """{"format":"${EqualizerPresetFileCodec.FORMAT}","version":1,"model":"freebuds_6i","presets":[{"name":"A","bands":[0]}]}"""
        val invalid = """{"format":"${EqualizerPresetFileCodec.FORMAT}","version":1,"model":"freebuds_6i","presets":[{"name":"A","bands":[61,0,0,0,0,0,0,0,0,0]}]}"""
        assertThrows(IllegalArgumentException::class.java) { EqualizerPresetFileCodec.decode(short, "freebuds_6i", module) }
        assertThrows(IllegalArgumentException::class.java) { EqualizerPresetFileCodec.decode(invalid, "freebuds_6i", module) }
    }

    @Test
    fun `rejects empty file and too many presets`() {
        val empty = """{"format":"${EqualizerPresetFileCodec.FORMAT}","version":1,"model":"freebuds_6i","presets":[]}"""
        val one = """{"name":"A","bands":[0,0,0,0,0,0,0,0,0,0]}"""
        val many = """{"format":"${EqualizerPresetFileCodec.FORMAT}","version":1,"model":"freebuds_6i","presets":[$one,$one,$one,$one]}"""
        assertThrows(IllegalArgumentException::class.java) { EqualizerPresetFileCodec.decode(empty, "freebuds_6i", module) }
        assertThrows(IllegalArgumentException::class.java) { EqualizerPresetFileCodec.decode(many, "freebuds_6i", module) }
    }

    @Test
    fun `rejects name longer than UTF8 limit`() {
        val json = EqualizerPresetFileCodec.encode(
            "freebuds_6i",
            listOf(CustomEqualizerPreset(100, "Очень длинное название", List(10) { 0 })),
        )
        assertThrows(IllegalArgumentException::class.java) { EqualizerPresetFileCodec.decode(json, "freebuds_6i", module) }
    }
    @Test
    fun `SE 4 ANC preset files round trip and stay model scoped`() {
        val source = listOf(CustomEqualizerPreset(100, "SE preset", List(10) { 5 }))
        val json = EqualizerPresetFileCodec.encode("freebuds_se_4_anc", source)
        val decoded = EqualizerPresetFileCodec.decode(json, "freebuds_se_4_anc", module)
        assertEquals("freebuds_se_4_anc", decoded.model)
        assertEquals(source.first().name, decoded.presets.first().name)
        assertEquals(source.first().bands, decoded.presets.first().bands)
        assertThrows(IllegalArgumentException::class.java) {
            EqualizerPresetFileCodec.decode(json, "freebuds_6i", module)
        }
        assertEquals(
            "EQ_SE4_ANC_FBN.json",
            EqualizerPresetFileCodec.suggestedFileName("freebuds_se_4_anc"),
        )
        assertEquals(
            "EQ_6i_FBN.json",
            EqualizerPresetFileCodec.suggestedFileName("freebuds_6i"),
        )
    }

}
