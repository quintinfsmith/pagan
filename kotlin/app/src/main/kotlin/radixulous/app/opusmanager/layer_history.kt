package radixulous.app.opusmanager
import radixulous.app.structure.OpusTree
import radixulous.app.opusmanager.CursorLayer
import radixulous.app.opusmanager.BeatKey
import radixulous.app.opusmanager.OpusEvent

open class HistoryLayer() : CursorLayer() {

    var history_ledger: MutableList<MutableList<String>> = mutableListOf()
    var history_locked = false
    var multi_counter: Int = 0
    var int_stack: MutableList<Int> = mutableListOf()
    var beat_stack: MutableList<OpusTree<OpusEvent>> = mutableListOf()

    private fun get_position_from_int_stack(): List<Int> {
        // List<Int> prefaced by length
        var position: MutableList<Int> = mutableListOf()
        for (_i in 0 .. this.int_stack.removeLast() - 1) {
            position.add(0, this.int_stack.removeLast())
        }
        return position
    }

    private fun add_position_to_int_stack(position: List<Int>) {
        var size = position.size
        for (i in position) {
            this.int_stack.add(i)
        }
        this.int_stack.add(size)
    }

    private fun get_beatkey_from_int_stack(): BeatKey {
        return BeatKey(
            this.int_stack.removeLast(),
            this.int_stack.removeLast(),
            this.int_stack.removeLast()
        )
    }

    private fun add_beatkey_to_int_stack(beat_key: BeatKey) {
        this.int_stack.add(beat_key.beat)
        this.int_stack.add(beat_key.line_offset)
        this.int_stack.add(beat_key.channel)
    }

    private fun get_boolean_from_int_stack(): Boolean {
        return this.int_stack.removeLast() != 0
    }

    private fun add_boolean_to_int_stack(bool: Boolean) {
        if (bool) {
            this.int_stack.add(1)
        } else {
            this.int_stack.add(0)
        }
    }

    private fun get_int_from_int_stack(): Int {
        return this.int_stack.removeLast()
    }
    private fun add_int_to_int_stack(int: Int) {
        this.int_stack.add(int)
    }

    fun apply_undo() {
        if (this.history_ledger.size == 0) {
            return
        }

        this.history_locked = true

        for (func_name in this.history_ledger.removeLast()) {
            when (func_name) {
                "split_tree" -> {
                    var splits = this.get_int_from_int_stack()
                    var position = this.get_position_from_int_stack()
                    var beat_key = this.get_beatkey_from_int_stack()
                    this.split_tree(beat_key, position, splits)
                }
                "set_event" -> {
                    var relative = this.get_boolean_from_int_stack()
                    var value = this.get_int_from_int_stack()
                    var position = this.get_position_from_int_stack()
                    var beat_key = this.get_beatkey_from_int_stack()
                    var event = OpusEvent(value, this.RADIX, beat_key.channel, relative)

                    this.set_event(beat_key, position, event)
                }
                "set_percussion_event" -> {
                    var position = this.get_position_from_int_stack()
                    var beat_key = this.get_beatkey_from_int_stack()
                    this.set_percussion_event(beat_key, position)
                }
                "unset" -> {
                    var position = this.get_position_from_int_stack()
                    var beat_key = this.get_beatkey_from_int_stack()
                    this.unset(beat_key, position)
                }
                "replace_beat" -> {
                    var beat = this.beat_stack.removeLast()
                    var beat_key = this.get_beatkey_from_int_stack()
                    this.replace_beat(beat_key, beat)
                }
                "swap_channels" -> {
                    var channel_b = this.get_int_from_int_stack()
                    var channel_a = this.get_int_from_int_stack()
                    this.swap_channels(channel_a, channel_b)
                }
                "remove_line" -> {
                    var line_offset = this.get_int_from_int_stack()
                    var channel = this.get_int_from_int_stack()
                    this.remove_line(channel, line_offset)
                }
                "new_line" -> {
                    var line_offset = this.get_int_from_int_stack()
                    var channel = this.get_int_from_int_stack()
                    this.new_line(channel, line_offset)
                }
                "remove" -> {
                    var position = this.get_position_from_int_stack()
                    var beat_key = this.get_beatkey_from_int_stack()
                    this.remove(beat_key, position)
                }
                "remove_beat" -> {
                    var index = this.get_int_from_int_stack()
                    this.remove_beat(index)
                }
                "insert_beat" -> {
                    var index = this.get_int_from_int_stack()
                    this.insert_beat(index)
                }
                "set_cursor" -> {
                    var position = this.get_position_from_int_stack()
                    var beat_key = this.get_beatkey_from_int_stack()
                    var y = this.get_y(beat_key.channel, beat_key.line_offset)
                    this.cursor.set(y, beat_key.beat, position)
                }
            }
        }

        this.history_locked = false
    }

