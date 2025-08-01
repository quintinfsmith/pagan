package com.qfs.json

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

    override fun copy(): JSONInteger {
        return JSONInteger(this.value)
    }

    override fun hashCode(): Int {
        return this.value
    }
}