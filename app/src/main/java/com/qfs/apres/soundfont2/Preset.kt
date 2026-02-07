/*
 * Apres, A Midi & Soundfont library
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.apres.soundfont2

class Preset(
    var name: String = "",
    var preset: Int = 0, // MIDI Preset Number
    var bank: Int = 0, // MIDI Bank Number
    // dwLibrary, dwGenre, dwMorphology don't do anything yet
) {

    companion object {
        var next_uid: Int = 0
        fun gen_uid(): Int {
            return this.next_uid++
        }
    }
    var instruments = HashMap<Int, InstrumentDirective>()
    var global_zone = InstrumentDirective()
    private val quick_instrument_ref_vel = Array<MutableSet<Int>>(128) { mutableSetOf() }
    private val quick_instrument_ref_key = Array<MutableSet<Int>>(128) { mutableSetOf() }

    val uid = Preset.gen_uid()

    fun set_global_zone(new_global_zone: InstrumentDirective) {
        this.global_zone = new_global_zone
    }

    fun add_instrument(pinstrument: InstrumentDirective) {
        val uuid = pinstrument.uid
        val key_range = if (pinstrument.key_range == null) {
            0..127
        } else {
            pinstrument.key_range!!.first ..pinstrument.key_range!!.second
        }
        for (i in key_range) {
            this.quick_instrument_ref_key[i].add(uuid)
        }
        val vel_range = if (pinstrument.velocity_range == null) {
            0..127
        } else {
            pinstrument.velocity_range!!.first ..pinstrument.velocity_range!!.second
        }

        for (i in vel_range) {
            this.quick_instrument_ref_vel[i].add(uuid)
        }

        this.instruments[uuid] = pinstrument
    }

    fun get_instruments(key: Int, velocity: Int): Set<InstrumentDirective> {
        val ids = this.quick_instrument_ref_vel[velocity].intersect(this.quick_instrument_ref_key[key])
        val output = mutableSetOf<InstrumentDirective>()
        for (id in ids) {
            output.add(this.instruments[id]!!)
        }
        return output
    }
}
