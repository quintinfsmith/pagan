package com.qfs.pagan.opusmanager

import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.pagan.jsoninterfaces.OpusTreeJSONInterface
import com.qfs.pagan.structure.OpusTree

class OpusLineJSONInterface {
    companion object {
        fun to_json(line: OpusLineAbstract<*>): JSONHashMap {
            var output = JSONHashMap()

            val beats = JSONList()
            for (i in line.beats.indices) {
                val generalized_tree = OpusTreeJSONInterface.to_json(line.beats[i]) { opus_event: InstrumentEvent ->
                    InstrumentEventJSONInterface.to_json(opus_event)
                } ?: continue

                beats.add(
                    JSONList(
                        JSONInteger(i),
                        generalized_tree
                    )
                )
            }

            output["beats"] = beats
            output["controllers"] = ActiveControlSetJSONInterface.to_json(line.controllers)
            output["muted"] = line.muted
            output["color"] = if (line.color != null) {
                val i = line.color!!
                val red = ((i and 16711680) shr 16).toInt()
                val green = ((i and 65280) shr 8).toInt()
                val blue = (i and 255).toInt()
                "#%02x".format(red) + "%02x".format(green) + "%02x".format(blue)
            } else {
                null
            }

            when (line) {
                is OpusLinePercussion -> {
                    output["instrument"] = JSONInteger(line.instrument)
                }
            }

            return output
        }

        fun percussion_line(input: JSONHashMap, size: Int): OpusLinePercussion {
            val beats = input.get_list("beats")
            val beat_list = MutableList<OpusTree<PercussionEvent>>(size) { OpusTree() }
            for (i in 0 until beats.size) {
                val pair = beats.get_list(i)
                val beat_index = pair.get_int(0)
                val tree = OpusTreeJSONInterface.from_json(pair.get_hashmap(1)) { event: JSONHashMap? ->
                    if (event != null) {
                        InstrumentEventJSONInterface.from_json(event) as PercussionEvent
                    } else {
                        null
                    }
                }

                beat_list[beat_index] = tree
            }

            val output = OpusLinePercussion(
                input.get_int("instrument"),
                beat_list
            )

            _interpret_general(input, output)

            return output
        }

        fun opus_line(input: JSONHashMap, size: Int): OpusLine {
            val beats = input.get_list("beats")

            val beat_list = MutableList<OpusTree<TunedInstrumentEvent>>(size) { OpusTree() }
            for (i in 0 until beats.size) {
                val pair = beats.get_list(i)
                val beat_index = pair.get_int(0)
                val tree = OpusTreeJSONInterface.from_json(pair.get_hashmap(1)) { event: JSONHashMap? ->
                    if (event != null) {
                        InstrumentEventJSONInterface.from_json(event) as TunedInstrumentEvent
                    } else {
                        null
                    }
                }

                beat_list[beat_index] = tree
            }

            val output = OpusLine(beat_list)

            _interpret_general(input, output)

            return output
        }

        private fun _interpret_general(input: JSONHashMap, output: OpusLineAbstract<*>) {
            output.controllers = ActiveControlSetJSONInterface.from_json(input.get_hashmap("controllers"), output.beat_count())
            output.muted = input.get_boolean("muted", false)

            val tmp_color = input.get_stringn("color")
            if (tmp_color == null) {
                output.color = null
            } else {
                val red = tmp_color.substring(1, 3).toInt(16)
                val green = tmp_color.substring(3, 5).toInt(16)
                val blue = tmp_color.substring(5, 7).toInt(16)
                output.color = (red * 256 * 256) + (green * 256) + blue
            }
        }
    }
}