    private fun append_undoer_key(func: String): Boolean {
        if (this.history_locked) {
            return false
        }

        if (this.multi_counter > 0) {
            this.history_ledger.last().add(func)
        } else {
            this.history_ledger.add(mutableListOf(func))
        }

        return true
    }

    private fun open_multi() {
        if (this.history_locked) {
            return
        }

        if (this.multi_counter == 0) {
            this.history_ledger.add(mutableListOf())
        }
        this.multi_counter += 1
    }

    open fun close_multi() {
        if (this.history_locked) {
            return
        }
        this.multi_counter -= 1

        if (! this.history_locked || this.multi_counter > 0) {
            this.add_beatkey_to_int_stack(this.cursor.get_beatkey())
            this.add_position_to_int_stack(this.cursor.get_position())

            this.history_ledger.last().add("set_cursor")
        }
    }

    private fun setup_repopulate(beat_key: BeatKey, start_position: List<Int>) {
        if (this.history_locked) {
            return
        }
        this.open_multi()

        val beat_tree = this.channel_lines[beat_key.channel][beat_key.line_offset][beat_key.beat]

        var stack: MutableList<List<Int>> = mutableListOf()

        if (start_position.size == 0) {
            for (i in 0..beat_tree.size - 1) {
                stack.add(listOf(i))
            }

            //////////////////////
            if (this.append_undoer_key("split_tree")) {
                this.add_beatkey_to_int_stack(beat_key)
                this.add_position_to_int_stack(listOf())
                this.add_int_to_int_stack(beat_tree.size)
            }
            //////////////////////
        } else {
            stack.add(start_position)
        }

        while (stack.size > 0) {
            var position = stack.removeAt(0)
            var tree = beat_tree
            for (i in position) {
                tree = tree.get(i)
            }

            if (! tree.is_leaf()) {
                //////////////////////
                if (this.append_undoer_key("split_tree")) {
                    this.add_beatkey_to_int_stack(beat_key)
                    this.add_position_to_int_stack(position)
                    this.add_int_to_int_stack(tree.size)
                }
                //////////////////////
                for (i in 0 .. tree.size - 1) {
                    var next_position = position.toMutableList()
                    next_position.add(i)
                    stack.add(next_position)
                }
            } else if (tree.is_event()) {
                var event = tree.get_event()!!
                if (beat_key.channel != 9) {
                    //////////////////////
                    if (this.append_undoer_key("set_event")) {
                        this.add_beatkey_to_int_stack(beat_key)
                        this.add_position_to_int_stack(position)
                        this.add_int_to_int_stack(event.note)
                        this.add_boolean_to_int_stack(event.relative)
                    }
                    //////////////////////
                } else {
                    //////////////////////
                    if (this.append_undoer_key("set_percussion_event")) {
                        this.add_beatkey_to_int_stack(beat_key)
                        this.add_position_to_int_stack(position)
                    }
                    //////////////////////
                }
            } else {
                //////////////////////
                if (this.append_undoer_key("unset")) {
                    this.add_beatkey_to_int_stack(beat_key)
                    this.add_position_to_int_stack(position)
                }
                //////////////////////
            }
        }
        this.close_multi()
    }

