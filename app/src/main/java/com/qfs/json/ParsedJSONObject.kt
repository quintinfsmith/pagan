package com.qfs.json
import kotlin.math.max
import kotlin.math.min

class InvalidJSON(json_string: String, index: Int): Exception("Invalid JSON @ $index In \"${json_string.substring(max(0, index - 20), min(json_string.length, index + 20))}\"")
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
            var top_item: ParsedObject? = null

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
                                val new_number: ParsedObject = if (working_number.contains(".")) {
                                    ParsedFloat(working_number.toFloat())
                                } else {
                                    ParsedInt(working_number.toInt())
                                }

                                if (object_stack.isEmpty()) {
                                    top_item = new_number
                                } else {
                                    when (val parent_item = object_stack.last()) {
                                        is ParsedList -> {
                                            parent_item.list.add(new_number)
                                        }

                                        is ParsedHashMap -> {
                                            parent_item.hash_map[hashmap_key_stack.removeLast().second] = new_number
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
                                val new_string_item = ParsedString(working_string)
                                if (object_stack.isEmpty()) {
                                    top_item = new_string_item
                                } else {
                                    when (val parent_item = object_stack.last()) {
                                        is ParsedList -> {
                                            parent_item.list.add(new_string_item)
                                        }

                                        is ParsedHashMap -> {
                                            if (hashmap_key_stack.isNotEmpty()) {
                                                val (key_depth, hashmap_key) = hashmap_key_stack.last()
                                                if (key_depth == object_stack.size) {
                                                    parent_item.hash_map[hashmap_key] = new_string_item
                                                    hashmap_key_stack.removeLast()
                                                } else {
                                                    hashmap_key_stack.add(Pair(object_stack.size, working_string))
                                                }
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

                        Char(123) -> { // "{"
                            val new_hashmap = ParsedHashMap()
                            if (object_stack.isNotEmpty()) {
                                when (val parent_item = object_stack.last()) {
                                    is ParsedList -> {
                                        parent_item.list.add(new_hashmap)
                                    }

                                    is ParsedHashMap -> {
                                        val (_, hashmap_key) = hashmap_key_stack.removeLast()
                                        parent_item.hash_map[hashmap_key] = new_hashmap
                                    }

                                    else -> {
                                        throw InvalidJSON(json_content, index)
                                    }
                                }
                            }
                            object_stack.add(new_hashmap)
                        }

                        Char(125) -> { // "}"
                            if (object_stack.isNotEmpty() && object_stack.last() is ParsedHashMap) {
                                top_item = object_stack.removeLast()
                            } else {
                                throw InvalidJSON(json_content, index)
                            }
                        }
                        Char(91) -> { // "["
                            val new_list = ParsedList()
                            if (object_stack.isNotEmpty()) {
                                when (val parent_item = object_stack.last()) {
                                    is ParsedList -> {
                                        parent_item.list.add(new_list)
                                    }

                                    is ParsedHashMap -> {
                                        val (_, hashmap_key) = hashmap_key_stack.removeLast()
                                        parent_item.hash_map[hashmap_key] = new_list
                                    }

                                    else -> {
                                        throw InvalidJSON(json_content, index)
                                    }
                                }
                            }
                            object_stack.add(new_list)
                        }
                        Char(93) -> { // "]"
                            if (object_stack.isNotEmpty() && object_stack.last() is ParsedList) {
                                top_item = object_stack.removeLast()
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
                                if (object_stack.isNotEmpty()) {
                                    when (val parent_item = object_stack.last()) {
                                        is ParsedList -> {
                                            parent_item.list.add(null)
                                        }

                                        is ParsedHashMap -> {
                                            val (_, hashmap_key) = hashmap_key_stack.removeLast()
                                            parent_item.hash_map[hashmap_key] = null
                                        }

                                        else -> {
                                            throw InvalidJSON(json_content, index)
                                        }
                                    }
                                } else {
                                    top_item = null
                                }
                                index += 3
                            }
                        }
                        'f' -> {
                            if (json_content.substring(index, index + 5) != "false") {
                                throw InvalidJSON(json_content, index)
                            } else {
                                if (object_stack.isNotEmpty()) {
                                    when (val parent_item = object_stack.last()) {
                                        is ParsedList -> {
                                            parent_item.list.add(ParsedBoolean(false))
                                        }
                                        is ParsedHashMap -> {
                                            val (_, hashmap_key) = hashmap_key_stack.removeLast()
                                            parent_item.hash_map[hashmap_key] = ParsedBoolean(false)
                                        }
                                        else -> {
                                            throw InvalidJSON(json_content, index)
                                        }
                                    }
                                } else {
                                    top_item = ParsedBoolean(false)
                                }
                                index += 4
                            }
                        }
                        't' -> {
                            if (json_content.substring(index, index + 4) != "true") {
                                throw InvalidJSON(json_content, index)
                            } else {
                                if (object_stack.isNotEmpty()) {
                                    when (val parent_item = object_stack.last()) {
                                        is ParsedList -> {
                                            parent_item.list.add(ParsedBoolean(true))
                                        }
                                        is ParsedHashMap -> {
                                            val (_, hashmap_key) = hashmap_key_stack.removeLast()
                                            parent_item.hash_map[hashmap_key] = ParsedBoolean(true)
                                        }
                                        else -> {
                                            throw InvalidJSON(json_content, index)
                                        }
                                    }
                                } else {
                                    top_item = ParsedBoolean(true)
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

            return top_item
        }
    }
}
