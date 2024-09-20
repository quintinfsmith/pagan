package com.qfs.pagan.opusmanager

abstract class OpusControlEvent: OpusEvent() {
    abstract fun copy(): OpusControlEvent
}

enum class ControlEventType {
    Tempo,
    Volume,
    Reverb,
}

enum class ControlTransition {
    Instant,
    Linear,
    Concave,
    Convex
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

class OpusBendEvent(var numerator: Int, var denominator: Int, var transition: ControlTransition = ControlTransition.Instant): OpusControlEvent() {
    override fun copy(): OpusBendEvent {
        return OpusBendEvent(this.numerator, this.denominator, this.transition)
    }
    override fun equals(other: Any?): Boolean {
        return other is OpusBendEvent && this.numerator == other.numerator && this.denominator == other.denominator && this.transition == other.transition
    }
}
