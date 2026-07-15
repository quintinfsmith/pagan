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
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.asEffectTransition
import kotlin.math.pow

class OpusVolumeEvent(
    override var value: Float = 1F,
    override var duration: Int = 1,
    override var transition: EffectTransition = EffectTransition.Instant
): SingleFloatEvent<OpusVolumeEvent> {
    companion object: Deserializable<OpusVolumeEvent> {
        override fun from_json(map: JSONHashMap): OpusVolumeEvent {
            val value = if (map["volume"] is JSONInteger) {
                map.get_int("volume").toFloat() / 128F
            } else {
                map.get_float("volume")
            }
            return OpusVolumeEvent(
                value,
                map.get_int("duration", 1),
                /* Note: Need the try catch since I initially had transitions as int, but only used 0 */
                try {
                    map.get_string("transition", "Instant").asEffectTransition()
                } catch (e: ClassCastException) {
                    EffectTransition.Instant
                }
            )
        }
    }

    override val event_type = EffectType.Volume
    override fun to_float_array(): FloatArray {
        val adjusted = this.value / 1.27F
        return floatArrayOf(adjusted.pow(1.5F)) // 1.27 == 1
    }
    override fun equals(other: Any?): Boolean {
        return other is OpusVolumeEvent && this.value == other.value && this.transition == other.transition && super.equals(other)
    }
    override fun copy(): OpusVolumeEvent {
        return OpusVolumeEvent(this.value, this.duration, this.transition)
    }

    override fun apply_to_json(json: JSONHashMap) {
        json["volume"] = this.value
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + this.event_type.hashCode()
        return result
    }
}