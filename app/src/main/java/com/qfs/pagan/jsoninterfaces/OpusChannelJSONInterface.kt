package com.qfs.pagan.structure.opusmanager

import com.qfs.json.JSONBoolean
import com.qfs.json.JSONFloat
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONString
import com.qfs.pagan.CH_ADD
import com.qfs.pagan.CH_CLOSE
import com.qfs.pagan.CH_DOWN
import com.qfs.pagan.CH_HOLD
import com.qfs.pagan.CH_NEXT
import com.qfs.pagan.CH_OPEN
import com.qfs.pagan.CH_SUBTRACT
import com.qfs.pagan.CH_UP
import com.qfs.pagan.REL_CHARS
import com.qfs.pagan.SPECIAL_CHARS
import com.qfs.pagan.char_to_int
import com.qfs.pagan.jsoninterfaces.ExpectedCharacterException
import com.qfs.pagan.jsoninterfaces.OpusTreeJSONInterface
import com.qfs.pagan.jsoninterfaces.UnknownChannelTypeException
import com.qfs.pagan.structure.opusmanager.base.OpusChannel
import com.qfs.pagan.structure.opusmanager.base.OpusChannelAbstract
import com.qfs.pagan.structure.opusmanager.base.OpusPercussionChannel
import com.qfs.pagan.structure.rationaltree.ReducibleTree

