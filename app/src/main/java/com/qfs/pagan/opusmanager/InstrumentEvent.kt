package com.qfs.pagan.opusmanager

import com.qfs.json.*

class InstrumentEventParser() {
    companion object {
        fun to_json(event: InstrumentEvent): ParsedHashMap {
            val output = ParsedHashMap(hashMapOf(
                "duration" to ParsedInt(event.duration)
            ))
            when (event) {
                is AbsoluteNoteEvent -> {
                    output.hash_map["type"] = ParsedString("abs")
                    output.hash_map["note"] = ParsedInt(event.note)
                }
                is RelativeNoteEvent -> {
                    output.hash_map["type"] = ParsedString("rel")
                    output.hash_map["offset"] = ParsedInt(event.offset)
                }
                is PercussionEvent -> {
                    output.hash_map["type"] = ParsedString("perc")
                }
            }

            return output
        }

        fun from_json(input: ParsedHashMap): InstrumentEvent {
            val output = when (input.get_string("type")) {
                "rel" -> {
                    RelativeNoteEvent(input.get_int("offset") ?: 0)
                }
                "abs" -> {
                    AbsoluteNoteEvent(input.get_int("note") ?: 0)
                }
                "perc" -> {
                    PercussionEvent()
                }
                // TODO: SPECIFY Exception
                else -> throw Exception()
            }
            output.duration = input.get_int("duration") ?: 1
            return output
        }
    }
}

abstract class InstrumentEvent() {
    var duration: Int = 1
}

abstract class TunedInstrumentEvent(): InstrumentEvent()
class AbsoluteNoteEvent(var note: Int): TunedInstrumentEvent()
class RelativeNoteEvent(var offset: Int): TunedInstrumentEvent()
class PercussionEvent(): InstrumentEvent()

