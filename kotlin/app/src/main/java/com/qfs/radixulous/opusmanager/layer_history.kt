package com.qfs.radixulous.opusmanager
import com.qfs.radixulous.structure.OpusTree
import java.lang.Integer.max

class HistoryNode(var func_name: String, var args: List<Any>) {
    var children: MutableList<HistoryNode> = mutableListOf()
    var parent: HistoryNode? = null
}

class HistoryCache() {
    var history_locked = false
    var multi_counter: Int = 0
    var history: MutableList<HistoryNode> = mutableListOf()
    var working_node: HistoryNode? = null

    fun isLocked(): Boolean {
        return this.history_locked
    }

    fun isEmpty(): Boolean {
        return this.history.isEmpty()
    }

    fun append_undoer(func: String, args: List<Any>) {
        if (this.history_locked) {
            return
        }

        if (this.working_node != null) {
            var new_node = HistoryNode(func, args)
            new_node.parent = this.working_node
            this.working_node!!.children.add(new_node)
        } else {
            this.history.add(HistoryNode(func, args))
        }

    }

    fun open_multi() {
        if (this.history_locked) {
            return
        }
        var next_node = HistoryNode("multi${this.history.size}", listOf())
        if (this.working_node != null) {
            next_node.parent = this.working_node
            this.working_node!!.children.add(next_node)
        } else {
            this.history.add(next_node)
        }
        this.working_node = next_node
    }

    open fun close_multi() {
        if (this.history_locked) {
            return
        }

        if (this.working_node != null) {
            this.working_node = this.working_node!!.parent
        }
    }

    open fun close_multi(beat_key: BeatKey, position: List<Int>) {
        if (this.history_locked) {
            return
        }
        if (this.working_node != null) {
            if (this.working_node!!.parent == null) {
                this.append_undoer("set_cursor", listOf(beat_key.copy(), position.toList()))
            }
            this.working_node = this.working_node!!.parent
        }
    }

    open fun cancel_multi() {
        if (this.history_locked) {
            return
        }

        if (this.working_node != null) {
            this.working_node = this.working_node!!.parent
        }
    }

    fun clear() {
        this.history.clear()
        this.history_locked = false
        this.multi_counter = 0
    }

    fun lock(): Boolean {
        var was_locked = this.history_locked
        this.history_locked = true
        return was_locked
    }
    fun unlock() {
        this.history_locked = false
    }
    fun pop(): HistoryNode? {
        if (this.history.isEmpty()) {
            return null
        } else {
            return this.history.removeLast()
        }
    }
}

open class HistoryLayer() : CursorLayer() {
    var history_cache = HistoryCache()

    private fun apply_history_node(current_node: HistoryNode, depth: Int = 0) {
        when (current_node.func_name) {
            "split_tree" -> {
                this.split_tree(
                    current_node.args[0] as BeatKey,
                    current_node.args[1] as List<Int>,
                    current_node.args[2] as Int
                )
            }
            "unlink_beats" -> {
                this.unlink_beat(current_node.args[0] as BeatKey)
            }
            "set_event" -> {
                this.set_event(
                    current_node.args[0] as BeatKey,
                    current_node.args[1] as List<Int>,
                    current_node.args[2] as OpusEvent
                )
            }
            "set_percussion_event" -> {
                this.set_percussion_event(
                    current_node.args[0] as BeatKey,
                    current_node.args[1] as List<Int>
                )
            }
            "unset" -> {
                this.unset(
                    current_node.args[0] as BeatKey,
                    current_node.args[1] as List<Int>
                )
            }

            "replace_tree" -> {
                var beatkey = current_node.args[0] as BeatKey
                var position = current_node.args[1] as List<Int>
                var tree = current_node.args[2] as OpusTree<OpusEvent>

                this.replace_tree( beatkey, position, tree )
            }

            "remove_line" -> {
                this.remove_line(
                    current_node.args[0] as Int,
                    current_node.args[1] as Int
                )
            }
            "new_line" -> {
                this.new_line(
                    current_node.args[0] as Int,
                    current_node.args[1] as Int
                )
            }
            "remove_channel" -> {
                this.remove_channel_by_uuid(current_node.args[0] as Int)
            }
            "new_channel" -> {
                this.new_channel(current_node.args[0] as Int)
            }
            "remove" -> {
                this.remove(
                    current_node.args[0] as BeatKey,
                    current_node.args[1] as List<Int>
                )
            }
            "remove_beat" -> {
                this.remove_beat(current_node.args[0] as Int)
            }
            "insert_beat" -> {
                this.insert_beat(current_node.args[0] as Int)
            }
            "set_cursor" -> {
                val beat_key = current_node.args[0] as BeatKey
                val y = this.get_y(beat_key.channel, beat_key.line_offset)
                this.get_cursor().set(y, beat_key.beat, current_node.args[1] as List<Int>)
            }
            else -> {}
        }
        for (child in current_node.children) {
            this.apply_history_node(child, depth + 1)
        }
    }
    open fun apply_undo() {
        this.history_cache.lock()

        var node = this.history_cache.pop()
        if (node == null) {
            this.history_cache.unlock()
            return
        }

        this.apply_history_node(node)

        this.history_cache.unlock()
        this.get_cursor().settle()
    }


