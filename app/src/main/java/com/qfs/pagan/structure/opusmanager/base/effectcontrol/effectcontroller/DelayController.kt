package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent

class DelayController(beat_count: Int): EffectController<DelayEvent>(beat_count, DelayEvent(2, 1, 1, .75F, 1))
