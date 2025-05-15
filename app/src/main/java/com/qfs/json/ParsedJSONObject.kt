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
class NonNullableException : Exception("Attempting to access non-nullable value which is null")
class InvalidJSONObject(obj: Any): Exception("Not a valid JSON Object $obj")

interface JSONEncodeable {
    fun to_json(): JSONObject
}

interface JSONObject {
    fun to_string(): String
}

data class JSONString(var value: String): JSONObject {
    override fun to_string(): String {
        val escaped_string = this.value.replace("\"", "\\\"")
        return "\"$escaped_string\""
    }
    override fun equals(other: Any?): Boolean {
        return (other is JSONString && other.value == this.value)
    }

    override fun toString(): String {
        return this.to_string()
    }
}

data class JSONFloat(var value: Float): JSONObject {
    override fun to_string(): String {
        return "${this.value}"
    }
    override fun equals(other: Any?): Boolean {
        return (other is JSONFloat && other.value == this.value)
    }
    override fun toString(): String {
        return this.to_string()
    }
}

data class JSONInteger(var value: Int): JSONObject {
    override fun to_string(): String {
        return "${this.value}"
    }
    override fun equals(other: Any?): Boolean {
        return (other is JSONInteger && other.value == this.value)
    }
    override fun toString(): String {
        return this.to_string()
    }
}
data class JSONBoolean(var value: Boolean): JSONObject {
    override fun to_string(): String {
        return this.value.toString()
    }

    override fun equals(other: Any?): Boolean {
        return (other is JSONBoolean && other.value == this.value)
    }
    override fun toString(): String {
        return this.to_string()
    }
}

class JSONHashMap(vararg args: Pair<String, Any?>): JSONObject {
    private val hash_map = HashMap<String, JSONObject?>()
    val keys: Set<String>
        get() = this.hash_map.keys

    init {
        for (arg in args) {
            when (arg.second) {
                null -> this[arg.first] = null
                is Boolean -> this[arg.first] = arg.second as Boolean
                is Int -> this[arg.first] = arg.second as Int
                is Float -> this[arg.first] = arg.second as Float
                is String -> this[arg.first] = arg.second as String
                is JSONEncodeable -> this[arg.first] = (arg.second as JSONEncodeable)
                is JSONObject -> this[arg.first] = arg.second as JSONObject
                else -> throw InvalidJSONObject(arg.second!!)
            }
        }
    }

    operator fun get(key: String): JSONObject? {
        return this.hash_map[key]
    }

    operator fun set(key: String, n: Nothing?) {
        this.hash_map[key] = null
    }

    operator fun set(key: String, value: JSONEncodeable) {
        this.hash_map[key] = value.to_json()
    }

    operator fun set(key: String, value: JSONObject?) {
        this.hash_map[key] = value
    }
    operator fun set(key: String, value: Int?) {
        if (value == null) {
            return this.set_null(key)
        }
        this[key] = JSONInteger(value)
    }
    operator fun set(key: String, value: String?) {
        if (value == null) {
            return this.set_null(key)
        }
        this.hash_map[key] = JSONString(value)
    }
    operator fun set(key: String, value: Float?) {
        if (value == null) {
            return this.set_null(key)
        }
        this.hash_map[key] = JSONFloat(value)
    }
    operator fun set(key: String, value: Boolean?) {
        if (value == null) {
            return this.set_null(key)
        }
        this.hash_map[key] = JSONBoolean(value)
    }
    fun set_null(key: String) {
        this.hash_map[key] = null
    }
    fun get_int(key: String, default: Int): Int {
        if (this.hash_map[key] == null) {
            return default
        }
        return (this.hash_map[key] as JSONInteger).value
    }
    fun get_string(key: String, default: String): String {
        if (this.hash_map[key] == null) {
            return default
        }
        return (this.hash_map[key] as JSONString).value
    }
    fun get_float(key: String, default: Float): Float {
        if (this.hash_map[key] == null) {
            return default
        }
        return (this.hash_map[key] as JSONFloat).value
    }
    fun get_boolean(key: String, default: Boolean): Boolean {
        if (this.hash_map[key] == null) {
            return default
        }
        return (this.hash_map[key] as JSONBoolean).value
    }
    fun get_intn(key: String): Int? {
        if (this.hash_map[key] == null) {
            return null
        }
        return (this.hash_map[key] as JSONInteger).value
    }
    fun get_stringn(key: String): String? {
        if (this.hash_map[key] == null) {
            return null
        }
        return (this.hash_map[key] as JSONString).value
    }
    fun get_floatn(key: String): Float? {
        if (this.hash_map[key] == null) {
            return null
        }
        return (this.hash_map[key] as JSONFloat).value
    }
    fun get_booleann(key: String): Boolean? {
        if (this.hash_map[key] == null) {
            return null
        }
        return (this.hash_map[key] as JSONBoolean).value
    }

