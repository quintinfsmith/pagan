package com.qfs.pagan.opusmanager

import com.qfs.json.ParsedHashMap
import com.qfs.json.ParsedInt
import com.qfs.json.ParsedList

class ActiveControlSetGeneralizer() {
    class UnknownControllerException(): Exception()
    companion object {
        fun from_json(json_obj: ParsedHashMap, size: Int): ActiveControlSet {
            val control_set = ActiveControlSet(size)
            for (json_controller in json_obj.get_listn("controllers")?.list ?: listOf()) {
                val controller = ActiveControllerGeneralizer.from_json(json_controller as ParsedHashMap, size)
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

        fun to_json(control_set: ActiveControlSet): ParsedHashMap {
            val output = ParsedHashMap()
            output["beat_count"] = control_set.beat_count

            val controllers = control_set.controllers.values.toList()
            output["controllers"] = ParsedList(
                MutableList(controllers.size) {
                    ActiveControllerGeneralizer.to_json(controllers[it])
                }
            )

            return output
        }

        fun convert_v2_to_v3(input: ParsedList, beat_count: Int): ParsedHashMap {
            return ParsedHashMap(
                hashMapOf(
                    "beat_count" to ParsedInt(beat_count),
                    "controllers" to ParsedList(
                        MutableList(input.list.size) { i: Int ->
                            ActiveControllerGeneralizer.convert_v2_to_v3(input.get_hashmap(i))
                        }
                    )
                )
            )
        }
    }
}
