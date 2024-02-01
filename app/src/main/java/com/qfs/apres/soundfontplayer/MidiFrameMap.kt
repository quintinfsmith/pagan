package com.qfs.apres.soundfontplayer

import com.qfs.apres.event.MIDIEvent

abstract class MidiFrameMap {
    abstract fun get_events(frame: Int): List<MIDIEvent>?
    abstract fun get_beat_frames(): List<Int>
}