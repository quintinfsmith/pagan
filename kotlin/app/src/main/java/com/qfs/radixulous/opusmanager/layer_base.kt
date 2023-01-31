package com.qfs.radixulous.opusmanager
import com.qfs.radixulous.apres.*
import com.qfs.radixulous.structure.OpusTree
import com.qfs.radixulous.from_string
import com.qfs.radixulous.to_string
import java.io.File


open class OpusManagerBase {
    var RADIX: Int = 12
    var DEFAULT_PERCUSSION: Int = 0
    var channels: MutableList<OpusChannel> = mutableListOf()
    var opus_beat_count: Int = 1
    var path: String? = null
    var percussion_channel: Int? = null
    var tempo: Float = 120F

    open fun reset() {
        this.opus_beat_count = 1
        this.channels.clear()
        this.path = null
    }

    fun set_percussion_channel(channel: Int) {
        this.unset_percussion_channel()

        this.percussion_channel = channel
        this.channels[channel].midi_instrument = 0
        this.channels[channel].midi_channel = 9
        this.channels[channel].set_mapped()
    }

    fun unset_percussion_channel() {
        if (this.percussion_channel != null) {
            this.channels[this.percussion_channel!!].midi_channel = 0
            this.channels[this.percussion_channel!!].midi_instrument = 1
            this.channels[this.percussion_channel!!].unmap()
            this.percussion_channel = null
        }
    }

    fun is_percussion(channel: Int): Boolean {
        return channel == this.percussion_channel
    }

    fun get_channel_count(): Int {
        return this.channels.size
    }

    open fun insert_after(beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) {
            throw Exception("Invalid Position $position")
        }

        val tree = this.get_tree(beat_key, position)
        val parent = tree.get_parent() ?: throw Exception("Invalid Position $position")

