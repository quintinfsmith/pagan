package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent

class DelayController(beat_count: Int): EffectController<DelayEvent>(beat_count, DelayEvent(Rational(1, 2), 2, .5F, 1))
