package com.qfs.radixulous.opusmanager
import com.qfs.radixulous.apres.MIDI
import com.qfs.radixulous.structure.OpusTree
import java.lang.Integer.max

class HistoryNode(var func_name: String, var args: List<Any>) {
    var children: MutableList<HistoryNode> = mutableListOf()
    var parent: HistoryNode? = null
    var location_stamp: Pair<BeatKey, List<Int>>? = null
}

class HistoryCache() {
    var history_lock = 0
    var history: MutableList<HistoryNode> = mutableListOf()
    var working_node: HistoryNode? = null

    fun isLocked(): Boolean {
        return this.history_lock != 0
    }

    fun isEmpty(): Boolean {
        return this.history.isEmpty()
    }

    fun append_undoer(func: String, args: List<Any>, beatkey: BeatKey, position: List<Int>) {
        this.append_undoer(func, args, Pair(beatkey, position))
    }

    fun append_undoer(func: String, args: List<Any>, location_stamp: Pair<BeatKey, List<Int>>? = null) {
        if (this.isLocked()) {
            return
        }

        val new_node = HistoryNode(func, args)
        if (location_stamp != null) {
            new_node.location_stamp = location_stamp
        }

        if (this.working_node != null) {
            new_node.parent = this.working_node
            this.working_node!!.children.add(new_node)
        } else {
            this.history.add(new_node)
        }
    }

    // Keep track of all history as one group
    fun <T> remember(callback: () -> T): T {

        this.open_multi()
        try {
            val output = callback()
            this.close_multi()
            return output
        } catch (e: Exception) {
            this.cancel_multi()
            throw e
        }
    }

    // Run a callback with logging history
    fun <T> forget(callback: () -> T): T {
        this.lock()
        val output = callback()
        this.unlock()
        return output
    }

    fun open_multi(beatkey: BeatKey, position: List<Int>) {
        this.open_multi(Pair(beatkey, position))
    }

    fun open_multi(location_stamp: Pair<BeatKey, List<Int>>? = null) {
        if (this.isLocked()) {
            return
        }

        val next_node = HistoryNode("multi", listOf())
        if (location_stamp != null) {
            next_node.location_stamp = location_stamp
        }

        if (this.working_node != null) {
            next_node.parent = this.working_node
            this.working_node!!.children.add(next_node)
        } else {
            this.history.add(next_node)
        }
        this.working_node = next_node
    }

    open fun close_multi() {
        if (this.isLocked()) {
            return
        }

        if (this.working_node != null) {
            this.working_node = this.working_node!!.parent
        }
    }

    open fun cancel_multi() {
        if (this.isLocked()) {
            return
        }
        this.close_multi()
        if (this.working_node != null) {
            this.working_node!!.children.removeLast()
        } else {
            this.history.removeLast()
        }
    }

    fun clear() {
        this.history.clear()
    }

    fun lock() {
        this.history_lock += 1
    }

    fun unlock() {
        this.history_lock -= 1
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

open class HistoryLayer() : LinksLayer() {
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
            "set_project_name" -> {
                this.set_project_name(current_node.args[0] as String)
            }
            "set_line_volume" -> {
                this.set_line_volume(
                    current_node.args[0] as Int,
                    current_node.args[1] as Int,
                    current_node.args[2] as Int
                )
            }
            "unlink_beat" -> {
                this.unlink_beat(current_node.args[0] as BeatKey)
            }
            "create_link_pool" -> {
                this.create_link_pool((current_node.args[0] as LinkedHashSet<BeatKey>).toList())
            }
            "set_percussion_channel" -> {
                this.set_percussion_channel(current_node.args[0] as Int)
            }
            "unset_percussion_channel" -> {
                this.unset_percussion_channel()
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
                val beatkey = current_node.args[0] as BeatKey
                val position = current_node.args[1] as List<Int>
                val tree = current_node.args[2] as OpusTree<OpusEvent>

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
            //"set_cursor" -> {
            //    val beat_key = current_node.args[0] as BeatKey
            //    val y = this.get_y(beat_key.channel, beat_key.line_offset)
            //    this.get_cursor().set(y, beat_key.beat, current_node.args[1] as List<Int>)
            //}
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

        val node = this.history_cache.pop()
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
    }


    override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        this.history_cache.remember {
            this.push_replace_tree(old_beat, listOf())
            super.overwrite_beat(old_beat, new_beat)
        }
    }

    fun new_line(channel: Int, index: Int, count: Int): List<List<OpusTree<OpusEvent>>> {
        return this.history_cache.remember {
            val output: MutableList<List<OpusTree<OpusEvent>>> = mutableListOf()
            for (i in 0 until count) {
                output.add(this.new_line(channel, index))
            }
            output
        }
    }

    override fun new_line(channel: Int, index: Int?): List<OpusTree<OpusEvent>> {
        return this.history_cache.remember {
            val output = super.new_line(channel, index)
            this.push_remove_line(channel, index ?: (this.channels[channel].size - 1))
            output
        }
    }

    override fun insert_line(channel: Int, line_index: Int, line: MutableList<OpusTree<OpusEvent>>) {
        this.history_cache.remember {
            super.insert_line(channel, line_index, line)
            this.push_remove_line(channel, line_index)
        }
    }

    fun remove_line(channel: Int, line_offset: Int, count: Int) {
        this.history_cache.remember {
            for (i in 0 until count) {
                if (this.channels[channel].size > 1) {
                    this.remove_line(channel, kotlin.math.min(line_offset, this.channels[channel].size - 1))
                } else {
                    break
                }
            }
        }
    }

    override fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        this.history_cache.remember {
            super.move_line(channel_old, line_old, channel_new, line_new)
        }
    }

