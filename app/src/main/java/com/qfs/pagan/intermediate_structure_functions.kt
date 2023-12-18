package com.qfs.pagan

import android.util.Log
import com.qfs.apres.Midi
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event.TimeSignature
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
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

val REL_CHARS = listOf(CH_ADD, CH_SUBTRACT, CH_UP, CH_DOWN, CH_HOLD)
val SPECIAL_CHARS = listOf(CH_OPEN, CH_CLOSE, CH_NEXT, CH_ADD, CH_SUBTRACT, CH_UP, CH_DOWN, CH_HOLD)

fun to_string(radix: Int, node: OpusTree<OpusEvent>, depth: Int = 0): String {
    var output: String
    if (node.is_event()) {
        val opus_event = node.get_event()!!
        output = if (opus_event.relative) {
            var new_string: String
            if (opus_event.note == 0 || opus_event.note % radix != 0) {
                new_string = if (opus_event.note < 0) {
                    CH_SUBTRACT.toString()
                } else {
                    CH_ADD.toString()
                }
                new_string += get_number_string(abs(opus_event.note), radix, 1)
            } else {
                new_string = if (opus_event.note < 0) {
                    CH_DOWN.toString()
                } else {
                    CH_UP.toString()
                }
                new_string += get_number_string(abs(opus_event.note) / radix, radix, 1)
            }
            new_string
        } else {
            get_number_string(opus_event.note, radix, 2)
        }
    } else if (node.is_leaf()) {
        output = "__"
    } else {
        output = ""
        for (i in 0 until node.size) {
            val child = node[i]
            output += to_string(radix, child, depth+1)
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
    val repstring = input_string
        .trim()
        .replace(" ", "")
        .replace("\n", "")
        .replace("\t", "")
        .replace("_", "")

    val output = OpusTree<OpusEvent>()

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
                    OpusEvent(
                        odd_note,
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
                val odd_note = (register * radix) + char_to_int(character, radix)
                if (tree_stack.last().is_leaf()) {
                    tree_stack.last().set_size(1)
                }
                val leaf = tree_stack.last()[tree_stack.last().size - 1]
                leaf.set_event(
                    OpusEvent(
                        odd_note,
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
    var output = ""
    var working_number = number
    val selector = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
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
    val selector = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    var output = 0
    for (element in number) {
        output *= radix
        val index = selector.indexOf(element.uppercase())
        output += index
    }
    return output
}

fun tree_from_midi(midi: Midi): OpusTree<Set<OpusEvent>> {
    var beat_size = midi.get_ppqn()
    var total_beat_offset = 0
    var last_ts_change = 0
    val beat_values: MutableList<OpusTree<Set<OpusEvent>>> = mutableListOf()
    var max_tick = 0
    val press_map = HashMap<Int, Pair<Int, Int>>()

    for (pair in midi.get_all_events()) {
        val tick = pair.first
        val event = pair.second

        max_tick = max(tick, max_tick)
        val beat_index = ((tick - last_ts_change) / beat_size) + total_beat_offset
        val inner_beat_offset = (tick - last_ts_change) % beat_size
        if (event is NoteOn && event.get_velocity() > 0) {
            while (beat_values.size <= beat_index) {
                val new_tree = OpusTree<Set<OpusEvent>>()
                new_tree.set_size(beat_size)
                beat_values.add(new_tree)
            }

            val tree = beat_values[beat_index]
            val eventset = if (tree[inner_beat_offset].is_event()) {
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
                    event.channel,
                    false
                )
            )

            tree[inner_beat_offset].set_event(eventset)
            press_map[event.get_note()] = Pair(beat_index, inner_beat_offset)
        } else if (event is TimeSignature) {
            total_beat_offset += (tick - last_ts_change) / beat_size
            last_ts_change = tick
            beat_size = midi.get_ppqn() / 2.toFloat().pow(event.get_denominator()).toInt()
            //denominator = 2.toFloat().pow(event.get_denominator()).toInt()
        } else if (event is SetTempo) {
            //pass TODO (maybe)
        }
    }

    total_beat_offset += (max_tick - last_ts_change) / beat_size
    total_beat_offset += 1

    val opus = OpusTree<Set<OpusEvent>>()
    opus.set_size(total_beat_offset)

    beat_values.forEachIndexed { i, beat_tree ->
        if (! beat_tree.is_leaf()) {
            for (subtree in beat_tree.divisions.values) {
                subtree.clear_singles()
            }
        }
        opus.set(i, beat_tree)
    }

    for ((_, beat) in opus.divisions) {
        beat.flatten()
        beat.reduce()
        beat.clear_singles()
    }
    return opus
}

fun tlog(label: String, callback: () -> Unit) {
    val a = System.currentTimeMillis()
    callback()
    Log.d("TLOG", "$label: ${System.currentTimeMillis() - a}")
}