    fun get_hashmapn(key: String): JSONHashMap? {
        if (this.hash_map[key] == null) {
            return null
        }
        return (this.hash_map[key] as JSONHashMap)
    }

    fun get_listn(key: String): JSONList? {
        if (this.hash_map[key] == null) {
            return null
        }
        return (this.hash_map[key] as JSONList)
    }

    fun get_int(key: String): Int {
        if (this.hash_map[key] == null) {
            throw NonNullableException()
        }
        return (this.hash_map[key] as JSONInteger).value
    }
    fun get_string(key: String): String {
        if (this.hash_map[key] == null) {
            throw NonNullableException()
        }
        return (this.hash_map[key] as JSONString).value
    }
    fun get_float(key: String): Float {
        if (this.hash_map[key] == null) {
            throw NonNullableException()
        }
        return (this.hash_map[key] as JSONFloat).value
    }
    fun get_boolean(key: String): Boolean {
        if (this.hash_map[key] == null) {
            throw NonNullableException()
        }
        return (this.hash_map[key] as JSONBoolean).value
    }
    fun get_hashmap(key: String): JSONHashMap {
        if (this.hash_map[key] == null) {
            throw NonNullableException()
        }
        return (this.hash_map[key] as JSONHashMap)
    }

    fun get_list(key: String): JSONList {
        if (this.hash_map[key] == null) {
            throw NonNullableException()
        }
        return (this.hash_map[key] as JSONList)
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

    override fun equals(other: Any?): Boolean {
        if (other !is JSONHashMap || other.keys != this.keys) {
            return false
        }

        for (key in this.keys) {
            if (other[key] != this[key]) {
                return false
            }
        }

        return true
    }
    override fun toString(): String {
        return this.to_string()
    }

    fun isNotEmpty(): Boolean {
        return this.hash_map.isNotEmpty()
    }

    fun isEmpty(): Boolean {
        return this.hash_map.isEmpty()
    }

}

class JSONList(vararg args: JSONObject?): JSONObject {
    constructor(size: Int, callback: (Int) -> JSONObject?): this(*Array(size) { i: Int -> callback(i) })
    private val list = args.toMutableList()

    val size: Int
        get() = this.list.size
    val indices: IntRange
        get() = this.list.indices

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
    fun forEachIndexed(callback: (Int, JSONObject?) -> Unit) {
        this.list.forEachIndexed(callback)
    }

