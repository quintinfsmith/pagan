package com.qfs.apres.soundfontplayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
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

    var frame = 0
    var kill_frame: Int? = null
    private var _empty_chunks_count = 0
    private var _active_sample_handles = HashMap<Int, ActiveHandleMapItem>()
    private var timeout: Int? = null
    private val core_count = Runtime.getRuntime().availableProcessors()

    fun generate(): ShortArray {
        val output_array = ShortArray(this.buffer_size * 2)
        this.generate(output_array)
        return output_array
    }

    fun generate(array: ShortArray) {
        val buffer_size = array.size / 2
        if (buffer_size != this.buffer_size) {
            throw InvalidArraySize()
        }

        val first_frame = this.frame
        this.update_active_sample_handles(this.frame)

        if (this.frame >= this.midi_frame_map.get_size()) {
            throw DeadException()
        }

        if (this._active_sample_handles.isEmpty()) {
            this.frame += this.buffer_size
            throw EmptyException()
        }

        val arrays: Array<IntArray> = runBlocking {
            val tmp = Array(this@WaveGenerator.core_count) { i: Int ->
                async(Dispatchers.Default) {
                    this@WaveGenerator.gen_partial_int_array(first_frame, i)
                }
            }

            Array(tmp.size) { i: Int ->
                tmp[i].await()
            }
        }

        // Volume Attenuation----
        val MAX = Short.MAX_VALUE * 3 / 4
        var max_frame_value = MAX
        arrays.forEachIndexed { i: Int, input_array: IntArray ->
            input_array.forEachIndexed { j: Int, v: Int ->
                max_frame_value = max(abs(v), max_frame_value)
            }
        }

        // Note: |MIN_VALUE| is one greater than MAX_VALUE, so use the smaller MAX_VALUE
        val factor = if (max_frame_value > MAX) {
            max_frame_value.toFloat() / MAX.toFloat()
        } else {
            1F
        }
        // -------------------

        var offset = 0
        arrays.forEachIndexed { i: Int, input_array: IntArray ->
            input_array.forEachIndexed { x: Int, v: Int ->
                array[offset++] = (if (factor > 1F) {
                    v / factor
                } else {
                    v
                }).toShort()
            }
        }

        this.frame += this.buffer_size

        if (this.timeout != null && this._empty_chunks_count >= this.timeout!!) {
            throw DeadException()
        }
    }

    private fun gen_partial_int_array(first_frame: Int, sample_index: Int): IntArray {
        val int_array = IntArray(this.buffer_size * 2 / this.core_count)
        for ((_, item) in this._active_sample_handles) {
            if (item.first_frame >= first_frame + this.buffer_size) {
                continue
            }

            val real_index = if (item.first_section > 0) {
                if ((this.core_count - item.sample_handles.size) > sample_index) {
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
                // Ignore Samples in Right for mono mode
                if (this.stereo_mode == StereoMode.Mono && sample_handle.stereo_mode and 7 == 4 && item.sample_handles.size > 1) {
                    continue
                }

                this.populate_partial_int_array(
                    sample_handle,
                    int_array,
                    if (real_index == 0 && (0 until this.buffer_size).contains(item.first_frame - first_frame)) {
                        (item.first_frame - first_frame) - (this.buffer_size * sample_index / this.core_count)
                    } else {
                        0
                    }
                )
            }
        }

        return int_array
    }

    private fun populate_partial_int_array(sample_handle: SampleHandle, working_int_array: IntArray, offset: Int) {
        // Assume working_int_array.size % 2 == 0
        val range = if (offset < 0) {
            0 until (working_int_array.size / 2)
        } else {
            offset until working_int_array.size / 2
        }

        for (f in range) {
            val frame_value = sample_handle.get_next_frame() ?: break

            // TODO: Implement ROM stereo modes
            val pan = sample_handle.pan
            val (left_frame, right_frame) = when (sample_handle.stereo_mode and 7) {
                1 -> { // mono
                    if (this.stereo_mode == StereoMode.Stereo && pan != 0F) {
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
                    if (this.stereo_mode == StereoMode.Stereo) {
                        Pair(
                            0,
                            if (pan > 0F) {
                                (frame_value * (100 - pan.toInt())) / 100
                            } else {
                                frame_value
                            }
                        )
                    } else { // MONO
                        Pair(
                            frame_value,
                            frame_value
                        )
                    }
                }

                4 -> { // left
                    if (this.stereo_mode == StereoMode.Stereo) {
                        Pair(
                            if (pan < 0F) {
                                (frame_value * (100 + pan.toInt())) / 100
                            } else {
                                frame_value
                            },
                            0
                        )
                    } else { // MONO (allowed if there is ONLY a left sample for an instrument
                        Pair(
                            frame_value,
                            frame_value
                        )
                    }
                }

                else -> Pair(0,0)
            }

            working_int_array[(f * 2)] += when (sample_handle.stereo_mode and 7) {
                1, 2 -> right_frame
                else -> 0
            }

            working_int_array[(f * 2) + 1] += when (sample_handle.stereo_mode and 7) {
                1, 4 -> left_frame
                else -> 0
            }
        }

        sample_handle.set_working_frame(sample_handle.working_frame + (this.buffer_size * (this.core_count - 1) / this.core_count))
    }

    private fun update_active_sample_handles(initial_frame: Int) {
        // First check for, and remove dead sample handles
        val remove_set = mutableSetOf<Int>()
        for ((key, item) in this._active_sample_handles) {
            if (item.first_frame >= initial_frame) {
                continue
            }

            for ((handle, _) in item.sample_handles) {
                if (handle != null && handle.is_dead) {
                    remove_set.add(key)
                    break
                }
            }
        }

        for (key in remove_set) {
            this._active_sample_handles.remove(key)
        }

        for (i in 0 until this.core_count) {
            for (j in 0 until this.buffer_size / this.core_count) {
                val working_frame = j + initial_frame + (i * this.buffer_size / this.core_count)
                val handle_tracks = this.midi_frame_map.get_new_handles(working_frame) ?: continue
                for (handles in handle_tracks) {
                    this.activate_sample_handles(handles, i, j, initial_frame)
                }
            }
        }
    }

    /* Add handles that would be active but aren't because of a jump in position */
    private fun activate_active_handles(frame: Int) {
        val handles = this.midi_frame_map.get_active_handles(frame)
        for ((first_frame, handle) in handles) {
            if (first_frame == frame) {
                continue
            }

            handle.set_working_frame(frame - first_frame)
            this.activate_sample_handles(mutableSetOf(handle), 0, 0, frame)
        }
    }

    fun activate_sample_handles(handles: Set<SampleHandle>, core: Int, frame_in_core_chunk: Int, initial_frame: Int) {
        val base_butt_offset = (this.buffer_size / this.core_count) - frame_in_core_chunk

        // then populate the next active frames with upcoming sample handles
        val working_frame = frame_in_core_chunk + initial_frame + (core * this.buffer_size / this.core_count)
        for (handle in handles) {
            val split_handles = Array<Pair<SampleHandle?, Int>>(this.core_count - core) { k: Int ->
                //val new_handle = SampleHandle(handle)
                //new_handle.release_frame = handle.release_frame
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
                    //val new_handle = SampleHandle(handle)
                    //new_handle.release_frame = handle.release_frame
                    Pair(
                        null,
                        handle.working_frame + base_butt_offset + (this.buffer_size * ((k - 1) + (this.core_count - core)) / this.core_count)
                    )
                }

                this._active_sample_handles[(2 * handle.uuid) + 1] = ActiveHandleMapItem(
                    initial_frame + buffer_size,
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