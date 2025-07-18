package com.qfs.pagan.structure.opusmanager.activecontroller
import com.qfs.pagan.structure.opusmanager.OpusReverbEvent
class ReverbController(beat_count: Int): ActiveController<OpusReverbEvent>(beat_count, OpusReverbEvent(1F))
