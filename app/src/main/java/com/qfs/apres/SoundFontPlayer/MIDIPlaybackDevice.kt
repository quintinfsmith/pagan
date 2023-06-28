package com.qfs.apres.SoundFontPlayer

import android.content.Context
import com.qfs.apres.AllSoundOff
import com.qfs.apres.BankSelect
import com.qfs.apres.MIDI
import com.qfs.apres.MIDIStop
import com.qfs.apres.NoteOff
import com.qfs.apres.NoteOn
import com.qfs.apres.ProgramChange
import com.qfs.apres.SoundFont
import com.qfs.apres.VirtualMIDIDevice


class MIDIPlaybackDevice(var context: Context, var sound_font: SoundFont): VirtualMIDIDevice() {
    private val soundfont_player = SoundFontPlayer(this.sound_font)
//    override fun onMIDIStop(event: MIDIStop) {
//        this.soundfont_player.stop()
//        this.soundfont_player.enable_play()
//    }
//
//    override fun onNoteOn(event: NoteOn) {
//        this.soundfont_player.press_note(event)
//    }
//
//    override fun onNoteOff(event: NoteOff) {
//        this.soundfont_player.release_note(event)
//    }
//
//    override fun onProgramChange(event: ProgramChange) {
//        this.soundfont_player.change_program(event.channel, event.program)
//    }
//    override fun onBankSelect(event: BankSelect) {
//        this.soundfont_player.select_bank(event.channel, event.value)
//    }
//
//    override fun onAllSoundOff(event: AllSoundOff) {
//        this.soundfont_player.kill_channel_sound(event.channel)
//    }
//
}

