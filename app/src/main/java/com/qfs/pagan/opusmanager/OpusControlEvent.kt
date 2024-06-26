package com.qfs.pagan.opusmanager

abstract class OpusControlEvent

enum class ControlEventType {
    Tempo,
    Volume,
    Reverb,
}

class OpusTempoEvent(var value: Float): OpusControlEvent() {
    override fun equals(other: Any?): Boolean {
        return other is OpusTempoEvent && this.value == other.value
    }
}
class OpusVolumeEvent(var value: Int, var transition: Int = 0): OpusControlEvent() {
    override fun equals(other: Any?): Boolean {
        return other is OpusVolumeEvent && this.value == other.value && this.transition == other.transition
    }
}
class OpusReverbEvent(var value: Float): OpusControlEvent() {
    override fun equals(other: Any?): Boolean {
        return other is OpusReverbEvent && this.value == other.value
    }
}

