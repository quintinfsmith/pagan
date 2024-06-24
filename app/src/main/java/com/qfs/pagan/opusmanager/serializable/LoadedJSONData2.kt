package com.qfs.pagan.opusmanager.serializable
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

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

