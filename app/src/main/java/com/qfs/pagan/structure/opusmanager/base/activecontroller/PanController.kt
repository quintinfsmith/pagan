package com.qfs.pagan.structure.opusmanager.base.activecontroller

import com.qfs.pagan.structure.opusmanager.base.OpusPanEvent

class PanController(beat_count: Int): ActiveController<OpusPanEvent>(beat_count, OpusPanEvent(0F))
