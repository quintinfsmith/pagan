package com.qfs.json

data class JSONFloat(var value: Float): JSONObject {
    override fun to_string(indent: Int?): String {
        return "${this.value}"
    }
    override fun equals(other: Any?): Boolean {
        return (other is JSONFloat && other.value == this.value)
    }
    override fun toString(): String {
        return this.to_string()
    }

    override fun copy(): JSONObject {
        return JSONFloat(this.value)
    }

    override fun hashCode(): Int {
        return this.value.hashCode()
    }
}