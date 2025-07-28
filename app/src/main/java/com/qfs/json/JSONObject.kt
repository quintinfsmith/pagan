package com.qfs.json

interface JSONObject {
    fun to_string(): String
    fun copy(): JSONObject
}