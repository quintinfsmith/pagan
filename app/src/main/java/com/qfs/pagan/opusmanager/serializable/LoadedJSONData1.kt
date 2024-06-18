package com.qfs.pagan.opusmanager.serializable
import kotlinx.serialization.Serializable

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
