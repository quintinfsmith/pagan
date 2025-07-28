package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition

abstract class SingleFloatEvent(var value: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): EffectEvent(duration, transition) {
    override fun equals(other: Any?): Boolean {
        return other is SingleFloatEvent && this.value == other.value && super.equals(other)
    }

    override fun hashCode(): Int {
        val code = super.hashCode().xor(this.value.toRawBits())
        val shift = when (this.transition) {
            EffectTransition.Instant -> 0
            EffectTransition.Linear -> 1
        }
        return (code shl shift) + (code shr (32 - shift))
    }

    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value)
    }

    override fun get_event_instant(position: Rational, preceding_event: EffectEvent): EffectEvent {
        val copy_event = this.copy()
        when (this.transition) {
            EffectTransition.Linear -> {
                val diff = this.value - (preceding_event as SingleFloatEvent).value
                copy_event.value = preceding_event.value + (diff * position.toFloat())
            }
            EffectTransition.Instant -> {}
        }

        return copy_event
    }

    abstract override fun copy(): SingleFloatEvent
}