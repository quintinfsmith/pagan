package com.qfs.pagan.opusmanager

enum class ControlEventType(val i: Int) {
    Tempo(0),
    Volume(1),
    Reverb(2),
    Pan(3)
}

enum class ControlTransition(val i: Int) {
    Instant(0),
    Linear(1)
   // Concave,
   // Convex
}

abstract class OpusControlEvent(duration: Int = 1, var transition: ControlTransition = ControlTransition.Instant): OpusEvent(duration) {
    // TODO: within hashCodes, account for transition being moved here
    abstract override fun copy(): OpusControlEvent
    abstract fun to_float(): Float
}

class OpusTempoEvent(var value: Float, duration: Int = 1): OpusControlEvent(duration) {
    override fun equals(other: Any?): Boolean {
        return other is OpusTempoEvent && this.value == other.value && super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode().xor(this.value.toRawBits())
    }

    override fun copy(): OpusTempoEvent {
        return OpusTempoEvent(this.value, this.duration)
    }

    override fun to_float(): Float {
        return this.value
    }
}

class OpusVolumeEvent(var value: Float, transition: ControlTransition = ControlTransition.Instant, duration: Int = 1): OpusControlEvent(duration, transition) {
    override fun copy(): OpusVolumeEvent {
        return OpusVolumeEvent(this.value, this.transition, this.duration)
    }
    override fun to_float(): Float {
        return this.value / 1.27F // 1.27 == 1
    }
    override fun hashCode(): Int {
        val code = super.hashCode().xor(this.value.toRawBits())
        val shift = when (this.transition) {
            ControlTransition.Instant -> 0
            ControlTransition.Linear -> 1
        }
        return (code shl shift) + (code shr (32 - shift))
    }
    override fun equals(other: Any?): Boolean {
        return other is OpusVolumeEvent && this.value == other.value && this.transition == other.transition && super.equals(other)
    }
}

class OpusReverbEvent(var value: Float, duration: Int = 1): OpusControlEvent(duration) {
    override fun copy(): OpusReverbEvent {
        return OpusReverbEvent(this.value, this.duration)
    }

    override fun to_float(): Float {
        return this.value
    }

    override fun hashCode(): Int {
        return super.hashCode().xor(this.value.toRawBits())
    }
    override fun equals(other: Any?): Boolean {
        return other is OpusReverbEvent && this.value == other.value && super.equals(other)
    }
}

class OpusPanEvent(var value: Float, transition: ControlTransition = ControlTransition.Instant, duration: Int = 1): OpusControlEvent(duration, transition) {
    override fun copy(): OpusPanEvent {
        return OpusPanEvent(this.value, this.transition, this.duration)
    }

    override fun to_float(): Float {
        return this.value
    }

    override fun equals(other: Any?): Boolean {
        return other is OpusPanEvent && this.value == other.value && this.transition == other.transition && super.equals(other)
    }

    override fun hashCode(): Int {
        val code = super.hashCode().xor(this.value.toRawBits())
        val shift = when (this.transition) {
            ControlTransition.Instant -> 0
            ControlTransition.Linear -> 1
        }
        return (code shl shift) + (code shr (32 - shift))
    }
}
