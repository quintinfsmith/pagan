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
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class OpusVelocityEvent(
    var value: Float,
    var slide_duration: Rational? = null,
    duration: Int = 1,
    transition: EffectTransition = EffectTransition.Instant
): EffectEvent(duration, transition) {
    override val event_type = EffectType.Velocity
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value) // 1.27 == 1
    }
    override fun copy(): OpusVelocityEvent {
        return OpusVelocityEvent(this.value, this.slide_duration, this.duration, this.transition)
    }

    override fun get_event_instant(
        position: Rational,
        preceding_event: EffectEvent
    ): EffectEvent {
        return OpusVelocityEvent(
            this.value,
            this.slide_duration,
            this.duration,
            this.transition
        )
    }

    override fun hashCode(): Int {
        val code = super.hashCode().xor(this.value.toRawBits())
        val shift = this.transition.i
        return (code shl shift) + (code shr (32 - shift))
    }

    override fun equals(other: Any?): Boolean {
        return other is OpusVelocityEvent && this.value == other.value && this.slide_duration == other.slide_duration && this.transition == other.transition && super.equals(other)
    }
}