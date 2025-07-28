package com.qfs.json

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

    fun remove_at(index: Int) {
        this.list.removeAt(index)
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

    override fun copy(): JSONList {
        return JSONList(*Array(this.size) { i: Int ->
            this.list[i]?.copy()
        })
    }
    fun <R: Comparable<R>> sort_by(sortfunc: (JSONObject?) -> R) {
        this.list.sortBy(sortfunc)
    }
}