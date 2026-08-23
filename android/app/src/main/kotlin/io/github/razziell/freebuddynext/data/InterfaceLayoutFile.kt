package io.github.razziell.freebuddynext.data

import org.json.JSONArray
import org.json.JSONObject

data class InterfaceLayoutFile(val layout: InterfaceLayout)

/** Portable, device-independent interface layout backup. */
object InterfaceLayoutFileCodec {
    const val FORMAT = "freebuddy-next-interface-layout"
    const val VERSION = 1
    const val FILE_NAME = "Layout_FBN.json"
    const val MAX_FILE_BYTES = 32 * 1024

    fun encode(layout: InterfaceLayout): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("homeOrder", JSONArray(normalizeOrder(layout.homeOrder, HomeBlock.entries).map { it.name }))
        put(
            "settingsOrder",
            JSONArray(normalizeOrder(layout.settingsOrder, SettingsBlock.entries).map { it.name }),
        )
        put("imageScalePercent", layout.imageScalePercent)
        put("batteryScalePercent", layout.batteryScalePercent)
        put("ancScalePercent", layout.ancScalePercent)
        put(
            "hiddenHomeBlocks",
            JSONArray(layout.hiddenHomeBlocks.filterNot { it in REQUIRED_HOME_BLOCKS }.map { it.name }),
        )
        put("hiddenSettingsBlocks", JSONArray(layout.hiddenSettingsBlocks.map { it.name }))
    }.toString(2)

    fun decode(json: String): InterfaceLayoutFile {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) { "File is too large" }
        val root = JSONObject(json)
        require(root.optString("format") == FORMAT) { "Unsupported file format" }
        require(root.optInt("version", -1) == VERSION) { "Unsupported file version" }

        val homeOrder = parseEnumArray<HomeBlock>(root.getJSONArray("homeOrder"))
        val settingsOrder = parseEnumArray<SettingsBlock>(root.getJSONArray("settingsOrder"))
        val imageScale = root.getInt("imageScalePercent")
        val batteryScale = root.getInt("batteryScalePercent")
        val ancScale = root.getInt("ancScalePercent")
        val hiddenHome = root.optJSONArray("hiddenHomeBlocks")
            ?.let { parseEnumArray<HomeBlock>(it, allowEmpty = true).toSet() }
            .orEmpty() - REQUIRED_HOME_BLOCKS
        val hiddenSettings = root.optJSONArray("hiddenSettingsBlocks")
            ?.let { parseEnumArray<SettingsBlock>(it, allowEmpty = true).toSet() }
            ?: emptySet()

        require(imageScale in 70..140 && imageScale % 5 == 0) { "Invalid image scale" }
        require(batteryScale in 80..140 && batteryScale % 5 == 0) { "Invalid battery scale" }
        require(ancScale in 80..125 && ancScale % 5 == 0) { "Invalid ANC scale" }

        return InterfaceLayoutFile(
            InterfaceLayout(
                homeOrder = normalizeOrder(homeOrder, HomeBlock.entries),
                settingsOrder = normalizeOrder(settingsOrder, SettingsBlock.entries),
                imageScalePercent = imageScale,
                batteryScalePercent = batteryScale,
                ancScalePercent = ancScale,
                hiddenHomeBlocks = hiddenHome,
                hiddenSettingsBlocks = hiddenSettings,
            ),
        )
    }

    private inline fun <reified T : Enum<T>> parseEnumArray(
        source: JSONArray,
        allowEmpty: Boolean = false,
    ): List<T> {
        val minimum = if (allowEmpty) 0 else 1
        require(source.length() in minimum..enumValues<T>().size) { "Invalid layout order" }
        val values = List(source.length()) { index ->
            val name = source.getString(index)
            enumValues<T>().firstOrNull { it.name == name }
                ?: throw IllegalArgumentException("Unknown layout item: $name")
        }
        require(values.distinct().size == values.size) { "Duplicate layout item" }
        return values
    }
}
