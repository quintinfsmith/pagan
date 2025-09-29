package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import kotlin.math.pow

class BandPassEvent(var limit_lower: Float?, var limit_upper: Float?, var resonance: Float?, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): EffectEvent(duration, transition) {
    override val event_type = EffectType.BandPass
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.limit_lower ?: 0F, this.limit_upper ?: 0F, this.resonance ?: 0F)
    }
    override fun equals(other: Any?): Boolean {
        return other is BandPassEvent && this.limit_lower == other.limit_lower && this.limit_upper == other.limit_upper && this.resonance == other.resonance && super.equals(other)
    }
    override fun copy(): BandPassEvent {
        return BandPassEvent(
            this.limit_lower,
            this.limit_upper,
            this.resonance
        )
    }

    override fun get_event_instant(position: Rational, preceding_event: EffectEvent): EffectEvent {
        TODO("Not yet implemented")
    }
}