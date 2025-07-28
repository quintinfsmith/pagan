package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent

class VelocityController(beat_count: Int): EffectController<OpusVelocityEvent>(beat_count, OpusVelocityEvent(1F))