/*
 * Apres, A Midi & Soundfont library
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.apres.soundfontplayer

import com.qfs.apres.soundfont2.SampleData

class PitchedBuffer(var ptr: Long) {
    constructor(data: SampleData, pitch: Float, known_max: Int? = null, range: IntRange? = null, is_loop: Boolean = false): this(
        PitchedBuffer.create(
            data.ptr,
            pitch,
            range?.first ?: 0,
            range?.last ?: data.size,
            is_loop
        )
    )

    companion object {
        init {
            System.loadLibrary("pagan")
        }
        external fun create(data_ptr: Long, pitch: Float, start: Int, end: Int, is_loop: Boolean): Long
    }

    class PitchedBufferOverflow : Exception()

    val size: Int
        get() = this.get_virtual_size(this.ptr)

    val position: Int
        get() = this.get_virtual_position(this.ptr)

    external fun get_range_inner(ptr: Long, output: IntArray)
    external fun get_virtual_size(ptr: Long): Int
    external fun is_overflowing_inner(ptr: Long): Boolean
    external fun is_loop(ptr: Long): Boolean
    external fun repitch_inner(ptr: Long, new_pitch_adjustment: Float)
    external fun get_virtual_position(ptr: Long): Int
    external fun set_virtual_position(ptr: Long, new_position: Int)
    external fun get_inner(ptr: Long): Float
    external fun copy_inner(ptr: Long): Long
    external fun free(ptr: Long)

    fun destroy() {
        this.free(this.ptr)
    }

    fun get_range(): IntRange {
        var array = IntArray(2) { 0 }
        this.get_range_inner(this.ptr, array)
        // TODO: Double check '..' or 'until'
        return array[0] .. array[1]
    }

    fun is_overflowing(): Boolean {
        return this.is_overflowing_inner(this.ptr)
    }

    fun repitch(new_pitch_adjustment: Float) {
        this.repitch_inner(this.ptr, new_pitch_adjustment)
    }

    fun reset_pitch() {
        this.repitch(1F)
    }

    fun set_position(value: Int) {
        this.set_virtual_position(this.ptr, value)
    }

    fun get(): Float {
        return this.get_inner(this.ptr)
    }

    fun copy(): PitchedBuffer {
        return PitchedBuffer(this.copy_inner(this.ptr))
    }
}