package com.qfs.pagan.opusmanager

import com.qfs.json.ParsedBoolean
import com.qfs.json.ParsedFloat
import com.qfs.json.ParsedHashMap
import com.qfs.json.ParsedInt
import com.qfs.json.ParsedList
import com.qfs.json.ParsedString
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
import com.qfs.pagan.generalizers.OpusTreeGeneralizer
import com.qfs.pagan.structure.OpusTree

class OpusChannelGeneralizer {
    companion object {
        fun generalize(channel: OpusChannelAbstract<*,*>): ParsedHashMap {
            val channel_map = ParsedHashMap()
            val lines = ParsedList(
                MutableList(channel.size) { i: Int ->
                    OpusLineGeneralizer.to_json(channel.lines[i])
                }
            )
            channel_map["lines"] = lines
            channel_map["midi_channel"] = channel.get_midi_channel()
            channel_map["midi_bank"] = channel.get_midi_bank()
            channel_map["midi_program"] = channel.midi_program

            return channel_map
        }

        private fun _interpret_percussion(input_map: ParsedHashMap, beat_count: Int): OpusPercussionChannel {
            val channel = OpusPercussionChannel()
            val input_lines = input_map.get_list("lines")
            for (line in input_lines.list) {
                channel.lines.add(OpusLineGeneralizer.percussion_line(line as ParsedHashMap, beat_count))
            }

            return channel
        }

        private fun _interpret_std(input_map: ParsedHashMap, beat_count: Int): OpusChannel {
            val channel = OpusChannel(-1)
            val midi_channel = input_map.get_int("midi_channel")
            channel.midi_channel = midi_channel
            channel.midi_bank = input_map.get_int("midi_bank")

            val input_lines = input_map.get_list("lines")
            for (line in input_lines.list) {
                channel.lines.add(OpusLineGeneralizer.opus_line(line as ParsedHashMap, beat_count))
            }

            return channel
        }

        fun interpret(input_map: ParsedHashMap, beat_count: Int): OpusChannelAbstract<*,*> {
            val midi_channel = input_map.get_int("midi_channel")
            val channel = if (midi_channel == 9) {
                _interpret_percussion(input_map, beat_count)
            } else {
                _interpret_std(input_map, beat_count)
            }

            channel.size = channel.lines.size
            channel.midi_program = input_map.get_int("midi_program")

            return channel
        }

        fun interpret_v0_string(input_string: String, radix: Int, channel: Int): OpusTree<ParsedHashMap> {
            val repstring = input_string
                .trim()
                .replace(" ", "")
                .replace("\n", "")
                .replace("\t", "")
                .replace("_", "")

            val output = OpusTree<ParsedHashMap>()

            val tree_stack = mutableListOf(output)
            var register: Int? = null
            val opened_indeces: MutableList<Int> = mutableListOf()
            var relative_flag: Char? = null
            for (i in repstring.indices) {
                val character = repstring[i]
                if (character == CH_CLOSE) {
                    // Remove completed tree from stack
                    tree_stack.removeLast()
                    opened_indeces.removeLast()
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
                    if (! new_tree.is_leaf() && ! new_tree.is_event()) {
                        throw Exception("MISSING COMMA")
                    }
                    tree_stack.add(new_tree)
                    opened_indeces.add(i)
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
                            ParsedHashMap(
                                hashMapOf(
                                    "note" to ParsedInt(odd_note),
                                    "relative" to ParsedBoolean(true)
                                )
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
                            ParsedHashMap(
                                hashMapOf(
                                    "note" to ParsedInt(odd_note),
                                    "relative" to ParsedBoolean(false)
                                )
                            )
                        )
                        null
                    }
                }
            }

            if (tree_stack.size > 1) {
                throw Exception("Unclosed Opus Tree Error")
            }

            return output
        }

        fun convert_v0_to_v1(input_map: ParsedHashMap, radix: Int): ParsedHashMap {
            val lines = input_map.get_list("lines")

            val new_lines = ParsedList(
                MutableList(lines.list.size) { i: Int ->
                    val beat_splits = lines.get_string(i).split("|")
                    val working_tree = OpusTree<ParsedHashMap>()
                    working_tree.set_size(beat_splits.size)

                    beat_splits.forEachIndexed { j: Int, beat_string: String ->
                        val beat_tree = interpret_v0_string(beat_string, radix, 0)
                        beat_tree.clear_singles()
                        working_tree[j] = beat_tree
                    }

                    OpusTreeGeneralizer.to_v1_json(working_tree) { it }
                }
            )

            return ParsedHashMap(
                hashMapOf(
                    "lines" to new_lines,
                    "midi_channel" to input_map["midi_channel"],
                    "midi_bank" to input_map["midi_bank"],
                    "midi_program" to input_map["midi_program"],
                    "line_volumes" to input_map["line_volumes"]
                )
            )
        }

