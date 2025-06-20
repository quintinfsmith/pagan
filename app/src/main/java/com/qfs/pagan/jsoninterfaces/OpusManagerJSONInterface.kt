package com.qfs.pagan.jsoninterfaces
import com.qfs.json.JSONFloat
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONString
import com.qfs.pagan.opusmanager.ActiveControlSetJSONInterface
import com.qfs.pagan.opusmanager.OpusChannelJSONInterface

class OpusManagerJSONInterface {
    companion object {
        const val LATEST_VERSION = 4

        fun convert_v0_to_v1(input: JSONHashMap): JSONHashMap {
            val old_channels = input.get_list("channels")
            val radix = input.get_int("radix")
            return JSONHashMap(
                "name" to JSONString(input.get_string("name")),
                "transpose" to JSONInteger(input.get_int("transpose", 0)),
                "tempo" to JSONFloat(input.get_float("tempo")),
                "tuning_map" to JSONList(radix) { i: Int ->
                    JSONHashMap(
                        "first" to JSONInteger(i),
                        "second" to JSONInteger(radix)
                    )
                },
                "channels" to JSONList(old_channels.size) { i: Int ->
                    OpusChannelJSONInterface.convert_v0_to_v1(old_channels.get_hashmap(i), radix)
                }
            )
        }

        fun convert_v1_to_v2(input: JSONHashMap): JSONHashMap {
            // Get Beat Count
            val line_tree = OpusTreeJSONInterface.from_v1_json(input.get_list("channels").get_hashmap(0).get_list("lines").get_hashmap(0)) { null }
            // radix may have existed in v1 AND v0, so check if its used instead of tuning_map
            var tuning_map = input.get_listn("tuning_map")
            if (tuning_map == null) {
                val radix = input.get_intn("radix") ?: 12
                tuning_map = JSONList(radix) {
                    JSONHashMap(
                        "first" to JSONInteger(it),
                        "second" to JSONInteger(radix)
                    )
                }
            }

            val channels = input.get_list("channels")

            return JSONHashMap(
                "tuning_map" to tuning_map,
                "transpose" to input["transpose"],
                "name" to input["name"],
                "controllers" to JSONList(
                    JSONHashMap(
                        "type" to JSONString("Tempo"),
                        "initial_value" to JSONHashMap(
                            "type" to JSONString("com.qfs.pagan.opusmanager.OpusTempoEvent"),
                            "value" to JSONFloat(input.get_float("tempo", 120F))
                        ),
                        "children" to JSONList()
                    )
                ),
                "channels" to JSONList(channels.size) { i: Int ->
                    OpusChannelJSONInterface.convert_v1_to_v2(channels.get_hashmap(i))
                }
            )
        }

        fun detect_version(input: JSONHashMap): Int {
            return when (input.keys) {
                setOf("v", "d") -> {
                    input.get_int("v")
                }
                else -> {
                    // There was some time between v0 and 1 where the 'tuning_map' didn't exist, so
                    // need to check by lines
                    if (!input.keys.contains("controllers")) {
                        if (input.keys.contains("radix")) {
                            val channel = input.get_list("channels").get_hashmap(0)
                            val lines = channel.get_list("lines")
                            if (lines[0] is JSONString) {
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

        fun convert_v3_to_v4(input_map: JSONHashMap): JSONHashMap {
            val output_map = input_map.copy()
            val channel_maps = output_map.get_hashmap("d").get_list("channels")
            for (i in 0 until channel_maps.size) {
                channel_maps[i] = OpusChannelJSONInterface.convert_v3_to_v4(
                    channel_maps.get_hashmap(i)
                )
            }

            channel_maps.add(
                OpusChannelJSONInterface.convert_v3_to_v4(
                    output_map.get_hashmap("d").get_hashmap("percussion_channel")
                )
            )

            return output_map
        }

        fun convert_v2_to_v3(input_map: JSONHashMap): JSONHashMap {
            val input_channels = input_map.get_list("channels")
            val channels = JSONList()
            for (i in 0 until input_channels.size - 1) {
                channels.add(
                    OpusChannelJSONInterface.convert_v2_to_v3(
                        input_channels.get_hashmap(i)
                    )
                )
            }
            val input_tuning_map = input_map.get_list("tuning_map")
            val beat_count = input_channels.get_hashmap(0).get_list("lines").get_hashmap(0).get_list("children").size


            return JSONHashMap(
                "v" to JSONInteger(LATEST_VERSION),
                "d" to JSONHashMap(
                    "size" to JSONInteger(beat_count),
                    "title" to input_map["name"],
                    "tuning_map" to JSONList(input_tuning_map.size) { i: Int ->
                        val pair = input_tuning_map.get_hashmap(i)
                        JSONList(
                            JSONInteger(pair.get_int("first")),
                            JSONInteger(pair.get_int("second"))
                        )
                    },
                    "transpose" to input_map["transpose"],
                    "controllers" to ActiveControlSetJSONInterface.convert_v2_to_v3(input_map["controllers"] as JSONList, beat_count),
                    "channels" to channels,
                    "percussion_channel" to OpusChannelJSONInterface.convert_v2_to_v3(
                        input_channels.get_hashmap(input_channels.size - 1)
                    )
                )
            )
        }
    }
}