package com.qfs.pagan.structure.opusmanager.base

import com.qfs.pagan.structure.rationaltree.ReducibleTree

class OpusLine(beats: MutableList<ReducibleTree<TunedInstrumentEvent>>): OpusLineAbstract<TunedInstrumentEvent>(beats) {
    constructor(beat_count: Int) : this(Array<ReducibleTree<TunedInstrumentEvent>>(beat_count) { ReducibleTree() }.toMutableList())

    override fun equals(other: Any?): Boolean {
        return other is OpusLine && super.equals(other)
    }
}

