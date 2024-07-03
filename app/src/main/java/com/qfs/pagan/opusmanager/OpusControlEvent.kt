package com.qfs.pagan.opusmanager

abstract class OpusControlEvent: OpusEvent() {
    abstract fun copy(): OpusControlEvent
}

enum class ControlEventType {
    Tempo,
    Volume,
    Reverb,
}

class OpusTempoEvent(var value: Float): OpusControlEvent() {
    override fun equals(other: Any?): Boolean {
        return other is OpusTempoEvent && this.value == other.value
    }

    override fun copy(): OpusTempoEvent {
        return OpusTempoEvent(this.value)
    }
}
class OpusVolumeEvent(var value: Int, var transition: Int = 0): OpusControlEvent() {
    override fun copy(): OpusVolumeEvent {
        return OpusVolumeEvent(this.value, this.transition)
    }

    override fun equals(other: Any?): Boolean {
        return other is OpusVolumeEvent && this.value == other.value && this.transition == other.transition
    }
}
class OpusReverbEvent(var value: Float): OpusControlEvent() {
    override fun copy(): OpusReverbEvent {
        return OpusReverbEvent(this.value)
    }
    override fun equals(other: Any?): Boolean {
        return other is OpusReverbEvent && this.value == other.value
    }
}

