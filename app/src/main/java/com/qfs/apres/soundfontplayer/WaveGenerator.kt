package com.qfs.apres.soundfontplayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh

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

    data class GeneratedSampleChunk(
        var key: Int,
        var smoothing_factor: Float,
        var volume_array: FloatArray,
        var chunk_data: FloatArray
    )

    data class CompoundFrame(
        val value: Float = 0F,
        val volume: Float = 0F,
        val pan: Float = 0F
    )

    var frame = 0
    var kill_frame: Int? = null
    private var _empty_chunks_count = 0
    private var _active_sample_handles = HashMap<Int, ActiveHandleMapItem>()
    private var timeout: Int? = null
    private val core_count = Runtime.getRuntime().availableProcessors()
    private val active_sample_handle_mutex = Mutex()
    private val _cached_frame_weights = HashMap<Int, Float>() // Store 'previous frame's between chunks so smoothing can be accurately applied


    fun generate(): FloatArray {
        val output_array = FloatArray(this.buffer_size * 2)
        this.generate(output_array)
        return output_array
    }

    fun generate(array: FloatArray) {
        val buffer_size = array.size / 2
        if (buffer_size != this.buffer_size) {
            throw InvalidArraySize()
        }

        val first_frame = this.frame
        this.update_active_sample_handles(this.frame)

        if (this._active_sample_handles.isEmpty()) {
            this.frame += this.buffer_size
            throw EmptyException()
        }

        val arrays: Array<HashMap<Int, Pair<Float, Array<CompoundFrame>>>> = runBlocking {
            val tmp = Array(this@WaveGenerator.core_count) { i: Int ->
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
            val initial_array_index = array.size * x / this.core_count

            for ((key, pair) in separated_lines_map) {
                // Apply the volume, pan and low-pass filter
                val (smoothing_factor, uncompiled_array) = pair
                var weight_value: Float? = latest_weights[key] ?: this._cached_frame_weights[key]

                for (i in uncompiled_array.indices) {
                    val frame = uncompiled_array[i]
                    var compiled_frame = if (weight_value == null) {
                        frame.value
                    } else {
                        weight_value + (smoothing_factor * (frame.value - weight_value))
                    }

                    weight_value = compiled_frame
                    compiled_frame *= frame.volume

                    // Adjust manual pan
                    array[initial_array_index + (i * 2)] += compiled_frame * if (frame.pan >= 0f) {
                        1F
                    }  else {
                        1F + frame.pan
                    }

                    array[initial_array_index + (i * 2) + 1] += compiled_frame * if (frame.pan <= 0f) {
                        1F
                    }  else {
                        1F - frame.pan
                    }
                }

                latest_weights[key] = weight_value
            }
        }

        for ((k, v) in latest_weights) {
            if (v == null) {
                continue
            }
            this._cached_frame_weights.put(k, v)
        }

        // Run the tanh() on all cores
        runBlocking {
            val chunk_size = array.size / this@WaveGenerator.core_count
            val tmp = Array(this@WaveGenerator.core_count) { i: Int ->
                val start_index = i * chunk_size
                async(Dispatchers.Default) {
                    for (j in 0 until chunk_size) {
                        array[start_index + j] = tanh(array[start_index + j])
                    }
                }
            }

            Array(tmp.size) { i: Int ->
                tmp[i].await()
            }
        }

        this.frame += this.buffer_size

        if (this.timeout != null && this._empty_chunks_count >= this.timeout!!) {
            throw DeadException()
        }
    }

    private fun gen_partial_int_array(first_frame: Int, sample_index: Int): HashMap<Int, Pair<Float, Array<CompoundFrame>>> {
        val sample_handles_to_use = mutableSetOf<Triple<Int, SampleHandle, Int>>()
        runBlocking {
            this@WaveGenerator.active_sample_handle_mutex.withLock {
                for ((_, item) in this@WaveGenerator._active_sample_handles) {
                    if (item.first_frame >= first_frame + this@WaveGenerator.buffer_size) {
                        continue
                    }

                    val real_index = if (item.first_section > 0) {
                        if ((this@WaveGenerator.core_count - item.sample_handles.size) > sample_index) {
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
                        sample_handle = SampleHandle.copy(item.handle)
                        sample_handle.set_working_frame(start_frame)
                        item.sample_handles[real_index] = Pair(sample_handle, 0)
                    }

                    if (!sample_handle.is_dead) {
                        sample_handles_to_use.add(
                            Triple(
                                item.handle.uuid,
                                sample_handle,
                                if (real_index == 0 && (0 until this@WaveGenerator.buffer_size).contains(item.first_frame - first_frame)) {
                                    (item.first_frame - first_frame) - (this@WaveGenerator.buffer_size * sample_index / this@WaveGenerator.core_count)
                                } else {
                                    0
                                }
                            )
                        )
                    }
                }
            }
        }


        val output = HashMap<Int, Pair<Float, Array<CompoundFrame>>>()
        for ((key, sample_handle, index) in sample_handles_to_use) {
            output[key] = Pair(sample_handle.smoothing_factor, this.populate_partial_int_array(sample_handle, index))
        }
        return output
    }

    private fun populate_partial_int_array(sample_handle: SampleHandle, offset: Int): Array<CompoundFrame> {
        val output = Array<CompoundFrame>(this.buffer_size / this.core_count) {
            CompoundFrame()
        }
        // Assume working_int_array.size % 2 == 0
        val range = if (offset < 0) {
            0 until output.size
        } else {
            offset until output.size
        }

        for (f in range) {
            var frame_value = sample_handle.get_next_frame() ?: break

            // TODO: Implement ROM stereo modes
            val pan: Float = when (this.stereo_mode) {
                StereoMode.Stereo -> when (sample_handle.stereo_mode and 7) {
                    // right
                    2 -> {
                        if (sample_handle.pan > 0F) {
                            sample_handle.pan
                        } else {
                            -1F // Mutes this sample_handle in the right side completely
                        }
                    }
                    // left
                    4 -> {
                        if (sample_handle.pan < 0F) {
                            sample_handle.pan
                        } else {
                            1F // Mutes this sample_handle in the left side completely
                        }
                    }
                    else -> sample_handle.pan
                }

                StereoMode.Mono -> 0F
            }

            // NOTE: It may be insufficient to limit the pan and I rather may need
            // to modify the outgoing pan relatively to the sample_handle.pan
            output[f] = CompoundFrame(
                frame_value.first,
                frame_value.second,
                if (pan < 0f) {
                    max(sample_handle.pan_profile?.get_next() ?: 0F, pan)
                } else if (pan > 0F) {
                    min(sample_handle.pan_profile?.get_next() ?: 0F, pan)
                } else {
                    sample_handle.pan_profile?.get_next() ?: 0F
                }
            )


        }
        if (!sample_handle.is_dead) {
            sample_handle.set_working_frame(sample_handle.working_frame + (this.buffer_size * (this.core_count - 1) / this.core_count))
        }
        return output
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
                remove_set.add(key)
                this._cached_frame_weights.remove(item.handle.uuid)
            }
        }

        for (key in remove_set) {
            this._active_sample_handles.remove(key)
        }

        for (i in 0 until this.core_count) {
            for (j in 0 until this.buffer_size / this.core_count) {
                val working_frame = j + initial_frame + (i * this.buffer_size / this.core_count)
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
        val base_butt_offset = (this.buffer_size / this.core_count) - frame_in_core_chunk

        // then populate the next active frames with upcoming sample handles
        val working_frame = frame_in_core_chunk + initial_frame + (core * this.buffer_size / this.core_count)
        for (handle in handles) {
            val split_handles = Array<Pair<SampleHandle?, Int>>(this.core_count - core) { k: Int ->
                Pair(
                    null,
                    if (k > 0) {
                       handle.working_frame + base_butt_offset + (this.buffer_size * (k - 1) / this.core_count)
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
                        handle.working_frame + base_butt_offset + (this.buffer_size * ((k - 1) + (this.core_count - core)) / this.core_count)
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