        var index = position.last()
        parent.insert(index + 1, OpusTree<OpusEvent>())
    }

    open fun remove(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)

        // Can't remove beat
        if (tree.parent == null || position.isEmpty()) {
            return
        }
        var parent_tree = tree.parent!!

        when (parent_tree.size) {
            1 -> {
                var next_position = position.toMutableList()
                next_position.removeLast()
                if (next_position.isNotEmpty()) {
                    this.remove(beat_key, next_position)
                }
                tree.detach()
            }
            2 -> {
                tree.detach()
                var prev_position = position.toMutableList()
                prev_position.removeLast()
                var to_replace = parent_tree.get(0)
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

        var tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            tree.unset_event()
        }

        var instrument = this.get_percussion_instrument(beat_key.line_offset)
        tree.set_event(OpusEvent(
            instrument,
            this.RADIX,
            9,
            false
        ))
    }

    open fun get_percussion_instrument(line_offset: Int): Int {
        if (this.percussion_channel == null) {
            return this.DEFAULT_PERCUSSION
        }

        var channel = this.channels[this.percussion_channel!!]
        return channel.get_mapped_line_offset(line_offset) ?: this.DEFAULT_PERCUSSION
    }

    open fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        if (this.percussion_channel == null) {
            return
        }

        var channel = this.channels[this.percussion_channel!!]
        channel.map_line(line_offset, instrument)
    }

    open fun set_channel_instrument(channel: Int, instrument: Int) {
        if (channel == this.percussion_channel) {
            this.unset_percussion_channel()
        }
        var channel = this.channels[channel]
        channel.set_instrument(instrument)
    }

    fun get_channel_instrument(channel: Int): Int {
        var channel = this.channels[channel]
        return channel.get_instrument()
    }

    open fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        if (this.is_percussion(beat_key.channel)) {
            throw Exception("Attempting to set percussion channel")
        }

        var tree = this.get_tree(beat_key, position)
        tree.set_event(event)
    }

    open fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        val tree: OpusTree<OpusEvent> = this.get_tree(beat_key, position)
        if (tree.is_leaf()) {
            var event = tree.get_event()
            tree.unset_event()
            tree.set_size(splits)
            if (event != null) {
                tree.get(0).set_event(event!!)
            }
        } else {
            tree.set_size(splits)
        }
    }

    open fun unset(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)

        tree.unset_event()
        tree.empty()

        if (tree.parent != null) {
            var index = tree.getIndex()
            tree.parent!!.divisions.remove(index)
        }
    }

    open fun new_channel() {
        var new_channel = OpusChannel()
        new_channel.set_beat_count(this.opus_beat_count)
        this.channels.add(new_channel)
    }

    open fun change_line_channel(old_channel: Int, line_index: Int, new_channel: Int) {
        var line = this.channels[old_channel].remove_line(line_index)
        this.channels[new_channel].insert_line(new_channel, line)
    }

    open fun insert_beat(index: Int?) {
        this.opus_beat_count += 1
        for (channel in this.channels) {
            channel.set_beat_count(this.opus_beat_count)
        }
    }

    open fun move_line(channel: Int, old_index: Int, new_index: Int) {
        this.channels[channel].move_line(old_index, new_index)
    }

    open fun new_line(channel: Int, index: Int? = null) {
        this.channels[channel].new_line(index)
    }

    open fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        var new_tree = this.channels[new_beat.channel].get_line(new_beat.line_offset)[new_beat.beat].copy()
        var old_line = this.channels[old_beat.channel].get_line(old_beat.line_offset)
        var old_tree = old_line[old_beat.beat]

        // replaces in parents
        old_tree.replace_with(new_tree)

        old_line[old_beat.beat] = new_tree
    }

    open fun remove_beat(rel_beat_index: Int?) {
        var beat_index = if (rel_beat_index == null) {
            this.opus_beat_count - 1
        } else if (rel_beat_index < 0) {
            this.opus_beat_count + rel_beat_index
        } else {
            rel_beat_index
        }

        for (channel in this.channels) {
            channel.remove_beat(beat_index)
        }
        this.set_beat_count(this.opus_beat_count - 1)
    }

    open fun remove_channel(channel: Int) {
        this.channels.removeAt(channel)
    }

    open fun remove_line(channel: Int, index: Int? = null) {
        this.channels[channel].remove_line(index)
    }

    open fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        this.channels[beat_key.channel].replace_tree(beat_key.line_offset, beat_key.beat, position, tree)
    }

    open fun replace_beat(beat_key: BeatKey, tree: OpusTree<OpusEvent>) {
        this.channels[beat_key.channel].replace_tree(beat_key.line_offset, beat_key.beat, listOf(), tree)
    }

    open fun swap_channels(channel_a: Int, channel_b: Int) {
        var tmp = this.channels[channel_b]
        this.channels[channel_b] = this.channels[channel_a]
        this.channels[channel_a] = tmp
    }

    open fun get_midi(start_beat: Int = 0, end_beat_rel: Int? = null): MIDI {
        var end_beat = if (end_beat_rel == null) {
            this.opus_beat_count
        } else if (end_beat_rel < 0) {
            this.opus_beat_count + end_beat_rel
        } else {
            end_beat_rel
        }
        //var instruments = i_arg ?: HashMap<Int, Int>()
        var instruments = HashMap<Int, Int>()
        var tempo = this.tempo

        var midi = MIDI()
        for (i in 0 until 16) {
            var instrument = instruments[i]?: 0
            midi.insert_event(0,0, ProgramChange(i, instrument))
        }

        midi.insert_event(0,0, SetTempo.from_bpm(tempo))
        data class StackItem(var tree: OpusTree<OpusEvent>, var divisions: Int, var offset: Int, var size: Int)
        this.channels.forEachIndexed { c, channel ->
            for (l in 0 until channel.size) {
                var line = channel.get_line(l)
                var current_tick = 0
                var prev_note = 0
                line.forEachIndexed { b, beat ->
                    var stack: MutableList<StackItem> = mutableListOf(StackItem(beat, 1, current_tick, midi.ppqn))
                    while (stack.isNotEmpty()) {
                        var current = stack.removeFirst()

                        if (current.tree.is_event()) {
                            var event = current.tree.get_event()!!
                            var note = if (this.is_percussion(c)) { // Ignore the event data and use percussion map
                                this.get_percussion_instrument(l) + 35
                            } else if (event.relative) {
                                event.note + prev_note
                            } else {
                                event.note + 21
                            }

                            if (!(b < start_beat || b >= end_beat)) {
                                midi.insert_event(
                                    0,
                                    current.offset,
                                    NoteOn(c, note, 64)
                                )
                                midi.insert_event(
                                    0,
                                    current.offset + current.size,
                                    NoteOff(c, note, 64)
                                )
                            }
                            prev_note = note
                        } else if (!current.tree.is_leaf()) {
                            var working_subdiv_size = current.size / current.tree.size
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
        return midi
    }


    fun get_beat_tree(beat_key: BeatKey): OpusTree<OpusEvent> {
        if (beat_key.channel >= this.channels.size) {
            throw Exception("Invalid BeatKey $beat_key")
        }

        // TODO: Check if i ever use a negative line_offset. and change it if i do
        var line_offset: Int = if (beat_key.line_offset < 0) {
            this.channels[beat_key.channel].size - beat_key.line_offset
        } else {
            beat_key.line_offset
        }
        if (line_offset > this.channels[beat_key.channel].size) {
            throw Exception("Invalid BeatKey $beat_key")
        }

        return this.channels[beat_key.channel].get_tree(line_offset, beat_key.line_offset)
    }

    fun get_tree(beat_key: BeatKey, position: List<Int>): OpusTree<OpusEvent> {
        return this.channels[beat_key.channel].get_tree(beat_key.line_offset, beat_key.beat, position)
    }

    fun get_preceding_leaf(beat_key: BeatKey, position: List<Int>): OpusTree<OpusEvent>? {
        var pair = this.get_preceding_leaf_position(beat_key, position) ?: return null
        return this.get_tree(pair.first, pair.second)
    }

    fun get_preceding_leaf_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
        var working_position = position.toMutableList()
        var working_beat_key = BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat)

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
            working_tree = working_tree.get(working_tree.size - 1)
        }

        return Pair(working_beat_key, working_position)
    }

    open fun set_beat_count(new_count: Int) {
        this.opus_beat_count = new_count
        for (channel in this.channels) {
            channel.set_beat_count(new_count)
        }
    }

    open fun save(path: String? = null) {
        if (path == null && this.path == null) {
            throw Exception("NoPathGiven")
        }

        if (path != null) {
            this.path = path
        }

        var directory = File(this.path)
        if (!directory.isDirectory) {
            if (! directory.mkdirs()) {
                throw Exception("Could not make directory")
            }
        }

        for (file in directory.list()!!) {
            File("${this.path}/$file").delete()
        }

        for (i in 0 until this.channels.size) {
            var channel = this.channels[i]
            var strLines: MutableList<String> = mutableListOf()
            for (j in 0 until channel.size) {
                var line = channel.get_line(j)
                var beatstrs: MutableList<String> = mutableListOf()
                for (beat in line) {
                    beatstrs.add(to_string(beat))
                }
                var str_line =  beatstrs.joinToString("|", "{", "}")
                strLines.add(str_line)
            }
            var working_file = File("${this.path}/channel_$i")
            working_file.createNewFile()
            working_file.writeText(strLines.joinToString("\n"))
        }
    }

    open fun load(path: String) {
        this.reset()
        this.path = path
        this.load_folder(path)
    }

    open fun new() {
        this.reset()
        this.new_channel()
        this.new_line(0)
        this.set_beat_count(4)
    }

    fun get_working_dir(): String? {
        return this.path
    }

    open fun load_folder(path: String) {
        var channel_map = HashMap<String, Int>()
        var suffix_patt = ".*_((\\d{1,3})?)(\\..*)?".toRegex()
        var filenames: MutableList<String> = mutableListOf()
        for (file in File(path).list()!!) {
            if (file.endsWith(".json")) {
                continue
            }
            var matches = suffix_patt.findAll(file).toList()
            if (matches.isNotEmpty()) {
                var channel = matches.first().groups[1]?.value?.toInt()!!
                channel_map["$path/$file"] = channel
                filenames.add("${path}/${file}")
            }
        }
        var line_patt = "\\{(.*?)\\}".toRegex()
        var beat_count = 1
        for (filename in filenames) {
            var channel = if (channel_map.containsKey(filename)) {
                channel_map[filename]!!
            } else {
                0
            }

            while (channel >= this.get_channel_count()) {
                this.new_channel()
            }

            var content = File(filename).readText(Charsets.UTF_8)
            var lines = line_patt.findAll(content)
            for (line in lines) {
                var opus_line: MutableList<OpusTree<OpusEvent>> = mutableListOf()
                for (beat_str in line.groups[1]?.value?.split("|")!!) {
                    var beat_tree = from_string(beat_str, this.RADIX, channel)
                    beat_tree.clear_singles()
                    opus_line.add(beat_tree)
                }
                beat_count = kotlin.math.max(opus_line.size, beat_count)
                while (opus_line.size < beat_count) {
                    opus_line.add(OpusTree<OpusEvent>())
                }
                if (this.channels[channel].size != beat_count) {
                    this.channels[channel].set_beat_count(beat_count)
                }
                this.channels[channel].insert_line(this.channels[channel].size, opus_line)
            }
        }
        this.opus_beat_count = beat_count
    }

    fun import_midi(path: String) { }

    fun get_channel_line_counts(): List<Int> {
        var output: MutableList<Int> = mutableListOf()
        for (i in 0 until this.channels.size) {
            output.add(this.channels[i].size)
        }
        return output
    }

    fun convert_event_to_relative(beat_key: BeatKey, position: List<Int>) {
        var tree = this.get_tree(beat_key, position)
        if (!tree.is_event()) {
            return
        }

        var event = tree.get_event()!!
        if (event.relative) {
            return
        }

        var working_beat_key: BeatKey = beat_key
        var working_position: List<Int> = position
        var preceding_value: Int? = null
        while (preceding_value == null) {
            var pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: throw Exception("No preceding value")
            preceding_value = this.get_absolute_value(pair.first, pair.second)
            working_beat_key = pair.first
            working_position = pair.second
        }

        this.set_event(beat_key, position, OpusEvent(
            event.note - preceding_value!!,
            event.radix,
            event.channel,
            true
        ))
    }

    fun convert_event_to_absolute(beat_key: BeatKey, position: List<Int>) {
        var tree = this.get_tree(beat_key, position)
        if (!tree.is_event()) {
            return
        }

        var event = tree.get_event()!!
        if (!event.relative) {
            return
        }

        var value = this.get_absolute_value(beat_key, position) ?: throw Exception("No Preceding value")
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

    fun get_absolute_value(beat_key: BeatKey, position: List<Int>): Int? {
        var tree = this.get_tree(beat_key, position)

        var abs_value = 0
        if (tree.is_event()) {
            var event = tree.get_event()!!
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
            var pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: throw Exception("No Initial Value")
            working_beat_key = pair.first
            working_position = pair.second

            var working_tree = this.get_tree(working_beat_key, working_position)

            if (working_tree.is_event()) {
                var working_event = working_tree.get_event()!!
                abs_value += working_event.note
                if (!working_event.relative) {
                    break
                }
            }
        }

        return abs_value
    }

    fun has_preceding_absolute_event(beat_key: BeatKey, position: List<Int>): Boolean {
        var working_beat_key = beat_key
        var working_position = position.toList()

        var output = false
        while (true) {
            var pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: break
            working_beat_key = pair.first
            working_position = pair.second

            var working_tree = this.get_tree(working_beat_key, working_position)

            if (working_tree.is_event()) {
                var working_event = working_tree.get_event()!!
                if (!working_event.relative) {
                    output = true
                    break
                }
            }
        }

        return output
    }
}
