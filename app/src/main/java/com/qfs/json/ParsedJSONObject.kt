package com.qfs.json
import kotlin.math.max
import kotlin.math.min

class InvalidJSON(msg: String): Exception(msg) {
    constructor(json_string: String, index: Int): this({
        val start = max(0, index - 20)
        val end = min(json_string.length, index + 20)
        val part_a = "Invalid JSON @ $index In \""
        val part_b = json_string.substring(start, end).replace("\n", " ")
        var output = "\n$part_a$part_b\"\n"
        output += Array(part_a.length + (index - start)) { " " }.joinToString("")
        output += "^".padEnd(end - index)
        output
    }())
}
class NonNullableException(): Exception("Attempting to access non-nullable value which is null")

interface ParsedObject {
    fun to_string(): String
}

data class ParsedString(var value: String): ParsedObject {
    override fun to_string(): String {
        val escaped_string = this.value.replace("\"", "\\\"")
        return "\"$escaped_string\""
    }
}
data class ParsedFloat(var value: Float): ParsedObject {
    override fun to_string(): String {
        return "${this.value}"
    }
}

data class ParsedInt(var value: Int): ParsedObject {
    override fun to_string(): String {
        return "${this.value}"
    }
}
data class ParsedBoolean(var value: Boolean): ParsedObject {
    override fun to_string(): String {
        return this.value.toString()
    }
}

class ParsedHashMap(input_map: HashMap<String, ParsedObject?>? = null): ParsedObject {
    val hash_map = input_map ?: HashMap<String, ParsedObject?>()

    operator fun get(key: String): ParsedObject? {
        return this.hash_map[key]
    }
    operator fun set(key: String, value: ParsedObject?) {
        this.hash_map[key] = value
    }
    operator fun set(key: String, value: Int?) {
        if (value == null) {
            return this.set_null(key)
        }
        this[key] = ParsedInt(value)
    }
    operator fun set(key: String, value: String?) {
        if (value == null) {
            return this.set_null(key)
        }
        this.hash_map[key] = ParsedString(value)
    }
    operator fun set(key: String, value: Float?) {
        if (value == null) {
            return this.set_null(key)
        }
        this.hash_map[key] = ParsedFloat(value)
    }
    operator fun set(key: String, value: Boolean?) {
        if (value == null) {
            return this.set_null(key)
        }
        this.hash_map[key] = ParsedBoolean(value)
    }
    fun set_null(key: String) {
        this.hash_map[key] = null
    }
    fun get_int(key: String, default: Int): Int {
        if (this.hash_map[key] == null) {
            return default
        }
        return (this.hash_map[key] as ParsedInt).value
    }
    fun get_string(key: String, default: String): String {
        if (this.hash_map[key] == null) {
            return default
        }
        return (this.hash_map[key] as ParsedString).value
    }
    fun get_float(key: String, default: Float): Float {
        if (this.hash_map[key] == null) {
            return default
        }
        return (this.hash_map[key] as ParsedFloat).value
    }
    fun get_boolean(key: String, default: Boolean): Boolean {
        if (this.hash_map[key] == null) {
            return default
        }
        return (this.hash_map[key] as ParsedBoolean).value
    }
    fun get_intn(key: String): Int? {
        if (this.hash_map[key] == null) {
            return null
        }
        return (this.hash_map[key] as ParsedInt).value
    }
    fun get_stringn(key: String): String? {
        if (this.hash_map[key] == null) {
            return null
        }
        return (this.hash_map[key] as ParsedString).value
    }
    fun get_floatn(key: String): Float? {
        if (this.hash_map[key] == null) {
            return null
        }
        return (this.hash_map[key] as ParsedFloat).value
    }
    fun get_booleann(key: String): Boolean? {
        if (this.hash_map[key] == null) {
            return null
        }
        return (this.hash_map[key] as ParsedBoolean).value
    }

    fun get_hashmapn(key: String): ParsedHashMap? {
        if (this.hash_map[key] == null) {
            return null
        }
        return (this.hash_map[key] as ParsedHashMap)
    }

    fun get_listn(key: String): ParsedList? {
        if (this.hash_map[key] == null) {
            return null
        }
        return (this.hash_map[key] as ParsedList)
    }

