package com.qfs.json

import kotlin.collections.iterator

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

    override fun copy(): JSONHashMap {
        val output = JSONHashMap()
        for ((key, value) in this.hash_map) {
            output.hash_map[key] = value?.copy()
        }
        return output
    }

    override fun hashCode(): Int {
        var result = this.hash_map.hashCode()
        result = 31 * result + this.keys.hashCode()
        return result
    }
}