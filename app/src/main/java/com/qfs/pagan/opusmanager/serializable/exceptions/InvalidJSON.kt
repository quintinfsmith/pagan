package com.qfs.pagan.opusmanager.serializable.exceptions
import kotlin.math.max
import kotlin.math.min
class InvalidJSON(json_string: String, index: Int): Exception("Invalid JSON @ $index In \"${json_string.substring(max(0, index - 20), min(json_string.length, index + 20))}\"")
