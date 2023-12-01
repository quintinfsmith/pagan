package com.qfs.pagan

import com.qfs.apres.VirtualMidiInputDevice
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.thread

class MidiFeedbackDispatcher: VirtualMidiInputDevice() {
    private var _handle_mutex = Mutex()
    private var _index_mutex = Mutex()
    // channel 17 for midi2 devices
    private var _active_handles = HashMap<Triple<Int, Int, Boolean>, Int>()
    private var _index_gen = HashMap<Int, Int>()
    private val _note_duration: Long = 400

    private fun gen_index(channel: Int): Int {
        return runBlocking {
            this@MidiFeedbackDispatcher._index_mutex.withLock {
                val index = this@MidiFeedbackDispatcher._index_gen[channel] ?: 0
                this@MidiFeedbackDispatcher._index_gen[channel] = index + 1
                index
            }
        }
    }

    fun play_note(channel: Int, note: Int, bend: Int = 0, midi2: Boolean = true) {
        val handle = if (midi2) {
            val index = this.gen_index(channel)
            this.send_event(
                NoteOn79(
                    index = index,
                    channel = channel,
                    note = note,
                    bend = bend,
                    velocity = 64 shl 8
                )
            )
            Triple(channel, index, true)
        } else {
            this.send_event(
                NoteOn(
                    channel = channel,
                    note = note,
                    velocity = 64
                )
            )
            Triple(channel, note, false)
        }

        val initial_count = runBlocking {
            this@MidiFeedbackDispatcher._handle_mutex.withLock {
                val count = this@MidiFeedbackDispatcher._active_handles[handle] ?: 0
                this@MidiFeedbackDispatcher._active_handles[handle] = count + 1
                count + 1
            }
        }

        thread {
            Thread.sleep(this._note_duration)
            val do_cancel = runBlocking {
                this@MidiFeedbackDispatcher._handle_mutex.withLock {
                    val active_count = this@MidiFeedbackDispatcher._active_handles[handle] ?: 1
                    // If another note on the same handle has been pressed during the sleep,
                    // don't bother turning this one off
                    if (active_count == initial_count) {
                        this@MidiFeedbackDispatcher._active_handles.remove(handle)
                        false
                    } else {
                        true
                    }
                }
            }

            if (do_cancel) {
                return@thread
            }

            if (midi2) {
                this@MidiFeedbackDispatcher.send_event(
                    NoteOff79(
                        index = handle.second,
                        channel = channel,
                        note = 0,
                        velocity = 64 shl 8
                    )
                )
            } else {
                this@MidiFeedbackDispatcher.send_event(
                    NoteOff(
                        channel = channel,
                        note = note,
                        velocity = 64
                    )
                )
            }
        }
    }
}