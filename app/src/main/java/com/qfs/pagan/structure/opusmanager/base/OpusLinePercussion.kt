package com.qfs.pagan.structure.opusmanager.base

import com.qfs.pagan.structure.rationaltree.ReducibleTree

class OpusLinePercussion(var instrument: Int, beats: MutableList<ReducibleTree<PercussionEvent>>): OpusLineAbstract<PercussionEvent>(beats){
    constructor(instrument: Int, beat_count: Int) : this(instrument, Array<ReducibleTree<PercussionEvent>>(beat_count) { ReducibleTree() }.toMutableList())

    override fun equals(other: Any?): Boolean {
        return other is OpusLinePercussion
                && other.instrument == this.instrument
                &&super.equals(other)
    }
}
