package com.qfs.pagan

import com.qfs.apres.VirtualMidiInputDevice
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.thread

class MidiFeedBackDispatcher: VirtualMidiInputDevice() {
    var mutex = Mutex()
    private var active_handles = mutableSetOf<Int>()

    fun play_note(channel: Int, note: Int, bend: Int) {
        val handle = runBlocking {
            this@MidiFeedBackDispatcher.mutex.withLock {
                var new_handle = 0
                while (this@MidiFeedBackDispatcher.active_handles.contains(new_handle)) {
                    new_handle += 1
                }
                this@MidiFeedBackDispatcher.active_handles.add(new_handle)
                new_handle
            }
        }

        this.send_event(
            NoteOn79(
                index = handle,
                channel = channel,
                note = note,
                bend = bend,
                velocity = 64 shl 8
            )
        )

        thread {
            Thread.sleep(400)
            this.disable_note(handle, channel)
        }
    }

    private fun disable_note(handle: Int, channel: Int) {
        this.send_event(
            NoteOff79(
                index = handle,
                channel = channel,
                note = 0,
                velocity = 64 shl 8
            )
        )
        runBlocking {
            this@MidiFeedBackDispatcher.mutex.withLock {
                this@MidiFeedBackDispatcher.active_handles.remove(handle)
            }
        }

        Thread.sleep(1000)

        if (this.active_handles.isEmpty()) {
            this.send_event(MIDIStop())
        }
    }
}