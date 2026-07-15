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

import com.qfs.json.Deserializable
import com.qfs.json.JSONHashMap
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.asEffectTransition

interface FilterEvent<T: FilterEvent<T>>: EffectEvent<T> {
    var filter_cutoff: Float
    var resonance: Float?

    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.filter_cutoff, this.resonance ?: 0F)
    }
    // override fun equals(other: Any?): Boolean {
    //     return other is FilterEvent && this.filter_cutoff == other.filter_cutoff && this.resonance == other.resonance && super.equals(other)
    // }

    // abstract override fun copy(): FilterEvent

    override fun get_event_instant(position: Rational, preceding_event: T): T {
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
    override var filter_cutoff: Float = 20000F,
    override var resonance: Float? = null,
    override var duration: Int = 1,
    override var transition: EffectTransition = EffectTransition.Instant
): FilterEvent<LowPassEvent> {
    companion object: Deserializable<LowPassEvent> {
        override fun from_json(map: JSONHashMap): LowPassEvent {
            return LowPassEvent(
                filter_cutoff = map.get_float("cutoff"),
                resonance = map.get_floatn("res"),
                map.get_int("duration", 1),
                map.get_string("transition", "Instant").asEffectTransition()
            )
        }
    }

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

    override fun apply_to_json(json: JSONHashMap) {
        json["cutoff"] = this.filter_cutoff
        json["res"] = this.resonance
    }
}

class HighPassEvent(
    override var filter_cutoff: Float = 0F,
    override var duration: Int = 1,
    override var transition: EffectTransition = EffectTransition.Instant
): FilterEvent<HighPassEvent> {
    override var resonance: Float? = null
    companion object: Deserializable<HighPassEvent> {
        override fun from_json(map: JSONHashMap): HighPassEvent {
            return HighPassEvent(
                filter_cutoff = map.get_float("cutoff"),
                duration = map.get_int("duration", 1),
                transition = map.get_string("transition", "Instant").asEffectTransition()
            )
        }
    }
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

    override fun apply_to_json(json: JSONHashMap) {
        json["cutoff"] = this.filter_cutoff
    }
}
