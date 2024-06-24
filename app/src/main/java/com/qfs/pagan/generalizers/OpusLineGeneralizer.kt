package com.qfs.pagan.opusmanager

import com.qfs.json.ParsedHashMap
import com.qfs.json.ParsedInt
import com.qfs.json.ParsedList
import com.qfs.pagan.generalizers.OpusTreeGeneralizer

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
                    OpusTreeGeneralizer.from_json(beats.get_hashmap(i)) { event: ParsedHashMap? ->
                        if (event != null) {
                            InstrumentEventParser.from_json(event) as PercussionEvent
                        } else {
                            null
                        }
                    }
                }
            )
        }

        fun opus_line(input: ParsedHashMap): OpusLine {
            val beats = input.get_list("beats")
            return OpusLine(
                MutableList(beats.list.size) { i: Int ->
                    OpusTreeGeneralizer.from_json(beats.get_hashmap(i)) { event: ParsedHashMap? ->
                        if (event != null) {
                            InstrumentEventParser.from_json(event) as TunedInstrumentEvent
                        } else {
                            null
                        }
                    }
                }
            )
        }
    }
}
