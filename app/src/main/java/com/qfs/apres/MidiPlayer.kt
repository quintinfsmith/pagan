/*
 * Apres, A Midi & Soundfont library
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.apres

import android.util.Log
import com.qfs.apres.event.AllSoundOff
import com.qfs.apres.event.GeneralMIDIEvent
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.thread

class MidiPlayer: VirtualMidiInputDevice() {
    var playing = false

    private var _handle_mutex = Mutex()
    private var _index_mutex = Mutex()
    // channel 17 for midi2 devices
    private var _active_handles = HashMap<Triple<Int, Int, Boolean>, Long>()
    private var _index_gen = HashMap<Int, Int>()
    private val _note_duration: Long = 400

    private fun gen_index(channel: Int): Int {
        return runBlocking {
            this@MidiPlayer.let { that ->
                that._index_mutex.withLock {
                    val index = that._index_gen[channel] ?: 0
                    that._index_gen[channel] = index + 1
                    index
                }
            }
        }
    }

    fun close() {
        runBlocking {
            this@MidiPlayer.let { that ->
                that._handle_mutex.withLock {
                    for (handle in that._active_handles.keys) {
                        that._note_off(handle)
                    }
                    that._active_handles.clear()
                }
            }
        }
    }

    fun play_note(channel: Int, note: Int, bend: Int = 0, velocity: Int = 64, midi2: Boolean = true) {
        val handle = if (midi2) {
            val index = this.gen_index(channel)
            Triple(channel, index, true)
        } else {
            Triple(channel, note, false)
        }

        if (this._active_handles.containsKey(handle)) {
            if (handle.third) {
                this.send_event(
                    NoteOff79(
                        index = handle.second,
                        channel = channel,
                        note = note,
                        bend = bend,
                        velocity = velocity shl 8
                    )
                )
            } else {
                this.send_event(
                    NoteOff(
                        channel = channel,
                        note = note,
                        velocity = velocity
                    )
                )

            }
        }

        if (handle.third) {
            this.send_event(
                NoteOn79(
                    index = handle.second,
                    channel = channel,
                    note = note,
                    bend = bend,
                    velocity = velocity shl 8
                )
            )
        } else {
            this.send_event(
                NoteOn(
                    channel = channel,
                    note = note,
                    velocity = velocity
                )
            )

        }

        val now = System.nanoTime()
        runBlocking {
            this@MidiPlayer._handle_mutex.withLock {
                this@MidiPlayer._active_handles[handle] = now
            }
        }

        thread {
            Thread.sleep(this._note_duration)
            val do_cancel = runBlocking {
                this@MidiPlayer._handle_mutex.withLock {
                    if (this@MidiPlayer._active_handles[handle] == now) {
                        this@MidiPlayer._active_handles.remove(handle)
                        false
                    } else {
                        true
                    }
                }
            }

            if (do_cancel) return@thread

            this._note_off(handle)
        }
    }

    private fun _note_off(handle: Triple<Int, Int, Boolean>) {
        val (channel, note, midi2) = handle
        if (midi2) {
            this@MidiPlayer.send_event(
                NoteOff79(
                    index = handle.second,
                    channel = channel,
                    note = 0,
                    velocity = 64 shl 8
                )
            )
        } else {
            this@MidiPlayer.send_event(
                NoteOff(
                    channel = channel,
                    note = note,
                    velocity = 64
                )
            )
        }

    }

    fun play_midi(midi: Midi, loop_playback: Boolean = false, callback: (() -> Unit)? = null) {
        if (this.playing) return

        if (! this.is_connected()) {
            Log.w("apres", "Can't play without registering a midi controller first")
            return
        }

        val notes_on = mutableSetOf<Triple<Int, Int, Boolean>>()

        this.playing = true
        val grouped_events = midi.get_all_events_grouped()
        val ppqn = midi.get_ppqn()
        while (true) {
            var us_per_tick = 60000000 / (ppqn * 120)
            var previous_tick = 0
            val start_time = System.currentTimeMillis()
            var delay_accum = 0

            for ((tick, events) in grouped_events) {
                if (!this.playing && notes_on.isEmpty()) break

                if (this.playing && (tick - previous_tick) > 0) {
                    val delay = ((tick - previous_tick) * us_per_tick) / 1000
                    val drift = delay_accum - (System.currentTimeMillis() - start_time)
                    delay_accum += delay

                    if (delay + drift > 0) {
                        Thread.sleep(delay + drift)
                    }
                    previous_tick = tick
                }

                for (event in events) {
                    if (!this.playing) {
                        when (event) {
                            is NoteOff -> {
                                notes_on.remove(Triple(event.channel, event.get_note(), false))
                            }

                            is NoteOff79 -> {
                                val elm = Triple(event.channel, event.index, true)
                                notes_on.remove(elm)
                            }

                            else -> continue
                        }
                    } else {
                        when (event) {
                            is NoteOn -> {
                                val elm = Triple(event.channel, event.get_note(), false)
                                if (event.get_velocity() > 0) {
                                    notes_on.add(elm)
                                } else {
                                    notes_on.remove(elm)
                                }
                            }

                            is NoteOff -> {
                                notes_on.remove(Triple(event.channel, event.get_note(), false))
                            }

                            is NoteOn79 -> {
                                val elm = Triple(event.channel, event.index, true)
                                notes_on.add(elm)
                            }

                            is NoteOff79 -> {
                                val elm = Triple(event.channel, event.index, true)
                                notes_on.remove(elm)
                            }

                            is SetTempo -> {
                                us_per_tick = event.get_uspqn() / ppqn
                            }
                        }
                    }
                    this@MidiPlayer.send_event(event)
                }
            }

            if (!loop_playback || !this.playing) break
        }

        // Ensure playing is off
        this.playing = false

        for ((channel, index, is_midi2) in notes_on) {
            val event: GeneralMIDIEvent = if (is_midi2) {
                NoteOff79(
                    index=index,
                    channel=channel,
                    note=index,
                    velocity=128
                )
            } else {
                NoteOff(channel, index, 100)
            }
            this.send_event(event)
        }

        for (i in 0 until 16) {
            this.send_event(AllSoundOff(i))
        }

        this.send_event(MIDIStop())


        if (callback != null) {
            callback()
        }
    }

    fun stop() {
        this.playing = false
    }
}
