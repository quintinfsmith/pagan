package com.qfs.pagan

import com.qfs.apres.soundfontplayer.SoundFontWavPlayer
import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class PaganConfiguration(
    var soundfont: String? = null,
    var relative_mode: Boolean = false,
    var sample_rate: Int = SoundFontWavPlayer.calc_sample_rate()
) {
    companion object {
        fun from_path(path: String): PaganConfiguration {
            val file = File(path)
            return if (file.exists()) {
                val json_content = file.readText(Charsets.UTF_8)
                Json.decodeFromString(json_content)
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