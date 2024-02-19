package com.qfs.pagan

import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.FrameMap
import com.qfs.apres.soundfontplayer.SampleHandle
import com.qfs.apres.soundfontplayer.SampleHandleManager
import kotlin.math.max
import kotlin.math.min

class PlaybackFrameMap: FrameMap {
    var beat_count: Int = 0
    var tempo: Float = 0f

    var sample_handle_manager: SampleHandleManager? = null
    private var handle_map = HashMap<Int, SampleHandle>()
    private var frame_map = HashMap<Int, MutableSet<Int>>()
    private val handle_range_map = HashMap<Int, IntRange>()
    var unmap_flags = HashMap<List<Int>, Boolean>()
    /*
     NOTE: key List where the first three entries are a BeatKey and the rest is the position since a Pair() would
     be matched by instance rather than content
    */
    private var quick_map_sample_handles =  HashMap<List<Int>, Set<Int>>()
    internal var cached_frame_count: Int? = null
    private var initial_delay_handles = HashMap<Int, Int>()

    // FrameMap Interface -------------------
    override fun get_new_handles(frame: Int): Set<SampleHandle>? {
        if (!this.frame_map.containsKey(frame)) {
            return null
        }

        val output = mutableSetOf<SampleHandle>()
        for (uuid in this.frame_map[frame]!!) {
            // Kludge? TODO: figure out a better place for these resets
            val use_handle = SampleHandle(this.handle_map[uuid]!!)

            use_handle.release_frame = this.handle_map[uuid]!!.release_frame!!
            use_handle.set_working_frame(0)
            use_handle.is_dead = false

            output.add(use_handle)
        }

        return output
    }

    override fun get_beat_frames(): HashMap<Int, IntRange> {
        val frames_per_beat = 60.0 * this.sample_handle_manager!!.sample_rate / this.tempo
        val output = HashMap<Int, IntRange>()

        for (i in 0 until this.beat_count) {
            output[i] = (frames_per_beat * i).toInt() until (frames_per_beat * (i + 1)).toInt()
        }
        return output
    }

    override fun get_active_handles(frame: Int): Set<Pair<Int, SampleHandle>> {
        val output = mutableSetOf<Pair<Int, SampleHandle>>()
        for ((uuid, range) in this.handle_range_map) {
            if (range.contains(frame)) {
                output.add(
                    Pair(
                        range.first,
                        this.handle_map[uuid]!!
                    )
                )
            }
        }
        return output
    }

    override fun get_size(): Int {
        if (this.cached_frame_count == null) {
            this.cached_frame_count = -1
            for (range in this.handle_range_map.values) {
                this.cached_frame_count = max(range.last, this.cached_frame_count!!)
            }

            val frames_per_beat = 60.0 * this.sample_handle_manager!!.sample_rate / this.tempo
            this.cached_frame_count = max(this.cached_frame_count!! + 1, (this.beat_count * frames_per_beat).toInt())
        }
        return this.cached_frame_count!!
    }
    // End FrameMap Interface --------------------------

    fun clear() {
        this.frame_map.clear()
        this.unmap_flags.clear()
        this.handle_map.clear()
        this.handle_range_map.clear()
        this.quick_map_sample_handles.clear()
        this.cached_frame_count = null
    }

    fun remove_handle(uuid: Int) {
        this.handle_map.remove(uuid)
        this.initial_delay_handles.remove(uuid)

        // reminder: 'end_frame' here is the last active frame in the sample, including decay
        val frame_range = this.handle_range_map.remove(uuid) ?: return
        if (this.frame_map.containsKey(frame_range.first)) {
            this.frame_map[frame_range.first]!!.remove(uuid)
            if (this.frame_map[frame_range.first]!!.isEmpty()) {
                this.frame_map.remove(frame_range.first)
            }
        }
    }

    fun remove_handle(key: List<Int>) {
        if (this.unmap_flags.getOrDefault(key, false)) {
            return
        }

        this.unmap_flags[key] = true

        val sample_handles = this.quick_map_sample_handles.remove(key) ?: return

        for (uuid in sample_handles) {
            this.remove_handle(uuid)
        }

        this.cached_frame_count = null
    }

