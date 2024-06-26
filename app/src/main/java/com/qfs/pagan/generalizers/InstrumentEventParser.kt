package com.qfs.pagan.opusmanager
import com.qfs.json.ParsedHashMap
import com.qfs.json.ParsedString

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

        fun convert_v1_to_v3_tuned(input: ParsedHashMap): ParsedHashMap {
            return if (input.get_boolean("relative", false)) {
                ParsedHashMap(
                    hashMapOf(
                        "duration" to input["duration"],
                        "type" to ParsedString("rel"),
                        "offset" to input["note"]
                    )
                )
            } else {
                ParsedHashMap(
                    hashMapOf(
                        "duration" to input["duration"],
                        "type" to ParsedString("abs"),
                        "note" to input["note"]
                    )
                )
            }
        }

        fun convert_v1_to_v3_percussion(input: ParsedHashMap): ParsedHashMap {
            return ParsedHashMap(
                hashMapOf(
                    "duration" to input["duration"],
                    "type" to ParsedString("perc"),
                )
            )
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
