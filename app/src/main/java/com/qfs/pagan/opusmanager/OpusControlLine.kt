package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree


class OpusControlLine(var beats: MutableList<OpusTree<OpusControlEvent>>) {
    constructor(beat_count: Int) : this(Array<OpusTree<OpusControlEvent>>(beat_count) { OpusTree() }.toMutableList())
}