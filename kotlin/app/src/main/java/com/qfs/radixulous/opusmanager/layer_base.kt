package com.qfs.radixulous.opusmanager
import com.qfs.radixulous.apres.*
import com.qfs.radixulous.from_string
import com.qfs.radixulous.structure.OpusTree
import com.qfs.radixulous.to_string
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.lang.Integer.max
import kotlin.math.pow

open class OpusManagerBase {
    var RADIX: Int = 12
    var DEFAULT_PERCUSSION: Int = 0
    var channels: MutableList<OpusChannel> = mutableListOf()
    private var channel_uuid_generator: Int = 0xFF
    private var channel_uuid_map = HashMap<Int, OpusChannel>()
    var opus_beat_count: Int = 1
    var path: String? = null
    var percussion_channel: Int? = null
    var tempo: Float = 120F
    var transpose: Int = 0
    var project_name: String = "New Opus"

    //// RO Functions ////
    fun get_channel_count(): Int {
        return this.channels.size
    }

    fun get_total_line_count(): Int {
        var output = 0
        this.channels.forEach { channel: OpusChannel ->
            output += channel.size
        }
        return output
    }

    fun get_abs_offset(channel_index: Int, line_offset: Int): Int {
        var count = 0
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            channel.lines.forEachIndexed { j: Int, line: OpusChannel.OpusLine ->
                if (i == channel_index && j == line_offset) {
                    return count
                }
                count += 1
            }
        }
        throw IndexOutOfBoundsException()
    }

    fun get_beatkeys_in_range(top_left_key: BeatKey, bottom_right_key: BeatKey): List<BeatKey> {
        var output = mutableListOf<BeatKey>()
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            if (i < top_left_key.channel || i > bottom_right_key.channel) {
                return@forEachIndexed // Continues
            }

            var (start, end) = if (top_left_key.channel == bottom_right_key.channel) {
                Pair(top_left_key.line_offset, bottom_right_key.line_offset)
            } else {
                if (i == top_left_key.channel) {
                    Pair(top_left_key.line_offset, channel.size - 1)
                } else if (i == bottom_right_key.channel) {
                    Pair(0, bottom_right_key.line_offset)
                } else {
                    Pair(0, channel.size - 1)
                }
            }
            for (j in start .. end) {
                var line = channel.lines[j]
                for (k in top_left_key.beat .. bottom_right_key.beat) {
                    output.add(BeatKey(i,j,k))
                }
            }
        }
        return output
    }

    open fun get_abs_difference(beata: BeatKey, beatb: BeatKey): Pair<Int, Int> {
        val beata_y = this.get_abs_offset(beata.channel, beata.line_offset)
        val beatb_y = this.get_abs_offset(beatb.channel, beatb.line_offset)

        return Pair(beatb_y - beata_y, beatb.beat - beata.beat)
    }

    fun get_std_offset(absolute: Int): Pair<Int, Int> {
        var count = 0
        this.channels.forEachIndexed {i: Int, channel: OpusChannel ->
            channel.lines.forEachIndexed { j: Int, lines: OpusChannel.OpusLine ->
                if (count == absolute) {
                    return Pair(i, j)
                }
                count += 1
            }
        }
        throw IndexOutOfBoundsException()
    }

    fun get_channel_instrument(channel: Int): Int {
        return this.channels[channel].get_instrument()
    }

    open fun get_percussion_instrument(line_offset: Int): Int {
        if (this.percussion_channel == null) {
            return this.DEFAULT_PERCUSSION
        }

        val channel = this.channels[this.percussion_channel!!]
        return channel.get_mapped_line_offset(line_offset) ?: this.DEFAULT_PERCUSSION
    }

    fun get_beat_tree(beat_key: BeatKey): OpusTree<OpusEvent> {
        if (beat_key.channel >= this.channels.size) {
            throw Exception("Invalid BeatKey $beat_key")
        }

        // TODO: Check if i ever use a negative line_offset. and change it if i do
        val line_offset: Int = if (beat_key.line_offset < 0) {
            this.channels[beat_key.channel].size - beat_key.line_offset
        } else {
            beat_key.line_offset
        }

        if (line_offset > this.channels[beat_key.channel].size) {
            throw Exception("Invalid BeatKey $beat_key")
        }

        return this.channels[beat_key.channel].get_tree(line_offset, beat_key.beat)
    }

    fun get_tree(beat_key: BeatKey, position: List<Int>): OpusTree<OpusEvent> {
        return this.channels[beat_key.channel].get_tree(beat_key.line_offset, beat_key.beat, position)
    }

    fun get_proceding_leaf(beat_key: BeatKey, position: List<Int>): OpusTree<OpusEvent>? {
        val pair = this.get_proceding_leaf_position(beat_key, position) ?: return null
        return this.get_tree(pair.first, pair.second)
    }

    fun get_preceding_leaf(beat_key: BeatKey, position: List<Int>): OpusTree<OpusEvent>? {
        val pair = this.get_preceding_leaf_position(beat_key, position) ?: return null
        return this.get_tree(pair.first, pair.second)
    }

    fun get_preceding_leaf_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
        val working_position = position.toMutableList()
        val working_beat_key = BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat)

        // Move left/up
        while (true) {
            if (working_position.isNotEmpty()) {
                if (working_position.last() > 0) {
                    working_position[working_position.size - 1] -= 1
                    break
                } else {
                    working_position.removeLast()
                }
            } else if (working_beat_key.beat > 0) {
                working_beat_key.beat -= 1
                break
            } else {
                return null
            }
        }

        var working_tree = this.get_tree(working_beat_key, working_position)

        // Move right/down to leaf
        while (!working_tree.is_leaf()) {
            working_position.add(working_tree.size - 1)
            working_tree = working_tree[working_tree.size - 1]
        }

        return Pair(working_beat_key, working_position)
    }

    fun get_proceding_leaf_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
        var working_position = position.toMutableList()
        val working_beat_key = BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat)
        var working_tree = this.get_tree(working_beat_key, working_position)

        // Move right/up
        while (true) {
            if (working_tree.parent != null) {
                if (working_tree.parent!!.size - 1 > working_position.last()) {
                    working_position[working_position.size - 1] += 1
                    working_tree = this.get_tree(working_beat_key, working_position)
                    break
                } else {
                    working_position.removeLast()
                    working_tree = working_tree.parent!!
                }
            } else if (working_beat_key.beat < this.opus_beat_count - 1) {
                working_beat_key.beat += 1
                working_position = mutableListOf()
                working_tree = this.get_tree(working_beat_key, working_position)
                break
            } else {
                return null
            }
        }
        // Move left/down to leaf
        while (!working_tree.is_leaf()) {
            working_position.add(0)
            working_tree = working_tree[0]
        }
        return Pair(working_beat_key, working_position)
    }

    open fun get_absolute_value(beat_key: BeatKey, position: List<Int>): Int? {
        val tree = this.get_tree(beat_key, position)

        var abs_value: Int
        if (tree.is_event()) {
            val event = tree.get_event()!!
            if (!event.relative) {
                return event.note
            } else {
                abs_value = event.note
            }
        } else {
            return null
        }

        var working_beat_key = beat_key
        var working_position = position.toList()

        while (true) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: return abs_value
            working_beat_key = pair.first
            working_position = pair.second

            val working_tree = this.get_tree(working_beat_key, working_position)

            if (working_tree.is_event()) {
                val working_event = working_tree.get_event()!!
                abs_value += working_event.note
                if (!working_event.relative) {
                    break
                }
            }
        }
        return abs_value
    }

    fun get_channel_line_counts(): List<Int> {
        val output: MutableList<Int> = mutableListOf()
        for (i in 0 until this.channels.size) {
            output.add(this.channels[i].size)
        }
        return output
    }

    fun has_preceding_absolute_event(beat_key: BeatKey, position: List<Int>): Boolean {
        var working_beat_key = beat_key
        var working_position = position.toList()

        var output = false
        while (true) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: break
            working_beat_key = pair.first
            working_position = pair.second

            val working_tree = this.get_tree(working_beat_key, working_position)

            if (working_tree.is_event()) {
                val working_event = working_tree.get_event()!!
                if (!working_event.relative) {
                    output = true
                    break
                }
            }
        }

        return output
    }

    fun is_percussion(channel: Int): Boolean {
        return channel == this.percussion_channel
    }
    //// END RO Functions ////

    fun convert_event_to_relative(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (!tree.is_event()) {
            throw Exception("Can't Convert a non-event")
        }

        val event = tree.get_event()!!
        if (event.relative) {
            return
        }

        var working_beat_key: BeatKey = beat_key
        var working_position: List<Int> = position
        var preceding_value: Int? = null
        while (preceding_value == null) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: break
            preceding_value = this.get_absolute_value(pair.first, pair.second)
            working_beat_key = pair.first
            working_position = pair.second
        }

        if (preceding_value == null) {
            this.set_event(
                beat_key, position, OpusEvent(
                    event.note,
                    event.radix,
                    event.channel,
                    true
                )
            )
        } else {
            this.set_event(
                beat_key, position, OpusEvent(
                    event.note - preceding_value,
                    event.radix,
                    event.channel,
                    true
                )
            )
        }
    }

    fun convert_event_to_absolute(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (!tree.is_event()) {
            throw Exception("Can't Convert a non-event")
        }

        val event = tree.get_event()!!
        if (!event.relative) {
            return
        }

        // The implied first value can be 0
        val value = this.get_absolute_value(beat_key, position) ?: event.note
        if (value < 0 || value > 95) {
            throw Exception("Note out of bounds ($value)")
        }
        this.set_event(beat_key, position, OpusEvent(
            value,
            event.radix,
            event.channel,
            false
        ))
    }

    open fun insert_after(beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) {
            throw Exception("Invalid Position $position")
        }

        val tree = this.get_tree(beat_key, position)
        val parent = tree.get_parent() ?: throw Exception("Invalid Position $position")

        val index = position.last()
        parent.insert(index + 1, OpusTree())
    }

    open fun remove(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)

        // Can't remove beat
        if (tree.parent == null || position.isEmpty()) {
            return
        }

        val parent_tree = tree.parent!!

        when (parent_tree.size) {
            1 -> {
                val next_position = position.toMutableList()
                next_position.removeLast()
                if (next_position.isNotEmpty()) {
                    this.remove(beat_key, next_position)
                }
                tree.detach()
            }
            2 -> {
                tree.detach()
                val prev_position = position.toMutableList()
                prev_position.removeLast()
                val to_replace = parent_tree[0]
                this.replace_tree(beat_key, prev_position, to_replace)
            }
            else -> {
                tree.detach()
            }
        }
    }

    open fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        if (!this.is_percussion(beat_key.channel)) {
            throw Exception("Attempting to set non-percussion channel")
        }

        val tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            tree.unset_event()
        }

        val instrument = this.get_percussion_instrument(beat_key.line_offset)
        tree.set_event(OpusEvent(
            instrument,
            this.RADIX,
            9,
            false
        ))
    }

    open fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        if (this.percussion_channel == null) {
            return
        }

        val channel = this.channels[this.percussion_channel!!]
        channel.map_line(line_offset, instrument)
    }

    open fun set_channel_instrument(channel: Int, instrument: Int) {
        if (channel == this.percussion_channel) {
            this.unset_percussion_channel()
        }

        this.channels[channel].set_instrument(instrument)
    }

    open fun set_percussion_channel(channel: Int) {
        this.unset_percussion_channel()

        this.percussion_channel = channel
        this.channels[channel].midi_instrument = 0
        this.channels[channel].midi_channel = 9
        this.channels[channel].set_mapped()
    }

    open fun unset_percussion_channel() {
        if (this.percussion_channel != null) {
            this.channels[this.percussion_channel!!].midi_channel = this.get_next_available_midi_channel()
            this.channels[this.percussion_channel!!].midi_instrument = 1
            this.channels[this.percussion_channel!!].unmap()
            this.percussion_channel = null
        }
    }

    open fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        if (this.is_percussion(beat_key.channel)) {
            throw Exception("Attempting to set percussion channel")
        }
        val tree = this.get_tree(beat_key, position)
        tree.set_event(event)
    }

    open fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        var tree: OpusTree<OpusEvent> = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            val event = tree.get_event()!!

            tree.unset_event()
            tree.set_size(splits)

            if (splits > 1) {
                tree = tree[0]
            }

            tree.set_event(event)
        } else {
            tree.set_size(splits)
        }
    }

    open fun unset(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)

        tree.unset_event()
        tree.empty()

        if (tree.parent != null) {
            val index = tree.getIndex()
            tree.parent!!.divisions.remove(index)
        }
    }

    private fun get_next_available_midi_channel(): Int {
        // Find the next available MIDI channel, ignore '9' which needs to be manually set
        // NOTE: This *will* generate past MIDI 1's limit of 16 channels
        val used_channels: MutableSet<Int> = mutableSetOf(9)
        for (channel in this.channels) {
            used_channels.add(channel.midi_channel)
        }
        var new_channel = 0
        while (new_channel in used_channels) {
            new_channel += 1
        }

        return new_channel
    }

    private fun gen_channel_uuid(): Int {
        val output = this.channel_uuid_generator
        this.channel_uuid_generator += 1
        return output
    }

    open fun new_channel(channel: Int? = null, lines: Int = 1) {
        val new_channel = OpusChannel(this.gen_channel_uuid())
        new_channel.set_beat_count(this.opus_beat_count)
        new_channel.midi_channel = this.get_next_available_midi_channel()
        this.channel_uuid_map[new_channel.uuid] = new_channel
        for (i in 0 until lines) {
            new_channel.new_line(i)
        }

        if (channel != null) {
            this.channels.add(channel, new_channel)
        } else {
            this.channels.add(new_channel)
        }
    }

    open fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        // Don't move empty lines
        if (this.channels[channel_old].line_is_empty(line_old)) {
            return
        }

        val line = this.remove_line(channel_old, line_old)

        if (channel_old == channel_new) {
            if (line_old < line_new) {
                this.insert_line(channel_new, line_new - 1, line)
            } else {
                this.insert_line(channel_new, line_new, line)
            }
        } else {
            this.insert_line(channel_new, line_new, line)
            if (this.channels[channel_old].size == 0) {
                this.new_line(channel_old, 0)
            }
        }
    }

    fun insert_beat() {
        this.insert_beat(this.opus_beat_count, 1)
    }

    open fun insert_beat(beat_index: Int, count: Int) {
        for (i in 0 until count) {
            this.insert_beat(beat_index + i)
        }
    }

    open fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>? = null) {
        this.opus_beat_count += 1
        for (channel in this.channels) {
            channel.insert_beat(beat_index)
            channel.set_beat_count(this.opus_beat_count)
        }
        if (beats_in_column == null) {
            return
        }
        var y = 0
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            channel.lines.forEachIndexed { j: Int, line: OpusChannel.OpusLine ->
                line.beats[beat_index] = beats_in_column[y]
                y += 1
            }
        }
    }


    open fun insert_line(channel: Int, line_index: Int, line: MutableList<OpusTree<OpusEvent>>) {
        this.channels[channel].insert_line(line_index, line)
    }

    open fun new_line(channel: Int, line_offset: Int? = null): List<OpusTree<OpusEvent>> {
        return this.channels[channel].new_line(line_offset)
    }

    open fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        val new_tree = this.channels[new_beat.channel].get_line(new_beat.line_offset).beats[new_beat.beat].copy()

        this.replace_tree(old_beat, listOf(), new_tree)
    }

    open fun remove_beat(beat_index: Int) {
        for (channel in this.channels) {
            channel.remove_beat(beat_index)
        }
        this.set_beat_count(this.opus_beat_count - 1)
    }

    fun remove_channel_by_uuid(uuid: Int) {
        val channel = this.channel_uuid_map[uuid] ?: throw Exception("Channel UUID $uuid Not found")
        var channel_index: Int? = null
        for (i in 0 until this.channels.size) {
            if (this.channels[i] == channel) {
                channel_index = i
                break
            }
        }
        if (channel_index != null) {
            this.remove_channel(channel_index)
        }
    }

    open fun remove_channel(channel: Int) {
        val opus_channel = this.channels.removeAt(channel)
        this.channel_uuid_map.remove(opus_channel.uuid)

        if (this.percussion_channel != null && this.percussion_channel!! > channel) {
            this.percussion_channel = this.percussion_channel!! - 1
        }
    }

    open fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        val from_tree = this.get_tree(beatkey_from, position_from).copy()
        this.replace_tree(beatkey_to, position_to, from_tree)
        this.unset(beatkey_from, position_from)
    }

    open fun remove_line(channel: Int, line_offset: Int): MutableList<OpusTree<OpusEvent>> {
        return this.channels[channel].remove_line(line_offset)
    }

    fun copy_func(tree: OpusTree<OpusEvent>): OpusEvent? {
        return if (tree.event == null) {
            null
        } else {
            tree.event!!.copy()
        }
    }

    open fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        val tree_copy = tree.copy(this::copy_func)
        this.channels[beat_key.channel].replace_tree(beat_key.line_offset, beat_key.beat, position, tree_copy)
    }

    open fun replace_beat_tree(beat_key: BeatKey, tree: OpusTree<OpusEvent>) {
        this.replace_tree(beat_key, listOf(), tree)
    }

    open fun set_beat_count(new_count: Int) {
        this.opus_beat_count = new_count
        for (channel in this.channels) {
            channel.set_beat_count(new_count)
        }
    }

    open fun get_midi(start_beat: Int = 0, end_beat_rel: Int? = null): MIDI {
        val end_beat = if (end_beat_rel == null) {
            this.opus_beat_count
        } else if (end_beat_rel < 0) {
            this.opus_beat_count + end_beat_rel
        } else {
            end_beat_rel
        }
        val tempo = this.tempo

        val midi = MIDI()

        midi.insert_event(0,0, SetTempo.from_bpm(tempo))
        data class StackItem(var tree: OpusTree<OpusEvent>, var divisions: Int, var offset: Int, var size: Int)
        val position_pointer_ticks = mutableSetOf<Pair<Int, Int>>()
        this.channels.forEachIndexed { c, channel ->
            if (channel.midi_instrument != 9) {
                midi.insert_event(
                    0,
                    0,
                    ProgramChange(channel.midi_channel, channel.midi_instrument)
                )
            }

            for (l in 0 until channel.size) {
                val line = channel.get_line(l)
                var current_tick = 0
                var prev_note = 0
                line.beats.forEachIndexed { b, beat ->
                    if (b in start_beat until end_beat) {
                        position_pointer_ticks.add(Pair(b, current_tick))
                    }
                    val stack: MutableList<StackItem> = mutableListOf(StackItem(beat, 1, current_tick, midi.ppqn))
                    while (stack.isNotEmpty()) {
                        val current = stack.removeFirst()
                        if (current.tree.is_event()) {
                            val event = current.tree.get_event()!!
                            val note = if (this.is_percussion(c)) { // Ignore the event data and use percussion map
                                this.get_percussion_instrument(l) + 27
                            } else if (event.relative) {
                                event.note + prev_note
                            } else {
                                event.note + 21 + this.transpose
                            }

                            if (!(b < start_beat || b >= end_beat)) {
                                midi.insert_event(
                                    0,
                                    current.offset,
                                    NoteOn(channel.midi_channel, note, line.volume)
                                )
                                midi.insert_event(
                                    0,
                                    current.offset + current.size,
                                    NoteOff(channel.midi_channel, note, line.volume)
                                )
                            }
                            prev_note = note
                        } else if (!current.tree.is_leaf()) {
                            val working_subdiv_size = current.size / current.tree.size
                            for ((i, subtree) in current.tree.divisions) {
                                stack.add(
                                    StackItem(
                                        subtree,
                                        current.tree.size,
                                        current.offset + (working_subdiv_size * i),
                                        working_subdiv_size
                                    )
                                )
                            }
                        }
                    }

                    if (!(b < start_beat || b >= end_beat)) {
                        current_tick += midi.ppqn
                    }
                }
            }
        }
        for ((beat, tick) in position_pointer_ticks) {
            midi.insert_event(
                0,
                tick,
                SongPositionPointer(beat)
            )
        }
        return midi
    }

    open fun to_json(): LoadedJSONData {
        val channels: MutableList<ChannelJSONData> = mutableListOf()
        for (channel in this.channels) {
            val lines: MutableList<String> = mutableListOf()
            val line_volumes: MutableList<Int> = mutableListOf()
            for (i in 0 until channel.size) {
                val line = channel.get_line(i)
                val beatstrs: MutableList<String> = mutableListOf()
                for (beat in line.beats) {
                    beatstrs.add(to_string(beat))
                }
                val str_line =  beatstrs.joinToString("|")
                lines.add(str_line)
                line_volumes.add(line.volume)
            }

            channels.add(
                ChannelJSONData(
                    midi_channel = channel.midi_channel,
                    midi_instrument = channel.midi_instrument,
                    lines = lines,
                    line_volumes = line_volumes
                )
            )
        }
        return LoadedJSONData(
            name = this.project_name,
            tempo = this.tempo,
            radix = this.RADIX,
            channels = channels,
            transpose = this.transpose
        )
    }

    open fun save(path: String? = null) {
        if (path == null && this.path == null) {
            throw Exception("NoPathGiven")
        }

        if (path != null) {
            this.path = path
        }

        val file_obj = File(this.path!!)
        val json_string = Json.encodeToString(this.to_json())
        file_obj.writeText(json_string)
    }

    // Clear function is used for new projects
    open fun clear() {
        this.opus_beat_count = 0
        for (i in this.channels.size - 1 downTo 0) {
            this.remove_channel(i)
        }
        this.path = null
        this.percussion_channel = null
        this.project_name = "New Opus"
    }

    open fun load(path: String) {
        this.load_json_file(path)
    }

    open fun load(bytes: ByteArray) {
        val json_content = bytes.toString(Charsets.UTF_8)
        this.load_json(Json.decodeFromString(json_content))
    }

    open fun new() {
        this.clear()
        this.new_channel()
        for (i in 0 until 4) {
            this.insert_beat()
        }
        this.set_project_name(this.project_name)
    }


    open fun load_json_file(path: String) {
        val json_content = File(path).readText(Charsets.UTF_8)
        this.load_json(Json.decodeFromString(json_content))
        this.path = path
    }

    fun parse_line_data(json_data: LoadedJSONData): List<List<List<OpusTree<OpusEvent>>>> {
        var output = mutableListOf<MutableList<MutableList<OpusTree<OpusEvent>>>>()

        json_data.channels.forEachIndexed { i, channel_data ->
            var line_list = mutableListOf<MutableList<OpusTree<OpusEvent>>>()
            channel_data.lines.forEachIndexed { j, line_str ->
                var beat_list = mutableListOf<OpusTree<OpusEvent>>()
                line_str.split("|").forEachIndexed { b, beat_str ->
                    val beat_tree = from_string(beat_str, this.RADIX, channel_data.midi_channel)
                    beat_tree.clear_singles()
                    beat_list.add(beat_tree)
                }
                line_list.add(beat_list)
            }
            output.add(line_list)
        }
        return output
    }

    open fun load_json(json_data: LoadedJSONData) {
        // Parse line_data first, if there's an error, it can be caught and ignored so the
        // active project is unaffected
        val parsed = this.parse_line_data(json_data)
        this.clear()

        this.RADIX = json_data.radix
        this.tempo = json_data.tempo
        this.transpose = json_data.transpose
        this.set_project_name(json_data.name)

        var beat_count = 0
        json_data.channels.forEachIndexed { i: Int, channel_data ->
            this.new_channel(lines = channel_data.lines.size)
            channel_data.lines.forEach { line_str ->
                val beatstrs = line_str.split("|")
                beat_count = max(beat_count, beatstrs.size)
            }
            channel_data.line_volumes.forEachIndexed { j: Int, volume: Int ->
                this.channels[i].lines[j].volume = volume
            }
        }

        for (i in 0 until beat_count) {
            this.insert_beat()
        }

        json_data.channels.forEachIndexed { i, channel_data ->
            if (channel_data.midi_channel == 9) {
                this.percussion_channel = i
            }

            this.channels[i].midi_channel = channel_data.midi_channel
            this.channels[i].midi_instrument = channel_data.midi_instrument

            channel_data.lines.forEachIndexed { j, line_str ->
                val note_set: MutableSet<Int> = mutableSetOf()
                parsed[i][j].forEachIndexed { b: Int, beat_tree: OpusTree<OpusEvent> ->
                    this.replace_tree(BeatKey(i, j, b), listOf(), beat_tree)
                    if (i == this.percussion_channel) {
                        for ((_, event) in beat_tree.get_events_mapped()) {
                            note_set.add(event.note)
                        }
                    }
                }

                // TODO: Allow any channel to be line_mapped here. for now, staying with percussion only
                if (i == this.percussion_channel) {
                    this.set_percussion_instrument(j, note_set.first())
                }
            }
        }
    }

    open fun import_midi(path: String) {
        val midi = MIDI.from_path(path)
        this.import_midi(midi)
    }

    private fun tree_from_midi(midi: MIDI): Triple<OpusTree<Set<OpusEvent>>, Float, List<Pair<Int, Int>>> {
        var beat_size = midi.get_ppqn()
        var total_beat_offset = 0
        var last_ts_change = 0
        val beat_values: MutableList<OpusTree<Set<OpusEvent>>> = mutableListOf()
        var max_tick = 0
        val press_map = HashMap<Int, Pair<Int, Int>>()
        var tempo = 120F
        val instrument_map = mutableListOf<Pair<Int, Int>>()

        var denominator = 4F
        for (pair in midi.get_all_events()) {
            val tick = pair.first
            val event = pair.second

            max_tick = kotlin.math.max(tick, max_tick)
            val beat_index = ((tick - last_ts_change) / beat_size) + total_beat_offset
            val inner_beat_offset = (tick - last_ts_change) % beat_size
            if (event is NoteOn && event.get_velocity() > 0) {
                // Add trees to list of trees
                while (beat_values.size <= beat_index) {
                    val new_tree = OpusTree<Set<OpusEvent>>()
                    new_tree.set_size(beat_size)
                    beat_values.add(new_tree)
                }

                val tree = beat_values[beat_index]
                val eventset = if (tree[inner_beat_offset].is_event()) {
                    tree[inner_beat_offset].get_event()!!.toMutableSet()
                } else {
                    mutableSetOf()
                }

                eventset.add(
                    OpusEvent(
                        if (event.channel == 9) {
                            event.get_note() - 27
                        } else {
                            event.get_note() - 21
                        },
                        12,
                        event.channel,
                        false
                    )
                )

                tree[inner_beat_offset].set_event(eventset)
                press_map[event.note] = Pair(beat_index, inner_beat_offset)
            } else if (event is TimeSignature) {
                total_beat_offset += (tick - last_ts_change) / beat_size
                last_ts_change = tick
                denominator = 2F.pow(event.get_denominator())

                beat_size = (midi.get_ppqn().toFloat() * (4 / denominator)).toInt()
            } else if (event is SetTempo) {
                if (tick == 0) {
                    tempo = event.get_bpm() * (denominator / 4)
                }
            } else if (event is ProgramChange) {
                instrument_map.add(Pair(event.channel, event.program))
            }
        }

        total_beat_offset += (max_tick - last_ts_change) / beat_size

        val opus = OpusTree<Set<OpusEvent>>()
        opus.set_size(beat_values.size)

        var overflow_events = mutableSetOf<OpusEvent>()
        beat_values.forEachIndexed { i, beat_tree ->

            // Quantize the beat ////////////
            val quantized_tree = OpusTree<Set<OpusEvent>>()
            quantized_tree.set_size(beat_tree.size)

            if (overflow_events.isNotEmpty()) {
                quantized_tree[0].set_event(overflow_events.toSet())
                overflow_events = mutableSetOf()
            }

            // Can easily merge quantized positions since the beats are still flat
            val qmap = beat_tree.get_quantization_map(listOf(2,2,2,3,5,7))
            for ((new_position, old_positions) in qmap) {
                val new_event_set = mutableSetOf<OpusEvent>()
                for (old_position in old_positions) {
                    val next_tree = beat_tree[old_position]
                    for (e in next_tree.get_event()!!) {
                        new_event_set.add(e)
                    }
                }

                if (new_position == quantized_tree.size) {
                    if (i < beat_values.size - 1) {
                        for (e in new_event_set) {
                            overflow_events.add(e)
                        }
                    }
                } else {
                    if (quantized_tree[new_position].is_event()) {
                        for (e in quantized_tree[new_position].get_event()!!) {
                            new_event_set.add(e)
                        }
                    }

                    quantized_tree[new_position].set_event(new_event_set.toSet())
                }
            }
            /////////////////////////////////////


            quantized_tree.reduce()
            quantized_tree.clear_singles()
            opus.set(i, quantized_tree)
        }


        return Triple(opus, tempo, instrument_map)
    }

    open fun import_midi(midi: MIDI) {
        this.clear()

        this.RADIX = 12

        val (settree, tempo, instrument_map) = tree_from_midi(midi)
        this.tempo = tempo

        val mapped_events = settree.get_events_mapped()
        val midi_channel_map = HashMap<Int, Int>()
        val channel_sizes = mutableListOf<Int>()
        var percussion_channel: Int? = null

        val percussion_map = HashMap<Int, Int>()

        for ((_, event_set) in mapped_events) {
            val tmp_channel_counts = HashMap<Int, Int>()
            event_set.forEachIndexed { _: Int, event: OpusEvent ->
                if (!midi_channel_map.contains(event.channel)) {
                    midi_channel_map[event.channel] = midi_channel_map.size
                }
                val channel_index = midi_channel_map[event.channel]!!

                if (event.channel == 9) {
                    if (!percussion_map.contains(event.note)) {
                        percussion_map[event.note] = percussion_map.size
                    }
                    tmp_channel_counts[channel_index] = percussion_map.size
                } else {
                    if (!tmp_channel_counts.contains(channel_index)) {
                        tmp_channel_counts[channel_index] = 1
                    } else {
                        tmp_channel_counts[channel_index] = tmp_channel_counts[channel_index]!! + 1
                    }
                }
            }

            for ((channel, size) in tmp_channel_counts) {
                while (channel >= channel_sizes.size) {
                    channel_sizes.add(0)
                }
                channel_sizes[channel] = max(channel_sizes[channel], size)
            }
        }

        channel_sizes.forEach { line_count: Int ->
            this.new_channel(lines = line_count)
        }

        for (i in 0 until settree.size) {
            this.insert_beat()
        }

        val events_to_set = mutableSetOf<Triple<BeatKey, List<Int>, OpusEvent>>()
        for ((position, event_set) in mapped_events) {
            val tmp_channel_counts = HashMap<Int, Int>()
            val event_list = event_set.toMutableList()
            event_list.sortWith(compareBy { 127 - it.note })
            event_list.forEachIndexed { _: Int, event: OpusEvent ->
                val channel_index = midi_channel_map[event.channel]!!
                if (event.channel == 9) {
                    percussion_channel = midi_channel_map[9]
                }
                if (!tmp_channel_counts.contains(channel_index)) {
                    tmp_channel_counts[channel_index] = 0
                }

                val line_offset = if (event.channel == 9) {
                    percussion_map[event.note]!!
                } else {
                    tmp_channel_counts[channel_index]!!
                }
                tmp_channel_counts[channel_index] = tmp_channel_counts[channel_index]!! + 1

                val working_position = mutableListOf<Int>()
                var working_beatkey: BeatKey? = null

                position.forEachIndexed { i: Int, (x, size): Pair<Int, Int> ->
                    if (i == 0) {
                        working_beatkey = BeatKey(channel_index, line_offset, x)
                    } else {
                        if (this.get_tree(working_beatkey!!, working_position).size != size) {
                            this.split_tree(working_beatkey!!, working_position, size)
                        }
                        working_position.add(x)
                    }
                }

                if (working_beatkey != null) {
                    events_to_set.add(Triple(working_beatkey!!, working_position, event))
                }
            }
        }

        for ((beatkey, position, event) in events_to_set) {
            if (event.note in 0..127) {
                this.set_event(beatkey, position, event)
            }
        }

        if (percussion_channel != null) {
            this.set_percussion_channel(percussion_channel!!)
            for ((note, index) in percussion_map) {
                this.set_percussion_instrument(index, note)
            }
        }

        for ((midi_channel, instrument) in instrument_map) {
            // Midi may have contained programchange event for channel, but no music
            val opus_channel = midi_channel_map[midi_channel] ?: continue
            this.set_channel_instrument(opus_channel, instrument)
        }
    }

    open fun set_line_volume(channel: Int, line_offset: Int, volume: Int) {
        this.channels[channel].set_line_volume(line_offset, volume)
    }
    fun get_line_volume(channel: Int, line_offset: Int): Int {
        return this.channels[channel].get_line_volume(line_offset)
    }

    open fun set_project_name(new_name: String) {
        this.project_name = new_name
    }

    open fun set_transpose(new_transpose: Int) {
        this.transpose = new_transpose
    }

    open fun set_tempo(new_tempo: Float) {
        this.tempo = new_tempo
    }
}
