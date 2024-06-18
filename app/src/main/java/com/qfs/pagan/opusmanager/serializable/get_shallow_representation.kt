package com.qfs.pagan.opusmanager.serializable

enum class ItemType {
    String,
    Boolean,
    List,
    HashMap,
    Int,
    Float
}
data class ItemToken(val item_type: ItemType, val index: Int)

class ParsedJSON(json_string: String) {
    val strings = mutableListOf<String>()
    val booleans = mutableListOf<Boolean>()
    val lists = mutableListOf<ItemToken>()
    val hashmaps = mutableListOf<HashMap<String, ItemToken>>()
    val ints = mutableListOf<Int>()
    val floats = mutableListOf<Float>()

    fun get_string(token: ItemToken): String {
        if (token.item_type != ItemType.String) {
            throw Exception()
        }
        return this.strings[token.index]
    }
    fun get_int(token: ItemToken): Int {
        if (token.item_type != ItemType.Int) {
            throw Exception()
        }
        return this.ints[token.index]
    }
    fun get_float(token: ItemToken): Float {
        if (token.item_type != ItemType.Float) {
            throw Exception()
        }
        return this.floats[token.index]
    }


    fun parse(json_content: String): HashMap<String, String> {
        val output = HashMap<String, String>()

        var value_index_i = -1
        var value_index_f = -1
        var working_key: String? = null

        var working_number: String? = null
        var working_string: String? = null
        var string_escape_flagged = false

        val type_stack = mutableListOf<Int>() // 0 = dict, 1 = list
        val hashmap_key_stack = mutableListOf<String>()
        val hashmaps_stack = mutableListOf<HashMap<String, ItemToken>>()
        val list_stack = mutableListOf<MutableList<ItemToken>>()

        var index = 0
        while (index < json_content.length) {
            val working_char = json_content[index]
            if (working_number != null) {
                when (working_char) {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' -> {
                        working_number += working_char
                    }
                    ' ', '\r', '\n', Char(125), Char(93), ',' -> {
                        try {
                            working_number.toFloat()
                            working_number = null
                            continue
                        } catch (e: Exception) {
                            throw exceptions.InvalidJSON(json_content, index)
                        }
                    }
                    else -> {
                        throw exceptions.InvalidJSON(json_content, index)
                    }
                }
            } else if (working_string != null) {
                if (string_escape_flagged) {
                    working_string += working_char
                    string_escape_flagged = false
                } else {
                    when (working_char) {
                        '\\' -> {
                            string_escape_flagged = true
                        }
                        '"' -> {
                            if (working_key == null && type_stack.size == 1) {
                                working_key = working_string
                            }
                            working_string = null
                        }
                        else -> {
                            working_string += working_char
                        }
                    }
                }
            } else {
                when (working_char) {
                    ',' -> {
                        if (type_stack.size == 1 && value_index_i >= 0) {
                            value_index_f = index
                        }
                    }
                    ':' -> {
                        if (type_stack.size == 1) {
                            value_index_i = index + 1
                        }
                    }
                    Char(123) -> {
                        if (type_stack.size == 1) {
                            working_key == null
                        }
                        type_stack.add(0)
                    }
                    Char(125) -> {
                        if (type_stack.last() == 0) {
                            type_stack.removeLast()
                        } else {
                            throw exceptions.InvalidJSON(json_content, index)
                        }
                        if (type_stack.size < 2 && value_index_i >= 0) {
                            value_index_f = index + 1
                        }
                    }
                    Char(91) -> {
                        type_stack.add(1)
                    }
                    Char(93) -> {
                        if (type_stack.last() == 1) {
                            type_stack.removeLast()
                        } else {
                            throw exceptions.InvalidJSON(json_content, index)
                        }

                        if (type_stack.size == 1 && value_index_i >= 0) {
                            value_index_f = index + 1
                        }
                    }
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-' -> {
                        working_number = "" + working_char
                    }
                    '"' -> {
                        working_string = ""
                    }
                    ' ', '\r', '\n', '\t' -> {
                    }
                    'n' -> {
                        if (json_content.substring(index, index + 4) != "null") {
                            throw exceptions.InvalidJSON(json_content, index)
                        } else {
                            index += 3
                        }
                    }
                    'f' -> {
                        if (json_content.substring(index, index + 5) != "false") {
                            throw exceptions.InvalidJSON(json_content, index)
                        } else {
                            index += 4
                        }
                    }
                    't' -> {
                        if (json_content.substring(index, index + 4) != "true") {
                            throw exceptions.InvalidJSON(json_content, index)
                        } else {
                            index += 3
                        }
                    }
                    else -> {
                        throw exceptions.InvalidJSON(json_content, index)
                    }
                }
            }

