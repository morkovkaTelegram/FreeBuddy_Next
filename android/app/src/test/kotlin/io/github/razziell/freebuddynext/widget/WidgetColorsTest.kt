package io.github.razziell.freebuddynext.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetColorsTest {
    @Test fun parsesHexWithOrWithoutHash() {
        assertEquals(0xFF82B1FF.toInt(), WidgetColorHex.parse("#82b1ff"))
        assertEquals(0xFF82B1FF.toInt(), WidgetColorHex.parse("82B1FF"))
        assertEquals(0xFF82B1FF.toInt(), WidgetColorHex.parse(" #82B1FF "))
    }
    @Test fun preservesBlackAndWhite() {
        assertEquals(0xFF000000.toInt(), WidgetColorHex.parse("000000"))
        assertEquals(0xFFFFFFFF.toInt(), WidgetColorHex.parse("FFFFFF"))
    }
    @Test fun rejectsInvalidAndPartialInput() {
        for (value in listOf("", "#", "123", "#12345", "#1234567", "GG0000", "12 345", "##123456")) {
            assertNull(value, WidgetColorHex.parse(value))
        }
    }
    @Test fun alphaIsNotPartOfColorPicker() {
        assertNull(WidgetColorHex.parse("#8082B1FF"))
        assertEquals(0xFF123456.toInt(), WidgetColorHex.opaque(0x00123456))
        assertEquals(0xFF123456.toInt(), WidgetColorHex.opaque(0x80123456.toInt()))
    }
    @Test fun formatsCanonicalSixDigitHex() {
        assertEquals("#000001", WidgetColorHex.format(1))
        assertEquals("#82B1FF", WidgetColorHex.format(0xFF82B1FF.toInt()))
    }
    @Test fun formattingRoundTripsRgb() {
        for (rgb in listOf(0, 1, 0x123456, 0x82B1FF, 0xFFFFFF)) {
            assertEquals(WidgetColorHex.opaque(rgb), WidgetColorHex.parse(WidgetColorHex.format(rgb)))
        }
    }
    @Test fun unknownModePreservesDefaultAppearance() {
        assertEquals(WidgetColorMode.DEFAULT, WidgetColorMode.normalize(-1))
        assertEquals(WidgetColorMode.DEFAULT, WidgetColorMode.normalize(200))
        for (mode in 0..2) assertEquals(mode, WidgetColorMode.normalize(mode))
    }
    @Test fun changingModeKeepsCustomColorAndOtherTarget() {
        val colors = WidgetColors(backgroundMode = 2, backgroundRgb = 0xFF123456.toInt(), elementMode = 2)
        val accent = colors.copy(backgroundMode = 1)
        assertEquals(colors.backgroundRgb, accent.backgroundRgb)
        assertEquals(colors.elementMode, accent.elementMode)
        assertEquals(colors.elementRgb, accent.elementRgb)
    }
    @Test fun hueRingMatchesSweepGradientDirections() {
        assertEquals(0f, WidgetPickerGeometry.hue(1f, 0f), 0.01f)
        assertEquals(90f, WidgetPickerGeometry.hue(0f, 1f), 0.01f)
        assertEquals(180f, WidgetPickerGeometry.hue(-1f, 0f), 0.01f)
        assertEquals(270f, WidgetPickerGeometry.hue(0f, -1f), 0.01f)
    }
    @Test fun saturationAndValueClampAtEdges() {
        assertEquals(0f, WidgetPickerGeometry.component(-100f, -50f, 100f), 0f)
        assertEquals(0.5f, WidgetPickerGeometry.component(0f, -50f, 100f), 0f)
        assertEquals(1f, WidgetPickerGeometry.component(100f, -50f, 100f), 0f)
    }
    @Test fun zeroSizeDoesNotDivideByZero() {
        assertEquals(0f, WidgetPickerGeometry.component(10f, 0f, 0f), 0f)
    }
}
