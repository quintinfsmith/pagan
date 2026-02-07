/*
 * Apres, A Midi & Soundfont library
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.apres

import com.qfs.apres.event.KeySignature
import com.qfs.apres.event.PitchWheelChange
import kotlin.experimental.and

class InvalidMIDIFile(var path: String): Exception("$path is not a valid midi file")

fun dequeue_n(bytelist: MutableList<Byte>, n: Int): Int {
    var output = 0
    for (_i in 0 until n) {
        output *= 256
        val x = toUInt(bytelist.removeAt(0))
        output += x
    }
    return output
}

fun get_variable_length_number(bytes: MutableList<Byte>): Int {
    var output: Int = 0
    while (true) {
        output = output shl 7
        val x = toUInt(bytes.removeAt(0))
        output = output or (x and 0x7F)
        if (x and 0x80 == 0) {
            break
        }
    }
    return output
}

fun to_variable_length_bytes(number: Int): List<Byte> {
    val output: MutableList<Byte> = mutableListOf()
    var first_pass = true
    var working_number = number
    while (working_number > 0 || first_pass) {
        var tmp = working_number and 0x7F
        working_number = working_number shr 7
        if (! first_pass) {
            tmp = tmp or 0x80
        }

        output.add(tmp.toByte())
        first_pass = false
    }
    return output.asReversed()
}

fun get_pitchwheel_value(n: Float): Int {
    val output = if (n < 0) {
        ((1 + n) * (0x2000)).toInt()
    } else if (n > 0) {
        (n * 0x1FFF).toInt() + 0x2000
    } else {
        0x2000
    }
    return output
}

fun build_key_signature(mi: Byte, sf: Byte): KeySignature {
    val chord_name = get_chord_name_from_mi_sf(mi, sf)
    return KeySignature(chord_name)
}

fun build_pitch_wheel_change(channel: Byte, lsb: Byte, msb: Byte): PitchWheelChange {
    return PitchWheelChange(channel.toInt(), PitchWheelChange.from_bytes(msb.toInt(), lsb.toInt()))
}

fun get_mi_sf(chord_name: String): Pair<Byte, Byte> {
    val output: Pair<Byte, Byte> = when (chord_name) {
        "A" -> {
            Pair(0, 3)
        }
        "A#", "Bb" -> {
            Pair(0, 10)
        }
        "B" -> {
            Pair(0, 5)
        }
        "C" -> {
            Pair(0, 0)
        }
        "C#", "Db" -> {
            Pair(0, 7)
        }
        "D" -> {
            Pair(0, 2)
        }
        "D#", "Eb" -> {
            Pair(0, 11)
        }
        "E" -> {
            Pair(0, 4)
        }
        "F" -> {
            Pair(0, 9)
        }
        "F#", "Gb" -> {
            Pair(0, 6)
        }
        "G" -> {
            Pair(0, 1)
        }
        "Am" -> {
            Pair(1, 0)
        }
        "A#m", "Bbm" -> {
            Pair(1, 7)
        }
        "Bm" -> {
            Pair(1, 2)
        }
        "Cm" -> {
            Pair(1, 11)
        }
        "C#m", "Dbm" -> {
            Pair(1, 4)
        }
        "Dm" -> {
            Pair(1, 9)
        }
        "D#m", "Ebm" -> {
            Pair(1, 6)
        }
        "Em" -> {
            Pair(1, 1)
        }
        "Fm" -> {
            Pair(1, 2)
        }
        "F#m", "Gbm" -> {
            Pair(1, 3)
        }
        "Gm" -> {
            Pair(1, 10)
        }
        else -> {
            Pair(0, 0) // Default to C Major
        }
    }
    return output
}

fun get_chord_name_from_mi_sf(mi: Byte, sf: Byte): String {
    val mi_int = mi.toInt()
    val sf_int: Int = if (sf < 0) {
        (sf and 0xFF.toByte()).toInt()
    } else {
        sf.toInt()
    }

    val map: List<List<String>> = listOf(
        listOf(
            "Cb", "Gb", "Db", "Ab",
            "Eb", "Bb", "F",
            "C", "G", "D", "A",
            "E", "B", "F#", "C#"
        ),
        listOf(
            "Abm", "Ebm", "Bbm", "Fm",
            "Cm", "Gm", "Dm",
            "Am", "Em", "Bm", "F#m",
            "C#m", "G#m", "D#m", "A#m"
        )
    )

    return map[mi_int][sf_int + 7]
}

fun toUInt(byte: Byte): Int {
    var new_int = (byte and 0x7F.toByte()).toInt()
    if (byte.toInt() < 0) {
        new_int += 128
    }
    return new_int
}
