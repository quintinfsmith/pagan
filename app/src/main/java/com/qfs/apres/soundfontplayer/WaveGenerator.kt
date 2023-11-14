package com.qfs.apres.soundfontplayer

import com.qfs.apres.event.AllSoundOff
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class WaveGenerator(var sample_handle_manager: SampleHandleManager) {
    class KilledException: Exception()
    class DeadException: Exception()
    var frame = 0
    private var _empty_chunks_count = 0
    var timestamp: Long = System.nanoTime()
    private var _active_sample_handles = HashMap<Pair<Int, Int>, MutableList<Pair<Int, MutableList<SampleHandle>>>>()
    private var sample_release_map = HashMap<Int, Int>() // Key = samplehandle uuid, value = Off frame

    private var _midi_events_by_frame = HashMap<Int, MutableList<MIDIEvent>>()
    private var _event_mutex = Mutex()

    fun place_events(events: List<MIDIEvent>, frame: Int) {
        if (frame < this.frame) {
            return
        }
        runBlocking {
            this@WaveGenerator._event_mutex.withLock {
                if (!this@WaveGenerator._midi_events_by_frame.containsKey(frame)) {
                    this@WaveGenerator._midi_events_by_frame[frame] = mutableListOf()
                }
                for (event in events) {
                    this@WaveGenerator._midi_events_by_frame[frame]!!.add(event)
                }
            }
        }
    }

    fun place_event(event: MIDIEvent, frame: Int) {
        if (frame < this.frame) {
            return
        }

        runBlocking {
            this@WaveGenerator._event_mutex.withLock {
                if (!this@WaveGenerator._midi_events_by_frame.containsKey(frame)) {
                    this@WaveGenerator._midi_events_by_frame[frame] = mutableListOf()
                }
                this@WaveGenerator._midi_events_by_frame[frame]!!.add(event)
            }
        }
    }

    fun update_active_frames(initial_frame: Int, buffer_size: Int) {
        for (f in initial_frame until initial_frame + buffer_size) {
            if (this._midi_events_by_frame.containsKey(f)) {
                for (event in this._midi_events_by_frame[f]!!) {
                    when (event) {
                        is NoteOn -> {
                            var key_pair = Pair(event.channel, event.get_note())
                            if (!this._active_sample_handles.containsKey(key_pair)) {
                                this._active_sample_handles[key_pair] = mutableListOf()
                            }
                            this._active_sample_handles[key_pair]!!.add(
                                Pair(
                                    f,
                                    this.sample_handle_manager.gen_sample_handles(event).toMutableList()
                                )
                            )
                        }

                        is NoteOff -> {
                            var key_pair = Pair(event.channel, event.get_note())
                            for ((_, handles) in this._active_sample_handles[key_pair]
                                ?: continue) {
                                for (handle in handles) {
                                    if (!this.sample_release_map.containsKey(handle.uuid)) {
                                        this.sample_release_map[handle.uuid] = f
                                    }
                                }

                            }
                        }

                        is NoteOn79 -> {
                            var key_pair = Pair(event.channel, event.index)
                            if (!this._active_sample_handles.containsKey(key_pair)) {
                                this._active_sample_handles[key_pair] = mutableListOf()
                            }
                            this._active_sample_handles[key_pair]!!.add(
                                Pair(
                                    f,
                                    this.sample_handle_manager.gen_sample_handles(event).toMutableList()
                                )
                            )
                        }

                        is NoteOff79 -> {
                            var key_pair = Pair(event.channel, event.index)
                            for ((_, handles) in this._active_sample_handles[key_pair]
                                ?: continue) {
                                for (handle in handles) {
                                    if (!this.sample_release_map.containsKey(handle.uuid)) {
                                        this.sample_release_map[handle.uuid] = f
                                    }
                                }

                            }

                            // for (handle in this._active_sample_handles[key_pair] ?: continue) {
                            //     handle.release_note()
                            // }
                        }

                        is MIDIStop -> {
                            throw KilledException()
                        }

                        is AllSoundOff -> {
                            for ((_, buffer_handle_list) in this._active_sample_handles) {
                                for ((_, handles) in buffer_handle_list) {
                                    for (handle in handles) {
                                        if (!this.sample_release_map.containsKey(handle.uuid)) {
                                            this.sample_release_map[handle.uuid] = f
                                        }
                                    }
                                }
                            }
                        }

                        is ProgramChange -> {
                            this.sample_handle_manager.change_program(
                                event.channel,
                                event.get_program()
                            )
                        }

                        is BankSelect -> {
                            this.sample_handle_manager.select_bank(event.channel, event.value)
                        }
                        //is SongPositionPointer -> {
                        //    var millis = (f - this.frame) * 1_000 / this.sample_handle_manager.sample_rate
                        //    pointer_list.add(Pair(millis, event.beat))
                        //}
                    }
                }
            }
        }
    }

    data class Quad(var first: Int, var second: Int, var third: Int, var fourth: Int)
    fun generate(buffer_size: Int): ShortArray {
        this.update_active_frames(this.frame, buffer_size)

        val initial_array = IntArray(buffer_size * 2) { 0 }
        var max_frame_value = 0
        var is_empty = true

        val killed_sample_handles = mutableListOf<Quad>()

        for ((key, pair_list) in this._active_sample_handles) {
            // Run in reverse order to get the most recent frame
            pair_list.forEachIndexed { k: Int, (first_frame: Int, sample_handles: List<SampleHandle>) ->
                sample_handles.forEachIndexed { j: Int, sample_handle: SampleHandle ->
                    if (sample_handle.is_dead) {
                        return@forEachIndexed
                    }
                    is_empty = false

                    for (f in this.frame + first_frame until this.frame + buffer_size) {
                        if (sample_handle.is_pressed && f == this.sample_release_map[sample_handle.uuid]) {
                            sample_handle.release_note()
                            this.sample_release_map.remove(sample_handle.uuid)
                        }


                        val frame_value = sample_handle.get_next_frame()
                        if (frame_value == null) {
                            killed_sample_handles.add(Quad(key.first, key.second, k, j))
                            break
                        }

                        var left_frame: Int = 0
                        var right_frame: Int = 0

                        // TODO: Implement ROM stereo modes
                        val pan = sample_handle.pan
                        when (sample_handle.stereo_mode and 7) {
                            1 -> { // mono
                                if (pan > 0) {
                                    left_frame += frame_value
                                    right_frame += (frame_value * (100 - pan.toInt()) / 100)
                                } else if (pan < 0) {
                                    left_frame += frame_value * (100 + pan.toInt()) / 100
                                    right_frame += frame_value
                                } else {
                                    left_frame += frame_value
                                    right_frame += frame_value
                                }
                            }

                            2 -> { // right
                                right_frame += if (pan > 0.0) {
                                    (frame_value * (100 - pan.toInt())) / 100
                                } else {
                                    frame_value
                                }
                            }

                            4 -> { // left
                                left_frame += if (pan < 0.0) {
                                    (frame_value * (100 + pan.toInt())) / 100
                                } else {
                                    frame_value
                                }
                            }

                            else -> {}
                        }
                        var buffer_index = (f - this.frame) * 2
                        initial_array[buffer_index] += right_frame
                        initial_array[buffer_index + 1] += left_frame
                    }
                }
            }
        }

        for (v in initial_array) {
            max_frame_value = max(max_frame_value, v)
        }


        var ordered_killed_sample_handles = killed_sample_handles.toMutableList()
        ordered_killed_sample_handles.sortWith { quad, quad2 ->
            if (quad.third > quad2.third) {
                -1
            } else if (quad.third == quad2.third) {
                if (quad.fourth > quad2.fourth) {
                    -1
                } else if (quad.fourth == quad2.fourth) {
                    0
                } else {
                    1
                }
            } else {
                1
            }
        }

        for (quad in ordered_killed_sample_handles) {
            this._active_sample_handles[Pair(quad.first, quad.second)]!![quad.third].second.removeAt(quad.fourth)
        }
        for (quad in ordered_killed_sample_handles) {
            if (this._active_sample_handles[Pair(quad.first, quad.second)]!!.size >= quad.third) {
                continue
            }
            if ((this._active_sample_handles[Pair(quad.first, quad.second)]!![quad.third].second).isEmpty()) {
                this._active_sample_handles[Pair(quad.first, quad.second)]!!.removeAt(quad.third)
            }
        }
        for (quad in ordered_killed_sample_handles) {
            if (this._active_sample_handles[Pair(quad.first, quad.second)]?.isEmpty() ?: continue) {
                this._active_sample_handles.remove(Pair(quad.first, quad.second))
            }
        }

        val mid = Short.MAX_VALUE / 2
        val compression_ratio = if (max_frame_value <= Short.MAX_VALUE) {
            1F
        } else {
            (Short.MAX_VALUE - mid).toFloat() / (max_frame_value - mid).toFloat()
        }

        val compressed_array = ShortArray(initial_array.size) { i: Int ->
            val v = initial_array[i]
            if (compression_ratio >= 1F || (0 - mid <= v && v <= mid)) {
                v.toShort()
            } else if (v > mid) {
                (mid + ((v - mid).toFloat() * compression_ratio).toInt()).toShort()
            } else {
                (((v + mid).toFloat() * compression_ratio).toInt() - mid).toShort()
            }
        }

        this.frame += buffer_size

        if (is_empty) {
            this._empty_chunks_count += 1
        } else {
            this._empty_chunks_count = 0
        }

        return compressed_array
    }

    fun clear() {
        this._active_sample_handles.clear()
        this._midi_events_by_frame.clear()
        this.frame = 0
        this._empty_chunks_count = 0
    }
}