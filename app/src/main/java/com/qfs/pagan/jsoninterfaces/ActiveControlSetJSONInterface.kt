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
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.HighPassEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.LowPassEvent

object ActiveControlSetJSONInterface {
    fun from_json(json_obj: JSONHashMap, size: Int): EffectControlSet {
        val control_set = EffectControlSet(size)
        for (json_controller in json_obj.get_listn("controllers") ?: JSONList()) {
            if (json_controller !is JSONHashMap) continue
            // Ensure size exists, (it didn't initially)
            if (!json_controller.contains_key("size")) {
                json_controller["size"] = size
            }

            val controller = EffectController.from_json(json_controller)
            control_set.new_controller(controller)
        }
        return control_set
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
