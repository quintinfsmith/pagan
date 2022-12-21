package com.qfs.radixulous.opusmanager
import android.util.Log
import com.qfs.radixulous.apres.MIDI
import com.qfs.radixulous.structure.OpusTree
import com.qfs.radixulous.from_string
import com.qfs.radixulous.to_string
import com.qfs.radixulous.tree_to_midi
import java.io.File
import java.lang.Math.max


data class OpusEvent(var note: Int, var radix: Int, var channel: Int, var relative: Boolean)
data class BeatKey(var channel: Int, var line_offset: Int, var beat: Int)

open class OpusManagerBase {
    var RADIX: Int = 12
    var DEFAULT_PERCUSSION: Int = 0x35
    var channel_lines: Array<MutableList<MutableList<OpusTree<OpusEvent>>>> = Array(16, { _ -> mutableListOf() })
    var opus_beat_count: Int = 1
    var path: String? = null
    var percussion_map: HashMap<Int, Int> = HashMap<Int, Int>()
    var channel_instruments: HashMap<Int, Int> = HashMap()


    open fun reset() {
        this.opus_beat_count = 1
        for (channel in this.channel_lines) {
            channel.clear()
        }
        this.percussion_map.clear()
        this.path = null
    }

    open fun insert_after(beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) {
            throw Exception("Invalid Position ${position}")
        }

