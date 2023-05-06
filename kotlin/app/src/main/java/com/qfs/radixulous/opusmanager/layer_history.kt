package com.qfs.radixulous.opusmanager
import android.util.Log
import com.qfs.radixulous.apres.MIDI
import com.qfs.radixulous.structure.OpusTree

class HistoryNode(var func_name: String, var args: List<Any>) {
    var children: MutableList<HistoryNode> = mutableListOf()
    var parent: HistoryNode? = null
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

    fun append_undoer(func: String, args: List<Any>) {
        if (this.isLocked()) {
            return
        }

        val new_node = HistoryNode(func, args)

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
        try {
            val output = callback()
            this.unlock()
            return output
        } catch (exception: Exception) {
            this.unlock()
            throw exception
        }
    }

    fun open_multi() {
        if (this.isLocked()) {
            return
        }

        val next_node = HistoryNode("multi", listOf())

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

    open fun push_to_history_stack(func_name: String, args: List<Any>) {
        this.history_cache.append_undoer(func_name, args)
    }

    open fun apply_history_node(current_node: HistoryNode, depth: Int = 0) {
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
            "move_line" -> {
                this.move_line(
                    current_node.args[0] as Int,
                    current_node.args[1] as Int,
                    current_node.args[2] as Int,
                    current_node.args[3] as Int
                )
            }

            "insert_line" -> {
                this.insert_line(
                    current_node.args[0] as Int,
                    current_node.args[1] as Int,
                    current_node.args[2] as MutableList<OpusTree<OpusEvent>>
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
                this.insert_beat(
                    current_node.args[0] as Int,
                    current_node.args[1] as List<OpusTree<OpusEvent>>
                )
            }
            "set_transpose" -> {
                this.set_transpose(current_node.args[0] as Int)
            }
            "set_tempo" -> {
                this.set_tempo(current_node.args[0] as Float)
            }
            "set_channel_instrument" -> {
                this.set_channel_instrument(
                    current_node.args[0] as Int,
                    current_node.args[1] as Int
                )
            }
            "set_percussion_instrument" -> {
                this.set_percussion_instrument(
                    current_node.args[0] as Int, // line
                    current_node.args[1] as Int // Instrument
                )
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

    fun new_line(channel: Int, line_offset: Int, count: Int): List<List<OpusTree<OpusEvent>>> {
        return this.history_cache.remember {
            val output: MutableList<List<OpusTree<OpusEvent>>> = mutableListOf()
            for (i in 0 until count) {
                output.add(this.new_line(channel, line_offset))
            }
            output
        }
    }

    override fun new_line(channel: Int, line_offset: Int?): List<OpusTree<OpusEvent>> {
        return this.history_cache.remember {
            val output = super.new_line(channel, line_offset)
            this.push_remove_line(channel, line_offset ?: (this.channels[channel].size - 1))
            output
        }
    }

    override fun insert_line(channel: Int, line_index: Int, line: MutableList<OpusTree<OpusEvent>>) {
        this.history_cache.remember {
            super.insert_line(channel, line_index, line)
            this.push_remove_line(channel, line_index)
        }
    }

    open fun remove_line(channel: Int, line_offset: Int, count: Int) {
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

    override fun remove_line(channel: Int, line_offset: Int): MutableList<OpusTree<OpusEvent>> {
        return this.history_cache.remember {
            this.push_rebuild_line(channel, line_offset)
            super.remove_line(channel, line_offset)
        }
    }

    fun insert_after(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        this.history_cache.remember {
            for (i in 0 until repeat) {
                this.insert_after(beat_key, position)
            }
        }
        this.history_cache.close_multi()
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.history_cache.remember {
            val remove_position = position.toMutableList()
            remove_position[remove_position.size - 1] += 1
            super.insert_after(beat_key, position)
            this.push_remove(beat_key, remove_position)
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.history_cache.open_multi()
        this.push_replace_tree(beat_key, position)
        super.split_tree(beat_key, position, splits)
        this.history_cache.close_multi()
    }

    fun remove(beat_key: BeatKey, position: List<Int>, count: Int) {
        for (i in 0 until count) {
            this.remove(beat_key, position)
        }
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.history_cache.remember {
            val new_position = position.toMutableList()
            if (new_position.isNotEmpty()) {
                new_position.removeLast()
            }
            this.push_replace_tree(beat_key, new_position)
            super.remove(beat_key, position)
        }
    }

    override fun insert_beat(index: Int, count: Int) {
        this.history_cache.remember {
            super.insert_beat(index, count)
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        super.insert_beat(beat_index, beats_in_column)
        this.push_remove_beat(beat_index)
    }

    fun remove_beat(beat_index: Int, count: Int) {
        this.history_cache.remember {
            for (i in 0 until count) {
                if (this.opus_beat_count > 1) {
                    this.remove_beat(beat_index)
                } else {
                    break
                }
            }
        }
    }

    override fun remove_beat(beat_index: Int) {
        this.history_cache.remember {
            this.push_insert_beat(beat_index, this.get_channel_line_counts())
            super.remove_beat(beat_index)
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
                this.push_to_history_stack(
                    "set_percussion_channel",
                    listOf(this.percussion_channel!!)
                )
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

    override fun load(bytes: ByteArray) {
        this.history_cache.forget {
            super.load(bytes)
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

    fun push_replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>? = null) {
        if (!this.history_cache.isLocked()) {
            val use_tree = tree ?: this.get_tree(beat_key, position).copy()

            var stamp_position = position.toMutableList()
            var tmp_tree = use_tree
            while (! tmp_tree.is_leaf()) {
                stamp_position.add(0)
                tmp_tree = tmp_tree[0]
            }

            this.push_to_history_stack(
                "replace_tree",
                listOf(beat_key.copy(), position.toList(), use_tree)
            )
        }
    }

    fun push_new_channel(channel: Int) {
        this.history_cache.remember {
            for (i in this.channels[channel].size - 1 downTo 0) {
                this.push_rebuild_line(channel, i)
            }

            this.push_to_history_stack(
                "new_channel",
                listOf(channel)
            )
        }

    }

    fun push_rebuild_line(channel: Int, line_offset: Int) {
        this.history_cache.remember {
            this.push_to_history_stack(
                "insert_line",
                listOf(
                    channel,
                    line_offset,
                    this.channels[channel].lines[line_offset].beats.toMutableList()
                )
            )
        }
    }

    fun push_remove(beat_key: BeatKey, position: List<Int>) {
        if (position.isNotEmpty()) {
            var stamp_position = position.toMutableList()
            var parent = this.get_tree(beat_key, position).parent!!
            if (stamp_position.last() >= parent.size - 1 && parent.size > 1) {
                stamp_position[stamp_position.size - 1] = parent.size - 2
            } else if (parent.size <= 1) {
                // Shouldn't be Possible
            }
            this.push_to_history_stack( "remove", listOf(beat_key.copy(), position) )
        }
    }

    // Assumes called AFTER beat has been inserted
    fun push_remove_beat(index: Int) {
        this.push_to_history_stack( "remove_beat", listOf(index) )
    }

    fun push_insert_beat(index: Int, channel_sizes: List<Int>) {
        this.history_cache.remember {
            var beat_cells = mutableListOf<OpusTree<OpusEvent>>()
            for (channel in channel_sizes.indices) {
                val line_count = channel_sizes[channel]
                for (j in 0 until line_count) {
                    beat_cells.add(this.get_beat_tree(BeatKey(channel, j, index)))
                }
            }

            this.push_to_history_stack( "insert_beat", listOf(index, beat_cells))
        }
    }

    fun push_set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.push_to_history_stack( "set_event", listOf(beat_key.copy(), position, event.copy()) )
    }

    fun push_set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this.push_to_history_stack( "set_percussion_event", listOf(beat_key.copy(), position.toList()) )
    }

    fun push_unset(beat_key: BeatKey, position: List<Int>) {
        this.push_to_history_stack(
            "unset",
            listOf(beat_key.copy(), position.toList())
        )
    }

    fun push_remove_channel(channel: Int) {
        this.push_to_history_stack(
            "remove_channel",
            listOf(this.channels[channel].uuid)
        )
    }

    fun push_remove_line(channel: Int, index: Int) {
        this.push_to_history_stack( "remove_line", listOf(channel, index) )
    }

    fun push_split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.push_to_history_stack( "split_tree", listOf(beat_key.copy(), position.toList(), splits) )
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
            this.push_to_history_stack("create_link_pool", listOf(old_link_pool))
            for (beat_key in this.link_pools[index_first]) {
                this.push_to_history_stack("unlink_beat", listOf(beat_key))
            }

            super.merge_link_pools(index_first, index_second)
        }
    }

    override fun link_beat_into_pool(beat_key: BeatKey, index: Int, overwrite_pool: Boolean) {
        this.history_cache.remember {
            super.link_beat_into_pool(beat_key, index, overwrite_pool)
            this.push_to_history_stack("unlink_beat", listOf(beat_key))
        }
    }

    override fun create_link_pool(beat_keys: List<BeatKey>) {
        this.history_cache.remember {
            for (beat_key in beat_keys) {
                this.push_to_history_stack("unlink_beat", listOf(beat_key))
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
            this.push_to_history_stack("set_channel_instrument", listOf(channel, this.channels[channel].get_instrument()))
            var original_percussion_channel = this.percussion_channel
            super.set_percussion_channel(channel)
            if (original_percussion_channel == null) {
                this.push_to_history_stack("unset_percussion_channel", listOf())
            } else {
                this.push_to_history_stack("set_percussion_channel", listOf(original_percussion_channel!!))
            }
        }
    }

    override fun save(path: String?) {
        super.save(path)
        this.save_point_popped = false
        if (this.has_changed_since_save()) {
            this.push_to_history_stack("save_point", listOf())
        }
    }

    fun has_changed_since_save(): Boolean {
        val node = this.history_cache.peek()
        return (this.save_point_popped || (node != null && node.func_name != "save_point"))
    }

    override fun set_line_volume(channel: Int, line_offset: Int, volume: Int) {
        val current_volume = this.get_line_volume(channel, line_offset)
        this.push_to_history_stack("set_line_volume", listOf(channel, line_offset, current_volume))
        super.set_line_volume(channel, line_offset, volume)
    }

    override fun set_project_name(new_name: String) {
        this.push_to_history_stack("set_project_name", listOf(this.project_name))
        super.set_project_name(new_name)
    }

    override fun set_transpose(new_transpose: Int) {
        this.push_to_history_stack("set_transpose", listOf(this.transpose))
        super.set_transpose(new_transpose)
    }

    override fun set_tempo(new_tempo: Float) {
        this.push_to_history_stack("set_tempo", listOf(this.tempo))
        super.set_tempo(new_tempo)
    }

    override fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        this.push_move_line_back(channel_old, line_old, channel_new, line_new)
        this.history_cache.forget {
            super.move_line(channel_old, line_old, channel_new, line_new)
        }
    }

    fun push_move_line_back(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        if (this.history_cache.isLocked()) {
            return
        }
        this.history_cache.remember {
            var restore_old_line = false
            var return_from_line = if (channel_old == channel_new) {
                if (line_old < line_new) {
                    line_new - 1
                } else {
                    line_new
                }
            } else {
                line_new
            }

            var return_to_line = if (channel_old == channel_new) {
                if (line_old < line_new) {
                    line_old
                } else {
                    line_old + 1
                }
            } else if (this.channels[channel_old].size == 1) {
                restore_old_line = true
                0
            } else {
                line_old
            }

            this.push_to_history_stack("move_line", listOf(channel_new, return_from_line, channel_old, return_to_line))
            if (restore_old_line) {
                this.push_to_history_stack("remove_line", listOf(channel_old, line_old))
            }
        }
    }

    override fun set_channel_instrument(channel: Int, instrument: Int) {
        this.history_cache.remember {
            this.push_to_history_stack("set_channel_instrument",
                listOf(channel, this.channels[channel].get_instrument()))
            super.set_channel_instrument(channel, instrument)
        }
    }

    override fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        this.history_cache.remember {
            var current = this.get_percussion_instrument(line_offset)
            this.push_to_history_stack("set_percussion_instrument", listOf(line_offset, current))
            super.set_percussion_instrument(line_offset, instrument)
        }
    }
}
