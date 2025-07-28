package com.qfs.pagan.structure.opusmanager

import com.qfs.json.JSONHashMap
import com.qfs.json.JSONList
import com.qfs.pagan.jsoninterfaces.UnhandledControllerException
import com.qfs.pagan.jsoninterfaces.UnknownControllerException
import com.qfs.pagan.structure.opusmanager.base.EffectControlSet
import com.qfs.pagan.structure.opusmanager.base.ControlEventType
import com.qfs.pagan.structure.opusmanager.base.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontroller.PanController
import com.qfs.pagan.structure.opusmanager.base.effectcontroller.TempoController
import com.qfs.pagan.structure.opusmanager.base.effectcontroller.VelocityController
import com.qfs.pagan.structure.opusmanager.base.effectcontroller.VolumeController

class ActiveControlSetJSONInterface {
    companion object {
        fun from_json(json_obj: JSONHashMap, size: Int): EffectControlSet {
            val control_set = EffectControlSet(size)
            for (json_controller in json_obj.get_listn("controllers") ?: JSONList()) {
                val controller = when (val label = (json_controller as JSONHashMap).get_string("type")) {
                    "tempo" -> ActiveControllerJSONInterface.from_json<OpusTempoEvent>(json_controller, size)
                    "volume" -> ActiveControllerJSONInterface.from_json<OpusVolumeEvent>(json_controller, size)
                    "velocity" -> ActiveControllerJSONInterface.from_json<OpusVelocityEvent>(json_controller, size)
                    "pan" -> ActiveControllerJSONInterface.from_json<OpusPanEvent>(json_controller, size)
                    else -> throw UnknownControllerException(label)
                }

                val key = when (controller) {
                    is TempoController -> ControlEventType.Tempo
                    is VolumeController -> ControlEventType.Volume
                    is PanController -> ControlEventType.Pan
                    is VelocityController -> ControlEventType.Velocity
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
}
