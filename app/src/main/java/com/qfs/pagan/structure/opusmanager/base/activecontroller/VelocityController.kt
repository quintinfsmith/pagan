package com.qfs.pagan.structure.opusmanager.base.activecontroller

import com.qfs.pagan.structure.opusmanager.base.OpusVelocityEvent

class VelocityController(beat_count: Int): ActiveController<OpusVelocityEvent>(beat_count, OpusVelocityEvent(1F))