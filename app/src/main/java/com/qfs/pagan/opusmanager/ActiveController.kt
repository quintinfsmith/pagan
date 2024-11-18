package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree

abstract class ActiveController<T: OpusControlEvent>(beat_count: Int, var initial_event: T): OpusTreeArray<T>(MutableList(beat_count) { OpusTree() }) {
    var visible = true // I don't like this logic here, but the code is substantially cleaner with it hear than in the OpusLayerInterface
    fun set_initial_event(value: T) {
        this.initial_event = value
    }
    fun get_initial_event(): T {
        return this.initial_event
    }
}

class VolumeController(beat_count: Int): ActiveController<OpusVolumeEvent>(beat_count, OpusVolumeEvent(64))
class TempoController(beat_count: Int): ActiveController<OpusTempoEvent>(beat_count, OpusTempoEvent(120F))
class ReverbController(beat_count: Int): ActiveController<OpusReverbEvent>(beat_count, OpusReverbEvent(1F))
class PanController(beat_count: Int): ActiveController<OpusPanEvent>(beat_count, OpusPanEvent(0F))
