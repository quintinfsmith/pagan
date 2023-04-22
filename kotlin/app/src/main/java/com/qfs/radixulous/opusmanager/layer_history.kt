package com.qfs.radixulous.opusmanager
import android.util.Log
import com.qfs.radixulous.apres.MIDI
import com.qfs.radixulous.structure.OpusTree
import java.lang.Integer.max

class HistoryNode(var func_name: String, var args: List<Any>) {
    var children: MutableList<HistoryNode> = mutableListOf()
    var parent: HistoryNode? = null
}

class HistoryCache() {
    var history_locked = false
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

        var next_node = HistoryNode("multi", listOf())

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
                //this.append_undoer("set_cursor", listOf(beat_key.copy(), position.toList()))
            }
            this.close_multi()
        }
    }

    open fun cancel_multi() {
        this.close_multi()
        if (this.working_node != null) {
            this.working_node!!.children.removeLast()
        }
    }

    fun clear() {
        this.history.clear()
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
        return if (this.history.isEmpty()) {
            null
        } else {
            this.history.removeLast()
        }
    }

    fun peek(): HistoryNode? {
        return if (this.history.isEmpty()) {
            null
        } else {
            this.history.last()
        }
    }
}

open class HistoryLayer() : CursorLayer() {
    var history_cache = HistoryCache()
    var save_point_popped = false

    private fun apply_history_node(current_node: HistoryNode, depth: Int = 0) {
        when (current_node.func_name) {
            "split_tree" -> {
                this.split_tree(
                    current_node.args[0] as BeatKey,
                    current_node.args[1] as List<Int>,
                    current_node.args[2] as Int
                )
            }
            "unlink_beat" -> {
                this.unlink_beat(current_node.args[0] as BeatKey)
            }
            "create_link_pool" -> {
                this.create_link_pool((current_node.args[0] as LinkedHashSet<BeatKey>).toList())
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

                this.replace_tree(beatkey, position, tree)
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

        if (current_node.children.isNotEmpty()) {
            current_node.children.asReversed().forEach { child: HistoryNode ->
                this.apply_history_node(child, depth + 1)
            }
        }
    }

    open fun apply_undo() {
        this.history_cache.lock()

        var node = this.history_cache.pop()
        if (node == null) {
            this.history_cache.unlock()
            return
        }

        // Skip special case "save_point"
        if (node.func_name == "save_point") {
            this.save_point_popped = true
            this.history_cache.unlock()
            this.apply_undo()
            return
        }

        this.apply_history_node(node)

        this.history_cache.unlock()
        this.get_cursor().settle()
    }


    override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        this.history_cache.open_multi()
        this.push_replace_tree(old_beat, listOf())
        super.overwrite_beat(old_beat, new_beat)
        this.history_cache.close_multi()
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
        this.history_cache.open_multi()
        val output = super.new_line(channel, index)
        this.push_remove_line(channel, index ?: (this.channels[channel].size - 1))
        this.history_cache.close_multi()
        return output
    }

    override fun insert_line(channel: Int, line_index: Int, line: MutableList<OpusTree<OpusEvent>>) {
        this.history_cache.open_multi()
        super.insert_line(channel, line_index, line)
        this.push_remove_line(channel, line_index)
        this.history_cache.close_multi()
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

    override fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        this.history_cache.open_multi()
        super.move_line(channel_old, line_old, channel_new, line_new)
        this.history_cache.close_multi()
    }

    override fun remove_line(channel: Int, line_offset: Int): MutableList<OpusTree<OpusEvent>> {
        this.history_cache.open_multi()
        this.push_new_line(channel, line_offset)
        val output = super.remove_line(channel, line_offset)

        this.history_cache.close_multi()
        return output
    }

    fun insert_after(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        this.history_cache.open_multi()
        for (i in 0 until repeat) {
            this.insert_after(beat_key, position)
        }
        this.history_cache.close_multi(beat_key, position)
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.history_cache.open_multi()
        this.push_remove(beat_key, position.toMutableList())
        super.insert_after(beat_key, position)
        this.history_cache.close_multi(beat_key, position)
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.history_cache.open_multi()
        this.push_replace_tree(beat_key, position)
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
        this.history_cache.open_multi()
        var new_position = position.toMutableList()
        if (new_position.isNotEmpty()) {
            new_position.removeLast()
        }
        this.push_replace_tree(beat_key, new_position)
        super.remove(beat_key, position)
        this.history_cache.close_multi(beat_key, position)
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
        this.history_cache.open_multi()
        this.push_insert_beat(index, this.get_channel_line_counts())
        super.remove_beat(index)
        this.history_cache.close_multi()
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        this.history_cache.open_multi()
        this.push_replace_tree(beat_key, position, this.get_tree(beat_key, position).copy())
        super.replace_tree(beat_key, position, tree)
        this.history_cache.close_multi()
    }

    override fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        this.history_cache.open_multi()
        super.move_leaf(beatkey_from, position_from, beatkey_to, position_to)
        this.history_cache.close_multi()

    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        var tree = this.get_tree(beat_key, position).copy()
        this.history_cache.open_multi()
        try {
            super.set_event(beat_key, position, event)
            this.push_replace_tree(beat_key, position, tree)
        } catch (e: Exception) {
            this.history_cache.cancel_multi()
            throw e
        }
        this.history_cache.close_multi()
    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        this.history_cache.open_multi()
        if (tree.is_event()) {
            this.push_set_percussion_event(beat_key, position)
        } else {
            this.push_unset(beat_key, position)
        }

        try {
            super.set_percussion_event(beat_key, position)
        } catch (e: Exception) {
            this.history_cache.cancel_multi()
            throw e
        }
        this.history_cache.close_multi()
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.history_cache.open_multi()
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
        this.history_cache.close_multi()
    }