    fun add_handles(quick_key: List<Int>, start_frame: Int, end_frame: Int, start_event: MIDIEvent) {
        val handles = when (start_event) {
            is NoteOn -> {
                this.sample_handle_manager!!.gen_sample_handles(start_event)
            }
            is NoteOn79 -> {
                this.sample_handle_manager!!.gen_sample_handles(start_event)
            }
            else -> return
        }
        val uuids = mutableSetOf<Int>()
        var max_end_frame = 0
        var min_start_frame = Int.MAX_VALUE
        for (handle in handles) {
            val sample_end_frame = (end_frame + handle.get_release_duration()) - handle.volume_envelope.frames_delay
            val sample_start_frame = start_frame - handle.volume_envelope.frames_delay

            max_end_frame = max(max_end_frame, sample_end_frame)
            min_start_frame = min(min_start_frame, sample_start_frame)

            handle.release_frame = end_frame - start_frame

            uuids.add(handle.uuid)

            if (!this.frame_map.containsKey(sample_start_frame)) {
                this.frame_map[sample_start_frame] = mutableSetOf()
            }
            this.frame_map[sample_start_frame]!!.add(handle.uuid)

            this.handle_range_map[handle.uuid] = sample_start_frame .. sample_end_frame
            this.handle_map[handle.uuid] = handle
            if (sample_start_frame < 0) {
                this.initial_delay_handles[handle.uuid] = sample_start_frame
            }
        }

        this.quick_map_sample_handles[quick_key] = uuids

        this.unmap_flags[quick_key] = false
        this.cached_frame_count = null
    }

    fun insert_beat(beat_index: Int) {
        this.beat_count += 1
        this.cached_frame_count = null

        val frames_per_beat = 60.0 * this.sample_handle_manager!!.sample_rate / this.tempo

        val sorted_keys = this.quick_map_sample_handles.keys.sortedByDescending { it[2] }
        val samples_to_move = mutableSetOf<Int>()

        for (key in sorted_keys) {
            if (key[2] < beat_index) {
                break
            }

            val new_key = key.toMutableList()
            new_key[2] += 1

            this.quick_map_sample_handles[new_key] = this.quick_map_sample_handles.remove(key)!!
            samples_to_move.addAll(this.quick_map_sample_handles[new_key]!!)
        }

        var first_frame = ((this.beat_count + 1) * frames_per_beat).toInt()
        for (uuid in samples_to_move) {
            val pair = this.handle_range_map[uuid] ?: continue
            this.handle_range_map[uuid] = pair.first + frames_per_beat.toInt() .. pair.last + frames_per_beat.toInt()

            first_frame = min(first_frame, pair.first)
        }

        for (frame in this.frame_map.keys.sortedByDescending { it }) {
            if (frame < first_frame) {
                continue
            }
            this.frame_map[frame + frames_per_beat.toInt()] = this.frame_map.remove(frame)!!
        }
    }

    fun has_quick_key(key: List<Int>): Boolean {
        return this.quick_map_sample_handles.contains(key)
    }

    fun insert_channel(channel: Int) {
        val sorted_keys = this.quick_map_sample_handles.keys.sortedByDescending { it[0] }
        for (original_key in sorted_keys) {
            if (original_key[0] < channel) {
                break
            }
            val new_key = original_key.toMutableList()
            new_key[0] += 1

            this.quick_map_sample_handles[new_key] = this.quick_map_sample_handles.remove(original_key)!!
        }
    }

    fun remove_channel(channel: Int) {
        val sorted_keys = this.quick_map_sample_handles.keys.sortedBy { it[0] }
        for (original_key in sorted_keys) {
            if (original_key[0] < channel) {
                break
            }
            val new_key = original_key.toMutableList()
            new_key[0] -= 1

            this.quick_map_sample_handles[new_key] = this.quick_map_sample_handles.remove(original_key)!!
        }
    }

