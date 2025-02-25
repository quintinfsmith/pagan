package com.qfs.pagan.opusmanager

import com.qfs.json.JSONHashMap
import com.qfs.json.JSONList
import com.qfs.pagan.opusmanager.ActiveControllerJSONInterface.UnknownControllerException

class ActiveControlSetJSONInterface {
    class UnknownControllerException : Exception()
    companion object {
        fun from_json(json_obj: JSONHashMap, size: Int): ActiveControlSet {
            val control_set = ActiveControlSet(size)
            for (json_controller in json_obj.get_listn("controllers")?.list ?: listOf()) {
                val controller = when (val label = (json_controller as JSONHashMap).get_string("type")) {
                    "tempo" -> {
                        ActiveControllerJSONInterface.from_json<OpusTempoEvent>(json_controller, size)
                    }
                    "volume" -> {
                        ActiveControllerJSONInterface.from_json<OpusVolumeEvent>(json_controller, size)
                    }
                    "pan" -> {
                        ActiveControllerJSONInterface.from_json<OpusPanEvent>(json_controller, size)
                    }
                    else -> throw UnknownControllerException(label)
                }

                val key = when (controller) {
                    is TempoController -> ControlEventType.Tempo
                    is VolumeController -> ControlEventType.Volume
                    is PanController -> ControlEventType.Pan
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
