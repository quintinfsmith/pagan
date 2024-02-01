package com.qfs.pagan

import com.qfs.apres.Midi
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfontplayer.MidiFrameMap
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.CursorLayer
import com.qfs.pagan.opusmanager.LoadedJSONData
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.floor

open class WaveGeneratorLayer(): CursorLayer() {
    class OpusManagerMidiFrameMap(var opus_manager: WaveGeneratorLayer, var sample_rate: Int = 22050): MidiFrameMap() {
        var _midi_events_by_frame = HashMap<Int, Array<Array<MutableList<MIDIEvent>?>>>()
        private var frame_end_map = HashMap<Pair<BeatKey, Int>, Int>() // (beatKey/first frame, end frame)
        private val note_index_map = mutableListOf<MutableSet<Pair<Double, Double>>>()

        fun set_sample_rate(new_rate: Int) {
            val new_midi_events_map = HashMap<Int, Array<Array<MutableList<MIDIEvent>?>>>()
            for ((old_frame, value) in this._midi_events_by_frame) {
                new_midi_events_map[old_frame * new_rate / this.sample_rate] = value
            }

            this._midi_events_by_frame = new_midi_events_map
            this.sample_rate = new_rate
        }

        override fun get_beat_frames(): List<Int> {
            val frames_per_beat = (60.0 / this.opus_manager.tempo) * this.sample_rate
            return List(this.opus_manager.beat_count) { i: Int ->
                (frames_per_beat * (i + 1)).toInt()
            }
        }

        override fun get_events(frame: Int): List<MIDIEvent>? {
            if (!this._midi_events_by_frame.containsKey(frame)) {
                return null
            }

            val output = mutableListOf<MIDIEvent>()

            this._midi_events_by_frame[frame]!!.forEachIndexed { i: Int, line_array: Array<MutableList<MIDIEvent>?> ->
                line_array.forEachIndexed inner@{ j: Int, event_list: MutableList<MIDIEvent>? ->
                    if (event_list == null) {
                        return@inner
                    }

                    output.addAll(event_list)
                }
            }

            return output.sortedBy {
                when (it) {
                    is NoteOn -> { 2 }
                    is NoteOff -> { 0 }
                    else -> { 1 }
                }
            }
        }

