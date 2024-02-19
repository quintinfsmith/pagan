package com.qfs.pagan

import android.util.Log
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.LoadedJSONData
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.opusmanager.OpusLayerCursor
import com.qfs.pagan.structure.OpusTree
import kotlin.math.floor
import kotlin.math.max

open class OpusLayerFrameMap: OpusLayerCursor() {
    private var flux_indicator: Int = 0
    private var flag_cleared_in_flux: Boolean = false
    private var frame_map = PlaybackFrameMap()

    fun get_frame_map(): PlaybackFrameMap {
        return this.frame_map
    }

    //-----Layer Functions-------//
    fun set_sample_handle_manager(new_manager: SampleHandleManager) {
        this.frame_map.clear()

        this.frame_map.sample_handle_manager = new_manager
        this.setup_sample_handle_manager()
        this.setup_frame_map()
    }

    fun <T> clear_and_set_frames(callback: () -> T): T {
        if (this.frame_map.sample_handle_manager == null) {
            return callback()
        }

        return this.flux_wrapper {
            this.flag_cleared_in_flux = true

            if (this.flux_indicator == 1) {
                this.clear_frame_map_data()
            }

            val output = try {
                callback()
            } catch (e: Exception) {
                throw e
            }

            if (this.flux_indicator == 1) {
                this.setup_frame_map()
            }

            output
        }
    }

    fun <T> flux_wrapper(callback: () -> T): T {
        if (this.frame_map.sample_handle_manager == null) {
            return callback()
        }

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

        return output
    }

    fun setup_frame_map() {
        val start_ts = System.currentTimeMillis()
        Log.d("AAA", "SETTING FRAME MAP")
        if (this.frame_map.sample_handle_manager == null) {
            return
        }

        this.flux_wrapper {
            this.frame_map.tempo = this.tempo
            this.frame_map.beat_count = this.beat_count
            this.frame_map.unmap_flags.clear()
            this.channels.forEachIndexed { c: Int, channel: OpusChannel ->
                channel.lines.forEachIndexed { l: Int, line: OpusChannel.OpusLine ->
                    for (b in 0 until this.beat_count) {
                        this.map_frames(BeatKey(c, l, b), listOf())
                    }
                }
            }
        }
        Log.d("AAA", "sfm ${System.currentTimeMillis() - start_ts}")
    }

    fun setup_sample_handle_manager() {
        if (this.frame_map.sample_handle_manager == null) {
            return
        }

        this.flux_wrapper {
            for (channel in this.channels.indices) {
                val instrument = this.get_channel_instrument(channel)
                this.frame_map.sample_handle_manager!!.select_bank(this.channels[channel].midi_channel, instrument.first)
                this.frame_map.sample_handle_manager!!.change_program(this.channels[channel].midi_channel, instrument.second)
            }
        }
    }

