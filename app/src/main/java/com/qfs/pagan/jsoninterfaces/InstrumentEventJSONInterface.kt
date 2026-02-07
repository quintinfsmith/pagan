/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.structure.opusmanager
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONString
import com.qfs.pagan.jsoninterfaces.UnknownEventTypeException
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.InstrumentEvent
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent

object InstrumentEventJSONInterface {
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
        val event_type = input.get_stringn("type")
        val output = when (event_type) {
            "rel" -> RelativeNoteEvent(input.get_int("offset"))
            "abs" -> AbsoluteNoteEvent(input.get_int("note"))
            "perc" -> PercussionEvent()
            else -> throw UnknownEventTypeException(event_type)
        }
        output.duration = input.get_int("duration", 1)
        return output
    }
}
