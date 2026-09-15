package io.github.razziell.freebuddynext.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.datastore.preferences.core.Preferences
import io.github.razziell.freebuddynext.R

/** Stable persisted IDs; custom RGB stays available when switching back to the accent. */
internal object WidgetColorMode {
    const val DEFAULT = 0
    const val ACCENT = 1
    const val CUSTOM = 2
    fun normalize(mode: Int) = mode.takeIf { it in DEFAULT..CUSTOM } ?: DEFAULT
}

/** Separate mode space: background/element IDs must not change. */
internal object WidgetTextColorMode {
    const val AUTO = 0
    const val WHITE = 1
    const val BLACK = 2
    const val ACCENT = 3
    const val CUSTOM = 4
    fun normalize(mode: Int) = mode.takeIf { it in AUTO..CUSTOM } ?: AUTO
    fun resolve(mode: Int, automatic: Int, accent: Int, custom: Int): Int = when (normalize(mode)) {
        WHITE -> 0xFFFFFFFF.toInt()
        BLACK -> 0xFF000000.toInt()
        ACCENT -> WidgetColorHex.opaque(accent)
        CUSTOM -> WidgetColorHex.opaque(custom)
        else -> WidgetColorHex.opaque(automatic)
    }
}

internal object WidgetColorHex {
    fun opaque(color: Int): Int = color or (0xFF shl 24)
    fun format(color: Int): String = "#%06X".format(java.util.Locale.ROOT, color and 0xFFFFFF)
    fun parse(text: String): Int? {
        val hex = text.trim().removePrefix("#")
        if (hex.length != 6 || hex.any { it !in "0123456789abcdefABCDEF" }) return null
        return opaque(hex.toInt(16))
    }
}

internal data class WidgetColors(
    val backgroundMode: Int = WidgetColorMode.DEFAULT,
    val backgroundRgb: Int = 0xFF202124.toInt(),
    val elementMode: Int = WidgetColorMode.DEFAULT,
    val elementRgb: Int = 0xFF82B1FF.toInt(),
    val textMode: Int = WidgetTextColorMode.AUTO,
    val textRgb: Int = 0xFFFFFFFF.toInt(),
)

internal data class WidgetPalette(val background: Int, val filled: Int, val empty: Int, val text: Int) {
    fun resource(id: Int): Int = when (id) {
        R.color.battery_widget_bar_background -> background
        R.color.battery_widget_bar_color -> filled
        R.color.battery_widget_empty_color -> empty
        R.color.battery_widget_text_color -> text
        else -> error("Not a widget palette resource: $id")
    }
}

private val LocalWidgetPalette = staticCompositionLocalOf<WidgetPalette?> { null }

internal fun WidgetColors.resolve(context: Context): WidgetPalette {
    val defaultBackground = context.getColor(R.color.battery_widget_bar_background)
    val accent = context.getColor(R.color.battery_widget_bar_color)
    val defaultEmpty = context.getColor(R.color.battery_widget_empty_color)
    val background = when (WidgetColorMode.normalize(backgroundMode)) {
        WidgetColorMode.ACCENT -> accent
        WidgetColorMode.CUSTOM -> WidgetColorHex.opaque(backgroundRgb)
        else -> defaultBackground
    }
    val filled = if (elementMode == WidgetColorMode.CUSTOM) WidgetColorHex.opaque(elementRgb) else accent
    // Match the element hue without making the inactive portion as strong as the fill.
    val empty = if (elementMode == WidgetColorMode.CUSTOM) {
        // Keep both regions on the same side of the black/white contrast crossover.
        // A pale fill over a dark inactive region would make either text color unreadable.
        val base = if (ColorUtils.calculateLuminance(filled) >= 0.179) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        ColorUtils.blendARGB(base, filled, 0.25f)
    } else defaultEmpty
    val automaticText = if (elementMode != WidgetColorMode.CUSTOM) {
        context.getColor(R.color.battery_widget_text_color)
    } else {
        // Choose the better worst-case contrast across both progress regions. Transparency
        // over an arbitrary wallpaper cannot be guaranteed to meet a contrast threshold.
        fun contrast(color: Int) = minOf(ColorUtils.calculateContrast(color, filled), ColorUtils.calculateContrast(color, empty))
        if (contrast(android.graphics.Color.BLACK) >= contrast(android.graphics.Color.WHITE)) android.graphics.Color.BLACK
        else android.graphics.Color.WHITE
    }
    val text = WidgetTextColorMode.resolve(textMode, automaticText, accent, textRgb)
    return WidgetPalette(background, filled, empty, text)
}

@Composable
internal fun WidgetPaletteProvider(context: Context, colors: WidgetColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWidgetPalette provides colors.resolve(context), content = content)
}

@Composable
internal fun widgetColor(context: Context, resource: Int): Int =
    LocalWidgetPalette.current?.resource(resource) ?: context.getColor(resource)

@Composable
internal fun widgetComposeColor(context: Context, resource: Int): Color = Color(widgetColor(context, resource))

internal fun Preferences.widgetColors(fallback: WidgetColors): WidgetColors = WidgetColors(
    backgroundMode = this[WidgetStorage.BACKGROUND_COLOR_MODE_KEY] ?: fallback.backgroundMode,
    backgroundRgb = this[WidgetStorage.BACKGROUND_RGB_KEY] ?: fallback.backgroundRgb,
    elementMode = this[WidgetStorage.ELEMENT_COLOR_MODE_KEY] ?: fallback.elementMode,
    elementRgb = this[WidgetStorage.ELEMENT_RGB_KEY] ?: fallback.elementRgb,
    textMode = this[WidgetStorage.TEXT_COLOR_MODE_KEY] ?: fallback.textMode,
    textRgb = this[WidgetStorage.TEXT_RGB_KEY] ?: fallback.textRgb,
)
