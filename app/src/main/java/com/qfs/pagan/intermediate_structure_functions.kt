package com.qfs.pagan

import android.util.Log
import com.qfs.pagan.opusmanager.OpusEventSTD
import com.qfs.pagan.structure.OpusTree
import kotlin.math.abs

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

fun to_string(radix: Int, node: OpusTree<OpusEventSTD>, depth: Int = 0): String {
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

fun from_string(input_string: String, radix: Int, channel: Int): OpusTree<OpusEventSTD> {
    val repstring = input_string
        .trim()
        .replace(" ", "")
        .replace("\n", "")
        .replace("\t", "")
        .replace("_", "")

    val output = OpusTree<OpusEventSTD>()

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
                    OpusEventSTD(
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
                    OpusEventSTD(
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

fun tlog(label: String, callback: () -> Unit) {
    val a = System.currentTimeMillis()
    callback()
    Log.d("TLOG", "$label: ${System.currentTimeMillis() - a}")
}