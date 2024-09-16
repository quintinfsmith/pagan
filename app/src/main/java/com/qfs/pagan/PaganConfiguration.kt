package com.qfs.pagan

import com.qfs.apres.soundfontplayer.WaveGenerator
import com.qfs.pagan.ColorMap.Palette
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import java.io.File
import com.qfs.json.JSONParser
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONList

data class PaganConfiguration(
    var soundfont: String? = null,
    var relative_mode: Boolean = false,
    var sample_rate: Int = 22050,
    var show_percussion: Boolean = true, // Deprecated, use variable in view_model
    var link_mode: LinkMode = LinkMode.COPY,
    var palette: HashMap<Palette, Int>? = null,
    var use_palette: Boolean = false,
    var visible_line_controls: MutableSet<Pair<CtlLineLevel, ControlEventType>> = mutableSetOf(
        Pair(CtlLineLevel.Global, ControlEventType.Tempo)
    )
) {
    enum class LinkMode {
        MOVE,
        COPY,
        LINK,
        MERGE
    }
    companion object {
        fun from_path(path: String): PaganConfiguration {
            val file = File(path)
            return if (file.exists()) {
                val string = file.readText()
                val content = JSONParser.parse(string)
                if (content !is JSONHashMap) {
                    PaganConfiguration()
                } else {
                    val stored_palette = content.get_hashmapn("palette")
                    val stored_visible_line_controls = content.get_listn("visible_line_controls")
                    PaganConfiguration(
                        soundfont = content.get_stringn("soundfont"),
                        sample_rate = content.get_intn("sample_rate") ?: 22050,
                        relative_mode = content.get_booleann("relative_mode") ?: false,
                        link_mode = LinkMode.valueOf(content.get_stringn("link_mode") ?: "COPY"),
                        use_palette = content.get_booleann("use_palette") ?: false,
                        palette = if (stored_palette != null) {
                            val new_palette = HashMap<Palette, Int>()
                            for ((key, value) in stored_palette.hash_map) {
                                try {
                                    new_palette[Palette.valueOf(key)] = stored_palette.get_int(key)
                                } catch (e: IllegalArgumentException) {
                                    continue
                                }
                            }
                            new_palette
                        } else {
                            null
                        },
                        visible_line_controls = if (stored_visible_line_controls != null) {
                            val vlc_set = mutableSetOf<Pair<CtlLineLevel, ControlEventType>>()
                            for (pair in stored_visible_line_controls.list) {

                                try {
                                    vlc_set.add(
                                        Pair(
                                            CtlLineLevel.valueOf((pair as JSONList).get_string(0)),
                                            ControlEventType.valueOf((pair as JSONList).get_string(1))
                                        )
                                    )
                                } catch (e: IllegalArgumentException) {
                                    continue
                                }
                            }
                            vlc_set
                        } else {
                            mutableSetOf(
                                Pair(CtlLineLevel.Global, ControlEventType.Tempo)
                            )
                        }
                    )
                }
            } else {
                PaganConfiguration()
            }
        }
    }

    fun save(path: String) {
        val output = JSONHashMap()
        output["soundfont"] = this.soundfont
        output["sample_rate"] = this.sample_rate
        output["relative_mode"] = this.relative_mode
        output["link_mode"] = this.link_mode.name
        output["use_palette"] = this.use_palette
        output["palette"] = if (this.palette == null) {
            null
        } else {
            val hashmap = JSONHashMap()
            for ((key, value) in this.palette!!) {
                hashmap[key.name] = value
            }
            hashmap
        }
        val vlc = JSONList()
        for ((key, value) in this.visible_line_controls) {
            val pair = JSONList()
            pair.add(key.name)
            pair.add(value.name)
            vlc.add(pair)
        }
        output["visible_line_controls"] = vlc

       val file = File(path)
       file.writeText(output.to_string())
    }
}
