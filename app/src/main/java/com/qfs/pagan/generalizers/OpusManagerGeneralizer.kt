package com.qfs.pagan.generalizers
import com.qfs.json.ParsedHashMap
import com.qfs.json.ParsedInt
import com.qfs.json.ParsedList
import com.qfs.pagan.opusmanager.ActiveControlSetGeneralizer
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusChannelGeneralizer
import com.qfs.pagan.opusmanager.OpusLayerLinks
import com.qfs.pagan.opusmanager.OpusLayerBase as OpusManager

class OpusManagerGeneralizer {
    companion object {
        fun generalize(opus_manager: OpusManager): ParsedHashMap {
            val output = ParsedHashMap()

            val channels: MutableList<ParsedHashMap> = mutableListOf()
            for (channel in opus_manager.channels) {
                channels.add(OpusChannelGeneralizer.generalize(channel))
            }

            val project_name = opus_manager.project_name
            output["project_name"] = project_name

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

            if (opus_manager is OpusLayerLinks) {
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
            } else {
                output["reflection"] = ParsedList()
            }

            return output
        }

        fun interpret(input: ParsedHashMap): OpusManager {
            val opus_manager = OpusManager()
            opus_manager.set_project_name(input.get_string("project_name"))
            opus_manager.transpose = input.get_int("transpose")

            opus_manager.channels.clear()
            for (generalized_channel in input.get_list("channels").list) {
                val channel = OpusChannelGeneralizer.interpret(generalized_channel as ParsedHashMap)
                opus_manager.channels.add(channel)
            }
            val generalized_tuning_map = input.get_list("tuning_map")
            opus_manager.tuning_map = Array<Pair<Int, Int>>(generalized_tuning_map.list.size) { i: Int ->
                val g_pair = generalized_tuning_map.get_list(i)
                Pair(
                    g_pair.get_int(0),
                    g_pair.get_int(1)
                )
            }
            opus_manager.controllers = ActiveControlSetGeneralizer.from_json(input.get_hashmap("controllers"))

            val generalized_reflections = input.get_list("reflections")
            for (i in 0 until generalized_reflections.list.size) {
                val pool = generalized_reflections.get_list(i)
                (opus_manager as OpusLayerLinks).link_pools.add(
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
    }
}