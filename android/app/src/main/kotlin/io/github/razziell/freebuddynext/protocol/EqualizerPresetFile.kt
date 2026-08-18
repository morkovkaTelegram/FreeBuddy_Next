package io.github.razziell.freebuddynext.protocol

import org.json.JSONArray
import org.json.JSONObject

data class EqualizerPresetFile(
    val model: String,
    val presets: List<CustomEqualizerPreset>,
)

object EqualizerPresetFileCodec {
    const val FORMAT = "freebuddy-next-equalizer-presets"
    const val VERSION = 1

    fun encode(model: String, presets: List<CustomEqualizerPreset>): String {
        val items = JSONArray()
        presets.forEach { preset ->
            items.put(JSONObject().apply {
                put("name", preset.name)
                put("bands", JSONArray(preset.bands))
            })
        }
        return JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("model", model)
            put("presets", items)
        }.toString(2)
    }

    fun decode(
        json: String,
        module: CustomEqualizerModule,
    ): EqualizerPresetFile {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) { "File is too large" }
        val root = JSONObject(json)
        require(root.optString("format") == FORMAT) { "Unsupported file format" }
        require(root.optInt("version", -1) == VERSION) { "Unsupported file version" }
        val model = root.optString("model")
        require(model == "freebuds_6i") { "The file is not for FreeBuds 6i" }
        val source = root.getJSONArray("presets")
        val capacity = module.lastPresetId - module.firstPresetId + 1
        require(source.length() in 1..capacity) { "The file must contain 1 to $capacity presets" }

        val presets = buildList {
            repeat(source.length()) { index ->
                val item = source.getJSONObject(index)
                val name = item.getString("name").trim()
                require(name.isNotEmpty()) { "Preset ${index + 1} has an empty name" }
                require(name.toByteArray(Charsets.UTF_8).size <= module.maxNameBytes) {
                    "Preset ${index + 1} name is too long"
                }
                val values = item.getJSONArray("bands")
                require(values.length() == module.bandCount) {
                    "Preset ${index + 1} must have ${module.bandCount} bands"
                }
                val bands = List(values.length()) { band -> values.getInt(band) }
                require(bands.all { it in module.minimum..module.maximum && it % module.step == 0 }) {
                    "Preset ${index + 1} contains an invalid band value"
                }
                add(CustomEqualizerPreset(module.firstPresetId + index, name, bands))
            }
        }
        return EqualizerPresetFile(model, presets)
    }

    const val MAX_FILE_BYTES = 64 * 1024
}
