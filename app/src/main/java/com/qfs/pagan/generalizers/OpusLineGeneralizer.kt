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
                beats.add(
                    OpusTreeGeneralizer.to_json(line.beats[i]) { opus_event: InstrumentEvent ->
                        InstrumentEventParser.to_json(opus_event)
                    }
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

        fun percussion_line(input: ParsedHashMap): OpusLinePercussion {
            val beats = input.get_list("beats")
            return OpusLinePercussion(
                input.get_int("instrument"),
                MutableList(beats.list.size) { i: Int ->
                    val beat_hashmap = beats.get_hashmapn(i)
                    if (beat_hashmap != null) {
                        OpusTreeGeneralizer.from_json(beat_hashmap) { event: ParsedHashMap? ->
                            if (event != null) {
                                InstrumentEventParser.from_json(event) as PercussionEvent
                            } else {
                                null
                            }
                        }
                    } else {
                        OpusTree()
                    }
                }
            )
        }

        fun opus_line(input: ParsedHashMap): OpusLine {
            val beats = input.get_list("beats")
            return OpusLine(
                MutableList(beats.list.size) { i: Int ->
                    val beat_map = beats.get_hashmapn(i)
                    if (beat_map == null) {
                        OpusTree()
                    } else {
                        OpusTreeGeneralizer.from_json(beat_map) { event: ParsedHashMap? ->
                            if (event != null) {
                                InstrumentEventParser.from_json(event) as TunedInstrumentEvent
                            } else {
                                null
                            }
                        }
                    }
                }
            )
        }
    }
}
