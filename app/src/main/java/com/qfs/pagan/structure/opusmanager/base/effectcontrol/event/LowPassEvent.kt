package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import kotlin.math.pow

class LowPassEvent(var limit_lower: Float?, var resonance: Float?, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): EffectEvent(duration, transition) {
    override val event_type = EffectType.LowPass
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.limit_lower ?: 0F, this.resonance ?: 0F)
    }
    override fun equals(other: Any?): Boolean {
        return other is LowPassEvent && this.limit_lower == other.limit_lower && this.resonance == other.resonance && super.equals(other)
    }
    override fun copy(): LowPassEvent {
        return LowPassEvent(
            this.limit_lower,
            this.resonance
        )
    }

    override fun get_event_instant(position: Rational, preceding_event: EffectEvent): LowPassEvent {
        if (preceding_event !is LowPassEvent) throw Exception("Invalid event passed")

        val copy_event = this.copy()
        when (this.transition) {
            EffectTransition.Linear -> {
                val diff_limit_lower = (this.limit_lower ?: 0f) - (preceding_event.limit_lower ?: 0f)
                val diff_resonance = (this.resonance ?: 0f) - (preceding_event.resonance ?: 0f)

                copy_event.limit_lower = (preceding_event.limit_lower ?: 0F) + (diff_limit_lower * position.toFloat())
                copy_event.resonance = (preceding_event.resonance ?: 0F) + (diff_resonance * position.toFloat())
            }
            EffectTransition.RLinear -> {
                val diff_limit_lower = (preceding_event.limit_lower ?: 0F) - (this.limit_lower ?: 0F)
                val diff_resonance = (preceding_event.resonance ?: 0F) - (this.resonance ?: 0F)

                copy_event.limit_lower = (this.limit_lower ?: 0F) + (diff_limit_lower * position.toFloat())
                copy_event.resonance = (this.resonance ?: 1F) + (diff_resonance * position.toFloat())
            }

            EffectTransition.Instant -> {}
            EffectTransition.RInstant -> {}
        }

        return copy_event
    }
}
