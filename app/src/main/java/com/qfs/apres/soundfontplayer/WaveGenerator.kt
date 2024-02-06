package com.qfs.apres.soundfontplayer

import kotlin.math.abs
import kotlin.math.max

class WaveGenerator(var sample_handle_manager: SampleHandleManager, val midi_frame_map: FrameMap) {
    class KilledException: Exception()
    class EmptyException: Exception()
    class DeadException: Exception()
    class EventInPastException: Exception()
    var frame = 0
    var kill_frame: Int? = null
    private var _empty_chunks_count = 0
    private var _active_sample_handles = HashMap<Int, Pair<Int, SampleHandle>>()
    private var _working_int_array = IntArray(sample_handle_manager.buffer_size * 2)
    val cached_chunks = HashMap<Int, ShortArray>()

    fun generate(buffer_size: Int): ShortArray {
        val output_array = ShortArray(buffer_size * 2)
        this.generate(output_array)
        return output_array
    }

    fun generate(): ShortArray {
        return this.generate(this.sample_handle_manager.buffer_size)
    }

    fun generate(array: ShortArray) {
        val buffer_size = array.size / 2

        if (this.cached_chunks.containsKey(this.frame)) {
            val cached_chunk = this.cached_chunks[this.frame]!!
            cached_chunk.copyInto(array)
            this.set_position(this.frame + buffer_size)
            return
        }

        if (this.kill_frame != null && this.kill_frame!! <= this.frame) {
            throw KilledException()
        }

        val first_frame = this.frame
        this.update_active_frames(this.frame, buffer_size)

        if (this._active_sample_handles.isEmpty()) {
            this.frame += buffer_size
            throw EmptyException()
        }

        for (i in this._working_int_array.indices) {
            this._working_int_array[i] = 0
        }

        var max_frame_value = 0
        var is_empty = true

        for ((uuid, pair) in this._active_sample_handles) {
            val (first_sample_frame, sample_handle) = pair
            if (sample_handle.is_dead || (first_sample_frame >= first_frame + buffer_size)) {
                continue
            }

            is_empty = false
            for (f in max(first_sample_frame, first_frame) until first_frame + buffer_size) {
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

                val buffer_index = (f - first_frame) * 2
                this._working_int_array[buffer_index] += right_frame
                this._working_int_array[buffer_index + 1] += left_frame
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

    private fun update_active_frames(initial_frame: Int, buffer_size: Int) {
        // First check for, and remove dead sample handles
        val remove_set = mutableSetOf<Int>()
        for ((uuid, pair) in this._active_sample_handles) {
            val (_, sample_handle) = pair
            if (sample_handle.is_dead) {
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
                this._active_sample_handles[handle.uuid] = Pair(f, handle)
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

    fun set_active_handles() {
        this._active_sample_handles.clear()
        val new_handles = this.midi_frame_map.get_active_handles(this.frame)
        for ((frame, handle) in new_handles) {
            handle.set_working_frame(this.frame - frame)
            this._active_sample_handles[handle.uuid] = Pair(frame, handle)
        }
    }

    fun cache_chunk(start_frame: Int) {
        if (this.frame != start_frame) {
            this.set_position(start_frame)
            this.set_active_handles()
        }

        this.cached_chunks[start_frame] = try {
            this.generate(this.sample_handle_manager.buffer_size)
        } catch (e: EmptyException) {
            ShortArray(this.sample_handle_manager.buffer_size * 2) { 0 }
        } catch (e: KilledException) {
            return
        } catch (e: DeadException) {
            return
        }
    }

    fun decache_range(range: IntRange) {
        for (i in range) {
            this.cached_chunks.remove(i)
        }
    }
}