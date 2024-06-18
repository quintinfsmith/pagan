package com.qfs.pagan.opusmanager
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import com.qfs.pagan.from_string
import kotlin.math.max
import kotlin.math.min

class InvalidJSON(json_string: String, index: Int): Exception("Invalid JSON @ $index In \"${json_string.substring(max(0, index - 20), min(json_string.length, index + 20))}\"")
class FutureSaveVersionException(version: Int): Exception("Attempting to load a project made with a newer version of Pagan (format: $version)")

@Serializable
data class LineJSONData(
    var static_value: Int? = null,
    var beats: List<OpusTreeJSON<OpusEventSTD>?>,
    var controllers: List<ActiveControllerJSON>
)

@Serializable
data class ChannelJSONData(
    var midi_channel: Int,
    var midi_bank: Int,
    var midi_program: Int,
    var lines: List<LineJSONData>,
    var controllers: List<ActiveControllerJSON>,
)

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

            val shallow_map = get_shallow_representation(json_string)
            val map_keys = shallow_map.keys.toSet()

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
                    LoadedJSONData0.from_string(json_string)
                )
                1 -> LoadedJSONData.from_old_format(
                    LoadedJSONData1.from_string(json_string)
                )
                2 -> LoadedJSONData.from_old_format(
                    LoadedJSONData2.from_string(json_string)
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

@Serializable
data class JSONWrapper(
    val v: Int,
    val d: LoadedJSONData
)

@Serializable
data class ActiveControllerJSON(
    var type: ControlEventType,
    var initial_value: OpusControlEvent,
    var children: List<Pair<Int, OpusTreeJSON<OpusControlEvent>>>
)

@Serializable
data class OpusTreeJSON<T>(
    var event: T?,
    var children: List<OpusTreeJSON<T>?>?
)

// Old Fmt
@Serializable
data class ChannelJSONData0(
    var midi_channel: Int,
    var midi_bank: Int,
    var midi_program: Int,
    var lines: List<String>,
    var line_volumes: List<Int>
)
@Serializable
data class LoadedJSONData0(
    var tempo: Float,
    var radix: Int,
    var channels: List<ChannelJSONData0>,
    var reflections: List<List<BeatKey>>? = null,
    var transpose: Int = 0,
    var name: String
) {
    companion object {
        fun from_string(json_string: String): LoadedJSONData0 {
            val json = Json {
                ignoreUnknownKeys = true
            }
            return json.decodeFromString<LoadedJSONData0>(json_string)
        }
    }
}

@Serializable
data class ChannelJSONData1(
    var midi_channel: Int,
    var midi_bank: Int,
    var midi_program: Int,
    var lines: List<OpusTreeJSON<OpusEventSTD>>,
    var line_volumes: List<Int>,
    var line_static_values: List<Int?> = listOf()
)

@Serializable
data class LoadedJSONData1(
    var tempo: Float,
    var channels: List<ChannelJSONData1>,
    var reflections: List<List<BeatKey>>? = null,
    var transpose: Int = 0,
    var name: String,
    var tuning_map: Array<Pair<Int, Int>> = Array(12) { i: Int -> Pair(i, 12) },
    var radix: Int = tuning_map.size,
) {
    companion object {
        fun from_string(json_string: String): LoadedJSONData1 {
            val json = Json { ignoreUnknownKeys = true }
            return json.decodeFromString<LoadedJSONData1>(json_string)
        }

        fun from_old_format(old_data: LoadedJSONData0): LoadedJSONData1 {
            val new_channels = mutableListOf<ChannelJSONData1>()
            for (channel in old_data.channels) {
                val new_lines = mutableListOf<OpusTreeJSON<OpusEventSTD>>()
                for (line_string in channel.lines) {
                    val line_children = mutableListOf<OpusTreeJSON<OpusEventSTD>?>()
                    for (beat_string in line_string.split("|")) {
                        val beat_tree = from_string(beat_string, old_data.radix, channel.midi_channel)
                        beat_tree.clear_singles()

                        line_children.add(beat_tree.to_json())
                    }
                    new_lines.add(OpusTreeJSON<OpusEventSTD>(null, line_children))
                }

                new_channels.add(
                    ChannelJSONData1(
                        midi_channel = channel.midi_channel,
                        midi_bank = channel.midi_bank,
                        midi_program = channel.midi_program,
                        lines = new_lines,
                        line_volumes = channel.line_volumes,
                        line_static_values = List(channel.line_volumes.size) { null }
                    )
                )
            }

            return LoadedJSONData1(
                tempo = old_data.tempo,
                tuning_map = Array(old_data.radix) { i: Int ->
                    Pair(i, old_data.radix)
                },
                channels = new_channels,
                reflections = old_data.reflections,
                transpose = old_data.transpose,
                name = old_data.name
            )
        }
    }
}

@Serializable
data class ChannelJSONData2(
    var midi_channel: Int,
    var midi_bank: Int,
    var midi_program: Int,
    var lines: List<OpusTreeJSON<OpusEventSTD>>,
    var line_static_values: List<Int?> = listOf(),
    var line_controllers: List<List<ActiveControllerJSON>>,
    var channel_controllers: List<ActiveControllerJSON>,
)

@Serializable
data class LoadedJSONData2(
    var channels: List<ChannelJSONData2>,
    var reflections: List<List<BeatKey>>? = null,
    var transpose: Int = 0,
    var name: String,
    var tuning_map: Array<Pair<Int, Int>> = Array(12) { i: Int -> Pair(i, 12) },
    var controllers: List<ActiveControllerJSON> = listOf()
) {
    companion object {
        fun from_string(json_string: String): LoadedJSONData2 {
            val json = Json { ignoreUnknownKeys = true }
            return json.decodeFromString<LoadedJSONData2>(json_string)
        }

        fun from_old_format(old_data: LoadedJSONData1): LoadedJSONData2 {
            val new_channels = mutableListOf<ChannelJSONData2>()
            var beat_count = 0
            for (channel in old_data.channels) {
                channel.lines.forEachIndexed { line_index: Int, line: OpusTreeJSON<OpusEventSTD> ->
                    beat_count = if (line.children != null) {
                        max(line.children!!.size, beat_count)
                    }  else {
                        max(1, beat_count)
                    }

                }

                val line_controllers = mutableListOf<List<ActiveControllerJSON>>()
                for (i in channel.lines.indices) {
                    val new_controller = ActiveControllerJSON(
                        ControlEventType.Volume,
                        OpusVolumeEvent(channel.line_volumes[i]),
                        listOf()
                    )

                    line_controllers.add(listOf(new_controller))
                }

                new_channels.add(
                    ChannelJSONData2(
                        midi_channel = channel.midi_channel,
                        midi_bank = channel.midi_bank,
                        midi_program = channel.midi_program,
                        lines = channel.lines,
                        line_static_values = List(channel.lines.size) { null },
                        line_controllers = line_controllers,
                        channel_controllers = listOf()
                    )
                )
            }

            return LoadedJSONData2(
                tuning_map = old_data.tuning_map,
                reflections = old_data.reflections,
                transpose = old_data.transpose,
                name = old_data.name,
                channels = new_channels,
                controllers = listOf(
                    ActiveControllerJSON(
                        ControlEventType.Tempo,
                        OpusTempoEvent(old_data.tempo),
                        listOf()
                    )
                )
            )
        }
    }
}

fun get_shallow_representation(json_content: String): HashMap<String, String> {
    val output = HashMap<String, String>()

    var value_index_i = -1
    var value_index_f = -1
    var working_key: String? = null

    var working_number: String? = null
    var working_string: String? = null
    var string_escape_flagged = false

    var type_stack = mutableListOf<Int>() // 0 = dict, 1 = list

    var index = 0
    while (index < json_content.length) {
        val working_char = json_content[index]
        if (working_number != null) {
            when (working_char) {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' -> {
                    working_number += working_char
                }
                ' ', '\r', '\n', Char(125), Char(93), ',' -> {
                    try {
                        working_number.toFloat()
                        working_number = null
                        continue
                    } catch (e: Exception) {
                        throw InvalidJSON(json_content, index)
                    }
                }
                else -> {
                    throw InvalidJSON(json_content, index)
                }
            }
        } else if (working_string != null) {
            if (string_escape_flagged) {
                working_string += working_char
                string_escape_flagged = false
            } else {
                when (working_char) {
                    '\\' -> {
                        string_escape_flagged = true
                    }
                    '"' -> {
                        if (working_key == null && type_stack.size == 1) {
                            working_key = working_string
                        }
                        working_string = null
                    }
                    else -> {
                        working_string += working_char
                    }
                }
            }
        } else {
            when (working_char) {
                ',' -> {
                    if (type_stack.size == 1 && value_index_i >= 0) {
                        value_index_f = index
                    }
                }
                ':' -> {
                    if (type_stack.size == 1) {
                        value_index_i = index + 1
                    }
                }
                Char(123) -> {
                    if (type_stack.size == 1) {
                        working_key == null
                    }
                    type_stack.add(0)
                }
                Char(125) -> {
                    if (type_stack.last() == 0) {
                        type_stack.removeLast()
                    } else {
                        throw InvalidJSON(json_content, index)
                    }
                    if (type_stack.size < 2 && value_index_i >= 0) {
                        value_index_f = index + 1
                    }
                }
                Char(91) -> {
                    type_stack.add(1)
                }
                Char(93) -> {
                    if (type_stack.last() == 1) {
                        type_stack.removeLast()
                    } else {
                        throw InvalidJSON(json_content, index)
                    }

                    if (type_stack.size == 1 && value_index_i >= 0) {
                        value_index_f = index + 1
                    }
                }
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-' -> {
                    working_number = "" + working_char
                }
                '"' -> {
                    working_string = ""
                }
                ' ', '\r', '\n', '\t' -> {
                }
                'n' -> {
                    if (json_content.substring(index, index + 4) != "null") {
                        throw InvalidJSON(json_content, index)
                    } else {
                        index += 3
                    }
                }
                'f' -> {
                    if (json_content.substring(index, index + 5) != "false") {
                        throw InvalidJSON(json_content, index)
                    } else {
                        index += 4
                    }
                }
                't' -> {
                    if (json_content.substring(index, index + 4) != "true") {
                        throw InvalidJSON(json_content, index)
                    } else {
                        index += 3
                    }
                }
                else -> {
                    throw InvalidJSON(json_content, index)
                }
            }
        }

        if (working_key != null && value_index_f >= 0 && value_index_i >= 0) {
            output[working_key!!] = json_content.substring(value_index_i, value_index_f).trim()
            working_key = null
            value_index_i = -1
            value_index_f = -1
        }
        index += 1
    }
    return output
}

