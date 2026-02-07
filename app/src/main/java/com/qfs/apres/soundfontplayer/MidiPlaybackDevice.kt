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

import com.qfs.apres.Midi
import com.qfs.apres.soundfont2.SoundFont

class MidiPlaybackDevice(sample_handle_manager: SampleHandleManager): MappedPlaybackDevice(MidiFrameMap(sample_handle_manager), sample_handle_manager.sample_rate, sample_handle_manager.buffer_size) {
    class Builder {
        companion object {
            fun build(soundfont: SoundFont, sample_rate: Int): MidiPlaybackDevice {
                val sample_handle_manager = SampleHandleManager(soundfont, sample_rate)
                return MidiPlaybackDevice(sample_handle_manager)
            }
        }
    }

    fun play_midi(midi: Midi) {
        val frame_map = (this.sample_frame_map as MidiFrameMap)
        frame_map.clear()
        frame_map.parse_midi(midi)
        this.play()
    }

    override fun on_buffer() {
        TODO("Not yet implemented")
    }

    override fun on_buffer_done() {
        TODO("Not yet implemented")
    }

    override fun on_start() {
        TODO("Not yet implemented")
    }

    override fun on_stop() {
        TODO("Not yet implemented")
    }

    override fun on_cancelled() {
        TODO("Not yet implemented")
    }

    override fun on_mark(i: Int) {
        TODO("Not yet implemented")
    }
}
