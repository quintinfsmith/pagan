package com.qfs.pagan.opusmanager.activecontroller

import com.qfs.pagan.opusmanager.OpusPanEvent

class PanController(beat_count: Int): ActiveController<OpusPanEvent>(beat_count, OpusPanEvent(0F))
