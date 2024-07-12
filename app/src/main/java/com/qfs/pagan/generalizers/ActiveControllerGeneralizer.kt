package com.qfs.pagan.opusmanager

import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONObject
import com.qfs.json.JSONString
import com.qfs.pagan.generalizers.OpusTreeGeneralizer
import com.qfs.pagan.structure.OpusTree

class ActiveControllerGeneralizer {
    class UnknownControllerException(label: String): Exception("Unknown Controller: \"$label\"")
    companion object {
        fun from_json(obj: JSONHashMap, size: Int): ActiveController {
            val label = obj.get_string("type")
            val new_controller: ActiveController = when (label) {
                "tempo" -> TempoController(size)
                "volume" -> VolumeController(size)
                else -> throw UnknownControllerException(label)
            }

            new_controller.set_initial_event(
                when (new_controller) {
                    is TempoController -> OpusControlEventParser.tempo_event(obj.get_hashmap("initial"))
                    is VolumeController -> OpusControlEventParser.volume_event(obj.get_hashmap("initial"))
                    else -> throw UnknownControllerException(label)
                }
            )

            for (pair in obj.get_list("events").list) {
                val index = (pair as JSONList).get_int(0)
                val value = pair.get_hashmapn(1) ?: continue

                new_controller.events[index] = OpusTreeGeneralizer.from_json(value) { event: JSONHashMap? ->
                    if (event == null) {
                        null
                    } else {
                        when (new_controller) {
                            is TempoController -> OpusControlEventParser.tempo_event(event)
                            is VolumeController -> OpusControlEventParser.volume_event(event)
                            else -> throw UnknownControllerException(label)
                        }
                    }
                }
            }
            return new_controller
        }

        fun convert_v2_to_v3(input: JSONHashMap): JSONHashMap {
            val input_children = input.get_list("children")
            val events = JSONList()

            for (i in 0 until input_children.list.size) {
                val pair = input_children.get_hashmap(i)
                val generalized_tree = OpusTreeGeneralizer.convert_v1_to_v3(pair["second"] as JSONHashMap) { input_event: JSONHashMap ->
                    OpusControlEventParser.convert_v2_to_v3(input_event)
                }
                if (generalized_tree == null) {
                    continue
                }
                events.add(
                    JSONList(
                        mutableListOf(
                            pair["first"],
                            generalized_tree
                        )
                    )
                )
            }

            return JSONHashMap(
                hashMapOf(
                    "events" to events,
                    "type" to JSONString(
                        when (input.get_string("type")) {
                            "Tempo" -> "tempo"
                            "Volume" -> "volume"
                            else -> throw Exception() // Nothing else was implemented
                        }
                    ),
                    "initial" to OpusControlEventParser.convert_v2_to_v3(input.get_hashmap("initial_value")),
                )
            )
        }

        fun to_json(controller: ActiveController): JSONHashMap {
            val map = JSONHashMap()
            val event_list = JSONList()
            controller.events.forEachIndexed { i: Int, event_tree: OpusTree<OpusControlEvent>? ->
                if (event_tree == null) {
                    return@forEachIndexed
                }
                val generalized_tree = OpusTreeGeneralizer.to_json(event_tree) { event: OpusControlEvent ->
                    OpusControlEventParser.to_json(event)
                } ?: return@forEachIndexed

                event_list.add(
                    JSONList(
                        mutableListOf<JSONObject?>(
                            JSONInteger(i),
                            generalized_tree
                        )
                    )
                )
            }

            map["events"] = event_list
            map["initial"] = OpusControlEventParser.to_json(controller.initial_event)
            map["type"] = when (controller) {
                is TempoController -> "tempo"
                is VolumeController -> "volume"
                else -> throw Exception()
            }

            return map
        }
    }
}
