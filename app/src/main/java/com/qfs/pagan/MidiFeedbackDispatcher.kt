package com.qfs.pagan

import com.qfs.apres.VirtualMidiInputDevice
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
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


    fun play_note(channel: Int, note: Int, bend: Int = 0, midi2: Boolean = true) {
        val handle = runBlocking {
            this@MidiFeedbackDispatcher.mutex.withLock {
                if (midi2) {
                    var new_handle = this@MidiFeedbackDispatcher.handle_gen++
                    this@MidiFeedbackDispatcher.active_handles.add(new_handle)
                    new_handle
                } else {
                    note
                }
            }
        }

        this.send_event(
            if (midi2) {
                NoteOn79(
                    index = handle,
                    channel = channel,
                    note = note,
                    bend = bend,
                    velocity = 64 shl 8
                )
            } else {
                NoteOn(
                    channel = channel,
                    note = note,
                    velocity = 64
                )
            }
        )

        thread {
            Thread.sleep(400)
            this.disable_note(
                if (midi2) {
                    handle
                } else {
                   note
                },
                channel,
                midi2
            )
        }
    }

    private fun disable_note(handle: Int, channel: Int, midi2: Boolean) {
        runBlocking {
            this@MidiFeedbackDispatcher.mutex.withLock {
                if (midi2) {
                    this@MidiFeedbackDispatcher.send_event(
                        NoteOff79(
                            index = handle,
                            channel = channel,
                            note = 0,
                            velocity = 64 shl 8
                        )
                    )
                    this@MidiFeedbackDispatcher.active_handles.remove(handle)
                } else {
                    this@MidiFeedbackDispatcher.send_event(
                        NoteOff(
                            channel = channel,
                            note = handle,
                            velocity = 64
                        )
                    )
                }
                false
            }
        }

        Thread.sleep(1000)

        if (this.active_handles.isEmpty()) {
            this.send_event(MIDIStop())
        }
    }
}