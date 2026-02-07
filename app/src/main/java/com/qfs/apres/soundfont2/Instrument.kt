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

import kotlin.math.min

class Instrument(var name: String) {
    var sample_directives = HashMap<Int, SampleDirective>()
    var global_zone: SampleDirective = SampleDirective()

    private val quick_ref_vel = Array<MutableSet<Int>>(128) { mutableSetOf() }
    private val quick_ref_key = Array<MutableSet<Int>>(128) { mutableSetOf() }

    fun set_global_zone(new_global_zone: SampleDirective) {
        this.global_zone = new_global_zone
    }

    fun add_sample(sample_directive: SampleDirective) {
        val uuid = sample_directive.uid

        val key_range = if (sample_directive.key_range == null) {
            0..127
        } else {
            sample_directive.key_range!!.first..min(127,sample_directive.key_range!!.second)
        }

        for (i in key_range) {
            this.quick_ref_key[i].add(uuid)
        }

        val vel_range = if (sample_directive.velocity_range == null) {
            0..127
        } else {
            sample_directive.velocity_range!!.first..min(127, sample_directive.velocity_range!!.second)
        }

        for (i in vel_range) {
            this.quick_ref_vel[i].add(uuid)
        }

        this.sample_directives[uuid] = sample_directive
    }

    fun get_samples(key: Int, velocity: Int): Set<SampleDirective> {
        val output = mutableSetOf<SampleDirective>()
        if (this.sample_directives.isNotEmpty()) {
            val ids = this.quick_ref_vel[velocity].intersect(this.quick_ref_key[key])
            for (id in ids) {
                output.add(this.sample_directives[id]!!)
            }
        } else {
            val key_range = if (this.global_zone.key_range == null) {
                0..127
            } else {
                this.global_zone.key_range!!.first..this.global_zone.key_range!!.second
            }

            val vel_range = if (this.global_zone.velocity_range == null) {
                0..127
            } else {
                this.global_zone.velocity_range!!.first..this.global_zone.velocity_range!!.second
            }
            if (key_range.contains(key) && vel_range.contains(velocity)) {
                output.add(this.global_zone)
            }
        }
        return output
    }
}