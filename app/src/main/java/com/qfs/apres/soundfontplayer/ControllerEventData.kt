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

class ControllerEventData(val ptr: Long, val type: EffectType) {
    class IndexedProfileBufferFrame(val first_frame: Int, val last_frame: Int, val value: FloatArray, val increment: FloatArray)

    constructor(size: Int, frames: List<IndexedProfileBufferFrame>, type: EffectType): this(
        ControllerEventData.intermediary_create(size, frames, type), type
    )

    companion object {
        fun intermediary_create(size: Int, frames: List<IndexedProfileBufferFrame>, type: EffectType): Long {
            val value_width = frames[0].value.size
            val values = FloatArray(frames.size * value_width)
            val increments = FloatArray(frames.size * value_width)

            for (i in 0 until frames.size) {
                for (j in 0 until value_width) {
                    values[(i * value_width) + j] = frames[i].value[j]
                    increments[(i * value_width) + j] = frames[i].increment[j]
                }
            }

            return this.create(
                size,
                IntArray(frames.size) { i: Int -> frames[i].first_frame },
                IntArray(frames.size) { i: Int -> frames[i].last_frame },
                value_width,
                values,
                increments,
                type.i
            )
        }

        external fun create(size: Int, frame_indices: IntArray, frame_end_indices: IntArray, value_width: Int, values: FloatArray, increments: FloatArray, type: Int): Long
    }

    external fun destroy_jni(ptr: Long)
    fun destroy() {
        this.destroy_jni(this.ptr)
    }
}