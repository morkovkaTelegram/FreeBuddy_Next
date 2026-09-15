package io.github.razziell.freebuddynext.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetTextColorTest {
    private val automatic = 0xFF123456.toInt()
    private val accent = 0xFF82B1FF.toInt()
    private val custom = 0xFFF21D4D.toInt()
    private fun resolve(mode: Int) = WidgetTextColorMode.resolve(mode, automatic, accent, custom)

    @Test fun missingSettingKeepsAutomaticColor() {
        assertEquals(WidgetTextColorMode.AUTO, WidgetColors().textMode)
        assertEquals(automatic, resolve(WidgetColors().textMode))
    }
    @Test fun explicitWhiteAndBlackIgnorePalette() {
        assertEquals(0xFFFFFFFF.toInt(), resolve(WidgetTextColorMode.WHITE))
        assertEquals(0xFF000000.toInt(), resolve(WidgetTextColorMode.BLACK))
    }
    @Test fun accentUsesSystemAccentRatherThanCustomElementColor() {
        assertEquals(accent, resolve(WidgetTextColorMode.ACCENT))
    }
    @Test fun customUsesSavedRgb() {
        assertEquals(custom, resolve(WidgetTextColorMode.CUSTOM))
    }
    @Test fun textAlwaysRemainsOpaque() {
        assertEquals(0xFF123456.toInt(), WidgetTextColorMode.resolve(
            WidgetTextColorMode.CUSTOM, automatic, accent, 0x00123456))
        assertEquals(0xFF123456.toInt(), WidgetTextColorMode.resolve(
            WidgetTextColorMode.ACCENT, automatic, 0x80123456.toInt(), custom))
    }
    @Test fun invalidModeFallsBackToAutomatic() {
        assertEquals(automatic, resolve(-1))
        assertEquals(automatic, resolve(99))
        for (mode in 0..4) assertEquals(mode, WidgetTextColorMode.normalize(mode))
    }
    @Test fun changingTextDoesNotChangeBackgroundOrElements() {
        val original = WidgetColors(backgroundMode = 2, backgroundRgb = 0xFF345678.toInt(),
            elementMode = 2, elementRgb = 0xFF987654.toInt())
        val changed = original.copy(textMode = WidgetTextColorMode.CUSTOM, textRgb = custom)
        assertEquals(original, changed.copy(textMode = original.textMode, textRgb = original.textRgb))
    }
    @Test fun switchingModeRetainsCustomTextRgb() {
        val original = WidgetColors(textMode = WidgetTextColorMode.CUSTOM, textRgb = custom)
        assertEquals(custom, original.copy(textMode = WidgetTextColorMode.WHITE).textRgb)
    }
}
