package com.qfs.apres.soundfontplayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class WaveGenerator(val midi_frame_map: FrameMap, val sample_rate: Int, val buffer_size: Int, var stereo_mode: StereoMode = StereoMode.Stereo) {
    enum class StereoMode {
        Mono,
        Stereo
    }
    class EmptyException: Exception()
    class DeadException: Exception()
    class InvalidArraySize: Exception()

    data class ActiveHandleMapItem(
        var first_frame: Int,
        val handle: SampleHandle,
        val sample_handles: Array<Pair<SampleHandle?, Int>>,
        val first_section: Int
    )

    data class CompoundFrame(
        val value: Float = 0F,
        val volume: Float = 1F,
        val balance: Pair<Float, Float> = Pair(1F, 1F)
    )

    var frame = 0
    var kill_frame: Int? = null
    private var _empty_chunks_count = 0
    private var _active_sample_handles = HashMap<Int, ActiveHandleMapItem>()
    private var timeout: Int? = null
    // Using more processes than counters just in case 1 thread holds up the rest.
    //private val process_count = Runtime.getRuntime().availableProcessors() * 8
    private val process_count = 2
    private val active_sample_handle_mutex = Mutex()
    private val _cached_frame_weights = HashMap<Int, Float>() // Store 'previous frame's between chunks so smoothing can be accurately applied


    external fun merge_arrays(arrays: Array<FloatArray>, frame_count: Int): FloatArray
    external fun tanh_array(array: FloatArray): FloatArray
    fun generate(): FloatArray {
        val working_array = FloatArray(this.buffer_size * 2)
        val start_ts = System.nanoTime()

        val first_frame = this.frame
        this.update_active_sample_handles(this.frame)

        if (this._active_sample_handles.isEmpty()) {
            this.frame += this.buffer_size
            throw EmptyException()
        }

        val arrays: Array<HashMap<Int, Pair<Float, FloatArray>>> = runBlocking {
            val tmp = Array(this@WaveGenerator.process_count) { i: Int ->
                async(Dispatchers.Default) {
                    this@WaveGenerator.gen_partial_int_array(first_frame, i)
                }
            }

            Array(tmp.size) { i: Int ->
                tmp[i].await()
            }
        }

        // NOTE: We can't separate the smoothing function between coroutines,
        // smoothing is a function series.
        val latest_weights = HashMap<Int, Float?>()
        for (x in arrays.indices) {
            val separated_lines_map = arrays[x]
            val initial_array_index = working_array.size * x / this.process_count
            val keys = separated_lines_map.keys.toList()
            val arrays_to_merge = Array(keys.size) { i: Int ->
                separated_lines_map[keys[i]]!!.second
            }
            val merged_array = merge_arrays(arrays_to_merge, this.buffer_size / this.process_count)
            for (i in 0 until merged_array.size / 2) {
                working_array[initial_array_index + (i * 2)] = merged_array[i * 2]
                working_array[initial_array_index + (i * 2) + 1] = merged_array[(i * 2) + 1]
            }

        }

        for ((k, v) in latest_weights) {
            if (v == null) {
                continue
            }
            this._cached_frame_weights.put(k, v)
        }

        val output_array = this.tanh_array(working_array)

        this.frame += this.buffer_size

        if (this.timeout != null && this._empty_chunks_count >= this.timeout!!) {
            throw DeadException()
        }

        val delta = 1000000 / (System.nanoTime() - start_ts).toFloat()
        val max_delta = this.buffer_size.toFloat() / this.sample_rate.toFloat()
        // println("---GEN TIME: $delta | $max_delta")

        return output_array
    }

    private fun gen_partial_int_array(first_frame: Int, sample_index: Int): HashMap<Int, Pair<Float, FloatArray>> {
        val sample_handles_to_use = mutableSetOf<Triple<Int, SampleHandle, Int>>()
        runBlocking {
            this@WaveGenerator.active_sample_handle_mutex.withLock {
                for ((_, item) in this@WaveGenerator._active_sample_handles) {
                    if (item.first_frame >= first_frame + this@WaveGenerator.buffer_size) {
                        continue
                    }

                    val real_index = if (item.first_section > 0) {
                        if ((this@WaveGenerator.process_count - item.sample_handles.size) > sample_index) {
                            continue
                        }
                        sample_index - item.first_section
                    } else {
                        if (item.sample_handles.size <= sample_index) {
                            continue
                        }
                        sample_index
                    }

                    var (sample_handle, start_frame) = item.sample_handles[real_index]
                    if (sample_handle == null) {
                        sample_handle = item.handle.copy()
                        sample_handle.set_working_frame(start_frame)
                        item.sample_handles[real_index] = Pair(sample_handle, 0)
                    }

                    if (!sample_handle.is_dead) {
                        sample_handles_to_use.add(
                            Triple(
                                item.handle.uuid,
                                sample_handle,
                                if (real_index == 0 && (0 until this@WaveGenerator.buffer_size).contains(item.first_frame - first_frame)) {
                                    (item.first_frame - first_frame) - (this@WaveGenerator.buffer_size * sample_index / this@WaveGenerator.process_count)
                                } else {
                                    0
                                }
                            )
                        )
                    }
                }
            }
        }

        val output = HashMap<Int, Pair<Float, FloatArray>>()
        for ((key, sample_handle, index) in sample_handles_to_use) {
            output[key] = Pair(sample_handle.smoothing_factor, this.populate_partial_int_array(sample_handle, index))
        }

        return output
    }

    private fun populate_partial_int_array(sample_handle: SampleHandle, offset: Int): FloatArray {
        val output_size = this.buffer_size / this.process_count

        val chunk = sample_handle.get_next_frames(max(offset, 0), output_size)
        if (!sample_handle.is_dead) {
            sample_handle.set_working_frame(sample_handle.working_frame + (this.buffer_size * (this.process_count - 1) / this.process_count))
        }

        return chunk
    }

    private fun update_active_sample_handles(initial_frame: Int) {
        // First check for, and remove dead sample handles
        val remove_set = mutableSetOf<Int>()
        for ((key, item) in this._active_sample_handles) {
            if (item.first_frame >= initial_frame) {
                continue
            }

            var dead_count = 0
            for ((handle, _) in item.sample_handles) {
                if (handle != null && handle.is_dead) {
                    dead_count += 1
                }
            }
            if (dead_count == item.sample_handles.size) {
                for ((handle, _) in item.sample_handles) {
                    if (handle != null && handle.is_dead) {
                        handle.destroy()
                    }
                }
                remove_set.add(key)
                this._cached_frame_weights.remove(item.handle.uuid)
            }
        }

        for (key in remove_set) {
            this._active_sample_handles.remove(key)
        }

        for (i in 0 until this.process_count) {
            for (j in 0 until this.buffer_size / this.process_count) {
                val working_frame = j + initial_frame + (i * this.buffer_size / this.process_count)
                val handles = this.midi_frame_map.get_new_handles(working_frame) ?: continue
                this.activate_sample_handles(handles, i, j, initial_frame)
            }
        }

        if (this._active_sample_handles.isEmpty() && !this.midi_frame_map.has_frames_remaining(initial_frame)) {
            throw DeadException()
        }
    }

    /* Add handles that would be active but aren't because of a jump in position */
    private fun activate_active_handles(frame: Int) {
        val handles = this.midi_frame_map.get_active_handles(frame).toList()
        val handles_adj: MutableList<SampleHandle> = mutableListOf()
        for ((first_frame, handle) in handles) {
            if (first_frame == frame) {
                continue
            }
            handle.set_working_frame(frame - first_frame)
            handles_adj.add(handle)
        }

        this.activate_sample_handles(handles_adj.toSet(), 0, 0, frame)
    }

    fun activate_sample_handles(handles: Set<SampleHandle>, core: Int, frame_in_core_chunk: Int, initial_frame: Int) {
        val base_butt_offset = (this.buffer_size / this.process_count) - frame_in_core_chunk

        // then populate the next active frames with upcoming sample handles
        val working_frame = frame_in_core_chunk + initial_frame + (core * this.buffer_size / this.process_count)
        for (handle in handles) {
            val split_handles = Array<Pair<SampleHandle?, Int>>(this.process_count - core) { k: Int ->
                Pair(
                    null,
                    if (k > 0) {
                       handle.working_frame + base_butt_offset + (this.buffer_size * (k - 1) / this.process_count)
                    } else {
                       handle.working_frame
                    }
                )
            }

            this._active_sample_handles[2 * handle.uuid] = ActiveHandleMapItem(
                working_frame,
                handle,
                split_handles,
                core
            )

            if (core > 0) {
                val split_handles_b = Array<Pair<SampleHandle?, Int>>(core) { k: Int ->
                    Pair(
                        null,
                        handle.working_frame + base_butt_offset + (this.buffer_size * ((k - 1) + (this.process_count - core)) / this.process_count)
                    )
                }

                this._active_sample_handles[(2 * handle.uuid) + 1] = ActiveHandleMapItem(
                    initial_frame + this.buffer_size,
                    handle,
                    split_handles_b,
                    0
                )
            }
        }
    }

    fun clear() {
        this.kill_frame = null
        this._active_sample_handles.clear()
        this.frame = 0
        this._empty_chunks_count = 0
    }

    fun set_position(frame: Int, look_back: Boolean = false) {
        this.clear()
        if (look_back) {
          this.activate_active_handles(frame)
        }
        this.frame = frame
    }
}
