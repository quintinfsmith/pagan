package com.qfs.radixulous
import com.qfs.radixulous.structure.OpusTree
import com.qfs.radixulous.opusmanager.OpusEvent

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

public fun to_string(node: OpusTree<OpusEvent>): String {
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
                new_string += get_number_string(Math.abs(event.note), event.radix, 1)
            } else {
                new_string = if (event.note < 0) {
                    CH_DOWN.toString()
                } else {
                    CH_UP.toString()
                }
                new_string += get_number_string(Math.abs(event.note) / event.radix, event.radix, 1)
            }
            new_string
        } else {
            get_number_string(event.note, event.radix, 2)
        }
    } else if (node.is_leaf()) {
        output = "__"
    } else {
        output = ""
        for (i in 0 .. node.size - 1) {
            var child = node.get(i)
            output += to_string(child)
            if (i < node.size - 1) {
                output += CH_NEXT
            }
        }

        if (node.size > 1) {
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

    for (i in 0 .. repstring.length - 1) {
        var character = repstring[i]
        if (character == CH_CLOSE) {
            // Remove completed tree from stack
            tree_stack.removeLast()
            opened_indeces.removeLast()
        }

        if (character == CH_NEXT) {
            // Resize Active Tree
            tree_stack.last().set_size(tree_stack.last().size + 1, true)
        }

        if (character == CH_OPEN) {
            var new_tree = tree_stack.last().get(tree_stack.last().size - 1)
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
            if (register == null) {
                register = char_to_int(character, radix)
            } else {
                var odd_note = (register * radix) + char_to_int(character, radix)
                var leaf = tree_stack.last().get(tree_stack.last().size - 1)
                leaf.set_event(
                    OpusEvent(
                        odd_note,
                        radix,
                        channel,
                        false
                    )
                )
                register = null
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
        output = selector[number % radix] + output
        working_number /= radix
    }

    while (output.length < digits) {
        output = "0" + output
    }

    return output
}
fun char_to_int(number: Char, radix: Int): Int {
    return str_to_int(number.toString(), radix)
}
fun str_to_int(number: String, radix: Int): Int {
    var selector = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    var output: Int = 0
    for (i in 0 .. number.length - 1) {
        output *= radix
        var index = selector.indexOf(number[i].uppercase())
        output += index
    }

    return output
}
