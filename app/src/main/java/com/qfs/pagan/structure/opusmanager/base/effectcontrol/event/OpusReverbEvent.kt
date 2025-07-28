package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class OpusReverbEvent(value: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): SingleFloatEvent(value, duration, transition) {
    override val event_type = EffectType.Reverb
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value)
    }

    override fun copy(): OpusReverbEvent {
        return OpusReverbEvent(this.value, this.duration, this.transition)
    }

    override fun hashCode(): Int {
        return super.hashCode().xor(this.value.toRawBits())
    }
    override fun equals(other: Any?): Boolean {
        return other is OpusReverbEvent && this.value == other.value && super.equals(other)
    }
}