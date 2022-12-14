package com.qfs.radixulous.opusmanager
import com.qfs.radixulous.structure.OpusTree
import java.lang.Integer.max

class HistoryCache() {
    var history_locked = false
    var multi_counter: Int = 0
    var int_stack: MutableList<Int> = mutableListOf()
    var beat_stack: MutableList<OpusTree<OpusEvent>> = mutableListOf()
    var history_ledger: MutableList<MutableList<String>> = mutableListOf()
    fun isLocked(): Boolean {
        return this.history_locked
    }
    fun append_undoer_key(func: String): Boolean {
        if (this.history_locked) {
            return false
        }
        if (this.multi_counter > 0) {
            this.history_ledger.last().add(0, func)
        } else {
            this.history_ledger.add(mutableListOf(func))
        }

        return true
    }

    fun open_multi() {
        if (this.history_locked) {
            return
        }

        if (this.multi_counter == 0) {
            this.history_ledger.add(mutableListOf())
        }
        this.multi_counter += 1
    }

    open fun close_multi(beat_key: BeatKey, position: List<Int>) {
        if (this.history_locked) {
            return
        }
        this.multi_counter -= 1

        if (this.multi_counter > 0) {
            this.push_set_cursor(beat_key, position)
        }
    }

    fun push_set_cursor(beat_key: BeatKey, position: List<Int>) {
        if (this.append_undoer_key("set_cursor")) {
            this.add_beatkey(beat_key)
            this.add_position(position)
        }
    }

    fun clear() {
        this.int_stack.clear()
        this.beat_stack.clear()
        this.history_ledger.clear()
        this.history_locked = false
        this.multi_counter = 0
    }

    fun intpop(): Int {
        var output = this.int_stack.removeLast()
        return output
    }

    fun get_position(): List<Int> {
        // List<Int> prefaced by length
        var position: MutableList<Int> = mutableListOf()
        var length = this.intpop()
        for (_i in 0 until length) {
            position.add(0, this.intpop())
        }
        return position
    }

    fun add_position(position: List<Int>) {
        if (position.isEmpty()) {
            this.int_stack.add(0)
        } else {
            var size = position.size
            for (i in position) {
                this.int_stack.add(i)
            }
            this.int_stack.add(size)
        }
    }

    fun get_beatkey(): BeatKey {
        var channel = this.intpop()
        var line_offset = this.intpop()
        var beat = this.intpop()
        return BeatKey(channel, line_offset, beat)
    }

    fun add_beatkey(beat_key: BeatKey) {
        this.int_stack.add(beat_key.beat)
        this.int_stack.add(beat_key.line_offset)
        this.int_stack.add(beat_key.channel)
    }

    fun get_boolean(): Boolean {
        return this.intpop() != 0
    }

    fun add_boolean(bool: Boolean) {
        if (bool) {
            this.int_stack.add(1)
        } else {
            this.int_stack.add(0)
        }
    }

    fun get_int(): Int {
        return this.intpop()
    }
    fun add_int(int: Int) {
        this.int_stack.add(int)
    }

    fun get_beat(): OpusTree<OpusEvent> {
        return this.beat_stack.removeLast()
    }
    fun add_beat(beat: OpusTree<OpusEvent>) {
        this.beat_stack.add(beat)
    }
    fun isEmpty(): Boolean {
        return this.history_ledger.isEmpty()
    }
    fun lock() {
        this.history_locked = true
    }
    fun unlock() {
        this.history_locked = false
    }
    fun pop(): List<String> {
        return if (this.history_ledger.isEmpty()){
            listOf()
        } else {
            this.history_ledger.removeLast()
        }
    }
}

open class HistoryLayer() : CursorLayer() {
    var history_cache = HistoryCache()

    override fun reset() {
        this.history_cache.clear()
        super.reset()
    }

