package com.qfs.pagan

import com.qfs.apres.VirtualMidiInputDevice
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.thread

class MidiFeedbackDispatcher: VirtualMidiInputDevice() {
    var mutex = Mutex()
    private var active_handles = mutableSetOf<Int>()
    var handle_gen = 0

    fun play_note(channel: Int, note: Int, bend: Int) {
        val handle = runBlocking {
            this@MidiFeedbackDispatcher.mutex.withLock {
                var new_handle = this@MidiFeedbackDispatcher.handle_gen++
                this@MidiFeedbackDispatcher.active_handles.add(new_handle)
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
            this@MidiFeedbackDispatcher.mutex.withLock {
                this@MidiFeedbackDispatcher.active_handles.remove(handle)
            }
        }

        Thread.sleep(1000)

        if (this.active_handles.isEmpty()) {
            this.send_event(MIDIStop())
        }
    }
}