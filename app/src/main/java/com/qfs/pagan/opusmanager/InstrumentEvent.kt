package com.qfs.pagan.opusmanager

import com.qfs.json.*

abstract class InstrumentEvent() {
    var duration: Int = 1
    fun to_json(): ParsedHashMap {
        val output = ParsedHashMap(hashMapOf(
            "duration" to ParsedInt(this.duration)
        ))
        this.populate_json(output)
        return output
    }

    abstract fun populate_json(map: ParsedHashMap)
}

class AbsoluteNoteEvent(var note: Int): InstrumentEvent() {
    override fun populate_json(map: ParsedHashMap) {
        map.hash_map["note"] = ParsedInt(this.note)
    }
}
class RelativeNoteEvent(var offset: Int): InstrumentEvent() {
    override fun populate_json(map: ParsedHashMap) {
        map.hash_map["offset"] = ParsedInt(this.offset)
    }
}

class PercussionEvent(): InstrumentEvent() {
    override fun populate_json(map: ParsedHashMap) {
        // Nothing to be done
    }
}