    fun apply_undo() {
        if (this.history_cache.isEmpty()) {
            return
        }

        this.history_cache.lock()

        for (func_name in this.history_cache.pop()) {
            when (func_name) {
                "split_tree" -> {
                    val splits = this.history_cache.get_int()
                    val position = this.history_cache.get_position()
                    val beat_key = this.history_cache.get_beatkey()
                    this.split_tree(beat_key, position, splits)
                }
                "set_event" -> {
                    var relative = this.history_cache.get_boolean()
                    var value = this.history_cache.get_int()
                    var position = this.history_cache.get_position()
                    var beat_key = this.history_cache.get_beatkey()
                    var event = OpusEvent(value, this.RADIX, beat_key.channel, relative)

                    this.set_event(beat_key, position, event)
                }
                "set_percussion_event" -> {
                    var position = this.history_cache.get_position()
                    var beat_key = this.history_cache.get_beatkey()
                    this.set_percussion_event(beat_key, position)
                }
                "unset" -> {
                    var position = this.history_cache.get_position()
                    var beat_key = this.history_cache.get_beatkey()
                    this.unset(beat_key, position)
                }
                "replace_beat" -> {
                    var beat = this.history_cache.get_beat()
                    var beat_key = this.history_cache.get_beatkey()
                    this.replace_beat(beat_key, beat)
                }
                "swap_channels" -> {
                    var channel_b = this.history_cache.get_int()
                    var channel_a = this.history_cache.get_int()
                    this.swap_channels(channel_a, channel_b)
                }
                "remove_line" -> {
                    var line_offset = this.history_cache.get_int()
                    var channel = this.history_cache.get_int()
                    this.remove_line(channel, line_offset)
                }
                "new_line" -> {
                    var line_offset = this.history_cache.get_int()
                    var channel = this.history_cache.get_int()
                    this.new_line(channel, line_offset)
                }
                "remove" -> {
                    var position = this.history_cache.get_position()
                    var beat_key = this.history_cache.get_beatkey()
                    this.remove(beat_key, position)
                }
                "remove_beat" -> {
                    var index = this.history_cache.get_int()
                    this.remove_beat(index)
                }
                "insert_beat" -> {
                    var index = this.history_cache.get_int()
                    this.insert_beat(index)
                }
                "set_cursor" -> {
                    var position = this.history_cache.get_position()
                    var beat_key = this.history_cache.get_beatkey()
                    var y = this.get_y(beat_key.channel, beat_key.line_offset)
                    this.cursor.set(y, beat_key.beat, position)
                }
            }
        }

        this.history_cache.unlock()
        this.cursor.settle()
    }

    private fun setup_repopulate(beat_key: BeatKey, start_position: List<Int>) {
        if (this.history_cache.isLocked()) {
            return
        }
        this.history_cache.open_multi()

        val beat_tree = this.channel_lines[beat_key.channel][beat_key.line_offset][beat_key.beat]
        val stack: MutableList<List<Int>> = mutableListOf(start_position)

        var splits: MutableList<Pair<List<Int>, Int>> = mutableListOf()
        while (stack.isNotEmpty()) {
            val position = stack.removeAt(0)
            var tree = beat_tree
            for (i in position) {
                tree = tree.get(i)
            }

            if (! tree.is_leaf()) {
                for (i in 0 until tree.size) {
                    val next_position = position.toMutableList()
                    next_position.add(i)
                    stack.add(next_position)
                }
                splits.add(Pair(position, tree.size))
            } else if (tree.is_event()) {
                val event = tree.get_event()!!
                if (beat_key.channel != 9) {
                    this.push_set_event(
                        beat_key,
                        position,
                        event.note,
                        event.relative
                    )
                } else {
                    this.push_set_percussion_event(beat_key, position)
                }
            }
        }

        for ((position, size) in splits) {
            this.push_split_tree(beat_key, position, size)
        }

        this.history_cache.close_multi(this.cursor.get_beatkey(), this.cursor.get_position())
    }

