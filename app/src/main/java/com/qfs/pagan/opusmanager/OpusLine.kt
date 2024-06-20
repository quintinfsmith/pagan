package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree
import com.qfs.pagan.opusmanager.TunedInstrumentEvent

import com.qfs.json.*

class OpusLine(beats: MutableList<OpusTree<TunedInstrumentEvent>>): OpusLineAbstract(beats) {
    // constructor(beat_count: Int) : this(Array<OpusTree<TunedInstrumentEvent>>(beat_count) { OpusTree() }.toMutableList())

    override fun populate_json(map: ParsedHashMap) {
        // Nothing to be done
    }
}

