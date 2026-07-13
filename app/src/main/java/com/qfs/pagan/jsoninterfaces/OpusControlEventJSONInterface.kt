/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.structure.opusmanager
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONString
import com.qfs.pagan.jsoninterfaces.UnknownEventTypeException
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.asEffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.HighPassController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.HighPassEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.LowPassEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent

object OpusControlEventJSONInterface {
    fun convert_v2_to_v3(input: JSONHashMap): JSONHashMap {
        val output = JSONHashMap()
        val type = input.get_stringn("type")
        when (type) {
            "com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent" -> {
                output["tempo"] = input["value"]
            }
            "com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent" -> {
                output["volume"] = input["value"]
                output["transition"] = JSONInteger(0)
            }
            else -> throw UnknownEventTypeException(type)
        }

        return output
    }
    // ------------------------
}
