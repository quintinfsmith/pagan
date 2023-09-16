package com.qfs.pagan

import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class Configuration(
    var soundfont: String? = null,
    var relative_mode: Boolean = true
) {
    companion object {
        fun from_path(path: String): Configuration {
            val file = File(path)
            return if (file.exists()) {
                val json_content = file.readText(Charsets.UTF_8)
                Json.decodeFromString(json_content)
            } else {
                Configuration()
            }
        }
    }

    fun save(path: String) {
        val file = File(path)
        val json_string = Json.encodeToString(this)
        file.writeText(json_string)
    }
}