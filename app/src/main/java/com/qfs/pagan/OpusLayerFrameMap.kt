package com.qfs.pagan

import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.MidiFrameMap
import com.qfs.apres.soundfontplayer.SampleHandle
import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.opusmanager.OpusLayerCursor
import kotlin.math.floor

open class OpusLayerFrameMap: OpusLayerCursor(), MidiFrameMap {
    var sample_handle_manager: SampleHandleManager? = null
    var quick_map_midi_events =  HashMap<Pair<BeatKey, List<Int>>, List<SampleHandle>>()
    var frame_map = HashMap<Int, MutableList<SampleHandle>>()
    private val note_index_map = mutableListOf<MutableSet<Pair<Int, Int>>>()


    //-----Layer Functions-------//
    fun set_sample_handle_manager(new_manager: SampleHandleManager) {
        this.sample_handle_manager = new_manager
        this.frame_map.clear()
        // TODO: Populate frame_map
    }

    fun unset_sample_handle_manager() {
        this.sample_handle_manager = null
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

        var value = this.get_absolute_value(beat_key, position) ?: return null

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
        var working_tree = this.get_tree(beat_key, position)
        if (!working_tree.is_leaf()) {
            for (i in 0 until working_tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.unmap_frames(beat_key, position)
            }
        } else if (!working_tree.is_event()) {
            return
        }

        val (start_event, end_event) = this.quick_map_midi_events.remove(Pair(beat_key, position)) ?: return
        val (start_frame, end_frame) = this.get_frame_range(beat_key, position)

        if (this.frame_map.containsKey(start_frame)) {
            this.frame_map[start_frame]!!.remove(start_event)
        }
        if (this.frame_map.containsKey(end_frame)) {
            this.frame_map[end_frame]!!.remove(end_event)
        }
        if (start_event is NoteOn79) {
            this.note_index_map[start_event.index].remove(Pair(start_frame, end_frame))
        }
    }

    fun map_frames(beat_key: BeatKey, position: List<Int>) {
        var working_tree = this.get_tree(beat_key, position)
        if (!working_tree.is_leaf()) {
            for (i in 0 until working_tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.map_frames(beat_key, position)
            }
        } else if (!working_tree.is_event()) {
            return
        }

        val (start_frame, end_frame) = this.get_frame_range(beat_key, position)
        val start_event = gen_midi_event(beat_key, position) ?: return
        val end_event = when (start_event) {
            is NoteOn -> {
                NoteOff(
                    channel = start_event.channel,
                    velocity = start_event.get_velocity(),
                    note = start_event.get_note()
                )
            }
            is NoteOn79 -> {
                val index = this.calc_note_index(start_frame, end_frame)
                start_event.index = index
                NoteOff79(
                    index = index,
                    channel = start_event.channel,
                    velocity = start_event.velocity,
                    note = start_event.note,
                    bend = start_event.bend,
                )
            }
            else -> return
        }

        this.quick_map_midi_events[Pair(beat_key, position)] = Pair(start_event, end_event)

        if (!this.frame_map.containsKey(start_frame)) {
            this.frame_map[start_frame] = mutableListOf()
        }
        this.frame_map[start_frame]!!.add(start_event)

        if (!this.frame_map.containsKey(end_frame)) {
            this.frame_map[end_frame] = mutableListOf()
        }
        this.frame_map[end_frame]!!.add(end_event)
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

    // Calculate midi2.0 note index
    fun calc_note_index(start: Int, end: Int): Int {
        var output = 0
        for (i in 0 until this.note_index_map.size) {
            var index_in_use = false
            for ((w_start, w_end) in this.note_index_map[i]) {
                if ((w_start in start..end) || (w_end in start..end)) {
                    index_in_use = true
                    break
                }
            }

            if (!index_in_use) {
                break
            }
            output += 1
        }
        return output
    }

    //-----End Layer Functions-------//

    override fun set_channel_program(channel: Int, program: Int) {
        this.sample_handle_manager!!.change_program(this.channels[channel].midi_channel, program)
        super.set_channel_program(channel, program)
    }

    override fun set_channel_bank(channel: Int, bank: Int) {
        this.sample_handle_manager!!.select_bank(this.channels[channel].midi_channel, bank)
        super.set_channel_bank(channel, bank)
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.unmap_frames(beat_key, position)
        super.set_event(beat_key, position, event)
        val midi_channel = this.channels[beat_key.channel].midi_channel
        val midi_event = this.gen_midi_event(beat_key, position) ?: return

        val preset = this.sample_handle_manager?.get_preset(midi_channel)
        val sample_handles = when (midi_event) {
            is NoteOn -> this.sample_handle_manager!!.gen_sample_handles(midi_event)
            is NoteOn79 -> this.sample_handle_manager!!.gen_sample_handles(midi_event)
            else -> return
        }
        // TODO. (incomplete)
    }

    override fun set_beat_count(new_count: Int) {
        super.set_beat_count(new_count)
    }

    override fun load(bytes: ByteArray, new_path: String?) {
        super.load(bytes, new_path)
    }

    override fun import_midi(path: String) {
        super.import_midi(path)
    }

    override fun get_events(frame: Int): List<MIDIEvent>? {
        return this.frame_map.getOrDefault(frame, null)
    }

    override fun get_beat_frames(): List<Int> {
        TODO("Not yet implemented")
    }
}