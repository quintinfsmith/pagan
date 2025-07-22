package com.qfs.pagan.structure.opusmanager.base.activecontroller
import com.qfs.pagan.structure.opusmanager.base.OpusReverbEvent
class ReverbController(beat_count: Int): ActiveController<OpusReverbEvent>(beat_count, OpusReverbEvent(1F))
