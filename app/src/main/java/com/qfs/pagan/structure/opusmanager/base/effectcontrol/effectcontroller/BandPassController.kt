package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.BandPassEvent

class BandPassController(beat_count: Int): EffectController<BandPassEvent>(beat_count, BandPassEvent(null, null, null))
