package com.qfs.apres.soundfontplayer

import com.qfs.apres.event.AllSoundOff
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.IntBuffer
import kotlin.math.abs
import kotlin.math.max

class WaveGenerator(var sample_handle_manager: SampleHandleManager) {
    class KilledException: Exception()
    class DeadException: Exception()
    var frame = 0
    private var _empty_chunks_count = 0
    var timestamp: Long = System.nanoTime()
    private var _active_sample_handles = HashMap<Pair<Int, Int>, MutableSet<SampleHandle>>()
    private var _midi_events_by_frame = HashMap<Int, MutableList<MIDIEvent>>()
    private var _event_mutex = Mutex()

    fun place_events(events: List<MIDIEvent>, frame: Int) {
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
        runBlocking {
            this@WaveGenerator._event_mutex.withLock {
                if (!this@WaveGenerator._midi_events_by_frame.containsKey(frame)) {
                    this@WaveGenerator._midi_events_by_frame[frame] = mutableListOf()
                }
                this@WaveGenerator._midi_events_by_frame[frame]!!.add(event)
            }
        }
    }

    fun generate(buffer_size: Int): Pair<ShortArray, List<Pair<Int, Int>>> {
        val initial_array = IntArray(buffer_size * 2)
        val buffer = IntBuffer.wrap(initial_array)
        val pointer_list = mutableListOf<Pair<Int, Int>>()

        var max_frame_value = 0
        var is_empty = true
        for (i in 0 until buffer_size) {
            var left_frame: Int = 0
            var right_frame: Int = 0
            val f = this.frame + i

            if (this._midi_events_by_frame.containsKey(f)) {
                for (event in this._midi_events_by_frame[f]!!) {
                    when (event) {
                        is NoteOn -> {
                            var key_pair = Pair(event.channel, event.get_note())
                            val preset = this.sample_handle_manager.get_preset(event.channel) ?: continue
                            this._active_sample_handles[key_pair] = this.sample_handle_manager.gen_sample_handles(event, preset).toMutableSet()
                        }
                        is NoteOff -> {
                            var key_pair = Pair(event.channel, event.get_note())
                            for (handle in this._active_sample_handles[key_pair] ?: continue) {
                                handle.release_note()
                            }
                        }
                        is NoteOn79 -> {
                            var key_pair = Pair(event.channel, event.index)
                            val preset = this.sample_handle_manager.get_preset(event.channel) ?: continue
                            this._active_sample_handles[key_pair] = this.sample_handle_manager.gen_sample_handles(event, preset).toMutableSet()
                        }
                        is NoteOff79 -> {
                            var key_pair = Pair(event.channel, event.index)
                            for (handle in this._active_sample_handles[key_pair] ?: continue) {
                                handle.release_note()
                            }
                        }
                        is MIDIStop -> {
                            throw KilledException()
                        }
                        is AllSoundOff -> {
                            for ((_, handles) in this._active_sample_handles) {
                                for (handle in handles) {
                                    handle.release_note()
                                }
                            }
                        }
                        is ProgramChange -> {
                            this.sample_handle_manager.change_program(event.channel, event.get_program())
                        }
                        is BankSelect -> {
                            this.sample_handle_manager.select_bank(event.channel, event.value)
                        }
                        is SongPositionPointer -> {
                            var millis = (f - this.frame) * 1_000 / this.sample_handle_manager.sample_rate
                            pointer_list.add(Pair(millis, event.beat))
                        }
                    }
                }
            }

            runBlocking {
                this@WaveGenerator._event_mutex.withLock {
                    this@WaveGenerator._midi_events_by_frame.remove(f)
                }
            }

            val keys_to_pop = mutableSetOf<Pair<Int, Int>>()
            var overlap = 0
            for ((key, sample_handles) in this._active_sample_handles) {
                is_empty = false
                overlap += 1
                val to_kill = mutableSetOf<SampleHandle>()
                for (sample_handle in sample_handles) {
                    val frame_value = sample_handle.get_next_frame()
                    if (frame_value == null) {
                        to_kill.add(sample_handle)
                        continue
                    }

                    // TODO: Implement ROM stereo modes
                    val pan = sample_handle.pan
                    when (sample_handle.stereo_mode and 7) {
                        1 -> { // mono
                            if (pan > 0) {
                                left_frame += frame_value
                                right_frame += (frame_value * (100 - pan.toInt()) / 100)
                            } else if (pan < 0) {
                                left_frame = frame_value * (100 + pan.toInt()) / 100
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
                }

                for (sample_handle in to_kill) {
                    this._active_sample_handles[key]!!.remove(sample_handle)
                }

                if (this._active_sample_handles[key]!!.isEmpty()) {
                    keys_to_pop.add(key)
                }
            }

            for (key in keys_to_pop) {
                this._active_sample_handles.remove(key)
            }

            max_frame_value = max(
                max_frame_value,
                max(
                    abs(right_frame),
                    abs(left_frame)
                )
            )

            buffer.put(right_frame)
            buffer.put(left_frame)
        }

        val mid = Short.MAX_VALUE / 2
        val compression_ratio = if (max_frame_value <= Short.MAX_VALUE) {
            1F
        } else {
            (Short.MAX_VALUE - mid).toFloat() / (max_frame_value - mid).toFloat()
        }

        val compressed_array = ShortArray(initial_array.size) { i: Int ->
            val v = initial_array[i]
            if (compression_ratio >= 1F || (0 - mid .. mid).contains(v)) {
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

        return Pair(compressed_array, pointer_list)
    }

    fun clear() {
        this._active_sample_handles.clear()
        this._midi_events_by_frame.clear()
        this.frame = 0
        this._empty_chunks_count = 0
    }
}