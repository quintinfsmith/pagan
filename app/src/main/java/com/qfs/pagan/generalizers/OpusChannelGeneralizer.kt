package com.qfs.pagan.opusmanager

import com.qfs.json.ParsedBoolean
import com.qfs.json.ParsedHashMap
import com.qfs.json.ParsedInt
import com.qfs.json.ParsedList
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
        fun generalize(channel: OpusChannel): ParsedHashMap {
            val channel_map = ParsedHashMap()
            val lines = ParsedList(
                MutableList(channel.size) { i: Int ->
                    OpusLineGeneralizer.to_json(channel.lines[i])
                }
            )
            channel_map["lines"] = lines
            channel_map["midi_channel"] = channel.get_midi_channel()
            channel_map["midi_bank"] = channel.midi_bank
            channel_map["midi_program"] = channel.midi_program

            return channel_map
        }

        private fun _interpret_percussion(input_map: ParsedHashMap): OpusPercussionChannel {
            val channel = OpusPercussionChannel()
            val input_lines = input_map.get_list("lines")
            for (line in input_lines.list) {
                channel.lines.add(OpusLineGeneralizer.percussion_line(line as ParsedHashMap))
            }

            return channel
        }

        private fun _interpret_std(input_map: ParsedHashMap): OpusChannel {
            val channel = OpusChannel(-1)
            val midi_channel = input_map.get_int("midi_channel")
            channel.midi_channel = midi_channel
            channel.midi_bank = input_map.get_int("midi_bank")

            val input_lines = input_map.get_list("lines")
            for (line in input_lines.list) {
                channel.lines.add(OpusLineGeneralizer.opus_line(line as ParsedHashMap))
            }

            return channel
        }

        fun interpret(input_map: ParsedHashMap): OpusChannelAbstract<*,*> {
            val midi_channel = input_map.get_int("midi_channel")

            val channel = if (midi_channel == 9) {
                _interpret_percussion(input_map)
            } else {
                _interpret_std(input_map)
            }

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
                    "line_volumes" to input_map["line_volumes"],
                    "line_static_values" to ParsedList(MutableList(new_lines.list.size) { null })
                )
            )
        }

        fun convert_v1_to_v2(input_map: ParsedHashMap): ParsedHashMap {
            // Get Beat Count
            val line_tree = OpusTreeGeneralizer.from_v1_json(input_map.get_list("lines").get_hashmap(0)) { null }
            val beat_count = line_tree.size

            // Set up ControlSet with Tempo Controller
            val controllers = ActiveControlSet(beat_count)
            controllers.new_controller(ControlEventType.Tempo, TempoController(beat_count))
            val controller = controllers.get_controller(ControlEventType.Tempo)
            controller.set_initial_event(OpusTempoEvent(input_map.get_float("tempo")))

            val line_volumes = input_map.get_list("line_volumes")

            return ParsedHashMap(
                hashMapOf(
                    "midi_channel" to input_map["midi_channel"],
                    "midi_bank" to input_map["midi_bank"],
                    "midi_program" to input_map["midi_program"],
                    "line_static_values" to input_map["line_static_values"],
                    "line_controllers" to ParsedList(
                        MutableList(line_volumes.list.size) { i: Int ->
                            val active_control_set = ActiveControlSet(beat_count)
                            active_control_set.new_controller(ControlEventType.Volume, VolumeController(beat_count))
                            active_control_set.get_controller(ControlEventType.Volume).set_initial_event(OpusVolumeEvent(line_volumes.get_int(i)))
                            ActiveControlSetGeneralizer.to_json(active_control_set)
                        }
                    ),
                    "channel_controllers" to ActiveControlSetGeneralizer.to_json(controllers),
                    "lines" to input_map["lines"]
                )
            )
        }

        fun convert_v2_to_v3(input_map: ParsedHashMap): ParsedHashMap {
            val lines = input_map.get_list("lines")
            val midi_channel = input_map.get_int("midi_channel")
            return ParsedHashMap(
                hashMapOf(
                    "midi_channel" to input_map["midi_channel"],
                    "midi_bank" to input_map["midi_bank"],
                    "midi_program" to input_map["midi_program"],
                    "controllers" to input_map["controllers"],
                    "lines" to ParsedList(
                        MutableList(lines.list.size) { i: Int ->
                            val output_line = ParsedHashMap(
                                hashMapOf(
                                    "controllers" to input_map.get_list("line_controllers").get_hashmap(i),
                                    "beats" to lines.get_list(i)
                                )
                            )

                            if (midi_channel == 9) {
                                output_line["instrument"] = input_map["static_value"]
                            }

                            output_line
                        }
                    )
                )
            )
        }
    }
}
