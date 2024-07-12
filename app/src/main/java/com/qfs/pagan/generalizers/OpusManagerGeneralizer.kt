package com.qfs.pagan.generalizers
import com.qfs.json.JSONFloat
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONString
import com.qfs.pagan.opusmanager.ActiveControlSetGeneralizer
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusChannelGeneralizer
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusLayerLinks
import com.qfs.pagan.opusmanager.OpusPercussionChannel
import com.qfs.pagan.opusmanager.OpusLayerLinks as OpusManager

class OpusManagerGeneralizer {
    companion object {
        const val LATEST_VERSION = 3
        fun <T: OpusLayerBase> generalize(opus_manager: T): JSONHashMap {
            val output = JSONHashMap()

            val channels: MutableList<JSONHashMap> = mutableListOf()
            for (channel in opus_manager.channels) {
                channels.add(OpusChannelGeneralizer.generalize(channel))
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
            output["controllers"] = ActiveControlSetGeneralizer.to_json(opus_manager.controllers)

            if (opus_manager is OpusManager) {
                output["reflections"] = JSONList(
                    MutableList(opus_manager.link_pools.size) { i: Int ->
                        val pool = opus_manager.link_pools[i].toList()
                        JSONList(
                            MutableList(pool.size) { j: Int ->
                                val beat_key = pool[j]
                                JSONList(
                                    mutableListOf(
                                        JSONInteger(beat_key.channel),
                                        JSONInteger(beat_key.line_offset),
                                        JSONInteger(beat_key.beat)
                                    )
                                )
                            }
                        )
                    }
                )
            }
            output["channels"] = JSONList(
                MutableList(opus_manager.channels.size) { i: Int ->
                    OpusChannelGeneralizer.generalize(opus_manager.channels[i])
                }
            )
            output["percussion_channel"] = OpusChannelGeneralizer.generalize(opus_manager.percussion_channel)
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

        fun interpret(input: JSONHashMap): OpusLayerLinks {
            val inner_map = input["d"] as JSONHashMap
            val opus_manager = OpusLayerLinks()
            opus_manager.set_project_name(inner_map.get_stringn("title"))
            opus_manager.transpose = inner_map.get_int("transpose", 0)

            opus_manager.channels.clear()

            opus_manager.set_beat_count(inner_map.get_int("size"))
            for (generalized_channel in inner_map.get_list("channels").list) {
                opus_manager.add_channel(
                    OpusChannelGeneralizer.interpret(
                        generalized_channel as JSONHashMap,
                        opus_manager.beat_count
                    ) as OpusChannel
                )
            }
            opus_manager.percussion_channel = OpusChannelGeneralizer.interpret(
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
            opus_manager.controllers = ActiveControlSetGeneralizer.from_json(inner_map.get_hashmap("controllers"), opus_manager.beat_count)

            val generalized_reflections = inner_map.get_list("reflections")
            for (i in 0 until generalized_reflections.list.size) {
                val pool = generalized_reflections.get_list(i)
                opus_manager.link_pools.add(
                    MutableList<BeatKey>(pool.list.size) { j: Int ->
                        val generalized_beat_key = pool.get_list(j)
                        BeatKey(
                            generalized_beat_key.get_int(0),
                            generalized_beat_key.get_int(1),
                            generalized_beat_key.get_int(2)
                        )
                    }.toMutableSet()
                )
            }

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
                            OpusChannelGeneralizer.convert_v0_to_v1(old_channels.get_hashmap(i), radix)
                        }
                    ),
                    "reflections" to input.hash_map["reflections"]
                )
            )
        }

        fun convert_v1_to_v2(input: JSONHashMap): JSONHashMap {
            // Get Beat Count
            val line_tree = OpusTreeGeneralizer.from_v1_json(input.get_list("channels").get_hashmap(0).get_list("lines").get_hashmap(0)) { null }
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
                    "reflections" to input["reflections"],
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
                            OpusChannelGeneralizer.convert_v1_to_v2(channels.get_hashmap(i))
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
                    OpusChannelGeneralizer.convert_v2_to_v3(
                        input_channels.get_hashmap(i)
                    )
                )
            }
            val input_tuning_map = input_map.get_list("tuning_map")
            val beat_count = input_channels.get_hashmap(0).get_list("lines").get_hashmap(0).get_list("children").list.size

            val input_reflections = input_map.get_list("reflections")

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
                            "reflections" to JSONList(
                                MutableList(input_reflections.list.size) { i: Int ->
                                    val pool = input_reflections.get_list(i)
                                    JSONList(
                                        MutableList(pool.list.size) { j: Int ->
                                            val generalized_beat_key = pool.get_hashmap(j)
                                            JSONList(
                                                mutableListOf(
                                                    generalized_beat_key["channel"],
                                                    generalized_beat_key["line_offset"],
                                                    generalized_beat_key["beat"]
                                                )
                                            )
                                        }
                                    )
                                }
                            ),
                            "transpose" to input_map["transpose"],
                            "controllers" to ActiveControlSetGeneralizer.convert_v2_to_v3(input_map["controllers"] as JSONList, beat_count),
                            "channels" to channels,
                            "percussion_channel" to OpusChannelGeneralizer.convert_v2_to_v3(
                                input_channels.get_hashmap(input_channels.list.size - 1)
                            )
                        )
                    )
                )
            )
        }
    }
}