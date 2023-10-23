package com.qfs.pagan

import com.qfs.apres.VirtualMidiInputDevice
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.thread

class MidiFeedBackDispatcher: VirtualMidiInputDevice() {
    var mutex = Mutex()
    private var playing_note_map = HashMap<Pair<Int, Int>, Int>()
    private var note_handle_gen = 0

    fun play_note(channel: Int, note: Int) {
        val handle = runBlocking {
            this@MidiFeedBackDispatcher.mutex.withLock {
                val handle = this@MidiFeedBackDispatcher.note_handle_gen++
                this@MidiFeedBackDispatcher.playing_note_map[Pair(channel, note)] = handle
                handle
            }
        }
        this.send_event(NoteOn(channel, note, 64))
        thread {
            Thread.sleep(400)
            this.disable_note(channel, note, handle)
        }
    }

    private fun disable_note(channel: Int, note: Int, handle: Int) {
        var unused_note_handle = runBlocking {
            this@MidiFeedBackDispatcher.mutex.withLock {
                val key = Pair(channel, note)
                val playing_handle = this@MidiFeedBackDispatcher.playing_note_map[key]
                if (handle == playing_handle) {
                    this@MidiFeedBackDispatcher.send_event(NoteOff(channel, note, 64))
                    this@MidiFeedBackDispatcher.playing_note_map.remove(key)
                }
                this@MidiFeedBackDispatcher.note_handle_gen
            }
        }

        Thread.sleep(1000)

        if (unused_note_handle == this.note_handle_gen) {
            this.send_event(MIDIStop())
        }
    }
}