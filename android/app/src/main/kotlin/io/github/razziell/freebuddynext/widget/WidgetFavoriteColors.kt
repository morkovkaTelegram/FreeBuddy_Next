package io.github.razziell.freebuddynext.widget

/** Stable insertion order and opaque RGB identity; unrelated to per-widget settings. */
internal object WidgetFavoriteColors {
    const val LIMIT = 24

    fun normalize(colors: List<Int>): List<Int> =
        colors.map(WidgetColorHex::opaque).distinct().take(LIMIT)

    fun decode(value: String): List<Int> = normalize(
        value.take(4096).split(',').mapNotNull(WidgetColorHex::parse),
    )

    fun encode(colors: List<Int>): String = normalize(colors).joinToString(",", transform = WidgetColorHex::format)

    fun add(colors: List<Int>, color: Int): List<Int> = normalize(colors + color)

    fun remove(colors: List<Int>, color: Int): List<Int> =
        normalize(colors).filterNot { it == WidgetColorHex.opaque(color) }
}