        fun convert_v1_to_v2(input_map: ParsedHashMap): ParsedHashMap {
            // Get Beat Count
            val line_volumes = input_map.get_list("line_volumes")
            val midi_channel = input_map.get_int("midi_channel")
            val lines = input_map.get_list("lines")

            val static_values = ParsedList(
                MutableList(lines.list.size) { i: Int ->
                    if (midi_channel == 9) {
                        var static_value: Int? = null
                        val stack = mutableListOf(lines.get_hashmap(i))
                        while (stack.isNotEmpty()) {
                            var working_tree = stack.removeFirst()
                            val event = working_tree.get_hashmapn("event")
                            if (event != null) {
                                val note = event.get_intn("note")
                                if (note != null) {
                                    static_value = note
                                    break
                                }
                            }

                            val children = working_tree.get_listn("children") ?: ParsedList()
                            for (child in children.list) {
                                if (child != null) {
                                    stack.add(child as ParsedHashMap)
                                }
                            }
                        }

                        if (static_value != null) {
                            ParsedInt(static_value)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            )

            return ParsedHashMap(
                hashMapOf(
                    "midi_channel" to input_map["midi_channel"],
                    "midi_bank" to input_map["midi_bank"],
                    "midi_program" to input_map["midi_program"],
                    "line_static_values" to static_values,
                    "line_controllers" to ParsedList(
                        MutableList(line_volumes.list.size) { i: Int ->
                            ParsedList(
                                MutableList(1) {
                                    ParsedHashMap(
                                        hashMapOf(
                                            "type" to ParsedString("Volume"),
                                            "initial_value" to ParsedHashMap(
                                                hashMapOf(
                                                    "type" to ParsedString("com.qfs.pagan.opusmanager.OpusVolumeEvent"),
                                                    "value" to ParsedInt(line_volumes.get_int(i))
                                                )
                                            ),
                                            "children" to ParsedList()
                                        )
                                    )
                                }
                            )
                        }
                    ),
                    "channel_controllers" to ParsedList(
                        mutableListOf(
                            ParsedHashMap(
                                hashMapOf(
                                    "type" to ParsedString("Tempo"),
                                    "initial_value" to ParsedHashMap(
                                        hashMapOf(
                                            "type" to ParsedString("com.qfs.pagan.opusmanager.OpusTempoEvent"),
                                            "value" to ParsedFloat(input_map.get_float("tempo", 120F))
                                        )
                                    ),
                                    "children" to ParsedList()
                                )
                            )
                        )
                    ),
                    "lines" to input_map.get_list("lines"),
                )
            )
        }

        fun convert_v2_to_v3(input_map: ParsedHashMap): ParsedHashMap {
            val lines = input_map.get_list("lines")
            val beat_count = lines.get_hashmap(0).get_list("children").list.size
            val midi_channel = input_map.get_int("midi_channel")
            val new_controllers = if (input_map["channel_controllers"] != null) {
                ActiveControlSetGeneralizer.convert_v2_to_v3(input_map["channel_controllers"] as ParsedList, beat_count)
            } else {
                null
            }

            return ParsedHashMap(
                hashMapOf(
                    "midi_channel" to input_map["midi_channel"],
                    "midi_bank" to input_map["midi_bank"],
                    "midi_program" to input_map["midi_program"],
                    "controllers" to new_controllers,
                    "lines" to ParsedList(
                        MutableList(lines.list.size) { i: Int ->
                            val child_list = lines.get_hashmap(i).get_list("children")
                            val line_controllers = input_map.get_list("line_controllers").get_list(i)
                            val beats = ParsedList()
                            for (j in 0 until child_list.list.size) {
                                val generalized_beat = OpusTreeGeneralizer.convert_v1_to_v3(child_list.get_hashmapn(j)) { event_map: ParsedHashMap ->
                                    if (midi_channel == 9) {
                                        InstrumentEventParser.convert_v1_to_v3_percussion(event_map)
                                    } else {
                                        InstrumentEventParser.convert_v1_to_v3_tuned(event_map)
                                    }
                                } ?: continue
                                beats.add(
                                    ParsedList(
                                        mutableListOf(
                                            ParsedInt(j),
                                            generalized_beat
                                        )
                                    )
                                )
                            }

                            val output_line = ParsedHashMap(
                                hashMapOf(
                                    "controllers" to ActiveControlSetGeneralizer.convert_v2_to_v3(line_controllers, beat_count),
                                    "beats" to beats
                                )
                            )

                            if (midi_channel == 9) {
                                output_line["instrument"] = input_map.get_list("line_static_values").get_int(i)
                            }

                            output_line
                        }
                    )
                )
            )
        }
    }
}
