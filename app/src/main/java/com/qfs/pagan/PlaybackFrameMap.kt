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
import kotlinx.coroutines.runBlocking
import kotlin.math.floor
import kotlin.math.max

class PlaybackFrameMap(val opus_manager: OpusLayerBase, val sample_handle_manager: SampleHandleManager): FrameMap {
    var handle_set_id_gen = 0

    private var handle_set_map = HashMap<Int,() -> Set<SampleHandle>>()
    private var frame_map = HashMap<Int, MutableSet<Int>>()
    private val handle_range_map = HashMap<Int, IntRange>()
    var unmapped_flags = HashMap<List<Int>, Boolean>()
    /*
     NOTE: key is List where the first three entries are a BeatKey and the rest is the position since a Pair() would
     be matched by instance rather than content
    */
    private var quick_map_sample_handles =  HashMap<List<Int>, Int>()
    internal var cached_frame_count: Int? = null
    private var initial_delay_handles = HashMap<Int, Int>()

    override fun get_new_handles(frame: Int): Set<SampleHandle>? {
        if (!this.frame_map.containsKey(frame)) {
            return null
        }

        val output = mutableSetOf<SampleHandle>()
        for (uuid in this@PlaybackFrameMap.frame_map[frame]!!) {
            output.addAll(this@PlaybackFrameMap.handle_set_map[uuid]!!())
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
        runBlocking {
            for ((uuid, range) in this@PlaybackFrameMap.handle_range_map) {
                if (range.contains(frame)) {
                    for (handle in this@PlaybackFrameMap.handle_set_map[uuid]!!()) {
                        output.add(Pair(max(0, range.first - handle.volume_envelope.frames_delay), handle))
                    }
                }
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

            val frames_per_beat = 60.0 * this.sample_handle_manager.sample_rate / this.opus_manager.tempo
            this.cached_frame_count = max(this.cached_frame_count!! + 1, (this.opus_manager.beat_count * frames_per_beat).toInt())
        }
        return this.cached_frame_count!!
    }

    // End FrameMap Interface --------------------------
    fun has_quick_key(key: List<Int>): Boolean {
        return this.quick_map_sample_handles.contains(key)
    }

    fun clear() {
        this.frame_map.clear()
        this.unmapped_flags.clear()
        this.handle_set_map.clear()
        this.handle_range_map.clear()
        this.quick_map_sample_handles.clear()
        this.cached_frame_count = null
    }

    fun add_handles(quick_key: List<Int>, start_frame: Int, end_frame: Int, start_event: MIDIEvent) {
        val handle_id = this.handle_set_id_gen++

        // TODO: Reimplement release_duration and frames_delay comepensation
        //var max_end_frame = 0
        //var min_start_frame = Int.MAX_VALUE
        //val uuids = mutableSetOf<Int>()
        //for (handle in handles) {
        //    val sample_end_frame = (end_frame + handle.get_release_duration()) - handle.volume_envelope.frames_delay
        //    val sample_start_frame = start_frame - handle.volume_envelope.frames_delay

        //    max_end_frame = max(max_end_frame, sample_end_frame)
        //    min_start_frame = min(min_start_frame, sample_start_frame)

        //    handle.release_frame = end_frame - start_frame
        //    uuids.add(handle_id)

        //    if (!this.frame_map.containsKey(sample_start_frame)) {
        //        this.frame_map[sample_start_frame] = mutableSetOf()
        //    }
        //    this.frame_map[sample_start_frame]!!.add(handle_id)

        //    this.handle_range_map[handle_id] = sample_start_frame..sample_end_frame
        //    if (sample_start_frame < 0) {
        //        this.initial_delay_handles[handle_id] = sample_start_frame
        //    }
        // }

        if (!this.frame_map.containsKey(start_frame)) {
            this.frame_map[start_frame] = mutableSetOf()
        }
        this.frame_map[start_frame]!!.add(handle_id)

        this.handle_range_map[handle_id] = start_frame..end_frame

        this.quick_map_sample_handles[quick_key] = handle_id

        this.unmapped_flags[quick_key] = false
        this.cached_frame_count = null
        this@PlaybackFrameMap.handle_set_map[handle_id] =  {
            val handles = when (start_event) {
                is NoteOn -> this@PlaybackFrameMap.sample_handle_manager.gen_sample_handles(
                    start_event
                )

                is NoteOn79 -> this@PlaybackFrameMap.sample_handle_manager.gen_sample_handles(
                    start_event
                )

                else -> setOf()
            }

            for (handle in handles) {
                handle.release_frame = end_frame - start_frame
            }

            handles
        }
    }

    fun remove_handle_set(key: List<Int>) {
        this.unmapped_flags[key] = true

        val uuid = this.quick_map_sample_handles.remove(key) ?: return

        this.handle_set_map.remove(uuid)
        this.initial_delay_handles.remove(uuid)

        // reminder: 'end_frame' here is the last active frame in the sample, including decay
        val frame_range = this.handle_range_map.remove(uuid) ?: return
        if (this.frame_map.containsKey(frame_range.first)) {
            this.frame_map[frame_range.first]!!.remove(uuid)
            if (this.frame_map[frame_range.first]!!.isEmpty()) {
                this.frame_map.remove(frame_range.first)
            }
        }

        this.cached_frame_count = null
    }

    fun parse_opus() {
        this.clear()
        this.opus_manager.channels.forEach { channel: OpusChannel ->
            val instrument = channel.get_instrument()
            this.sample_handle_manager.select_bank(channel.midi_channel, instrument.first)
            this.sample_handle_manager.change_program(channel.midi_channel, instrument.second)
        }

        this.unmapped_flags.clear()

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

        val quick_key = mutableListOf(
            beat_key.channel,
            beat_key.line_offset,
            beat_key.beat
        )
        quick_key.addAll(position)

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
        this.add_handles(quick_key, start_frame, end_frame, start_event)

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
