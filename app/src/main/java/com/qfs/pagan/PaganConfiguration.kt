package com.qfs.pagan

import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONParser
import java.io.File

data class PaganConfiguration(
    var soundfont: String? = null,
    var relative_mode: Boolean = false,
    var sample_rate: Int = 32000,
    var move_mode: MoveMode = MoveMode.COPY,
    var clip_same_line_release: Boolean = true,
    var use_preferred_soundfont: Boolean = true,
    var force_orientation: Int = ActivityInfo.SCREEN_ORIENTATION_USER,
    var allow_std_percussion: Boolean = false,
    var project_directory: Uri? = null,
    var soundfont_directory: Uri? = null,
    var night_mode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    var indent_json: Boolean = false
) {
    enum class MoveMode {
        MOVE,
        COPY,
        MERGE
    }

    companion object {
        fun from_json(content: JSONHashMap): PaganConfiguration {
            return PaganConfiguration(
                soundfont = content.get_stringn("soundfont2"),
                sample_rate = content.get_int("sample_rate", 32000),
                relative_mode = content.get_boolean("relative_mode", false),
                move_mode = MoveMode.valueOf(content.get_string("move_mode", "COPY")),
                clip_same_line_release = content.get_boolean("clip_same_line_release", true),
                use_preferred_soundfont = content.get_boolean("use_preferred_soundfont", true),
                force_orientation = content.get_int("force_orientation", ActivityInfo.SCREEN_ORIENTATION_USER),
                allow_std_percussion = content.get_boolean("allow_std_percussion", false),
                project_directory = content.get_stringn("project_directory")?.toUri(),
                soundfont_directory = content.get_stringn("soundfont_directory")?.toUri(),
                night_mode = content.get_int("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
                indent_json = content.get_boolean("indent_json", false)
            )
        }

        fun from_path(path: String): PaganConfiguration {
            val file = File(path)
            return if (file.exists()) {
                val string = file.readText()
                val content = JSONParser.parse<JSONHashMap>(string)
                if (content == null) {
                    PaganConfiguration()
                } else {
                    this.from_json(content)
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
        output["soundfont2"] = this.soundfont
        output["sample_rate"] = this.sample_rate
        output["relative_mode"] = this.relative_mode
        output["move_mode"] = this.move_mode.name
        output["clip_same_line_release"] = this.clip_same_line_release
        output["use_preferred_soundfont"] = this.use_preferred_soundfont
        output["force_orientation"] = this.force_orientation
        output["allow_std_percussion"] = this.allow_std_percussion
        output["project_directory"] = this.project_directory?.toString()
        output["soundfont_directory"] = this.soundfont_directory?.toString()
        output["night_mode"] = this.night_mode
        output["indent_json"] = this.indent_json
        return output
    }
}