    open override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        this.setup_repopulate(old_beat, listOf())
        super.overwrite_beat(old_beat, new_beat)
    }

    open override fun swap_channels(channel_a: Int, channel_b: Int) {
        this.push_swap_channels(channel_a, channel_b)
        super.swap_channels(channel_a, channel_b)
    }

    open override fun new_line(channel: Int, index: Int?) {
        this.push_remove_line(channel, index ?: (this.channel_lines[channel].size - 1))
        super.new_line(channel, index)
    }

    open override fun remove_line(channel: Int, line_offset: Int?) {
        this.push_new_line(channel, line_offset ?: (this.channel_lines[channel].size - 1), this.opus_beat_count)
        super.remove_line(channel, line_offset)
    }

    open override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.push_remove(beat_key, position.toMutableList())
        super.insert_after(beat_key, position)
    }

    open override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.setup_repopulate(beat_key, position.toList())
        super.split_tree(beat_key, position, splits)
    }

    open override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.setup_repopulate(beat_key, listOf())
        super.remove(beat_key, position)
    }

    open override fun insert_beat(index: Int?) {
        this.push_remove_beat(index ?: (this.opus_beat_count - 1))

        super.insert_beat(index)
    }

    override fun remove_beat(index: Int?) {
        this.push_insert_beat(index ?: (this.opus_beat_count - 1), this.get_channel_line_counts())
        super.remove_beat(index)
    }

    open override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        var tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            var original_event = tree.get_event()!!
            this.push_set_event(beat_key, position, original_event.note, original_event.relative)
        } else {
            this.push_unset(beat_key, position)
        }

        super.set_event(beat_key, position, event)
    }

    open override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            val original_event = tree.get_event()!!
            if (beat_key.channel == 9) {
                this.push_set_event(beat_key, position, original_event.note, original_event.relative)
            } else {
                this.push_set_percussion_event(beat_key, position)
            }
        } else {
            this.push_unset(beat_key, position)
        }

        super.set_percussion_event(beat_key, position)
    }

    open override fun unset(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            val original_event = tree.get_event()!!
            this.push_set_event(beat_key, position, original_event.note, original_event.relative)
        }
        super.unset(beat_key, position)
    }


    fun push_new_line(channel: Int, line_offset: Int, beat_count: Int) {
        this.history_cache.open_multi()
        if (this.history_cache.append_undoer_key("new_line")) {
            this.history_cache.add_int(channel)
            this.history_cache.add_int(line_offset)
            for (i in 0 until beat_count) {
                var beat_key = BeatKey(channel, line_offset, i)
                this.setup_repopulate(beat_key, listOf())
            }
        }
        this.history_cache.close_multi(this.cursor.get_beatkey(), this.cursor.get_position())
    }

    fun push_remove(beat_key: BeatKey, position: MutableList<Int>) {
        if (position.isNotEmpty()) {
            if (this.history_cache.append_undoer_key("remove")) {
                position[position.size - 1] += 1

                this.history_cache.add_beatkey(beat_key)
                this.history_cache.add_position(position)
            }
        }
    }

    fun push_remove_beat(index: Int) {
        if (this.history_cache.append_undoer_key("remove_beat")) {
            this.history_cache.add_int(index)
        }
    }

    fun push_insert_beat(index: Int, channel_sizes: List<Pair<Int,Int>>) {
        this.history_cache.open_multi()
        if (this.history_cache.append_undoer_key("insert_beat")) {
            this.history_cache.add_int(index)

            for ((channel, line_count) in channel_sizes) {
                for (j in 0 until line_count) {
                    this.setup_repopulate(BeatKey(channel, j, index), listOf())
                }
            }
        }
        this.history_cache.close_multi(this.cursor.get_beatkey(), this.cursor.get_position())
    }

    fun push_set_event(beat_key: BeatKey, position: List<Int>, note: Int, relative: Boolean) {
        if (this.history_cache.append_undoer_key("set_event")) {
            this.history_cache.add_beatkey(beat_key)
            this.history_cache.add_position(position)
            this.history_cache.add_int(note)
            this.history_cache.add_boolean(relative)
        }
    }
    fun push_set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        if (this.history_cache.append_undoer_key("set_percussion_event")) {
            this.history_cache.add_beatkey(beat_key)
            this.history_cache.add_position(position)
        }
    }

    fun push_unset(beat_key: BeatKey, position: List<Int>) {
        if (this.history_cache.append_undoer_key("unset")) {
            this.history_cache.add_beatkey(beat_key)
            this.history_cache.add_position(position)
        }
    }

    fun push_remove_line(channel: Int, index: Int) {
        if (this.history_cache.append_undoer_key("remove_line")) {
            this.history_cache.add_int(channel)
            this.history_cache.add_int(index)
        }
    }

    fun push_swap_channels(channel_a: Int, channel_b: Int) {
        if (this.history_cache.append_undoer_key("swap_channels")) {
            this.history_cache.add_int(channel_a)
            this.history_cache.add_int(channel_b)
        }
    }

    fun push_split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        if (this.history_cache.append_undoer_key("split_tree")) {
            this.history_cache.add_beatkey(beat_key)
            this.history_cache.add_position(position)
            this.history_cache.add_int(splits)
        }
    }
}