    fun get_int(key: String): Int {
        if (this.hash_map[key] == null) {
            throw NonNullableException()
        }
        return (this.hash_map[key] as ParsedInt).value
    }
    fun get_string(key: String): String {
        if (this.hash_map[key] == null) {
            throw NonNullableException()
        }
        return (this.hash_map[key] as ParsedString).value
    }
    fun get_float(key: String): Float {
        if (this.hash_map[key] == null) {
            throw NonNullableException()
        }
        return (this.hash_map[key] as ParsedFloat).value
    }
    fun get_boolean(key: String): Boolean {
        if (this.hash_map[key] == null) {
            throw NonNullableException()
        }
        return (this.hash_map[key] as ParsedBoolean).value
    }
    fun get_hashmap(key: String): ParsedHashMap {
        if (this.hash_map[key] == null) {
            throw NonNullableException()
        }
        return (this.hash_map[key] as ParsedHashMap)
    }

    fun get_list(key: String): ParsedList {
        if (this.hash_map[key] == null) {
            throw NonNullableException()
        }
        return (this.hash_map[key] as ParsedList)
    }

    override fun to_string(): String {
        var output = "{"
        for ((key, value) in this.hash_map) {
            val escaped_key = key.replace("\"", "\\\"")
            output = "$output\"$escaped_key\": ${value?.to_string() ?: "null"},"
        }

        // remove trailing comma
        if (this.hash_map.isNotEmpty()) {
            output = output.substring(0, output.length - 1)
        }
        output = "$output}"
        return output
    }
}

class ParsedList(input_list: MutableList<ParsedObject?>? = null): ParsedObject {
    val list = input_list ?: mutableListOf<ParsedObject?>()

    override fun to_string(): String {
        var output = "["
        for (value in this.list) {
            output = "$output${value?.to_string() ?: "null"},"
        }

        // remove trailing comma
        if (this.list.isNotEmpty()) {
            output = output.substring(0, output.length - 1)
        }
        output = "$output]"
        return output
    }

    fun add(value: ParsedObject?) {
        this.list.add(value)
    }
    fun add(value: Int?) {
        if (value == null) {
            return this.add_null()
        }
        this.list.add(ParsedInt(value))
    }
    fun add(value: String?) {
        if (value == null) {
            return this.add_null()
        }
        this.list.add(ParsedString(value))
    }
    fun add(value: Float?) {
        if (value == null) {
            return this.add_null()
        }
        this.list.add(ParsedFloat(value))
    }
    fun add(value: Boolean?) {
        if (value == null) {
            return this.add_null()
        }
        this.list.add(ParsedBoolean(value))
    }
    fun add_null() {
        this.list.add(null)
    }
    operator fun set(index: Int, value: ParsedObject?) {
        this.list[index] = value
    }
    operator fun set(index: Int, value: Int?) {
        if (value == null) {
            return this.set_null(index)
        }
        this.list[index] = ParsedInt(value)
    }
    operator fun set(index: Int, value: String?) {
        if (value == null) {
            return this.set_null(index)
        }
        this.list[index] = ParsedString(value)
    }
    operator fun set(index: Int, value: Float?) {
        if (value == null) {
            return this.set_null(index)
        }
        this.list[index] = ParsedFloat(value)
    }
    operator fun set(index: Int, value: Boolean?) {
        if (value == null) {
            return this.set_null(index)
        }
        this.list[index] = ParsedBoolean(value)
    }
    fun set_null(index: Int) {
        this.list[index] = null
    }

    fun get_int(index: Int, default: Int?): Int? {
        if (this.list[index] == null) {
            return default
        }
        return (this.list[index] as ParsedInt).value
    }
    fun get_string(index: Int, default: String?): String? {
        if (this.list[index] == null) {
            return default
        }
        return (this.list[index] as ParsedString).value
    }
    fun get_float(index: Int, default: Float?): Float? {
        if (this.list[index] == null) {
            return default
        }
        return (this.list[index] as ParsedFloat).value
    }
    fun get_boolean(index: Int, default: Boolean?): Boolean? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as ParsedBoolean).value
    }

    fun get_intn(index: Int): Int? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as ParsedInt).value
    }
    fun get_stringn(index: Int): String? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as ParsedString).value
    }
    fun get_floatn(index: Int): Float? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as ParsedFloat).value
    }
    fun get_booleann(index: Int): Boolean? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as ParsedBoolean).value
    }
    fun get_listn(index: Int): ParsedList? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as ParsedList)
    }
    fun get_hashmapn(index: Int): ParsedHashMap? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as ParsedHashMap)
    }

    fun get_int(index: Int): Int {
        if (this.list[index] == null) {
            throw NonNullableException()
        }
        return (this.list[index] as ParsedInt).value
    }
    fun get_string(index: Int): String {
        if (this.list[index] == null) {
            throw NonNullableException()
        }
        return (this.list[index] as ParsedString).value
    }
    fun get_float(index: Int): Float {
        if (this.list[index] == null) {
            throw NonNullableException()
        }
        return (this.list[index] as ParsedFloat).value
    }
    fun get_boolean(index: Int): Boolean {
        if (this.list[index] == null) {
            throw NonNullableException()
        }
        return (this.list[index] as ParsedBoolean).value
    }

    fun get_hashmap(index: Int): ParsedHashMap {
        if (this.list[index] == null) {
            throw NonNullableException()
        }
        return (this.list[index] as ParsedHashMap)
    }

    fun get_list(index: Int): ParsedList {
        if (this.list[index] == null) {
            throw NonNullableException()
        }
        return (this.list[index] as ParsedList)
    }

}

