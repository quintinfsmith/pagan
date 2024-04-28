package com.qfs.pagan

import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.FrameMap
import com.qfs.apres.soundfontplayer.SampleHandle
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusEventSTD
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow

class PlaybackFrameMap(val opus_manager: OpusLayerBase, private val _sample_handle_manager: SampleHandleManager): FrameMap {
    private var _simple_mode: Boolean = false // Simple mode ignores delays, and decays. Reduces Lode on cpu
    private val _handle_map = HashMap<Int, SampleHandle>() // Handle UUID::Handle
    private val _handle_range_map = HashMap<Int, IntRange>() // Handle UUID::Frame Range
    private val _frame_map = HashMap<Int, MutableSet<Int>>() // Frame::Handle UUIDs

    private var _setter_id_gen = 0
    private var _setter_map = HashMap<Int,() -> Set<SampleHandle>>()
    private var _setter_frame_map = HashMap<Int, MutableSet<Int>>()
    private val _setter_range_map = HashMap<Int, IntRange>()
    private var _cached_frame_count: Int? = null
    private val _cached_beat_frames = HashMap<Int, IntRange>()
    private val _setter_overlaps = HashMap<Int, Array<Int>>()

    private val _tempo_map = HashMap<Int, Float>() // Frame::Tempo

    private val _percussion_setter_ids = mutableSetOf<Int>()

    private var _beat_count = 0

    override fun get_new_handles(frame: Int): Set<SampleHandle>? {
        // Check frame a buffer ahead to make sure frames are added as accurately as possible
        this.check_frame(frame + this._sample_handle_manager.buffer_size)

        if (!this._frame_map.containsKey(frame)) {
            return null
        }

        val output = mutableSetOf<SampleHandle>()

        if (this._frame_map.containsKey(frame)) {
            for (uuid in this._frame_map[frame]!!) {
                output.add(this._handle_map[uuid] ?: continue)
            }
        }

        return output
    }


    override fun get_beat_frames(): HashMap<Int, IntRange> {
        if (this._cached_beat_frames.isNotEmpty()) {
            return this._cached_beat_frames
        }

        val frames_per_minute = 60F * this._sample_handle_manager.sample_rate
        val tempos: MutableList<Pair<Int, Float>> = this._tempo_map.toList().toMutableList()
        tempos.sortBy { it.first }

        val beats = mutableListOf<Int>(0)

        var working_frame = tempos.first().first
        var working_tempo = tempos.first().second

        var frames_per_beat = (frames_per_minute / working_tempo).toInt()

        // First, just get the frame of each beat
        while (beats.size <= this.opus_manager.beat_count) {
            val next_change_frame = if (tempos.isNotEmpty()) {
                tempos.first().first
            } else {
                // Keep next change frame out of reach
                working_frame + frames_per_beat + 1
            }

            var next_frame = working_frame + frames_per_beat
            if (next_frame > next_change_frame) {
                var working_ratio = 0f
                while (tempos.isNotEmpty() && tempos.first().first < next_frame) {
                    working_ratio += (next_change_frame - working_frame).toFloat() / (frames_per_beat.toFloat() * (1f - working_ratio))

                    working_tempo = tempos.removeFirst().second
                    frames_per_beat = (frames_per_minute / working_tempo).toInt()

                    working_frame = next_change_frame
                    next_frame = (working_frame + ((1f - working_ratio) * frames_per_beat.toFloat())).toInt()
                }
            }
            beats.add(next_frame)
            working_frame = next_frame
        }

        // Convert beat frames to ranges
        for (i in 1 until beats.size) {
            this._cached_beat_frames[i - 1] = beats[i - 1] until beats[i]
        }

        return this._cached_beat_frames
    }

