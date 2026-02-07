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

data class SMPTEOffset(var hour: Int, var minute: Int, var second: Int, var ff: Int, var fr: Int):
    MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0x54.toByte(),
            0x05.toByte(),
            this.hour.toByte(),
            this.minute.toByte(),
            this.second.toByte(),
            this.ff.toByte(),
            this.fr.toByte()
        )
    }

    fun get_hour(): Int {
        return this.hour
    }
    fun get_minute(): Int {
        return this.minute
    }
    fun get_second(): Int {
        return this.second
    }
    fun get_ff(): Int {
        return this.ff
    }
    fun get_fr(): Int {
        return this.fr
    }
    fun set_hour(hour: Int) {
        this.hour = hour
    }
    fun set_minute(minute: Int) {
        this.minute = minute
    }
    fun set_second(second: Int) {
        this.second = second
    }
    fun set_ff(ff: Int) {
        this.ff = ff
    }
    fun set_fr(fr: Int) {
        this.fr = fr
    }
}