class Parser {
    companion object {
        fun parse(json_content: String): ParsedObject? {
            var working_number: String? = null
            var working_string: String? = null
            var string_escape_flagged = false

            val object_stack = mutableListOf<ParsedObject?>()
            val position_stack = mutableListOf<Int>()
            var index = 0
            var close_expected = false

            // try {
                while (index < json_content.length) {
                    val working_char = json_content[index]
                    if (working_number != null) {
                        when (working_char) {
                            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' -> {
                                working_number += working_char
                            }

                            ' ', '\r', '\n', Char(125), Char(93), ',' -> {
                                try {
                                    val new_number: ParsedObject = if (working_number.contains(".")) {
                                        ParsedFloat(working_number.toFloat())
                                    } else {
                                        ParsedInt(working_number.toInt())
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
                            working_string += working_char
                            string_escape_flagged = false
                        } else {
                            when (working_char) {
                                '\\' -> {
                                    string_escape_flagged = true
                                }

                                '"' -> {
                                    val new_string_item = ParsedString(working_string)
                                    object_stack.add(new_string_item)
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

                                val to_add_object = object_stack.removeLast()
                                when (object_stack.last()) {
                                    is ParsedList -> {
                                        (object_stack.last() as ParsedList).add(to_add_object)
                                    }

                                    is ParsedString -> {
                                        val key_object = object_stack.removeLast() as ParsedString
                                        if (object_stack.last() is ParsedHashMap) {
                                            (object_stack.last() as ParsedHashMap)[key_object.value] = to_add_object
                                        }
                                    }

                                    else -> {}
                                }
                                close_expected = false
                            }

                            Char(125) -> { // "}"
                                val object_index = position_stack.removeLast()
                                if (object_index == object_stack.size - 1) {
                                    // Nothing to be done
                                } else {
                                    val map_object = object_stack[object_index]
                                    val to_add_object = object_stack.removeLast()
                                    val to_add_key = object_stack.removeLast()

                                    if (map_object is ParsedHashMap && to_add_key is ParsedString) {
                                        map_object[to_add_key.value] = to_add_object
                                    } else {
                                        throw InvalidJSON(json_content, index)
                                    }
                                }

                            }

                            Char(93) -> { // "]"
                                val object_index = position_stack.removeLast()
                                if (object_index == object_stack.size - 1) {
                                    // Nothing to be Done
                                } else {
                                    val list_object = object_stack[object_index]
                                    val to_add_object = object_stack.removeLast()

                                    if (list_object is ParsedList) {
                                        list_object.add(to_add_object)
                                    } else {
                                        throw InvalidJSON(json_content, index)
                                    }
                                }
                            }

                            Char(123) -> { // "{"
                                position_stack.add(object_stack.size)
                                object_stack.add(ParsedHashMap())
                            }


                            Char(91) -> { // "["
                                if (close_expected) {
                                    throw InvalidJSON(json_content, index)
                                }

                                position_stack.add(object_stack.size)
                                object_stack.add(ParsedList())
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
                                    object_stack.add(ParsedBoolean(false))
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
                                    object_stack.add(ParsedBoolean(true))
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
            //} catch (e: NoSuchElementException) {
            //    throw InvalidJSON(json_content, index)
            //}


            if (position_stack.isNotEmpty() || object_stack.isEmpty()) {
                throw InvalidJSON(json_content, index)
            }


            return object_stack.last()
        }
    }
}
