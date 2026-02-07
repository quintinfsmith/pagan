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

open class ChannelVoiceMessage(
    var status: Int,
    var channel: Int,
    var data: Array<Int>): MIDIEvent {

    override fun as_bytes(): ByteArray {
        val output = mutableListOf<Byte>(
            (this.status or this.channel).toByte()
        )
        for (value in this.data) {
            output.add(value.toByte())
        }

        return output.toByteArray()
    }

    override fun as_ump_bytes(): ByteArray {
        val output = ByteArray(1) { 0x20 } + this.as_bytes()
        TODO()
    }

    fun get_channel(): Int {
        return this.channel
    }

    fun set_channel(channel: Int) {
        this.channel = channel
    }

    fun set_data(index: Int, value: Int) {
        this.data[index] = value
    }

    fun get_data(index: Int): Int {
        return this.data[index]
    }
}
