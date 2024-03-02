package com.qfs.pagan

import android.util.Log
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
import kotlin.math.pow

class PlaybackFrameMap(val opus_manager: OpusLayerBase, val sample_handle_manager: SampleHandleManager): FrameMap {
    private var simple_mode: Boolean = false // Simple mode ignores delays, and decays. Reduces Lode on cpu
    private val handle_map = HashMap<Int, SampleHandle>() // Handle UUID::Handle
    private val handle_range_map = HashMap<Int, IntRange>() // Handle UUID::Frame Range
    private val frame_map = HashMap<Int, MutableSet<Int>>() // Frame::Handle UUIDs

    private var setter_id_gen = 0
    private var setter_map = HashMap<Int,() -> Set<SampleHandle>>()
    private var setter_frame_map = HashMap<Int, MutableSet<Int>>()
    private val setter_range_map = HashMap<Int, IntRange>()
    private var cached_frame_count: Int? = null
    private val setter_overlaps = HashMap<Int, Array<Int>>()

    private val percussion_setter_ids = mutableSetOf<Int>()

    private var tempo = 0F
    private var beat_count = 0

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

        return output
    }

    override fun get_beat_frames(): HashMap<Int, IntRange> {
        val frames_per_beat = 60F * this.sample_handle_manager.sample_rate / this.opus_manager.tempo
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

            val frames_per_beat = 60F * this.sample_handle_manager.sample_rate / this.tempo
            this.cached_frame_count = max(this.cached_frame_count!! + 1, (this.beat_count * frames_per_beat).toInt())
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
        var sample_start_frame = start_frame - handle.volume_envelope.frames_delay
        var sample_end_frame = (end_frame + handle.get_release_duration()) - handle.volume_envelope.frames_delay
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
        this.tempo = 0F
        this.beat_count = 0

        this.setter_id_gen = 0
        this.setter_frame_map.clear()
        this.setter_map.clear()
        this.setter_range_map.clear()
        this.setter_overlaps.clear()
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

        val is_percussion = when (start_event) {
            is NoteOn -> start_event.channel == 9
            is NoteOn79 -> start_event.channel == 9
            else -> return
        }

        if (is_percussion) {
            this.percussion_setter_ids.add(setter_id)
        }

        // So percussion will be allow to be 6:4 louder than the other sections combined
        val std_perc_ratio = .4F

        this.setter_map[setter_id] = {
            val handles = when (start_event) {
                is NoteOn -> this.sample_handle_manager.gen_sample_handles(start_event)
                is NoteOn79 -> this.sample_handle_manager.gen_sample_handles(start_event)
                else -> setOf()
            }

            val overlap = if (is_percussion) {
                this.setter_overlaps[setter_id]!![1]
            } else {
                this.setter_overlaps[setter_id]!![0]
            }

            val maximum = 1f / 3f
            val minimum = 1f / 5f
            val delta = maximum - minimum

            var limit = maximum
            for (i in 1 until overlap - 1) {
                limit -= 2f.pow(0 - i) * delta
            }

            val handle_uuid_set = mutableSetOf<Int>()
            for (handle in handles) {
                handle.release_frame = end_frame - start_frame

                if (this.simple_mode) {
                    handle.volume_envelope.frames_release = 0
                    handle.volume_envelope.frames_delay = 0
                }

                handle_uuid_set.add(handle.uuid)
                val handle_volume_factor = handle.max_frame_value().toFloat() / Short.MAX_VALUE.toFloat()
                // won't increase sample's volume, but will use sample's actual volume if it is less than the available volume
                val sample_volume_adjustment = min(1F, limit / handle_volume_factor)

                handle.volume = handle.volume * sample_volume_adjustment * .7f // Not 100% sure about using this .7f factor here, but it seems to do the trick
            }

            handles
        }
    }

    fun parse_opus(force_simple_mode: Boolean = false) {
        this.clear()
        this.tempo = this.opus_manager.tempo.toFloat()
        this.beat_count = this.opus_manager.beat_count
        this.simple_mode = force_simple_mode

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
                    prev_abs_note = this.map_tree(beat_key, listOf(), working_tree, 1F, 0F, prev_abs_note)
                }
            }
        }

        this.calculate_overlaps()
    }

    private fun calculate_overlaps() {
        // TODO: Disregard non-parallel overlapping, ie a long note with short notes pressed over time
        val event_list = mutableListOf<Triple<Int, Int, Boolean>>()
        for ((handle_id,range) in this.setter_range_map) {
            event_list.add(Triple(range.first, handle_id, true))
            event_list.add(Triple(range.last, handle_id, false))
        }
        event_list.sortBy { it.first }

        val working_std_set = mutableSetOf<Int>()
        val working_perc_set = mutableSetOf<Int>()
        this.setter_overlaps.clear()
        // NOTE: Excluding percussion from overlap count, since they sort of exist in their own space
        // Percussion will still be attenuated to fit with the song, but a snare hit shouldn't make
        // Any notes played simultaneously play quieter
        for ((_, setter_id, setter_on) in event_list) {
            val is_perc = this.percussion_setter_ids.contains(setter_id)
            if (setter_on) {
                if (is_perc) {
                    for (id in working_std_set) {
                        this.setter_overlaps[id]!![1] += 1
                    }
                    for (id in working_perc_set) {
                        this.setter_overlaps[id]!![1] += 1
                    }

                    working_perc_set.add(setter_id)
                } else {
                    for (id in working_std_set) {
                        this.setter_overlaps[id]!![0] += 1
                    }
                    for (id in working_perc_set) {
                        this.setter_overlaps[id]!![0] += 1
                    }
                    working_std_set.add(setter_id)
                }
                this.setter_overlaps[setter_id] = arrayOf(working_std_set.size, working_perc_set.size)

            } else {
                if (is_perc) {
                    working_perc_set.remove(setter_id)
                } else {
                    working_std_set.remove(setter_id)
                }

            }
        }
    }

    private fun map_tree(beat_key: BeatKey, position: List<Int>, working_tree: OpusTree<OpusEvent>, relative_width: Float, relative_offset: Float, prev_note_value: Int): Int {
        if (!working_tree.is_leaf()) {
            val new_width = relative_width / working_tree.size.toFloat()
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

        val ratio = (60F * this.sample_handle_manager.sample_rate.toFloat() / this.tempo)
        val initial = relative_offset + beat_key.beat.toFloat()
        val start_frame = (initial * ratio).toInt()
        val end_frame = ((initial + (relative_width * duration)) * ratio).toInt()

        val start_event = this._gen_midi_event(event, beat_key)!!
        this.add_handles(start_frame, end_frame, start_event)

        return event.note
    }

    private fun _gen_midi_event(event: OpusEvent, beat_key: BeatKey): MIDIEvent? {
        if (this.opus_manager.is_percussion(beat_key.channel)) {
            return NoteOn(
                channel = 9,
                velocity = this.opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset),
                note = this.opus_manager.get_percussion_instrument(beat_key.line_offset) + 27
            )
        }
        // Assume event is *not* relative as it is modified in map_tree() before _gen_midi_event is called
        val value = event.note
        val radix = this.opus_manager.tuning_map.size
        val octave = value / radix
        val offset = this.opus_manager.tuning_map[value % radix]

        // This offset is calculated so the tuning map always reflects correctly
        val transpose_offset = 12F * this.opus_manager.transpose.toFloat() / radix.toFloat()
        val std_offset = (offset.first.toFloat() * 12F / offset.second.toFloat())

        val note = (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21
        val velocity = this.opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)

        return if (this.opus_manager.is_tuning_standard()) {
            NoteOn(
                channel = this.opus_manager.channels[beat_key.channel].midi_channel,
                velocity = velocity,
                note = note
            )
        } else {
            val bend = (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512F).toInt()
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
