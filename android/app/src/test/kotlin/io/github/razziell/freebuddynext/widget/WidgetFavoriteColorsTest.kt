package io.github.razziell.freebuddynext.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WidgetFavoriteColorsTest {
    private val pink = 0xFFF21D4D.toInt()
    private val blue = 0xFF82B1FF.toInt()

    @Test fun emptyStorageHasNoFavorites() {
        assertEquals(emptyList<Int>(), WidgetFavoriteColors.decode(""))
    }

    @Test fun addPreservesInsertionOrder() {
        assertEquals(listOf(pink, blue), WidgetFavoriteColors.add(listOf(pink), blue))
    }

    @Test fun duplicatesIgnoreAlphaAndDoNotReorder() {
        assertEquals(listOf(pink, blue), WidgetFavoriteColors.add(listOf(pink, blue), 0x12F21D4D))
    }

    @Test fun storageRoundTripKeepsExactRgb() {
        val colors = listOf(pink, blue, 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF010203.toInt())
        assertEquals(colors, WidgetFavoriteColors.decode(WidgetFavoriteColors.encode(colors)))
    }

    @Test fun malformedEntriesAreIgnored() {
        assertEquals(listOf(pink, blue), WidgetFavoriteColors.decode("bad,,#F21D4D,#GGGGGG,82b1ff,#f21d4d"))
    }

    @Test fun fullPaletteDoesNotEvictSavedColors() {
        val full = (0 until WidgetFavoriteColors.LIMIT).map(WidgetColorHex::opaque)
        assertEquals(full, WidgetFavoriteColors.add(full, pink))
    }

    @Test fun removingColorIgnoresAlpha() {
        assertEquals(listOf(blue), WidgetFavoriteColors.remove(listOf(pink, blue), 0x00F21D4D))
    }

    @Test fun removingAbsentColorDoesNotChangePalette() {
        assertEquals(listOf(blue), WidgetFavoriteColors.remove(listOf(blue), pink))
    }

    @Test fun removedSlotCanBeReused() {
        val full = (0 until WidgetFavoriteColors.LIMIT).map(WidgetColorHex::opaque)
        val result = WidgetFavoriteColors.add(WidgetFavoriteColors.remove(full, full[0]), pink)
        assertEquals(WidgetFavoriteColors.LIMIT, result.size)
        assertEquals(pink, result.last())
        assertFalse(full[0] in result)
    }

    @Test fun decodingCapsOversizedLists() {
        val encoded = (0..100).joinToString(",") { WidgetColorHex.format(it) }
        assertEquals(WidgetFavoriteColors.LIMIT, WidgetFavoriteColors.decode(encoded).size)
    }
}
