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

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class OpusReverbEvent(value: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): SingleFloatEvent(value, duration, transition) {
    override val event_type = EffectType.Reverb
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value)
    }

    override fun copy(): OpusReverbEvent {
        return OpusReverbEvent(this.value, this.duration, this.transition)
    }

    override fun hashCode(): Int {
        return super.hashCode().xor(this.value.toRawBits())
    }
    override fun equals(other: Any?): Boolean {
        return other is OpusReverbEvent && this.value == other.value && super.equals(other)
    }
}