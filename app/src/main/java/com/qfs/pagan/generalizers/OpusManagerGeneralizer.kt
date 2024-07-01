package com.qfs.pagan.generalizers
import com.qfs.json.ParsedFloat
import com.qfs.json.ParsedHashMap
import com.qfs.json.ParsedInt
import com.qfs.json.ParsedList
import com.qfs.json.ParsedString
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
        fun <T: OpusLayerBase> generalize(opus_manager: T): ParsedHashMap {
            val output = ParsedHashMap()

            val channels: MutableList<ParsedHashMap> = mutableListOf()
            for (channel in opus_manager.channels) {
                channels.add(OpusChannelGeneralizer.generalize(channel))
            }
            output["size"] = opus_manager.beat_count
            output["tuning_map"] = ParsedList(MutableList(opus_manager.tuning_map.size) { i: Int ->
                ParsedList(
                    mutableListOf(
                        ParsedInt(opus_manager.tuning_map[i].first),
                        ParsedInt(opus_manager.tuning_map[i].second)
                    )
                )
            })

            output["transpose"] = ParsedInt(opus_manager.transpose)
            output["controllers"] = ActiveControlSetGeneralizer.to_json(opus_manager.controllers)

            if (opus_manager is OpusManager) {
                output["reflections"] = ParsedList(
                    MutableList(opus_manager.link_pools.size) { i: Int ->
                        val pool = opus_manager.link_pools[i].toList()
                        ParsedList(
                            MutableList(pool.size) { j: Int ->
                                val beat_key = pool[j]
                                ParsedList(
                                    mutableListOf(
                                        ParsedInt(beat_key.channel),
                                        ParsedInt(beat_key.line_offset),
                                        ParsedInt(beat_key.beat)
                                    )
                                )
                            }
                        )
                    }
                )
            }
            output["channels"] = ParsedList(
                MutableList(opus_manager.channels.size) { i: Int ->
                    OpusChannelGeneralizer.generalize(opus_manager.channels[i])
                }
            )
            output["percussion_channel"] = OpusChannelGeneralizer.generalize(opus_manager.percussion_channel)
            output["title"] = if (opus_manager.project_name == null) {
                null
            } else {
                ParsedString(opus_manager.project_name!!)
            }

            return ParsedHashMap(
                hashMapOf(
                    "d" to output,
                    "v" to ParsedInt(LATEST_VERSION)
                )
            )
        }

        fun interpret(input: ParsedHashMap): OpusLayerLinks {
            val inner_map = input["d"] as ParsedHashMap
            val opus_manager = OpusLayerLinks()
            opus_manager.set_project_name(inner_map.get_stringn("title"))
            opus_manager.transpose = inner_map.get_int("transpose", 0)

            opus_manager.channels.clear()

            opus_manager.set_beat_count(inner_map.get_int("size"))

            for (generalized_channel in inner_map.get_list("channels").list) {
                opus_manager.add_channel(
                    OpusChannelGeneralizer.interpret(
                        generalized_channel as ParsedHashMap,
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
            opus_manager.controllers = ActiveControlSetGeneralizer.from_json(inner_map.get_hashmap("controllers"))

            // use percussion channel to calculate beat count since it's guaranteed to be there
            opus_manager.set_beat_count(opus_manager.percussion_channel.lines[0].beats.size)

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

        fun convert_v0_to_v1(input: ParsedHashMap): ParsedHashMap {
            val old_channels = input.get_list("channels")
            val radix = input.get_int("radix")
            return ParsedHashMap(
                hashMapOf(
                    "name" to ParsedString(input.get_string("name")),
                    "transpose" to ParsedInt(input.get_int("transpose", 0)),
                    "tempo" to ParsedFloat(input.get_float("tempo")),
                    "tuning_map" to ParsedList(
                        MutableList(radix) { i: Int ->
                            ParsedHashMap(
                                hashMapOf(
                                    "first" to ParsedInt(i),
                                    "second" to ParsedInt(radix)
                                )
                            )
                        }
                    ),
                    "channels" to ParsedList(
                        MutableList(old_channels.list.size) { i: Int ->
                            OpusChannelGeneralizer.convert_v0_to_v1(old_channels.get_hashmap(i), radix)
                        }
                    ),
                    "reflections" to input.hash_map["reflections"]
                )
            )
        }

        fun convert_v1_to_v2(input: ParsedHashMap): ParsedHashMap {
            // Get Beat Count
            val line_tree = OpusTreeGeneralizer.from_v1_json(input.get_list("channels").get_hashmap(0).get_list("lines").get_hashmap(0)) { null }
            val beat_count = line_tree.size

            val channels = input.get_list("channels")

            return ParsedHashMap(
                hashMapOf(
                    "tuning_map" to input["tuning_map"],
                    "reflections" to input["reflections"],
                    "transpose" to input["transpose"],
                    "name" to input["name"],
                    "controllers" to ParsedList(
                        mutableListOf(
                            ParsedHashMap(
                                hashMapOf(
                                    "type" to ParsedString("Tempo"),
                                    "initial_value" to ParsedHashMap(
                                        hashMapOf(
                                            "type" to ParsedString("com.qfs.pagan.opusmanager.OpusTempoEvent"),
                                            "value" to ParsedFloat(input.get_float("tempo", 120F))
                                        )
                                    ),
                                    "children" to ParsedList()
                                )
                            )
                        )
                    ),
                    "channels" to ParsedList(
                        MutableList(channels.list.size) { i: Int ->
                            OpusChannelGeneralizer.convert_v1_to_v2(channels.get_hashmap(i))
                        }
                    )
                )
            )
        }

        fun detect_version(input: ParsedHashMap): Int {
            return when (val map_keys = input.hash_map.keys) {
                setOf("v", "d") -> {
                    input.get_int("v")
                }
                else -> {
                    if (!map_keys.contains("controllers")) {
                        if (!map_keys.contains("tuning_map")) {
                            0
                        } else {
                            1
                        }
                    } else {
                        2
                    }
                }
            }
        }

        fun convert_v2_to_v3(input_map: ParsedHashMap): ParsedHashMap {
            val input_channels = input_map.get_list("channels")
            val channels = ParsedList()
            for (i in 0 until input_channels.list.size - 1) {
                channels.add(
                    OpusChannelGeneralizer.convert_v2_to_v3(
                        input_channels.get_hashmap(i)
                    )
                )
            }
            val input_tuning_map = input_map.get_list("tuning_map")
            val beat_count = channels.get_hashmap(0).get_list("lines").get_hashmap(0).get_list("beats").list.size

            val input_reflections = input_map.get_list("reflections")

            return ParsedHashMap(
                hashMapOf(
                    "v" to ParsedInt(LATEST_VERSION),
                    "d" to ParsedHashMap(
                        hashMapOf(
                            "size" to ParsedInt(beat_count),
                            "title" to input_map["name"],
                            "tuning_map" to ParsedList(
                                MutableList(input_tuning_map.list.size) { i: Int ->
                                    val pair = input_tuning_map.get_hashmap(i)
                                    ParsedList(
                                        mutableListOf(
                                            ParsedInt(pair.get_int("first")),
                                            ParsedInt(pair.get_int("second"))
                                        )
                                    )
                                }
                            ),
                            "reflections" to ParsedList(
                                MutableList(input_reflections.list.size) { i: Int ->
                                    val pool = input_reflections.get_list(i)
                                    ParsedList(
                                        MutableList(pool.list.size) { j: Int ->
                                            val generalized_beat_key = pool.get_hashmap(j)
                                            ParsedList(
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
                            "controllers" to ActiveControlSetGeneralizer.convert_v2_to_v3(input_map["controllers"] as ParsedList, beat_count),
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