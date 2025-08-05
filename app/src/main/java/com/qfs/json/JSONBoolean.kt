package com.qfs.json

data class JSONBoolean(var value: Boolean): JSONObject {
    override fun to_string(indent: Int?): String {
        return this.value.toString()
    }

    override fun equals(other: Any?): Boolean {
        return (other is JSONBoolean && other.value == this.value)
    }
    override fun toString(): String {
        return this.to_string()
    }

    override fun copy(): JSONBoolean {
        return JSONBoolean(this.value)
    }

    override fun hashCode(): Int {
        return this.value.hashCode()
    }
}