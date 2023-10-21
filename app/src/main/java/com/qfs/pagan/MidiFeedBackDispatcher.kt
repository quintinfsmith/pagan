package com.qfs.pagan

import com.qfs.apres.VirtualMidiInputDevice
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import kotlin.concurrent.thread

class MidiFeedBackDispatcher: VirtualMidiInputDevice() {
    private var playing_note_map = HashMap<Pair<Int, Int>, Int>()
    private var note_handle_gen = 0

    fun play_note(channel: Int, note: Int) {
        val handle = this.note_handle_gen++
        this.playing_note_map[Pair(channel, note)] = handle
        this.send_event(NoteOn(channel, note, 64))
        thread {
            Thread.sleep(400)
            this.disable_note(channel, note, handle)
        }
    }

    private fun disable_note(channel: Int, note: Int, handle: Int) {
        val key = Pair(channel, note)
        val playing_handle = this.playing_note_map[key]
        if (handle == playing_handle) {
            this.send_event(NoteOff(channel, note, 64))
        }
    }
}