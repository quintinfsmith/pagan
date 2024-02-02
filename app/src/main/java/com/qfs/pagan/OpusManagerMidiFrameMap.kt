package com.qfs.pagan

import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.MidiFrameMap
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusEvent
import kotlin.math.floor
import com.qfs.pagan.opusmanager.OpusLayerBase as OpusManager

class OpusManagerMidiFrameMap(opus_manager: OpusManager, sample_rate: Int = 22050): MidiFrameMap() {
    var _midi_events_by_frame = HashMap<Int, MutableList<MIDIEvent>>()
    private val note_index_map = mutableListOf<MutableSet<Pair<Double, Double>>>()
    var previous_note = 0
    val beat_frames : List<Int>

    init {
        val frames_per_beat = 60.0 * sample_rate / opus_manager.tempo
        opus_manager.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            for (j in 0 until channel.lines.size) {
                this.previous_note = 0
                for (k in 0 until opus_manager.beat_count) {
                    this.map_tree(opus_manager, sample_rate, BeatKey(i, j, k), listOf())
                }
            }
        }

        this.beat_frames = List<Int>(opus_manager.beat_count + 1) { i ->
            (frames_per_beat * (i + 1)).toInt()
        }
    }

    override fun get_beat_frames(): List<Int> {
        return this.beat_frames
    }

    override fun get_events(frame: Int): List<MIDIEvent>? {
        if (!this._midi_events_by_frame.containsKey(frame)) {
            return null
        }

        return (this._midi_events_by_frame[frame]!!).sortedBy {
            when (it) {
                is NoteOn -> { 2 }
                is NoteOff -> { 0 }
                else -> { 1 }
            }
        }
    }

    fun get_frame(opus_manager: OpusManager, beat_key: BeatKey, position: List<Int>): Double {
        var working_tree = opus_manager.get_tree(beat_key)
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

        return offset + beat_key.beat.toDouble()
    }

    fun get_range(opus_manager: OpusManager, beat_key: BeatKey, position: List<Int>, duration: Int = 1): Pair<Double, Double> {
        var working_tree = opus_manager.get_tree(beat_key)
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

        return Pair(
            offset + beat_key.beat.toDouble(),
            (offset + beat_key.beat.toDouble()) + (offset * duration),
        )
    }

    fun calc_midi_events_from_opus_event(opus_manager: OpusManager, beat_key: BeatKey, event: OpusEvent): Pair<MIDIEvent, MIDIEvent> {
        val midi_channel = opus_manager.channels[beat_key.channel].midi_channel
        if (opus_manager.is_percussion(beat_key.channel)) {
            val note = opus_manager.get_percussion_instrument(beat_key.line_offset) + 27
            val velocity = opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)
            return Pair(
                NoteOn(channel=midi_channel, note=note, velocity=velocity),
                NoteOff(channel=midi_channel, note=note, velocity=velocity)
            )
        }

        val current_note = if (event.relative) {
            event.note + this.previous_note
        } else {
            event.note
        }

        this.previous_note = current_note

        val tuning_map = opus_manager.tuning_map
        val radix = tuning_map.size
        val octave = current_note / radix
        val offset = tuning_map[current_note % radix]

        // This offset is calculated so the tuning map always reflects correctly
        val transpose_offset = 12.0 * opus_manager.transpose.toDouble() / radix.toDouble()
        val std_offset = (offset.first.toDouble() * 12.0 / offset.second.toDouble())

        val note = (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21
        val line_volume = opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)
        return if (opus_manager.is_tuning_standard()) {
            Pair(
                NoteOn(
                    channel = midi_channel,
                    note = note,
                    velocity = line_volume
                ),
                NoteOff(
                    channel = midi_channel,
                    note = note,
                    velocity = line_volume
                )
            )
        } else {
            val bend = (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512.0).toInt()
            Pair(
                NoteOn79(
                    index = 0,
                    channel = midi_channel,
                    note = note,
                    bend = bend,
                    velocity = line_volume shl 8
                ),
                NoteOff79(
                    index = 0,
                    channel = midi_channel,
                    note = note,
                    bend = bend,
                    velocity = line_volume shl 8
                )
            )
        }
    }

    fun map_tree(opus_manager: OpusManager, sample_rate: Int, beat_key: BeatKey, position: List<Int>) {
        val tree = opus_manager.get_tree(beat_key, position)
        if (!tree.is_leaf()) {
            for (i in 0 until tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.map_tree(opus_manager, sample_rate, beat_key, new_position)
            }
            return
        } else if (!tree.is_event()) {
            return
        }

        val event = tree.get_event()!!
        val midi_event_pair = this.calc_midi_events_from_opus_event(opus_manager, beat_key, event)
        val position_pair = this.get_range(opus_manager, beat_key, position, event.duration)

        if (midi_event_pair.first is NoteOn79) {
            val note_index = this.calc_note_index(position_pair.first, position_pair.second)
            (midi_event_pair.first as NoteOn79).index = note_index
            (midi_event_pair.second as NoteOff79).index = note_index

            if (this.note_index_map.size == note_index) {
                this.note_index_map.add(mutableSetOf())
            }

            this.note_index_map[note_index].add(position_pair)
        }

        val ratio_beats_to_frames = (60.0 / opus_manager.tempo) * sample_rate
        val first_frame = (position_pair.first * ratio_beats_to_frames).toInt()
        if (!this._midi_events_by_frame.containsKey(first_frame)) {
            this._midi_events_by_frame[first_frame] = mutableListOf<MIDIEvent>()
        }
        this._midi_events_by_frame[first_frame]?.add(midi_event_pair.first)

        val second_frame = (position_pair.second * ratio_beats_to_frames).toInt()
        if (!this._midi_events_by_frame.containsKey(second_frame)) {
            this._midi_events_by_frame[second_frame] = mutableListOf<MIDIEvent>()
        }
        this._midi_events_by_frame[second_frame]?.add(midi_event_pair.second)
    }

}

