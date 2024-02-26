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
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.structure.OpusTree
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class PlaybackFrameMap(val opus_manager: OpusLayerBase, val sample_handle_manager: SampleHandleManager): FrameMap {
    private val handle_map = HashMap<Int, SampleHandle>() // Handle UUID::Handle
    private val handle_range_map = HashMap<Int, IntRange>() // Handle UUID::Frame Range
    private val frame_map = HashMap<Int, MutableSet<Int>>() // Frame::Handle UUIDs

    private var setter_id_gen = 0
    private var setter_map = HashMap<Int,() -> Set<SampleHandle>>()
    private var setter_frame_map = HashMap<Int, MutableSet<Int>>()
    private val setter_range_map = HashMap<Int, IntRange>()
    private var cached_frame_count: Int? = null
    private var max_overlap: Int = 0

    override fun get_new_handles(frame: Int): Set<SampleHandle>? {
        // Check frame a buffer ahead to make sure frames are added as accurately as possible
        this.check_frame(frame + this.sample_handle_manager.buffer_size)

        if (!this.frame_map.containsKey(frame)) {
            return null
        }

        val output = mutableSetOf<SampleHandle>()

        if (this.frame_map.containsKey(frame)) {
            for (uuid in this.frame_map[frame]!!) {
                output.add(this.handle_map[uuid] ?: continue)
            }
        }

        for (handle in output) {
            val max_volume = (handle.max_frame_value().toDouble() / Short.MAX_VALUE.toDouble())
            handle.volume *= min((1.0 / this.max_overlap.toDouble()) / max_volume, 1.0)
        }

        return output
    }

    override fun get_beat_frames(): HashMap<Int, IntRange> {
        val frames_per_beat = 60.0 * this.sample_handle_manager.sample_rate / this.opus_manager.tempo
        val output = HashMap<Int, IntRange>()

        for (i in 0 until this.opus_manager.beat_count) {
            output[i] = (frames_per_beat * i).toInt() until (frames_per_beat * (i + 1)).toInt()
        }
        return output
    }
    override fun get_active_handles(frame: Int): Set<Pair<Int, SampleHandle>> {
        val output = mutableSetOf<Pair<Int, SampleHandle>>()

        for ((uuid, range) in this.handle_range_map) {
            if (!range.contains(frame)) {
                continue
            }

            val handle = this.handle_map[uuid]!!
            output.add(Pair(range.first, handle))
        }

        // NOTE: May miss tail end of samples with long decays, but for now, for my purposes, will be fine
        val setter_ids_to_remove = mutableSetOf<Int>()
        for ((setter_id, range) in this.setter_range_map) {
            if (range.contains(frame)) {
                setter_ids_to_remove.add(setter_id)
                for (i in range) {
                    this.setter_frame_map.remove(i)
                }
                for (handle in this.setter_map.remove(setter_id)!!()) {
                    this.map_real_handle(handle, range.first)
                    output.add(Pair(this.handle_range_map[handle.uuid]!!.first, handle))
                }
            }
        }

        for (setter_id in setter_ids_to_remove) {
            this.setter_range_map.remove(setter_id)
        }

        return output
    }

    override fun get_size(): Int {
        if (this.cached_frame_count == null) {
            this.cached_frame_count = -1
            for (range in this.setter_range_map.values) {
                this.cached_frame_count = max(range.last, this.cached_frame_count!!)
            }

            val frames_per_beat = 60.0 * this.sample_handle_manager.sample_rate / this.opus_manager.tempo
            this.cached_frame_count = max(this.cached_frame_count!! + 1, (this.opus_manager.beat_count * frames_per_beat).toInt())
        }
        return this.cached_frame_count!!
    }

    // End FrameMap Interface --------------------------
    fun check_frame(frame: Int) {
        if (!this.setter_frame_map.containsKey(frame)) {
            return
        }

        for (setter_id in this.setter_frame_map.remove(frame)!!) {
            val handles = this.setter_map.remove(setter_id)?.let { it() } ?: continue
            this.setter_range_map.remove(setter_id)
            for (handle in handles) {
                this.map_real_handle(handle, frame)
            }
        }
    }

    fun map_real_handle(handle: SampleHandle, start_frame: Int) {
        val end_frame = handle.release_frame!! + start_frame
        var sample_end_frame = (end_frame + handle.get_release_duration()) - handle.volume_envelope.frames_delay
        var sample_start_frame = start_frame - handle.volume_envelope.frames_delay
        if (sample_start_frame < 0) {
            sample_end_frame -= sample_start_frame
            sample_start_frame = 0
        }
        this.handle_range_map[handle.uuid] = sample_start_frame .. sample_end_frame
        this.handle_map[handle.uuid] = handle
        if (!this.frame_map.containsKey(sample_start_frame)) {
            this.frame_map[sample_start_frame] = mutableSetOf()
        }
        this.frame_map[sample_start_frame]!!.add(handle.uuid)
    }

    fun clear() {
        this.frame_map.clear()
        this.handle_map.clear()
        this.handle_range_map.clear()

        this.setter_id_gen = 0
        this.setter_frame_map.clear()
        this.setter_map.clear()
        this.setter_range_map.clear()
        this.cached_frame_count = null
    }

    fun add_handles(start_frame: Int, end_frame: Int, start_event: MIDIEvent) {
        val setter_id = this.setter_id_gen++

        if (!this.setter_frame_map.containsKey(start_frame)) {
            this.setter_frame_map[start_frame] = mutableSetOf()
        }
        this.setter_frame_map[start_frame]!!.add(setter_id)
        this.setter_range_map[setter_id] = start_frame..end_frame

        this.cached_frame_count = null

        this.setter_map[setter_id] = {
            val handles = when (start_event) {
                is NoteOn -> this.sample_handle_manager.gen_sample_handles(start_event)
                is NoteOn79 -> this.sample_handle_manager.gen_sample_handles(start_event)
                else -> setOf()
            }

            val handle_uuid_set = mutableSetOf<Int>()
            for (handle in handles) {
                handle.release_frame = end_frame - start_frame
                handle_uuid_set.add(handle.uuid)
            }


            handles
        }
    }

    fun parse_opus() {
        this.clear()

        this.opus_manager.channels.forEach { channel: OpusChannel ->
            val instrument = channel.get_instrument()
            this.sample_handle_manager.select_bank(channel.midi_channel, instrument.first)
            this.sample_handle_manager.change_program(channel.midi_channel, instrument.second)
        }


        this.opus_manager.channels.forEachIndexed { c: Int, channel: OpusChannel ->
            channel.lines.forEachIndexed { l: Int, line: OpusChannel.OpusLine ->
                var prev_abs_note = 0
                for (b in 0 until this.opus_manager.beat_count) {
                    val beat_key = BeatKey(c,l,b)
                    val working_tree = this.opus_manager.get_tree(beat_key)
                    prev_abs_note = this.map_tree(beat_key, listOf(), working_tree, 1.0, 0.0, prev_abs_note)
                }
            }
        }

        this.max_overlap = this.calculate_max_overlap()
    }

    private fun calculate_max_overlap(): Int {
        val event_list = mutableListOf<Pair<Int, Boolean>>()
        for (range in this.setter_range_map.values) {
            event_list.add(Pair(range.first, true))
            event_list.add(Pair(range.last, false))
        }
        event_list.sortBy { it.first }
        var max_overlap = 0
        var count = 0
        for ((_, sample_on) in event_list) {
            if (sample_on) {
                count += 1
                max_overlap = max(max_overlap, count)
            } else {
                count -= 1
            }
        }
        return max_overlap
    }

    private fun map_tree(beat_key: BeatKey, position: List<Int>, working_tree: OpusTree<OpusEvent>, relative_width: Double, relative_offset: Double, prev_note_value: Int): Int {
        if (!working_tree.is_leaf()) {
            val new_width = relative_width / working_tree.size.toDouble()
            var new_working_value = prev_note_value
            for (i in 0 until working_tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                new_working_value = this.map_tree(beat_key, new_position, working_tree[i], new_width, relative_offset + (new_width * i), new_working_value)
            }
            return new_working_value
        } else if (!working_tree.is_event()) {
            return prev_note_value
        }

        val event = working_tree.get_event()!!.copy()
        if (event.relative) {
            event.note = event.note + prev_note_value
            event.relative = false
        }

        val duration = event.duration

        val ratio = (60.0 * this.sample_handle_manager.sample_rate.toDouble() / this.opus_manager.tempo)
        val initial = relative_offset + beat_key.beat.toDouble()
        val start_frame = (initial * ratio).toInt()
        val end_frame = ((initial + (relative_width * duration)) * ratio).toInt()

        val start_event = this._gen_midi_event(event, beat_key)!!
        this.add_handles(start_frame, end_frame, start_event)

        return event.note
    }

    private fun _gen_midi_event(event: OpusEvent, beat_key: BeatKey): MIDIEvent? {
        if (this.opus_manager.is_percussion(beat_key.channel)) {
            return NoteOn(
                channel=9,
                velocity=this.opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset),
                note=this.opus_manager.get_percussion_instrument(beat_key.line_offset) + 27
            )
        }
        // Assume event is *not* relative as it is modified in map_tree() before _gen_midi_event is called
        val value = event.note
        val radix = this.opus_manager.tuning_map.size
        val octave = value / radix
        val offset = this.opus_manager.tuning_map[value % radix]

        // This offset is calculated so the tuning map always reflects correctly
        val transpose_offset = 12.0 * this.opus_manager.transpose.toDouble() / radix.toDouble()
        val std_offset = (offset.first.toDouble() * 12.0 / offset.second.toDouble())

        val note = (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21
        val velocity = this.opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)

        return if (this.opus_manager.is_tuning_standard()) {
            NoteOn(
                channel = this.opus_manager.channels[beat_key.channel].midi_channel,
                velocity = velocity,
                note = note
            )
        } else {
            val bend = (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512.0).toInt()
            NoteOn79(
                index = 0, // Set index as note is applied
                channel = this.opus_manager.channels[beat_key.channel].midi_channel,
                velocity = velocity shl 8,
                note = note,
                bend = bend
            )
        }
    }

}
