package com.qfs.json

class JSONParser {
    companion object {
        fun <T: JSONObject> parse(json_content: String): T? {
            var working_number: String? = null
            var working_string: String? = null
            var string_escape_flagged = false

            val object_stack = mutableListOf<JSONObject?>()
            val position_stack = mutableListOf<Int>()
            var index = 0
            var close_expected = false

            while (index < json_content.length) {
                val working_char = json_content[index]
                if (working_number != null) {
                    when (working_char) {
                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' -> {
                            working_number += working_char
                        }

                        ' ', '\r', '\n', Char(125), Char(93), ',' -> {
                            try {
                                val new_number: JSONObject = if (working_number.contains(".")) {
                                    JSONFloat(working_number.toFloat())
                                } else {
                                    JSONInteger(working_number.toInt())
                                }

                                object_stack.add(new_number)
                                close_expected = true

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
                        working_string += '\\'
                        working_string += working_char
                        string_escape_flagged = false
                    } else {
                        when (working_char) {
                            '\\' -> {
                                string_escape_flagged = true
                            }

                            '"' -> {
                                object_stack.add(JSONString.unescape(working_string))
                                close_expected = true

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
                            //if (!close_expected) {
                            //    throw InvalidJSON(json_content, index)
                            //}

                            val to_add_object = object_stack.removeAt(object_stack.size - 1)
                            when (object_stack.last()) {
                                is JSONList -> {
                                    (object_stack.last() as JSONList).add(to_add_object)
                                }

                                is JSONString -> {
                                    val key_object = object_stack.removeAt(object_stack.size - 1) as JSONString
                                    if (object_stack.last() is JSONHashMap) {
                                        (object_stack.last() as JSONHashMap)[key_object.value] = to_add_object
                                    }
                                }

                                else -> {}
                            }
                            close_expected = false
                        }

                        Char(125) -> { // "}"
                            val object_index = position_stack.removeAt(position_stack.size - 1)
                            if (object_index == object_stack.size - 1) {
                                // Nothing to be done
                            } else {
                                val map_object = object_stack[object_index]
                                val to_add_object = object_stack.removeAt(object_stack.size - 1)
                                val to_add_key = object_stack.removeAt(object_stack.size - 1)

                                if (map_object is JSONHashMap && to_add_key is JSONString) {
                                    map_object[to_add_key.value] = to_add_object
                                } else {
                                    throw InvalidJSON(json_content, index)
                                }
                            }

                        }

                        Char(93) -> { // "]"
                            val object_index = position_stack.removeAt(position_stack.size - 1)
                            if (object_index == object_stack.size - 1) {
                                // Nothing to be Done
                            } else {
                                val list_object = object_stack[object_index]
                                val to_add_object = object_stack.removeAt(object_stack.size - 1)

                                if (list_object is JSONList) {
                                    list_object.add(to_add_object)
                                } else {
                                    throw InvalidJSON(json_content, index)
                                }
                            }
                        }

                        Char(123) -> { // "{"
                            position_stack.add(object_stack.size)
                            object_stack.add(JSONHashMap())
                        }


                        Char(91) -> { // "["
                            if (close_expected) {
                                throw InvalidJSON(json_content, index)
                            }

                            position_stack.add(object_stack.size)
                            object_stack.add(JSONList())
                        }

                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-' -> {
                            if (close_expected) {
                                throw InvalidJSON(json_content, index)
                            }

                            working_number = "" + working_char
                        }

                        '"' -> {
                            if (close_expected) {
                                throw InvalidJSON(json_content, index)
                            }

                            working_string = ""
                        }

                        ':' -> {
                            if (!close_expected) {
                                throw InvalidJSON(json_content, index)
                            }
                            close_expected = false
                            // TODO: Check Expected char
                        }


                        ' ', '\r', '\n', '\t' -> {}

                        'n' -> {
                            if (close_expected) {
                                throw InvalidJSON(json_content, index)
                            }

                            if (json_content.substring(index, index + 4) != "null") {
                                throw InvalidJSON(json_content, index)
                            } else {
                                object_stack.add(null)
                                index += 3
                            }
                        }

                        'f' -> {
                            if (close_expected) {
                                throw InvalidJSON(json_content, index)
                            }

                            if (json_content.substring(index, index + 5) != "false") {
                                throw InvalidJSON(json_content, index)
                            } else {
                                object_stack.add(JSONBoolean(false))
                                index += 4
                            }
                        }

                        't' -> {
                            if (close_expected) {
                                throw InvalidJSON(json_content, index)
                            }

                            if (json_content.substring(index, index + 4) != "true") {
                                throw InvalidJSON(json_content, index)
                            } else {
                                object_stack.add(JSONBoolean(true))
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

            if (position_stack.isNotEmpty() || object_stack.isEmpty()) {
                throw InvalidJSON(json_content, index)
            }

            val output = object_stack.last()
            return if (output != null) {
                output as T
            } else {
                null
            }
        }
    }
}