            if (working_key != null && value_index_f >= 0 && value_index_i >= 0) {
                output[working_key!!] = json_content.substring(value_index_i, value_index_f).trim()
                working_key = null
                value_index_i = -1
                value_index_f = -1
            }
            index += 1
        }
        return output
    }
}

fun get_shallow_representation(json_content: String): HashMap<String, String> {
    val output = HashMap<String, String>()

    var value_index_i = -1
    var value_index_f = -1
    var working_key: String? = null

    var working_number: String? = null
    var working_string: String? = null
    var string_escape_flagged = false

    var type_stack = mutableListOf<Int>() // 0 = dict, 1 = list

    var index = 0
    while (index < json_content.length) {
        val working_char = json_content[index]
        if (working_number != null) {
            when (working_char) {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' -> {
                    working_number += working_char
                }
                ' ', '\r', '\n', Char(125), Char(93), ',' -> {
                    try {
                        working_number.toFloat()
                        working_number = null
                        continue
                    } catch (e: Exception) {
                        throw exceptions.InvalidJSON(json_content, index)
                    }
                }
                else -> {
                    throw exceptions.InvalidJSON(json_content, index)
                }
            }
        } else if (working_string != null) {
            if (string_escape_flagged) {
                working_string += working_char
                string_escape_flagged = false
            } else {
                when (working_char) {
                    '\\' -> {
                        string_escape_flagged = true
                    }
                    '"' -> {
                        if (type_stack.last() == 1) {
                            hashmap_key_stack.add(working_string)
                        }
                        working_string = null
                    }
                    else -> {
                        working_string += working_char
                    }
                }
            }
        } else {
            when (working_char) {
                ',' -> {
                    if (type_stack.size == 1 && value_index_i >= 0) {
                        value_index_f = index
                    }
                }
                ':' -> {
                    if (type_stack.size == 1) {
                        value_index_i = index + 1
                    }
                }
                Char(123) -> {
                    if (type_stack.size == 1) {
                        working_key == null
                    }
                    type_stack.add(0)
                }
                Char(125) -> {
                    if (type_stack.last() == 0) {
                        type_stack.removeLast()
                    } else {
                        throw exceptions.InvalidJSON(json_content, index)
                    }
                    if (type_stack.size < 2 && value_index_i >= 0) {
                        value_index_f = index + 1
                    }
                }
                Char(91) -> {
                    type_stack.add(1)
                }
                Char(93) -> {
                    if (type_stack.last() == 1) {
                        type_stack.removeLast()
                    } else {
                        throw exceptions.InvalidJSON(json_content, index)
                    }

                    if (type_stack.size == 1 && value_index_i >= 0) {
                        value_index_f = index + 1
                    }
                }
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-' -> {
                    working_number = "" + working_char
                }
                '"' -> {
                    working_string = ""
                }
                ' ', '\r', '\n', '\t' -> {
                }
                'n' -> {
                    if (json_content.substring(index, index + 4) != "null") {
                        throw exceptions.InvalidJSON(json_content, index)
                    } else {
                        index += 3
                    }
                }
                'f' -> {
                    if (json_content.substring(index, index + 5) != "false") {
                        throw exceptions.InvalidJSON(json_content, index)
                    } else {
                        index += 4
                    }
                }
                't' -> {
                    if (json_content.substring(index, index + 4) != "true") {
                        throw exceptions.InvalidJSON(json_content, index)
                    } else {
                        index += 3
                    }
                }
                else -> {
                    throw exceptions.InvalidJSON(json_content, index)
                }
            }
        }

        if (working_key != null && value_index_f >= 0 && value_index_i >= 0) {
            output[working_key!!] = json_content.substring(value_index_i, value_index_f).trim()
            working_key = null
            value_index_i = -1
            value_index_f = -1
        }
        index += 1
    }
    return output
}

