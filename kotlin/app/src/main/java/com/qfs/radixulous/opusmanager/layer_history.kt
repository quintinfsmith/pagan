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


    fun get_position(): List<Int> {
        // List<Int> prefaced by length
        val position: MutableList<Int> = mutableListOf()
        val length = this.get_int()
        for (_i in 0 until length) {
            position.add(0, this.get_int())
        }
        return position
    }

    fun add_position(position: List<Int>) {
        if (position.isEmpty()) {
            this.int_stack.add(0)
        } else {
            val size = position.size
            for (i in position) {
                this.int_stack.add(i)
            }
            this.int_stack.add(size)
        }
    }

    fun get_beatkey(): BeatKey {
        val channel = this.get_int()
        val line_offset = this.get_int()
        val beat = this.get_int()
        return BeatKey(channel, line_offset, beat)
    }

    fun add_beatkey(beat_key: BeatKey) {
        this.int_stack.add(beat_key.beat)
        this.int_stack.add(beat_key.line_offset)
        this.int_stack.add(beat_key.channel)
    }

    fun get_boolean(): Boolean {
        return this.get_int() != 0
    }

    fun add_boolean(bool: Boolean) {
        if (bool) {
            this.int_stack.add(1)
        } else {
            this.int_stack.add(0)
        }
    }

    fun get_int(): Int {
        return this.int_stack.removeLast()
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

    open fun apply_undo() {
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
                "unlink_beats" -> {
                    val beatkey = this.history_cache.get_beatkey()
                    this.unlink_beat(beatkey)
                }
                "set_event" -> {
                    val relative = this.history_cache.get_boolean()
                    val value = this.history_cache.get_int()
                    val position = this.history_cache.get_position()
                    val beat_key = this.history_cache.get_beatkey()
                    val event = OpusEvent(value, this.RADIX, beat_key.channel, relative)
                    this.set_event(beat_key, position, event)
                }
                "set_percussion_event" -> {
                    val position = this.history_cache.get_position()
                    val beat_key = this.history_cache.get_beatkey()
                    this.set_percussion_event(beat_key, position)
                }
                "unset" -> {
                    val position = this.history_cache.get_position()
                    val beat_key = this.history_cache.get_beatkey()
                    this.unset(beat_key, position)
                }
                "replace_beat" -> {
                    val beat = this.history_cache.get_beat()
                    val beat_key = this.history_cache.get_beatkey()
                    this.replace_beat_tree(beat_key, beat)
                }
                "remove_line" -> {
                    val line_offset = this.history_cache.get_int()
                    val channel = this.history_cache.get_int()
                    this.remove_line(channel, line_offset)
                }
                "new_line" -> {
                    val line_offset = this.history_cache.get_int()
                    val channel = this.history_cache.get_int()
                    this.new_line(channel, line_offset)
                }
                "remove_channel" -> {
                    val channel = this.history_cache.get_int()
                    this.remove_channel(channel)
                }
                "new_channel" -> {
                    val channel = this.history_cache.get_int()
                    this.new_channel(channel)
                }
                "remove" -> {
                    val position = this.history_cache.get_position()
                    val beat_key = this.history_cache.get_beatkey()
                    this.remove(beat_key, position)
                }
                "remove_beat" -> {
                    val index = this.history_cache.get_int()
                    this.remove_beat(index)
                }
                "insert_beat" -> {
                    val index = this.history_cache.get_int()
                    this.insert_beat(index)
                }
                "set_cursor" -> {
                    val position = this.history_cache.get_position()
                    val beat_key = this.history_cache.get_beatkey()
                    val y = this.get_y(beat_key.channel, beat_key.line_offset)
                    this.get_cursor().set(y, beat_key.beat, position)
                }
            }
        }

        this.history_cache.unlock()
        this.get_cursor().settle()
    }

    private fun setup_repopulate(beat_key: BeatKey, start_position: List<Int>) {
        if (this.history_cache.isLocked()) {
            return
        }
        this.history_cache.open_multi()

        val beat_tree = this.channels[beat_key.channel].get_tree(beat_key.line_offset, beat_key.beat)
        val stack: MutableList<List<Int>> = mutableListOf(start_position)

        val splits: MutableList<Pair<List<Int>, Int>> = mutableListOf()
        val events: MutableList<Pair<List<Int>, OpusTree<OpusEvent>>> = mutableListOf()
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
                splits.add(0, Pair(position, tree.size))
            } else if (tree.is_event()) {
                events.add(0, Pair(position, tree))
            }
        }

        for ((position, tree) in events) {
            val event = tree.get_event()!!
            if (!this.is_percussion(beat_key.channel)) {
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

        for ((position, size) in splits) {
            this.push_split_tree(beat_key, position, size)
        }


        this.history_cache.close_multi(this.get_cursor().get_beatkey(), this.get_cursor().get_position())
    }

    override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        this.setup_repopulate(old_beat, listOf())
        super.overwrite_beat(old_beat, new_beat)
    }

    fun new_line(channel: Int, index: Int, count: Int): List<List<OpusTree<OpusEvent>>> {
        val initial_beatkey = this.get_cursor().get_beatkey()
        val initial_position = this.get_cursor().get_position()
        this.history_cache.open_multi()

        val output: MutableList<List<OpusTree<OpusEvent>>> = mutableListOf()
        for (i in 0 until  count) {
           output.add(this.new_line(channel, index))
        }

        this.history_cache.close_multi(initial_beatkey, initial_position)

        return output
    }
    override fun new_line(channel: Int, index: Int?): List<OpusTree<OpusEvent>> {
        val output = super.new_line(channel, index)
        this.push_remove_line(channel, index ?: (this.channels[channel].size - 1))
        return output
    }

    fun remove_line(channel: Int, line_offset: Int, count: Int) {
        val initial_beatkey = this.get_cursor().get_beatkey()
        val initial_position = this.get_cursor().get_position()
        this.history_cache.open_multi()
        for (i in 0 until count) {
            if (this.channels[channel].size > 1) {
                this.remove_line(channel, kotlin.math.min(line_offset, this.channels[channel].size - 1))
            } else {
                break
            }
        }

        this.history_cache.close_multi(initial_beatkey, initial_position)
    }

    override fun remove_line(channel: Int, line_offset: Int) {
        this.push_new_line(channel, line_offset)
        super.remove_line(channel, line_offset)
    }

    fun insert_after(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        this.history_cache.open_multi()
        for (i in 0 until repeat) {
            this.insert_after(beat_key, position)
        }
        this.history_cache.close_multi(beat_key, position)
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.push_remove(beat_key, position.toMutableList())
        super.insert_after(beat_key, position)
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.history_cache.open_multi()

        val tree: OpusTree<OpusEvent> = this.get_tree(beat_key, position)
        this.push_split_tree(beat_key, position, 1)

        if (tree.is_event()) {
            var event = tree.get_event()!!
            this.push_set_event(beat_key, position, event.note, event.relative)
        }


        super.split_tree(beat_key, position, splits)
        this.history_cache.close_multi(beat_key, position)
    }

    fun remove(beat_key: BeatKey, position: List<Int>, count: Int) {
        this.history_cache.open_multi()
        for (i in 0 until count) {
            // TODO: Try/catching may cause issues. check that the position is valid first
            try {
                this.remove(beat_key, position)
            } catch (e: Exception) {
                break
            }
        }
        this.history_cache.close_multi(beat_key, position)
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.setup_repopulate(beat_key, listOf())
        super.remove(beat_key, position)
    }

    fun insert_beat(index: Int, count: Int) {
        val initial_beatkey = this.get_cursor().get_beatkey()
        val initial_position = this.get_cursor().get_position()
        this.history_cache.open_multi()

        for (i in 0 until count) {
            this.insert_beat(index + i)
        }

        this.history_cache.close_multi(initial_beatkey, initial_position)
    }

    override fun insert_beat(index: Int?) {
        this.push_remove_beat(index ?: (this.opus_beat_count - 1))

        super.insert_beat(index)
    }

    fun remove_beat(index: Int, count: Int) {
        val initial_beatkey = this.get_cursor().get_beatkey()
        val initial_position = this.get_cursor().get_position()
        this.history_cache.open_multi()

        for (i in 0 until count) {
            if (this.opus_beat_count > 1) {
                this.remove_beat(index)
            } else {
                break
            }
        }

        this.history_cache.close_multi(initial_beatkey, initial_position)
    }
    override fun remove_beat(index: Int) {
        this.push_insert_beat(index, this.get_channel_line_counts())
        super.remove_beat(index)
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.history_cache.open_multi()
        val tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            val original_event = tree.get_event()!!
            this.push_set_event(beat_key, position, original_event.note, original_event.relative)
        } else {
            this.push_unset(beat_key, position)
        }

        super.set_event(beat_key, position, event)
        this.history_cache.close_multi(beat_key, position)
    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)

        if (tree.is_event()) {
            this.push_set_percussion_event(beat_key, position)
        } else {
            this.push_unset(beat_key, position)
        }

        super.set_percussion_event(beat_key, position)
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            val original_event = tree.get_event()!!
            if (!this.is_percussion(beat_key.channel)) {
                this.push_set_event(
                    beat_key,
                    position,
                    original_event.note,
                    original_event.relative
                )
            } else {
                this.push_set_percussion_event(beat_key, position)
            }
        }
        super.unset(beat_key, position)
    }

    override fun load(path: String) {
        super.load(path)
        this.history_cache.clear()
    }

    override fun new() {
        super.new()
        this.history_cache.clear()
    }

    fun push_new_channel(channel: Int) {
        this.history_cache.open_multi()
        for (i in 0 until this.channels[channel].size) {
            this.push_new_line(channel, i)
        }
        this.history_cache.append_undoer_key("new_channel")
        this.history_cache.add_int(channel)
        this.history_cache.close_multi(
            this.get_cursor().get_beatkey(),
            this.get_cursor().get_position()
        )
    }

    fun push_new_line(channel: Int, line_offset: Int) {
        this.history_cache.open_multi()

        for (i in 0 until this.opus_beat_count) {
            val beat_key = BeatKey(channel, line_offset, i)
            this.setup_repopulate(beat_key, listOf())
        }

        this.history_cache.append_undoer_key("new_line")
        this.history_cache.add_int(channel)
        this.history_cache.add_int(line_offset)

        this.history_cache.close_multi(
            this.get_cursor().get_beatkey(),
            this.get_cursor().get_position()
        )
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

    fun push_insert_beat(index: Int, channel_sizes: List<Int>) {
        this.history_cache.open_multi()
        for (channel in channel_sizes.indices) {
            val line_count = channel_sizes[channel]
            for (j in 0 until line_count) {
                this.setup_repopulate(BeatKey(channel, j, index), listOf())
            }
        }
        if (this.history_cache.append_undoer_key("insert_beat")) {
            this.history_cache.add_int(index)
        }
        this.history_cache.close_multi(this.get_cursor().get_beatkey(), this.get_cursor().get_position())
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

    fun push_remove_channel(channel: Int) {
        if (this.history_cache.append_undoer_key("remove_channel")) {
            this.history_cache.add_int(channel)
        }
    }

    fun push_remove_line(channel: Int, index: Int) {
        if (this.history_cache.append_undoer_key("remove_line")) {
            this.history_cache.add_int(channel)
            this.history_cache.add_int(index)
        }
    }

    fun push_split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        if (this.history_cache.append_undoer_key("split_tree")) {
            this.history_cache.add_beatkey(beat_key)
            this.history_cache.add_position(position)
            this.history_cache.add_int(splits)
        }
    }

    fun has_history(): Boolean {
        return ! this.history_cache.isEmpty()
    }

    override fun link_beats(beat_key: BeatKey, target: BeatKey) {
        this.history_cache.open_multi()

        if (this.history_cache.append_undoer_key("unlink_beats")) {
            this.history_cache.add_beatkey(beat_key)
            this.setup_repopulate(beat_key, listOf())
        }

        this.history_cache.close_multi(
            this.get_cursor().get_beatkey(),
            this.get_cursor().get_position()
        )

        super.link_beats(beat_key, target)
    }

    override fun link_beat_range(beat: BeatKey, target_a: BeatKey, target_b: BeatKey) {
        this.history_cache.open_multi()

        super.link_beat_range(beat, target_a, target_b)

        this.history_cache.close_multi(
            this.get_cursor().get_beatkey(),
            this.get_cursor().get_position()
        )
    }

    override fun remove_channel(channel: Int) {
        this.push_new_channel(channel)
        super.remove_channel(channel)
    }

    override fun new_channel(channel: Int?) {
        this.push_remove_channel(this.channels.size)
        super.new_channel(channel)
    }
}