class OpusChannelJSONInterface {
    companion object {
        fun generalize(channel: OpusChannelAbstract<*, *>): JSONHashMap {
            val channel_map = JSONHashMap()
            val lines = JSONList(channel.size) { i: Int ->
                OpusLineJSONInterface.to_json(channel.lines[i])
            }

            channel_map["type"] = when (channel) {
                is OpusPercussionChannel -> "kit"
                is OpusChannel -> "std"
                else -> "???"
            }
            channel_map["lines"] = lines
            channel_map["midi_channel"] = channel.get_midi_channel()
            channel_map["midi_bank"] = channel.get_midi_bank()
            channel_map["midi_program"] = channel.midi_program
            channel_map["controllers"] = ActiveControlSetJSONInterface.to_json(channel.controllers)
            channel_map["muted"] = channel.muted

            return channel_map
        }

        private fun _interpret_percussion(input_map: JSONHashMap, beat_count: Int): OpusPercussionChannel {
            val channel = OpusPercussionChannel(-1)
            val input_lines = input_map.get_list("lines")
            for (line in input_lines) {
                channel.lines.add(OpusLineJSONInterface.percussion_line(line as JSONHashMap, beat_count))
            }

            return channel
        }

        private fun _interpret_std(input_map: JSONHashMap, beat_count: Int): OpusChannel {
            val channel = OpusChannel(-1)
            channel.midi_bank = input_map.get_int("midi_bank")

            val input_lines = input_map.get_list("lines")
            for (line in input_lines) {
                channel.lines.add(
                    OpusLineJSONInterface.opus_line(
                        line as JSONHashMap,
                        beat_count
                    )
                )
            }

            return channel
        }

        fun interpret(input_map: JSONHashMap, beat_count: Int): OpusChannelAbstract<*, *> {
            val channel_type = input_map.get_stringn("type")
            val channel = when (channel_type) {
                "std" -> this._interpret_std(input_map, beat_count)
                "kit" -> this._interpret_percussion(input_map, beat_count)
                else -> throw UnknownChannelTypeException(channel_type)
            }

            channel.size = channel.lines.size
            channel.set_beat_count(beat_count)
            channel.midi_channel = input_map.get_int("midi_channel")
            channel.midi_program = input_map.get_int("midi_program")
            channel.controllers = ActiveControlSetJSONInterface.from_json(input_map.get_hashmap("controllers"), beat_count)
            channel.muted = input_map.get_boolean("muted", false)

            return channel
        }

        fun interpret_v0_string(input_string: String, radix: Int, channel: Int): ReducibleTree<JSONHashMap> {
            val repstring = input_string
                .trim()
                .replace(" ", "")
                .replace("\n", "")
                .replace("\t", "")
                .replace("_", "")

            val output = ReducibleTree<JSONHashMap>()

            val tree_stack = mutableListOf(output)
            var register: Int? = null
            val opened_indices: MutableList<Int> = mutableListOf()
            var relative_flag: Char? = null
            for (i in repstring.indices) {
                val character = repstring[i]
                if (character == CH_CLOSE) {
                    // Remove completed tree from stack
                    tree_stack.removeAt(tree_stack.size - 1)
                    opened_indices.removeAt(opened_indices.size - 1)
                }

                if (character == CH_NEXT) {
                    // Resize Active Tree
                    if (tree_stack.last().is_leaf()) {
                        tree_stack.last().set_size(2)
                    } else {
                        tree_stack.last().set_size(tree_stack.last().size + 1, true)
                    }
                }

                if (character == CH_OPEN) {
                    val last = tree_stack.last()
                    if (last.is_leaf()) {
                        last.set_size(1)
                    }

                    val new_tree = last[last.size - 1]
                    if (! new_tree.is_leaf() && ! new_tree.has_event()) {
                        throw ExpectedCharacterException(CH_NEXT, i, repstring)
                    }
                    tree_stack.add(new_tree)
                    opened_indices.add(i)
                } else if (relative_flag != null) {
                    var odd_note = 0
                    when (relative_flag) {
                        CH_SUBTRACT -> {
                            odd_note -= char_to_int(character, radix)
                        }
                        CH_ADD -> {
                            odd_note += char_to_int(character, radix)
                        }
                        CH_UP -> {
                            odd_note += char_to_int(character, radix) * radix
                        }
                        CH_DOWN -> {
                            odd_note -= char_to_int(character, radix) * radix
                        }
                    }

                    if (tree_stack.last().is_leaf()) {
                        tree_stack.last().set_size(1)
                    }
                    val leaf = tree_stack.last()[tree_stack.last().size - 1]
                    if (relative_flag != CH_HOLD) {
                        leaf.set_event(
                            JSONHashMap(
                                "note" to JSONInteger(odd_note),
                                "relative" to JSONBoolean(true)
                            )
                        )
                    }
                    relative_flag = null
                } else if (REL_CHARS.contains(character)) {
                    relative_flag = character
                } else if (!SPECIAL_CHARS.contains(character)) {
                    register = if (register == null) {
                        char_to_int(character, radix)
                    } else {

                        val odd_note = (register * radix) + char_to_int(character, radix)
                        if (tree_stack.last().is_leaf()) {
                            tree_stack.last().set_size(1)
                        }
                        val leaf = tree_stack.last()[tree_stack.last().size - 1]
                        leaf.set_event(
                            JSONHashMap(
                                "note" to JSONInteger(odd_note),
                                "relative" to JSONBoolean(false)
                            )
                        )
                        null
                    }
                }
            }

            if (tree_stack.size > 1) {
                throw ExpectedCharacterException(CH_CLOSE, repstring.length, repstring)
            }

            return output
        }

        fun convert_v0_to_v1(input_map: JSONHashMap, radix: Int): JSONHashMap {
            val lines = input_map.get_list("lines")

            val new_lines = JSONList(lines.size) { i: Int ->
                val beat_splits = lines.get_string(i).split("|")
                val working_tree = ReducibleTree<JSONHashMap>()
                working_tree.set_size(beat_splits.size)

                beat_splits.forEachIndexed { j: Int, beat_string: String ->
                    val beat_tree = this.interpret_v0_string(beat_string, radix, 0)
                    beat_tree.clear_singles()
                    working_tree[j] = beat_tree
                }

                OpusTreeJSONInterface.to_v1_json(working_tree) { it }
            }

            return JSONHashMap(
                "lines" to new_lines,
                "midi_channel" to input_map["midi_channel"],
                "midi_bank" to input_map["midi_bank"],
                "midi_program" to input_map["midi_program"],
                "line_volumes" to input_map["line_volumes"]
            )
        }

        fun convert_v1_to_v2(input_map: JSONHashMap): JSONHashMap {
            // Get Beat Count
            val line_volumes = input_map.get_list("line_volumes")
            val midi_channel = input_map.get_int("midi_channel")
            val lines = input_map.get_list("lines")

            val static_values = JSONList(lines.size) { i: Int ->
                if (midi_channel == 9) {
                    var static_value: Int? = null
                    val stack = mutableListOf(lines.get_hashmap(i))
                    while (stack.isNotEmpty()) {
                        var working_tree = stack.removeAt(0)
                        val event = working_tree.get_hashmapn("event")
                        if (event != null) {
                            val note = event.get_intn("note")
                            if (note != null) {
                                static_value = note
                                break
                            }
                        }

                        val children = working_tree.get_listn("children") ?: JSONList()
                        for (child in children) {
                            if (child != null) {
                                stack.add(child as JSONHashMap)
                            }
                        }
                    }

                    if (static_value != null) {
                        JSONInteger(static_value)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

            return JSONHashMap(
                "midi_channel" to input_map["midi_channel"],
                "midi_bank" to input_map["midi_bank"],
                "midi_program" to input_map["midi_program"],
                "line_static_values" to static_values,
                "line_controllers" to JSONList(line_volumes.size) { i: Int ->
                    JSONList(
                        JSONHashMap(
                            "type" to JSONString("Volume"),
                            "initial_value" to JSONHashMap(
                                "type" to JSONString("com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent"),
                                "value" to JSONInteger(line_volumes.get_int(i))
                            ),
                            "children" to JSONList()
                        )
                    )
                },
                "channel_controllers" to JSONList(
                    JSONHashMap(
                        "type" to JSONString("Tempo"),
                        "initial_value" to JSONHashMap(
                            "type" to JSONString("com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent"),
                            "value" to JSONFloat(input_map.get_float("tempo", 120F))
                        ),
                        "children" to JSONList()
                    )
                ),
                "lines" to input_map.get_list("lines"),
            )
        }

        fun convert_v2_to_v3(input_map: JSONHashMap): JSONHashMap {
            val lines = input_map.get_list("lines")
            val beat_count = lines.get_hashmap(0).get_list("children").size
            val midi_channel = input_map.get_int("midi_channel")
            val new_controllers = if (input_map["channel_controllers"] != null) {
                ActiveControlSetJSONInterface.convert_v2_to_v3(input_map["channel_controllers"] as JSONList, beat_count)
            } else {
                null
            }

            return JSONHashMap(
                "midi_channel" to input_map["midi_channel"],
                "midi_bank" to input_map["midi_bank"],
                "midi_program" to input_map["midi_program"],
                "controllers" to new_controllers,
                "lines" to JSONList(lines.size) { i: Int ->
                    val child_list = lines.get_hashmap(i).get_list("children")
                    val line_controllers = input_map.get_list("line_controllers").get_list(i)
                    val beats = JSONList()
                    for (j in 0 until child_list.size) {
                        val generalized_beat = OpusTreeJSONInterface.convert_v1_to_v3(child_list.get_hashmapn(j)) { event_map: JSONHashMap ->
                            if (midi_channel == 9) {
                                InstrumentEventJSONInterface.convert_v1_to_v3_percussion(event_map)
                            } else {
                                InstrumentEventJSONInterface.convert_v1_to_v3_tuned(event_map)
                            }
                        } ?: continue
                        beats.add(
                            JSONList(JSONInteger(j), generalized_beat)
                        )
                    }

                    val output_line = JSONHashMap(
                        "controllers" to ActiveControlSetJSONInterface.convert_v2_to_v3(line_controllers, beat_count),
                        "beats" to beats
                    )

                    if (midi_channel == 9) {
                        output_line["instrument"] = input_map.get_list("line_static_values").get_int(i)
                    }

                    output_line
                }
            )
        }

        fun convert_v3_to_v4(input_map: JSONHashMap): JSONHashMap {
            val output_map = input_map.copy()
            output_map["type"] = if (input_map.get_intn("midi_channel") == 9) {
                "kit"
            } else {
                "std"
            }
            return output_map
        }
    }
}
