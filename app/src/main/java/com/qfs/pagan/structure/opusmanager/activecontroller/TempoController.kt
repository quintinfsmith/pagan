package com.qfs.pagan.structure.opusmanager.activecontroller

import com.qfs.pagan.structure.opusmanager.OpusTempoEvent

class TempoController(beat_count: Int): ActiveController<OpusTempoEvent>(beat_count, OpusTempoEvent(120F))
