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

import com.qfs.apres.get_chord_name_from_mi_sf
import com.qfs.apres.get_mi_sf

data class KeySignature(var key: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val misf = get_mi_sf(this.key)
        return byteArrayOf(
            0xFF.toByte(),
            0x59.toByte(),
            0x02.toByte(),
            misf.second,
            misf.first
        )
    }

    companion object {
        fun from_mi_sf(mi: Byte, sf: Byte): KeySignature {
            val chord_name = get_chord_name_from_mi_sf(mi, sf)
            return KeySignature(chord_name)
        }
    }

    fun get_key(): String {
        return this.key
    }

    fun set_key(key: String) {
        this.key = key
    }
}