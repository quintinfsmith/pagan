package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class BendTransitionEvent(val transition_duration: Rational = Rational(0, 1), duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): EffectEvent(duration, transition) {
    override val event_type: EffectType
        get() = EffectType.BendTransition

    override fun to_float_array(): FloatArray { return FloatArray(0) } // Not Needed

    override fun copy(): EffectEvent {
        return BendTransitionEvent(
            this.transition_duration,
            this.duration,
            this.transition
        )
    }

    override fun get_event_instant(position: Rational, preceding_event: EffectEvent): EffectEvent {
        return this.copy()
    }
}

class PitchEvent(pitch: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): SingleFloatEvent(pitch, duration, transition) {
    override val event_type: EffectType
        get() = EffectType.Pitch

    override fun copy(): PitchEvent {
        return PitchEvent(
            this.value,
            this.duration,
            this.transition
        )
    }
}
