/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2026  Quintin Foster Smith
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.json

import androidx.annotation.NonNull
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
                is JSONObject -> this[arg.first] = arg.second as JSONObject
                is JSONCompliant -> this[arg.first] = (arg.second as JSONCompliant)
                else -> throw InvalidJSONObject(arg.second!!)
            }
        }
    }

    fun contains_key(key: String): Boolean {
        return this.keys.contains(key)
    }

    operator fun get(key: String): JSONObject? {
        return this.hash_map[key]
    }

    operator fun set(key: String, n: Nothing?) {
        this.hash_map[key] = null
    }

    fun remove(key: String): JSONObject? {
        return this.hash_map.remove(key)
    }

    operator fun set(key: String, value: JSONCompliant) {
        this.hash_map[key] = value.to_json()
    }

    operator fun set(key: String, value: JSONObject?) {
        this.hash_map[key] = value
    }
    operator fun set(key: String, value: Int?) {
        this[key] = value?.let { JSONInteger(value) }
    }
    operator fun set(key: String, value: String?) {
        this[key] = value?.let { JSONString(value) }
    }
    operator fun set(key: String, value: Float?) {
        this[key] = value?.let { JSONFloat(value) }
    }
    operator fun set(key: String, value: Boolean?) {
        this[key] = value?.let { JSONBoolean(it) }
    }

    fun get_intn(key: String): Int? {
        return this.hash_map[key]?.let { (it as JSONInteger).value }
    }
    fun get_int(key: String): Int {
        return this.get_intn(key) ?: throw NonNullableException()
    }
    fun get_booleann(key: String): Boolean? {
        return this.hash_map[key]?.let { (it as JSONBoolean).value }
    }
    fun get_boolean(key: String): Boolean {
        return this.get_booleann(key) ?: throw NonNullableException()
    }

    fun get_stringn(key: String): String? {
        return this.hash_map[key]?.let { (it as JSONString).value }
    }
    fun get_string(key: String): String {
        return this.get_stringn(key) ?: throw NonNullableException()
    }

    fun get_floatn(key: String): Float? {
        return this.hash_map[key]?.let { (it as JSONFloat).value }
    }
    fun get_float(key: String): Float {
        return this.get_floatn(key) ?: throw NonNullableException()
    }

    fun get_hashmapn(key: String): JSONHashMap? {
        return this.hash_map[key]?.let { (it as JSONHashMap) }
    }
    fun get_hashmap(key: String): JSONHashMap {
        return this.get_hashmapn(key) ?: throw NonNullableException()
    }

    fun get_listn(key: String): JSONList? {
        return this.hash_map[key]?.let { (it as JSONList) }
    }
    fun get_list(key: String): JSONList {
        return this.get_listn(key) ?: throw NonNullableException()
    }

    override fun to_string(indent: Int?): String {
        val lines = mutableListOf<String>()
        for (key in this.hash_map.keys.sorted()) {
            val value = this.hash_map[key]
            val escaped_key = key.replace("\"", "\\\"")
            lines.add("\"$escaped_key\": ${value?.to_string(indent) ?: "null"}")
        }

        return if (indent == null) {
            "{ ${lines.joinToString(", ")} }"
        } else {
            val indent_string = " ".repeat(indent)
            for (i in lines.indices) {
                val line = lines[i]
                val sublines = line.split("\n")
                lines[i] = sublines.joinToString("\n$indent_string")
            }
            "{\n$indent_string" + lines.joinToString(",\n$indent_string") + "\n}"
        }
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

    fun clear() {
        this.hash_map.clear()
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