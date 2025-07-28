package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
class ReverbController(beat_count: Int): EffectController<OpusReverbEvent>(beat_count, OpusReverbEvent(1F))
