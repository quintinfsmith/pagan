/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan

import android.util.Log

const val CH_OPEN = '['
const val CH_CLOSE = ']'
const val CH_NEXT = ','
const val CH_ADD = '+'
const val CH_SUBTRACT = '-'
const val CH_UP = '^'
const val CH_DOWN = 'v'
const val CH_HOLD = '~'

val REL_CHARS = listOf(CH_ADD, CH_SUBTRACT, CH_UP, CH_DOWN, CH_HOLD)
val SPECIAL_CHARS = listOf(CH_OPEN, CH_CLOSE, CH_NEXT, CH_ADD, CH_SUBTRACT, CH_UP, CH_DOWN, CH_HOLD)

// fun to_string(radix: Int, node: OpusTree<InstrumentEvent>, depth: Int = 0): String {
//     var output: String
//     if (node.is_event()) {
//         val opus_event = node.get_event()!!
//         output = if (opus_event.relative) {
//             var new_string: String
//             if (opus_event.note == 0 || opus_event.note % radix != 0) {
//                 new_string = if (opus_event.note < 0) {
//                     CH_SUBTRACT.toString()
//                 } else {
//                     CH_ADD.toString()
//                 }
//                 new_string += get_number_string(abs(opus_event.note), radix, 1)
//             } else {
//                 new_string = if (opus_event.note < 0) {
//                     CH_DOWN.toString()
//                 } else {
//                     CH_UP.toString()
//                 }
//                 new_string += get_number_string(abs(opus_event.note) / radix, radix, 1)
//             }
//             new_string
//         } else {
//             get_number_string(opus_event.note, radix, 2)
//         }
//     } else if (node.is_leaf()) {
//         output = "__"
//     } else {
//         output = ""
//         for (i in 0 until node.size) {
//             val child = node[i]
//             output += to_string(radix, child, depth+1)
//             if (i < node.size - 1) {
//                 output += CH_NEXT
//             }
//         }
//
//         if (node.size > 1 && depth > 0) {
//             output = "$CH_OPEN$output$CH_CLOSE"
//         }
//     }
//
//     return output
// }

fun get_number_string(number: Int, radix: Int, digits: Int): String {
    var output = ""
    var working_number = number
    val selector = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    while (working_number > 0) {
        output = "${selector[working_number % radix]}${output}"
        working_number /= radix
    }

    while (output.length < digits) {
        output = "0${output}"
    }

    return output
}

fun char_to_int(number: Char, radix: Int): Int {
    return str_to_int(number.toString(), radix)
}

fun str_to_int(number: String, radix: Int): Int {
    val selector = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    var output = 0
    for (element in number) {
        output *= radix
        val index = selector.indexOf(element.uppercase())
        output += index
    }
    return output
}

fun tlog(label: String, callback: () -> Unit) {
    val a = System.currentTimeMillis()
    callback()
    Log.d("TLOG", "$label: ${System.currentTimeMillis() - a}")
}

