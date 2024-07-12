package com.qfs.pagan.opusmanager

import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList

class ActiveControlSetJSONInterface() {
    class UnknownControllerException(): Exception()
    companion object {
        fun from_json(json_obj: JSONHashMap, size: Int): ActiveControlSet {
            val control_set = ActiveControlSet(size)
            for (json_controller in json_obj.get_listn("controllers")?.list ?: listOf()) {
                val controller = ActiveControllerJSONInterface.from_json(json_controller as JSONHashMap, size)
                val key = when (controller) {
                    is TempoController -> ControlEventType.Tempo
                    is VolumeController -> ControlEventType.Volume
                    //is ReverbController -> ControlEventType.Reverb
                    else -> throw UnknownControllerException()
                }
                control_set.new_controller(key, controller)
            }
            return control_set
        }

        fun to_json(control_set: ActiveControlSet): JSONHashMap {
            val output = JSONHashMap()
            output["beat_count"] = control_set.beat_count

            val controllers = control_set.controllers.values.toList()
            output["controllers"] = JSONList(
                MutableList(controllers.size) {
                    ActiveControllerJSONInterface.to_json(controllers[it])
                }
            )

            return output
        }

        fun convert_v2_to_v3(input: JSONList, beat_count: Int): JSONHashMap {
            return JSONHashMap(
                hashMapOf(
                    "beat_count" to JSONInteger(beat_count),
                    "controllers" to JSONList(
                        MutableList(input.list.size) { i: Int ->
                            ActiveControllerJSONInterface.convert_v2_to_v3(input.get_hashmap(i))
                        }
                    )
                )
            )
        }
    }
}