    override fun get_active_handles(frame: Int): Set<Pair<Int, SampleHandle>> {
        val output = mutableSetOf<Pair<Int, SampleHandle>>()

        for ((uuid, range) in this._handle_range_map) {
            if (!range.contains(frame)) {
                continue
            }

            val handle = this._handle_map[uuid]!!
            output.add(Pair(range.first, handle))
        }

        // NOTE: May miss tail end of samples with long decays, but for now, for my purposes, will be fine
        val setter_ids_to_remove = mutableSetOf<Int>()
        for ((setter_id, range) in this._setter_range_map) {
            if (range.contains(frame)) {
                setter_ids_to_remove.add(setter_id)
                for (i in range) {
                    this._setter_frame_map.remove(i)
                }
                for (handle in this._setter_map.remove(setter_id)!!()) {
                    this._map_real_handle(handle, range.first)
                    output.add(Pair(this._handle_range_map[handle.uuid]!!.first, handle))
                }
            }
        }

        for (setter_id in setter_ids_to_remove) {
            this._setter_range_map.remove(setter_id)
        }

        return output
    }

    override fun get_size(): Int {
        if (this._cached_frame_count == null) {
            this._cached_frame_count = -1
            for (range in this._setter_range_map.values) {
                this._cached_frame_count = max(range.last, this._cached_frame_count!!)
            }
            var beat_frames = this.get_beat_frames()
            this._cached_frame_count = beat_frames[beat_frames.keys.max()]!!.last

        }
        return this._cached_frame_count!!
    }

    // End FrameMap Interface --------------------------
    fun check_frame(frame: Int) {
        if (!this._setter_frame_map.containsKey(frame)) {
            return
        }

        for (setter_id in this._setter_frame_map.remove(frame)!!) {
            val handles = this._setter_map.remove(setter_id)?.let { it() } ?: continue
            this._setter_range_map.remove(setter_id)
            for (handle in handles) {
                this._map_real_handle(handle, frame)
            }
        }
    }

    private fun _map_real_handle(handle: SampleHandle, start_frame: Int) {
        val end_frame = handle.release_frame!! + start_frame
        var sample_start_frame = start_frame - handle.volume_envelope.frames_delay
        var sample_end_frame = (end_frame + handle.get_release_duration()) - handle.volume_envelope.frames_delay
        if (sample_start_frame < 0) {
            sample_end_frame -= sample_start_frame
            sample_start_frame = 0
        }

        this._handle_range_map[handle.uuid] = sample_start_frame .. sample_end_frame
        this._handle_map[handle.uuid] = handle
        if (!this._frame_map.containsKey(sample_start_frame)) {
            this._frame_map[sample_start_frame] = mutableSetOf()
        }
        this._frame_map[sample_start_frame]!!.add(handle.uuid)
    }

    fun clear() {
        this._frame_map.clear()
        this._handle_map.clear()
        this._handle_range_map.clear()
        this._tempo_map.clear()
        this._beat_count = 0
        this._cached_beat_frames.clear()

        this._setter_id_gen = 0
        this._setter_frame_map.clear()
        this._setter_map.clear()
        this._setter_range_map.clear()
        this._setter_overlaps.clear()
        this._cached_frame_count = null
    }

    private fun _add_handles(start_frame: Int, end_frame: Int, start_event: MIDIEvent) {
        val setter_id = this._setter_id_gen++

        if (!this._setter_frame_map.containsKey(start_frame)) {
            this._setter_frame_map[start_frame] = mutableSetOf()
        }
        this._setter_frame_map[start_frame]!!.add(setter_id)
        this._setter_range_map[setter_id] = start_frame..end_frame

        this._cached_frame_count = null

        val is_percussion = when (start_event) {
            is NoteOn -> start_event.channel == 9
            is NoteOn79 -> start_event.channel == 9
            else -> return
        }

        if (is_percussion) {
            this._percussion_setter_ids.add(setter_id)
        }

        this._setter_map[setter_id] = {
            val handles = when (start_event) {
                is NoteOn -> this._sample_handle_manager.gen_sample_handles(start_event)
                is NoteOn79 -> this._sample_handle_manager.gen_sample_handles(start_event)
                else -> setOf()
            }

            val overlap = if (is_percussion) {
                1
            } else {
                this._setter_overlaps[setter_id]!![0] + this._setter_overlaps[setter_id]!![1]
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

                if (this._simple_mode) {
                    handle.volume_envelope.frames_release = 0
                    handle.volume_envelope.frames_delay = 0
                }

                handle_uuid_set.add(handle.uuid)
                // won't increase sample's volume, but will use sample's actual volume if it is less than the available volume
                val sample_volume_adjustment = limit // min(1F, limit / handle_volume_factor)

                handle.volume = if (is_percussion) {
                    handle.volume * sample_volume_adjustment
                } else {
                    handle.volume * sample_volume_adjustment * .6f // Not 100% sure about using this factor here, but it seems to do the trick
                }
            }

            handles
        }
    }

