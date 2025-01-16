package com.qfs.pagan

import com.qfs.json.JSONHashMap
import com.qfs.json.JSONParser
import java.io.File

data class PaganConfiguration(
    var soundfont: String? = null,
    var relative_mode: Boolean = false,
    var sample_rate: Int = 22050,
    var show_percussion: Boolean = true, // Deprecated, use variable in view_model
    var move_mode: MoveMode = MoveMode.COPY
) {

    enum class MoveMode {
        MOVE,
        COPY,
        MERGE
    }
    companion object {
        fun from_path(path: String): PaganConfiguration {
            val file = File(path)
            return if (file.exists()) {
                val string = file.readText()
                val content = JSONParser.parse<JSONHashMap>(string)
                if (content == null) {
                    PaganConfiguration()
                } else {
                    PaganConfiguration(
                        soundfont = content.get_stringn("soundfont"),
                        sample_rate = content.get_intn("sample_rate") ?: 22050,
                        relative_mode = content.get_booleann("relative_mode") ?: false,
                        move_mode = MoveMode.valueOf(content.get_stringn("move_mode") ?: "COPY")
                    )
                }
            } else {
                PaganConfiguration()
            }
        }
    }

    fun save(path: String) {
        val json_map = this.to_json()
        val file = File(path)
        file.writeText(json_map.to_string())
    }

    fun to_json(): JSONHashMap {
        val output = JSONHashMap()
        output["soundfont"] = this.soundfont
        output["sample_rate"] = this.sample_rate
        output["relative_mode"] = this.relative_mode
        output["move_mode"] = this.move_mode.name
        return output
    }
}
