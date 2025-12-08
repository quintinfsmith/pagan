package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class OpusPanEvent(value: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): SingleFloatEvent(value, duration, transition) {
    companion object {
        var gen_id = 0;
    }
    val uuid = gen_id++
    override val event_type = EffectType.Pan
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value)
    }
    override fun equals(other: Any?): Boolean {
        return super.equals(other) && other is OpusPanEvent && this.value == other.value && this.transition == other.transition
    }

    override fun hashCode(): Int {
        val code = super.hashCode().xor(this.value.toRawBits())
        val shift = this.transition.i
        return (code shl shift) + (code shr (32 - shift))
    }

    override fun copy(): OpusPanEvent {
        return OpusPanEvent(this.value, this.duration, this.transition)
    }

    // override fun <T : OpusEvent> copy(): T {
   // }
}