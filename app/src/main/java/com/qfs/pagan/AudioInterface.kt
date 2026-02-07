/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan

import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfont2.Preset
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleManager
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

class AudioInterface {
    var sample_rate: Int = 44100
    var soundfont: SoundFont? = null
    var playback_sample_handle_manager: SampleHandleManager? = null
    var soundfont_supported_instrument_names = HashMap<Pair<Int, Int>, String>()
    var feedback_revolver = FeedbackRevolver(4)
    var minimum_instrument_index_cache = HashMap<Pair<Int, Int>, Int>() // <Program, Bank>: index

    class FeedbackRevolver(var size: Int = 4) {
        var sample_handle_manager: SampleHandleManager? = null
        private var current_index: Int = 0
        private var devices =  Array<FeedbackDevice?>(this.size) { null }

        fun set_handle_manager(sample_handle_manager: SampleHandleManager) {
            this.clear()
            this.sample_handle_manager = sample_handle_manager
            for (i in 0 until this.size) {
                this.devices[i] = FeedbackDevice(sample_handle_manager)
            }
        }

        fun play(channel: Int, note: Int, bend: Int, velocity: Int, duration: Int = 250) {
            val event = NoteOn79(
                index = this.current_index++,
                channel = channel,
                note = note,
                bend = bend,
                velocity = velocity
            )


            this.devices[this.current_index]?.let {
                thread {
                    it.new_event(event, duration)
                }
            }
            this.current_index = (this.current_index + 1) % this.size
        }

        fun clear() {
            this.sample_handle_manager?.destroy()
            this.sample_handle_manager = null
            for (i in 0 until this.size) {
                this.devices[i]?.destroy()
                this.devices[i] = null
            }
            this.current_index = 0
        }
    }

    fun set_sample_rate(new_rate: Int) {
        if (this.sample_rate == new_rate) return
        this.sample_rate = new_rate
        this.soundfont?.let { this.set_soundfont(it) }
    }

    fun has_soundfont(): Boolean {
        return this.soundfont != null
    }

    fun reset() {
        this.soundfont?.let { this.set_soundfont(it) }
    }

    fun set_soundfont(soundfont: SoundFont) {
        this.unset_soundfont()
        this.soundfont = soundfont

        this.playback_sample_handle_manager = SampleHandleManager(
            soundfont,
            this.sample_rate,
            this.sample_rate, // Use Large buffer
            ignore_lfo = true
        )

        this.connect_feedback_device()
    }



    fun unset_soundfont() {
        this.soundfont?.destroy()
        this.soundfont = null
        this.playback_sample_handle_manager?.destroy()
        this.playback_sample_handle_manager = null
        this.feedback_revolver.clear()
        this.minimum_instrument_index_cache.clear()
    }

    fun update_channel_preset(channel: Int, bank: Int, program: Int) {
        this.feedback_revolver.sample_handle_manager?.let {
            it.select_bank(channel, bank)
            it.change_program(channel, program)
        }

        // Don't need to update anything but percussion in the sample_handle_manager
        this.playback_sample_handle_manager?.let {
            it.select_bank(channel, bank)
            it.change_program(channel, program)
        }
    }

    fun connect_feedback_device() {
        val buffer_size = this.sample_rate / 40
        this.feedback_revolver.set_handle_manager(
            SampleHandleManager(
                this.soundfont!!,
                this.sample_rate,
                buffer_size - 2 + (if (buffer_size % 2 == 0) { 2 } else { 1 })
            )
        )
    }
    fun disconnect_feedback_device() {
        this.feedback_revolver.clear()
    }

    fun play_feedback(channel: Int, note: Int, bend: Int, velocity: Int) {
        this.feedback_revolver.play(channel, note, bend, velocity, 250)
    }

    fun get_preset(channel: Int): Preset? {
        return this.playback_sample_handle_manager?.get_preset(channel)
    }
    fun get_preset(key: Pair<Int, Int>): Preset? {
        return this.playback_sample_handle_manager?.get_preset(key)
    }


    fun get_minimum_instrument_index(instrument: Pair<Int, Int>): Int {
        val preset = this.playback_sample_handle_manager?.get_preset(instrument) ?: return 0

        if (!this.minimum_instrument_index_cache.contains(instrument)) {
            var min_key = 999
            for ((_, preset_instrument) in preset.instruments) {
                if (preset_instrument.instrument == null) continue
                val instrument_range = preset_instrument.key_range ?: Pair(0, 127)

                for (sample_directive in preset_instrument.instrument!!.sample_directives.values) {
                    val key_range = sample_directive.key_range ?: Pair(0, 127)
                    val usable_range = max(key_range.first, instrument_range.first)..min(key_range.second, instrument_range.second)
                    for (key in usable_range) {
                        min_key = min(key, min_key)
                    }
                }
                this.minimum_instrument_index_cache[instrument] = min_key
            }
        }

        return this.minimum_instrument_index_cache[instrument] ?: 0
    }

}