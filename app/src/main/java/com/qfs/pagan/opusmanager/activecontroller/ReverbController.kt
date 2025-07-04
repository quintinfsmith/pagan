package com.qfs.pagan.opusmanager.activecontroller
import com.qfs.pagan.opusmanager.OpusReverbEvent
class ReverbController(beat_count: Int): ActiveController<OpusReverbEvent>(beat_count, OpusReverbEvent(1F))
