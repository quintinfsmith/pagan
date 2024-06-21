package com.qfs.pagan.opusmanager

import com.qfs.json.ParsedHashMap

class InstrumentEventParser {
    companion object {
        fun to_json(event: InstrumentEvent): ParsedHashMap {
            val output = ParsedHashMap()
            output["duration"] = event.duration
when (event) {
                is AbsoluteNoteEvent -> {
                    output["type"] = "abs"
                    output["note"] = event.note
                }
                is RelativeNoteEvent -> {
                    output["type"] = "rel"
                    output["offset"] = event.offset
                }
                is PercussionEvent -> {
                    output["type"] = "perc"
                }
            }

            return output
        }

        fun from_json(input: ParsedHashMap): InstrumentEvent {
            val output = when (input.get_string("type")) {
                "rel" -> RelativeNoteEvent(input.get_int("offset"))
                "abs" -> AbsoluteNoteEvent(input.get_int("note"))
                "perc" -> PercussionEvent()
                // TODO: SPECIFY Exception
                else -> throw Exception()
            }
            output.duration = input.get_int("duration", 1)
            return output
        }
    }
}
