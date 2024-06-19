package com.qfs.pagan.opusmanager.serializable
import com.qfs.json.Parser
import com.qfs.json.ParsedHashMap
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ChannelJSONData
import com.qfs.pagan.opusmanager.OpusEventSTD
import com.qfs.pagan.opusmanager.serializable.exceptions.FutureSaveVersionException
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
data class LoadedJSONData(
    var name: String? = null,
    var channels: List<ChannelJSONData>,
    var reflections: List<List<BeatKey>>? = null,
    var transpose: Int = 0,
    var tuning_map: Array<Pair<Int, Int>> = Array(12) { i: Int -> Pair(i, 12) },
    var controllers: List<ActiveControllerJSON> = listOf()
) {
    companion object {
        fun from_string(json_string: String): LoadedJSONData {
            val json = Json {
                ignoreUnknownKeys = true
            }

            val shallow_map = Parser.parse(json_string)
            val map_keys: Set<String> = (shallow_map as ParsedHashMap).hash_map.keys.toSet()

            val version = when (map_keys) {
                setOf("v", "d") -> {
                    json.decodeFromString<Int>(shallow_map["v"]!!)
                }
                else -> {
                    if (!map_keys.contains("controllers")) {
                        if (!map_keys.contains("tuning_map")) {
                            0
                        } else {
                            1
                        }
                    } else {
                        2
                    }
                }
            }


            // NOTE: chaining the convert_old_fmt calls saves me having to update all the functions
            // every time I change the save format
            return when (version) {
                0 -> LoadedJSONData.from_old_format(
                    json.decodeFromString<LoadedJSONData0>(json_string)
                )
                1 -> LoadedJSONData.from_old_format(
                    json.decodeFromString<LoadedJSONData1>(json_string)
                )
                2 -> LoadedJSONData.from_old_format(
                    json.decodeFromString<LoadedJSONData2>(json_string)
                )
                3 -> json.decodeFromString<LoadedJSONData>(shallow_map["d"]!!)
                else -> {
                    throw FutureSaveVersionException(version)
                }
            }
        }

        fun from_old_format(old_data: LoadedJSONData0): LoadedJSONData {
            return LoadedJSONData.from_old_format(
                LoadedJSONData2.from_old_format(
                    LoadedJSONData1.from_old_format(old_data)
                )
            )
        }

        fun from_old_format(old_data: LoadedJSONData1): LoadedJSONData {
            return LoadedJSONData.from_old_format(
                LoadedJSONData2.from_old_format(old_data)
            )
        }

        fun from_old_format(old_data: LoadedJSONData2): LoadedJSONData {
            val new_channel_data = List<ChannelJSONData>(old_data.channels.size) { i: Int ->
                val channel: ChannelJSONData2 = old_data.channels[i]
                ChannelJSONData(
                    midi_channel = channel.midi_channel,
                    midi_bank = channel.midi_bank,
                    midi_program = channel.midi_program,
                    controllers = channel.channel_controllers,
                    lines = List<LineJSONData>(channel.lines.size) { j: Int ->
                        val line: OpusTreeJSON<OpusEventSTD> = channel.lines[j]
                        LineJSONData(
                            beats = List<OpusTreeJSON<OpusEventSTD>?>(line.children!!.size) { k: Int ->
                                line.children!![k]
                            },
                            static_value = channel.line_static_values[j],
                            controllers = channel.line_controllers[j]
                        )
                    }
                )
            }

            return LoadedJSONData(
                name = old_data.name,
                channels = new_channel_data,
                reflections = old_data.reflections,
                transpose = old_data.transpose,
                tuning_map = old_data.tuning_map,
                controllers = old_data.controllers
            )
        }
    }
}

