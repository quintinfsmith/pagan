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
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONString
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.asEffectTransition


class OpusVelocityEvent(
    var value: Float = 1F,
    var slide: Pair<SlideMaxWidth, Int>? = null,
    duration: Int = 1,
    transition: EffectTransition = EffectTransition.Instant
): EffectEvent(duration, transition) {
    enum class SlideMaxWidth {
        Beat,
        Note
    }
    companion object: Deserializable<OpusVelocityEvent> {
        override fun from_json(map: JSONHashMap): OpusVelocityEvent {
            val value = if (map["velocity"] is JSONInteger) {
                map.get_int("velocity").toFloat() / 128F
            } else {
                map.get_float("velocity")
            }

            val slide_list = map.get_listn("slide")

            return OpusVelocityEvent(
                value,
                if (slide_list != null) {
                    Pair(
                        OpusVelocityEvent.SlideMaxWidth.valueOf(slide_list.get_string(0)),
                        slide_list.get_intn(1) ?: 1
                    )
                } else {
                    null
                },
                map.get_intn("duration") ?: 1,
                /* Note: Need the try catch since I initially had transitions as int, but only used 0 */
                try {
                    (map.get_stringn("transition") ?: "Instant").asEffectTransition()
                } catch (e: ClassCastException) {
                    EffectTransition.Instant
                }
            )
        }


    }

    override val event_type = EffectType.Velocity
    override fun to_float_array(): FloatArray {
        return floatArrayOf(this.value) // 1.27 == 1
    }
    override fun copy(): OpusVelocityEvent {
        return OpusVelocityEvent(
            this.value,
            this.slide,
            this.duration,
            this.transition
        )
    }

    override fun get_event_instant(
        position: Rational,
        preceding_event: EffectEvent
    ): EffectEvent {
        return OpusVelocityEvent(
            this.value,
            this.slide,
            this.duration,
            this.transition
        )
    }

    override fun apply_to_json(json: JSONHashMap) {
        json["velocity"] = this.value
        json["slide"] = this.slide?.let {
            JSONList(
                JSONString(it.first.name),
                JSONInteger(it.second)
            )
        }
    }

    override fun hashCode(): Int {
        val code = super.hashCode().xor(this.value.toRawBits())
        val shift = this.transition.i
        return (code shl shift) + (code shr (32 - shift))
    }

    override fun equals(other: Any?): Boolean {
        return other is OpusVelocityEvent
                && this.value == other.value
                && this.slide == other.slide
                && this.transition == other.transition
                && super.equals(other)
    }
}