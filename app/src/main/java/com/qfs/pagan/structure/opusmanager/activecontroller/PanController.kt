package com.qfs.pagan.structure.opusmanager.activecontroller

import com.qfs.pagan.structure.opusmanager.OpusPanEvent

class PanController(beat_count: Int): ActiveController<OpusPanEvent>(beat_count, OpusPanEvent(0F))
