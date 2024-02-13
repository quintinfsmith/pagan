package com.qfs.pagan

import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.FrameMap
import com.qfs.apres.soundfontplayer.SampleHandle
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.opusmanager.OpusLayerCursor
import com.qfs.pagan.structure.OpusTree
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

open class OpusLayerFrameMap: OpusLayerCursor() {
    class PlaybackFrameMap: FrameMap {
        var sample_rate: Int = 0
        var beat_count: Int = 0
        var tempo: Float = 0f

        private var handle_map = HashMap<Int, SampleHandle>()
        private var frame_map = HashMap<Int, MutableSet<Int>>()
        private val handle_range_map = HashMap<Int, IntRange>()
        private var quick_map_sample_handles =  HashMap<Pair<BeatKey, List<Int>>, Set<Int>>()

        internal var changed_frames = mutableSetOf<IntRange>()
        internal var cached_frame_count: Int? = null
        private var initial_delay_handles = HashMap<Int, Int>()

        // FrameMap Interface -------------------
        override fun get_new_handles(frame: Int): Set<SampleHandle>? {
            val adjusted_frame = if (this.initial_delay_handles.isEmpty()) {
                frame
            } else {
                frame + this.initial_delay_handles.values.min()
            }
            if (!this.frame_map.containsKey(adjusted_frame)) {
                return null
            }

            val output = mutableSetOf<SampleHandle>()
            for (uuid in this.frame_map[adjusted_frame]!!) {
                // Kludge? TODO: figure out a better place for these resets
                val use_handle = SampleHandle(this.handle_map[uuid]!!)

                use_handle.release_frame = this.handle_map[uuid]!!.release_frame!!
                use_handle.set_working_frame(0)
                use_handle.is_dead = false

                output.add(use_handle)
            }

            return output
        }


        override fun get_beat_frames(): List<Int> {
            val frames_per_beat = 60.0 * this.sample_rate / this.tempo
            return List(this.beat_count) { i ->
                (frames_per_beat * (i + 1)).toInt()
            }
        }