        fun remove_beat(beat_index: Int) {
            this.opus_manager.channels.forEachIndexed { i: Int, channel: OpusChannel ->
                channel.lines.forEachIndexed { j: Int, line: OpusChannel.OpusLine ->
                    this.unmap_events(
                        BeatKey(i, j, beat_index),
                        listOf()
                    )
                }
            }


            val ratio_beats_to_frames = (60.0 / this.opus_manager.tempo) * this.sample_rate
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

        fun insert_beat(beat_index: Int) {
            val ratio_beats_to_frames = (60.0 / this.opus_manager.tempo) * this.sample_rate
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

        fun new_channel(channel: Int) {
            for ((frame, channel_array) in this._midi_events_by_frame) {
                this._midi_events_by_frame[frame] = Array(this.opus_manager.channels.size) { i ->
                    if (channel > i) {
                        channel_array[i]
                    } else if (channel == i) {
                        Array(this.opus_manager.channels[i].size) { null }
                    } else {
                        channel_array[i - 1]
                    }
                }
            }

            val new_frame_end_map = HashMap<Pair<BeatKey, Int>, Int>()
            for ((key, frame) in this.frame_end_map) {
                val new_beat_key = BeatKey(
                    if (channel > key.first.line_offset) {
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

        fun remove_channel(channel: Int) {
            for ((frame, channel_array) in this._midi_events_by_frame) {
                this._midi_events_by_frame[frame] = Array(this.opus_manager.channels.size) { i: Int ->
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

        fun get_frame_range(beat_key: BeatKey, position: List<Int>, duration: Int = 1): Pair<Double, Double> {
            var working_tree = this.opus_manager.get_tree(beat_key)
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

        fun calc_midi_events_from_opus_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent): Pair<MIDIEvent, MIDIEvent> {
            val midi_channel = this.opus_manager.channels[beat_key.channel].midi_channel
            if (this.opus_manager.is_percussion(beat_key.channel)) {
                val note = this.opus_manager.get_percussion_instrument(beat_key.line_offset) + 27
                val velocity = this.opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)
                return Pair(
                    NoteOn(channel=midi_channel, note=note, velocity=velocity),
                    NoteOff(channel=midi_channel, note=note, velocity=velocity)
                )
            }

            val prev_note = 0 // TODO: GET RELATIVE NOTES
            val current_note = if (event.relative) {
                event.note + prev_note
            } else {
                event.note
            }

            val tuning_map = this.opus_manager.tuning_map
            val radix = tuning_map.size
            val octave = current_note / radix
            val offset = tuning_map[current_note % radix]

            // This offset is calculated so the tuning map always reflects correctly
            val transpose_offset = 12.0 * this.opus_manager.transpose.toDouble() / radix.toDouble()
            val std_offset = (offset.first.toDouble() * 12.0 / offset.second.toDouble())

            //prev_note = current_note

            val note = (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21
            val line_volume = this.opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)
            return if (this.opus_manager.is_tuning_standard()) {
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

        fun unmap_events(beat_key: BeatKey, position: List<Int>) {
            val position_pair = this.get_frame_range(beat_key, position, 1)
            val ratio_beats_to_frames = (60.0 / this.opus_manager.tempo) * this.sample_rate
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
            val tree = this.opus_manager.get_tree(beat_key, position)
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

            val ratio_beats_to_frames = (60.0 / this.opus_manager.tempo) * this.sample_rate
            val first_frame = (position_pair.first * ratio_beats_to_frames).toInt()
            if (!this._midi_events_by_frame.containsKey(first_frame)) {
                this._midi_events_by_frame[first_frame] = Array(this.opus_manager.channels.size) {
                    Array(this.opus_manager.channels[it].lines.size) { null }
                }
            }

            var working_array = this._midi_events_by_frame[first_frame]!!
            if (working_array[beat_key.channel][beat_key.line_offset] == null) {
                working_array[beat_key.channel][beat_key.line_offset] = mutableListOf()
            }

            working_array[beat_key.channel][beat_key.line_offset]!!.add(midi_event_pair.first)

            val second_frame = (position_pair.second * ratio_beats_to_frames).toInt()
            if (!this._midi_events_by_frame.containsKey(second_frame)) {
                this._midi_events_by_frame[second_frame] = Array(this.opus_manager.channels.size) {
                    Array(this.opus_manager.channels[it].lines.size) { null }
                }
            }

            working_array = this._midi_events_by_frame[second_frame]!!
            if (working_array[beat_key.channel][beat_key.line_offset] == null) {
                working_array[beat_key.channel][beat_key.line_offset] = mutableListOf()
            }

            working_array[beat_key.channel][beat_key.line_offset]!!.add(midi_event_pair.second)
            this.frame_end_map[Pair(beat_key, first_frame)] = second_frame
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

        fun new_line(channel: Int, line_offset: Int) {
            for ((frame, channel_array) in this._midi_events_by_frame) {
                this._midi_events_by_frame[frame] = Array(this.opus_manager.channels.size) { i ->
                    Array(this.opus_manager.channels[i].size) { j: Int ->
                        if (line_offset > j) {
                            channel_array[i][j]
                        } else if (line_offset == j) {
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
                    if (line_offset > key.first.line_offset) {
                        key.first.line_offset
                    } else {
                        key.first.line_offset + 1
                    },
                    key.first.beat
                )
                new_frame_end_map[Pair(new_beat_key, key.second)] = frame
            }

            this.frame_end_map = new_frame_end_map
        }

        fun clear() {
            this._midi_events_by_frame.clear()
            this.frame_end_map.clear()
            this.note_index_map.clear()
        }

        fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
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

        fun remove_line(channel: Int, line_offset: Int) {
            for ((frame, channel_array) in this._midi_events_by_frame) {
                this._midi_events_by_frame[frame] = Array(this.opus_manager.channels.size) { i: Int ->
                    Array(this.opus_manager.channels[i].size) { j: Int ->
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
        }

        fun unmap_line(channel: Int, line_offset: Int) {
            for (i in 0 until this.opus_manager.beat_count) {
                this.unmap_events(BeatKey(channel, line_offset, i), listOf())
            }
        }

        fun map_line(channel: Int, line_offset: Int) {
            for (i in 0 until this.opus_manager.beat_count) {
                this.map_event(BeatKey(channel, line_offset, i), listOf())
            }
        }
    }

    val frame_map: OpusManagerMidiFrameMap = OpusManagerMidiFrameMap(this)

    override fun clear() {
        this.frame_map.clear()
        super.clear()
    }

    override fun new() {
        super.new()
        this.populate_frame_map()
    }

    override fun load_json(json_data: LoadedJSONData) {
        super.load_json(json_data)
        this.populate_frame_map()
    }

    override fun import_midi(midi: Midi) {
        super.import_midi(midi)
        this.populate_frame_map()
    }

    fun populate_frame_map() {
        this.frame_map.clear()
        for (k in 0 until this.beat_count) {
            this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
                for (j in 0 until channel.lines.size) {
                    this.frame_map.map_event(BeatKey(i, j, k), listOf())
                }
            }
        }
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.frame_map.unmap_events(beat_key, position)
        super.set_event(beat_key, position, event)
        this.frame_map.map_event(beat_key, position)
    }

    override fun new_line(channel: Int, line_offset: Int?): OpusChannel.OpusLine {
        val adj_line = line_offset ?: this.channels[channel].size

        val output = super.new_line(channel, line_offset)

        this.frame_map.new_line(channel, adj_line)
        return output
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusChannel.OpusLine {
        val output = super.remove_line(channel, line_offset)
        this.remove_line(channel, line_offset)
        return output
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<OpusEvent>) {
        if (!position.isNullOrEmpty()) {
            this.frame_map.unmap_events(beat_key, position)
        }

        super.replace_tree(beat_key, position, tree)

        if (!position.isNullOrEmpty()) {
            this.frame_map.map_event(beat_key, position)
        }
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this.frame_map.unmap_events(beat_key, position)
        super.set_duration(beat_key, position, duration)
        this.frame_map.map_event(beat_key, position)
    }


    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        val parent_position = position.subList(0, position.size - 1)
        this.frame_map.unmap_events(beat_key, parent_position)
        super.remove_standard(beat_key, position)
        this.frame_map.map_event(beat_key, parent_position)
    }

    override fun remove_only(beat_key: BeatKey, position: List<Int>) {
        this.frame_map.unmap_events(beat_key, position)
        super.remove_only(beat_key, position)
    }

    override fun remove_one_of_two(beat_key: BeatKey, position: List<Int>) {
        val parent_position = position.subList(0, position.size - 1)
        this.frame_map.unmap_events(beat_key, parent_position)
        super.remove_one_of_two(beat_key, position)
        this.frame_map.map_event(beat_key, parent_position)
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        if (position.isNotEmpty()) {
            this.frame_map.unmap_events(beat_key, position.subList(0, -1))
        }
        super.insert(beat_key, position)
        if (position.isNotEmpty()) {
            this.frame_map.map_event(beat_key, position.subList(0, -1))
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.frame_map.unmap_events(beat_key, position)
        super.split_tree(beat_key, position, splits)
        this.frame_map.map_event(beat_key, position)
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        super.new_channel(channel, lines, uuid)
        this.frame_map.new_channel(channel ?: (this.channels.size - 1))
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
        this.frame_map.remove_channel(channel)
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        super.insert_beat(beat_index, beats_in_column)
        this.frame_map.insert_beat(beat_index)
    }

    override fun remove_beat(beat_index: Int) {
        // TODO: CAN CAUSE ERROR on final beat
        super.remove_beat(beat_index)
        this.frame_map.remove_beat(beat_index)
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        super.swap_lines(channel_a, line_a, channel_b, line_b)
        this.frame_map.swap_lines(channel_a, line_a, channel_b, line_b)
    }
    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this.frame_map.unmap_events(beat_key, position)
        super.set_percussion_event(beat_key, position)
        this.frame_map.map_event(beat_key, position)
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.frame_map.unmap_events(beat_key, position)
        super.unset(beat_key, position)
    }

    override fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        this.frame_map.unmap_line(this.channels.size - 1, line_offset)
        super.set_percussion_instrument(line_offset, instrument)
        this.frame_map.map_line(this.channels.size - 1, line_offset)
    }
}
