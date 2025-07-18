package com.qfs.pagan.structure.opusmanager.activecontroller

import com.qfs.pagan.structure.opusmanager.OpusVolumeEvent

class VolumeController(beat_count: Int): ActiveController<OpusVolumeEvent>(beat_count, OpusVolumeEvent(1F))
