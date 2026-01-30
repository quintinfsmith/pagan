package com.qfs.pagan

import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONParser
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
class PaganConfiguration(
    soundfont: String? = null,
    sample_rate: Int = 32000,
    move_mode: MoveMode = MoveMode.COPY,
    clip_same_line_release: Boolean = true,
    use_preferred_soundfont: Boolean = true,
    force_orientation: Int = ActivityInfo.SCREEN_ORIENTATION_USER,
    allow_std_percussion: Boolean = false,
    project_directory: Uri? = null,
    soundfont_directory: Uri? = null,
    night_mode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    indent_json: Boolean = false,
    latest_input_indicator: Boolean = true
) {
    val soundfont: MutableState<String?> = mutableStateOf(soundfont)
    val sample_rate: MutableState<Int> = mutableStateOf(sample_rate)
    val move_mode: MutableState<MoveMode> = mutableStateOf(move_mode)
    val clip_same_line_release: MutableState<Boolean> = mutableStateOf(clip_same_line_release)
    val use_preferred_soundfont: MutableState<Boolean> = mutableStateOf(use_preferred_soundfont)
    val force_orientation: MutableState<Int> = mutableStateOf(force_orientation)
    val allow_std_percussion: MutableState<Boolean> = mutableStateOf(allow_std_percussion)
    val project_directory: MutableState<Uri?> = mutableStateOf(project_directory)
    val soundfont_directory: MutableState<Uri?> = mutableStateOf(soundfont_directory)
    val night_mode: MutableState<Int> = mutableStateOf(night_mode)
    val indent_json: MutableState<Boolean> = mutableStateOf(indent_json)
    val latest_input_indicator: MutableState<Boolean> = mutableStateOf(latest_input_indicator)


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
                move_mode = MoveMode.valueOf(content.get_string("move_mode", "COPY")),
                clip_same_line_release = content.get_boolean("clip_same_line_release", true),
                use_preferred_soundfont = content.get_boolean("use_preferred_soundfont", true),
                force_orientation = content.get_int("force_orientation", ActivityInfo.SCREEN_ORIENTATION_USER),
                allow_std_percussion = content.get_boolean("allow_std_percussion", false),
                project_directory = content.get_stringn("project_directory")?.toUri(),
                soundfont_directory = content.get_stringn("soundfont_directory")?.toUri(),
                night_mode = content.get_int("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
                indent_json = content.get_boolean("indent_json", false),
                latest_input_indicator = content.get_boolean("latest_input_indicator", true)
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

    fun update_from_path(path: String) {
        val config = PaganConfiguration.from_path(path)
        this.soundfont.value = config.soundfont.value
        this.sample_rate.value = config.sample_rate.value
        this.move_mode.value = config.move_mode.value
        this.clip_same_line_release.value = config.clip_same_line_release.value
        this.use_preferred_soundfont.value = config.use_preferred_soundfont.value
        this.force_orientation.value = config.force_orientation.value
        this.allow_std_percussion.value = config.allow_std_percussion.value
        this.project_directory.value = config.project_directory.value
        this.soundfont_directory.value = config.soundfont_directory.value
        this.night_mode.value = config.night_mode.value
        this.indent_json.value = config.indent_json.value
        this.latest_input_indicator.value = config.latest_input_indicator.value
    }

    fun save(path: String) {
        File(path).writeText(this.to_json().to_string())
    }

    fun to_json(): JSONHashMap {
        val output = JSONHashMap()
        output["soundfont2"] = this.soundfont.value
        output["sample_rate"] = this.sample_rate.value
        output["move_mode"] = this.move_mode.value.name
        output["clip_same_line_release"] = this.clip_same_line_release.value
        output["use_preferred_soundfont"] = this.use_preferred_soundfont.value
        output["force_orientation"] = this.force_orientation.value
        output["allow_std_percussion"] = this.allow_std_percussion.value
        output["project_directory"] = this.project_directory.value?.toString()
        output["soundfont_directory"] = this.soundfont_directory.value?.toString()
        output["night_mode"] = this.night_mode.value
        output["indent_json"] = this.indent_json.value
        output["latest_input_indicator"] = this.latest_input_indicator.value
        // output["channel_colors"] = JSONList(*Array(this.channel_colors.size) {
        //     JSONString(this.channel_colors[it].toHexString(HexFormat.Default))
        // })
        return output
    }
}
