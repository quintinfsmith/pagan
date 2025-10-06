package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.LowPassEvent

class LowPassController(beat_count: Int): EffectController<LowPassEvent>(beat_count, LowPassEvent(null, null))
