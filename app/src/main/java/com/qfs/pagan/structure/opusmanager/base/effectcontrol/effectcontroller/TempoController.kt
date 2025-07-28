package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent

class TempoController(beat_count: Int): EffectController<OpusTempoEvent>(beat_count, OpusTempoEvent(120F))
