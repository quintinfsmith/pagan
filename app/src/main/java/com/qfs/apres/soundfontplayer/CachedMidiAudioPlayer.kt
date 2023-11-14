package com.qfs.apres.soundfontplayer

import com.qfs.apres.Midi
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event2.NoteOn79

open class CachedMidiAudioPlayer(sample_handle_manager: SampleHandleManager): MidiPlaybackDevice(sample_handle_manager) {
    var frame_count: Int = 0
    internal fun parse_midi(midi: Midi) {
        var start_frame = this.wave_generator.frame
        var frames_per_tick = ((500_000 / midi.get_ppqn()) * this.sample_handle_manager.sample_rate) / 1_000_000
        var last_tick = 0
        for ((tick, events) in midi.get_all_events_grouped()) {
            last_tick = tick
            val tick_frame = (tick * frames_per_tick) + start_frame

            this.wave_generator.place_events(events, tick_frame)

            // Need to handle some functions so the sample handles are created before the playback
            // & Need to set Tempo
            for (event in events) {
                when (event) {
                    is ProgramChange -> {
                        this.sample_handle_manager.change_program(event.channel, event.get_program())
                    }
                    is BankSelect -> {
                        this.sample_handle_manager.select_bank(event.channel, event.value)
                    }
                    is NoteOn -> {
                        this.sample_handle_manager.gen_sample_handles(event)
                    }
                    is NoteOn79 -> {
                        this.sample_handle_manager.gen_sample_handles(event)
                    }
                    is SetTempo -> {
                        frames_per_tick = ((event.get_uspqn() / midi.get_ppqn()) * this.sample_handle_manager.sample_rate) / 1_000_000
                    }
                }
            }
        }
        val tick_frame = (last_tick * frames_per_tick) + start_frame
        this.wave_generator.place_event(MIDIStop(), tick_frame)
        this.frame_count = tick_frame
    }

    fun play_midi(midi: Midi) {
        this.parse_midi(midi)
        this.start_playback()
    }
}
