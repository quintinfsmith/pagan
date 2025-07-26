package com.qfs.pagan.structure.opusmanager.base

import com.qfs.pagan.structure.Rational


enum class ControlEventType(val i: Int) {
    Tempo(0),
    Volume(1),
    Reverb(2),
    Pan(3),
    Velocity(4)
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
    abstract fun to_float_array(): FloatArray
    abstract fun get_event_instant(position: Rational, preceding_event: OpusControlEvent): OpusControlEvent

    fun is_reset_transition(): Boolean {
        // reset types not implemented yet
        return false
    }

}

open class SingleFloatEvent(var value: Float, duration: Int = 1, transition: ControlTransition = ControlTransition.Instant): OpusControlEvent(duration, transition) {
    override fun equals(other: Any?): Boolean {
        return other is SingleFloatEvent && this.value == other.value && super.equals(other)
    }

    override fun hashCode(): Int {
        val code = super.hashCode().xor(this.value.toRawBits())
        val shift = when (this.transition) {
            ControlTransition.Instant -> 0
            ControlTransition.Linear -> 1
        }
        return (code shl shift) + (code shr (32 - shift))
    }

    override fun copy(): SingleFloatEvent {
        return SingleFloatEvent(this.value, this.duration, this.transition)
    }

    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value)
    }

    override fun get_event_instant(position: Rational, preceding_event: OpusControlEvent): OpusControlEvent {
        val copy_event = this.copy()
        when (this.transition) {
            ControlTransition.Linear -> {
                val diff = this.value - (preceding_event as SingleFloatEvent).value
                copy_event.value = preceding_event.value + (diff * position.toFloat())
            }
            ControlTransition.Instant -> {}
        }

        return copy_event
    }
}

class OpusTempoEvent(value: Float, duration: Int = 1, transition: ControlTransition = ControlTransition.Instant): SingleFloatEvent(value, duration, transition)

class OpusVolumeEvent(value: Float, duration: Int = 1, transition: ControlTransition = ControlTransition.Instant): SingleFloatEvent(value, duration, transition) {
    override fun to_float_array(): FloatArray {
        val adjusted = this.value / 1.27F
        return floatArrayOf(adjusted * adjusted) // 1.27 == 1
    }
    override fun equals(other: Any?): Boolean {
        return other is OpusVolumeEvent && this.value == other.value && this.transition == other.transition && super.equals(other)
    }
}

class OpusReverbEvent(value: Float, duration: Int = 1, transition: ControlTransition = ControlTransition.Instant): SingleFloatEvent(value, duration, transition) {
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value)
    }

    override fun hashCode(): Int {
        return super.hashCode().xor(this.value.toRawBits())
    }
    override fun equals(other: Any?): Boolean {
        return other is OpusReverbEvent && this.value == other.value && super.equals(other)
    }
}

class OpusPanEvent(value: Float, duration: Int = 1, transition: ControlTransition = ControlTransition.Instant): SingleFloatEvent(value, duration, transition) {
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value)
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

class OpusVelocityEvent(value: Float, duration: Int = 1, transition: ControlTransition = ControlTransition.Instant): SingleFloatEvent(value, duration, transition) {
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value) // 1.27 == 1
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
        return other is OpusVelocityEvent && this.value == other.value && this.transition == other.transition && super.equals(other)
    }
}

