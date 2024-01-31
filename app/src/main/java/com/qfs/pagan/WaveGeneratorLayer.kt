package com.qfs.pagan

import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.MidiFrameMap
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.CursorLayer
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.floor

open class WaveGeneratorLayer(var sample_rate: Int): CursorLayer() {
    class OpusManagerMidiFrameMap: MidiFrameMap() {
        private val _midi_events_by_frame = HashMap<Int, Array<Array<MutableList<MIDIEvent>?>>>()
        override fun get_events(frame: Int): List<MIDIEvent> {
        }
        fun clear() {
            this._midi_events_by_frame.clear()
        }
    }
    private val note_index_map = mutableListOf<MutableSet<Pair<Double, Double>>>()
    private var frame_end_map = HashMap<Pair<BeatKey, Int>, Int>() // (beatKey/first frame, end frame)

    override fun clear() {
        this.frame_end_map.clear()
        this.note_index_map.clear()
        this._midi_events_by_frame.clear()
        super.clear()
    }

    // Calculate midi2.0 note index
    fun calc_note_index(start: Double, end: Double): Int {
        var output = 0
        for (i in 0 until this.note_index_map.size) {
            var index_in_use = false
            for ((w_start, w_end) in this.note_index_map[i]) {
                if ((start <= w_start && w_start <= end) || (start <= w_end && w_end <= end)) {
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

    fun get_frame_range(beat_key: BeatKey, position: List<Int>, duration: Int = 1): Pair<Double, Double> {
        var working_tree = this.get_tree(beat_key)
        var offset = 1.0

        for (p in position) {
            if (working_tree.is_leaf()) {
                break
            }

            offset = offset * p / working_tree.size
            working_tree = working_tree[p]
        }

        return Pair(
            offset + beat_key.beat.toDouble(),
            (offset + beat_key.beat.toDouble()) + (offset * duration),
        )
    }

    fun calc_midi_events_from_opus_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent): Pair<MIDIEvent, MIDIEvent> {
        val prev_note = 0
        val current_note = if (event.relative) {
            event.note + prev_note
        } else {
            event.note
        }

        val radix = this.tuning_map.size
        val octave = current_note / radix
        val offset = this.tuning_map[current_note % radix]

        // This offset is calculated so the tuning map always reflects correctly
        val transpose_offset = 12.0 * this.transpose.toDouble() / radix.toDouble()
        val std_offset = (offset.first.toDouble() * 12.0 / offset.second.toDouble())

        //prev_note = current_note

        val note = (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21
        return if (this.is_tuning_standard()) {
            Pair(
                NoteOn(
                    channel = beat_key.channel,
                    note = note,
                    velocity = this.get_line_volume(beat_key.channel, beat_key.line_offset),
                ),
                NoteOff(
                    channel = beat_key.channel,
                    note = note,
                    velocity = this.get_line_volume(beat_key.channel, beat_key.line_offset),
                )
            )
        } else {
            val bend = (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512.0).toInt()
            Pair(
                NoteOn79(
                    index = 0,
                    channel = beat_key.channel,
                    note = note,
                    bend = bend,
                    velocity = this.get_line_volume(beat_key.channel, beat_key.line_offset) shl 8,
                ),
                NoteOff79(
                    index = 0,
                    channel = beat_key.channel,
                    note = note,
                    bend = bend,
                    velocity = this.get_line_volume(beat_key.channel, beat_key.line_offset) shl 8,
                )
            )
        }
    }

    fun unmap_events(beat_key: BeatKey, position: List<Int>) {
        val position_pair = this.get_frame_range(beat_key, position, 1)
        val ratio_beats_to_frames = (60.0 / this.tempo) * this.sample_rate
        val first_frame = (position_pair.first * ratio_beats_to_frames).toInt()
        val second_frame = (position_pair.second * ratio_beats_to_frames).toInt()

        val events_to_remove = mutableListOf<Pair<Int, Int>>()
        for (i in first_frame .. second_frame) {
            if (!this._midi_events_by_frame.containsKey(i)) {
                continue
            }
            this._midi_events_by_frame[i]!![beat_key.channel][beat_key.line_offset]?.forEachIndexed outer@{ j: Int, event: MIDIEvent ->
                if (event !is NoteOn && event !is NoteOn79) {
                    return@outer
                }

                events_to_remove.add(Pair(i, j))
                val f = this.frame_end_map.remove(Pair(beat_key, j)) ?: return@outer
                this._midi_events_by_frame[f]!![beat_key.channel][beat_key.line_offset]?.forEachIndexed inner@{ k: Int, event_b: MIDIEvent ->
                    if (event is NoteOn && event_b is NoteOff && event.get_note() == event_b.get_note()) {
                        events_to_remove.add(Pair(f, k))
                        return@outer
                    }

                    if (event is NoteOn79 && event_b is NoteOff79 && event.index == event_b.index) {
                        events_to_remove.add(Pair(f, k))
                        return@outer
                    }
                }
            }
        }

        events_to_remove.sortWith( compareByDescending { it.second })
        for ((i, j) in events_to_remove) {
            this._midi_events_by_frame[i]!![beat_key.channel][beat_key.line_offset]!!.removeAt(j)
        }
    }

    fun map_event(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (!tree.is_leaf()) {
            for (i in 0 until tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.map_event(beat_key, new_position)
            }
            return
        } else if (!tree.is_event()) {
            return
        }

        val event = tree.get_event()!!
        val midi_event_pair = this.calc_midi_events_from_opus_event(beat_key, position, event)
        val position_pair = this.get_frame_range(beat_key, position, event.duration)

        if (midi_event_pair.first is NoteOn79) {
            val note_index = this.calc_note_index(position_pair.first, position_pair.second)
            (midi_event_pair.first as NoteOn79).index = note_index
            (midi_event_pair.second as NoteOff79).index = note_index

            if (this.note_index_map.size == note_index) {
                this.note_index_map.add(mutableSetOf())
            }

            this.note_index_map[note_index].add(position_pair)
        }

        val ratio_beats_to_frames = (60.0 / this.tempo) * this.sample_rate
        val first_frame = (position_pair.first * ratio_beats_to_frames).toInt()
        if (!this._midi_events_by_frame.containsKey(first_frame)) {
            this._midi_events_by_frame[first_frame] = Array(this.channels.size) {
                Array(this.channels[it].lines.size) { null }
            }
        }

        var working_array = this._midi_events_by_frame[first_frame]!!
        if (working_array[beat_key.channel][beat_key.line_offset] == null) {
            working_array[beat_key.channel][beat_key.line_offset] = mutableListOf()
        }

        working_array[beat_key.channel][beat_key.line_offset]!!.add(midi_event_pair.first)

        val second_frame = (position_pair.second * ratio_beats_to_frames).toInt()
        if (!this._midi_events_by_frame.containsKey(second_frame)) {
            this._midi_events_by_frame[second_frame] = Array(this.channels.size) {
                Array(this.channels[it].lines.size) { null }
            }
        }

        working_array = this._midi_events_by_frame[second_frame]!!
        if (working_array[beat_key.channel][beat_key.line_offset] == null) {
            working_array[beat_key.channel][beat_key.line_offset] = mutableListOf()
        }

        working_array[beat_key.channel][beat_key.line_offset]!!.add(midi_event_pair.second)
        this.frame_end_map[Pair(beat_key, first_frame)] = second_frame
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.unmap_events(beat_key, position)
        super.set_event(beat_key, position, event)
        this.map_event(beat_key, position)
    }

    override fun new_line(channel: Int, line_offset: Int?): OpusChannel.OpusLine {
        val adj_line = line_offset ?: this.channels[channel].size

        val output = super.new_line(channel, line_offset)

        for ((frame, channel_array) in this._midi_events_by_frame) {
            this._midi_events_by_frame[frame] = Array(this.channels.size) { i ->
                Array(this.channels[i].size) { j: Int ->
                    if (adj_line > j) {
                        channel_array[i][j]
                    } else if (adj_line == j) {
                        null
                    } else {
                        channel_array[i][j - 1]
                    }
                }
            }
        }

        val new_frame_end_map = HashMap<Pair<BeatKey, Int>, Int>()
        for ((key, frame) in this.frame_end_map) {
            val new_beat_key = BeatKey(
                channel,
                if (adj_line > key.first.line_offset) {
                    key.first.line_offset
                } else {
                    key.first.line_offset + 1
                },
                key.first.beat
            )
            new_frame_end_map[Pair(new_beat_key, key.second)] = frame
        }

        this.frame_end_map = new_frame_end_map
        return output
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusChannel.OpusLine {
        val output = super.remove_line(channel, line_offset)

        for ((frame, channel_array) in this._midi_events_by_frame) {
            this._midi_events_by_frame[frame] = Array(this.channels.size) { i: Int ->
                Array(this.channels[i].size) { j: Int ->
                    if (j >= line_offset) {
                        channel_array[i][j + 1]
                    } else {
                        channel_array[i][j]
                    }
                }
            }
        }

        val new_frame_end_map = HashMap<Pair<BeatKey, Int>, Int>()
        for ((key, frame) in this.frame_end_map) {
            if (key.first.line_offset == line_offset) {
                continue
            }

            val new_beat_key = BeatKey(
                channel,
                if (key.first.line_offset >= line_offset) {
                    key.first.line_offset - 1
                } else {
                    key.first.line_offset
                },
                key.first.beat
            )
            new_frame_end_map[Pair(new_beat_key, key.second)] = frame
        }

        this.frame_end_map = new_frame_end_map

        return output
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<OpusEvent>) {
        if (position != null && position.isNotEmpty()) {
            this.unmap_events(beat_key, position)
        }

        super.replace_tree(beat_key, position, tree)

        if (position != null && position.isNotEmpty()) {
            this.map_event(beat_key, position)
        }
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this.unmap_events(beat_key, position)
        super.set_duration(beat_key, position, duration)
        this.map_event(beat_key, position)
    }


    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        val parent_position = position.subList(0, position.size - 1)
        this.unmap_events(beat_key, parent_position)
        super.remove_standard(beat_key, position)
        this.map_event(beat_key, parent_position)
    }

    override fun remove_only(beat_key: BeatKey, position: List<Int>) {
        this.unmap_events(beat_key, position)
        super.remove_only(beat_key, position)
    }

    override fun remove_one_of_two(beat_key: BeatKey, position: List<Int>) {
        val parent_position = position.subList(0, position.size - 1)
        this.unmap_events(beat_key, parent_position)
        super.remove_one_of_two(beat_key, position)
        this.map_event(beat_key, parent_position)
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        if (position.isNotEmpty()) {
            this.unmap_events(beat_key, position.subList(0, -1))
        }
        super.insert(beat_key, position)
        if (position.isNotEmpty()) {
            this.map_event(beat_key, position.subList(0, -1))
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.unmap_events(beat_key, position)
        super.split_tree(beat_key, position, splits)
        this.map_event(beat_key, position)
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        super.new_channel(channel, lines, uuid)
        val adj_channel = channel ?: this.channels.size - 1

        for ((frame, channel_array) in this._midi_events_by_frame) {
            this._midi_events_by_frame[frame] = Array(this.channels.size) { i ->
                if (adj_channel > i) {
                    channel_array[i]
                } else if (adj_channel == i) {
                    Array(this.channels[i].size) { null }
                } else {
                    channel_array[i - 1]
                }
            }
        }

        val new_frame_end_map = HashMap<Pair<BeatKey, Int>, Int>()
        for ((key, frame) in this.frame_end_map) {
            val new_beat_key = BeatKey(
                if (adj_channel > key.first.line_offset) {
                    key.first.channel
                } else {
                    key.first.channel + 1
                },
                key.first.line_offset,
                key.first.beat
            )
            new_frame_end_map[Pair(new_beat_key, key.second)] = frame
        }

        this.frame_end_map = new_frame_end_map
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)

        for ((frame, channel_array) in this._midi_events_by_frame) {
            this._midi_events_by_frame[frame] = Array(this.channels.size) { i: Int ->
                if (i >= channel) {
                    channel_array[i + 1]
                } else {
                    channel_array[i]
                }
            }
        }

        val new_frame_end_map = HashMap<Pair<BeatKey, Int>, Int>()
        for ((key, frame) in this.frame_end_map) {
            if (key.first.channel == channel) {
                continue
            }
            val new_beat_key = BeatKey(
                if (key.first.channel > channel) {
                    channel - 1
                } else {
                    channel
                },
                key.first.line_offset,
                key.first.beat
            )
            new_frame_end_map[Pair(new_beat_key, key.second)] = frame
        }

        this.frame_end_map = new_frame_end_map
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        super.insert_beat(beat_index, beats_in_column)

        val ratio_beats_to_frames = (60.0 / this.tempo) * this.sample_rate
        val target_beat_frame = ratio_beats_to_frames * beat_index

        for (frame in this._midi_events_by_frame.keys.sortedWith( compareByDescending { it } )) {
            if (frame < target_beat_frame) {
                continue
            }

            val new_frame = frame + ratio_beats_to_frames.toInt()
            this._midi_events_by_frame[new_frame] = this._midi_events_by_frame.remove(frame)!!
        }

        for ((beat_key, frame) in this.frame_end_map.keys.sortedWith(compareByDescending { it.first.beat })) {
            if (beat_key.beat < beat_index) {
                continue
            }
            val new_start_frame = frame + ratio_beats_to_frames.toInt()
            val new_end_frame = this.frame_end_map.remove(Pair(beat_key, frame))!!
            val new_beat_key = BeatKey(
                beat_key.channel,
                beat_key.line_offset,
                beat_key.beat + 1
            )

            this.frame_end_map[Pair(new_beat_key, new_start_frame)] = new_end_frame
        }
    }

    override fun remove_beat(beat_index: Int) {
        super.remove_beat(beat_index)

        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            channel.lines.forEachIndexed { j: Int, line: OpusChannel.OpusLine ->
                this.unmap_events(
                    BeatKey(i, j, beat_index),
                    listOf()
                )
            }
        }


        val ratio_beats_to_frames = (60.0 / this.tempo) * this.sample_rate
        val target_beat_frame = ratio_beats_to_frames * (beat_index + 1)

        for (frame in this._midi_events_by_frame.keys.sortedWith( compareBy { it } )) {
            if (frame < target_beat_frame) {
                continue
            }

            val new_frame = frame - ratio_beats_to_frames.toInt()
            this._midi_events_by_frame[new_frame] = this._midi_events_by_frame.remove(frame)!!
        }

        for ((beat_key, frame) in this.frame_end_map.keys.sortedWith(compareBy { it.first.beat })) {
            if (beat_key.beat < beat_index) {
                continue
            }

            val new_start_frame = frame + ratio_beats_to_frames.toInt()
            val new_end_frame = this.frame_end_map.remove(Pair(beat_key, frame))!!
            val new_beat_key = BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat - 1)

            this.frame_end_map[Pair(new_beat_key, new_start_frame)] = new_end_frame
        }
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        super.swap_lines(channel_a, line_a, channel_b, line_b)

        for ((frame, channel_array) in this._midi_events_by_frame) {
            val tmp = channel_array[channel_a][line_a]
            channel_array[channel_a][line_a] = channel_array[channel_b][line_b]
            channel_array[channel_b][line_b] = tmp
        }

        val new_frame_end_map = HashMap<Pair<BeatKey, Int>, Int>()
        for ((key, frame) in this.frame_end_map) {
            val new_beat_key = BeatKey(
                if (key.first.channel == channel_a && key.first.line_offset == line_a) {
                    channel_b
                } else if (key.first.channel == channel_b && key.first.line_offset == line_b) {
                    channel_a
                } else {
                    key.first.channel
                },
                if (key.first.channel == channel_a && key.first.line_offset == line_a) {
                    line_b
                } else if (key.first.channel == channel_b && key.first.line_offset == line_b) {
                    line_a
                } else {
                    key.first.line_offset
                },
                key.first.beat
            )
            new_frame_end_map[Pair(new_beat_key, key.second)] = frame
        }
        this.frame_end_map = new_frame_end_map
    }
}
