package com.qfs.apres.soundfontplayer

import com.qfs.apres.event.AllSoundOff
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import kotlin.math.abs
import kotlin.math.max

class WaveGenerator(var sample_handle_manager: SampleHandleManager) {
    class KilledException: Exception()
    class EmptyException: Exception()
    class DeadException: Exception()
    class EventInPastException: Exception()
    var frame = 0
    var kill_frame: Int? = null
    var last_frame: Int = 0
    private var _empty_chunks_count = 0
    private var _active_sample_handles = HashMap<Pair<Int, Int>, MutableList<Pair<Int, MutableList<SampleHandle>>>>()
    private var sample_release_map = HashMap<Int, Int>() // Key = samplehandle uuid, value = Off frame
    private var _working_int_array = IntArray(sample_handle_manager.buffer_size * 2)

    var midi_frame_map: MidiFrameMap? = null
    fun set_midi_frame_map(midi_frame_map: MidiFrameMap) {
        this.midi_frame_map = midi_frame_map
    }

    fun generate(buffer_size: Int): ShortArray {
        val output_array = ShortArray(buffer_size * 2)
        this.generate(output_array)
        return output_array
    }

    fun generate(): ShortArray {
        return this.generate(this.sample_handle_manager.buffer_size)
    }

    fun generate(array: ShortArray) {
        if (this.kill_frame != null && this.kill_frame!! <= this.frame) {
            throw KilledException()
        }

        val buffer_size = array.size / 2
        val first_frame = this.frame
        this.update_active_frames(this.frame, buffer_size)

        if (this._active_sample_handles.isEmpty()) {
            if (this.last_frame <= this.frame) {
                throw DeadException()
            } else {
                this.frame += buffer_size
                throw EmptyException()
            }
        }

        for (i in this._working_int_array.indices) {
            this._working_int_array[i] = 0
        }

        var max_frame_value = 0
        var is_empty = true

        for ((_, pair_list) in this._active_sample_handles) {
            for (i in pair_list.indices) {
                val (first_sample_frame, sample_handles) = pair_list[i]
                for (j in sample_handles.indices) {
                    val sample_handle = sample_handles[j]
                    if (sample_handle.is_dead || (first_sample_frame >= first_frame + buffer_size)) {
                        continue
                    }

                    is_empty = false
                    for (f in max(first_sample_frame, first_frame) until first_frame + buffer_size) {
                        if (sample_handle.is_pressed() && f == this.sample_release_map[sample_handle.uuid]) {
                            sample_handle.release_note()
                        }

                        val frame_value = sample_handle.get_next_frame() ?: break

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

                        var buffer_index = (f - first_frame) * 2
                        this._working_int_array[buffer_index] += right_frame
                        this._working_int_array[buffer_index + 1] += left_frame
                    }
                }
            }
        }

        for (v in this._working_int_array) {
            max_frame_value = max(max_frame_value, abs(v))
        }

        val mid = Short.MAX_VALUE / 2
        val compression_ratio = if (max_frame_value <= Short.MAX_VALUE) {
            1F
        } else {
            (Short.MAX_VALUE - mid).toFloat() / (max_frame_value - mid).toFloat()
        }

        this._working_int_array.forEachIndexed { i: Int, v: Int ->
            array[i] = if (compression_ratio >= 1F || (0 - mid <= v && v <= mid)) {
                v.toShort()
            } else if (v > mid) {
                (mid + ((v - mid).toFloat() * compression_ratio).toInt()).toShort()
            } else {
                (((v + mid).toFloat() * compression_ratio).toInt() - mid).toShort()
            }
        }

        if (is_empty) {
            this._empty_chunks_count += 1
        } else {
            this._empty_chunks_count = 0
        }

        this.frame += buffer_size
    }

    fun update_active_frames(initial_frame: Int, buffer_size: Int) {
        // First check for, and remove dead sample handles
        val empty_pairs = mutableListOf<Pair<Int, Int>>()
        for ((key, pair_list) in this._active_sample_handles) {
            for (i in pair_list.indices.reversed()) {
                val (_, sample_handles) = pair_list[i]
                for (j in sample_handles.indices.reversed()) {
                    var sample_handle = sample_handles[j]
                    if (sample_handle.is_dead) {
                        sample_handles.removeAt(j)
                    }
                }
                if (sample_handles.isEmpty()) {
                    pair_list.removeAt(i)
                }
            }
            if (pair_list.isEmpty()) {
                empty_pairs.add(key)
            }
        }

        for (key in empty_pairs) {
            this._active_sample_handles.remove(key)
        }

        // then populate the next active frames with upcoming sample handles
        for (f in initial_frame until initial_frame + buffer_size) {
            val events = this.midi_frame_map?.get_events(f) ?: continue
            for (event in events) {
                when (event) {
                    is NoteOn -> {
                        val key_pair = Pair(event.channel, event.get_note())
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
                            for (handle in handles.reversed()) {
                                if (handle.is_pressed() && !this.sample_release_map.containsKey(handle.uuid)) {
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
                            for (handle in handles.reversed()) {
                                if (handle.is_pressed() && !this.sample_release_map.containsKey(handle.uuid)) {
                                    this.sample_release_map[handle.uuid] = f
                                }
                            }
                        }
                    }

                    is MIDIStop -> {
                        this.kill_frame = f
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

                    is SongPositionPointer -> { }
                }
            }
        }
    }

    fun clear() {
        this.kill_frame = null
        this._active_sample_handles.clear()
        this.sample_release_map.clear()
        this.frame = 0
        this._empty_chunks_count = 0
    }
}