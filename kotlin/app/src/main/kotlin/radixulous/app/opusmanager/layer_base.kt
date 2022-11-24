package radixulous.app.opusmanager
import radixulous.app.structure.OpusTree

data class OpusEvent(var note: Int, var radix: Int, var channel: Int, var relative: Boolean)
data class BeatKey(var channel: Int, var line_offset: Int, var beat: Int)

open class OpusManagerBase {
    var RADIX: Int = 12
    var DEFAULT_PERCUSSION: Int = 0x35
    var channel_lines: Array<MutableList<MutableList<OpusTree<OpusEvent>>>> = Array(16, { _ -> mutableListOf() })
    var opus_beat_count: Int = 1
    var path: String? = null
    var percussion_map: HashMap<Int, Int> = HashMap<Int, Int>()


    open fun insert_after(beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) {
            throw Exception("Invalid Position {position}")
        }
        val tree = this.get_tree(beat_key, position)
        val parent = tree.get_parent()
        if (parent != null && position.last() != parent.size - 1) {
            val tmp = parent.get(parent.size - 1)
            var i = parent.size - 1;
            while (i > position.last() + 1) {
                parent.set(i, parent.get(i - 1))
                i -= 1
            }
            parent.set(i, tmp)
        }
    }

    open fun remove(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)

        // Can't remove beat
        if (tree.parent == null) {
            return
        }
        if (tree.parent.size == 1) {
            var next_postision = position.copy()
            next_position.removeLast()
            this.remove(beat_key, next_position)
        }

        tree.detach
    }

    open fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        if (beat_key.channel != 9) {
            throw Exception("Attempting to set non-percussion channel")
        }

        var tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            tree.unset_event()
        }

        var instrument = if (this.percussion_map.containsKey(beat_key.line_offset)) {
            this.percussion_map[beat_key.line_offset]!!
        } else {
            this.DEFAULT_PERCUSSION
        }

        tree.set_event(OpusEvent(
            instrument,
            this.RADIX,
            9,
            false
        ))
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

        var new_tree = OpusTree<OpusEvent>()
        new_tree.set_size(splits)
        tree.replace_with(new_tree)
        new_tree.get(0).replace_with(tree)
        while (new_tree.parent != null && new_tree.parent.size == 1) {
            new_tree.parent.replace_with(new_tree)

        if (new_tree.parent == null) {
            this.channel_lines[beat_key[0]][beat_key[1]][beat_key[2]] = new_tree
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
        var abs_index = if (index == null) {
            this.opus_beat_count
        } else if (index < 0) {
            this.opus_beat_count + index + 1
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
        var line: MutableList<OpusTree<OpusEvent>> = MutableList(16, { _ -> OpusTree<OpusEvent>() })

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
        var adj_index = if (index == null) {
            this.channel_lines[channel].size - 1
        } else {
            index
        }
        this.channel_lines[channel].removeAt(adj_index)
    }

    open fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        this.get_tree(beat_key, position).replace_with(tree)
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

    //open fun export(path: String? = null, kwargs: HashMap) { }

    fun get_beat_tree(beat_key: BeatKey): OpusTree<OpusEvent> {
        var line_offset: Int
        if (beat_key.channel >= this.channel_lines.size) {
            throw Exception("Invalid BeatKey {beat_key}")
        }

        if (beat_key.line_offset < 0) {
            line_offset = this.channel_lines[beat_key.channel].size - beat_key.line_offset
        } else {
            line_offset = beat_key.line_offset
        }
        if (line_offset > this.channel_lines[beat_key.channel].size) {
            throw Exception("Invalid BeatKey {beat_key}")
        }
        return this.channel_lines[beat_key.channel][line_offset][beat_key.beat]
    }

    fun get_tree(beat_key: BeatKey, position: List<Int>): OpusTree<OpusEvent> {
        if (position.size < 1) {
            throw Exception("Invalid Position {position}")
        }
        var tree = this.get_beat_tree(beat_key)
        for (pos in position) {
            if (pos < tree.size) {
                tree = tree.get(pos)
            } else {
                throw Exception("Invalid Position {position}")
            }
        }

        return tree
    }

    private fun set_beat_count(new_count: Int) {
        this.opus_beat_count = new_count
        for (channel in this.channel_lines) {
            for (line in channel) {
                while (line.size > new_count) {
                    line.removeLast()
                }
                while (new_count > line.size) {
                    line.add(OpusTree<OpusEvent>())
                }
            }
        }
    }

    open fun save(path: String? = null) { }
    open fun load(path: String) { }
    open fun new() {
        var new_line: MutableList<OpusTree<OpusEvent>> = MutableList(4, { _ -> OpusTree<OpusEvent>() })

        this.channel_lines[0].add(new_line)

        this.opus_beat_count = 4
    }

    private fun get_working_dir(): String? { return "" }
    open fun load_folder(path: String) { }
    open fun load_file(path: String) { }

    fun import_midi(path: String) { }
}
