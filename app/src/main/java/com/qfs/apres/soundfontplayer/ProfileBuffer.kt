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

class ProfileBuffer(var ptr: Long, val type: EffectType) {
    // TODO: Memory Management
    constructor(data: ControllerEventData, start_frame: Int = 0): this(
        ProfileBuffer.create(data.ptr, start_frame),
        data.type
    )

    companion object {
        external fun create(data_ptr: Long, start_frame: Int): Long
    }

    external fun allow_empty_jni(ptr: Long): Boolean
    fun allow_empty(): Boolean {
        return this.allow_empty_jni(this.ptr)
    }

    external fun set_frame_jni(ptr: Long, frame: Int)
    fun set_frame(frame: Int) {
        this.set_frame_jni(this.ptr, frame)
    }

    external fun copy_jni(ptr: Long): Long
    fun copy(): ProfileBuffer {
        return ProfileBuffer(this.copy_jni(this.ptr), this.type)
    }

    external fun destroy_jni(ptr: Long, deep: Boolean)
    fun destroy(deep: Boolean = false) {
        if (this.ptr != 0L) {
            this.destroy_jni(this.ptr, deep)
        }

        this.ptr = 0
    }

    external fun get_data_ptr_jni(ptr: Long): Long
    fun get_data(): ControllerEventData {
        return ControllerEventData(this.get_data_ptr_jni(this.ptr), this.type)
    }
}

