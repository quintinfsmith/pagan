package com.qfs.apres.soundfontplayer

import com.qfs.apres.Midi
import com.qfs.apres.soundfont.SoundFont

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
