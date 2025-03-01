package com.qfs.pagan

import com.qfs.apres.soundfontplayer.SampleHandle
import kotlin.math.max
import kotlin.math.min

class OpusLineFrameTracker {
    class IndefiniteNoteException: Exception()

    var generated_frames = Array(0) { 0F } // Stereo. 1 frame is 2 Floats
    val size: Int
        get() = this.generated_frames.size / 2

    var handles: HashMap<Int, Pair<SampleHandle, IntRange>> = hashMapOf()
    var handle_start_map: HashMap<Int, MutableSet<Int>> = hashMapOf() // Frame::[Uuid]
    var handle_end_map: HashMap<Int, MutableSet<Int>> = hashMapOf() // frame:: [uuid]

    private var queued_ranges: MutableSet<IntRange> = mutableSetOf()

    fun invalidate(from_frame: Int = 0, to_frame: Int = this.size) {
        val invalid_range = from_frame until to_frame

        val relevant_handles = this.handles.keys.filter { key ->
            val range = this.handles[key]!!.second
            invalid_range.contains(range.first) || invalid_range.contains(range.last)
        }

        for (uuid in relevant_handles) {
            val handle_range = this.handles[uuid]!!.second
            var merged_range = handle_range
            val preserved_ranges = mutableSetOf<IntRange>()
            for (range in this.queued_ranges) {
                val handle_contains_first = merged_range.contains(range.first)
                val handle_contains_last = merged_range.contains(range.last)
                if (handle_contains_first && handle_contains_last) {
                    continue
                } else if (handle_contains_first || handle_contains_last) {
                    merged_range = min(merged_range.first, range.first) .. max(merged_range.last, range.last)
                } else {
                    preserved_ranges.add(merged_range)
                }
            }
            this.queued_ranges = preserved_ranges
        }

    }

    fun generate_queued_frames() {
        val sorted_ranges = this.queued_ranges.sortedBy { range: IntRange ->
            range.first
        }

        for (i_range in sorted_ranges) {
            for (i in i_range) {
                this.generated_frames[(i * 2)] = 0F
                this.generated_frames[(i * 2) + 1] = 0F
            }

            for (hid in this.handles.keys) { // #NONOPT
                val (handle, h_range) = this.handles[hid]!!
                if (i_range.contains(h_range.first)) {
                    handle.set_working_frame(0)
                } else if (h_range.contains(i_range.last)) {
                    handle.set_working_frame(i_range.last - h_range.first)
                    for (i in h_range.first .. i_range.last) {
                        val (lv, rv) = handle.get_next_frame() ?: break
                        this.generated_frames[(i * 2)] += rv
                        this.generated_frames[(i * 2) + 1] += lv
                    }
                }
            }
        }

        this.queued_ranges.clear()
    }

    fun remove_handles_at_frame(frame: Int) {
        val handles = this.handle_start_map.remove(frame) ?: return
        for (handle_id in handles) {
            var (handle, range) = this.handles.remove(handle_id) ?: continue
            val id_set = this.handle_end_map[range.last]!!.remove(handle_id)
        }
    }

    fun add_handles(frame: Int, handles: Set<SampleHandle>) {
        // TODO: Clip release
        if (!this.handle_start_map.containsKey(frame)) {
            this.handle_start_map[frame] = mutableSetOf()
        }
        for (handle in handles) {
            this.handle_start_map[frame]!!.add(handle.uuid)
        }

        var max_end_frame = 0
        for (handle in handles) {
            if (handle.release_frame == null) {
                throw IndefiniteNoteException()
            }

            val end_frame = frame + handle.release_frame!! + handle.volume_envelope.frames_release
            if (!this.handle_end_map.containsKey(end_frame)) {
                this.handle_end_map[end_frame] = mutableSetOf()
            }

            this.handle_end_map[end_frame]!!.add(handle.uuid)
            this.handles[handle.uuid] = Pair(handle, frame .. end_frame)

            max_end_frame = max(max_end_frame, end_frame)
        }

        if (max_end_frame > this.size) {
            this.add_frames(max_end_frame - this.size, this.size)
        }

        this.invalidate(frame, max_end_frame)
    }

    private fun add_frames(count: Int, position: Int) {
        val a_count = count * 2
        val a_position = position * 2
        this.generated_frames = Array(a_count + this.generated_frames.size) { i: Int ->
            if (i <= a_position) {
                this.generated_frames[i]
            } else if (i > a_position + a_count) {
                this.generated_frames[i - a_count]
            } else {
                0F
            }
        }
    }
}