package com.qfs.pagan

import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.FrameMap
import com.qfs.apres.soundfontplayer.SampleHandle
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.InstrumentEvent
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.PercussionEvent
import com.qfs.pagan.opusmanager.RelativeNoteEvent
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
    private var _cached_beat_frames: Array<Int>? = null

    private val _tempo_ratio_map = mutableListOf<Pair<Float, Float>>()// rational position:: tempo
    private val _percussion_setter_ids = mutableSetOf<Int>()

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

    override fun get_marked_frames(): Array<Int> {
        if (!this._cached_beat_frames.isNullOrEmpty()) {
            return this._cached_beat_frames!!
        }

        val frames_per_minute = 60F * this._sample_handle_manager.sample_rate

        val beats = mutableListOf(0)

        var working_frame = 0
        val working_tempo = this._tempo_ratio_map[0].second
        var frames_per_beat = (frames_per_minute / working_tempo).toInt()
        var tempo_index = 0
        val frames_to_add = mutableListOf<Int>()
        // First, just get the frame of each beat
        for (i in 1 until this.opus_manager.beat_count + 1) {
            val beat_position = i.toFloat() / this.opus_manager.beat_count.toFloat()
            var working_position = (i - 1).toFloat() / this.opus_manager.beat_count.toFloat()

            while (tempo_index < this._tempo_ratio_map.size) {
                val tempo_change_position = (this._tempo_ratio_map[tempo_index].first)

                if (tempo_change_position < beat_position) {
                    frames_to_add.add((frames_per_beat * (tempo_change_position - working_position)).toInt())

                    working_position = tempo_change_position
                    frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()

                    tempo_index += 1
                } else {
                    break
                }
            }

            frames_to_add.add((frames_per_beat * (beat_position - working_position)).toInt())
            working_frame += frames_to_add.sum() * this.opus_manager.beat_count
            frames_to_add.clear()

            beats.add(working_frame)
        }

        this._cached_beat_frames = beats.toTypedArray()

        return this._cached_beat_frames!!
    }

    override fun has_frames_remaining(frame: Int): Boolean {
        return (this._frame_map.isNotEmpty() && this._frame_map.keys.maxOf { it } >= frame)
                || (this._setter_frame_map.isNotEmpty() && this._setter_frame_map.keys.maxOf { it } >= frame)
                || this.get_marked_frames().last() >= frame
    }

    override fun get_size(): Int {
        // subject to changing, really just an estimate
        return this._cached_beat_frames?.last() ?: 0
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
            }
        }

        for (setter_id in setter_ids_to_remove) {
            val range = this._setter_range_map[setter_id] ?: continue
            val handle_getter = this._setter_map.remove(setter_id) ?: continue

            for (handle in handle_getter()) {
                this._map_real_handle(handle, range.first)
                output.add(Pair(this._handle_range_map[handle.uuid]!!.first, handle))
            }

            for (i in range.first until frame) {
                this._setter_frame_map.remove(i)
            }
        }

        return output
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
        var sample_start_frame = start_frame
        var sample_end_frame = end_frame + handle.get_release_duration()

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
        this._tempo_ratio_map.clear()
        this._cached_beat_frames = null

        this._setter_id_gen = 0
        this._setter_frame_map.clear()
        this._setter_map.clear()
        this._setter_range_map.clear()
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


            val handle_uuid_set = mutableSetOf<Int>()
            for (handle in handles) {
                handle.set_release_frame(end_frame - start_frame)

                if (this._simple_mode) {
                    handle.volume_envelope.frames_release = 0
                    handle.volume_envelope.frames_delay = 0
                }

                handle_uuid_set.add(handle.uuid)
            }

            handles
        }
    }

    fun parse_opus(force_simple_mode: Boolean = false) {
        this.clear()
        this._simple_mode = force_simple_mode

        for (channel in this.opus_manager.get_all_channels()) {
            val instrument = channel.get_instrument()
            this._sample_handle_manager.select_bank(channel.get_midi_channel(), instrument.first)
            this._sample_handle_manager.change_program(channel.get_midi_channel(), instrument.second)
        }

        this.map_tempo_changes()
        this.get_marked_frames()

        this.opus_manager.channels.forEachIndexed { c: Int, channel: OpusChannel ->
            for (l in channel.lines.indices) {
                var prev_abs_note = 0
                for (b in 0 until this.opus_manager.beat_count) {
                    val beat_key = BeatKey(c,l,b)
                    val working_tree = this.opus_manager.get_tree(beat_key)
                    prev_abs_note = this.map_tree(beat_key, listOf(), working_tree, 1F, 0F, prev_abs_note)
                }
            }
        }

        val c = this.opus_manager.channels.size
        for (l in this.opus_manager.percussion_channel.lines.indices) {
            for (b in 0 until this.opus_manager.beat_count) {
                val beat_key = BeatKey(c,l,b)
                val working_tree = this.opus_manager.get_tree(beat_key)
                this.map_tree(beat_key, listOf(), working_tree, 1F, 0F, 0)
            }
        }
    }

    fun map_tempo_changes() {
        val controller = this.opus_manager.controllers.get_controller(ControlEventType.Tempo)
        var working_tempo = (controller.initial_event as OpusTempoEvent).value


        this._tempo_ratio_map.add(Pair(0f, working_tempo))

        controller.events.forEachIndexed { i: Int, tree: OpusTree<OpusControlEvent>? ->
            if (tree == null) {
                return@forEachIndexed
            }

            val stack = mutableListOf(Triple(tree, 1F, 0F))
            while (stack.isNotEmpty()) {
                val (working_tree, working_ratio, working_offset) = stack.removeFirst()

                if (working_tree.is_event()) {
                    working_tempo = (working_tree.get_event()!! as OpusTempoEvent).value
                    this._tempo_ratio_map.add(
                        Pair(
                            (i.toFloat() + working_offset) / this.opus_manager.beat_count.toFloat(),
                            working_tempo
                        )
                    )
                } else if (!working_tree.is_leaf()) {
                    for ((j, child) in working_tree.divisions) {
                        val child_width = working_ratio / working_tree.size.toFloat()
                        stack.add(
                            Triple(
                                child,
                                child_width,
                                working_offset + (j * child_width)
                            )
                        )
                    }
                }
            }
        }
    }


    private fun map_tree(beat_key: BeatKey, position: List<Int>, working_tree: OpusTree<out InstrumentEvent>, relative_width: Float, relative_offset: Float, bkp_note_value: Int): Int {
        if (!working_tree.is_leaf()) {
            val new_width = relative_width / working_tree.size.toFloat()
            var new_working_value = bkp_note_value
            for (i in 0 until working_tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                new_working_value = this.map_tree(beat_key, new_position, working_tree[i], new_width, relative_offset + (new_width * i), new_working_value)
            }
            return new_working_value
        } else if (!working_tree.is_event()) {
            return bkp_note_value
        }

        val event = working_tree.get_event()!!.copy()

        val frames_per_minute = 60F * this._sample_handle_manager.sample_rate
        // Find the tempo active at the beginning of the beat
        var working_position = beat_key.beat.toFloat() / this.opus_manager.beat_count.toFloat()
        var working_tempo = 0f
        var tempo_index = 0

        for (i in this._tempo_ratio_map.size - 1 downTo 0) {
            val (absolute_offset, tempo) = this._tempo_ratio_map[i]
            if (absolute_offset <= working_position) {
                working_tempo = tempo
                tempo_index = i
                break
            }
        }

        var frames_per_beat = (frames_per_minute / working_tempo).toInt()

        // Calculate Start Position
        var start_frame = this._cached_beat_frames!![beat_key.beat]
        val target_start_position = (beat_key.beat.toFloat() + relative_offset) / this.opus_manager.beat_count.toFloat()
        while (tempo_index < this._tempo_ratio_map.size) {
            val tempo_change_position = this._tempo_ratio_map[tempo_index].first
            if (tempo_change_position < target_start_position) {
                start_frame += (frames_per_beat * (tempo_change_position - working_position)).toInt() * this.opus_manager.beat_count

                working_position = tempo_change_position
                frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()

                tempo_index += 1
            } else {
                break
            }
        }
        start_frame += (frames_per_beat * (target_start_position - working_position)).toInt() * this.opus_manager.beat_count

        // Calculate End Position
        working_position = target_start_position
        var end_frame = start_frame
        // Note: divide duration to keep in-line with 0-1 range
        val target_end_position = target_start_position + ((event.duration * relative_width) / this.opus_manager.beat_count.toFloat())
        while (tempo_index < this._tempo_ratio_map.size) {
            val tempo_change_position = this._tempo_ratio_map[tempo_index].first
            if (tempo_change_position < target_end_position) {
                end_frame += (frames_per_beat * (tempo_change_position - working_position)).toInt() * this.opus_manager.beat_count

                working_position = tempo_change_position
                frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()


                tempo_index += 1
            } else {
                break
            }
        }

        end_frame += (frames_per_beat * (target_end_position - working_position)).toInt() * this.opus_manager.beat_count

        val start_event = this._gen_midi_event(
            when (event) {
                is RelativeNoteEvent -> {
                    AbsoluteNoteEvent(
                        event.offset + bkp_note_value,
                        event.duration
                    )
                }
                else -> event
            },
            beat_key
        )
        // Don't add negative notes since they can't be played, BUT keep track
        // of it so the rest of the song isn't messed up
        if (start_event != null) {
            this._add_handles(start_frame, end_frame, start_event)
        }

        return when (event) {
            is RelativeNoteEvent -> event.offset + bkp_note_value
            is AbsoluteNoteEvent -> event.note
            is PercussionEvent -> bkp_note_value
            else -> 0 // Should be unreachable
        }
    }

    private fun _gen_midi_event(event: InstrumentEvent, beat_key: BeatKey): MIDIEvent? {
        val velocity = this.opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)

        // Assume event is *not* relative as it is modified in map_tree() before _gen_midi_event is called
        val (note, bend) = when (event) {
            is PercussionEvent -> {
                Pair(27 + this.opus_manager.get_percussion_instrument(beat_key.line_offset), 0)
            }
            is AbsoluteNoteEvent -> {
                // Can happen since we convert RelativeNotes to Absolute ones before passing them to this function
                if (event.note < 0) {
                    return null
                }
                val radix = this.opus_manager.tuning_map.size
                val octave = event.note / radix
                val offset = this.opus_manager.tuning_map[event.note % radix]

                // This offset is calculated so the tuning map always reflects correctly
                val transpose_offset = 12F * this.opus_manager.transpose.toFloat() / radix.toFloat()
                val std_offset = (offset.first.toFloat() * 12F / offset.second.toFloat())

                Pair(
                    21 + (octave * 12) + std_offset.toInt() + transpose_offset.toInt(),
                    (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512F).toInt()
                )
            }
            else -> Pair(0, 0) // Should be unreachable
        }

        return if (this.opus_manager.is_tuning_standard()) {
            NoteOn(
                channel = this.opus_manager.get_channel(beat_key.channel).get_midi_channel(),
                velocity = velocity,
                note = note
            )
        } else {
            NoteOn79(
                index = 0, // Set index as note is applied
                channel = this.opus_manager.get_channel(beat_key.channel).get_midi_channel(),
                velocity = velocity shl 8,
                note = note,
                bend = bend
            )
        }
    }
}
