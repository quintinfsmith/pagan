package com.qfs.apres

import android.util.Log
import com.qfs.apres.event.AllSoundOff
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event.SongPositionPointer

class MidiPlayer: VirtualMidiDevice() {
    var playing = false
    fun play_midi(midi: Midi, callback: (() -> Unit)? = null) {
        if (this.playing) {
            return
        }

        if (! this.is_connected()) {
            Log.w("apres", "Can't play without registering a midi controller first")
            return
        }

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
                    is SetTempo -> {
                        us_per_tick = event.get_uspqn() / ppqn
                    }
                }
                if (this@MidiPlayer.playing) {
                    this@MidiPlayer.sendEvent(event)
                }
            }
        }

        // if the song wasn't manually stopped, return to the start
        if (this.playing) {
            this.sendEvent(SongPositionPointer(0))
        }

        for (i in 0 until 16) {
            this.sendEvent(AllSoundOff(i))
        }

        this.playing = false

        if (callback != null) {
            callback()
        }
    }

    fun stop() {
        this.sendEvent(MIDIStop())
        this.playing = false
    }
}