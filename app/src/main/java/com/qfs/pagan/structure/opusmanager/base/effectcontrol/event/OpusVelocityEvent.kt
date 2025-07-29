package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class OpusVelocityEvent(value: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): SingleFloatEvent(value, duration, transition) {
    override val event_type = EffectType.Velocity
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value) // 1.27 == 1
    }
    override fun copy(): OpusVelocityEvent {
        return OpusVelocityEvent(this.value, this.duration, this.transition)
    }

    override fun hashCode(): Int {
        val code = super.hashCode().xor(this.value.toRawBits())
        val shift = this.transition.i
        return (code shl shift) + (code shr (32 - shift))
    }

    override fun equals(other: Any?): Boolean {
        return other is OpusVelocityEvent && this.value == other.value && this.transition == other.transition && super.equals(other)
    }
}