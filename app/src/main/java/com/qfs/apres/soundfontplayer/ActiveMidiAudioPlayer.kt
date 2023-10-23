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
import com.qfs.apres.soundfont.SoundFont

class ActiveMidiAudioPlayer(sample_rate: Int, sound_font: SoundFont): MidiPlaybackDevice(
    sample_rate = sample_rate,
    cache_size_limit = 1,
    sound_font = sound_font), VirtualMidiOutputDevice {
    override fun onNoteOn(event: NoteOn) {
        this.process_event(event) ?: return
        this.start_playback() // Only starts if not already started
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
        this.select_bank(event.channel, event.value)
    }

    override fun onAllSoundOff(event: AllSoundOff) {
        this.process_event(event)
    }

    override fun onProgramChange(event: ProgramChange) {
        this.change_program(event.channel, event.program)
    }

    private fun process_event(event: MIDIEvent) {
        if (!this.is_playing()) {
            return
        }
        this.wave_generator.process_event(event, this.buffer_delay)
    }
}