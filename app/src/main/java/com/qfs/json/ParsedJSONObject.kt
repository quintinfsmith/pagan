package com.qfs.json
import kotlin.math.max
import kotlin.math.min

class InvalidJSON(json_string: String, index: Int): Exception("Invalid JSON @ $index In \"${json_string.substring(max(0, index - 20), min(json_string.length, index + 20))}\"")
enum class ItemType {
    String,
    Boolean,
    List,
    HashMap,
    Int,
    Float,
    Null
}
data class ItemToken(val item_type: ItemType, val index: Int)

interface ParsedObject { }

data class ParsedString(var value: String): ParsedObject
data class ParsedFloat(var value: Float): ParsedObject
data class ParsedInt(var value: Int): ParsedObject
data class ParsedBoolean(var value: Boolean): ParsedObject

class ParsedHashMap(input_map: HashMap<String, ParsedObject?>? = null): ParsedObject {
    val hash_map = input_map ?: HashMap<String, ParsedObject?>()
}

class ParsedList(input_list: List<ParsedObject?>? = null): ParsedObject {
    val list = input_list ?: mutableListOf<ParsedObject?>()
}

class Parser {
    companion object {
        fun parse(json_content: String): ParsedObject? {
            var working_number: String? = null
            var working_string: String? = null
            var string_escape_flagged = false

            val hashmap_key_stack = mutableListOf<Pair<Int, String>>()
            val object_stack = mutableListOf<ParsedObject?>()

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
                                val working_number: ParsedObject = if (working_number.contains(".")) {
                                    ParsedFloat(working_number.toFloat())
                                } else {
                                    ParsedInt(working_number.toInt())
                                }

                                if (object_stack.isEmpty()) {
                                    object_stack.add(working_number)
                                } else {
                                    when (object_stack.last()) {
                                        is ParsedList -> {
                                            (object_stack.last() as ParsedList).list.add(working_number)
                                        }
                                        is ParsedHashMap -> {
                                            (object_stack.last() as ParsedHashMap).hash_map[hashmap_key_stack.removeLast()] = working_number
                                        }
                                        else -> {
                                            throw InvalidJSON(json_content, index)
                                        }
                                    }
                                }

                                working_number = null
                                continue
                            } catch (e: Exception) {
                                throw InvalidJSON(json_content, index)
                            }
                        }
                        else -> {
                            throw InvalidJSON(json_content, index)
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
                                if (object_stack.isEmpty()) {
                                    object_stack.add(ParsedString(working_string))
                                } else {
                                    when (object_stack.last()) {
                                        is ParsedList -> {
                                            (object_stack.last() as ParsedList).list.add(ParsedString(working_string))
                                        }
                                        is ParsedHashMap -> {
                                            val (key_depth, hashmap_key) = hashmap_key_stack.last()
                                            if (key_depth == object_stack.size) {
                                                (object_stack.last() as ParsedHashMap).hash_map[hashmap_key] = ParsedString(working_string)
                                                hashmap_key_stack.removeLast()
                                            } else {
                                                hashmap_key_stack.add(Pair(object_stack.size, working_string))
                                            }
                                        }
                                        else -> {
                                            throw InvalidJSON(json_content, index)
                                        }
                                    }
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
                            // TODO: Check Expected char
                        }
                        ':' -> {
                            // TODO: Check Expected char
                        }

                        Char(123) -> {
                            if (object_stack.isEmpty()) {
                                object_stack.add(ParsedHashMap())
                            } else {
                                when (object_stack.last()) {
                                    is ParsedList -> {
                                        (object_stack.last() as ParsedList).list.add(ParsedHashMap())
                                    }
                                    is ParsedHashMap -> {
                                        val (_, hashmap_key) = hashmap_key_stack.removeLast()
                                        (object_stack.last() as ParsedHashMap).hash_map[hashmap_key] = ParsedHashMap()
                                    }
                                    else -> {
                                        throw InvalidJSON(json_content, index)
                                    }
                                }
                            }
                        }

                        Char(125) -> {
                            if (object_stack.isNotEmpty() && object_stack.last() is ParsedHashMap) {
                                object_stack.removeLast()
                            } else {
                                throw InvalidJSON(json_content, index)
                            }
                        }
                        Char(91) -> {
                            if (object_stack.isEmpty()) {
                                object_stack.add(ParsedList())
                            } else {
                                when (object_stack.last()) {
                                    is ParsedList -> {
                                        (object_stack.last() as ParsedList).list.add(ParsedList())
                                    }
                                    is ParsedHashMap -> {
                                        val (_, hashmap_key) = hashmap_key_stack.removeLast()
                                        (object_stack.last() as ParsedHashMap).hash_map[hashmap_key] = ParsedList()
                                    }
                                    else -> {
                                        throw InvalidJSON(json_content, index)
                                    }
                                }
                            }
                        }
                        Char(93) -> {
                            if (object_stack.isNotEmpty() && object_stack.last() is ParsedList) {
                                object_stack.removeLast()
                            } else {
                                throw InvalidJSON(json_content, index)
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
                                throw InvalidJSON(json_content, index)
                            } else {
                                if (object_stack.isEmpty()) {
                                    object_stack.add(null)
                                } else {
                                    when (object_stack.last()) {
                                        is ParsedList -> {
                                            (object_stack.last() as ParsedList).list.add(null)
                                        }
                                        is ParsedHashMap -> {
                                            val (_, hashmap_key) = hashmap_key_stack.removeLast()
                                            (object_stack.last() as ParsedHashMap).hash_map[hashmap_key] = null
                                        }
                                        else -> {
                                            throw InvalidJSON(json_content, index)
                                        }
                                    }
                                }
                                index += 3
                            }
                        }
                        'f' -> {
                            if (json_content.substring(index, index + 5) != "false") {
                                throw InvalidJSON(json_content, index)
                            } else {
                                if (object_stack.isEmpty()) {
                                    object_stack.add(ParsedBoolean(false))
                                } else {
                                    when (object_stack.last()) {
                                        is ParsedList -> {
                                            (object_stack.last() as ParsedList).list.add(ParsedBoolean(false))
                                        }
                                        is ParsedHashMap -> {
                                            val (_, hashmap_key) = hashmap_key_stack.removeLast()
                                            (object_stack.last() as ParsedHashMap).hash_map[hashmap_key] = ParsedBoolean(false)
                                        }
                                        else -> {
                                            throw InvalidJSON(json_content, index)
                                        }
                                    }
                                }
                                index += 4
                            }
                        }
                        't' -> {
                            if (json_content.substring(index, index + 4) != "true") {
                                throw InvalidJSON(json_content, index)
                            } else {
                                if (object_stack.isEmpty()) {
                                    object_stack.add(ParsedBoolean(true))
                                } else {
                                    when (object_stack.last()) {
                                        is ParsedList -> {
                                            (object_stack.last() as ParsedList).list.add(ParsedBoolean(true))
                                        }
                                        is ParsedHashMap -> {
                                            val (_, hashmap_key) = hashmap_key_stack.removeLast()
                                            (object_stack.last() as ParsedHashMap).hash_map[hashmap_key] = ParsedBoolean(true)
                                        }
                                        else -> {
                                            throw InvalidJSON(json_content, index)
                                        }
                                    }
                                }
                                index += 3
                            }
                        }
                        else -> {
                            throw InvalidJSON(json_content, index)
                        }
                    }
                }

                index += 1
            }

            if (object_stack.size != 1) {
                throw InvalidJSON(json_content, json_content.length - 1)
            }

            return object_stack.removeFirst()
        }
    }
}
