/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan

import com.qfs.apres.Midi
import com.qfs.apres.event.GeneralMIDIEvent
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.ControllerEventData
import com.qfs.apres.soundfontplayer.EffectType
import com.qfs.apres.soundfontplayer.FrameMap
import com.qfs.apres.soundfontplayer.ProfileBuffer
import com.qfs.apres.soundfontplayer.SampleHandle
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.get_next_biggest
import com.qfs.pagan.structure.max
import com.qfs.pagan.structure.minus
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.InstrumentEvent
import com.qfs.pagan.structure.opusmanager.base.OpusChannelAbstract
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.OpusLineAbstract
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.ControllerProfile
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.PitchController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.TempoController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.PitchEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.plus
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import com.qfs.pagan.structure.times
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType as PaganEffectType

class PlaybackFrameMap(val opus_manager: OpusLayerBase, private val _sample_handle_manager: SampleHandleManager): FrameMap {
    companion object {
        var merge_offset_gen = 1

        fun calculate_beat_frames(beat_count: Int, sample_rate: Int, tempo_map: List<Pair<Rational, Float>>): IntArray? {
            if (tempo_map.isEmpty()) return null

            val frames_per_minute = 60F * sample_rate.toFloat()
            val beats = mutableListOf(0)
            val working_tempo = tempo_map[0].second
            val frames_to_add = mutableListOf<Int>()

            var frames_per_beat = (frames_per_minute / working_tempo).toInt()
            var tempo_index = 0
            var working_frame = 0

            // First, just get the frame of each beat
            for (i in 1 until beat_count + 1) {
                val beat_position = i
                var working_position = Rational(i - 1,1)

                while (tempo_index < tempo_map.size) {
                    val tempo_change_position = tempo_map[tempo_index].first
                    if (tempo_change_position < beat_position) {
                        frames_to_add.add((frames_per_beat * (tempo_change_position - working_position)).toInt())

                        working_position = tempo_change_position
                        frames_per_beat = (frames_per_minute / tempo_map[tempo_index].second).toInt()
                        tempo_index += 1
                    } else {
                        break
                    }
                }

                frames_to_add.add((frames_per_beat * (beat_position - working_position)).toInt())
                working_frame += frames_to_add.sum()
                frames_to_add.clear()

                beats.add(working_frame)
            }

            return beats.toIntArray()
        }

        fun calculate_tempo_changes(tempo_controller: TempoController): List<Pair<Rational, Float>> {
            var working_tempo = tempo_controller.initial_event.value

            val output = mutableListOf(Pair(Rational(0,1), working_tempo))

            tempo_controller.beats.forEachIndexed { i: Int, tree: ReducibleTree<OpusTempoEvent>? ->
                if (tree == null) return@forEachIndexed

                val stack = mutableListOf(
                    Triple(tree, Rational(1,1), Rational(0,1))
                )

                while (stack.isNotEmpty()) {
                    val (working_tree, working_ratio, working_offset) = stack.removeAt(0)

                    if (working_tree.has_event()) {
                        working_tree.get_event()?.let { event ->
                            output.add(Pair(i + working_offset, event.value))
                            when (event.transition) {
                                EffectTransition.Instant -> {
                                    working_tempo = event.value
                                }
                                EffectTransition.InstantB -> {
                                    output.add(Pair(i + working_offset + (working_ratio * event.duration), working_tempo))
                                }
                                else -> {/* TODO: throw exception? */}
                            }
                        }
                    } else if (!working_tree.is_leaf()) {
                        for ((j, child) in working_tree.divisions) {
                            val child_width = working_ratio / working_tree.size
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
            return output
        }

        private fun generate_merge_keys(channel: Int, line_offset: Int? = null): IntArray {
            return intArrayOf(
                PlaybackFrameMap.merge_offset_gen++,
                if (line_offset != null) {
                    line_offset + (channel * 1000)
                } else {
                    -1
                }, // LAYER_LINE
                channel // LAYER_CHANNEL
            )
        }

        /**
         *  Get Frame (given the sample_rate) and tempo index within give tempo_map
         */
        private fun get_position_data(sample_rate: Int, tempo_map: List<Pair<Rational, Float>>, beat_map: IntArray, position: Rational): Pair<Int, Int> {
            val frames_per_minute = 60F * sample_rate.toFloat()
            var working_position = Rational(position.toInt(), 1)
            var working_tempo = 0f
            var tempo_index = 0

            for (i in tempo_map.size - 1 downTo 0) {
                val (absolute_offset, tempo) = tempo_map[i]
                if (absolute_offset <= working_position) {
                    working_tempo = tempo
                    tempo_index = i
                    break
                }
            }

            var frames_per_beat = (frames_per_minute / working_tempo).toInt()
            // Calculate Start Position
            var frame = beat_map[position.toInt()]
            while (tempo_index < tempo_map.size) {
                val tempo_change_position = tempo_map[tempo_index].first
                if (tempo_change_position < position) {
                    frame += (frames_per_beat * (tempo_change_position - working_position)).toInt()

                    working_position = tempo_change_position
                    frames_per_beat = (frames_per_minute / tempo_map[tempo_index++].second).toInt()
                } else {
                    break
                }
            }
            frame += (frames_per_beat * (position - working_position)).toInt()

            return Pair(frame, min(tempo_map.size - 1, tempo_index))
        }

        private fun convert_to_indexed_profile_buffer_frames(sample_rate: Int, tempo_map: List<Pair<Rational, Float>>, beat_map: IntArray, effect_event: ControllerProfile.ProfileEffectEvent, event_type: EffectType, start_offset: Rational = Rational(0,1)): List<ControllerEventData.IndexedProfileBufferFrame> {
            val adjusted_event = when (event_type) {
                EffectType.LowPass -> ControllerProfile.ProfileEffectEvent(
                        effect_event.start_position,
                        effect_event.end_position,
                        floatArrayOf(sample_rate.toFloat(), effect_event.start_value[0], effect_event.start_value[1]),
                        floatArrayOf(sample_rate.toFloat(), effect_event.end_value[0], effect_event.end_value[1]),
                        effect_event.transition
                    )
                else -> effect_event
            }
            val output = mutableListOf<ControllerEventData.IndexedProfileBufferFrame>()

            var (start_frame, tempo_index) = this.get_position_data(sample_rate, tempo_map, beat_map, adjusted_event.start_position)
            val (offset_frame, _) = if (start_offset.numerator == 0) {
                Pair(0, 0)
            } else {
                this.get_position_data(sample_rate, tempo_map, beat_map, start_offset)
            }

            val frames_per_minute = 60F * sample_rate.toFloat()
            var frames_per_beat = (frames_per_minute / tempo_map[tempo_index++].second).toInt()


            if (adjusted_event.transition == EffectTransition.Instant) {
                val adj_value = when (event_type) {
                    EffectType.Delay -> this.convert_delay_event_values(adjusted_event.end_value, frames_per_beat)
                    else -> adjusted_event.end_value
                }

                return listOf(
                    ControllerEventData.IndexedProfileBufferFrame(
                        first_frame = start_frame - offset_frame,
                        last_frame = start_frame - offset_frame,
                        value = adj_value,
                        increment = FloatArray(adj_value.size)
                    )
                )
            }


            // Calculate End Position
            var working_position = adjusted_event.start_position
            val data_width = adjusted_event.start_value.size
            var end_frame = start_frame
            var working_values = adjusted_event.start_value
            // Note: divide duration to keep in-line with 0-1 range

            while (tempo_index < tempo_map.size) {
                val tempo_change_position = tempo_map[tempo_index].first
                if (tempo_change_position < adjusted_event.end_position) {
                    val next_end_frame = end_frame + (frames_per_beat * (tempo_change_position - working_position)).toInt()

                    val (next_values, increments) = if (!adjusted_event.is_trivial() && next_end_frame != end_frame) {
                        Pair(FloatArray(data_width), FloatArray(data_width)).also {
                            val p = (tempo_change_position - adjusted_event.start_position) / (adjusted_event.end_position - adjusted_event.start_position)
                            for (i in 0 until data_width) {
                                val n = (adjusted_event.end_value[i] - adjusted_event.start_value[i]) * p.toFloat()
                                it.first[i] = n + adjusted_event.start_value[i]
                                it.second[i] = n / (next_end_frame - end_frame)
                            }
                        }
                    } else {
                        Pair(working_values, FloatArray(working_values.size))
                    }

                    output.add(
                        ControllerEventData.IndexedProfileBufferFrame(
                            first_frame = end_frame - offset_frame,
                            last_frame = next_end_frame - offset_frame,
                            value = working_values,
                            increment = increments
                        )
                    )

                    working_values = next_values
                    end_frame = next_end_frame

                    working_position = tempo_change_position
                    frames_per_beat = (frames_per_minute / tempo_map[tempo_index].second).toInt()
                    tempo_index += 1
                } else {
                    break
                }
            }

            val next_end_frame = end_frame + (frames_per_beat * (adjusted_event.end_position - working_position)).toInt()
            output.add(
                ControllerEventData.IndexedProfileBufferFrame(
                    first_frame = end_frame - offset_frame,
                    last_frame = next_end_frame - offset_frame,
                    value = working_values,
                    increment = if (!adjusted_event.is_trivial() && next_end_frame != end_frame) {
                        FloatArray(data_width) { i ->
                            (adjusted_event.end_value[i] - working_values[i]) / (next_end_frame - end_frame).toFloat()
                        }
                    } else {
                        FloatArray(data_width)
                    }
                )
            )


            return output
        }

        private fun convert_delay_event_values(values: FloatArray, frames_per_beat: Int): FloatArray {
            val output = FloatArray(values.size + 1) { i: Int ->
                if (i == values.size) {
                    frames_per_beat.toFloat()
                } else {
                    values[i]
                }
            }
            return output
        }
    }

    private val _fade_limit = this._sample_handle_manager.sample_rate / 12 // when clipping a release phase, limit the fade out so it doesn't click
    private val _handle_map = HashMap<Int, Pair<SampleHandle, IntArray>>() // Handle UUID::(Handle::Merge Keys)
    private val _handle_range_map = HashMap<Int, IntRange>() // Handle UUID::Frame Range
    private val _frame_map = HashMap<Int, MutableSet<Int>>() // Frame::Handle UUIDs

    private var _setter_id_gen = 0
    private var _setter_map = HashMap<Int,() -> Set<Pair<SampleHandle, IntArray>>>()
    private var _setter_frame_map = HashMap<Int, MutableSet<Int>>()
    private val _setter_range_map = HashMap<Int, IntRange>()
    private var _cached_frame_count: Int? = null
    private var _cached_beat_frames: IntArray? = null

    private var working_tempo_controller: ControllerProfile? = null
    private val _tempo_ratio_map = mutableListOf<Pair<Rational, Float>>()// rational position:: tempo
    private val _volume_map = HashMap<Pair<Int, Int>, ControllerEventData>()
    private val _pan_map = HashMap<Pair<Int, Int>, ControllerEventData>()
    private val _percussion_setter_ids = mutableSetOf<Int>()
    private val _pitch_controllers = mutableSetOf<ProfileBuffer>() // Stored in order to be destroyed in clear()

    private val _effect_profiles = mutableListOf<Triple<Int, Int, ProfileBuffer>>()
    private var frame_count = 0
    var is_looping: Boolean = false

    private val _ignored_events = HashMap<Pair<BeatKey, List<Int>>, Int>()

    override fun get_new_handles(frame: Int): Set<Pair<SampleHandle, IntArray>>? {
        // Check frame a buffer ahead to make sure frames are added as accurately as possible
        this.check_frame(frame + this._sample_handle_manager.buffer_size)

        if (!this._frame_map.containsKey(frame)) return null

        val output = mutableSetOf<Pair<SampleHandle, IntArray>>()

        if (this._frame_map.containsKey(frame)) {
            for (uuid in this._frame_map[frame]!!) {
                output.add(this._handle_map[uuid] ?: continue)
            }
        }

        return output
    }

    private fun _cache_beat_frames() {
        this._cached_beat_frames = PlaybackFrameMap.calculate_beat_frames(
            this.opus_manager.length,
            this._sample_handle_manager.sample_rate,
            this._tempo_ratio_map
        )
    }

    override fun get_marked_frame(i: Int): Int? {
        val marked_frames = this.get_marked_frames()

        return if (marked_frames.isEmpty()) {
            0
        } else if (this.is_looping) {
            marked_frames.let {
                it[i % (it.size - 1)] + (i / (it.size - 1) * this.frame_count)
            }
        } else {
            marked_frames.let { it[i % it.size] }
        }
    }

    fun get_marked_frames(): IntArray {
        this._cached_beat_frames?.let {
            if (it.isNotEmpty()) return it
        }

        this._cache_beat_frames()
        return this._cached_beat_frames ?: intArrayOf()
    }

    override fun has_frames_remaining(frame: Int): Boolean {
        return this.is_looping
                || (this._frame_map.isNotEmpty() && this._frame_map.keys.maxOf { it } >= frame)
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

            this._setter_frame_map.remove(range.first)
            this.replace_handle(handle_getter, setter_id, range.first + this.frame_count .. range.last + this.frame_count)
        }

        return output
    }

    fun replace_handle(handle_getter: () -> Set<Pair<SampleHandle, IntArray>>, map_id: Int, new_range: IntRange) {
        if (!this.is_looping) return

        this._setter_map[map_id] = handle_getter
        this._setter_range_map[map_id] = new_range

        if (!this._setter_frame_map.containsKey(new_range.first)) {
            this._setter_frame_map[new_range.first] = mutableSetOf()
        }

        this._setter_frame_map[new_range.first]!!.add(map_id)
    }

    // End FrameMap Interface --------------------------
    fun check_frame(frame: Int) {
        if (!this._setter_frame_map.containsKey(frame)) return

        val setter_ids = mutableListOf<Int>()
        for (setter_id in this._setter_frame_map.remove(frame)!!) {
            setter_ids.add(setter_id)
        }

        for (setter_id in setter_ids) {
            val handle_function = this._setter_map.remove(setter_id) ?: continue
            val previous_range = this._setter_range_map.remove(setter_id) ?: continue

            val handles = handle_function()
            for (handle in handles) {
                this._map_real_handle(handle, frame)
            }
            this.replace_handle(handle_function, setter_id, previous_range.first + this.frame_count .. previous_range.last + this.frame_count)
        }
    }

    private fun _map_real_handle(handle: Pair<SampleHandle, IntArray>, start_frame: Int) {
        val end_frame = handle.first.release_frame!! + start_frame
        val sample_end_frame = end_frame + handle.first.get_release_duration()

        val uuid = handle.first.uuid
        this._handle_range_map[uuid] = start_frame..sample_end_frame
        this._handle_map[uuid] = handle
        if (!this._frame_map.containsKey(start_frame)) {
            this._frame_map[start_frame] = mutableSetOf()
        }
        this._frame_map[start_frame]!!.add(uuid)
    }

    fun clear() {
        for ((_, _, profile) in this._effect_profiles) {
            profile.destroy(true) // Destroy the buffer AND the data
        }
        for (controller in this._pitch_controllers) {
            controller.destroy()
        }
        this._pitch_controllers.clear()

        this._effect_profiles.clear()
        this._frame_map.clear()
        this._ignored_events.clear()

        for ((_, handle) in this._handle_map) {
            handle.first.destroy()
        }

        this._handle_map.clear()
        this._handle_range_map.clear()
        this.working_tempo_controller = null
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

    private fun _add_handles(start_frame: Int, end_frame: Int, start_event: GeneralMIDIEvent, next_event_frame: Int? = null, merge_keys: IntArray, pitch_controller: ProfileBuffer? = null) {
        val setter_id = this._setter_id_gen++

        if (!this._setter_frame_map.containsKey(start_frame)) {
            this._setter_frame_map[start_frame] = mutableSetOf()
        }

        this._setter_frame_map[start_frame]!!.add(setter_id)
        this._setter_range_map[setter_id] = start_frame..end_frame

        this._cached_frame_count = null

        val is_percussion = when (start_event) {
            is NoteOn -> start_event.channel == Midi.PERCUSSION_CHANNEL
            is NoteOn79 -> start_event.channel == Midi.PERCUSSION_CHANNEL
            else -> return
        }

        if (is_percussion) {
            this._percussion_setter_ids.add(setter_id)
        }
        pitch_controller?.let {
            this._pitch_controllers.add(it)
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
                pitch_controller?.let {
                    handle.attach_pitch_controller(pitch_controller.copy())
                }
                if (next_event_frame != null) {
                    // Remove release phase. can get noisy on things like tubular bells with long fade outs
                    //handle.volume_envelope.frames_release = min(this._sample_handle_manager.sample_rate / 11, handle.volume_envelope.frames_release)
                    //handle.volume_envelope.frames_delay = 0
                    val volume_envelope = handle.volume_envelope
                    if (volume_envelope.frames_release > this._fade_limit) {
                        volume_envelope.release = max(this._fade_limit, min(next_event_frame - end_frame, volume_envelope.frames_release)).toFloat() / this._sample_handle_manager.sample_rate.toFloat()
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

        for (i in this.opus_manager.channels.indices) {
            val channel = this.opus_manager.get_channel(i)
            val instrument = channel.get_preset()
            val midi_channel =  this.opus_manager.get_midi_channel(i)
            this._sample_handle_manager.select_bank(midi_channel, instrument.first)
            this._sample_handle_manager.change_program(midi_channel, instrument.second)
        }

        this.map_tempo_changes(this.opus_manager.get_controller<OpusTempoEvent>(PaganEffectType.Tempo) as TempoController)
        this._cache_beat_frames()

        this.frame_count = this._cached_beat_frames!!.last()

        this.setup_effect_buffers(ignore_global_controls, ignore_channel_controls, ignore_line_controls)
        this.opus_manager.channels.forEachIndexed { c: Int, channel: OpusChannelAbstract<out InstrumentEvent, out OpusLineAbstract<out InstrumentEvent>> ->
            if (channel.muted) return@forEachIndexed

            for (l in channel.lines.indices) {
                if (channel.get_line(l).muted) continue

                var prev_abs_note = 0
                for (b in 0 until this.opus_manager.length) {
                    val beat_key = BeatKey(c, l, b)
                    val working_tree = this.opus_manager.get_tree(beat_key)
                    prev_abs_note = this.map_tree(beat_key, listOf(), working_tree, Rational(1, 1), Rational(0, 1), prev_abs_note)
                }
            }
        }
    }

    fun setup_effect_buffers(ignore_global_controls: Boolean = false, ignore_channel_controls: Boolean = false, ignore_line_controls: Boolean = false) {
        data class Quad(var layer: Int, var layer_key: Int, var profile: ControllerProfile, var type: EffectType)
        this._effect_profiles.clear()

        val temp_data = mutableListOf<Quad>()

        this.opus_manager.get_all_channels().forEachIndexed { c: Int, channel: OpusChannelAbstract<*, *> ->
            if (channel.muted) return@forEachIndexed
            channel.lines.forEachIndexed { l: Int, line: OpusLineAbstract<out InstrumentEvent> ->
                if (line.muted) return@forEachIndexed

                if (!ignore_line_controls) {
                    for ((control_type, controller) in line.controllers.get_all()) {
                        control_type.apres_type?.let { apres_type ->
                            temp_data.add(
                                Quad(
                                    FrameMap.LAYER_LINE, // layer (channel)
                                    PlaybackFrameMap.generate_merge_keys(c, l)[FrameMap.LAYER_LINE], // key
                                    controller.generate_profile(),
                                    apres_type
                                )
                            )
                        }
                    }
                }
            }

            if (!ignore_channel_controls) {
                for ((control_type, controller) in channel.controllers.get_all()) {
                    control_type.apres_type?.let { apres_type ->
                        temp_data.add(
                            Quad(
                                FrameMap.LAYER_CHANNEL, // layer (channel)
                                PlaybackFrameMap.generate_merge_keys(c)[FrameMap.LAYER_CHANNEL], // key
                                controller.generate_profile(),
                                apres_type
                            )
                        )
                    }
                }
            }
        }

        if (!ignore_global_controls) {
            for ((control_type, controller) in this.opus_manager.controllers.get_all()) {
                control_type.apres_type?.let { apres_type ->
                    temp_data.add(
                        Quad(
                            FrameMap.LAYER_GLOBAL, // layer (global)
                            -1,
                            controller.generate_profile(),
                            apres_type
                        )
                    )
                }
            }
        }


        for ((layer, layer_key, controller_profile, control_type) in temp_data) {
            this.add_effect_profile(layer, layer_key, controller_profile, control_type)
        }

        this._effect_profiles.sortBy { (_, _, buffer) -> buffer.type }
    }

    fun add_effect_profile(layer: Int, layer_key: Int, controller_profile: ControllerProfile, control_type: EffectType) {
        val sample_rate = this._sample_handle_manager.sample_rate
        val tempo_map = this._tempo_ratio_map
        val beat_map = this._cached_beat_frames!!

        val control_event_data = mutableListOf<ControllerEventData.IndexedProfileBufferFrame>()
        if (control_type == EffectType.Delay) {
            val merged_events: MutableList<Pair<EffectType?, ControllerProfile.ProfileEffectEvent>> = mutableListOf()

            for (event in controller_profile.get_events()) {
                merged_events.add(Pair(control_type, event))
            }

            for (event in this.working_tempo_controller!!.get_events()) {
                merged_events.add(Pair(null, event))
            }
            merged_events.sortBy { it.second.start_position }

            var working_event: ControllerProfile.ProfileEffectEvent? = null
            var index = 0
            while (index < merged_events.size) {
                val (current_type, current_event) = merged_events[index]
                if (current_type == control_type) {
                    working_event = current_event
                    index += 1
                } else if (working_event == null) {
                    merged_events.removeAt(index)
                } else {
                    merged_events.removeAt(index)
                    merged_events.add(
                        index,
                        Pair(
                            control_type,
                            ControllerProfile.ProfileEffectEvent(
                                start_position = current_event.start_position,
                                end_position = current_event.end_position,
                                start_value = working_event.start_value,
                                end_value = working_event.end_value,
                                transition = current_event.transition
                            )
                        )
                    )
                }
            }

            for ((_, effect_event) in merged_events) {
                control_event_data.addAll(PlaybackFrameMap.convert_to_indexed_profile_buffer_frames(sample_rate, tempo_map, beat_map, effect_event, control_type))
            }
        } else {
            for (effect_event in controller_profile.get_events()) {
                control_event_data.addAll(PlaybackFrameMap.convert_to_indexed_profile_buffer_frames(sample_rate, tempo_map, beat_map, effect_event, control_type))
            }
        }
        this._effect_profiles.add(
            Triple(
                layer,
                layer_key,
                ProfileBuffer(
                    ControllerEventData(
                        this.frame_count,
                        control_event_data,
                        control_type
                    )
                )
            )
        )
    }

    fun map_tempo_changes(tempo_controller: TempoController) {
        this.working_tempo_controller = tempo_controller.generate_profile()
        this._tempo_ratio_map.clear()
        this._tempo_ratio_map.addAll(
            PlaybackFrameMap.calculate_tempo_changes(tempo_controller)
        )
    }

    private fun calculate_event_frame_range(beat: Int, duration: Int, relative_width: Rational, relative_offset: Rational): Pair<Int, Int> {
        var (start_frame, tempo_index) = PlaybackFrameMap.get_position_data(
            this._sample_handle_manager.sample_rate,
            this._tempo_ratio_map,
            this._cached_beat_frames!!,
            beat + relative_offset
        )

        val frames_per_minute = 60F * this._sample_handle_manager.sample_rate.toFloat()
        var frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index].second).toInt()

        // Calculate End Position
        var working_position = beat + relative_offset
        var end_frame = start_frame
        val target_end_position = working_position + (duration * relative_width)
        while (tempo_index < this._tempo_ratio_map.size) {
            val (tempo_change_position, new_tempo) = this._tempo_ratio_map[tempo_index]
            if (tempo_change_position < target_end_position) {
                end_frame += (frames_per_beat * (tempo_change_position - working_position)).toInt()

                working_position = tempo_change_position
                frames_per_beat = (frames_per_minute / this._tempo_ratio_map[tempo_index++].second).toInt()
            } else {
                break
            }
        }


        end_frame += (frames_per_beat * (target_end_position - working_position)).toInt()
        return Pair(start_frame, end_frame)
    }

    private fun map_tree(beat_key: BeatKey, position: List<Int>, working_tree: ReducibleTree<out InstrumentEvent>, relative_width: Rational, relative_offset: Rational, bkp_note_value: Int): Int {
        //TODO: clean up 'working' v 'next' nomenclature
        if (this._ignored_events.containsKey(Pair(beat_key, position))) {
            return this._ignored_events[Pair(beat_key, position)]!!
        } else if (!working_tree.is_leaf()) {
            val new_width = relative_width / working_tree.size
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

        var initial_event = working_tree.get_event()!!.copy()
        var working_event = initial_event

        ////////////////////////////////////////////////
        var working_relative_width = relative_width
        var next_event_frame: Int? = null
        val next_beat_key = beat_key.copy()
        var next_position = position
        val (initial_offset, width_denominator) = this.opus_manager.get_leaf_offset_and_width(beat_key, position)
        var working_note_end = initial_offset + Rational(working_event.duration, width_denominator)
        var working_note_start = initial_offset
        var working_backup_value = bkp_note_value

        val pitch_controller = PitchController(this.opus_manager.length)
        var flag_pitch_shift: Boolean = false
        var working_note_size = Rational(working_event.duration, width_denominator)
        val event_frame_ranges = mutableListOf<Pair<Int, Int>>()
        var working_relative_offset = relative_offset
        var i = 0
        while (true) {
            event_frame_ranges.add(
                this.calculate_event_frame_range(
                    next_beat_key.beat,
                    working_event.duration,
                    working_relative_width,
                    working_relative_offset
                )
            )

            val next_event_position = this.opus_manager.get_proceeding_event_position(next_beat_key, next_position) ?: break
            val next_beat = next_event_position.first
            next_position = next_event_position.second
            next_beat_key.beat = next_beat

            working_event = this.opus_manager.get_tree(next_beat_key, next_position).get_event()!!

            val (offset, next_width_denominator) = this.opus_manager.get_leaf_offset_and_width(
                next_beat_key,
                next_position
            )
            val next_rel_offset = offset - offset.toInt()

            val controller  = this.opus_manager.get_line_controller<OpusVelocityEvent>(PaganEffectType.Velocity, next_beat_key.channel, next_beat_key.line_offset)
            val working_velocity_event = controller.coerce_event(offset.toInt(), next_rel_offset)

            val (next_start, _) = this.calculate_event_frame_range(
                offset.toFloat().toInt(),
                working_event.duration,
                Rational(1, next_width_denominator),
                next_rel_offset
            )

            next_event_frame = next_start

            val slide = working_velocity_event.slide ?: break
            val slide_rational = when (slide.first) {
                OpusVelocityEvent.SlideMaxWidth.Beat -> {
                    Rational(1, slide.second)
                }
                OpusVelocityEvent.SlideMaxWidth.Note -> {
                    Rational(working_note_size.numerator, working_note_size.denominator * slide.second)
                }
            }

            // overwrite the next note with the current note and add a pitch bend effect
            if (working_note_end == offset) {

                // Add pitch event
                val transition_offset = max(working_note_start, offset - slide_rational)
                val adj_transition_width = offset - transition_offset
                val transition_beat = transition_offset.toInt()
                val transition_relative_offset = transition_offset - transition_beat

                // Make room for the transition
                val pitch_tree = pitch_controller.get_tree(transition_beat)
                val original_size = pitch_tree.size

                val new_size = get_next_biggest(transition_relative_offset.denominator, original_size, adj_transition_width.denominator)
                pitch_tree.resize(new_size)

                // Adjust existing events
                for ((i, child) in pitch_tree.divisions) {
                    val child_event = child.get_event() ?: continue
                    child_event.duration = new_size * child_event.duration / original_size
                }

                val radix = this.opus_manager.get_radix()
                val from_note = this.opus_manager.get_absolute_value(beat_key, position)!!
                val from_octave = from_note / radix
                val from_offset = from_note % radix
                val (from_tuning_offset, from_tuning_radix) = this.opus_manager.tuning_map[from_offset]
                val from_pitch = 2F.pow((from_tuning_offset + (from_tuning_radix * from_octave)).toFloat() / from_tuning_radix.toFloat())

                val to_note = this.opus_manager.get_absolute_value(next_beat_key, next_position)!!
                val to_octave = to_note / radix
                val to_offset = to_note % radix
                val (to_tuning_offset, to_tuning_radix) = this.opus_manager.tuning_map[to_offset]
                val to_pitch = 2F.pow((to_tuning_offset + (to_tuning_radix * to_octave)).toFloat() / to_tuning_radix.toFloat())

                pitch_controller.set_event(
                    transition_beat,
                    listOf(transition_relative_offset.numerator * new_size / transition_relative_offset.denominator),
                    PitchEvent(
                        pitch = to_pitch / from_pitch,
                        duration = adj_transition_width.numerator * new_size / adj_transition_width.denominator,
                        transition = if (adj_transition_width.numerator == 0) {
                            EffectTransition.Instant
                        } else {
                            EffectTransition.Linear
                        } // Assume temporary, will be set to permanent(Linear) if more transitions follow
                    )
                )

                working_backup_value = when (working_event) {
                    is RelativeNoteEvent -> working_event.offset + working_backup_value
                    is AbsoluteNoteEvent -> working_event.note
                    is PercussionEvent -> bkp_note_value
                    else -> 0 // Should be unreachable
                }

                this._ignored_events[Pair(next_beat_key.copy(), next_position.toList())] = working_backup_value

                working_note_start = offset
                working_note_size = Rational(working_event.duration, next_width_denominator)
                working_note_end = offset + working_note_size
                flag_pitch_shift = true
                working_relative_offset = next_rel_offset
                working_relative_width = Rational(1, next_width_denominator)
            } else {
                break
            }
        }

        ////////////////////////////////////////////////
        val start_frame = event_frame_ranges.first().first
        val end_frame = event_frame_ranges.last().second

        // Don't add negative notes since they can't be played, BUT keep track
        // of it so the rest of the song isn't messed up
        this._gen_midi_event(
            when (initial_event) {
                is RelativeNoteEvent -> AbsoluteNoteEvent(initial_event.offset + bkp_note_value, initial_event.duration)
                else -> initial_event
            },
            beat_key,
            position
        )?.let { start_event ->
            val merge_key_array = PlaybackFrameMap.generate_merge_keys(beat_key.channel, beat_key.line_offset)
            val profile_buffer = if (flag_pitch_shift) {
                val profile = pitch_controller.generate_profile()
                val control_event_data = mutableListOf<ControllerEventData.IndexedProfileBufferFrame>()
                for (effect_event in profile.get_events()) {
                    control_event_data.addAll(
                        PlaybackFrameMap.convert_to_indexed_profile_buffer_frames(
                            this._sample_handle_manager.sample_rate,
                            this._tempo_ratio_map,
                            this._cached_beat_frames!!,
                            effect_event,
                            EffectType.Pitch,
                            initial_offset
                        )
                    )
                }
                ProfileBuffer(
                    ControllerEventData(
                        end_frame - start_frame,
                        control_event_data,
                        EffectType.Pitch
                    )
                )
            } else {
                null
            }

            this._add_handles(start_frame, end_frame, start_event, next_event_frame, merge_key_array, profile_buffer)
        }

        return when (initial_event) {
            is RelativeNoteEvent -> initial_event.offset + bkp_note_value
            is AbsoluteNoteEvent -> initial_event.note
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
                if (event.note < 0) return null

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
            channel = this.opus_manager.get_midi_channel(beat_key.channel),
            velocity = velocity shl 8,
            note = note,
            bend = bend
        )
    }

    fun shift_before_frame(frame: Int) {
        val setter_ids_to_remove = mutableSetOf<Int>()
        for ((setter_id, range) in this._setter_range_map) {
            if ((0 until frame).contains(range.first)) {
                setter_ids_to_remove.add(setter_id)
            }
        }

        for (setter_id in setter_ids_to_remove) {
            val range = this._setter_range_map[setter_id] ?: continue
            val handle_getter = this._setter_map.remove(setter_id) ?: continue
            this._setter_frame_map.remove(range.first)
            this.replace_handle(handle_getter, setter_id, range.first + this.frame_count .. range.last + this.frame_count)
        }
    }
}
