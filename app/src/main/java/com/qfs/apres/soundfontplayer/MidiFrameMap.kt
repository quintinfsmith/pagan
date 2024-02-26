package com.qfs.apres.soundfontplayer

import com.qfs.apres.Midi
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SetTempo
import kotlin.math.max
import kotlin.math.min

class MidiFrameMap(val sample_handle_manager: SampleHandleManager): FrameMap {
    private val frames = HashMap<Int, MutableSet<SampleHandle>>()
    private val beat_frames = mutableListOf<Int>()
    private var final_frame: Int = -1
    private var max_overlap = 0
    private val percussion_handles = mutableSetOf<Int>()

    fun clear() {
        this.beat_frames.clear()
        this.frames.clear()
        this.final_frame = 0
        this.percussion_handles.clear()
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
                            this.final_frame = max(tick_frame + handle.get_release_duration(), this.final_frame)
                        }
                    }
                    is NoteOn -> {
                        val check_pair = note_on_frames.remove(Pair(event.channel, event.get_note()))
                        if (check_pair != null) {
                            for (handle in check_pair.first) {
                                handle.release_frame = tick_frame - check_pair.second
                                this.final_frame = max(tick_frame + handle.get_release_duration(), this.final_frame)
                            }
                        }
                        if (event.get_velocity() > 0) {
                            val handles = this.sample_handle_manager.gen_sample_handles(event)
                            if (!this.frames.containsKey(tick_frame)) {
                                this.frames[tick_frame] = mutableSetOf()
                            }
                            this.frames[tick_frame]!!.addAll(handles)
                            note_on_frames[Pair(event.channel, event.get_note())] = Pair(
                                handles,
                                tick_frame
                            )
                            if (event.channel == 9) {
                                for (handle in handles) {
                                    this.percussion_handles.add(handle.uuid)
                                }
                            }
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
        this.max_overlap = this.calculate_max_overlap()
    }

    override fun get_size(): Int {
       return this.final_frame + 1
    }

    override fun get_new_handles(frame: Int): Set<SampleHandle>? {
        val output = mutableSetOf<SampleHandle>()

        for (handle in this.frames[frame] ?: setOf()) {
            val max_volume = (handle.max_frame_value().toDouble() / Short.MAX_VALUE.toDouble())

            if (this.percussion_handles.contains(handle.uuid)) {
                // Do not equalize the percussion channel
            } else {
                handle.volume *= min((1.0 / this.max_overlap.toDouble()) / max_volume, 1.0)
            }
            output.add(handle)
        }

        return output
    }

    override fun get_beat_frames(): HashMap<Int, IntRange> {
        // TODO: Track Beats
        return HashMap()
    }

    override fun get_active_handles(frame: Int): Set<Pair<Int, SampleHandle>> {
        // TODO: implement
        return setOf()
    }

    private fun calculate_max_overlap(): Int {
        val event_list = mutableListOf<Pair<Int, Boolean>>()
        for ((frame, handles) in this.frames) {
            for (handle in handles) {
                event_list.add(Pair(frame, true))
                event_list.add(Pair(frame + (handle.release_frame ?: 0), false))
            }
        }

        event_list.sortBy { it.first }
        var max_overlap = 0
        var count = 0
        for ((_, sample_on) in event_list) {
            if (sample_on) {
                count += 1
                max_overlap = max(max_overlap, count)
            } else {
                count -= 1
            }
        }
        return max_overlap
    }
}