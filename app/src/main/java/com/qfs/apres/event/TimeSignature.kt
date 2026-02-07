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

data class TimeSignature(var numerator: Int, var denominator: Int, var clocks_per_metronome: Int, var thirtysecondths_per_quarter: Int):
    MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0x58.toByte(),
            0x04.toByte(),
            this.numerator.toByte(),
            this.denominator.toByte(),
            this.clocks_per_metronome.toByte(),
            this.thirtysecondths_per_quarter.toByte()
        )
    }
    fun get_numerator(): Int {
        return this.numerator
    }

    fun get_denominator(): Int {
        return this.denominator
    }

    fun get_clocks_per_metronome(): Int {
        return this.clocks_per_metronome
    }

    fun get_thirtysecondths_per_quarter_note(): Int {
        return this.thirtysecondths_per_quarter
    }

    fun set_numerator(new_value: Int) {
        this.numerator = new_value
    }
    fun set_denominator(new_value: Int) {
        this.denominator = new_value
    }
    fun set_clocks_per_metronome(new_value: Int) {
        this.clocks_per_metronome = new_value
    }
    fun set_thirtysecondths_per_quarter_note(new_value: Int) {
        this.thirtysecondths_per_quarter = new_value
    }
}