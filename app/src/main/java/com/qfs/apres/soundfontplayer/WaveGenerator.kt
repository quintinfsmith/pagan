package com.qfs.apres.soundfontplayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    var frame = 0
    var kill_frame: Int? = null
    private var _empty_chunks_count = 0
    private var _active_sample_handles = HashMap<Int, ActiveHandleMapItem>()
    private var timeout: Int? = null
    private val core_count = Runtime.getRuntime().availableProcessors()
    private val active_sample_handle_mutex = Mutex()


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

        val arrays: Array<FloatArray> = runBlocking {
            val tmp = Array(this@WaveGenerator.core_count) { i: Int ->
                async(Dispatchers.Default) {
                    this@WaveGenerator.gen_partial_int_array(first_frame, i)
                }
            }

            Array(tmp.size) { i: Int ->
                tmp[i].await()
            }
        }

        var offset = 0
        for (input_array in arrays) {
            for (v in input_array) {
                array[offset++] += v
            }
        }

        this.frame += this.buffer_size

        if (this.timeout != null && this._empty_chunks_count >= this.timeout!!) {
            throw DeadException()
        }
    }

    private fun gen_partial_int_array(first_frame: Int, sample_index: Int): FloatArray {
        val sample_handles_to_use = mutableSetOf<Pair<SampleHandle, Int>>()
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
                            Pair(
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

        val output = FloatArray(this.buffer_size * 2 / this.core_count) {
            0f
        }
        for ((sample_handle, index) in sample_handles_to_use) {
            // FIXME: This is wonky. not sure whats up
            // Ignore Samples in Right for mono mode
            // if (this.stereo_mode == StereoMode.Mono && sample_handle.stereo_mode and 7 == 4 && item.sample_handles.size > 1) {
            //     continue
            // }

            this.populate_partial_int_array(sample_handle, output, index)
        }


        return output
    }

    private fun populate_partial_int_array(sample_handle: SampleHandle, working_int_array: FloatArray, offset: Int) {
        // Assume working_int_array.size % 2 == 0
        val range = if (offset < 0) {
            0 until (working_int_array.size / 2)
        } else {
            offset until working_int_array.size / 2
        }
        for (f in range) {
            var frame_value = sample_handle.get_next_frame() ?: break

            // TODO: Implement ROM stereo modes
            val pan = sample_handle.pan
            val (left_frame, right_frame) = when (this.stereo_mode) {
                StereoMode.Stereo -> when (sample_handle.stereo_mode and 7) {
                    1 -> { // mono
                        if (pan != 0F) {
                            if (pan > 0) {
                                Pair(
                                    frame_value,
                                    (frame_value * (100 - pan.toInt()) / 100)
                                )
                            } else {
                                Pair(
                                    frame_value * (100 + pan.toInt()) / 100,
                                    frame_value
                                )
                            }
                        } else {
                            Pair(
                                frame_value,
                                frame_value
                            )
                        }
                    }

                    2 -> { // right
                        Pair(
                            0,
                            if (pan > 0F) {
                                (frame_value * (100 - pan.toInt())) / 100
                            } else {
                                frame_value
                            }
                        )
                    }

                    4 -> { // left
                        Pair(
                            if (pan < 0F) {
                                (frame_value * (100 + pan.toInt())) / 100
                            } else {
                                frame_value
                            },
                            0
                        )
                    }

                    else -> Pair(0,0)
                }
                StereoMode.Mono -> {
                    when (sample_handle.stereo_mode and 7) {
                        1, 2, 4 -> {
                            Pair(
                                frame_value,
                                frame_value
                            )
                        }
                        else -> Pair(0, 0)
                    }
                }
            }

            val right_value = when (sample_handle.stereo_mode and 7) {
                1, 2 -> right_frame.toFloat() / (Short.MAX_VALUE + 1).toFloat()
                else -> 0f
            }

            working_int_array[(f * 2)] += right_value

            val left_value = when (sample_handle.stereo_mode and 7) {
                1, 4 -> left_frame.toFloat() / (Short.MAX_VALUE + 1).toFloat()
                else -> 0f
            }
            working_int_array[(f * 2) + 1] += left_value
        }

        if (!sample_handle.is_dead) {
            sample_handle.set_working_frame(sample_handle.working_frame + (this.buffer_size * (this.core_count - 1) / this.core_count))
        }
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
            // increase sample's volume so it take up the full range -1 .. 1 (the sample may be quieter)
            val handle_volume_factor = handle.max_frame_value().toFloat() / Short.MAX_VALUE.toFloat()
            handle.volume /= handle_volume_factor


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
