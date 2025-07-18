package com.qfs.pagan.structure.opusmanager

import com.qfs.pagan.structure.rationaltree.ReducibleTree

class OpusLine(beats: MutableList<ReducibleTree<TunedInstrumentEvent>>): OpusLineAbstract<TunedInstrumentEvent>(beats) {
    constructor(beat_count: Int) : this(Array<ReducibleTree<TunedInstrumentEvent>>(beat_count) { ReducibleTree() }.toMutableList())
}

