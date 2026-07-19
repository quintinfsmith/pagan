package com.qfs.json

import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class JSONList(vararg args: JSONObject?): JSONObject {
    constructor(size: Int, callback: (Int) -> JSONObject?): this(*Array(size) { i: Int -> callback(i) })
    private val list = args.toMutableList()

    val size: Int
        get() = this.list.size
    val indices: IntRange
        get() = this.list.indices

    operator fun iterator(): Iterator<JSONObject?> {
        return this.list.iterator()
    }
    operator fun get(i: Int): JSONObject? {
        return this.list[i]
    }
    operator fun set(index: Int, nothing: Nothing?) {
        this.list[index] = null
    }
    operator fun set(index: Int, value: JSONObject?) {
        this.list[index] = value
    }
    operator fun set(index: Int, value: Int?) {
        this.list[index] = value?.let { JSONInteger(it) }
    }
    operator fun set(index: Int, value: String?) {
        this.list[index] = value?.let { JSONString(it) }
    }
    operator fun set(index: Int, value: Float?) {
        this.list[index] = value?.let { JSONFloat(it) }
    }
    operator fun set(index: Int, value: Boolean?) {
        this.list[index] = value?.let { JSONBoolean(it) }
    }
    operator fun set(index: Int, value: JSONCompliant?) {
        this.list[index] = value?.let { value.to_json() }
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

    override fun copy(): JSONList {
        return JSONList(*Array(this.size) { i: Int ->
            this.list[i]?.copy()
        })
    }
    override fun toString(): String {
        return this.to_string()
    }

    override fun to_string(indent: Int?): String {
        val lines = mutableListOf<String>()
        for (value in this.list) {
            lines.add(value?.to_string(indent) ?: "null")
        }

        return if (indent == null) {
            "[ ${lines.joinToString(", ")} ]"
        } else {
            val indent_string = " ".repeat(indent)
            for (i in 0 until lines.size) {
                val sublines = lines[i].split("\n")
                lines[i] = sublines.joinToString("\n$indent_string")
            }
            "[\n$indent_string" + lines.joinToString(",\n$indent_string") + "\n]"
        }
    }

    fun forEachIndexed(callback: (Int, JSONObject?) -> Unit) {
        this.list.forEachIndexed(callback)
    }

    fun add(value: JSONObject?) {
        this.list.add(value)
    }
    fun add(value: Int?) {
        this.add(value?.let { JSONInteger(it) })
    }
    fun add(value: String?) {
        this.add(value?.let { JSONString(it) })
    }
    fun add(value: Float?) {
        this.add(value?.let { JSONFloat(it) })
    }
    fun add(value: Boolean?) {
        this.add(value?.let { JSONBoolean(it) })
    }

    fun remove_at(index: Int): JSONObject? {
        return this.list.removeAt(index)
    }

    fun get_intn(index: Int): Int? {
        return this.list[index]?.let { (it as JSONInteger).value }
    }
    fun get_int(index: Int): Int {
        return this.get_intn(index) ?: throw NonNullableException()
    }

    fun get_stringn(index: Int): String? {
        return this.list[index]?.let { (it as JSONString).value }
    }
    fun get_string(index: Int): String {
        return this.get_stringn(index) ?: throw NonNullableException()
    }

    fun get_floatn(index: Int): Float? {
        return this.list[index]?.let { (it as JSONFloat).value }
    }
    fun get_float(index: Int): Float {
        return this.get_floatn(index) ?: throw NonNullableException()
    }

    fun get_booleann(index: Int): Boolean? {
        return this.list[index]?.let { (it as JSONBoolean).value }
    }
    fun get_boolean(index: Int): Boolean {
        return this.get_booleann(index) ?: throw NonNullableException()
    }

    fun get_listn(index: Int): JSONList? {
        return this.list[index]?.let { (it as JSONList) }
    }
    fun get_list(index: Int): JSONList {
        return this.get_listn(index) ?: throw NonNullableException()
    }

    fun get_hashmapn(index: Int): JSONHashMap? {
        return this.list[index]?.let { (it as JSONHashMap) }
    }
    fun get_hashmap(index: Int): JSONHashMap {
        return this.get_hashmapn(index) ?: throw NonNullableException()
    }

    fun clear() {
        this.list.clear()
    }
    fun isNotEmpty(): Boolean {
        return this.list.isNotEmpty()
    }
    fun isEmpty(): Boolean {
        return this.list.isEmpty()
    }

    fun <R: Comparable<R>> sort_by(sortfunc: (JSONObject?) -> R) {
        this.list.sortBy(sortfunc)
    }
}