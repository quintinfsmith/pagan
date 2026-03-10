package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.PitchEvent

class PitchController(beat_count: Int): EffectController<PitchEvent>(beat_count, PitchEvent(0F))
