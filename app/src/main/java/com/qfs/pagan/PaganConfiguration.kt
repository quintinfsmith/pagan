/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONList
import com.qfs.json.JSONParser
import com.qfs.json.JSONString
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.math.min

@Serializable
class PaganConfiguration(
    soundfonts: Array<String> = arrayOf(),
    sample_rate: Int = 32000,
    move_mode: MoveMode = MoveMode.COPY,
    use_preferred_soundfont: Boolean = true,
    force_orientation: Int = ActivityInfo.SCREEN_ORIENTATION_USER,
    allow_std_percussion: Boolean = false,
    project_directory: Uri? = null,
    soundfont_directory: Uri? = null,
    night_mode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    indent_json: Boolean = false,
    latest_input_indicator: Boolean = true,
    normalize_beat_widths: Boolean = false,
    beat_stroke_thickness: Dp = 0.dp,
    allow_multiple_soundfonts: Boolean = false,
    soundfont_uris: Array<Uri> = arrayOf(),
    sort_load: Int = -3
) {
    val soundfonts: MutableState<Array<MutableState<String>>> = mutableStateOf(Array(if (allow_multiple_soundfonts) soundfonts.size else min(1, soundfonts.size)) { mutableStateOf(soundfonts[it]) })
    val soundfont_uris: MutableState<Array<MutableState<Uri>>> = mutableStateOf(Array(if (allow_multiple_soundfonts) soundfont_uris.size else min(1, soundfont_uris.size)) { mutableStateOf(soundfont_uris[it]) })
    val sample_rate: MutableState<Int> = mutableStateOf(sample_rate)
    val move_mode: MutableState<MoveMode> = mutableStateOf(move_mode)
    val use_preferred_soundfont: MutableState<Boolean> = mutableStateOf(use_preferred_soundfont)
    val force_orientation: MutableState<Int> = mutableStateOf(force_orientation)
    val allow_std_percussion: MutableState<Boolean> = mutableStateOf(allow_std_percussion)
    val project_directory: MutableState<Uri?> = mutableStateOf(project_directory)
    val soundfont_directory: MutableState<Uri?> = mutableStateOf(soundfont_directory)
    val night_mode: MutableState<Int> = mutableStateOf(night_mode)
    val indent_json: MutableState<Boolean> = mutableStateOf(indent_json)
    val latest_input_indicator: MutableState<Boolean> = mutableStateOf(latest_input_indicator)
    val normalize_beat_widths: MutableState<Boolean> = mutableStateOf(normalize_beat_widths)
    val beat_stroke_thickness: MutableState<Dp> = mutableStateOf(beat_stroke_thickness)
    val allow_multiple_soundfonts: MutableState<Boolean> = mutableStateOf(allow_multiple_soundfonts)
    val sort_load: MutableState<Int?> = mutableStateOf(sort_load)

    enum class MoveMode {
        MOVE,
        COPY,
        MERGE
    }

    companion object {
        fun from_json(content: JSONHashMap): PaganConfiguration {
            val soundfonts = mutableListOf<String>()
            // Handle old-style single soundfont
            content.get_stringn("soundfont2")?.let {
                soundfonts.add(it)
            }
            // Handle organized soundfonts
            content.get_listn("soundfonts")?.let {
                for (i in 0 until it.size) {
                    soundfonts.add(it.get_string(i))
                }
            }

            val soundfont_uris = mutableListOf<Uri>()
            // Handle dirless soundfonts
            content.get_listn("soundfont_uris")?.let {
                for (i in 0 until it.size) {
                    soundfont_uris.add(it.get_string(i).toUri())
                }
            }

            println("LOADING... ${content.get_intn("sort_load")}")
            return PaganConfiguration(
                soundfonts = soundfonts.toTypedArray(),
                soundfont_uris = soundfont_uris.toTypedArray(),
                sample_rate = content.get_int("sample_rate", 32000),
                move_mode = MoveMode.valueOf(content.get_string("move_mode", "COPY")),
                use_preferred_soundfont = content.get_boolean("use_preferred_soundfont", true),
                force_orientation = content.get_int("force_orientation", ActivityInfo.SCREEN_ORIENTATION_USER),
                allow_std_percussion = content.get_boolean("allow_std_percussion", false),
                project_directory = content.get_stringn("project_directory")?.toUri(),
                soundfont_directory = content.get_stringn("soundfont_directory")?.toUri(),
                night_mode = content.get_int("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
                indent_json = content.get_boolean("indent_json", false),
                latest_input_indicator = content.get_boolean("latest_input_indicator", true),
                normalize_beat_widths = content.get_boolean("normalize_beat_widths", false),
                beat_stroke_thickness = content.get_floatn("beat_stroke_thickness")?.dp ?: 0.dp,
                allow_multiple_soundfonts = content.get_boolean("allow_multiple_soundfonts", false),
                sort_load = content.get_int("sort_load", -3)
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
        val soundfonts_size = if (config.allow_multiple_soundfonts.value) {
            config.soundfonts.value.size
        } else {
            min(1, config.soundfonts.value.size)
        }
        this.soundfonts.value = Array(soundfonts_size) { mutableStateOf(config.soundfonts.value[it].value) }

        val soundfont_uris_size = if (config.allow_multiple_soundfonts.value) {
            config.soundfont_uris.value.size
        } else {
            min(1, config.soundfont_uris.value.size)
        }
        this.soundfont_uris.value = Array(soundfont_uris_size) { mutableStateOf(config.soundfont_uris.value[it].value) }

        this.sample_rate.value = config.sample_rate.value
        this.move_mode.value = config.move_mode.value
        this.use_preferred_soundfont.value = config.use_preferred_soundfont.value
        this.force_orientation.value = config.force_orientation.value
        this.allow_std_percussion.value = config.allow_std_percussion.value
        this.project_directory.value = config.project_directory.value
        this.soundfont_directory.value = config.soundfont_directory.value
        this.night_mode.value = config.night_mode.value
        this.indent_json.value = config.indent_json.value
        this.latest_input_indicator.value = config.latest_input_indicator.value
        this.normalize_beat_widths.value = config.normalize_beat_widths.value
        this.beat_stroke_thickness.value = config.beat_stroke_thickness.value
        this.allow_multiple_soundfonts.value = config.allow_multiple_soundfonts.value
        this.sort_load.value = config.sort_load.value
    }

    fun save(path: String) {
        File(path).writeText(this.to_json().to_string())
    }

    fun to_json(): JSONHashMap {
        val output = JSONHashMap()
        output["allow_multiple_soundfonts"] = this.allow_multiple_soundfonts.value

        output["soundfont_directory"] = this.soundfont_directory.value?.toString()
        val soundfonts_size = if (this.allow_multiple_soundfonts.value) {
            this.soundfonts.value.size
        } else {
            min(1, this.soundfonts.value.size)
        }
        output["soundfonts"] = JSONList(*Array(soundfonts_size) { JSONString(this.soundfonts.value[it].value) })

        val soundfont_uris_size = if (this.allow_multiple_soundfonts.value) {
            this.soundfont_uris.value.size
        } else {
            min(1, this.soundfont_uris.value.size)
        }
        output["soundfont_uris"] = JSONList(*Array(soundfont_uris_size) { JSONString(this.soundfont_uris.value[it].value.toString()) })

        output["sample_rate"] = this.sample_rate.value
        output["move_mode"] = this.move_mode.value.name
        output["use_preferred_soundfont"] = this.use_preferred_soundfont.value
        output["force_orientation"] = this.force_orientation.value
        output["allow_std_percussion"] = this.allow_std_percussion.value
        output["project_directory"] = this.project_directory.value?.toString()
        output["night_mode"] = this.night_mode.value
        output["indent_json"] = this.indent_json.value
        output["latest_input_indicator"] = this.latest_input_indicator.value
        output["normalize_beat_widths"] = this.normalize_beat_widths.value
        output["beat_stroke_thickness"] = this.beat_stroke_thickness.value.value
        println("SAVING.... ${this.sort_load.value}")
        output["sort_load"] = this.sort_load.value

        // output["channel_colors"] = JSONList(*Array(this.channel_colors.size) {
        //     JSONString(this.channel_colors[it].toHexString(HexFormat.Default))
        // })
        return output
    }

    fun to_intent(intent: Intent) {
        intent.putExtra("config_json", this.to_json().toString())
    }
}
