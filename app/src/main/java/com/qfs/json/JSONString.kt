package com.qfs.json

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

    override fun copy(): JSONObject {
        return JSONString(this.value)
    }

    override fun hashCode(): Int {
        return this.value.hashCode()
    }
}