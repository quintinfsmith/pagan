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

abstract class CompoundEvent(
        var channel: Int,
        var value: Int,
        val controller: Int,
        private val controller_lsb: Int = 0x20 + controller // NRN/RN are the only events that arent 0x20 apart. So assume 0x20 unless specified
    ): MIDIEvent {

    override fun as_bytes(): ByteArray {
        val value_msb = (0xFF00 and this.value) shr 8
        val value_lsb = 0x00FF and this.value
        return byteArrayOf(
            (0xB0 or this.channel).toByte(),
            this.controller.toByte(),
            value_msb.toByte(),
            0x00.toByte(),
            (0xB0 or this.channel).toByte(),
            this.controller_lsb.toByte(),
            value_lsb.toByte()
        )
    }

}