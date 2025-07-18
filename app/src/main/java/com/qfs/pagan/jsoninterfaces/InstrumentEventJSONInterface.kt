package com.qfs.pagan.structure.opusmanager
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONString

class InstrumentEventJSONInterface {
    companion object {
        fun to_json(event: InstrumentEvent): JSONHashMap {
            val output = JSONHashMap()
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

        fun convert_v1_to_v3_tuned(input: JSONHashMap): JSONHashMap {
            return if (input.get_boolean("relative", false)) {
                JSONHashMap(
                    "duration" to input["duration"],
                    "type" to JSONString("rel"),
                    "offset" to input["note"]
                )
            } else {
                JSONHashMap(
                    "duration" to input["duration"],
                    "type" to JSONString("abs"),
                    "note" to input["note"]
                )
            }
        }

        fun convert_v1_to_v3_percussion(input: JSONHashMap): JSONHashMap {
            return JSONHashMap(
                "duration" to input["duration"],
                "type" to JSONString("perc"),
            )
        }

        fun from_json(input: JSONHashMap): InstrumentEvent {
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
