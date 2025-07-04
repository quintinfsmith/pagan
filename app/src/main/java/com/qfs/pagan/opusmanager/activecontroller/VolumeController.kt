package com.qfs.pagan.opusmanager.activecontroller

import com.qfs.pagan.opusmanager.OpusVolumeEvent

class VolumeController(beat_count: Int): ActiveController<OpusVolumeEvent>(beat_count, OpusVolumeEvent(1F))
