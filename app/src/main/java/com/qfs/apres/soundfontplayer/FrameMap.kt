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

import com.qfs.pagan.structure.Rational

interface FrameMap {
    companion object {
        const val LAYER_SAMPLE = 0
        const val LAYER_LINE = 1
        const val LAYER_CHANNEL = 2
        const val LAYER_GLOBAL = 3
    }
    /* Get handles that start at frame */
    fun get_new_handles(frame: Int): Set<Pair<SampleHandle, IntArray>>? //handle, merge keys
    /* Get handles that are already playing during frame */
    fun get_active_handles(frame: Int): Set<Pair<Int, Pair<SampleHandle, IntArray>>> // first_frame::(handle::mergekeys)
    //fun get_marked_frames(): Array<Int>
    fun get_marked_frame(i: Int): Int?
    fun has_frames_remaining(frame: Int): Boolean
    fun get_size(): Int
    fun get_effect_buffers(): List<Triple<Int, Int, ProfileBuffer>>
}
