package com.qfs.pagan

import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.apres.soundfontplayer.WaveGenerator
import kotlin.collections.set
import kotlin.rem

class AudioInterface {
    var sample_rate: Int = 44100
    var soundfont: SoundFont? = null
    var playback_sample_handle_manager: SampleHandleManager? = null
    var playback_device: PlaybackDevice? = null
    var soundfont_supported_instrument_names = HashMap<Pair<Int, Int>, String>()
    var feedback_revolver = FeedbackDeviceRevolver(4)

    class FeedbackDeviceRevolver(var size: Int = 4) {
        var sample_handle_manager: SampleHandleManager? = null
        private var current_index: Int = 0
        private val devices = Array<FeedbackDevice?>(this.size) { null }

        fun set_handle_manager(sample_handle_manager: SampleHandleManager) {
            this.clear()
            this.sample_handle_manager = sample_handle_manager
        }

        fun play(event: NoteOn79, duration: Int = 250) {
            if (this.devices[this.current_index] == null) {
                this.devices[this.current_index] = FeedbackDevice(this.sample_handle_manager!!)
            }

            this.devices[this.current_index++]!!.new_event(event, duration)
            this.current_index %= this.devices.size
        }

        fun clear() {
            this.sample_handle_manager?.destroy()
            this.sample_handle_manager = null
            this.devices.forEachIndexed { i: Int, device: FeedbackDevice? ->
                device?.kill()
                device?.destroy()
                this.devices[i] = null
            }
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

    fun populate_supported_soundfont_instrument_names() {
        // populate a cache of available soundfont names so se don't have to open up the soundfont data
        // every time
        this.soundfont_supported_instrument_names.clear()
        this.soundfont?.let { soundfont ->
            for ((name, program, bank) in soundfont.get_available_presets()) {
                this.soundfont_supported_instrument_names[Pair(bank, program)] = name
            }
        }
        //     var program = 0
        //     for (name in this.resources.getStringArray(R.array.midi_instruments)) {
        //         this.soundfont_supported_instrument_names[Pair(0, program++)] = name
        //     }
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
        val event = NoteOn79(
            index = 0,
            channel = channel,
            note = note,
            bend = bend,
            velocity = velocity
        )
        this.feedback_revolver.play(event, 250)
    }

}