    fun parse_opus(force_simple_mode: Boolean = false) {
        this.clear()
        this._beat_count = this.opus_manager.beat_count
        this._simple_mode = force_simple_mode

        for (channel in this.opus_manager.channels) {
            val instrument = channel.get_instrument()
            this._sample_handle_manager.select_bank(channel.midi_channel, instrument.first)
            this._sample_handle_manager.change_program(channel.midi_channel, instrument.second)
        }
        this.map_tempo_changes()
        this.get_beat_frames()

        this.opus_manager.channels.forEachIndexed { c: Int, channel: OpusChannel ->
            for (l  in channel.lines.indices) {
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

    fun map_tempo_changes() {
        var working_frame = 0
        val controller = this.opus_manager.controllers.get_controller(ControlEventType.Tempo)
        var working_tempo = (controller.initial_event as OpusTempoEvent).value

        val frames_per_minute = 60F * this._sample_handle_manager.sample_rate
        var frames_per_beat = (frames_per_minute / working_tempo).toInt()

        this._tempo_map[working_frame] = working_tempo
        for (tree in controller.events) {
            if (tree == null) {
                working_frame += frames_per_beat
                continue
            }

            var running_ratio = 0F
            var frame_offset = 0
            val stack = mutableListOf(Triple(tree, 1F, 0F))
            while (stack.isNotEmpty()) {
                val (working_tree, working_ratio, working_offset) = stack.removeFirst()

                if (working_tree.is_event()) {
                    working_tempo = (working_tree.get_event()!! as OpusTempoEvent).value

                    frame_offset += (((1f - running_ratio) * frames_per_beat) * (working_offset - running_ratio)).toInt()
                    running_ratio = working_offset
                    frames_per_beat = (frames_per_minute / working_tempo).toInt()

                    this._tempo_map[working_frame + frame_offset] = working_tempo
                } else if (!working_tree.is_leaf()) {
                    for ((i, child) in working_tree.divisions) {
                        val child_width = working_ratio / working_tree.size.toFloat()
                        stack.add(
                            Triple(
                                child,
                                child_width,
                                working_offset + (i * child_width)
                            )
                        )
                    }
                }
            }

            frame_offset += (((1f - running_ratio) * frames_per_beat) * (1f - running_ratio)).toInt()
            working_frame += frame_offset
        }
    }

    private fun calculate_overlaps() {
        val event_list = mutableListOf<Triple<Int, Int, Boolean>>()
        for ((handle_id,range) in this._setter_range_map) {
            event_list.add(Triple(range.first, handle_id, true))
            event_list.add(Triple(range.last, handle_id, false))
        }
        event_list.sortBy { it.first }

        val working_std_set = mutableSetOf<Int>()
        val working_perc_set = mutableSetOf<Int>()
        this._setter_overlaps.clear()

        // NOTE: Excluding percussion from overlap count, since they sort of exist in their own space
        // Percussion will still be attenuated to fit with the song, but a snare hit shouldn't make
        // Any notes played simultaneously play quieter
        val working_overlaps = HashMap<Int, Array<Int>>() // Keep track of overlaps here, use _setter_overlaps to track the maximums
        for ((_, setter_id, setter_on) in event_list) {
            val is_perc = this._percussion_setter_ids.contains(setter_id)
            if (setter_on) {
                if (is_perc) {
                    for (id in working_std_set) {
                        working_overlaps[id]!![1] += 1
                    }
                    for (id in working_perc_set) {
                        working_overlaps[id]!![1] += 1
                    }

                    working_perc_set.add(setter_id)
                } else {
                    for (id in working_std_set) {
                        working_overlaps[id]!![0] += 1
                    }
                    for (id in working_perc_set) {
                        working_overlaps[id]!![0] += 1
                    }
                    working_std_set.add(setter_id)
                }
                working_overlaps[setter_id] = arrayOf(working_std_set.size, working_perc_set.size)
                this._setter_overlaps[setter_id] = arrayOf(working_std_set.size, working_perc_set.size)
            } else if (is_perc) {
                working_perc_set.remove(setter_id)
                for (id in working_std_set) {
                    this._setter_overlaps[id]!![1] = max(working_overlaps[id]!![1], this._setter_overlaps[id]!![1])
                    working_overlaps[id]!![1] -= 1
                }
                for (id in working_perc_set) {
                    this._setter_overlaps[id]!![1] = max(working_overlaps[id]!![1], this._setter_overlaps[id]!![1])
                    working_overlaps[id]!![1] -= 1
                }
            } else {
                working_std_set.remove(setter_id)
                for (id in working_std_set) {
                    this._setter_overlaps[id]!![0] = max(working_overlaps[id]!![0], this._setter_overlaps[id]!![0])
                    working_overlaps[id]!![0] -= 1
                }
                for (id in working_perc_set) {
                    this._setter_overlaps[id]!![0] = max(working_overlaps[id]!![0], this._setter_overlaps[id]!![0])
                    working_overlaps[id]!![0] -= 1
                }
            }
        }
    }

    private fun map_tree(beat_key: BeatKey, position: List<Int>, working_tree: OpusTree<OpusEventSTD>, relative_width: Float, relative_offset: Float, prev_note_value: Int): Int {
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
            event.note += prev_note_value
            event.relative = false
        }

        val start_event = this._gen_midi_event(event, beat_key)!!

        val tempos: MutableList<Pair<Int, Float>> = this._tempo_map.toList().toMutableList()
        tempos.sortBy { it.first }

        val frames_per_minute = 60F * this._sample_handle_manager.sample_rate
        val beat_frame = this._cached_beat_frames[beat_key.beat]
        var start_frame = beat_frame!!.first

        var working_tempo = 0F
        while (tempos.isNotEmpty() && start_frame >= tempos.first().first) {
            working_tempo = tempos.removeFirst().second
        }

        var frames_per_beat = (frames_per_minute / working_tempo).toInt()
        var remaining_ratio = relative_offset

        while (remaining_ratio > 0f) {
            while (tempos.isNotEmpty() && tempos.first().first < start_frame) {
                working_tempo = tempos.removeFirst().second
                frames_per_beat = (frames_per_minute / working_tempo).toInt()
            }

            val add_frames = (frames_per_beat * remaining_ratio).toInt()
            if (tempos.isEmpty()) {
                start_frame += add_frames
                break
            } else {
                val next_tempo_frame = tempos.first().first
                if (start_frame + add_frames <= next_tempo_frame) {
                    start_frame += add_frames
                    break
                } else {
                    remaining_ratio -= ((start_frame + add_frames) - next_tempo_frame).toFloat() / frames_per_beat.toFloat()
                    start_frame += (start_frame + add_frames) - next_tempo_frame
                }
            }

        }

        var end_frame = start_frame
        remaining_ratio = (relative_width * event.duration)
        while (remaining_ratio > 0f) {
            while (tempos.isNotEmpty() && tempos.first().first < end_frame) {
                working_tempo = tempos.removeFirst().second
                frames_per_beat = (frames_per_minute / working_tempo).toInt()
            }

            val add_frames = (frames_per_beat * remaining_ratio).toInt()
            if (tempos.isEmpty()) {
                end_frame += add_frames
                break
            } else {
                val next_tempo_frame = tempos.first().first
                if (end_frame + add_frames <= next_tempo_frame) {
                    end_frame += add_frames
                    break
                } else {
                    remaining_ratio -= ((end_frame + add_frames) - next_tempo_frame).toFloat() / frames_per_beat.toFloat()
                    end_frame += (end_frame + add_frames) - next_tempo_frame
                }
            }
        }

        this._add_handles(start_frame, end_frame, start_event)

        return event.note
    }

    private fun _gen_midi_event(event: OpusEventSTD, beat_key: BeatKey): MIDIEvent? {
        if (this.opus_manager.is_percussion(beat_key.channel)) {
            return NoteOn(
                channel = 9,
                velocity = this.opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset),
                note = this.opus_manager.get_percussion_instrument(beat_key.line_offset) + 27
            )
        }
        // Assume event is *not* relative as it is modified in map_tree() before _gen_midi_event is called
        val value = event.note

        if (event.note < 0) {
            return null
        }

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
