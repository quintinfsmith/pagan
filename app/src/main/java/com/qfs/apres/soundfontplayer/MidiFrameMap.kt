package com.qfs.apres.soundfontplayer

import com.qfs.apres.event.MIDIEvent

abstract class MidiFrameMap {
    abstract fun get_events(frame: Int): List<MIDIEvent>
}
//open class MidiFrameMap {
//    private var _midi_events_by_frame = HashMap<Int, MutableList<MIDIEvent>>()
//
//    fun get_events(frame: Int): List<MIDIEvent> {
//        return this._midi_events_by_frame[frame]!!.sortedBy {
//            when (it) {
//                is NoteOn -> { 2 }
//                is NoteOff -> { 0 }
//                else -> { 1 }
//            }
//        }
//    }
//}