package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

abstract class EffectEvent(duration: Int = 1, var transition: EffectTransition = EffectTransition.Instant): OpusEvent(duration) {
    abstract val event_type: EffectType
    // TODO: within hashCodes, account for transition being moved here
    abstract fun to_float_array(): FloatArray
    abstract override fun copy(): EffectEvent
    abstract fun get_event_instant(position: Rational, preceding_event: EffectEvent): EffectEvent

    fun is_persistent(): Boolean {
        return when (this.transition) {
            EffectTransition.RInstant,
            EffectTransition.RLinear -> false
            else -> true
        }
    }
}