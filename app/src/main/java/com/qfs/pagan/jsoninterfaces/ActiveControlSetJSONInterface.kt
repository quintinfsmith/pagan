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
import com.qfs.json.JSONList
import com.qfs.pagan.jsoninterfaces.UnhandledControllerException
import com.qfs.pagan.jsoninterfaces.UnknownControllerException
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectControlSet
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.DelayController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.PanController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.TempoController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.VelocityController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.VolumeController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent

object ActiveControlSetJSONInterface {
    fun from_json(json_obj: JSONHashMap, size: Int): EffectControlSet {
        val control_set = EffectControlSet(size)
        for (json_controller in json_obj.get_listn("controllers") ?: JSONList()) {
            val controller = when (val label = (json_controller as JSONHashMap).get_string("type")) {
                "tempo" -> ActiveControllerJSONInterface.from_json<OpusTempoEvent>(json_controller, size)
                "volume" -> ActiveControllerJSONInterface.from_json<OpusVolumeEvent>(json_controller, size)
                "velocity" -> ActiveControllerJSONInterface.from_json<OpusVelocityEvent>(json_controller, size)
                "pan" -> ActiveControllerJSONInterface.from_json<OpusPanEvent>(json_controller, size)
                "delay" -> ActiveControllerJSONInterface.from_json<DelayEvent>(json_controller, size)
                else -> throw UnknownControllerException(label)
            }

            val key = when (controller) {
                is TempoController -> EffectType.Tempo
                is VolumeController -> EffectType.Volume
                is PanController -> EffectType.Pan
                is VelocityController -> EffectType.Velocity
                is DelayController -> EffectType.Delay
                //is ReverbController -> ControlEventType.Reverb
                else -> throw UnhandledControllerException(controller)
            }
            control_set.new_controller(key, controller)
        }
        return control_set
    }

    fun to_json(control_set: EffectControlSet): JSONHashMap {
        val output = JSONHashMap()
        output["beat_count"] = control_set.beat_count

        val controllers = control_set.controllers.values.toList()
        output["controllers"] = JSONList(controllers.size) {
            ActiveControllerJSONInterface.to_json(controllers[it])
        }

        return output
    }

    fun convert_v2_to_v3(input: JSONList, beat_count: Int): JSONHashMap {
        return JSONHashMap(
            "beat_count" to beat_count,
            "controllers" to JSONList(input.size) { i: Int ->
                ActiveControllerJSONInterface.convert_v2_to_v3(input.get_hashmap(i))
            }
        )
    }
}
