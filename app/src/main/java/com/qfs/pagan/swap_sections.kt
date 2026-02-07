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

inline fun <reified T> MutableList<T>.swap_sections(first_index: Int, first_size: Int, second_index: Int, second_size: Int) {
    if (first_index == second_index) return

    val (lesser, larger) = if (first_index < second_index) {
        Pair(Pair(first_index, first_size), Pair(second_index, second_size))
    } else {
        Pair(Pair(second_index, second_size), Pair(first_index, first_size))
    }

    val larger_items = Array<T>(larger.second) {
        this.removeAt(larger.first)
    }
    val lesser_items = Array<T>(lesser.second) {
        this.removeAt(lesser.first)
    }

    for ((i, elm) in larger_items.enumerate()) {
        this.add(lesser.first + i, elm)
    }
    for ((i, elm) in lesser_items.enumerate()) {
        this.add(larger.first + i + (larger.second - lesser.second), elm)
    }
}