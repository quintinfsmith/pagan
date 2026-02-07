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

data class SetTempo(var uspqn: Int): MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0x51.toByte(),
            0x03.toByte(),
            ((this.uspqn shr 16) and 0xFF).toByte(),
            ((this.uspqn shr 8) and 0xFF).toByte(),
            (this.uspqn and 0xFF).toByte()
        )
    }
    companion object {
        fun from_bpm(bpm: Float): SetTempo {
            return SetTempo((60000000.toFloat() / bpm).toInt())
        }
    }

    fun get_bpm(): Float {
        val uspqn = this.get_uspqn()
        return if (uspqn > 0) {
            60000000.toFloat() / uspqn
        } else {
            0.toFloat()
        }
    }

    fun get_uspqn(): Int {
        return this.uspqn
    }

    fun set_uspqn(new_uspqn: Int) {
        this.uspqn = new_uspqn
    }

    fun set_bpm(new_bpm: Float) {
        if (new_bpm > 0) {
            this.uspqn = (60000000.toFloat() / new_bpm).toInt()
        } else {
            this.uspqn = 0
        }
    }
}