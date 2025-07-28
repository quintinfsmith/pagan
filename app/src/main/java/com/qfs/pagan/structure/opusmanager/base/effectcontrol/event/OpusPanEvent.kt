package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class OpusPanEvent(value: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): SingleFloatEvent(value, duration, transition) {
    override val event_type = EffectType.Pan
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value)
    }
    override fun copy(): OpusPanEvent {
        return OpusPanEvent(this.value, this.duration, this.transition)
    }

    override fun equals(other: Any?): Boolean {
        return other is OpusPanEvent && this.value == other.value && this.transition == other.transition && super.equals(other)
    }

    override fun hashCode(): Int {
        val code = super.hashCode().xor(this.value.toRawBits())
        val shift = when (this.transition) {
            EffectTransition.Instant -> 0
            EffectTransition.Linear -> 1
        }
        return (code shl shift) + (code shr (32 - shift))
    }
}