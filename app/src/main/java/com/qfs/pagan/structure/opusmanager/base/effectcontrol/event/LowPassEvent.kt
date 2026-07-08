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
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.HighPassController

abstract class FilterEvent(var filter_cutoff: Float, var resonance: Float?, duration: Int, transition: EffectTransition): EffectEvent(duration, transition) {
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.filter_cutoff, this.resonance ?: 0F)
    }
    override fun equals(other: Any?): Boolean {
        return other is FilterEvent && this.filter_cutoff == other.filter_cutoff && this.resonance == other.resonance && super.equals(other)
    }

    abstract override fun copy(): FilterEvent

    override fun get_event_instant(position: Rational, preceding_event: EffectEvent): FilterEvent {
        if (preceding_event !is FilterEvent) throw Exception("Invalid event passed")

        val copy_event = this.copy()
        when (this.transition) {
            EffectTransition.LinearB -> {
                if (position <= 1) {
                    val diff_limit_lower = (this.filter_cutoff) - (preceding_event.filter_cutoff)
                    val diff_resonance = (this.resonance ?: 0F) - (preceding_event.resonance ?: 0F)
                    copy_event.filter_cutoff = (preceding_event.filter_cutoff) + (diff_limit_lower * position.toFloat())
                    copy_event.resonance = (preceding_event.resonance ?: 0F) + (diff_resonance * position.toFloat())
                }
            }
            EffectTransition.Linear -> {
                val diff_limit_lower = (this.filter_cutoff) - (preceding_event.filter_cutoff)
                val diff_resonance = (this.resonance ?: 0F) - (preceding_event.resonance ?: 0F)

                copy_event.filter_cutoff = (preceding_event.filter_cutoff) + (diff_limit_lower * position.toFloat())
                copy_event.resonance = (preceding_event.resonance ?: 0F) + (diff_resonance * position.toFloat())
            }
            EffectTransition.RLinear -> {
                val diff_limit_lower = (preceding_event.filter_cutoff) - (this.filter_cutoff)
                val diff_resonance = (preceding_event.resonance ?: 0F) - (this.resonance ?: 0F)

                copy_event.filter_cutoff = (this.filter_cutoff) + (diff_limit_lower * position.toFloat())
                copy_event.resonance = (this.resonance ?: 0F) + (diff_resonance * position.toFloat())
            }

            EffectTransition.Instant -> {}
            EffectTransition.InstantB -> {}
        }

        return copy_event
    }
}
class LowPassEvent(
    filter_cutoff: Float,
    resonance: Float?,
    duration: Int = 1,
    transition: EffectTransition = EffectTransition.Instant
): FilterEvent(filter_cutoff, resonance, duration, transition) {
    override val event_type = EffectType.LowPass

    override fun equals(other: Any?): Boolean {
        return other is LowPassEvent && super.equals(other)
    }

    override fun copy(): LowPassEvent {
        return LowPassEvent(
            this.filter_cutoff,
            this.resonance,
            this.duration,
            this.transition
        )
    }
}

class HighPassEvent(
    filter_cutoff: Float,
    duration: Int = 1,
    transition: EffectTransition = EffectTransition.Instant
): FilterEvent(filter_cutoff, null, duration, transition) {
    override val event_type = EffectType.HighPass
    override fun equals(other: Any?): Boolean {
        return other is HighPassEvent && super.equals(other)
    }
    override fun copy(): HighPassEvent {
        return HighPassEvent(
            this.filter_cutoff,
            this.duration,
            this.transition
        )
    }
}