    fun add(value: JSONObject?) {
        this.list.add(value)
    }
    fun add(value: Int?) {
        if (value == null) {
            return this.add_null()
        }
        this.list.add(JSONInteger(value))
    }
    fun add(value: String?) {
        if (value == null) {
            return this.add_null()
        }
        this.list.add(JSONString(value))
    }
    fun add(value: Float?) {
        if (value == null) {
            return this.add_null()
        }
        this.list.add(JSONFloat(value))
    }
    fun add(value: Boolean?) {
        if (value == null) {
            return this.add_null()
        }
        this.list.add(JSONBoolean(value))
    }
    fun add_null() {
        this.list.add(null)
    }
    operator fun set(index: Int, value: JSONObject?) {
        this.list[index] = value
    }
    operator fun set(index: Int, value: Int?) {
        if (value == null) {
            return this.set_null(index)
        }
        this.list[index] = JSONInteger(value)
    }
    operator fun set(index: Int, value: String?) {
        if (value == null) {
            return this.set_null(index)
        }
        this.list[index] = JSONString(value)
    }
    operator fun set(index: Int, value: Float?) {
        if (value == null) {
            return this.set_null(index)
        }
        this.list[index] = JSONFloat(value)
    }
    operator fun set(index: Int, value: Boolean?) {
        if (value == null) {
            return this.set_null(index)
        }
        this.list[index] = JSONBoolean(value)
    }
    fun set_null(index: Int) {
        this.list[index] = null
    }

    fun get_int(index: Int, default: Int): Int {
        if (this.list[index] == null) {
            return default
        }
        return (this.list[index] as JSONInteger).value
    }
    fun get_string(index: Int, default: String): String {
        if (this.list[index] == null) {
            return default
        }
        return (this.list[index] as JSONString).value
    }
    fun get_float(index: Int, default: Float): Float {
        if (this.list[index] == null) {
            return default
        }
        return (this.list[index] as JSONFloat).value
    }
    fun get_boolean(index: Int, default: Boolean): Boolean {
        if (this.list[index] == null) {
            return default
        }
        return (this.list[index] as JSONBoolean).value
    }

    fun get_intn(index: Int): Int? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as JSONInteger).value
    }
    fun get_stringn(index: Int): String? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as JSONString).value
    }
    fun get_floatn(index: Int): Float? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as JSONFloat).value
    }
    fun get_booleann(index: Int): Boolean? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as JSONBoolean).value
    }
    fun get_listn(index: Int): JSONList? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as JSONList)
    }
    fun get_hashmapn(index: Int): JSONHashMap? {
        if (this.list[index] == null) {
            return null
        }
        return (this.list[index] as JSONHashMap)
    }

    fun get_int(index: Int): Int {
        if (this.list[index] == null) {
            throw NonNullableException()
        }
        return (this.list[index] as JSONInteger).value
    }
    fun get_string(index: Int): String {
        if (this.list[index] == null) {
            throw NonNullableException()
        }
        return (this.list[index] as JSONString).value
    }
    fun get_float(index: Int): Float {
        if (this.list[index] == null) {
            throw NonNullableException()
        }
        return (this.list[index] as JSONFloat).value
    }
    fun get_boolean(index: Int): Boolean {
        if (this.list[index] == null) {
            throw NonNullableException()
        }
        return (this.list[index] as JSONBoolean).value
    }

    fun get_hashmap(index: Int): JSONHashMap {
        if (this.list[index] == null) {
            throw NonNullableException()
        }
        return (this.list[index] as JSONHashMap)
    }

    fun get_list(index: Int): JSONList {
        if (this.list[index] == null) {
            throw NonNullableException()
        }
        return (this.list[index] as JSONList)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is JSONList || other.size != this.size) {
            return false
        }

        for (i in this.list.indices) {
            if (other.list[i] != this.list[i]) {
                return false
            }
        }

        return true
    }
    override fun toString(): String {
        return this.to_string()
    }

    operator fun iterator(): Iterator<JSONObject?> {
        return this.list.iterator()
    }

    operator fun get(i: Int): JSONObject? {
        return this.list[i]
    }

    fun isNotEmpty(): Boolean {
        return this.list.isNotEmpty()
    }

    fun isEmpty(): Boolean {
        return this.list.isEmpty()
    }
}

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
                            working_string += working_char
                            string_escape_flagged = false
                        } else {
                            when (working_char) {
                                '\\' -> {
                                    string_escape_flagged = true
                                }

                                '"' -> {
                                    val new_string_item = JSONString(working_string)
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
            //} catch (e: NoSuchElementException) {
            //    throw InvalidJSON(json_content, index)
            //}


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