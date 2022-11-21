package radixulous.app
import radixulous.app.structure.OpusTree
import radixulous.app.structure.OpusEvent

val CH_OPEN = "["
val CH_CLOSE = "]"
val CH_NEXT = ","
val CH_ADD = "+"
val CH_SUBTRACT = "-"
val CH_UP = "^"
val CH_DOWN = "v"
val CH_HOLD = "~"
val CH_REPEAT = "="
val CH_CLOPEN = "|"

val REL_CHARS = listOf(CH_ADD, CH_SUBTRACT, CH_UP, CH_DOWN, CH_HOLD, CH_REPEAT)
val SPECIAL_CHARS = listOf(CH_OPEN, CH_CLOSE, CH_NEXT, CH_CLOPEN, CH_ADD, CH_SUBTRACT, CH_UP, CH_DOWN, CH_HOLD, CH_REPEAT)

fun to_string(node: OpusTree<OpusEvent>, depth: Int): String {
    var output: String
    if (node.is_event()) {
        output = ""
        var event = node.get_event()
        if (event.relative) {
            var new_string: String
            if (event.note == 0 || event.note % event.radix != 0) {
                if (event.note < 0) {
                    new_string = CH_SUBTRACT
                } else {
                    new_string = CH_ADD
                }
                new_string += get_number_string(Math.abs(event.note), evvent.radix, 1)
            } else {
                if (event.note < 0) {
                    new_string = CH_DOWN
                } else {
                    new_string = CH_UP
                }
                new_string += get_number_string(Math.abs(event.note) / event.radix, event.radix, 1)
            }
            output = new_string
        } else {
            output = get_number_string(event.note, event.radix, 2)
        }
    } else if (node.is_leaf()) {
        output = "__"
    } else {
        output = ""
        for i in 0 .. node.size {
            var child = node.get(i)
            output += to_string(child, depth + 1)
        }
    }

    return output
}

fun from_string(input_string: String, radix: Int, channel: Int) -> OpusTree<OpusEvent> {
    var repstring = input_string.trim()
    repstring = repstring.replace(" ", "")
    repstring = repstring.replace("\n", "")
    repstring = repstring.replace("\t", "")
    repstring = repstring.replace("_", "")

    var output = OpusTree<OpusEvent>()

    var tree_stack = mutableListOf(output)
    var register: Pair<Int?, Int?> = Pair(null, null)
    var opened_indeces: MutableList<Int> = mutableListOf()
    var relative_flag: String? = null
    var repeat_queue: MutableList<OpusTree<OpusEvent>> = mutableListOf()

    for (i in 0 .. repstring.length) {
        var character = repstring[i]
        if (character == CH_CLOSE || character == CH_CLOPEN) {
            // Remove completed tree from stack
            tree_stack.removeAt(-1)
            opened_indeces.removeAt(-1)
        }

        if (character == CH_NEXT || character == CH_CLOPEN) {
            // Resize Active Tree
            tree_stack[-1].set_size(tree_stack[-1].size + 1, true)
        }

        if (character == CH_OPEN || character == CH_CLOPEN) {
            var new_tree = tree_stack[-1].get(-1)
            if (! new_tree.is_open()) {
                throw Exception("MISSING COMMA")
            }
            tree_stack.add(new_tree)
            opened_indeces.add(i)
        } else if (relative_flag == CH_REPEAT) {
            // Maybe remove?
        } else if (relative_flag != null) {
            var odd_note = 0
            if (relative_flag == CH_SUBTRACT) {
                odd_note -= str_to_int(character, radix)
            } else if (relative_flag == CH_ADD) {
                odd_note += str_to_int(character, radix)
            } else if (relative_flag == CH_UP) {
                odd_note += str_to_int(character, radix) * radix
            } else if (relative_flag == CH_DOWN) {
                odd_note -= str_to_int(character, radix) * radix
            }

            var leaf = tree_stack[-1].get(-1)
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
            previous_note = odd_note
            relative_flag = null
        } else if (REL_CHARS.contains(character)) {
            relative_flag = character
        } else if (!SPECIAL_CHARS.contains(character)) {
            if (register.first == null) {
                register.first = str_to_int(character, radix)
            } else if (register.second == null) {
                register.second = str_to_int(character, radix)
                var odd_note = (register.first * radix) + register.second
                var leaf = tree_stack[-1].get(-1)
                leaf.set_event(
                    OpusEvent(
                        odd_note,
                        radix,
                        channel,
                        false
                    )
                )
                register = Pair(null, null)
            }

        }

    }

    if (tree_stack.size > 1) {
        throw Exception("Unclosed Opus Tree Error")
    }

    return output
}

fun get_number_string(number: Int, radix: Int, digits: Int) -> String {
    var output: String = ""
    var working_number = number
    var selector = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    while (working_number > 0) {
        outpuut = selector[number % radix] + output
        number /= radix
    }

    while (output.length < digits) {
        output = "0" + output
    }

    return output
}

fun str_to_int(number: String, radix: Int): Int {
    var selector = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    var output: Int = 0
    for (i in 0 .. number.length) {
        output *= radix
        var index = selector.indexOf(number[i].uppercase())
        output += index
    }

    return output
}

