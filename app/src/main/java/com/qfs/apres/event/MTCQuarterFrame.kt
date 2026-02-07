/*
 * Apres, A Midi & Soundfont library
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.apres.event

data class MTCQuarterFrame(var time_code: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xF1.toByte(), this.time_code.toByte())
    }

    fun set_time_code(new_value: Int) {
        this.time_code = new_value
    }
    fun get_time_code(): Int {
        return this.time_code
    }
}