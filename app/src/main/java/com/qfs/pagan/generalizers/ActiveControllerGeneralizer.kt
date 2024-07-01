package com.qfs.pagan.opusmanager

import com.qfs.json.ParsedHashMap
import com.qfs.json.ParsedInt
import com.qfs.json.ParsedList
import com.qfs.json.ParsedObject
import com.qfs.json.ParsedString
import com.qfs.pagan.generalizers.OpusTreeGeneralizer
import com.qfs.pagan.structure.OpusTree

class ActiveControllerGeneralizer {
    class UnknownControllerException(label: String): Exception("Unknown Controller: \"$label\"")
    companion object {
        fun from_json(obj: ParsedHashMap, size: Int): ActiveController {
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
                val index = (pair as ParsedList).get_int(0)
                val value = pair.get_hashmapn(1) ?: continue

                new_controller.events[index] = OpusTreeGeneralizer.from_json(value) { event: ParsedHashMap? ->
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

        fun convert_v2_to_v3(input: ParsedHashMap): ParsedHashMap {
            val input_children = input.get_list("children")
            val events = ParsedList()

            for (i in 0 until input_children.list.size) {
                val pair = input_children.get_hashmap(i)
                val generalized_tree = OpusTreeGeneralizer.convert_v1_to_v3(pair["second"] as ParsedHashMap) { input_event: ParsedHashMap ->
                    OpusControlEventParser.convert_v2_to_v3(input_event)
                }
                if (generalized_tree == null) {
                    continue
                }
                events.add(
                    ParsedList(
                        mutableListOf(
                            pair["first"],
                            generalized_tree
                        )
                    )
                )
            }

            return ParsedHashMap(
                hashMapOf(
                    "events" to events,
                    "type" to ParsedString(
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

        fun to_json(controller: ActiveController): ParsedHashMap {
            val map = ParsedHashMap()
            val event_list = ParsedList()
            controller.events.forEachIndexed { i: Int, event_tree: OpusTree<OpusControlEvent>? ->
                if (event_tree == null) {
                    return@forEachIndexed
                }
                val generalized_tree = OpusTreeGeneralizer.to_json(event_tree) { event: OpusControlEvent ->
                    OpusControlEventParser.to_json(event)
                } ?: return@forEachIndexed

                event_list.add(
                    ParsedList(
                        mutableListOf<ParsedObject?>(
                            ParsedInt(i),
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
