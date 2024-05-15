package com.qfs.pagan

import com.qfs.apres.soundfontplayer.WaveGenerator
import com.qfs.pagan.ColorMap.Palette
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PaganConfiguration(
    var soundfont: String? = null,
    var relative_mode: Boolean = false,
    var sample_rate: Int = 44100,
    var show_percussion: Boolean = true, // Deprecated, use variable in view_model
    var link_mode: LinkMode = LinkMode.COPY,
    var palette: HashMap<Palette, Int>? = null,
    var use_palette: Boolean = false,
    var visible_line_controls: MutableSet<Pair<CtlLineLevel, ControlEventType>> = mutableSetOf(
        Pair(CtlLineLevel.Global, ControlEventType.Tempo)
    ),
    var playback_sample_limit: Int? = null,
    var playback_stereo_mode: WaveGenerator.StereoMode = WaveGenerator.StereoMode.Mono
) {
    enum class LinkMode {
        MOVE,
        COPY,
        LINK
    }
    companion object {
        fun from_path(path: String): PaganConfiguration {
            val file = File(path)
            return if (file.exists()) {
                val json_content = file.readText(Charsets.UTF_8)
                val json = Json {
                    ignoreUnknownKeys = true
                }

                json.decodeFromString( json_content )
            } else {
                PaganConfiguration()
            }
        }
    }

    fun save(path: String) {
        val file = File(path)
        val json_string = Json.encodeToString(this)
        file.writeText(json_string)
    }
}