    override fun remove_line(channel: Int, line_offset: Int): MutableList<OpusTree<OpusEvent>> {
        return this.history_cache.remember {
            this.push_new_line(channel, line_offset)
            super.remove_line(channel, line_offset)
        }
    }

    fun insert_after(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        this.history_cache.open_multi(beat_key, position)
        for (i in 0 until repeat) {
            this.insert_after(beat_key, position)
        }
        this.history_cache.close_multi()
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.history_cache.open_multi(beat_key, position)
        val remove_position = position.toMutableList()
        remove_position[remove_position.size - 1] += 1
        this.push_remove(beat_key, remove_position)
        super.insert_after(beat_key, position)
        this.history_cache.close_multi()
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.history_cache.open_multi(beat_key, position)
        this.push_replace_tree(beat_key, position)
        super.split_tree(beat_key, position, splits)
        this.history_cache.close_multi()
    }

    fun remove(beat_key: BeatKey, position: List<Int>, count: Int) {
        this.history_cache.open_multi(beat_key, position)
        var cancelled = false

        for (i in 0 until count) {
            // TODO: Try/catching may cause issues. check that the position is valid first
            try {
                this.remove(beat_key, position)
            } catch (e: Exception) {
                cancelled = i == 0
                break
            }
        }
        if (!cancelled) {
            this.history_cache.close_multi()
        } else {
            this.history_cache.cancel_multi()
        }
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.history_cache.open_multi(beat_key, position)
        val new_position = position.toMutableList()
        if (new_position.isNotEmpty()) {
            new_position.removeLast()
        }
        this.push_replace_tree(beat_key, new_position)
        super.remove(beat_key, position)
        this.history_cache.close_multi()
    }

    override fun insert_beat(index: Int, count: Int) {
        this.history_cache.remember {
            super.insert_beat(index, count)
        }
    }

    override fun insert_beat(index: Int) {
        this.push_remove_beat(index)
        super.insert_beat(index)
    }

    fun remove_beat(index: Int, count: Int) {
        this.history_cache.remember {
            for (i in 0 until count) {
                if (this.opus_beat_count > 1) {
                    this.remove_beat(index)
                } else {
                    break
                }
            }
        }
    }

    override fun remove_beat(index: Int) {
        this.history_cache.remember {
            this.push_insert_beat(index, this.get_channel_line_counts())
            super.remove_beat(index)
        }
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        this.history_cache.remember {
            this.push_replace_tree(beat_key, position, this.get_tree(beat_key, position).copy())
            super.replace_tree(beat_key, position, tree)
        }
    }

