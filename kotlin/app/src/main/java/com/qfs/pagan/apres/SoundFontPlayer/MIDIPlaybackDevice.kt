package com.qfs.pagan.apres.SoundFontPlayer

import android.content.Context
import com.qfs.pagan.apres.AllSoundOff
import com.qfs.pagan.apres.MIDI
import com.qfs.pagan.apres.MIDIStop
import com.qfs.pagan.apres.NoteOff
import com.qfs.pagan.apres.NoteOn
import com.qfs.pagan.apres.Preset
import com.qfs.pagan.apres.ProgramChange
import com.qfs.pagan.apres.SoundFont
import com.qfs.pagan.apres.VirtualMIDIDevice
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class MIDIPlaybackDevice(var context: Context, var sound_font: SoundFont): VirtualMIDIDevice() {
    private val soundfont_player = SoundFontPlayer(this.sound_font)
    override fun onMIDIStop(event: MIDIStop) {
        this.soundfont_player.stop()
        this.soundfont_player.enable_play()
    }

    override fun onNoteOn(event: NoteOn) {
        this.soundfont_player.press_note(event)
    }

    override fun onNoteOff(event: NoteOff) {
        this.soundfont_player.release_note(event)
    }

    override fun onProgramChange(event: ProgramChange) {
        this.soundfont_player.change_program(event.channel, event.program)
    }

    override fun onAllSoundOff(event: AllSoundOff) {
        this.soundfont_player.kill_channel_sound(event.channel)
    }

    fun precache_midi(midi: MIDI) {
        this.soundfont_player.precache_midi(midi)
    }

    fun clear_sample_cache() {
        this.soundfont_player.clear_sample_cache()
    }
}

