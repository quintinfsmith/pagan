package com.qfs.radixulous
import android.util.Log
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.structure.OpusTree
import com.qfs.radixulous.apres.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

const val CH_OPEN = '['
const val CH_CLOSE = ']'
const val CH_NEXT = ','
const val CH_ADD = '+'
const val CH_SUBTRACT = '-'
const val CH_UP = '^'
const val CH_DOWN = 'v'
const val CH_HOLD = '~'
const val CH_REPEAT = '='

val REL_CHARS = listOf(CH_ADD, CH_SUBTRACT, CH_UP, CH_DOWN, CH_HOLD, CH_REPEAT)
val SPECIAL_CHARS = listOf(CH_OPEN, CH_CLOSE, CH_NEXT, CH_ADD, CH_SUBTRACT, CH_UP, CH_DOWN, CH_HOLD, CH_REPEAT)

public fun to_string(node: OpusTree<OpusEvent>, depth: Int = 0): String {
    var output: String
    if (node.is_event()) {
        var event = node.get_event()!!
        output = if (event.relative) {
            var new_string: String
            if (event.note == 0 || event.note % event.radix != 0) {
                new_string = if (event.note < 0) {
                    CH_SUBTRACT.toString()
                } else {
                    CH_ADD.toString()
                }
                new_string += get_number_string(abs(event.note), event.radix, 1)
            } else {
                new_string = if (event.note < 0) {
                    CH_DOWN.toString()
                } else {
                    CH_UP.toString()
                }
                new_string += get_number_string(abs(event.note) / event.radix, event.radix, 1)
            }
            new_string
        } else {
            get_number_string(event.note, event.radix, 2)
        }
    } else if (node.is_leaf()) {
        output = "__"
    } else {
        output = ""
        for (i in 0 until node.size) {
            var child = node.get(i)
            output += to_string(child, depth+1)
            if (i < node.size - 1) {
                output += CH_NEXT
            }
        }

        if (node.size > 1 && depth > 0) {
            output = "$CH_OPEN$output$CH_CLOSE"
        }
    }

    return output
}

