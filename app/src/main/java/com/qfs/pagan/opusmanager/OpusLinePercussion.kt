package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree
import com.qfs.json.*

class OpusLinePercussion(beats: MutableList<OpusTree<OpusEventPercussion>>): OpusLineAbstract(beats) {
    // constructor(beat_count: Int) : this(Array<OpusTree<OpusEventPercussion>>(beat_count) { OpusTree() }.toMutableList())

    override fun populate_json(map: ParsedHashMap) {
        val static_value = this.static_value
        map.hash_map["static_value"] = if (static_value == null) {
            null
        } else {
            ParsedInt(static_value)
        }
    }
}

