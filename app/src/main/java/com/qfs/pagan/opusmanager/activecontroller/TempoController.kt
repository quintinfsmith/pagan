package com.qfs.pagan.opusmanager.activecontroller

import com.qfs.pagan.opusmanager.OpusTempoEvent

class TempoController(beat_count: Int): ActiveController<OpusTempoEvent>(beat_count, OpusTempoEvent(120F))
