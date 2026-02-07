/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition

abstract class SingleFloatEvent(var value: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): EffectEvent(duration, transition) {
    override fun equals(other: Any?): Boolean {
        return other is SingleFloatEvent && this.value == other.value && super.equals(other)
    }

    abstract override fun copy(): SingleFloatEvent

    override fun hashCode(): Int {
        val code = super.hashCode().xor(this.value.toRawBits())
        val shift = this.transition.i
        return (code shl shift) + (code shr (32 - shift))
    }

    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value)
    }

    override fun get_event_instant(position: Rational, preceding_event: EffectEvent): EffectEvent {
        val copy_event = this.copy()
        when (this.transition) {
            EffectTransition.Linear -> {
                val diff = this.value - (preceding_event as SingleFloatEvent).value
                copy_event.value = preceding_event.value + (diff * position.toFloat())
            }
            EffectTransition.RLinear -> {
                val diff = (preceding_event as SingleFloatEvent).value - this.value
                copy_event.value = this.value + (diff * position.toFloat())
            }
            EffectTransition.Instant -> {}
            EffectTransition.RInstant -> {}
        }

        return copy_event
    }

}