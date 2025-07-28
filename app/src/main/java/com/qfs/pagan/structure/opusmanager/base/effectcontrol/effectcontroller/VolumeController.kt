package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent

class VolumeController(beat_count: Int): EffectController<OpusVolumeEvent>(beat_count, OpusVolumeEvent(1F))