    override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        this.push_replace_tree(old_beat, listOf())
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
        this.history_cache.open_multi()
        this.push_new_line(channel, line_offset)
        super.remove_line(channel, line_offset)
        this.history_cache.close_multi()
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
        this.push_replace_tree(beat_key, position)
        var was_locked = this.history_cache.lock()
        super.split_tree(beat_key, position, splits)
        if (!was_locked) {
            this.history_cache.unlock()
        }
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
        var new_position = position.toMutableList()
        if (new_position.isNotEmpty()) {
            new_position.removeLast()
        }
        this.push_replace_tree(beat_key, new_position)
        super.remove(beat_key, position)
    }

    override fun insert_beat(index: Int, count: Int) {
        val initial_beatkey = this.get_cursor().get_beatkey()
        val initial_position = this.get_cursor().get_position()

        this.history_cache.open_multi()

        for (i in 0 until count) {
            this.push_remove_beat(index)
        }

        super.insert_beat(index, count)

        this.history_cache.close_multi(initial_beatkey, initial_position)
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
        var tree = this.get_tree(beat_key, position).copy()
        try {
            super.set_event(beat_key, position, event)
            this.push_replace_tree(beat_key, position, tree)
        } catch (e: Exception) {
            throw e
        }

    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            this.push_set_percussion_event(beat_key, position)
        } else {
            this.push_unset(beat_key, position)
        }

        try {
            super.set_percussion_event(beat_key, position)
        } catch (e: Exception) {
            this.history_cache.pop()
            throw e
        }

    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            val original_event = tree.get_event()!!
            if (!this.is_percussion(beat_key.channel)) {
                this.push_set_event(
                    beat_key,
                    position,
                    original_event
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

    fun push_replace_tree(beatkey: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>? = null) {
        if (!this.history_cache.isLocked()) {
            var use_tree = tree ?: this.get_tree(beatkey, position).copy()
            this.history_cache.append_undoer("replace_tree", listOf(beatkey.copy(), position.toList(), use_tree))
        }
    }

    fun push_new_channel(channel: Int) {
        this.history_cache.open_multi()


        this.history_cache.append_undoer("new_channel", listOf(channel))

        for (i in 0 until this.channels[channel].size) {
            this.push_new_line(channel, i)
        }


        this.history_cache.close_multi(
            this.get_cursor().get_beatkey(),
            this.get_cursor().get_position()
        )
    }

    fun push_new_line(channel: Int, line_offset: Int) {
        this.history_cache.open_multi()

        this.history_cache.append_undoer("new_line", listOf(channel, line_offset))

        for (i in 0 until this.opus_beat_count) {
            val beat_key = BeatKey(channel, line_offset, i)
            this.push_replace_tree(beat_key, listOf())
        }

        this.history_cache.close_multi()
    }

    fun push_remove(beat_key: BeatKey, position: MutableList<Int>) {
        if (position.isNotEmpty()) {
            this.history_cache.append_undoer("remove", listOf(beat_key.copy(), position.toList()))
            position[position.size - 1] += 1
        }
    }

    fun push_remove_beat(index: Int) {
        this.history_cache.append_undoer("remove_beat", listOf(index))
    }

    fun push_insert_beat(index: Int, channel_sizes: List<Int>) {
        this.history_cache.open_multi()

        this.history_cache.append_undoer("insert_beat", listOf(index))

        for (channel in channel_sizes.indices) {
            val line_count = channel_sizes[channel]
            for (j in 0 until line_count) {
                this.push_replace_tree(BeatKey(channel, j, index), listOf())
            }
        }


        this.history_cache.close_multi(this.get_cursor().get_beatkey(), this.get_cursor().get_position())
    }

    fun push_set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.history_cache.append_undoer("set_event", listOf(beat_key.copy(), position, event.copy()))
    }

    fun push_set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this.history_cache.append_undoer("set_percussion_event", listOf(beat_key.copy(), position.toList()))
    }

    fun push_unset(beat_key: BeatKey, position: List<Int>) {
        this.history_cache.append_undoer("unset", listOf(beat_key.copy(), position.toList()))
    }

    fun push_remove_channel(channel: Int) {
        this.history_cache.append_undoer("remove_channel", listOf(this.channels[channel].uuid))
    }

    fun push_remove_line(channel: Int, index: Int) {
        this.history_cache.append_undoer("remove_line", listOf(channel, index))
    }

    fun push_split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.history_cache.append_undoer("split_tree", listOf(beat_key.copy(), position.toList(), splits))
    }

    fun has_history(): Boolean {
        return ! this.history_cache.isEmpty()
    }

    override fun link_beats(beat_key: BeatKey, target: BeatKey) {
        this.history_cache.open_multi()

        this.history_cache.append_undoer("unlink_beats", listOf(beat_key.copy(), target.copy()))
        this.push_replace_tree(beat_key, listOf())

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
        var was_locked = this.history_cache.lock()
        super.remove_channel(channel)
        if (!was_locked) {
            this.history_cache.unlock()
        }
    }

    override fun new_channel(channel: Int?) {

        super.new_channel(channel)
        if (channel != null) {
            this.push_remove_channel(channel)
        } else {
            this.push_remove_channel(this.channels.size - 1)
        }

    }
}
