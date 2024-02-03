package com.qfs.pagan

import com.qfs.apres.Midi
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

open class OpusLayerFrameMap: OpusLayerCursor(), FrameMap {
    var sample_handle_manager: SampleHandleManager? = null
    var quick_map_sample_handles =  HashMap<Pair<BeatKey, List<Int>>, Set<Int>>()
    var frame_map = HashMap<Int, MutableSet<Int>>()
    var handle_map = HashMap<Int, SampleHandle>()
    private val handle_range_map = HashMap<Int, Pair<Int, Int>>()

    //-----Layer Functions-------//
    fun set_sample_handle_manager(new_manager: SampleHandleManager) {
        this.sample_handle_manager = new_manager
        this.frame_map.clear()
        this.handle_map.clear()
        this.setup_frame_map()
    }

    fun unset_sample_handle_manager() {
        this.sample_handle_manager = null
        this.frame_map.clear()
        this.handle_map.clear()
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
                note=this.get_percussion_instrument(beat_key.line_offset)
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
        val working_tree = this.get_tree(beat_key, position)
        if (!working_tree.is_leaf()) {
            for (i in 0 until working_tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.unmap_frames(beat_key, new_position)
            }
        } else if (!working_tree.is_event()) {
            return
        }

        val sample_handles = this.quick_map_sample_handles.remove(Pair(beat_key, position)) ?: return
        for (uuid in sample_handles) {
            val (start_frame, _) = this.handle_range_map.remove(uuid) ?: continue
            if (this.frame_map.containsKey(start_frame)) {
                this.frame_map[start_frame]!!.remove(uuid)
            }
        }
    }

    fun map_frames(beat_key: BeatKey, position: List<Int>) {
        val working_tree = this.get_tree(beat_key, position)
        if (!working_tree.is_leaf()) {
            for (i in 0 until working_tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.map_frames(beat_key, new_position)
            }
        } else if (!working_tree.is_event()) {
            return
        }

        val (start_frame, end_frame) = this.get_frame_range(beat_key, position)

        val start_event = gen_midi_event(beat_key, position) ?: return
        val handles = when (start_event) {
            is NoteOn -> {
                this.sample_handle_manager!!.gen_sample_handles(start_event)
            }
            is NoteOn79 -> {
                this.sample_handle_manager!!.gen_sample_handles(start_event)
            }
            else -> return
        }

        if (!this.frame_map.containsKey(start_frame)) {
            this.frame_map[start_frame] = mutableSetOf()
        }

        val uuids = mutableSetOf<Int>()
        for (handle in handles) {
            uuids.add(handle.uuid)

            handle.release_frame = end_frame - start_frame
            val sample_end_frame = start_frame + handle.release_frame!! + handle.frame_count_release
            this.handle_range_map[handle.uuid] = Pair(start_frame, sample_end_frame)
            this.frame_map[start_frame]!!.add(handle.uuid)
            this.handle_map[handle.uuid] = handle
        }
        this.quick_map_sample_handles[Pair(beat_key, position)] = uuids
    }

    fun get_frame_range(beat_key: BeatKey, position: List<Int>): Pair<Int, Int> {
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

        val duration = if (working_tree.is_event()) {
            working_tree.get_event()!!.duration
        } else {
            1
        }
        val ratio = (60.0 * this.sample_handle_manager!!.sample_rate / this.tempo)
        val initial = offset + beat_key.beat.toDouble()
        return Pair(
            (initial * ratio).toInt(),
            ((initial + (w * duration)) * ratio).toInt()
        )
    }

