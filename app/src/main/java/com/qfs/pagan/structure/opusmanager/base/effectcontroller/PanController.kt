package com.qfs.pagan.structure.opusmanager.base.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.OpusPanEvent

class PanController(beat_count: Int): EffectController<OpusPanEvent>(beat_count, OpusPanEvent(0F))
