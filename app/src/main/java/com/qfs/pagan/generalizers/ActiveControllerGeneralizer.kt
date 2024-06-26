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
            println(obj.to_string())
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
                new_controller.events[index] = OpusTreeGeneralizer.from_json(pair.get_hashmap(1)) { event: ParsedHashMap? ->
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
            println("${input.to_string()}")
            val input_children = input.get_list("children")
            return ParsedHashMap(
                hashMapOf(
                    "events" to ParsedList(
                        MutableList(input_children.list.size) { i: Int ->
                            val pair = input_children.get_hashmap(i)
                            ParsedList(
                                mutableListOf(
                                    pair["first"],
                                    OpusTreeGeneralizer.convert_v1_to_v3(pair["second"] as ParsedHashMap) { input_event: ParsedHashMap ->
                                        OpusControlEventParser.convert_v2_to_v3(input_event)
                                    }
                                )
                            )
                        }
                    ),
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
                event_list.add(
                    ParsedList(
                        mutableListOf<ParsedObject?>(
                            ParsedInt(i),
                            OpusTreeGeneralizer.to_json(event_tree) { event: OpusControlEvent ->
                                OpusControlEventParser.to_json(event)
                            }
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