    open override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        this.setup_repopulate(old_beat, listOf())
        super.overwrite_beat(old_beat, new_beat)
    }

    open override fun swap_channels(channel_a: Int, channel_b: Int) {
        if (this.append_undoer_key("swap_channels")) {
            this.add_int_to_int_stack(channel_a)
            this.add_int_to_int_stack(channel_b)
        }

        super.swap_channels(channel_a, channel_b)
    }

    open override fun new_line(channel: Int, index: Int?) {
        if (this.append_undoer_key("remove_line")) {
            var abs_index = if (index == null) {
                this.channel_lines[channel].size - 1
            } else {
                index
            }

            this.add_int_to_int_stack(channel)
            this.add_int_to_int_stack(abs_index)
        }

        super.new_line(channel, index)
    }

    open override fun remove_line(channel: Int, line_offset: Int?) {
        var abs_line_offset: Int
        if (line_offset == null) {
            abs_line_offset = this.channel_lines[channel].size - 1
        } else {
            abs_line_offset = line_offset
        }

        this.open_multi()
        if (this.append_undoer_key("new_line")) {
            this.add_int_to_int_stack(channel)
            this.add_int_to_int_stack(abs_line_offset)
            for (i in 0 .. this.opus_beat_count - 1) {
                var beat_key = BeatKey(channel, abs_line_offset, i)
                this.setup_repopulate(beat_key, listOf())
            }
        }
        this.close_multi()

        super.remove_line(channel, line_offset)
    }

    open override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        if (position.size > 0) {
            if (this.append_undoer_key("remove")) {
                var rposition = position.toMutableList()
                rposition[rposition.size - 1] += 1

                this.add_beatkey_to_int_stack(beat_key)
                this.add_position_to_int_stack(rposition)
            }
        }
        super.insert_after(beat_key, position)
    }

    open override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.setup_repopulate(beat_key, position.slice(0..position.size - 1))
        super.split_tree(beat_key, position, splits)
    }

    open override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.setup_repopulate(beat_key, position.slice(0..position.size - 1))
        super.remove(beat_key, position)
    }

    open override fun insert_beat(index: Int?) {
        if (this.append_undoer_key("remove_beat")) {
            var abs_index = if (index == null) {
                this.opus_beat_count - 1
            } else {
                index
            }
            this.add_int_to_int_stack(abs_index)
        }

        super.insert_beat(index)
    }

    override fun remove_beat(index: Int?) {
        var abs_index = if (index == null) {
            this.opus_beat_count - 1
        } else {
            index
        }

        this.open_multi()
        if (this.append_undoer_key("insert_beat")) {
            this.add_int_to_int_stack(abs_index)
        }

        for (i in 0 .. this.channel_lines.size - 1) {
            var channel = this.channel_lines[i]
            for (j in 0 .. channel.size - 1) {
                var line = channel[j]
                for (k in abs_index .. line.size - 1) {
                    this.setup_repopulate(BeatKey(i,j,k), listOf())
                }
            }
        }
        this.close_multi()

        super.remove_beat(index)
    }

    open override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        var tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            var original_event = tree.get_event()!!
            if (this.append_undoer_key("set_event")) {
                this.add_beatkey_to_int_stack(beat_key)
                this.add_position_to_int_stack(position)
                this.add_int_to_int_stack(original_event.note)
                this.add_boolean_to_int_stack(original_event.relative)
            }
        } else {
            if (this.append_undoer_key("unset")) {
                this.add_beatkey_to_int_stack(beat_key)
                this.add_position_to_int_stack(position)
            }
        }

        super.set_event(beat_key, position, event)
    }

    open override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        var tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            var original_event = tree.get_event()!!
            if (beat_key.channel == 9) {
                if (this.append_undoer_key("set_event")) {
                    this.add_beatkey_to_int_stack(beat_key)
                    this.add_position_to_int_stack(position)
                    this.add_int_to_int_stack(original_event.note)
                    this.add_boolean_to_int_stack(original_event.relative)
                }
            } else if (this.append_undoer_key("set_percussion_event")) {
                this.add_beatkey_to_int_stack(beat_key)
                this.add_position_to_int_stack(position)
            }
        } else if (this.append_undoer_key("unset")) {
            this.add_beatkey_to_int_stack(beat_key)
            this.add_position_to_int_stack(position)
        }

        super.set_percussion_event(beat_key, position)
    }

    open override fun unset(beat_key: BeatKey, position: List<Int>) {
        var tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            var original_event = tree.get_event()!!
            if (this.append_undoer_key("set_event")) {
                this.add_beatkey_to_int_stack(beat_key)
                this.add_position_to_int_stack(position)
                this.add_int_to_int_stack(original_event.note)
                this.add_boolean_to_int_stack(original_event.relative)
            }
        }
        super.unset(beat_key, position)
    }
}
