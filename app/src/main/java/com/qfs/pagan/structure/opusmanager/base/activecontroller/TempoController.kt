package com.qfs.pagan.structure.opusmanager.base.activecontroller

import com.qfs.pagan.structure.opusmanager.base.OpusTempoEvent

class TempoController(beat_count: Int): ActiveController<OpusTempoEvent>(beat_count, OpusTempoEvent(120F))
