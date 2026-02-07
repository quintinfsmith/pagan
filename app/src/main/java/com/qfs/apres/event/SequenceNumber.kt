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

data class SequenceNumber(var sequence: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0x00.toByte(),
            0x02.toByte(),
            ((this.sequence shr 8) and 0xFF).toByte(),
            (this.sequence and 0xFF).toByte()
        )
    }

    fun get_sequence(): Int {
        return this.sequence
    }

    fun set_sequence(new_sequence: Int) {
        this.sequence = new_sequence
    }
}