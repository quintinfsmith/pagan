package com.qfs.pagan.structure.opusmanager.base.activecontroller

import com.qfs.pagan.structure.opusmanager.base.OpusVolumeEvent

class VolumeController(beat_count: Int): ActiveController<OpusVolumeEvent>(beat_count, OpusVolumeEvent(1F))
