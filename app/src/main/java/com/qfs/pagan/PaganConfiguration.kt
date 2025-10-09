package com.qfs.pagan

import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONParser
import kotlinx.serialization.Serializable
import java.io.File
@Serializable
class PaganConfiguration(
    soundfont: String? = null,
    relative_mode: Boolean = false,
    sample_rate: Int = 32000,
    move_mode: MoveMode = MoveMode.COPY,
    clip_same_line_release: Boolean = true,
    use_preferred_soundfont: Boolean = true,
    force_orientation: Int = ActivityInfo.SCREEN_ORIENTATION_USER,
    allow_std_percussion: Boolean = false,
    project_directory: Uri? = null,
    soundfont_directory: Uri? = null,
    night_mode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    indent_json: Boolean = false
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
                content?.let { this.from_json(it) } ?: PaganConfiguration()
            } else {
                PaganConfiguration()
            }
        }
    }

    var callbacks_soundfont = mutableListOf<(String?) -> Unit>()
    var soundfont: String? = soundfont
        set(value) {
            val original = field
            field = value
            this.callbacks_soundfont.forEach { if (original != value) { it(value) } }
        }

    var callbacks_sample_rate = mutableListOf<(Int) -> Unit>()
    var sample_rate: Int = sample_rate
        set(value) {
            val original = field
            field = value
            this.callbacks_sample_rate.forEach { if (original != value) { it(value) } }
        }

    var callbacks_relative_mode = mutableListOf<(Boolean) -> Unit>()
    var relative_mode: Boolean = relative_mode
        set(value) {
            val original = field
            field = value
            this.callbacks_relative_mode.forEach { if (original != value) { it(value) } }
        }

    var callbacks_move_mode = mutableListOf<(MoveMode) -> Unit>()
    var move_mode: MoveMode = move_mode
        set(value) {
            val original = field
            field = value
            this.callbacks_move_mode.forEach { if (original != value) { it(value) } }
        }

    var callbacks_clip_same_line_release = mutableListOf<(Boolean) -> Unit>()
    var clip_same_line_release: Boolean = clip_same_line_release
        set(value) {
            val original = field
            field = value
            this.callbacks_clip_same_line_release.forEach { if (original != value) { it(value) } }
        }

    var callbacks_use_preferred_soundfont = mutableListOf<(Boolean) -> Unit>()
    var use_preferred_soundfont: Boolean = use_preferred_soundfont
        set(value) {
            val original = field
            field = value
            this.callbacks_use_preferred_soundfont.forEach { if (original != value) { it(value) } }
        }

    var callbacks_allow_std_percussion = mutableListOf<(Boolean) -> Unit>()
    var allow_std_percussion: Boolean = allow_std_percussion
        set(value) {
            val original = field
            field = value
            this.callbacks_allow_std_percussion.forEach { if (original != value) { it(value) } }
        }

    var callbacks_indent_json = mutableListOf<(Boolean) -> Unit>()
    var indent_json: Boolean = indent_json
        set(value) {
            val original = field
            field = value
            this.callbacks_indent_json.forEach { if (original != value) { it(value) } }
        }

    var callbacks_night_mode = mutableListOf<(Int) -> Unit>()
    var night_mode: Int = night_mode
        set(value) {
            val original = field
            field = value
            this.callbacks_night_mode.forEach { if (original != value) { it(value) } }
        }

    var callbacks_force_orientation = mutableListOf<(Int) -> Unit>()
    var force_orientation: Int = force_orientation
        set(value) {
            val original = field
            field = value
            this.callbacks_force_orientation.forEach { if (original != value) { it(value) } }
        }

    var callbacks_project_directory = mutableListOf<(Uri?) -> Unit>()
    var project_directory: Uri? = project_directory
        set(value) {
            val original = field
            field = value
            this.callbacks_project_directory.forEach { if (original != value) { it(value) } }
        }

    var callbacks_soundfont_directory = mutableListOf<(Uri?) -> Unit>()
    var soundfont_directory: Uri? = soundfont_directory
        set(value) {
            val original = field
            field = value
            this.callbacks_soundfont_directory.forEach { if (original != value) { it(value) } }
        }

    fun save(path: String) {
        File(path).writeText(this.to_json().to_string())
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