    override fun load(path: String) {
        this.history_cache.lock()
        super.load(path)
        this.history_cache.unlock()
    }

    override fun new() {
        this.history_cache.lock()
        super.new()
        this.history_cache.unlock()
    }

    override fun import_midi(midi: MIDI) {
        this.history_cache.lock()
        super.import_midi(midi)
        this.history_cache.unlock()
    }

    override fun clear() {
        this.history_cache.clear()
        this.save_point_popped = false
        super.clear()
    }

    fun push_replace_tree(beatkey: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>? = null) {
        if (!this.history_cache.isLocked()) {
            var use_tree = tree ?: this.get_tree(beatkey, position).copy()
            this.history_cache.append_undoer("replace_tree", listOf(beatkey.copy(), position.toList(), use_tree))
        }
    }

    fun push_new_channel(channel: Int) {
        this.history_cache.open_multi()


        for (i in 0 until this.channels[channel].size) {
            this.push_new_line(channel, i)
        }

        this.history_cache.append_undoer("new_channel", listOf(channel))

        this.history_cache.close_multi(
            this.get_cursor().get_beatkey(),
            this.get_cursor().get_position()
        )
    }

    fun push_new_line(channel: Int, line_offset: Int) {
        this.history_cache.open_multi()

        for (i in this.opus_beat_count - 1 downTo 0) {
            val beat_key = BeatKey(channel, line_offset, i)
            this.push_replace_tree(beat_key, listOf())
        }

        this.history_cache.append_undoer("new_line", listOf(channel, line_offset))

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

        for (channel in channel_sizes.indices) {
            val line_count = channel_sizes[channel]
            for (j in 0 until line_count) {
                this.push_replace_tree(BeatKey(channel, j, index), listOf())
            }
        }

        this.history_cache.append_undoer("insert_beat", listOf(index))
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
        try {
            super.link_beats(beat_key, target)
        } catch (e: Exception) {
            this.history_cache.cancel_multi()
            throw e
        }
        this.history_cache.close_multi()
    }

    override fun merge_link_pools(index_first: Int, index_second: Int) {
        this.history_cache.open_multi()

        var old_link_pool = mutableSetOf<BeatKey>()
        for (beat_key in this.link_pools[index_first]) {
            old_link_pool.add(beat_key.copy())
        }
        this.history_cache.append_undoer("create_link_pool", listOf(old_link_pool))
        for (beat_key in this.link_pools[index_first]) {
            this.history_cache.append_undoer("unlink_beat", listOf(beat_key))
        }

        super.merge_link_pools(index_first, index_second)

        this.history_cache.close_multi()
    }

    override fun link_beat_into_pool(beat_key: BeatKey, index: Int, overwrite_pool: Boolean) {
        this.history_cache.open_multi()
        // TODO: I just forgot to finish this.
        if (overwrite_pool) {

        } else {

        }
        super.link_beat_into_pool(beat_key, index, overwrite_pool)
        this.history_cache.append_undoer("unlink_beat", listOf(beat_key))
        this.history_cache.close_multi()
    }

    override fun link_beat_range(beat: BeatKey, target_a: BeatKey, target_b: BeatKey) {
        this.history_cache.open_multi()

        super.link_beat_range(beat, target_a, target_b)

        this.history_cache.close_multi(
            this.get_cursor().get_beatkey(),
            this.get_cursor().get_position()
        )
    }

    override fun create_link_pool(beat_keys: List<BeatKey>) {
        this.history_cache.open_multi()
        for (beat_key in beat_keys) {
            this.history_cache.append_undoer("unlink_beat", listOf(beat_key))
        }
        super.create_link_pool(beat_keys)
        this.history_cache.close_multi()
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

    override fun save(path: String?) {
        super.save(path)
        this.save_point_popped = false
        if (this.has_changed_since_save()) {
            this.history_cache.append_undoer("save_point", listOf())
        }
    }

    fun has_changed_since_save(): Boolean {
        var node = this.history_cache.peek()
        return (this.save_point_popped || (node != null && node.func_name != "save_point"))
    }
}