fun from_string(input_string: String, radix: Int, channel: Int): OpusTree<OpusEvent> {
    var repstring = input_string.trim()
    repstring = repstring.replace(" ", "")
    repstring = repstring.replace("\n", "")
    repstring = repstring.replace("\t", "")
    repstring = repstring.replace("_", "")

    var output = OpusTree<OpusEvent>()

    var tree_stack = mutableListOf(output)
    var register: Int? = null
    var opened_indeces: MutableList<Int> = mutableListOf()
    var relative_flag: Char? = null
    var repeat_queue: MutableList<OpusTree<OpusEvent>> = mutableListOf()
    for (i in repstring.indices) {
        var character = repstring[i]
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
            var last = tree_stack.last()
            if (last.is_leaf()) {
                last.set_size(1)
            }

            var new_tree = last.get(last.size - 1)
            if (! new_tree.is_leaf() && ! new_tree.is_event()) {
                throw Exception("MISSING COMMA")
            }
            tree_stack.add(new_tree)
            opened_indeces.add(i)
        } else if (relative_flag == CH_REPEAT) {
            // Maybe remove?
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
            var leaf = tree_stack.last().get(tree_stack.last().size - 1)
            if (relative_flag != CH_HOLD) {
                leaf.set_event(
                    OpusEvent(
                        odd_note,
                        radix,
                        channel,
                        true
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
                var odd_note = (register * radix) + char_to_int(character, radix)
                if (tree_stack.last().is_leaf()) {
                    tree_stack.last().set_size(1)
                }
                var leaf = tree_stack.last().get(tree_stack.last().size - 1)
                leaf.set_event(
                    OpusEvent(
                        odd_note,
                        radix,
                        channel,
                        false
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

fun get_number_string(number: Int, radix: Int, digits: Int): String {
    var output: String = ""
    var working_number = number
    var selector = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    while (working_number > 0) {
        output = "${selector[working_number % radix]}${output}"
        working_number /= radix
    }

    while (output.length < digits) {
        output = "0${output}"
    }

    return output
}

fun char_to_int(number: Char, radix: Int): Int {
    return str_to_int(number.toString(), radix)
}

fun str_to_int(number: String, radix: Int): Int {
    var selector = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    var output: Int = 0
    for (element in number) {
        output *= radix
        var index = selector.indexOf(element.uppercase())
        output += index
    }
    return output
}

fun tree_from_midi(midi: MIDI): OpusTree<Set<OpusEvent>> {
    var beat_size = midi.get_ppqn()
    var total_beat_offset = 0
    var last_ts_change = 0
    var beat_values: MutableList<OpusTree<Set<OpusEvent>>> = mutableListOf()
    var max_tick: Int = 0
    var press_map = HashMap<Int, Pair<Int, Int>>()

    var did_ts_change = false
    var denominator = 4
    for (pair in midi.get_all_events()) {
        var tick = pair.first
        var event = pair.second

        max_tick = max(tick, max_tick)
        var beat_index = ((tick - last_ts_change) / beat_size) + total_beat_offset
        var inner_beat_offset = (tick - last_ts_change) % beat_size
        if (event is NoteOn && event.get_velocity() > 0) {
            while (beat_values.size <= beat_index) {
                var new_tree = OpusTree<Set<OpusEvent>>()
                new_tree.set_size(beat_size)
                beat_values.add(new_tree)
            }

            var tree = beat_values[beat_index]
            var eventset = if (tree[inner_beat_offset].is_event()) {
                tree[inner_beat_offset].get_event()!!.toMutableSet()
            } else {
                mutableSetOf()
            }

            eventset.add(
                OpusEvent(
                    if (event.channel == 9) {
                        event.get_note() - 27
                    } else {
                        event.get_note() - 21
                    },
                    12,
                    event.channel,
                    false
                )
            )

            tree[inner_beat_offset].set_event(eventset)
            press_map[event.note] = Pair(beat_index, inner_beat_offset)
        } else if (event is TimeSignature) {
            total_beat_offset += (tick - last_ts_change) / beat_size
            last_ts_change = tick
            beat_size = midi.get_ppqn() / 2.toFloat().pow(event.get_denominator()).toInt()
            if (tick != 0) {
                did_ts_change = true
            } else {
                denominator = 2.toFloat().pow(event.get_denominator()).toInt()
            }
        } else if (event is SetTempo) {
            //pass TODO (maybe)
        }
    }

    total_beat_offset += (max_tick - last_ts_change) / beat_size
    total_beat_offset += 1

    var opus = OpusTree<Set<OpusEvent>>()
    opus.set_size(total_beat_offset)

    beat_values.forEachIndexed { i, beat_tree ->
        if (! beat_tree.is_leaf()) {
            for (subtree in beat_tree.divisions.values) {
                subtree.clear_singles()
            }
        }
        opus.set(i, beat_tree)
    }

    if (!did_ts_change) {
        opus.reduce(opus.size / denominator)
    }

    for ((k, beat) in opus.divisions) {
        beat.flatten()
        beat.reduce()
        beat.clear_singles()
    }
    return opus
}

fun tree_to_midi(
        tree: OpusTree<Set<OpusEvent>>,
        tempo: Float = 120.0.toFloat(),
        transpose: Int = 0,
        i_arg: HashMap<Int, Int>? = null
        ): MIDI {

    var instruments = i_arg ?: HashMap<Int, Int>()

    var midi = MIDI()
    for (i in 0 until 16) {
        var instrument = instruments[i]?: 0
        midi.insert_event(0,0, ProgramChange(i, instrument))
    }

    midi.insert_event(0,0, SetTempo.from_bpm(tempo))

    var current_tick = 0
    var prev_note_map = HashMap<Int, Int>()
    for (m in 0 until tree.size) {
        var beat = tree.get(m)

        if (beat.is_event()) {
            for (event in beat.get_event()!!) {
                var channel = event.channel

                var note = if (event.relative) {
                    prev_note_map[channel]!! + event.note
                } else {
                    var tmp: Int = event.note
                    if (channel == 9) {
                        tmp -= 3
                    } else {
                        tmp += transpose + 21
                    }
                    tmp
                }

                prev_note_map[channel] = note
                midi.insert_event(
                    0,
                    current_tick,
                    NoteOn(channel, note, 64)
                )
                midi.insert_event(
                    0,
                    current_tick + midi.ppqn,
                    NoteOff(channel, note, 64)
                )
            }

        } else if (!beat.is_leaf()) {
            if (!beat.is_flat()) {
                beat.flatten()
            }

            beat.reduce()
            beat.flatten()

            var div_size: Int = midi.ppqn / beat.size
            for (i in 0 until beat.size) {
                var leaf = beat.get(i)
                if (!leaf.is_event()) {
                    continue
                }

                for (event in leaf.get_event()!!) {
                    var channel = event.channel

                    var note = if (event.relative) {
                        prev_note_map[channel]!! + event.note
                    } else {
                        var tmp: Int = event.note
                        if (channel == 9) {
                            tmp -= 3
                        } else {
                            tmp += transpose + 21
                        }
                        tmp
                    }
                    prev_note_map[channel] = note
                    midi.insert_event(
                        0,
                        current_tick + (i * div_size),
                        NoteOn(channel, note, 64)
                    )
                    midi.insert_event(
                        0,
                        current_tick + ((i + 1) * div_size),
                        NoteOff(channel, note, 64)
                    )
                }
            }
        }
        current_tick += midi.ppqn
    }

    return midi
}
