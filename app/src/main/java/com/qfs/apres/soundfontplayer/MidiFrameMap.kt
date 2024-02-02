package com.qfs.apres.soundfontplayer

import com.qfs.apres.event.MIDIEvent

interface MidiFrameMap {
    fun get_events(frame: Int): List<MIDIEvent>?
    fun get_beat_frames(): List<Int>
}