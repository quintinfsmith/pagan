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

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && other is EffectEvent && other.transition == this.transition
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + this.transition.hashCode()
        result = 31 * result + this.event_type.hashCode()
        return result
    }
}