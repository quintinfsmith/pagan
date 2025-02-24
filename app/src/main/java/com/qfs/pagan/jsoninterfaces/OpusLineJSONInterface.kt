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
            for (i in 0 until beats.list.size) {
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
            output.controllers = ActiveControlSetJSONInterface.from_json(input.get_hashmap("controllers"), size)

            return output
        }

        fun opus_line(input: JSONHashMap, size: Int): OpusLine {
            val beats = input.get_list("beats")

            val beat_list = MutableList<OpusTree<TunedInstrumentEvent>>(size) { OpusTree() }
            for (i in 0 until beats.list.size) {
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
            output.controllers = ActiveControlSetJSONInterface.from_json(input.get_hashmap("controllers"), size)
            return output
        }
    }
}
