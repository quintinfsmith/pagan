package com.qfs.pagan

import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfont2.Preset
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.apres.soundfontplayer.WaveGenerator
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.min

class AudioInterface {
    var sample_rate: Int = 44100
    var soundfont: SoundFont? = null
    var playback_sample_handle_manager: SampleHandleManager? = null
    var playback_device: PlaybackDevice? = null
    var soundfont_supported_instrument_names = HashMap<Pair<Int, Int>, String>()
    var feedback_revolver = FeedbackRevolver(4)

    class FeedbackRevolver(var size: Int = 4) {
        var sample_handle_manager: SampleHandleManager? = null
        private var current_index: Int = 0
        private var device: FeedbackDevice? = null

        fun set_handle_manager(sample_handle_manager: SampleHandleManager) {
            this.clear()
            this.sample_handle_manager = sample_handle_manager
            this.device = FeedbackDevice(sample_handle_manager)
        }

        fun play(channel: Int, note: Int, bend: Int, velocity: Int, duration: Int = 250) {
            val event = NoteOn79(
                index = this.current_index++,
                channel = channel,
                note = note,
                bend = bend,
                velocity = velocity
            )
            this.device?.new_event(event, duration)
            this.current_index %= this.size
        }

        fun clear() {
            this.sample_handle_manager?.destroy()
            this.sample_handle_manager = null
            this.device?.destroy()
            this.device = null
            this.current_index = 0
        }
    }

    fun set_sample_rate(new_rate: Int) {
        val original_rate = this.sample_rate
        this.sample_rate = new_rate
        if (original_rate == new_rate || this.soundfont == null) return
        this.set_soundfont(this.soundfont!!)
    }

    fun has_soundfont(): Boolean {
        return this.soundfont != null
    }

    fun reset() {
        this.soundfont?.let {
            this.set_soundfont(it)
        }
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

        this.playback_device = PlaybackDevice(
            this,
            this.playback_sample_handle_manager!!,
            WaveGenerator.StereoMode.Stereo
        )

        this.connect_feedback_device()
    }



    fun unset_soundfont() {
        this.soundfont?.destroy()
        this.soundfont = null
        this.playback_sample_handle_manager?.destroy()
        this.playback_sample_handle_manager = null
        this.feedback_revolver.clear()
    }

    fun update_channel_instrument(channel: Int, bank: Int, program: Int) {
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
        val buffer_size = this.sample_rate / 4
        this.feedback_revolver.set_handle_manager(
            SampleHandleManager(
                this.soundfont!!,
                this.sample_rate,
                buffer_size - 2 + (if (buffer_size % 2 == 0) {
                    2
                } else {
                    1
                })
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

    fun get_instrument_options(midi_channel: Int): List<Pair<String, Int>>? {
        if (this.soundfont == null) return null
        val preset = this.get_preset(midi_channel) ?: return null

        val available_drum_keys = mutableSetOf<Pair<String, Int>>()
        for ((_, preset_instrument) in preset.instruments) {
            if (preset_instrument.instrument == null) continue
            val instrument_range = preset_instrument.key_range ?: Pair(0, 127)

            for (sample_directive in preset_instrument.instrument!!.sample_directives.values) {
                val key_range = sample_directive.key_range ?: Pair(0, 127)
                val usable_range = max(key_range.first, instrument_range.first)..min(key_range.second, instrument_range.second)

                var name = sample_directive.sample!!.first().name
                if (name.contains("(")) {
                    name = name.substring(0, name.indexOf("("))
                }

                for (key in usable_range) {
                    val use_name = if (usable_range.first != usable_range.last) {
                        "$name - ${(key - usable_range.first) + 1}"
                    } else {
                        name
                    }
                    available_drum_keys.add(Pair(use_name, key))
                }
            }
        }

        return available_drum_keys.sortedBy {
            it.second
        }
    }

}