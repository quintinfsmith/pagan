package com.qfs.apres.soundfontplayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.max

class WaveGenerator(val midi_frame_map: FrameMap, val sample_rate: Int, val buffer_size: Int) {
    class EmptyException: Exception()
    class DeadException: Exception()
    class InvalidArraySize: Exception()

    data class ActiveHandleMapItem(
        var first_frame: Int,
        val sample_handles: Array<SampleHandle>,
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
        this.update_active_frames(this.frame)

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
        val elbow = (Short.MAX_VALUE.toDouble() * .75)
        var max_frame_value = 0
        arrays.forEachIndexed { i: Int, input_array: IntArray ->
            input_array.forEachIndexed { x: Int, v: Int ->
                max_frame_value = max(abs(v), max_frame_value)
            }
        }

        val factor = if (max_frame_value >= Short.MAX_VALUE) {
            (max_frame_value.toDouble() - elbow) / (Short.MAX_VALUE - elbow)
        } else {
            1.0
        }
        // -------------------

        var offset = 0
        arrays.forEachIndexed { i: Int, input_array: IntArray ->
            input_array.forEachIndexed { x: Int, v: Int ->
                array[offset++] = if (factor <= 1.0) {
                    v.toShort()
                } else if (v > elbow) {
                    (elbow + ((v - elbow) / factor)).toInt().toShort()
                } else if (v < 0 - elbow) {
                    (0 - elbow - ((v + elbow) / factor)).toInt().toShort()
                } else {
                    v.toShort()
                }
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

            val sample_handle = item.sample_handles[real_index]
            if (!sample_handle.is_dead) {
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

    private fun update_active_frames(initial_frame: Int) {
        // First check for, and remove dead sample handles
        val remove_set = mutableSetOf<Int>()
        for ((uuid, item) in this._active_sample_handles) {
            if (item.first_frame >= initial_frame) {
                continue
            }

            for (handle in item.sample_handles) {
                if (handle.is_dead) {
                    remove_set.add(uuid)
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
                val handles = this.midi_frame_map.get_new_handles(working_frame) ?: continue

                this.activate_sample_handles(handles, i, j, initial_frame)
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
            val split_handles = Array(this.core_count - core) { k: Int ->
                val new_handle = SampleHandle(handle)
                new_handle.release_frame = handle.release_frame
                if (k > 0) {
                   new_handle.set_working_frame(
                       handle.working_frame + base_butt_offset + (this.buffer_size * (k - 1) / this.core_count)
                   )
                } else {
                    new_handle.set_working_frame(handle.working_frame)
                }
                new_handle
            }

            this._active_sample_handles[split_handles.first().uuid] = ActiveHandleMapItem(
                working_frame,
                split_handles,
                core
            )

            if (core > 0) {
                val split_handles_b = Array(core) { k: Int ->
                    val new_handle = SampleHandle(handle)
                    new_handle.release_frame = handle.release_frame
                    new_handle.set_working_frame(
                        handle.working_frame + base_butt_offset + (this.buffer_size * ((k - 1) + (this.core_count - core)) / this.core_count)
                    )
                    new_handle
                }

                this._active_sample_handles[split_handles_b.first().uuid] = ActiveHandleMapItem(
                    initial_frame + buffer_size,
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