    fun remap_channel_frames(channel: Int) {
        this.channels[channel].lines.forEachIndexed { i: Int, line: OpusChannel.OpusLine ->
            line.beats.forEachIndexed { j: Int, tree: OpusTree<OpusEvent> ->
                this.unmap_frames(BeatKey(channel, i, j), listOf())
            }
        }
        this.channels[channel].lines.forEachIndexed { i: Int, line: OpusChannel.OpusLine ->
            line.beats.forEachIndexed { j: Int, tree: OpusTree<OpusEvent> ->
                this.map_frames(BeatKey(channel, i, j), listOf())
            }
        }

    }
    //-----End Layer Functions-------//
    override fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>) {
        super.set_channel_instrument(channel, instrument)
        this.sample_handle_manager!!.select_bank(this.channels[channel].midi_channel, instrument.first)
        this.sample_handle_manager!!.change_program(this.channels[channel].midi_channel, instrument.second)
        this.remap_channel_frames(channel)
    }

    override fun set_channel_program(channel: Int, program: Int) {
        this.sample_handle_manager!!.change_program(this.channels[channel].midi_channel, program)
        super.set_channel_program(channel, program)
        this.remap_channel_frames(channel)
    }

    override fun set_channel_bank(channel: Int, bank: Int) {
        this.sample_handle_manager!!.select_bank(this.channels[channel].midi_channel, bank)
        super.set_channel_bank(channel, bank)
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.unmap_frames(beat_key, position)
        super.set_event(beat_key, position, event)
        this.map_frames(beat_key, position)
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<OpusEvent>) {
        this.unmap_frames(beat_key, position ?: listOf())
        super.replace_tree(beat_key, position, tree)
        this.map_frames(beat_key, position ?: listOf())
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        super.new_channel(channel, lines, uuid)
        val working_channel = channel ?: max(0, this.channels.size - 2)
        val sorted_keys = this.quick_map_sample_handles.keys.sortedByDescending { it.first.channel }
        for ((beat_key, position) in sorted_keys) {
            if (beat_key.channel < working_channel) {
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

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
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

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        super.insert_beat(beat_index, beats_in_column)

        val sorted_keys = this.quick_map_sample_handles.keys.sortedByDescending { it.first.beat }
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
        }

        val frames_per_beat = 60.0 * this.sample_handle_manager!!.sample_rate / this.tempo
        val first_frame = beat_index * frames_per_beat
        for (frame in this.frame_map.keys.sortedByDescending { it }) {
            if (frame < first_frame) {
                break
            }
            this.frame_map[frame + frames_per_beat.toInt()] = this.frame_map.remove(frame)!!
        }

        for ((uuid, pair) in this.handle_range_map) {
            if (pair.first < first_frame) {
                continue
            }

            this.handle_range_map[uuid] = Pair(
                if (pair.first < first_frame) {
                    pair.first
                } else {
                    pair.first + frames_per_beat.toInt()
                },
                if (pair.first < first_frame) {
                    pair.second
                } else {
                    pair.second + frames_per_beat.toInt()
                }
            )
        }
    }

    override fun remove_beat(beat_index: Int) {
        super.remove_beat(beat_index)

        val sorted_keys = this.quick_map_sample_handles.keys.sortedBy { it.first.beat }
        for ((beat_key, position) in sorted_keys) {
            if (beat_key.beat < beat_index) {
                continue
            } else if (beat_key.beat == beat_index) {
                this.quick_map_sample_handles.remove(Pair(beat_key, position))
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

            this.quick_map_sample_handles[new_key] = this.quick_map_sample_handles.remove(Pair(beat_key, position))!!
        }

        val frames_per_beat = 60.0 * this.sample_handle_manager!!.sample_rate / this.tempo
        val first_frame = beat_index * frames_per_beat
        val del_range = (first_frame.toInt() until frames_per_beat.toInt())
        val sample_uuids_to_delete = mutableSetOf<Int>()

        for (frame in this.frame_map.keys.sortedBy { it }) {
            if (frame < first_frame) {
                continue
            } else if (del_range.contains(frame)) {
                for (uuid in this.frame_map.remove(frame)!!) {
                    sample_uuids_to_delete.add(uuid)
                }
                continue
            }

            this.frame_map[frame - frames_per_beat.toInt()] = this.frame_map.remove(frame)!!
        }

        for (uuid in sample_uuids_to_delete) {
            this.handle_range_map.remove(uuid)
            this.handle_map.remove(uuid)
        }

        for ((uuid, pair) in this.handle_range_map) {
            if (pair.first < first_frame) {
                continue
            }
            this.handle_range_map[uuid] = Pair(
                if (pair.first < first_frame) {
                    pair.first
                } else {
                   pair.first - frames_per_beat.toInt()
                },
                if (pair.first < first_frame) {
                    pair.second
                } else {
                    pair.second - frames_per_beat.toInt()
                }
            )
        }
    }

    override fun remove_only(beat_key: BeatKey, position: List<Int>) {
        this.unmap_frames(beat_key, position)
        super.remove_only(beat_key, position)
    }

    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
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

    override fun remove_one_of_two(beat_key: BeatKey, position: List<Int>) {
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

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        if (position.isNotEmpty()) {
            this.unmap_frames(
                beat_key,
                position.subList(0, position.size - 1)
            )
        }

        super.insert(beat_key, position)

        if (position.isNotEmpty()) {
            this.map_frames(beat_key, position.subList(0, position.size - 1))
        }
    }

    override fun load(bytes: ByteArray, new_path: String?) {
        super.load(bytes, new_path)
        this.setup_frame_map()
    }

    override fun import_midi(midi: Midi) {
        super.import_midi(midi)
        this.setup_frame_map()
    }

    override fun get_new_handles(frame: Int): Set<SampleHandle>? {
        if (!this.frame_map.containsKey(frame)) {
            return null
        }

        val output = mutableSetOf<SampleHandle>()
        for (uuid in this.frame_map[frame]!!) {
            this.handle_map[uuid]!!.set_working_frame(0)
            this.handle_map[uuid]!!.is_dead = false
            output.add(this.handle_map[uuid]!!)
        }
        return output
    }

    override fun get_beat_frames(): List<Int> {
        val frames_per_beat = 60.0 * this.sample_handle_manager!!.sample_rate / this.tempo
        return List(this.beat_count) { i ->
            (frames_per_beat * (i + 1)).toInt()
        }
    }

    override fun clear() {
        this.frame_map.clear()
        super.clear()
    }

    fun setup_frame_map() {
        this.channels.forEachIndexed { c: Int, channel: OpusChannel ->
            channel.lines.forEachIndexed { l: Int, line: OpusChannel.OpusLine ->
                for (b in 0 until this.beat_count) {
                    this.map_frames(BeatKey(c, l, b), listOf())
                }
            }
        }
    }
}