package com.qfs.pagan.jsoninterfaces
import com.qfs.json.JSONFloat
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONString
import com.qfs.pagan.opusmanager.ActiveControlSetJSONInterface
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusChannelJSONInterface
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusPercussionChannel

class OpusManagerJSONInterface {
    companion object {
        const val LATEST_VERSION = 3
        fun <T: OpusLayerBase> generalize(opus_manager: T): JSONHashMap {
            val output = JSONHashMap()

            val channels: MutableList<JSONHashMap> = mutableListOf()
            for (channel in opus_manager.channels) {
                channels.add(OpusChannelJSONInterface.generalize(channel))
            }
            output["size"] = opus_manager.beat_count
            output["tuning_map"] = JSONList(MutableList(opus_manager.tuning_map.size) { i: Int ->
                JSONList(
                    mutableListOf(
                        JSONInteger(opus_manager.tuning_map[i].first),
                        JSONInteger(opus_manager.tuning_map[i].second)
                    )
                )
            })

            output["transpose"] = JSONInteger(opus_manager.transpose)
            output["controllers"] = ActiveControlSetJSONInterface.to_json(opus_manager.controllers)

            output["channels"] = JSONList(
                MutableList(opus_manager.channels.size) { i: Int ->
                    OpusChannelJSONInterface.generalize(opus_manager.channels[i])
                }
            )
            output["percussion_channel"] = OpusChannelJSONInterface.generalize(opus_manager.percussion_channel)
            output["title"] = if (opus_manager.project_name == null) {
                null
            } else {
                JSONString(opus_manager.project_name!!)
            }

            return JSONHashMap(
                hashMapOf(
                    "d" to output,
                    "v" to JSONInteger(LATEST_VERSION)
                )
            )
        }

        fun interpret(input: JSONHashMap): OpusLayerBase {
            val inner_map = input["d"] as JSONHashMap
            val opus_manager = OpusLayerBase()
            opus_manager.set_project_name(inner_map.get_stringn("title"))
            opus_manager.transpose = inner_map.get_int("transpose", 0)

            opus_manager.channels.clear()

            opus_manager.set_beat_count(inner_map.get_int("size"))
            for (generalized_channel in inner_map.get_list("channels").list) {
                opus_manager.add_channel(
                    OpusChannelJSONInterface.interpret(
                        generalized_channel as JSONHashMap,
                        opus_manager.beat_count
                    ) as OpusChannel
                )
            }
            opus_manager.percussion_channel = OpusChannelJSONInterface.interpret(
                inner_map.get_hashmap("percussion_channel"),
                opus_manager.beat_count
            ) as OpusPercussionChannel


            val generalized_tuning_map = inner_map.get_list("tuning_map")
            opus_manager.tuning_map = Array(generalized_tuning_map.list.size) { i: Int ->
                val g_pair = generalized_tuning_map.get_list(i)
                Pair(
                    g_pair.get_int(0),
                    g_pair.get_int(1)
                )
            }
            opus_manager.controllers = ActiveControlSetJSONInterface.from_json(inner_map.get_hashmap("controllers"), opus_manager.beat_count)

            return opus_manager
        }

        fun convert_v0_to_v1(input: JSONHashMap): JSONHashMap {
            val old_channels = input.get_list("channels")
            val radix = input.get_int("radix")
            return JSONHashMap(
                hashMapOf(
                    "name" to JSONString(input.get_string("name")),
                    "transpose" to JSONInteger(input.get_int("transpose", 0)),
                    "tempo" to JSONFloat(input.get_float("tempo")),
                    "tuning_map" to JSONList(
                        MutableList(radix) { i: Int ->
                            JSONHashMap(
                                hashMapOf(
                                    "first" to JSONInteger(i),
                                    "second" to JSONInteger(radix)
                                )
                            )
                        }
                    ),
                    "channels" to JSONList(
                        MutableList(old_channels.list.size) { i: Int ->
                            OpusChannelJSONInterface.convert_v0_to_v1(old_channels.get_hashmap(i), radix)
                        }
                    )
                )
            )
        }

        fun convert_v1_to_v2(input: JSONHashMap): JSONHashMap {
            // Get Beat Count
            val line_tree = OpusTreeJSONInterface.from_v1_json(input.get_list("channels").get_hashmap(0).get_list("lines").get_hashmap(0)) { null }
            // radix may have existed in v1 AND v0, so check if its used instead of tuning_map
            var tuning_map = input.get_listn("tuning_map")
            if (tuning_map == null) {
                val radix = input.get_intn("radix") ?: 12
                tuning_map = JSONList(
                    MutableList(radix) {
                        JSONHashMap(
                            hashMapOf(
                                "first" to JSONInteger(it),
                                "second" to JSONInteger(radix)
                            )
                        )
                    }
                )
            }

            val channels = input.get_list("channels")

            return JSONHashMap(
                hashMapOf(
                    "tuning_map" to tuning_map,
                    "transpose" to input["transpose"],
                    "name" to input["name"],
                    "controllers" to JSONList(
                        mutableListOf(
                            JSONHashMap(
                                hashMapOf(
                                    "type" to JSONString("Tempo"),
                                    "initial_value" to JSONHashMap(
                                        hashMapOf(
                                            "type" to JSONString("com.qfs.pagan.opusmanager.OpusTempoEvent"),
                                            "value" to JSONFloat(input.get_float("tempo", 120F))
                                        )
                                    ),
                                    "children" to JSONList()
                                )
                            )
                        )
                    ),
                    "channels" to JSONList(
                        MutableList(channels.list.size) { i: Int ->
                            OpusChannelJSONInterface.convert_v1_to_v2(channels.get_hashmap(i))
                        }
                    )
                )
            )
        }

        fun detect_version(input: JSONHashMap): Int {
            return when (val map_keys = input.hash_map.keys) {
                setOf("v", "d") -> {
                    input.get_int("v")
                }
                else -> {
                    // There was some time between v0 and 1 where the 'tuning_map' didn't exist, so
                    // need to check by lines
                    if (!map_keys.contains("controllers")) {
                        if (map_keys.contains("radix")) {
                            val channel = input.get_list("channels").get_hashmap(0)
                            val lines = channel.get_list("lines")
                            if (lines.list[0] is JSONString) {
                                0
                            } else {
                                1
                            }
                        } else {
                            1
                        }
                    } else {
                        2
                    }
                }
            }
        }

        fun convert_v2_to_v3(input_map: JSONHashMap): JSONHashMap {
            val input_channels = input_map.get_list("channels")
            val channels = JSONList()
            for (i in 0 until input_channels.list.size - 1) {
                channels.add(
                    OpusChannelJSONInterface.convert_v2_to_v3(
                        input_channels.get_hashmap(i)
                    )
                )
            }
            val input_tuning_map = input_map.get_list("tuning_map")
            val beat_count = input_channels.get_hashmap(0).get_list("lines").get_hashmap(0).get_list("children").list.size


            return JSONHashMap(
                hashMapOf(
                    "v" to JSONInteger(LATEST_VERSION),
                    "d" to JSONHashMap(
                        hashMapOf(
                            "size" to JSONInteger(beat_count),
                            "title" to input_map["name"],
                            "tuning_map" to JSONList(
                                MutableList(input_tuning_map.list.size) { i: Int ->
                                    val pair = input_tuning_map.get_hashmap(i)
                                    JSONList(
                                        mutableListOf(
                                            JSONInteger(pair.get_int("first")),
                                            JSONInteger(pair.get_int("second"))
                                        )
                                    )
                                }
                            ),
                            "transpose" to input_map["transpose"],
                            "controllers" to ActiveControlSetJSONInterface.convert_v2_to_v3(input_map["controllers"] as JSONList, beat_count),
                            "channels" to channels,
                            "percussion_channel" to OpusChannelJSONInterface.convert_v2_to_v3(
                                input_channels.get_hashmap(input_channels.list.size - 1)
                            )
                        )
                    )
                )
            )
        }
    }
}