        override fun get_active_handles(frame: Int): Set<Pair<Int, SampleHandle>> {
            val adjusted_frame = if (this.initial_delay_handles.isEmpty()) {
                frame
            } else {
                frame - this.initial_delay_handles.values.min()
            }

            val output = mutableSetOf<Pair<Int, SampleHandle>>()
            for ((uuid, range) in this.handle_range_map) {
                if (range.contains(adjusted_frame)) {
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
                this.cached_frame_count = this.cached_frame_count!! + 1
            }
            return this.cached_frame_count!!
        }

        // End FrameMap Interface --------------------------

        fun clear() {
            this.frame_map.clear()
            this.handle_map.clear()
            this.changed_frames.clear()
            this.handle_range_map.clear()
            this.quick_map_sample_handles.clear()
            this.cached_frame_count = null
        }

        fun remove_handle(uuid: Int) {
            this.handle_map.remove(uuid)
            this.initial_delay_handles.remove(uuid)

            // reminder: 'end_frame' here is the last active frame in the sample, including decay
            val frame_range = this.handle_range_map.remove(uuid) ?: return
            this.changed_frames.add(frame_range)
            if (this.frame_map.containsKey(frame_range.first)) {
                this.frame_map[frame_range.first]!!.remove(uuid)
                if (this.frame_map[frame_range.first]!!.isEmpty()) {
                    this.frame_map.remove(frame_range.first)
                }
            }
        }

        fun remove_handle(key: Pair<BeatKey, List<Int>>) {
            val sample_handles = this.quick_map_sample_handles.remove(key) ?: return

            for (uuid in sample_handles) {
                this.remove_handle(uuid)
            }

            this.cached_frame_count = null
        }

        fun add_handles(quick_key: Pair<BeatKey, List<Int>>, start_frame: Int, end_frame: Int, handles: Set<SampleHandle>) {
            val uuids = mutableSetOf<Int>()
            var max_end_frame = 0
            var min_start_frame = Int.MAX_VALUE
            for (handle in handles) {
                val sample_end_frame = (end_frame + handle.frame_count_release) - handle.frame_count_delay
                val sample_start_frame = start_frame - handle.frame_count_delay

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
            this.changed_frames.add(min_start_frame .. max_end_frame)
        }

        fun insert_beat(beat_index: Int) {
            this.beat_count += 1
            this.cached_frame_count = null

            val frames_per_beat = 60.0 * this.sample_rate / this.tempo

            val sorted_keys = this.quick_map_sample_handles.keys.sortedByDescending { it.first.beat }
            val samples_to_move = mutableSetOf<Int>()

            for ((beat_key, position) in sorted_keys) {
                if (beat_key.beat < beat_index) {
                    break
                }

                val new_key = Pair(
                    BeatKey(
                        beat_key.channel,
                        beat_key.line_offset,
                        beat_key.beat + 1
                    ),
                    position
                )

                this.quick_map_sample_handles[new_key] = this.quick_map_sample_handles.remove(Pair(beat_key, position))!!
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
            this.changed_frames.add((beat_index * frames_per_beat).toInt() .. (frames_per_beat * this.beat_count + 1).toInt())
        }

        fun has_quick_key(key: Pair<BeatKey, List<Int>>): Boolean {
            return this.quick_map_sample_handles.contains(key)
        }

        fun insert_channel(channel: Int) {
            val sorted_keys = this.quick_map_sample_handles.keys.sortedByDescending { it.first.channel }
            for ((beat_key, position) in sorted_keys) {
                if (beat_key.channel < channel) {
                    break
                }
                val new_key = Pair(
                    BeatKey(
                        beat_key.channel + 1,
                        beat_key.line_offset,
                        beat_key.beat
                    ),
                    position
                )

                this.quick_map_sample_handles[new_key] = this.quick_map_sample_handles.remove(Pair(beat_key, position))!!
            }
        }

        fun remove_channel(channel: Int) {
            val sorted_keys = this.quick_map_sample_handles.keys.sortedBy { it.first.channel }
            for ((beat_key, position) in sorted_keys) {
                if (beat_key.channel < channel) {
                    break
                }
                val new_key = Pair(
                    BeatKey(
                        beat_key.channel - 1,
                        beat_key.line_offset,
                        beat_key.beat
                    ),
                    position
                )

                this.quick_map_sample_handles[new_key] = this.quick_map_sample_handles.remove(Pair(beat_key, position))!!
            }
        }

        fun remove_beat(beat_index: Int) {
            this.cached_frame_count = null

            val frames_per_beat = 60.0 * this.sample_rate / this.tempo
            val samples_to_move = mutableSetOf<Int>()
            val samples_to_remove = mutableSetOf<Int>()

            val sorted_keys = this.quick_map_sample_handles.keys.sortedBy { it.first.beat }
            for ((beat_key, position) in sorted_keys) {
                if (beat_key.beat < beat_index) {
                    continue
                } else if (beat_key.beat == beat_index) {
                    samples_to_remove.addAll(
                        this.quick_map_sample_handles.remove(
                            Pair(
                                beat_key,
                                position
                            )
                        )!!
                    )
                    continue
                }

                val new_key = Pair(
                    BeatKey(
                        beat_key.channel,
                        beat_key.line_offset,
                        beat_key.beat - 1
                    ),
                    position
                )

                this.quick_map_sample_handles[new_key] =
                    this.quick_map_sample_handles.remove(Pair(beat_key, position))!!
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

            this.changed_frames.add((beat_index * frames_per_beat).toInt() .. (frames_per_beat * this.beat_count + 1).toInt())
            this.beat_count -= 1
        }

        fun remove_line(channel: Int, line_offset: Int) {
            val sorted_keys = this.quick_map_sample_handles.keys.sortedBy { it.first.channel }
            for ((beat_key, position) in sorted_keys) {
                if (beat_key.channel != channel || beat_key.line_offset < line_offset) {
                    break
                }
                val new_key = Pair(
                    BeatKey(
                        beat_key.channel,
                        beat_key.line_offset - 1,
                        beat_key.beat
                    ),
                    position
                )

                this.quick_map_sample_handles[new_key] =
                    this.quick_map_sample_handles.remove(Pair(beat_key, position))!!
            }
        }
        fun insert_line(channel: Int, line_offset: Int) {
            val sorted_keys = this.quick_map_sample_handles.keys.sortedByDescending { it.first.channel }
            for ((beat_key, position) in sorted_keys) {
                if (beat_key.channel != channel || beat_key.line_offset < line_offset) {
                    break
                }
                val new_key = Pair(
                    BeatKey(
                        beat_key.channel,
                        beat_key.line_offset + 1,
                        beat_key.beat
                    ),
                    position
                )

                this.quick_map_sample_handles[new_key] = this.quick_map_sample_handles.remove(Pair(beat_key, position))!!
            }
        }

        //fun clone(): PlaybackFrameMap {
        //    val new_map = PlaybackFrameMap()
        //    new_map.sample_rate = this.sample_rate
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

    var sample_handle_manager: SampleHandleManager? = null
    private var unmap_flags = HashMap<Pair<BeatKey, List<Int>>, Boolean>()
    private var flux_indicator: Int = 0
    private var flag_cleared_in_flux: Boolean = false
    private var clear_and_set_lock: Int = 0 // if a function clears and reset the map, don't bother with anything in between
    private var frame_map = PlaybackFrameMap()

    fun get_frame_map(): FrameMap {
        return this.frame_map
    }

    //-----Layer Functions-------//
    fun set_sample_handle_manager(new_manager: SampleHandleManager) {
        this.frame_map.clear()
        this.frame_map.sample_rate = new_manager.sample_rate

        this.sample_handle_manager = new_manager
        this.setup_sample_handle_manager()
        this.setup_frame_map()
    }

    open fun on_frames_changed(frames: List<IntRange>) { }

    fun <T> clear_and_set_frames(callback: () -> T): T {
        if (this.flux_indicator > 0) {
            this.flag_cleared_in_flux = true
        }

        if (this.clear_and_set_lock == 0) {
            this.clear_frame_map_data()
        }

        this.clear_and_set_lock += 1

        val output = try {
            callback()
        } catch (e: Exception) {
            this.clear_and_set_lock -= 1
            throw e
        }

        this.clear_and_set_lock -= 1
        if (this.clear_and_set_lock == 0) {
            this.setup_frame_map()
        }

        return output
    }

    fun <T> flux_wrapper(callback: () -> T): T {
        this.flux_indicator += 1
        val output = try {
            callback()
        } catch (e: Exception) {
            this.flux_indicator -= 1
            throw e
        }

        this.flux_indicator -= 1

        if (flux_indicator == 0 && this.flag_cleared_in_flux) {
            this.flag_cleared_in_flux = false
            this.setup_frame_map()
        }

        if (flux_indicator == 0 && this.frame_map.changed_frames.isNotEmpty()) {
            val sorted_list = this.frame_map.changed_frames.sortedBy { it.start }
            val merged_list = mutableListOf<IntRange>()

            var pivot_range: IntRange? = null
            sorted_list.forEachIndexed { i: Int, range: IntRange ->
                if (pivot_range == null) {
                    pivot_range = range
                }

                pivot_range = if (pivot_range!!.first in range || pivot_range!!.last in range || range.first in pivot_range!! || range.last in pivot_range!!) {
                    min(range.first, pivot_range!!.first) .. max(range.last, pivot_range!!.last)
                } else {
                    merged_list.add(pivot_range!!)
                    null
                }
            }
            if (pivot_range != null) {
                merged_list.add(pivot_range!!)
                pivot_range = null
            }

            this.frame_map.changed_frames.clear()
            this.on_frames_changed(merged_list)
        }

        return output
    }

    fun setup_frame_map() {
        this.flux_wrapper {
            this.frame_map.tempo = this.tempo
            this.frame_map.beat_count = this.beat_count
            this.unmap_flags.clear()
            this.channels.forEachIndexed { c: Int, channel: OpusChannel ->
                channel.lines.forEachIndexed { l: Int, line: OpusChannel.OpusLine ->
                    for (b in 0 until this.beat_count) {
                        this.map_frames(BeatKey(c, l, b), listOf())
                    }
                }
            }
        }
    }

    fun setup_sample_handle_manager() {
        if (this.sample_handle_manager == null) {
            return
        }

        this.flux_wrapper {
            for (channel in this.channels.indices) {
                val instrument = this.get_channel_instrument(channel)
                this.sample_handle_manager!!.select_bank(this.channels[channel].midi_channel, instrument.first)
                this.sample_handle_manager!!.change_program(this.channels[channel].midi_channel, instrument.second)
            }
        }
    }

    fun unset_sample_handle_manager() {
        this.sample_handle_manager = null
        this.frame_map.clear()
        this.unmap_flags.clear()
    }

    fun get_frame(beat_key: BeatKey, position: List<Int>): Int {
        var working_tree = this.get_tree(beat_key)
        var offset = 0.0
        var w = 1.0

        for (p in position) {
            if (working_tree.is_leaf()) {
                break
            }
            w /= working_tree.size
            offset += (w * p)
            working_tree = working_tree[p]
        }

        return ((offset + beat_key.beat.toDouble()) * (60.0 * this.sample_handle_manager!!.sample_rate / this.tempo)).toInt()
    }

    fun gen_midi_event(beat_key: BeatKey, position: List<Int>): MIDIEvent? {
        if (this.is_percussion(beat_key.channel)) {
            return NoteOn(
                channel=9,
                velocity=this.get_line_volume(beat_key.channel, beat_key.line_offset),
                note=this.get_percussion_instrument(beat_key.line_offset) + 27
            )
        }

        val value = this.get_absolute_value(beat_key, position) ?: return null

        val radix = this.tuning_map.size
        val octave = value / radix
        val offset = this.tuning_map[value % radix]

        // This offset is calculated so the tuning map always reflects correctly
        val transpose_offset = 12.0 * this.transpose.toDouble() / radix.toDouble()
        val std_offset = (offset.first.toDouble() * 12.0 / offset.second.toDouble())


        val note = (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21
        val velocity = this.get_line_volume(beat_key.channel, beat_key.line_offset)

        return if (this.is_tuning_standard()) {
            NoteOn(
                channel = this.channels[beat_key.channel].midi_channel,
                velocity = velocity,
                note = note
            )
        } else {
            val bend = (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512.0).toInt()
            NoteOn79(
                index = 0, // Set index as note is applied
                channel = this.channels[beat_key.channel].midi_channel,
                velocity = velocity shl 8,
                note = note,
                bend = bend
            )
        }
    }

    fun unmap_frames(beat_key: BeatKey, position: List<Int>) {
        if (this.flux_indicator > 0 && this.flag_cleared_in_flux) {
            return
        }

        val working_tree = this.get_tree(beat_key, position)

        if (!working_tree.is_leaf()) {
            for (i in 0 until working_tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.unmap_frames(beat_key, new_position)
            }
            return
        }
        val unmap_key = Pair(beat_key, position)

        if (this.unmap_flags.getOrDefault(unmap_key, false)) {
            return
        }

        this.unmap_flags[unmap_key] = true

        this.frame_map.remove_handle(unmap_key)
    }

    fun map_frames(beat_key: BeatKey, position: List<Int>) {
        if (this.flux_indicator > 0 && this.flag_cleared_in_flux) {
            return
        }

        val working_tree = this.get_tree(beat_key, position)
        if (!working_tree.is_leaf()) {
            for (i in 0 until working_tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.map_frames(beat_key, new_position)
            }
            return
        } else if (!working_tree.is_event()) {
            return
        }
        val quick_key = Pair(beat_key, position)
        if (this.frame_map.has_quick_key(quick_key)) {
            this.unmap_frames(beat_key, position)
        }

        val (start_frame, end_frame) = this.get_frame_range(beat_key, position)
        val start_event = gen_midi_event(beat_key, position)!!
        val handles = when (start_event) {
            is NoteOn -> {
                this.sample_handle_manager!!.gen_sample_handles(start_event)
            }
            is NoteOn79 -> {
                this.sample_handle_manager!!.gen_sample_handles(start_event)
            }
            else -> return
        }


        this.frame_map.add_handles(quick_key, start_frame, end_frame, handles)

        this.unmap_flags[Pair(beat_key, position)] = false

        this.frame_map.cached_frame_count = null
    }

    fun get_frame_range(beat_key: BeatKey, position: List<Int>): Pair<Int, Int> {
        var working_tree = this.get_tree(beat_key)
        var offset = 0.0
        var w = 1.0

        for (p in position) {
            w /= working_tree.size
            offset += (w * p)
            working_tree = working_tree[p]
        }

        val duration = if (working_tree.is_event()) {
            working_tree.get_event()!!.duration
        } else {
            1
        }
        val ratio = (60.0 * this.sample_handle_manager!!.sample_rate.toDouble() / this.tempo)
        val initial = offset + beat_key.beat.toDouble()
        return Pair(
            (initial * ratio).toInt(),
            ((initial + (w * duration)) * ratio).toInt()
        )
    }
    fun unmap_line_frames(channel: Int, line_offset: Int) {
        if (this.flux_indicator > 0 && this.flag_cleared_in_flux) {
            return
        }

        this.channels[channel].lines[line_offset].beats.forEachIndexed { j: Int, tree: OpusTree<OpusEvent> ->
            this.unmap_frames(BeatKey(channel, line_offset, j), listOf())
        }
    }

    fun map_line_frames(channel: Int, line_offset: Int) {
        if (this.flux_indicator > 0 && this.flag_cleared_in_flux) {
            return
        }

        this.channels[channel].lines[line_offset].beats.forEachIndexed { j: Int, tree: OpusTree<OpusEvent> ->
            this.map_frames(BeatKey(channel, line_offset, j), listOf())
        }
    }
    fun unmap_channel_frames(channel: Int) {
        if (this.flux_indicator > 0 && this.flag_cleared_in_flux) {
            return
        }

        this.channels[channel].lines.forEachIndexed { i: Int, line: OpusChannel.OpusLine ->
            line.beats.forEachIndexed { j: Int, tree: OpusTree<OpusEvent> ->
                this.unmap_frames(BeatKey(channel, i, j), listOf())
            }
        }
    }

    fun map_channel_frames(channel: Int) {
        if (this.flux_indicator > 0 && this.flag_cleared_in_flux) {
            return
        }

        this.channels[channel].lines.forEachIndexed { i: Int, line: OpusChannel.OpusLine ->
            line.beats.forEachIndexed { j: Int, tree: OpusTree<OpusEvent> ->
                this.map_frames(BeatKey(channel, i, j), listOf())
            }
        }
    }

    fun <T> unmap_wrapper(beat_key: BeatKey, position: List<Int>, callback: () -> T): T {
        return this.flux_wrapper {
            this.unmap_frames(beat_key, position)
            val output = callback()
            this.map_frames(beat_key, position)
            output
        }
    }

    //-----End Layer Functions-------//
    override fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>) {
        this.flux_wrapper {
            this.unmap_channel_frames(channel)
            super.set_channel_instrument(channel, instrument)
            this.sample_handle_manager!!.select_bank(
                this.channels[channel].midi_channel,
                instrument.first
            )
            this.sample_handle_manager!!.change_program(
                this.channels[channel].midi_channel,
                instrument.second
            )
            this.map_channel_frames(channel)
        }
    }

    override fun set_channel_program(channel: Int, program: Int) {
        this.flux_wrapper {
            this.unmap_channel_frames(channel)
            this.sample_handle_manager!!.change_program(this.channels[channel].midi_channel, program)
            super.set_channel_program(channel, program)
            this.map_channel_frames(channel)
        }
    }

    override fun set_channel_bank(channel: Int, bank: Int) {
        if (this.flux_indicator == 0 || !this.flag_cleared_in_flux) {
            this.sample_handle_manager!!.select_bank(this.channels[channel].midi_channel, bank)
        }
        super.set_channel_bank(channel, bank)
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.unmap_wrapper(beat_key, position) {
            super.set_event(beat_key, position, event)
        }
    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this.unmap_wrapper(beat_key, position) {
            super.set_percussion_event(beat_key, position)
        }
    }

    override fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        this.flux_wrapper {
            this.unmap_line_frames(this.channels.size - 1, line_offset)
            super.set_percussion_instrument(line_offset, instrument)
            this.map_line_frames(this.channels.size - 1, line_offset)
        }
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<OpusEvent>) {
        this.unmap_wrapper(beat_key, position ?: listOf()) {
            super.replace_tree(beat_key, position, tree)
        }
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        this.flux_wrapper {
            super.new_channel(channel, lines, uuid)
            val working_channel = channel ?: max(0, this.channels.size - 2)

            this.frame_map.insert_channel(working_channel)
        }
    }

    override fun remove_channel(channel: Int) {
        this.flux_wrapper {
            super.remove_channel(channel)
            this.frame_map.remove_channel(channel)
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        this.flux_wrapper {
            super.insert_beat(beat_index, beats_in_column)

            if (this.flag_cleared_in_flux) {
                return@flux_wrapper
            }

            this.frame_map.insert_beat(beat_index)
        }
    }

    override fun remove_beat(beat_index: Int) {
        this.flux_wrapper {
            super.remove_beat(beat_index)

            if (this.flag_cleared_in_flux) {
                return@flux_wrapper
            }
        }
    }

    override fun remove_only(beat_key: BeatKey, position: List<Int>) {
        this.flux_wrapper {
            this.unmap_frames(beat_key, position)
            super.remove_only(beat_key, position)
        }
    }

    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        this.flux_wrapper {
            if (position.isNotEmpty()) {
                this.unmap_frames(
                    beat_key,
                    position.subList(0, position.size - 1)
                )
            }
            super.remove_standard(beat_key, position)

            if (position.isNotEmpty()) {
                this.map_frames(beat_key, position.subList(0, position.size - 1))
            }
        }
    }

    override fun remove_one_of_two(beat_key: BeatKey, position: List<Int>) {
        this.flux_wrapper {
            if (position.isNotEmpty()) {
                this.unmap_frames(
                    beat_key,
                    position.subList(0, position.size - 1)
                )
            }
            super.remove_one_of_two(beat_key, position)
            if (position.isNotEmpty()) {
                this.map_frames(beat_key, position.subList(0, position.size - 1))
            }
        }
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.flux_wrapper {
            this.unmap_frames(beat_key, listOf())
            super.unset(beat_key, position)
        }
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        // TODO: This could be more precise
        this.unmap_wrapper(beat_key, listOf()) {
            super.insert(beat_key, position)
        }
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        // TODO: This could be more precise
        this.unmap_wrapper(beat_key, listOf()) {
            super.insert_after(beat_key, position)
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        // TODO: This could be more precise
        this.unmap_wrapper(beat_key, listOf()) {
            super.split_tree(beat_key, position, splits)
        }
    }

    override fun on_project_changed() {
        this.flux_wrapper {
            super.on_project_changed()
            this.setup_sample_handle_manager()
            this.setup_frame_map()
        }
    }

    override fun clear() {
        this.clear_frame_map_data()
        super.clear()
    }

    fun clear_frame_map_data() {
        this.frame_map.clear()
        this.unmap_flags.clear()
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        this.flux_wrapper {
            for (i in 0 until this.beat_count) {
                this.unmap_frames(BeatKey(channel_a, line_a, i), listOf())
                this.unmap_frames(BeatKey(channel_b, line_b, i), listOf())
            }
            super.swap_lines(channel_a, line_a, channel_b, line_b)
            for (i in 0 until this.beat_count) {
                this.map_frames(BeatKey(channel_a, line_a, i), listOf())
                this.map_frames(BeatKey(channel_b, line_b, i), listOf())
            }
        }
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusChannel.OpusLine {
        return this.flux_wrapper {
            if (!this.flag_cleared_in_flux) {
                for (i in 0 until this.beat_count) {
                    this.unmap_frames(BeatKey(channel, line_offset, i), listOf())
                }
                this.frame_map.remove_line(channel, line_offset)
            }

            super.remove_line(channel, line_offset)
        }
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusChannel.OpusLine) {
        this.flux_wrapper {
            super.insert_line(channel, line_offset, line)

            if (!this.flag_cleared_in_flux) {
                this.frame_map.insert_line(channel, line_offset)
                for (i in 0 until this.beat_count) {
                    this.map_frames(BeatKey(channel, line_offset, i), listOf())
                }
            }
        }
    }

    override fun set_line_volume(channel: Int, line_offset: Int, volume: Int) {
        this.flux_wrapper {
            this.unmap_line_frames(channel, line_offset)
            super.set_line_volume(channel, line_offset, volume)
            this.map_line_frames(channel, line_offset)
        }
    }

    override fun set_tempo(new_tempo: Float) {
        this.clear_and_set_frames {
            this.frame_map.tempo = new_tempo
            super.set_tempo(new_tempo)
        }
    }

    override fun set_tuning_map(new_map: Array<Pair<Int, Int>>, mod_events: Boolean) {
        this.clear_and_set_frames {
            super.set_tuning_map(new_map, mod_events)
        }
    }

    override fun set_transpose(new_transpose: Int) {
        this.clear_and_set_frames {
            super.set_transpose(new_transpose)
        }
    }

    override fun set_tuning_map_and_transpose(tuning_map: Array<Pair<Int, Int>>, transpose: Int) {
        this.clear_and_set_frames {
            super.set_tuning_map_and_transpose(tuning_map, transpose)
        }
    }

    override fun apply_undo() {
        // Wrapping the apply_undo in the flux wrapper will cause the flag_cleared_in_flux
        // to be considered
        this.flux_wrapper {
            super.apply_undo()
        }
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this.unmap_wrapper(beat_key, position) {
            super.set_duration(beat_key, position, duration)
        }
    }
}