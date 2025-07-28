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