        val tree = this.get_tree(beat_key, position)
        val parent = tree.get_parent() ?: throw Exception("Invalid Position ${position}")

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
        if (beat_key.channel != 9) {
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
        return if (this.percussion_map.containsKey(line_offset)) {
            this.percussion_map[line_offset]!!
        } else {
            this.DEFAULT_PERCUSSION
        }
    }
    open fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        this.percussion_map[line_offset] = instrument
    }

    fun set_channel_instrument(channel:Int, instrument: Int) {
        this.channel_instruments[channel] = instrument
    }

    fun get_channel_instrument(channel: Int): Int {
        return if (this.channel_instruments.containsKey(channel)) {
            this.channel_instruments[channel]!!
        } else {
            0
        }
    }

    open fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        if (beat_key.channel == 9) {
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
        if (tree.is_event()) {
            tree.unset_event()
        } else {
            tree.empty()
        }
    }

    open fun add_channel(channel: Int) {
        this.new_line(channel)
    }

    open fun change_line_channel(old_channel: Int, line_index: Int, new_channel: Int) {
        var tree = this.channel_lines[old_channel].removeAt(line_index)
        this.channel_lines[new_channel].add(tree)
    }

    open fun insert_beat(index: Int?) {
        val abs_index = if (index == null) {
            this.opus_beat_count
        } else if (index < 0) {
            this.opus_beat_count + index
        } else {
            index
        }
        this.opus_beat_count += 1
        for (channel in this.channel_lines) {
            for (line in channel) {
                line.add(abs_index, OpusTree<OpusEvent>())
            }
        }
    }

    open fun move_line(channel: Int, old_index: Int, new_index: Int) {
        if (old_index == new_index) {
            return
        }

        // Adjust the new_index so it doesn't get confused
        // when we pop() the old_index
        var adj_new_index: Int = if (new_index < 0) {
            this.channel_lines[channel].size + new_index
        } else {
            new_index
        }

        if (new_index < 0) {
            throw Exception("INDEXERROR")
        }
        if (old_index >= this.channel_lines[channel].size) {
            throw Exception("INDEXERROR")
        }

        var line = this.channel_lines[channel].removeAt(old_index)
        this.channel_lines[channel].add(adj_new_index, line)
    }

    open fun new_line(channel: Int, index: Int? = null) {
        val line: MutableList<OpusTree<OpusEvent>> = MutableList(this.opus_beat_count) { _ -> OpusTree<OpusEvent>() }

        if (index == null) {
            this.channel_lines[channel].add(line)
        } else {
            this.channel_lines[channel].add(index, line)
        }
    }

    open fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        var new_tree = this.channel_lines[new_beat.channel][new_beat.line_offset][new_beat.beat].copy()
        var old_tree = this.channel_lines[old_beat.channel][old_beat.line_offset][old_beat.beat]
        old_tree.replace_with(new_tree)
        this.channel_lines[old_beat.channel][old_beat.line_offset][old_beat.beat] = new_tree
    }

    open fun remove_beat(rel_beat_index: Int?) {
        var beat_index = if (rel_beat_index == null) {
            this.opus_beat_count - 1
        } else if (rel_beat_index < 0) {
            this.opus_beat_count + rel_beat_index
        } else {
            rel_beat_index
        }

        for (channel in this.channel_lines) {
            for (line in channel) {
                line.removeAt(beat_index)
            }
        }
        this.set_beat_count(this.opus_beat_count - 1)
    }

    open fun remove_channel(channel: Int) {
        while (this.channel_lines[channel].size > 0) {
            this.remove_line(channel, 0)
        }
    }

    open fun remove_line(channel: Int, index: Int? = null) {
        val adj_index = index ?: (this.channel_lines[channel].size - 1)
        this.channel_lines[channel].removeAt(adj_index)
    }

    open fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        if (position.isEmpty()) {
            this.channel_lines[beat_key.channel][beat_key.line_offset][beat_key.beat] = tree
        } else {
            this.get_tree(beat_key, position).replace_with(tree)
        }
    }

    open fun replace_beat(beat_key: BeatKey, tree: OpusTree<OpusEvent>) {
        var old_tree = this.channel_lines[beat_key.channel][beat_key.line_offset][beat_key.beat]
        old_tree.replace_with(tree)
    }

    open fun swap_channels(channel_a: Int, channel_b: Int) {
        var tmp = this.channel_lines[channel_b]
        this.channel_lines[channel_b] = this.channel_lines[channel_a]
        this.channel_lines[channel_a] = tmp
    }

    open fun get_midi(): MIDI {
        var opus = OpusTree<Set<OpusEvent>>()
        opus.set_size(this.opus_beat_count)
        var beatlists: MutableList<MutableList<OpusTree<OpusEvent>>> = mutableListOf()
        for (i in 0 until this.opus_beat_count) {
            beatlists.add(mutableListOf())
        }

        for (channel in this.channel_lines) {
            for (line in channel) {
                line.forEachIndexed { i, beat ->
                    beatlists[i].add(beat)
                }
            }
        }
        beatlists.forEachIndexed { i, beatlist ->
            beatlist.forEachIndexed { j, beat ->
                var new_beat = beat.merge(opus.get(i))
                opus.set(i, new_beat)
            }
        }
        // TODO: KWARGS
        return tree_to_midi(opus, 120.toFloat())
    }

    fun get_beat_tree(beat_key: BeatKey): OpusTree<OpusEvent> {
        if (beat_key.channel >= this.channel_lines.size) {
            throw Exception("Invalid BeatKey $beat_key")
        }

        var line_offset: Int = if (beat_key.line_offset < 0) {
            this.channel_lines[beat_key.channel].size - beat_key.line_offset
        } else {
            beat_key.line_offset
        }
        if (line_offset > this.channel_lines[beat_key.channel].size) {
            throw Exception("Invalid BeatKey $beat_key")
        }
        return this.channel_lines[beat_key.channel][line_offset][beat_key.beat]
    }

    fun get_tree(beat_key: BeatKey, position: List<Int>): OpusTree<OpusEvent> {
        var tree = this.get_beat_tree(beat_key)
        for (pos in position) {
            if (!tree.is_leaf()) {
                tree = tree.get(pos)
            } else {
                throw Exception("Invalid position $position")
            }
        }

        return tree
    }

    fun get_preceding_leaf(beat_key: BeatKey, position: List<Int>): OpusTree<OpusEvent>? {
        var pair = this.get_preceding_leaf_position(beat_key, position) ?: return null
        return this.get_tree(pair.first, pair.second)
    }

    private fun get_preceding_leaf_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
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

    private fun set_beat_count(new_count: Int) {
        this.opus_beat_count = new_count
        for (channel in this.channel_lines) {
            for (line in channel) {
                while (line.size > new_count) {
                    line.removeLast()
                }
                while (new_count > line.size) {
                    line.add(OpusTree())
                }
            }
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

        for (i in 0 until this.channel_lines.size) {
            var channel = this.channel_lines[i]
            if (channel.isEmpty()) {
                continue
            }
            var strLines: MutableList<String> = mutableListOf()
            for (line in channel) {
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
        var new_line: MutableList<OpusTree<OpusEvent>> = MutableList(4) { _ -> OpusTree<OpusEvent>() }

        this.channel_lines[0].add(new_line)
        this.opus_beat_count = 4
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
            var content = File(filename).readText(Charsets.UTF_8)
            var lines = line_patt.findAll(content)
            for (line in lines) {
                var opus_line: MutableList<OpusTree<OpusEvent>> = mutableListOf()
                for (beat_str in line.groups[1]?.value?.split("|")!!) {
                    var beat_tree = from_string(beat_str, this.RADIX, channel)
                    beat_tree.clear_singles()
                    opus_line.add(beat_tree)
                }
                beat_count = max(opus_line.size, beat_count)
                while (opus_line.size < beat_count) {
                    opus_line.add(OpusTree<OpusEvent>())
                }
                this.channel_lines[channel].add(opus_line)
            }
        }
        this.opus_beat_count = beat_count
    }

    fun import_midi(path: String) { }

    fun get_channel_line_counts(): List<Int> {
        var output: MutableList<Int> = mutableListOf()
        for (i in 0 until this.channel_lines.size) {
            output.add(this.channel_lines[i].size)
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
        this.set_event(beat_key, position, OpusEvent(
            value,
            event.radix,
            event.channel,
            false
        ))
    }

    private fun get_absolute_value(beat_key: BeatKey, position: List<Int>): Int? {
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
