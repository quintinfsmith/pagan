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
import com.qfs.pagan.jsoninterfaces.UnhandledControllerException
import com.qfs.pagan.jsoninterfaces.UnknownControllerException
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.DelayController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.PanController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.TempoController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.VelocityController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.VolumeController
import com.qfs.pagan.structure.rationaltree.ReducibleTree

object ActiveControllerJSONInterface {
    fun <T: EffectEvent> from_json(obj: JSONHashMap, size: Int): EffectController<out EffectEvent> {
        val output = when (val label = obj.get_string("type")) {
            "tempo" -> {
                val controller = TempoController(size)
                controller.set_initial_event(OpusControlEventJSONInterface.tempo_event(obj.get_hashmap("initial")))
                this.populate_controller(obj, controller, OpusControlEventJSONInterface::tempo_event)
                controller
            }
            "volume" -> {
                val controller = VolumeController(size)
                controller.set_initial_event(OpusControlEventJSONInterface.volume_event(obj.get_hashmap("initial")))
                this.populate_controller(obj, controller, OpusControlEventJSONInterface::volume_event)
                controller
            }
            "velocity" -> {
                val controller = VelocityController(size)
                controller.set_initial_event(OpusControlEventJSONInterface.velocity_event(obj.get_hashmap("initial")))
                this.populate_controller(obj, controller, OpusControlEventJSONInterface::velocity_event)
                controller
            }
            "pan" -> {
                val controller = PanController(size)
                controller.set_initial_event(OpusControlEventJSONInterface.pan_event(obj.get_hashmap("initial")))
                this.populate_controller(obj, controller, OpusControlEventJSONInterface::pan_event)
                controller
            }
            "delay" -> {
                val controller = DelayController(size)
                controller.set_initial_event(OpusControlEventJSONInterface.delay_event(obj.get_hashmap("initial")))
                this.populate_controller(obj, controller, OpusControlEventJSONInterface::delay_event)
                controller
            }
            else -> throw UnknownControllerException(label)
        }


        output.visible = obj.get_booleann("visible") ?: false

        return output
    }

    private fun <T: EffectEvent> populate_controller(obj: JSONHashMap, controller: EffectController<T>, converter: (JSONHashMap) -> T) {
        for (pair in obj.get_list("events")) {
            val index = (pair as JSONList).get_int(0)
            val value = pair.get_hashmapn(1) ?: continue

            val generic_event = OpusTreeJSONInterface.from_json(value) { event: JSONHashMap? ->
                if (event == null) {
                    null
                } else {
                    converter(event)
                }
            }
            controller.beats[index] = generic_event
        }
        controller.init_blocked_tree_caches()
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

    fun to_json(controller: EffectController<out EffectEvent>): JSONHashMap {
        val map = JSONHashMap()
        val event_list = JSONList()
        controller.beats.forEachIndexed { i: Int, event_tree: ReducibleTree<out EffectEvent>? ->
            if (event_tree == null) return@forEachIndexed
            val generalized_tree = OpusTreeJSONInterface.to_json(event_tree) { event: EffectEvent ->
                OpusControlEventJSONInterface.to_json(event)
            } ?: return@forEachIndexed

            event_list.add(
                JSONList(
                    JSONInteger(i),
                    generalized_tree
                )
            )
        }

        map["events"] = event_list
        map["initial"] = OpusControlEventJSONInterface.to_json(controller.initial_event)
        map["type"] = when (controller) {
            is TempoController -> "tempo"
            is VolumeController -> "volume"
            is VelocityController -> "velocity"
            is PanController -> "pan"
            is DelayController -> "delay"
            else -> throw UnhandledControllerException(controller)
        }
        map["visible"] = controller.visible

        return map
    }
}