    fun remove_beat(beat_index: Int) {
        this.cached_frame_count = null

        val frames_per_beat = 60.0 * this.sample_handle_manager!!.sample_rate / this.tempo
        val samples_to_move = mutableSetOf<Int>()
        val samples_to_remove = mutableSetOf<Int>()

        val sorted_keys = this.quick_map_sample_handles.keys.sortedBy { it[2] }
        for (original_key in sorted_keys) {
            if (original_key[2] < beat_index) {
                continue
            } else if (original_key[2] == beat_index) {
                samples_to_remove.addAll(
                    this.quick_map_sample_handles.remove(original_key)!!
                )
                continue
            }

            val new_key = original_key.toMutableList()
            new_key[2] -= 1

            this.quick_map_sample_handles[new_key] = this.quick_map_sample_handles.remove(original_key)!!
            samples_to_move.addAll(this.quick_map_sample_handles[new_key]!!)
        }

        var first_frame = ((this.beat_count + 1) * frames_per_beat).toInt()
        var last_frame = 0
        for (uuid in samples_to_move) {
            val pair = this.handle_range_map[uuid]!!
            this.handle_range_map[uuid] = (pair.first - frames_per_beat.toInt()) .. (pair.last - frames_per_beat.toInt())
            first_frame = min(first_frame, pair.first)
            last_frame = max(last_frame, pair.last)
        }

        var move_frames = (first_frame..last_frame).intersect(this.frame_map.keys)

        first_frame = ((this.beat_count + 1) * frames_per_beat).toInt()
        last_frame = 0
        for (uuid in samples_to_remove) {
            val pair = this.handle_range_map[uuid]!!
            this.handle_range_map.remove(uuid)
            this.handle_map.remove(uuid)
            first_frame = min(first_frame, pair.first)
            last_frame = max(last_frame, pair.last)
        }


        val del_frames = (first_frame..last_frame).intersect(this.frame_map.keys)
        for (f in del_frames) {
            this.frame_map.remove(f)
        }

        for (f in move_frames.toList().sortedBy { it }) {
            this.frame_map[f - frames_per_beat.toInt()] = this.frame_map.remove(f)!!
        }

        this.beat_count -= 1
    }

    fun remove_line(channel: Int, line_offset: Int) {
        val sorted_keys = this.quick_map_sample_handles.keys.sortedBy { (it[0] * 1000) + it[1] }
        for (original_key in sorted_keys) {
            if (original_key[0] != channel || original_key[1] < line_offset) {
                break
            }
            val new_key = original_key.toMutableList()
            new_key[1] -= 1

            this.quick_map_sample_handles[new_key] = this.quick_map_sample_handles.remove(original_key)!!
        }
    }

    fun insert_line(channel: Int, line_offset: Int) {
        val sorted_keys = this.quick_map_sample_handles.keys.sortedByDescending { (it[0] * 1000) + it[1] }
        for (original_key in sorted_keys) {
            if (original_key[0] != channel || original_key[1] < line_offset) {
                break
            }

            val new_key = original_key.toMutableList()
            new_key[1] += 1

            this.quick_map_sample_handles[new_key] = this.quick_map_sample_handles.remove(new_key)!!
        }
    }

    //fun clone(): PlaybackFrameMap {
    //    val new_map = PlaybackFrameMap()
    //    new_map.sample_rate = this.sample_handle_manager.sample_rate
    //    new_map.beat_count = this.beat_count
    //    new_map.tempo = this.tempo

    //    for ((k, v) in this.handle_map) {
    //        new_map.handle_map[k] = v
    //    }
    //    for ((k, v) in this.frame_map) {
    //        new_map.frame_map[k] = v.toMutableSet()
    //    }
    //    for ((k, v) in this.handle_range_map) {
    //        new_map.handle_range_map[k] = v
    //    }
    //    for ((k, v) in this.quick_map_sample_handles) {
    //        new_map.quick_map_sample_handles[k.copy()] = v.toSet()
    //    }

    //    new_map.cached_frame_count = this.cached_frame_count

    //    return new_map
    //}
}
