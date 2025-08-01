package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

abstract class EffectEvent(duration: Int = 1, var transition: EffectTransition = EffectTransition.Instant): OpusEvent(duration) {
    abstract val event_type: EffectType
    // TODO: within hashCodes, account for transition being moved here
    abstract override fun copy(): EffectEvent
    abstract fun to_float_array(): FloatArray
    abstract fun get_event_instant(position: Rational, preceding_event: EffectEvent): EffectEvent

    fun is_reset_transition(): Boolean {
        // reset types not implemented yet
        return false
    }

}