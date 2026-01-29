package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class OpusTempoEvent(value: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): SingleFloatEvent(value, duration, transition) {
    override val event_type = EffectType.Tempo
    override fun copy(): OpusTempoEvent {
        return OpusTempoEvent(this.value, this.duration, this.transition)
    }

    override fun equals(other: Any?): Boolean {
        return other is OpusTempoEvent && super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + this.event_type.hashCode()
        return result
    }
}