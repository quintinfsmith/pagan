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

data class Sample(
    var name: String,
    var loop_start: Int,
    var loop_end: Int,
    var sample_rate: Int,
    var original_pitch: Int,
    var pitch_correction: Int,
    var sample_type: Int,
    var data: SampleData = SampleData(0),
    var data_placeholder: Pair<Int, Int>
) {
    override fun equals(other: Any?): Boolean {
        if (other !is Sample) {
            return false
        }

        return (
            this.name == other.name
                && this.loop_end == other.loop_end
                && this.loop_start == other.loop_start
                && this.sample_rate == other.sample_rate
                && this.original_pitch == other.original_pitch
                && this.pitch_correction == other.pitch_correction
                && this.sample_type == other.sample_type
                && this.data_placeholder == other.data_placeholder
        )
    }

    fun destroy() {
        this.data.destroy()
    }

    fun set_data(new_data: SampleData) {
        this.data.ptr = new_data.ptr
        this.data.size = new_data.size
    }

}

// JNI Code, needs manual memory management so leave it unused for now
//data class Sample(val ptr: Long) {
//    constructor(
//        name: String,
//        loopStart: Int,
//        loopEnd: Int,
//        sampleRate: Int,
//        originalPitch: Int,
//        pitchCorrection: Int,
//        sampleType: Int,
//        data_placeholder: Pair<Int, Int>,
//    ): this(
//        create(
//            name,
//            loopStart,
//            loopEnd,
//            sampleRate,
//            originalPitch,
//            pitchCorrection,
//            sampleType,
//            data_placeholder.first,
//            data_placeholder.second
//        )
//    )
//
//    companion object {
//        external fun create(
//            name: String,
//            loopStart: Int,
//            loopEnd: Int,
//            sampleRate: Int,
//            originalPitch: Int,
//            pitchCorrection: Int,
//            sampleType: Int,
//            data_placeholder_start: Int,
//            data_placeholder_end: Int
//        ): Long
//    }
//
//    val name: String
//        get() = this.get_name_inner(this.ptr).toString()
//
//    val data_placeholder: Pair<Int, Int>
//        get() = this._get_placeholder()
//
//    val sample_type: Int
//        get() = this.get_sample_type_inner(this.ptr)
//
//    var data: ShortArray
//        get() = this.get_data_inner(this.ptr)
//        set(value) = this.set_data_inner(this.ptr, value)
//
//    val sample_rate: Int
//        get() = this.get_sample_rate_inner(this.ptr)
//
//    val original_pitch: Int
//        get() = this.get_original_pitch(this.ptr)
//
//    val pitch_correction: Int
//        get() = this.get_pitch_correction(this.ptr)
//
//    val loop_start: Int
//        get() = this.get_loop_start(this.ptr)
//
//    val loop_end: Int
//        get() = this.get_loop_end(this.ptr)
//
//    external fun get_original_pitch(ptr: Long): Int
//    external fun get_sample_rate_inner(ptr: Long): Int
//    external fun get_sample_type_inner(ptr: Long): Int
//    external fun set_data_inner(ptr: Long, data: ShortArray)
//    external fun jni_data_placeholders(ptr: Long): IntArray
//    external fun get_name_inner(ptr: Long): String
//    external fun get_data_inner(ptr: Long): ShortArray
//    external fun get_pitch_correction(ptr: Long): Int
//    external fun get_loop_start(ptr: Long): Int
//    external fun get_loop_end(ptr: Long): Int
//
//    fun set_data(data: ShortArray) {
//        this.set_data_inner(this.ptr, data)
//    }
//
//    private fun _get_placeholder(): Pair<Int, Int> {
//        val pair_as_array = this.jni_data_placeholders(this.ptr)
//        return Pair(
//            pair_as_array[0],
//            pair_as_array[1]
//        )
//        //return Pair(
//        //    this.get_placeholder_start(this.ptr),
//        //    this.get_placeholder_end(this.ptr)
//        //)
//    }
//
//    private fun get_data_placholders(ptr: Long) {
//
//    }
//
//}