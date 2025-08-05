package com.qfs.json

interface JSONObject {
    fun to_string(indent: Int? = null): String
    fun copy(): JSONObject
}