package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent

class PanController(beat_count: Int): EffectController<OpusPanEvent>(beat_count, OpusPanEvent(0F))
