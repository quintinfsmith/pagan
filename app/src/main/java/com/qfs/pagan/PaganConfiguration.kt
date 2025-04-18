package com.qfs.pagan

import android.content.pm.ActivityInfo
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONParser
import java.io.File

data class PaganConfiguration(
    var soundfont: String? = null,
    var relative_mode: Boolean = false,
    var sample_rate: Int = 22050,
    var show_percussion: Boolean = true, // Deprecated, use variable in view_model
    var move_mode: MoveMode = MoveMode.COPY,
    var clip_same_line_release: Boolean = true,
    var use_preferred_soundfont: Boolean = true,
    var force_orientation: Int = ActivityInfo.SCREEN_ORIENTATION_USER
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
                        sample_rate = content.get_int("sample_rate", 22050),
                        relative_mode = content.get_boolean("relative_mode", false),
                        move_mode = MoveMode.valueOf(content.get_string("move_mode", "COPY")),
                        clip_same_line_release = content.get_boolean("clip_same_line_release", true),
                        use_preferred_soundfont = content.get_boolean("use_preferred_soundfont", true),
                        force_orientation = content.get_int("force_orientation", ActivityInfo.SCREEN_ORIENTATION_USER)
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
        output["clip_same_line_release"] = this.clip_same_line_release
        output["use_preferred_soundfont"] = this.use_preferred_soundfont
        output["force_orientation"] = this.force_orientation

        return output
    }
}
