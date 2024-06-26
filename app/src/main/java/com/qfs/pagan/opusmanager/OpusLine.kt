package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree

class OpusLine(beats: MutableList<OpusTree<TunedInstrumentEvent>>): OpusLineAbstract<TunedInstrumentEvent>(beats) {
    constructor(beat_count: Int) : this(Array<OpusTree<TunedInstrumentEvent>>(beat_count) { OpusTree() }.toMutableList())

    override fun equals(other: Any?): Boolean {
        return other is OpusLine && super.equals(other)
    }
}

