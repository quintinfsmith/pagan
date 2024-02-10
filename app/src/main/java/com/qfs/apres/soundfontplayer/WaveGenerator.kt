package com.qfs.apres.soundfontplayer

import kotlin.math.abs
import kotlin.math.max

class WaveGenerator(val midi_frame_map: FrameMap, val sample_rate: Int, val buffer_size: Int) {
    class EmptyException: Exception()
    class DeadException: Exception()
    class InvalidArraySize: Exception()
    data class ActiveHandleMapItem(
        var first_frame: Int,
        var sample_handle_a: SampleHandle?,
        var sample_handle_b: SampleHandle?
    )
    var frame = 0
    var kill_frame: Int? = null
    private var _empty_chunks_count = 0
    private var _active_sample_handles = HashMap<Int, ActiveHandleMapItem>()
    var timeout: Int? = null

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

        val first_array = IntArray(this.buffer_size)
        val second_array = IntArray(this.buffer_size)

        for ((_, item) in this._active_sample_handles) {
            if (item.first_frame >= first_frame + buffer_size) {
                continue
            }

            if (item.sample_handle_a != null && !item.sample_handle_a!!.is_dead) {
                this.populate_half_int_array(
                    item.sample_handle_a!!,
                    first_array,
                    if ((0 until buffer_size).contains(item.first_frame - first_frame)) {
                        item.first_frame - first_frame
                    } else {
                        0
                    }
                )
            }

           if (item.sample_handle_b != null && !item.sample_handle_b!!.is_dead) {
               this.populate_half_int_array(
                   item.sample_handle_b!!,
                   second_array,
                   if (item.sample_handle_a == null && (0 until buffer_size).contains(item.first_frame - first_frame)) {
                       item.first_frame - first_frame - (buffer_size / 2)
                   } else {
                       0
                   }
               )
           }
        }

        val short_array_a = this.gen_half_short_array(first_array)
        val short_array_b = this.gen_half_short_array(second_array)

        for (i in array.indices) {
            array[i] = if (i >= short_array_a.size) {
                short_array_b[i - short_array_a.size]
            } else {
                short_array_a[i]
            }
        }

        this.frame += this.buffer_size

        if (this.timeout != null && this._empty_chunks_count >= this.timeout!!) {
            throw DeadException()
        }
    }

    private fun populate_half_int_array(sample_handle: SampleHandle, working_int_array: IntArray, offset: Int) {
        // Assume working_int_array.size % 2 == 0
        val first_frame = sample_handle.working_frame
        for (f in offset until working_int_array.size / 2) {
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

            working_int_array[(f * 2)] += right_frame
            working_int_array[(f * 2) + 1] += left_frame
        }
        sample_handle.set_working_frame(sample_handle.working_frame + (this.buffer_size / 2))
    }

    private fun gen_half_short_array(int_array: IntArray): ShortArray {
        var max_frame_value = 0
        for (v in int_array) {
            max_frame_value = max(max_frame_value, abs(v))
        }

        val mid = Short.MAX_VALUE / 2
        val compression_ratio = if (max_frame_value <= Short.MAX_VALUE) {
            1.0
        } else {
            (Short.MAX_VALUE - mid).toDouble() / (max_frame_value - mid).toDouble()
        }

        val array = ShortArray(int_array.size) { i: Int ->
            val v = int_array[i]
            if (compression_ratio >= 1.0 || (0 - mid <= v && v <= mid)) {
                v.toShort()
            } else if (v > mid) {
                (mid + ((v - mid).toFloat() * compression_ratio).toInt()).toShort()
            } else {
                (((v + mid).toFloat() * compression_ratio).toInt() - mid).toShort()
            }
        }
        return array
    }

    private fun update_active_frames(initial_frame: Int) {
        // First check for, and remove dead sample handles
        val remove_set = mutableSetOf<Int>()
        for ((uuid, item) in this._active_sample_handles) {
            if (item.first_frame >= initial_frame) {
                continue
            }

            if ((item.sample_handle_a != null && item.sample_handle_a!!.is_dead) || (item.sample_handle_b != null && item.sample_handle_b!!.is_dead)) {
                remove_set.add(uuid)
            }
        }

        for (key in remove_set) {
            this._active_sample_handles.remove(key)
        }

        // then populate the next active frames with upcoming sample handles
        for (f in 0 until buffer_size / 2) {
            val working_frame = f + initial_frame
            val handles = this.midi_frame_map.get_new_handles(working_frame) ?: continue
            val butt_offset = (this.buffer_size / 2) - f

            for (handle in handles) {
                val new_handle_a = SampleHandle(handle)
                new_handle_a.release_frame = handle.release_frame

                val new_handle_b = SampleHandle(handle)
                new_handle_b.release_frame = handle.release_frame
                new_handle_b.set_working_frame(butt_offset)

                this._active_sample_handles[new_handle_a.uuid] = ActiveHandleMapItem(
                    working_frame,
                    new_handle_a,
                    new_handle_b
                )
            }
        }

        /*
         If a sample is activated in the second half,
         it will be split such that the first chunk of the sample is treated as 'sample_b'
         and the second chunk is treated as sample_a of the next iteration
         */
        for (f in 0 until (buffer_size / 2)) {
            val working_frame = f + initial_frame + (buffer_size / 2)
            val butt_offset = (this.buffer_size / 2) - f
            val handles = this.midi_frame_map.get_new_handles(working_frame) ?: continue
            for (handle in handles) {
                val new_handle_a = SampleHandle(handle)
                new_handle_a.release_frame = handle.release_frame
                this._active_sample_handles[new_handle_a.uuid] = ActiveHandleMapItem(working_frame, null, new_handle_a)

                val new_handle_b = SampleHandle(handle)
                new_handle_b.release_frame = handle.release_frame
                new_handle_b.set_working_frame(butt_offset)
                this._active_sample_handles[new_handle_b.uuid] = ActiveHandleMapItem(initial_frame + buffer_size, new_handle_b, null)
            }
        }
    }

    fun clear() {
        this.kill_frame = null
        this._active_sample_handles.clear()
        this.frame = 0
        this._empty_chunks_count = 0
    }

    fun set_position(frame: Int) {
        this.clear()
        this.frame = frame
    }
}