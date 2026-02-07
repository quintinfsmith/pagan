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

class SampleData(var ptr: Long) {
    var size: Int = 0

    external fun set_data_jni(data_array: ShortArray): Long
    fun set_data(data_array: ShortArray) {
        this.size = data_array.size
        this.ptr = this.set_data_jni(data_array)
    }

    fun copy(): SampleData {
        return SampleData(this.ptr)
    }

    external fun destroy_jni(ptr: Long)
    fun destroy() {
        this.destroy_jni(this.ptr)
    }
}