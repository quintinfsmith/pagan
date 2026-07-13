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
import com.qfs.pagan.jsoninterfaces.OpusTreeJSONInterface
import com.qfs.pagan.jsoninterfaces.UnknownControllerException
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.DelayController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.HighPassController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.LowPassController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.PanController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.TempoController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.VelocityController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.VolumeController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.HighPassEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.LowPassEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.TTT
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.json_string
import com.qfs.pagan.structure.rationaltree.ReducibleTree

object ActiveControllerJSONInterface {
    fun <T: EffectEvent> from_json(obj: JSONHashMap, size: Int): EffectController<out EffectEvent> {
        val output = when (val label = obj.get_string("type")) {
            "tempo" -> {
                val controller = TempoController(size)
                controller.populate_controller_from_json(obj, OpusTempoEvent)
                controller
            }
            "volume" -> {
                val controller = VolumeController(size)
                controller.populate_controller_from_json(obj, OpusVolumeEvent)
                controller
            }
            "velocity" -> {
                val controller = VelocityController(size)
                controller.populate_controller_from_json(obj, OpusVelocityEvent)
                controller
            }
            "pan" -> {
                val controller = PanController(size)
                controller.populate_controller_from_json(obj, OpusPanEvent)
                controller
            }
            "delay" -> {
                val controller = DelayController(size)
                controller.populate_controller_from_json(obj, DelayEvent)
                controller
            }
            "lowpass" -> {
                val controller = LowPassController(size)
                controller.populate_controller_from_json(obj, LowPassEvent)
                controller
            }
            "highpass" -> {
                val controller = HighPassController(size)
                controller.populate_controller_from_json(obj, HighPassEvent)
                controller
            }
            else -> throw UnknownControllerException(label)
        }


        output.visible = obj.get_booleann("visible") ?: false

        return output
    }

    fun convert_v2_to_v3(input: JSONHashMap): JSONHashMap {
        val input_children = input.get_list("children")
        val events = JSONList()

        for (i in 0 until input_children.size) {
            val pair = input_children.get_hashmap(i)
            val generalized_tree = OpusTreeJSONInterface.convert_v1_to_v3(pair["second"] as JSONHashMap) { input_event: JSONHashMap ->
                OpusControlEventJSONInterface.convert_v2_to_v3(input_event)
            }
            if (generalized_tree == null) {
                continue
            }
            events.add(
                JSONList(
                    pair["first"],
                    generalized_tree
                )
            )
        }

        val controller_type = input.get_stringn("type")
        return JSONHashMap(
            "events" to events,
            "type" to JSONString(
                when (controller_type) {
                    "Tempo" -> "tempo"
                    "Volume" -> "volume"
                    "Pan" -> "pan"
                    else -> throw UnknownControllerException(controller_type)
                }
            ),
            "initial" to OpusControlEventJSONInterface.convert_v2_to_v3(input.get_hashmap("initial_value")),
        )
    }
}
