package com.qfs.apres.soundfontplayer

import com.qfs.apres.Midi
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SetTempo

class MidiFrameMap(val sample_handle_manager: SampleHandleManager): FrameMap {
    private val frames = HashMap<Int, MutableSet<SampleHandle>>()
    private val beat_frames = mutableListOf<Int>()

    fun clear() {
        this.beat_frames.clear()
        this.frames.clear()
    }

    fun parse_midi(midi: Midi) {
        var ticks_per_beat = (500_000 / midi.get_ppqn())
        var frames_per_tick = (ticks_per_beat * this.sample_handle_manager.sample_rate) / 1_000_000
        var last_frame = 0
        var last_tick = 0
        val note_on_frames = HashMap<Pair<Int, Int>, Pair<Set<SampleHandle>, Int>>()

        for ((tick, events) in midi.get_all_events_grouped()) {
            val tick_frame = ((tick - last_tick) * frames_per_tick) + last_frame
            if (events.isNotEmpty() && !this.frames.containsKey(tick_frame)) {
                this.frames[tick_frame] = mutableSetOf<SampleHandle>()
            }

            // Need to handle some functions so the sample handles are created before the playback
            // & Need to set Tempo
            for (event in events) {
                when (event) {
                    is NoteOff -> {
                        val (handles, start_frame) = note_on_frames.remove(Pair(event.channel, event.get_note())) ?: continue
                        for (handle in handles) {
                            handle.release_frame = tick_frame - start_frame
                        }
                    }
                    is NoteOn -> {
                        val check_pair = note_on_frames.remove(Pair(event.channel, event.get_note()))
                        if (check_pair != null) {
                            for (handle in check_pair.first) {
                                handle.release_frame = tick_frame - check_pair.second
                            }
                        }
                        if (event.get_velocity() > 0) {
                            note_on_frames[Pair(event.channel, event.get_note())] = Pair(
                                this.sample_handle_manager.gen_sample_handles(event),
                                tick_frame
                            )
                        }
                    }
                    is ProgramChange -> {
                        this.sample_handle_manager.change_program(event.channel, event.get_program())
                    }
                    is BankSelect -> {
                        this.sample_handle_manager.select_bank(event.channel, event.value)
                    }
                    is SetTempo -> {
                        ticks_per_beat = (event.get_uspqn() / midi.get_ppqn())
                        frames_per_tick = (ticks_per_beat * this.sample_handle_manager.sample_rate) / 1_000_000
                    }
                    //is SongPositionPointer -> {
                    //    working_beat_frame += (midi.get_ppqn() * frames_per_tick)
                    //    this.beat_frames.add(working_beat_frame)
                    //}
                }
            }
            last_frame = tick_frame
            last_tick = tick
        }
    }

    override fun get_new_handles(frame: Int): Set<SampleHandle>? {
        return this.frames[frame]
    }

    override fun get_beat_frames(): List<Int> {
        // TODO: Track Beats
        return listOf()
    }

    override fun get_active_handles(frame: Int): Set<Pair<Int, SampleHandle>> {
        // TODO: implement
        return setOf()
    }
}