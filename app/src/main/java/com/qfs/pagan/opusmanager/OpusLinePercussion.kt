package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree

class OpusLinePercussion(var instrument: Int, beats: MutableList<OpusTree<PercussionEvent>>): OpusLineAbstract(beats)
