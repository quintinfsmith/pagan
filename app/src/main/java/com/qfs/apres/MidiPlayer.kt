package com.qfs.apres

import android.util.Log
import com.qfs.apres.event.AllSoundOff
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79

class MidiPlayer: VirtualMidiInputDevice() {
    var playing = false
    fun play_midi(midi: Midi, callback: (() -> Unit)? = null) {
        if (this.playing) {
            return
        }

        if (! this.is_connected()) {
            Log.w("apres", "Can't play without registering a midi controller first")
            return
        }

        val notes_on = mutableSetOf<Triple<Int, Int, Boolean>>()

        this.playing = true
        val ppqn = midi.get_ppqn()
        var us_per_tick = 60000000 / (ppqn * 120)
        var previous_tick = 0
        val start_time = System.currentTimeMillis()
        var delay_accum = 0

        for ((tick, events) in midi.get_all_events_grouped()) {
            if (!this.playing) {
                break
            }

            if ((tick - previous_tick) > 0) {
                val delay = ((tick - previous_tick) * us_per_tick) / 1000
                val drift = delay_accum - (System.currentTimeMillis() - start_time)
                delay_accum += delay

                if (delay + drift > 0) {
                    Thread.sleep(delay + drift)
                }
                previous_tick = tick
            }

            for (event in events) {
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

                if (this@MidiPlayer.playing) {
                    this@MidiPlayer.send_event(event)
                }
            }
        }

        for ((channel, index, is_midi2) in notes_on) {
            val event: MIDIEvent = if (is_midi2) {
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

        // if the song wasn't manually stopped, return to the start
        if (this.playing) {
            this.send_event(SongPositionPointer(0))
            this.send_event(MIDIStop())
        }


        for (i in 0 until 16) {
            this.send_event(AllSoundOff(i))
        }

        this.playing = false

        if (callback != null) {
            callback()
        }
    }

    fun stop() {
        this.send_event(MIDIStop())
        this.playing = false
    }
}
