/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.jsoninterfaces

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import kotlin.math.max
import kotlin.math.min

class UnknownEventTypeException(type_string: String?): Exception("Unknown Event Type $type_string")
class UnknownChannelTypeException(type_string: String?): Exception("Unknown Channel Type $type_string")

class ExpectedCharacterException(char: Char, i: Int, string: String): Exception("Excpected \"$char\": ${if (i > 5) {"..."} else { }}${string.substring(max(0, i - 5) until min(string.length, i + 5))}${if (i < string.length - 5) { "..." } else { "" } }")

class UnknownControllerException(label: String?): Exception("Unknown Controller: \"$label\"")
class UnhandledControllerException(controller: EffectController<out EffectEvent>): Exception("Unhandled Controller: ${controller::class.java.name}")
