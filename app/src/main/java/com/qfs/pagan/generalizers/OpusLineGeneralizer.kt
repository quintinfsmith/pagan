package com.qfs.pagan.opusmanager

import com.qfs.json.ParsedHashMap
import com.qfs.json.ParsedInt
import com.qfs.json.ParsedList
import com.qfs.pagan.generalizers.OpusTreeGeneralizer
import com.qfs.pagan.structure.OpusTree

class OpusLineGeneralizer {
    companion object {
        fun to_json(line: OpusLineAbstract<*>): ParsedHashMap {
            var output = ParsedHashMap()

            val beats = ParsedList()
            for (i in line.beats.indices) {
                val generalized_tree = OpusTreeGeneralizer.to_json(line.beats[i]) { opus_event: InstrumentEvent ->
                    InstrumentEventParser.to_json(opus_event)
                } ?: continue

                beats.add(
                    ParsedList(
                        mutableListOf(
                            ParsedInt(i),
                            generalized_tree
                        )
                    )
                )
            }

            output["beats"] = beats
            output["controllers"] = ActiveControlSetGeneralizer.to_json(line.controllers)

            when (line) {
                is OpusLinePercussion -> {
                    output["instrument"] = ParsedInt(line.instrument)
                }
            }

            return output
        }

        fun percussion_line(input: ParsedHashMap, size: Int): OpusLinePercussion {
            val beats = input.get_list("beats")
            val beat_list = MutableList<OpusTree<PercussionEvent>>(size) { OpusTree() }
            for (i in 0 until beats.list.size) {
                val pair = beats.get_list(i)
                val beat_index = pair.get_int(0)
                val tree = OpusTreeGeneralizer.from_json(pair.get_hashmap(1)) { event: ParsedHashMap? ->
                    if (event != null) {
                        InstrumentEventParser.from_json(event) as PercussionEvent
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
            output.controllers = ActiveControlSetGeneralizer.from_json(input.get_hashmap("controllers"), size)

            return output
        }

        fun opus_line(input: ParsedHashMap, size: Int): OpusLine {
            val beats = input.get_list("beats")

            val beat_list = MutableList<OpusTree<TunedInstrumentEvent>>(size) { OpusTree() }
            for (i in 0 until beats.list.size) {
                val pair = beats.get_list(i)
                val beat_index = pair.get_int(0)
                val tree = OpusTreeGeneralizer.from_json(pair.get_hashmap(1)) { event: ParsedHashMap? ->
                    if (event != null) {
                        InstrumentEventParser.from_json(event) as TunedInstrumentEvent
                    } else {
                        null
                    }
                }

                beat_list[beat_index] = tree
            }

            val output = OpusLine(beat_list)
            output.controllers = ActiveControlSetGeneralizer.from_json(input.get_hashmap("controllers"), size)
            return output
        }
    }
}
