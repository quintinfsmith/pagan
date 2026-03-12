package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class PitchEvent(pitch: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): SingleFloatEvent(pitch, duration, transition) {
    init {
        println(">>> PITCH: $pitch")
    }
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
