package com.qfs.json

interface Deserializable<K> {
    fun from_json(map: JSONHashMap): K
}