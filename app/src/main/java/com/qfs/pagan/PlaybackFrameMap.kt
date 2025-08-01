package com.qfs.pagan

import com.qfs.apres.event.GeneralMIDIEvent
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.ControllerEventData
import com.qfs.apres.soundfontplayer.FrameMap
import com.qfs.apres.soundfontplayer.ProfileBuffer
import com.qfs.apres.soundfontplayer.SampleHandle
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.InstrumentEvent
import com.qfs.pagan.structure.opusmanager.base.OpusChannelAbstract
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.OpusLineAbstract
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class PlaybackFrameMap(val opus_manager: OpusLayerBase, private val _sample_handle_manager: SampleHandleManager): FrameMap {
    private val FADE_LIMIT = this._sample_handle_manager.sample_rate / 12 // when clipping a release phase, limit the fade out so it doesn't click
    private val _handle_map = HashMap<Int, Pair<SampleHandle, IntArray>>() // Handle UUID::(Handle::Merge Keys)
    private val _handle_range_map = HashMap<Int, IntRange>() // Handle UUID::Frame Range
    private val _frame_map = HashMap<Int, MutableSet<Int>>() // Frame::Handle UUIDs

    private var _setter_id_gen = 0
    private var _setter_map = HashMap<Int,() -> Set<Pair<SampleHandle, IntArray>>>()
    private var _setter_frame_map = HashMap<Int, MutableSet<Int>>()
    private val _setter_range_map = HashMap<Int, IntRange>()
    private var _cached_frame_count: Int? = null
    private var _cached_beat_frames: Array<Int>? = null

    private val _tempo_ratio_map = mutableListOf<Pair<Float, Float>>()// rational position:: tempo
    private val _volume_map = HashMap<Pair<Int, Int>, ControllerEventData>()
    private val _pan_map = HashMap<Pair<Int, Int>, ControllerEventData>()
    private val _percussion_setter_ids = mutableSetOf<Int>()

    private val _effect_profiles = mutableListOf<Triple<Int, Int, ProfileBuffer>>()

    var clip_same_line_release = false

    override fun get_new_handles(frame: Int): Set<Pair<SampleHandle, IntArray>>? {
        // Check frame a buffer ahead to make sure frames are added as accurately as possible
        this.check_frame(frame + this._sample_handle_manager.buffer_size)

        if (!this._frame_map.containsKey(frame)) {
            return null
        }

        val output = mutableSetOf<Pair<SampleHandle, IntArray>>()

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

        val frames_per_minute = 60F * this._sample_handle_manager.sample_rate.toFloat()

        val beats = mutableListOf(0)

        var working_frame = 0
        val working_tempo = this._tempo_ratio_map[0].second
        var frames_per_beat = (frames_per_minute / working_tempo).toInt()
        var tempo_index = 0
        val frames_to_add = mutableListOf<Float>()
        // First, just get the frame of each beat
        for (i in 1 until this.opus_manager.length + 1) {
            val beat_position = i.toFloat() / this.opus_manager.length.toFloat()
            var working_position = (i - 1).toFloat() / this.opus_manager.length.toFloat()

            while (tempo_index < this._tempo_ratio_map.size) {
                val tempo_change_position = (this._tempo_ratio_map[tempo_index].first)

                if (tempo_change_position < beat_position) {
                    frames_to_add.add(frames_per_beat * (tempo_change_position - working_position))

                    working_position = tempo_change_position
                    frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()
                    tempo_index += 1
                } else {
                    break
                }
            }
            frames_to_add.add(frames_per_beat * (beat_position - working_position))
            working_frame += (frames_to_add.sum() * this.opus_manager.length).toInt()
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

    override fun get_effect_buffers(): List<Triple<Int, Int, ProfileBuffer>> {
        return this._effect_profiles
    }

    override fun get_active_handles(frame: Int): Set<Pair<Int, Pair<SampleHandle, IntArray>>> {
        val output = mutableSetOf<Pair<Int, Pair<SampleHandle, IntArray>>>()
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

            for (handle_pair in handle_getter()) {
                this._map_real_handle(handle_pair, range.first)
                output.add(
                    Pair(
                        this._handle_range_map[handle_pair.first.uuid]!!.first,
                        handle_pair
                    )
                )
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

    private fun _map_real_handle(handle: Pair<SampleHandle, IntArray>, start_frame: Int) {
        val end_frame = handle.first.release_frame!! + start_frame
        val sample_start_frame = start_frame
        val sample_end_frame = end_frame + handle.first.get_release_duration()
        val uuid = handle.first.uuid
        this._handle_range_map[uuid] = sample_start_frame .. sample_end_frame
        this._handle_map[uuid] = handle
        if (!this._frame_map.containsKey(sample_start_frame)) {
            this._frame_map[sample_start_frame] = mutableSetOf()
        }
        this._frame_map[sample_start_frame]!!.add(uuid)
    }

    fun clear() {
        for ((_, _, profile) in this._effect_profiles) {
            profile.destroy(true) // Destroy the buffer AND the data
        }
        this._effect_profiles.clear()
        this._frame_map.clear()

        for ((_, handle) in this._handle_map) {
            handle.first.destroy()
        }

        this._handle_map.clear()
        this._handle_range_map.clear()
        this._tempo_ratio_map.clear()
        this._volume_map.clear()
        this._pan_map.clear()
        this._cached_beat_frames = null

        this._setter_id_gen = 0
        this._setter_frame_map.clear()
        this._setter_map.clear()
        this._setter_range_map.clear()
        this._cached_frame_count = null
    }

    private fun _add_handles(start_frame: Int, end_frame: Int, start_event: GeneralMIDIEvent, next_event_frame: Int? = null, merge_keys: IntArray) {
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

            val output = mutableSetOf<Pair<SampleHandle, IntArray>>()
            val handle_uuid_set = mutableSetOf<Int>()
            for (handle in handles) {
                handle.release_frame = end_frame - start_frame

                if (next_event_frame != null) {
                    // Remove release phase. can get noisy on things like tubular bells with long fade outs
                    //handle.volume_envelope.frames_release = min(this._sample_handle_manager.sample_rate / 11, handle.volume_envelope.frames_release)
                    //handle.volume_envelope.frames_delay = 0
                    val volume_envelope = handle.volume_envelope;
                    if (volume_envelope.frames_release > this.FADE_LIMIT) {
                        volume_envelope.release = max(this.FADE_LIMIT, min(next_event_frame - end_frame, volume_envelope.frames_release)).toFloat() / this._sample_handle_manager.sample_rate.toFloat()
                    }
                }

                handle_uuid_set.add(handle.uuid)
                output.add(Pair(handle, merge_keys))
            }

            output
        }
    }

    fun parse_opus(ignore_global_controls: Boolean = false, ignore_channel_controls: Boolean = false, ignore_line_controls: Boolean = false) {
        this.clear()

        for (channel in this.opus_manager.get_all_channels()) {
            val instrument = channel.get_instrument()
            this._sample_handle_manager.select_bank(channel.get_midi_channel(), instrument.first)
            this._sample_handle_manager.change_program(channel.get_midi_channel(), instrument.second)
        }

        this.map_tempo_changes()
        this.get_marked_frames()
        this.setup_effect_buffers(ignore_global_controls, ignore_channel_controls, ignore_line_controls)

        this.opus_manager.channels.forEachIndexed { c: Int, channel: OpusChannelAbstract<out InstrumentEvent, out OpusLineAbstract<out InstrumentEvent>> ->
            if (channel.muted) {
                return@forEachIndexed
            }

            for (l in channel.lines.indices) {
                if (channel.get_line(l).muted)  {
                    continue
                }

                var prev_abs_note = 0
                for (b in 0 until this.opus_manager.length) {
                    val beat_key = BeatKey(c, l, b)
                    val working_tree = this.opus_manager.get_tree(beat_key)
                    prev_abs_note = this.map_tree(beat_key, listOf(), working_tree, 1F, 0F, prev_abs_note)
                }
            }
        }
    }

    private fun convert_controller_to_event_data(type_key: Int, controller: EffectController<*>): ControllerEventData {
        val controller_profile = controller.generate_profile()

        val control_event_data = HashMap<Int, Triple<Int, FloatArray, FloatArray>>()
        for (entry in controller_profile.values) {
            for ((frames, pair) in this.adjust_effect_profile_event_to_tempo(entry.third, entry.first.first, entry.first.second, entry.second.first, entry.second.second)) {
                control_event_data[frames.first] = Triple(frames.second, pair.first, pair.second)
            }
        }

        val keys = control_event_data.keys.sorted()
        val array = Array(control_event_data.size) { i: Int ->
            val (end_frame, value, increment) = control_event_data[keys[i]]!!
            Pair(
                Pair(keys[i], end_frame),
                Pair(value, increment)
            )
        }
        return ControllerEventData(array, type_key)
    }

    private fun get_control_type_key(control_type: EffectType): Int {
        return control_type.i
    }

    fun setup_effect_buffers(ignore_global_controls: Boolean = false, ignore_channel_controls: Boolean = false, ignore_line_controls: Boolean = false) {
        this.opus_manager.get_all_channels().forEachIndexed { c: Int, channel: OpusChannelAbstract<*, *> ->
            if (channel.muted) {
                return@forEachIndexed
            }
            channel.lines.forEachIndexed { l: Int, line: OpusLineAbstract<out InstrumentEvent> ->
                if (line.muted) {
                    return@forEachIndexed
                }
                if (!ignore_line_controls) {
                    for ((control_type, controller) in line.controllers.get_all()) {
                        val control_type_key = this.get_control_type_key(control_type)
                        this._effect_profiles.add(
                            Triple(
                                0, // layer ( line)
                                this.generate_merge_keys(c, l)[0], // key
                                ProfileBuffer(
                                    this.convert_controller_to_event_data(
                                        control_type_key,
                                        controller
                                    )
                                )
                            )
                        )
                    }
                }
            }
            if (!ignore_channel_controls) {
                for ((control_type, controller) in channel.controllers.get_all()) {
                    val control_type_key = this.get_control_type_key(control_type)
                    this._effect_profiles.add(
                        Triple(
                            1, // layer (channel)
                            this.generate_merge_keys(c, -1)[1], // key
                            ProfileBuffer(
                                this.convert_controller_to_event_data(control_type_key, controller)
                            )
                        )
                    )
                }
            }
        }

        if (!ignore_global_controls) {
            for ((control_type, controller) in this.opus_manager.controllers.get_all()) {
                val control_type_key = this.get_control_type_key(control_type)
                this._effect_profiles.add(
                    Triple(
                        2, // layer (global)
                        0,
                        ProfileBuffer(
                            this.convert_controller_to_event_data(control_type_key, controller)
                        )
                    )
                )
            }
        }
    }

    fun map_tempo_changes() {
        val controller = this.opus_manager.get_controller<OpusTempoEvent>(EffectType.Tempo)
        var working_tempo = controller.initial_event.value

        this._tempo_ratio_map.add(Pair(0f, working_tempo))

        controller.beats.forEachIndexed { i: Int, tree: ReducibleTree<OpusTempoEvent>? ->
            if (tree == null) {
                return@forEachIndexed
            }

            val stack = mutableListOf(Triple(tree, 1F, 0F))
            while (stack.isNotEmpty()) {
                val (working_tree, working_ratio, working_offset) = stack.removeAt(0)

                if (working_tree.has_event()) {
                    working_tempo = working_tree.get_event()!!.value
                    this._tempo_ratio_map.add(
                        Pair(
                            (i.toFloat() + working_offset) / this.opus_manager.length.toFloat(),
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

    // TODO: NEEDS BETTER NAME
    private fun adjust_effect_profile_event_to_tempo(transition: EffectTransition, relative_start: Float, relative_end: Float, start_values: FloatArray, end_values: FloatArray): List<Pair<Pair<Int, Int>, Pair<FloatArray, FloatArray>>> {
        val output = mutableListOf<Pair<Pair<Int, Int>, Pair<FloatArray, FloatArray>>>()

        val frames_per_minute = 60F * this._sample_handle_manager.sample_rate
        // Find the tempo active at the beginning of the beat
        var working_position = (relative_start * this.opus_manager.length).toInt().toFloat() / this.opus_manager.length
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
        val i = (relative_start * this.opus_manager.length)
        var start_frame = this._cached_beat_frames!![i.toInt()]

        while (tempo_index < this._tempo_ratio_map.size) {
            val tempo_change_position = this._tempo_ratio_map[tempo_index].first
            if (tempo_change_position < relative_start) {
                start_frame += (frames_per_beat * (tempo_change_position - working_position)).toInt() * this.opus_manager.length

                working_position = tempo_change_position
                frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()

                tempo_index += 1
            } else {
                break
            }
        }

        start_frame += (frames_per_beat * (relative_start - working_position)).toInt() * this.opus_manager.length

        if (transition == EffectTransition.Instant) {
            return listOf(
                Pair(
                    Pair(start_frame, start_frame),
                    Pair(end_values, FloatArray(end_values.size))
                )
            )
        }

        // Calculate End Position
        working_position = relative_start
        var end_frame = start_frame
        var working_values = start_values
        // Note: divide duration to keep in-line with 0-1 range

        while (tempo_index < this._tempo_ratio_map.size) {
            val tempo_change_position = this._tempo_ratio_map[tempo_index].first
            if (tempo_change_position < relative_end) {
                val next_end_frame = end_frame + (frames_per_beat * (tempo_change_position - working_position)).toInt() * this.opus_manager.length

                val (next_values, increments) = if (!start_values.contentEquals(end_values) && next_end_frame != end_frame) {
                    Pair(FloatArray(start_values.size), FloatArray(start_values.size)).also {
                        val p = (tempo_change_position - relative_start) / (relative_end - relative_start)
                        for (i in 0 until start_values.size) {
                            val n = (end_values[i] - start_values[i]) * p
                            it.first[i] = n + start_values[i]
                            it.second[i] = n / (next_end_frame - end_frame)
                        }
                    }
                } else {
                     Pair(working_values, FloatArray(working_values.size))
                }

                output.add(
                    Pair(
                        Pair(end_frame, next_end_frame - 1),
                        Pair(working_values, increments)
                    )
                )

                working_values = next_values
                end_frame = next_end_frame

                working_position = tempo_change_position
                frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()
                tempo_index += 1
            } else {
                break
            }
        }

        val next_end_frame = end_frame + (frames_per_beat * (relative_end - working_position)).toInt() * this.opus_manager.length
        val increment = if (!start_values.contentEquals(end_values) && next_end_frame != end_frame) {
            FloatArray(start_values.size) { i ->
                (end_values[i] - working_values[i]) / (next_end_frame - end_frame).toFloat()
            }
        } else {
            FloatArray(start_values.size)
        }

        output.add(
            Pair(
                Pair(end_frame, next_end_frame - 1),
                Pair(working_values, increment)
            )
        )

        return output
    }

    private fun calculate_event_frame_range(beat: Int, duration: Int, relative_width: Float, relative_offset: Float): Pair<Int, Int> {
        val frames_per_minute = 60F * this._sample_handle_manager.sample_rate
        // Find the tempo active at the beginning of the beat
        var working_position = beat.toFloat() / this.opus_manager.length.toFloat()
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
        var start_frame = this._cached_beat_frames!![beat]
        val target_start_position = (beat.toFloat() + relative_offset) / this.opus_manager.length.toFloat()
        while (tempo_index < this._tempo_ratio_map.size) {
            val tempo_change_position = this._tempo_ratio_map[tempo_index].first
            if (tempo_change_position < target_start_position) {
                start_frame += (frames_per_beat * (tempo_change_position - working_position)).toInt() * this.opus_manager.length

                working_position = tempo_change_position
                frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()

                tempo_index += 1
            } else {
                break
            }
        }

        start_frame += (frames_per_beat * (target_start_position - working_position)).toInt() * this.opus_manager.length

        // Calculate End Position
        working_position = target_start_position
        var end_frame = start_frame
        // Note: divide duration to keep in-line with 0-1 range
        val target_end_position = target_start_position + ((duration * relative_width) / this.opus_manager.length.toFloat())
        while (tempo_index < this._tempo_ratio_map.size) {
            val tempo_change_position = this._tempo_ratio_map[tempo_index].first
            if (tempo_change_position < target_end_position) {
                end_frame += (frames_per_beat * (tempo_change_position - working_position)).toInt() * this.opus_manager.length

                working_position = tempo_change_position
                frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()

                tempo_index += 1
            } else {
                break
            }
        }

        end_frame += (frames_per_beat * (target_end_position - working_position)).toInt() * this.opus_manager.length

        return Pair(start_frame, end_frame)
    }

    private fun map_tree(beat_key: BeatKey, position: List<Int>, working_tree: ReducibleTree<out InstrumentEvent>, relative_width: Float, relative_offset: Float, bkp_note_value: Int): Int {
        if (!working_tree.is_leaf()) {
            val new_width = relative_width / working_tree.size.toFloat()
            var new_working_value = bkp_note_value
            for (i in 0 until working_tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                new_working_value = this.map_tree(beat_key, new_position, working_tree[i], new_width, relative_offset + (new_width * i), new_working_value)
            }
            return new_working_value
        } else if (!working_tree.has_event()) {
            return bkp_note_value
        }

        val event = working_tree.get_event()!!.copy()
        val next_event_frame = if (this.clip_same_line_release) {
            val next_event_position = this.opus_manager.get_proceeding_event_position(beat_key, position)
            if (next_event_position != null) {
                val (next_beat, next_position) = next_event_position
                val (offset, _) = this.opus_manager.get_leaf_offset_and_width(
                    BeatKey(
                        beat_key.channel,
                        beat_key.line_offset,
                        next_beat
                    ), next_position
                )
                val float_offset = offset.toFloat()
                val beat = float_offset.toInt()
                val next_rel_offset = float_offset - beat.toFloat()
                val (next_start, _) = this.calculate_event_frame_range(
                    offset.toFloat().toInt(),
                    1,
                    1F,
                    next_rel_offset
                )
                next_start
            } else {
                null
            }
        } else {
            null
        }

        val (start_frame, end_frame) = this.calculate_event_frame_range(beat_key.beat, event.duration, relative_width, relative_offset)

        // Don't add negative notes since they can't be played, BUT keep track
        // of it so the rest of the song isn't messed up
        this._gen_midi_event(
            when (event) {
                is RelativeNoteEvent -> {
                    AbsoluteNoteEvent(
                        event.offset + bkp_note_value,
                        event.duration
                    )
                }
                else -> event
            },
            beat_key,
            position
        )?.let { start_event ->
            val merge_key_array = this.generate_merge_keys(beat_key.channel, beat_key.line_offset)
            this._add_handles(start_frame, end_frame, start_event, next_event_frame, merge_key_array)
        }

        return when (event) {
            is RelativeNoteEvent -> event.offset + bkp_note_value
            is AbsoluteNoteEvent -> event.note
            is PercussionEvent -> bkp_note_value
            else -> 0 // Should be unreachable
        }
    }

    private fun _gen_midi_event(event: InstrumentEvent, beat_key: BeatKey, position: List<Int>): GeneralMIDIEvent? {
        val velocity = min((this.opus_manager.get_current_velocity(beat_key, position) * 100F).toInt(), 127)
        // Assume event is *not* relative as it is modified in map_tree() before _gen_midi_event is called
        val (note, bend) = when (event) {
            is PercussionEvent -> {
                Pair(27 + this.opus_manager.get_percussion_instrument(beat_key.channel, beat_key.line_offset), 0)
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
                val transpose_offset = 12F * this.opus_manager.transpose.first.toFloat() / this.opus_manager.transpose.second.toFloat()
                val std_offset = (offset.first.toFloat() * 12F / offset.second.toFloat())

                Pair(
                    21 + (octave * 12) + std_offset.toInt() + transpose_offset.toInt(),
                    (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512F).toInt()
                )
            }
            else -> Pair(0, 0) // Should be unreachable
        }

        return NoteOn79(
            index = 0, // Set index as note is applied
            channel = this.opus_manager.get_channel(beat_key.channel).get_midi_channel(),
            velocity = velocity shl 8,
            note = note,
            bend = bend
        )
    }

    private fun generate_merge_keys(channel: Int, line_offset: Int): IntArray {
        return intArrayOf(
            line_offset + (channel * 1000),
            channel
        )
    }
}
