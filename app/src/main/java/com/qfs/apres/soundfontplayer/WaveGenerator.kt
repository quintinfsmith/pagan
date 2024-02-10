package com.qfs.apres.soundfontplayer

import android.util.Log
import kotlin.math.abs
import kotlin.math.max

class WaveGenerator(val midi_frame_map: FrameMap, val sample_rate: Int, val buffer_size: Int) {
    class EmptyException: Exception()
    class DeadException: Exception()
    class InvalidArraySize: Exception()
    var frame = 0
    var kill_frame: Int? = null
    private var _empty_chunks_count = 0
    private var _active_sample_handles = HashMap<Int, Triple<Int, SampleHandle, SampleHandle>>()
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

        for ((_, triple) in this._active_sample_handles) {
            val (first_sample_frame, sample_handle_a, sample_handle_b) = triple
            if (first_sample_frame >= first_frame + buffer_size) {
                continue
            }


            if (!sample_handle_a.is_dead) {
                var offset = max(0, first_sample_frame - first_frame)

                this.populate_half_int_array(sample_handle_a, first_array, offset)
            }

            if (!sample_handle_b.is_dead) {
                var offset = first_sample_frame - first_frame
                this.populate_half_int_array(sample_handle_b, second_array, offset)
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
        Log.d("AAA", "$offset // ${working_int_array.size / 2}")
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
        for ((uuid, triple) in this._active_sample_handles) {
            if (triple.second.is_dead && triple.third.is_dead) {
                remove_set.add(uuid)
            }
        }

        for (key in remove_set) {
            this._active_sample_handles.remove(key)
        }

        // then populate the next active frames with upcoming sample handles
        for (f in initial_frame until initial_frame + buffer_size) {
            val handles = this.midi_frame_map.get_new_handles(f) ?: continue
            for (handle in handles) {
                val new_handle_a = SampleHandle(handle)
                val new_handle_b = SampleHandle(handle)
                new_handle_a.release_frame = handle.release_frame
                new_handle_b.release_frame = handle.release_frame

                try {
                    // TODO: THIS HERE!. the offsets are the problem
                    new_handle_b.set_working_frame((this.buffer_size / 2) - (f - initial_frame))
                } catch (e: Exception){
                    throw e
                }
                this._active_sample_handles[handle.uuid] = Triple(f, new_handle_a, new_handle_b)
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