package com.qfs.pagan.jsoninterfaces

import com.qfs.pagan.structure.opusmanager.base.OpusControlEvent
import com.qfs.pagan.structure.opusmanager.base.activecontroller.ActiveController
import kotlin.math.max
import kotlin.math.min

class UnknownEventTypeException(type_string: String?): Exception("Unknown Event Type $type_string")
class UnknownChannelTypeException(type_string: String?): Exception("Unknown Channel Type $type_string")

class ExpectedCharacterException(char: Char, i: Int, string: String): Exception("Excpected \"$char\": ${if (i > 5) {"..."} else { }}${string.substring(max(0, i - 5) until min(string.length, i + 5))}${if (i < string.length - 5) { "..." } else { "" } }")

class UnknownControllerException(label: String?): Exception("Unknown Controller: \"$label\"")
class UnhandledControllerException(controller: ActiveController<out OpusControlEvent>): Exception("Unhandled Controller: ${controller::class.java.name}")
