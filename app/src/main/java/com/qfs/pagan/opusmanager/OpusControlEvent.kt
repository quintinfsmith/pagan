package com.qfs.pagan.opusmanager

enum class ControlEventType {
    Tempo,
    Volume,
    Reverb,
    Pan
}

enum class ControlTransition {
    Instant,
    Linear
   // Concave,
   // Convex
}

abstract class OpusControlEvent(duration: Int = 1): OpusEvent(duration) {
    abstract override fun copy(): OpusControlEvent
}


class OpusTempoEvent(var value: Float, duration: Int = 1): OpusControlEvent(duration) {
    override fun equals(other: Any?): Boolean {
        return other is OpusTempoEvent && this.value == other.value && super.equals(other)
    }

    override fun copy(): OpusTempoEvent {
        return OpusTempoEvent(this.value, this.duration)
    }
}

class OpusVolumeEvent(var value: Int, var transition: ControlTransition = ControlTransition.Instant, duration: Int = 1): OpusControlEvent(duration) {
    override fun copy(): OpusVolumeEvent {
        return OpusVolumeEvent(this.value, this.transition, this.duration)
    }

    override fun equals(other: Any?): Boolean {
        return other is OpusVolumeEvent && this.value == other.value && this.transition == other.transition && super.equals(other)
    }
}

class OpusReverbEvent(var value: Float, duration: Int = 1): OpusControlEvent(duration) {
    override fun copy(): OpusReverbEvent {
        return OpusReverbEvent(this.value, this.duration)
    }
    override fun equals(other: Any?): Boolean {
        return other is OpusReverbEvent && this.value == other.value && super.equals(other)
    }
}

class OpusPanEvent(var value: Float, var transition: ControlTransition = ControlTransition.Instant, duration: Int = 1): OpusControlEvent(duration) {
    override fun copy(): OpusPanEvent {
        return OpusPanEvent(this.value, this.transition, this.duration)
    }
    override fun equals(other: Any?): Boolean {
        return other is OpusPanEvent && this.value == other.value && this.transition == other.transition && super.equals(other)
    }
}
