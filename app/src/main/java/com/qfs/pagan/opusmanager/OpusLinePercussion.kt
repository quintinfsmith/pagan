package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree

class OpusLinePercussion(var instrument: Int, beats: MutableList<OpusTree<PercussionEvent>>): OpusLineAbstract<PercussionEvent>(beats){
    constructor(instrument: Int, beat_count: Int) : this(instrument, Array<OpusTree<PercussionEvent>>(beat_count) { OpusTree() }.toMutableList())
}
