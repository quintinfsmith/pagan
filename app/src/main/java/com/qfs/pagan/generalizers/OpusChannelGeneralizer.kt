package com.qfs.pagan.opusmanager

import com.qfs.json.ParsedHashMap
import com.qfs.json.ParsedList

class OpusChannelGeneralizer {
    companion object {
        fun generalize(channel: OpusChannel): ParsedHashMap {
            val channel_map = ParsedHashMap()
            val lines = ParsedList(
                MutableList(channel.size) { i: Int ->
                    OpusLineGeneralizer.to_json(channel.lines[i])
                }
            )
            channel_map["lines"] = lines
            channel_map["midi_channel"] = channel.midi_channel
            channel_map["midi_bank"] = channel.midi_bank
            channel_map["midi_program"] = channel.midi_program

            return channel_map
        }

        fun interpret(input_map: ParsedHashMap): OpusChannel {
            val channel = OpusChannel(-1)
            channel.midi_channel = input_map.get_int("midi_channel")
            channel.midi_bank = input_map.get_int("midi_bank")
            channel.midi_program = input_map.get_int("midi_program")

            val input_lines = input_map.get_list("lines")
            for (line in input_lines.list) {
                if (channel.midi_channel == 9) {
                    channel.lines.add(OpusLineGeneralizer.percussion_line(line as ParsedHashMap))
                } else {
                    channel.lines.add(OpusLineGeneralizer.opus_line(line as ParsedHashMap))
                }
            }

            return channel
        }
    }
}
