package com.qfs.pagan

import com.qfs.apres.soundfontplayer.SampleHandle
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.math.min

class OpusLineFrameTracker {
    class IndefiniteNoteException: Exception()

    private var generating: Boolean = false
    private var mutex = Mutex()
    var generated_frames = HashMap<Int, FloatArray>()
    val size: Int
        get() = this._calc_size()

    var handles: HashMap<Int, Pair<SampleHandle, IntRange>> = hashMapOf()
    var handle_start_map: HashMap<Int, MutableSet<Int>> = hashMapOf() // Frame::[Uuid]
    var handle_end_map: HashMap<Int, MutableSet<Int>> = hashMapOf() // frame:: [uuid]

    private var queued_ranges: MutableSet<IntRange> = mutableSetOf()

    var volume_map: Map<Int, Pair<Float, Float>>? = null

    private fun _calc_size(): Int {
        var max_start_frame = this.generated_frames.keys.max()
        return this.generated_frames[max_start_frame]!!.size + max_start_frame
    }

    fun set_volume_map(new_map: Map<Int, Pair<Float, Float>>) {
        this.volume_map = new_map
    }

    fun get_volume(frame: Int): Float {
        if (this.volume_map == null) {
            throw Exception("Volume Map Uninitialized")
        }

        var working_increment = 0F
        var working_base = 0F
        var prev_frame = 0

        for ((i, pair) in this.volume_map!!) {
            if (i <= frame) {
                prev_frame = i
                working_base = pair.first
                working_increment = pair.second
            } else {
                break
            }
        }

        return ((frame - prev_frame).toFloat() * working_increment) + working_base
    }

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

    suspend fun generate_queued_frames() {
        this.mutex.withLock {
            val sorted_ranges = this.queued_ranges.sortedBy { range: IntRange ->
                range.first
            }

            this.generating = true

            val handled_ranges = mutableSetOf<IntRange>()
            for (i_range in sorted_ranges) {
                val frames_to_stitch = mutableSetOf<Int>()
                for ((start_frame, array) in this.generated_frames) {
                    if (i_range.intersects(start_frame until start_frame + (array.size / 2))) {
                        frames_to_stitch.add(start_frame)
                    }
                }

                val new_frame_key: Int
                val new_size: Int
                val new_array: FloatArray
                if (frames_to_stitch.isNotEmpty()) {
                    new_frame_key = min(i_range.first, frames_to_stitch.min())
                    new_size = (this.generated_frames[frames_to_stitch.max()]!!.size / 2) - new_frame_key
                    new_array = FloatArray(new_size * 2) { i: Int -> 0F }
                    for (k in frames_to_stitch) {
                        this.generated_frames[k]!!.forEachIndexed { f: Int, frame: Float ->
                            new_array[(k * 2) + f] = frame
                        }
                    }

                    // TODO: Fix doubling up
                    for (i in i_range) {
                        val ii = i * 2
                        new_array[(ii - new_frame_key)] = 0F
                        new_array[((ii + 1) - new_frame_key)] = 0F
                    }

                    for (k in frames_to_stitch) {
                        this.generated_frames.remove(k)
                    }

                } else {
                    new_frame_key = i_range.first
                    new_size = i_range.last() - i_range.first
                    new_array = FloatArray(new_size * 2) { i: Int -> 0F }
                }

                this.generated_frames[new_frame_key] = new_array

                for (hid in this.handles.keys) { // #NONOPT
                    val (handle, h_range) = this.handles[hid]!!
                    if (i_range.contains(h_range.first)) {
                        handle.set_working_frame(0)
                    } else if (h_range.contains(i_range.last)) {
                        handle.set_working_frame(i_range.last - h_range.first)
                        for (i in h_range.first..i_range.last) {
                            val (lv, rv) = handle.get_next_frame() ?: break
                            val ii = (i * 2) - new_frame_key
                            new_array[ii] += rv
                            new_array[ii + 1] += lv
                        }
                    }
                }
                handled_ranges.add(i_range)
            }

            this.queued_ranges.clear()
            this.generating = false
        }
    }

    fun remove_handles_at_frame(frame: Int) {
        val handles = this.handle_start_map.remove(frame) ?: return
        for (handle_id in handles) {
            var (handle, range) = this.handles.remove(handle_id) ?: continue
            val id_set = this.handle_end_map[range.last]!!.remove(handle_id)
            this.invalidate(range.first, range.last)
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

        this.invalidate(frame, max_end_frame)
    }

    fun insert_frames(count: Int, position: Int) {
        this.handle_start_map.clear()
        this.handle_end_map.clear()

        for ((uid, pair) in this.handles) {
            val (handle, range) = pair
            if (range.first <= position) {
                val new_first = range.first + count
                val new_last = range.last + count
                this.handles[uid] = Pair(handle, new_first .. new_last)

                if (!this.handle_start_map.contains(new_first)) {
                    this.handle_start_map[new_first] = mutableSetOf()
                }

                if (!this.handle_end_map.contains(new_last)) {
                    this.handle_end_map[new_last] = mutableSetOf()
                }

                this.handle_start_map[new_first]!!.add(uid)
                this.handle_end_map[new_last]!!.add(uid)
            } else {
                if (!this.handle_start_map.contains(range.first)) {
                    this.handle_start_map[range.first] = mutableSetOf()
                }

                if (!this.handle_end_map.contains(range.last)) {
                    this.handle_end_map[range.last] = mutableSetOf()
                }

                this.handle_start_map[range.first]!!.add(uid)
                this.handle_end_map[range.last]!!.add(uid)
            }
        }

        val keys = this.generated_frames.keys.sorted().reversed()
        for (frame in keys) {
            if (frame < position) {
                break
            }

            this.generated_frames[frame + count] = this.generated_frames.remove(frame)!!
        }
    }
}

inline fun IntRange.intersects(other: IntRange): Boolean {
    return this.first() in other || this.last() in other
}