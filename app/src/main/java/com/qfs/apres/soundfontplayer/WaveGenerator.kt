package com.qfs.apres.soundfontplayer

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
        val merge_keys: IntArray
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


    external fun merge_arrays(
        arrays: Array<FloatArray>,
        frame_count: Int,
        merge_keys: Array<IntArray>,
        profile_ptrs: LongArray,
        profile_info: IntArray,
        profile_keys: IntArray
    ): FloatArray
    external fun tanh_array(array: FloatArray): FloatArray
    fun generate(): FloatArray {
        val working_array = FloatArray(this.buffer_size * 2)
        val start_ts = System.nanoTime()

        val first_frame = this.frame
        this.update_active_sample_handles(this.frame)

        if (this._active_sample_handles.isEmpty()) {
            this.frame += this.buffer_size
            for ( (_, _, buffer) in this.midi_frame_map.get_effect_buffers()) {
                buffer.set_frame(this.frame)
            }
            throw EmptyException()
        }

        val separated_lines_map: HashMap<Int, Pair<FloatArray, IntArray>> = this@WaveGenerator.generate_sample_arrays(first_frame)

        val keys = separated_lines_map.keys.toList()
        val arrays_to_merge = Array(keys.size) { i: Int ->
            separated_lines_map[keys[i]]!!.first
        }
        val layers = Array(keys.size) { i: Int ->
            separated_lines_map[keys[i]]!!.second
        }

        val profiles = this.midi_frame_map.get_effect_buffers()
        val merged_array = merge_arrays(
            arrays_to_merge,
            this.buffer_size,
            layers,
            LongArray(profiles.size) { profiles[it].third.ptr },
            IntArray(profiles.size) { profiles[it].first },
            IntArray(profiles.size) { profiles[it].second }
        )

        for (i in 0 until merged_array.size / 2) {
            working_array[(i * 2)] = merged_array[i * 2]
            working_array[(i * 2) + 1] = merged_array[(i * 2) + 1]
        }

        val output_array = this.tanh_array(working_array)

        this.frame += this.buffer_size

        if (this.timeout != null && this._empty_chunks_count >= this.timeout!!) {
            throw DeadException()
        }

        //val delta = 1000000 / (System.nanoTime() - start_ts).toFloat()
        //val max_delta = this.buffer_size.toFloat() / this.sample_rate.toFloat()
        // println("---GEN TIME: $delta | $max_delta")

        return output_array
    }

    private fun generate_sample_arrays(first_frame: Int): HashMap<Int, Pair<FloatArray, IntArray>> {
        val sample_handles_to_use = mutableSetOf<Triple<Int, Pair<SampleHandle, IntArray>, Int>>()
        for ((_, item) in this._active_sample_handles) {
            if (item.first_frame >= first_frame + this.buffer_size) {
                continue
            }
            if (!item.handle.is_dead) {
                sample_handles_to_use.add(
                    Triple(
                        item.handle.uuid,
                        Pair(
                            item.handle,
                            item.merge_keys
                        ),
                        if ((0 until this@WaveGenerator.buffer_size).contains(item.first_frame - first_frame)) {
                            item.first_frame - first_frame
                        } else {
                            0
                        }
                    )
                )
            }
        }

        val output = HashMap<Int, Pair<FloatArray, IntArray>>()
        for ((key, handle_pair, left_pad) in sample_handles_to_use) {
            val (sample_handle, merge_keys) = handle_pair
            output[key] = Pair(
                sample_handle.get_next_frames(left_pad, this.buffer_size),
                merge_keys
            )
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

            if (item.handle.is_dead) {
                remove_set.add(key)
            }
        }

        for (key in remove_set) {
            this._active_sample_handles.remove(key)?.handle?.destroy()
        }

        for (i in 0 until this.buffer_size) {
            this.midi_frame_map.get_new_handles(initial_frame + i)?.let {
                this.activate_sample_handles(it, initial_frame, i)
            }
        }

        if (this._active_sample_handles.isEmpty() && !this.midi_frame_map.has_frames_remaining(initial_frame)) {
            throw DeadException()
        }
    }

    /* Add handles that would be active but aren't because of a jump in position */
    private fun activate_active_handles(frame: Int) {
        val handles = this.midi_frame_map.get_active_handles(frame).toList()
        val handles_adj: MutableList<Pair<SampleHandle, IntArray>> = mutableListOf()
        for ((first_frame, handle_pair) in handles) {
            val (handle, _) = handle_pair
            if (first_frame == frame) {
                continue
            }
            handle.set_working_frame(frame - first_frame)
            handles_adj.add(handle_pair)
        }

        this.activate_sample_handles(handles_adj.toSet(), frame, 0)
    }

    fun activate_sample_handles(handles: Set<Pair<SampleHandle, IntArray>>, initial_frame: Int, offset: Int) {
        // then populate the next active frames with upcoming sample handles
        for ((handle, merge_keys) in handles) {
            val new_handle = handle.copy()
            this._active_sample_handles[handle.uuid] = ActiveHandleMapItem(
                initial_frame + offset,
                new_handle,
                merge_keys
            )
        }
    }

    fun clear() {
        this.kill_frame = null
        for ((uuid, item) in this._active_sample_handles) {
            item.handle.destroy()
        }
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
        for ((_,_,buffer) in midi_frame_map.get_effect_buffers()) {
            buffer.set_frame(this.frame)
        }
    }
}