    fun unset_sample_handle_manager() {
        this.frame_map.sample_handle_manager = null
        this.frame_map.clear()
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

        return ((offset + beat_key.beat.toDouble()) * (60.0 * this.frame_map.sample_handle_manager!!.sample_rate / this.tempo)).toInt()
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
        if (this.frame_map.sample_handle_manager == null) {
            return
        }

        if (this.flag_cleared_in_flux) {
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
        val unmap_key = mutableListOf(
            beat_key.channel,
            beat_key.line_offset,
            beat_key.beat
        )
        unmap_key.addAll(position)

        this.frame_map.remove_handle_by_quick_key(unmap_key)
    }

    private fun map_frames(beat_key: BeatKey, position: List<Int>) {
        if (this.frame_map.sample_handle_manager == null) {
            return
        }

        if (this.flag_cleared_in_flux) {
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

        val quick_key = mutableListOf(
            beat_key.channel,
            beat_key.line_offset,
            beat_key.beat
        )
        quick_key.addAll(position)

        if (this.frame_map.has_quick_key(quick_key)) {
            this.unmap_frames(beat_key, position)
        }

        val (start_frame, end_frame) = this.get_frame_range(beat_key, position)
        val start_event = gen_midi_event(beat_key, position)!!
        this.frame_map.add_handles(quick_key, start_frame, end_frame, start_event)

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
        val ratio = (60.0 * this.frame_map.sample_handle_manager!!.sample_rate.toDouble() / this.tempo)
        val initial = offset + beat_key.beat.toDouble()
        return Pair(
            (initial * ratio).toInt(),
            ((initial + (w * duration)) * ratio).toInt()
        )
    }

    fun unmap_line_frames(channel: Int, line_offset: Int) {
        if (this.frame_map.sample_handle_manager == null) {
            return
        }

        if (this.flag_cleared_in_flux) {
            return
        }

        this.channels[channel].lines[line_offset].beats.forEachIndexed { j: Int, tree: OpusTree<OpusEvent> ->
            this.unmap_frames(BeatKey(channel, line_offset, j), listOf())
        }
    }

    fun map_line_frames(channel: Int, line_offset: Int) {
        if (this.frame_map.sample_handle_manager == null) {
            return
        }

        if (this.flag_cleared_in_flux) {
            return
        }

        this.channels[channel].lines[line_offset].beats.forEachIndexed { j: Int, tree: OpusTree<OpusEvent> ->
            this.map_frames(BeatKey(channel, line_offset, j), listOf())
        }
    }

    fun unmap_channel_frames(channel: Int) {
        if (this.frame_map.sample_handle_manager == null) {
            return
        }

        if (this.flag_cleared_in_flux) {
            return
        }

        this.channels[channel].lines.forEachIndexed { i: Int, line: OpusChannel.OpusLine ->
            line.beats.forEachIndexed { j: Int, tree: OpusTree<OpusEvent> ->
                this.unmap_frames(BeatKey(channel, i, j), listOf())
            }
        }
    }

    fun map_channel_frames(channel: Int) {
        if (this.frame_map.sample_handle_manager == null) {
            return
        }

        if (this.flag_cleared_in_flux) {
            return
        }

        this.channels[channel].lines.forEachIndexed { i: Int, line: OpusChannel.OpusLine ->
            line.beats.forEachIndexed { j: Int, tree: OpusTree<OpusEvent> ->
                this.map_frames(BeatKey(channel, i, j), listOf())
            }
        }
    }

    fun <T> unmap_wrapper(beat_key: BeatKey, position: List<Int>, callback: () -> T): T {
        return if (this.frame_map.sample_handle_manager == null) {
            callback()
        } else {
            this.flux_wrapper {
                this.unmap_frames(beat_key, position)
                val output = callback()
                this.map_frames(beat_key, position)
                output
            }
        }
    }

    //-----End Layer Functions-------//
    override fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>) {
        if (this.frame_map.sample_handle_manager == null) {
            return super.set_channel_instrument(channel, instrument)
        }

        this.flux_wrapper {
            this.unmap_channel_frames(channel)
            super.set_channel_instrument(channel, instrument)
            this.frame_map.sample_handle_manager!!.select_bank(
                this.channels[channel].midi_channel,
                instrument.first
            )
            this.frame_map.sample_handle_manager!!.change_program(
                this.channels[channel].midi_channel,
                instrument.second
            )
            this.map_channel_frames(channel)
        }
    }

    override fun set_channel_program(channel: Int, program: Int) {
        if (this.frame_map.sample_handle_manager == null) {
            return super.set_channel_program(channel, program)
        }

        this.flux_wrapper {
            this.unmap_channel_frames(channel)
            this.frame_map.sample_handle_manager!!.change_program(this.channels[channel].midi_channel, program)
            super.set_channel_program(channel, program)
            this.map_channel_frames(channel)
        }
    }

    override fun set_channel_bank(channel: Int, bank: Int) {
        if (this.frame_map.sample_handle_manager != null) {
            this.frame_map.sample_handle_manager!!.select_bank(this.channels[channel].midi_channel, bank)
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
        if (this.frame_map.sample_handle_manager == null) {
            return super.set_percussion_instrument(line_offset, instrument)
        }

        this.flux_wrapper {
            this.unmap_line_frames(this.channels.size - 1, line_offset)
            super.set_percussion_instrument(line_offset, instrument)
            this.map_line_frames(this.channels.size - 1, line_offset)
        }
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<OpusEvent>) {
        if (this.frame_map.sample_handle_manager == null) {
            return super.replace_tree(beat_key, position, tree)
        }

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
        if (this.frame_map.sample_handle_manager == null) {
            return super.insert_beat(beat_index, beats_in_column)
        }

        this.flux_wrapper {
            super.insert_beat(beat_index, beats_in_column)

            if (this.flag_cleared_in_flux) {
                return@flux_wrapper
            }

            this.frame_map.insert_beat(beat_index)
        }
    }

    override fun remove_beat(beat_index: Int) {
        if (this.frame_map.sample_handle_manager == null) {
            return super.remove_beat(beat_index)
        }

        this.flux_wrapper {
            super.remove_beat(beat_index)

            if (this.flag_cleared_in_flux) {
                return@flux_wrapper
            }
        }
    }

    override fun remove_only(beat_key: BeatKey, position: List<Int>) {
        if (this.frame_map.sample_handle_manager == null) {
            return super.remove_only(beat_key, position)
        }

        this.flux_wrapper {
            this.unmap_frames(beat_key, position)
            super.remove_only(beat_key, position)
        }
    }

    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        if (this.frame_map.sample_handle_manager == null) {
            super.remove_standard(beat_key, position)
        }
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
        if (this.frame_map.sample_handle_manager == null) {
            return super.remove_one_of_two(beat_key, position)
        }

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
        if (this.frame_map.sample_handle_manager == null) {
            return super.unset(beat_key, position)
        }

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
        }
        Log.d("AAA", "samples generated: ${this.frame_map.sample_handle_manager!!.get_samples_generated()} | ${this.frame_map.sample_handle_manager!!.handles_got}")
    }

    override fun clear() {
        this.clear_frame_map_data()
        super.clear()
    }

    fun clear_frame_map_data() {
        this.frame_map.clear()
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
        if (this.frame_map.sample_handle_manager == null) {
            return super.set_duration(beat_key, position, duration)
        }

        this.unmap_wrapper(beat_key, position) {
            super.set_duration(beat_key, position, duration)
        }
    }

    override fun load_json(json_data: LoadedJSONData) {
        this.clear_and_set_frames {
            super.load_json(json_data)
        }
    }

}