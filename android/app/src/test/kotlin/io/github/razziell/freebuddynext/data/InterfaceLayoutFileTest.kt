package io.github.razziell.freebuddynext.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InterfaceLayoutFileTest {
    @Test
    fun `round trip keeps shared order and scales`() {
        val source = InterfaceLayout(
            homeOrder = listOf(HomeBlock.ANC, HomeBlock.BATTERY, HomeBlock.HEADPHONES_IMAGE),
            settingsOrder = listOf(SettingsBlock.LDAC, SettingsBlock.EQUALIZER),
            imageScalePercent = 135,
            batteryScalePercent = 85,
            ancScalePercent = 120,
            hiddenHomeBlocks = setOf(HomeBlock.DUAL_CONNECT),
            hiddenSettingsBlocks = setOf(SettingsBlock.LDAC, SettingsBlock.FIND_EARBUDS),
        )
        val decoded = InterfaceLayoutFileCodec.decode(InterfaceLayoutFileCodec.encode(source)).layout
        assertEquals(
            listOf(
                HomeBlock.ANC,
                HomeBlock.BATTERY,
                HomeBlock.HEADPHONES_IMAGE,
                HomeBlock.DUAL_CONNECT,
                HomeBlock.SETTINGS_BUTTON,
            ),
            decoded.homeOrder,
        )
        assertEquals(SettingsBlock.LDAC, decoded.settingsOrder.first())
        assertEquals(SettingsBlock.EQUALIZER, decoded.settingsOrder[1])
        assertEquals(SettingsBlock.entries.size, decoded.settingsOrder.size)
        assertEquals(135, decoded.imageScalePercent)
        assertEquals(85, decoded.batteryScalePercent)
        assertEquals(120, decoded.ancScalePercent)
        assertEquals(setOf(HomeBlock.DUAL_CONNECT), decoded.hiddenHomeBlocks)
        assertEquals(
            setOf(SettingsBlock.LDAC, SettingsBlock.FIND_EARBUDS),
            decoded.hiddenSettingsBlocks,
        )
    }

    @Test
    fun `old layout files keep all blocks visible`() {
        val decoded = InterfaceLayoutFileCodec.decode(validJson()).layout
        assertEquals(emptySet<HomeBlock>(), decoded.hiddenHomeBlocks)
        assertEquals(emptySet<SettingsBlock>(), decoded.hiddenSettingsBlocks)
    }

    @Test
    fun `rejects unknown or duplicate hidden block IDs`() {
        val unknown = validJson().replace(
            "\n    }",
            ",\n        \"hiddenHomeBlocks\":[\"FUTURE_BLOCK\"]\n    }",
        )
        val duplicate = validJson().replace(
            "\n    }",
            ",\n        \"hiddenSettingsBlocks\":[\"LDAC\",\"LDAC\"]\n    }",
        )
        assertThrows(IllegalArgumentException::class.java) {
            InterfaceLayoutFileCodec.decode(unknown)
        }
        assertThrows(IllegalArgumentException::class.java) {
            InterfaceLayoutFileCodec.decode(duplicate)
        }
    }

    @Test
    fun `required home blocks cannot be hidden by imported layout`() {
        val json = validJson().replace(
            "\n    }",
            ",\n        \"hiddenHomeBlocks\":[\"HEADPHONES_IMAGE\",\"BATTERY\",\"SETTINGS_BUTTON\",\"ANC\"]\n    }",
        )
        val decoded = InterfaceLayoutFileCodec.decode(json).layout
        assertEquals(setOf(HomeBlock.ANC), decoded.hiddenHomeBlocks)
        assertEquals(emptySet<HomeBlock>(), decoded.hiddenHomeBlocks intersect REQUIRED_HOME_BLOCKS)
    }

    @Test
    fun `rejects duplicate and unknown layout IDs`() {
        val duplicate = validJson(home = "[\"BATTERY\",\"BATTERY\"]")
        val unknown = validJson(home = "[\"BATTERY\",\"FUTURE_BLOCK\"]")
        assertThrows(IllegalArgumentException::class.java) { InterfaceLayoutFileCodec.decode(duplicate) }
        assertThrows(IllegalArgumentException::class.java) { InterfaceLayoutFileCodec.decode(unknown) }
    }

    @Test
    fun `rejects invalid scales and format`() {
        assertThrows(IllegalArgumentException::class.java) {
            InterfaceLayoutFileCodec.decode(validJson(image = 101))
        }
        assertThrows(IllegalArgumentException::class.java) {
            InterfaceLayoutFileCodec.decode(validJson(format = "other"))
        }
    }

    @Test
    fun `rejects oversized files`() {
        val oversized = " ".repeat(InterfaceLayoutFileCodec.MAX_FILE_BYTES + 1)
        assertThrows(IllegalArgumentException::class.java) { InterfaceLayoutFileCodec.decode(oversized) }
    }

    @Test
    fun `uses a short stable file name`() {
        assertEquals("Layout_FBN.json", InterfaceLayoutFileCodec.FILE_NAME)
    }

    private fun validJson(
        format: String = InterfaceLayoutFileCodec.FORMAT,
        home: String = "[\"BATTERY\"]",
        image: Int = 100,
    ): String = """{
        "format":"$format",
        "version":1,
        "homeOrder":$home,
        "settingsOrder":["EQUALIZER"],
        "imageScalePercent":$image,
        "batteryScalePercent":100,
        "ancScalePercent":100
    }"""
}
