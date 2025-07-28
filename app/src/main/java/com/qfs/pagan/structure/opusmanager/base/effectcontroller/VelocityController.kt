package com.qfs.pagan.structure.opusmanager.base.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.OpusVelocityEvent

class VelocityController(beat_count: Int): EffectController<OpusVelocityEvent>(beat_count, OpusVelocityEvent(1F))