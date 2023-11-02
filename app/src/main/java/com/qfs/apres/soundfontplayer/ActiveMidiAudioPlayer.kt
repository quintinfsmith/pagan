package com.qfs.apres.soundfontplayer

import com.qfs.apres.VirtualMidiOutputDevice
import com.qfs.apres.event.AllSoundOff
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.MIDIStart
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79

class ActiveMidiAudioPlayer(sample_handle_manager: SampleHandleManager): MidiPlaybackDevice(
    sample_handle_manager,
    cache_size_limit = 1), VirtualMidiOutputDevice {
    init {
        this.buffer_delay = 1
    }
    override fun onNoteOn79(event: NoteOn79) {
        this.process_event(event)
        this.start_playback() // Only starts if not already started
    }
    override fun onNoteOn(event: NoteOn) {
        this.process_event(event)
        this.start_playback() // Only starts if not already started
    }

    override fun onNoteOff79(event: NoteOff79) {
        this.process_event(event)
    }
    override fun onNoteOff(event: NoteOff) {
        this.process_event(event)
    }

    override fun onMIDIStop(event: MIDIStop) {
        this.kill()
        this.process_event(event)
    }

    override fun onMIDIStart(event: MIDIStart) {
        this.start_playback()
    }

    override fun onBankSelect(event: BankSelect) {
        this.process_event(event)
    }

    override fun onAllSoundOff(event: AllSoundOff) {
        this.process_event(event)
    }

    override fun onProgramChange(event: ProgramChange) {
        this.process_event(event)
    }

    private fun process_event(event: MIDIEvent) {
        val delta_nano = if (this.is_playing()) {
            (System.nanoTime() - this.wave_generator.timestamp).toFloat()
        } else {
            0f
        }
        val frame = (this.SAMPLE_RATE_NANO * delta_nano).toInt() + (this.buffer_delay * this.sample_handle_manager.buffer_size)
        this.wave_generator.place_event(event, frame)
    }
}