    override fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        this.history_cache.remember {
            super.move_leaf(beatkey_from, position_from, beatkey_to, position_to)
        }
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.history_cache.remember {
            val tree = this.get_tree(beat_key, position).copy()
            super.set_event(beat_key, position, event)
            this.push_replace_tree(beat_key, position, tree)
        }
    }

    override fun unset_percussion_channel() {
        this.history_cache.remember {
            if (this.percussion_channel != null) {
                this.history_cache.append_undoer("set_percussion_channel",
                    listOf(this.percussion_channel!!))
            }
            super.unset_percussion_channel()
        }
    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this.history_cache.remember {
            val tree = this.get_tree(beat_key, position)
            if (tree.is_event()) {
                this.push_set_percussion_event(beat_key, position)
            } else {
                this.push_unset(beat_key, position)
            }
            super.set_percussion_event(beat_key, position)
        }
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.history_cache.remember {
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
            } else if (!tree.is_leaf()) {
                this.push_replace_tree(beat_key, position, tree.copy())
            }
            super.unset(beat_key, position)
        }
    }

    override fun load(path: String) {
        this.history_cache.forget {
            super.load(path)
        }
    }

    override fun new() {
        this.history_cache.forget {
            super.new()
        }
    }

    override fun import_midi(midi: MIDI) {
        this.history_cache.forget {
            super.import_midi(midi)
        }
    }

    fun import_midi(midi: MIDI, path: String, title: String) {
        this.history_cache.forget {
            this.import_midi(midi)
            this.path = path
            this.set_project_name(title)
        }
    }

    override fun clear() {
        this.history_cache.clear()
        this.save_point_popped = false
        super.clear()
    }

    fun push_replace_tree(beatkey: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>? = null) {
        if (!this.history_cache.isLocked()) {
            val use_tree = tree ?: this.get_tree(beatkey, position).copy()
            this.history_cache.append_undoer("replace_tree", listOf(beatkey.copy(), position.toList(), use_tree))
        }
    }

    fun push_new_channel(channel: Int) {
        this.history_cache.remember {
            for (i in this.channels[channel].size - 1 downTo 0) {
                this.push_new_line(channel, i)
            }

            this.history_cache.append_undoer("new_channel", listOf(channel))
        }

    }

    fun push_new_line(channel: Int, line_offset: Int) {
        this.history_cache.remember {
            for (i in this.opus_beat_count - 1 downTo 0) {
                val beat_key = BeatKey(channel, line_offset, i)
                this.push_replace_tree(beat_key, listOf())
            }

            this.history_cache.append_undoer("new_line", listOf(channel, line_offset))
        }

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
        this.history_cache.remember {

            for (channel in channel_sizes.indices) {
                val line_count = channel_sizes[channel]
                for (j in 0 until line_count) {
                    this.push_replace_tree(BeatKey(channel, j, index), listOf())
                }
            }

            this.history_cache.append_undoer("insert_beat", listOf(index))
        }
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
        this.history_cache.remember {
            super.link_beats(beat_key, target)
        }
    }

    override fun merge_link_pools(index_first: Int, index_second: Int) {
        this.history_cache.remember {

            val old_link_pool = mutableSetOf<BeatKey>()
            for (beat_key in this.link_pools[index_first]) {
                old_link_pool.add(beat_key.copy())
            }
            this.history_cache.append_undoer("create_link_pool", listOf(old_link_pool))
            for (beat_key in this.link_pools[index_first]) {
                this.history_cache.append_undoer("unlink_beat", listOf(beat_key))
            }

            super.merge_link_pools(index_first, index_second)
        }
    }

    override fun link_beat_into_pool(beat_key: BeatKey, index: Int, overwrite_pool: Boolean) {
        this.history_cache.remember {
            super.link_beat_into_pool(beat_key, index, overwrite_pool)
            this.history_cache.append_undoer("unlink_beat", listOf(beat_key))
        }
    }

    override fun create_link_pool(beat_keys: List<BeatKey>) {
        this.history_cache.remember {
            for (beat_key in beat_keys) {
                this.history_cache.append_undoer("unlink_beat", listOf(beat_key))
            }
            super.create_link_pool(beat_keys)
        }
    }

    override fun batch_link_beats(beat_key_pairs: List<Pair<BeatKey, BeatKey>>) {
        this.history_cache.remember {
            super.batch_link_beats(beat_key_pairs)
        }
    }

    override fun remove_channel(channel: Int) {
        this.push_new_channel(channel)
        this.history_cache.lock()
        super.remove_channel(channel)
        this.history_cache.unlock()
    }

    override fun new_channel(channel: Int?, lines: Int) {
        this.history_cache.remember {
            super.new_channel(channel, lines)
            if (channel != null) {
                this.push_remove_channel(channel)
            } else {
                this.push_remove_channel(this.channels.size - 1)
            }
        }
    }

    override fun set_percussion_channel(channel: Int) {
        this.history_cache.remember {
            if (this.percussion_channel == null) {
                this.history_cache.append_undoer("unset_percussion_channel", listOf())
            } else {
                this.history_cache.append_undoer("set_percussion_channel",
                    listOf(this.percussion_channel!!))
            }
            super.set_percussion_channel(channel)
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
        val node = this.history_cache.peek()
        return (this.save_point_popped || (node != null && node.func_name != "save_point"))
    }

    override fun set_line_volume(channel: Int, line_offset: Int, volume: Int) {
        val current_volume = this.get_line_volume(channel, line_offset)
        this.history_cache.append_undoer("set_line_volume", listOf(channel, line_offset, current_volume))
        super.set_line_volume(channel, line_offset, volume)
    }

    override fun set_project_name(new_name: String) {
        this.history_cache.append_undoer("set_project_name", listOf(this.project_name))
        super.set_project_name(new_name)
    }
}
