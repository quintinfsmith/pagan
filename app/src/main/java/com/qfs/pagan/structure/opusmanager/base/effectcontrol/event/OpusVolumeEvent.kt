package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import kotlin.math.pow

class OpusVolumeEvent(value: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): SingleFloatEvent(value, duration, transition) {
    override val event_type = EffectType.Volume
    override fun to_float_array(): FloatArray {
        val adjusted = this.value / 1.27F
        return floatArrayOf(adjusted.pow(1.5F)) // 1.27 == 1
    }
    override fun equals(other: Any?): Boolean {
        return other is OpusVolumeEvent && this.value == other.value && this.transition == other.transition && super.equals(other)
    }
    override fun copy(): OpusVolumeEvent {
        return OpusVolumeEvent(this.value, this.duration, this.transition)
    }
}