package com.qfs.pagan.opusmanager
import com.qfs.apres.MIDI
import com.qfs.pagan.structure.OpusTree


open class HistoryLayer : LinksLayer() {
    class HistoryCache {
        class HistoryNode(var token: HistoryToken, var args: List<Any>) {
            var children: MutableList<HistoryNode> = mutableListOf()
            var parent: HistoryNode? = null
        }

        private var history_lock = 0
        private var history: MutableList<HistoryNode> = mutableListOf()
        private var working_node: HistoryNode? = null

        fun isLocked(): Boolean {
            return this.history_lock != 0
        }

        fun isEmpty(): Boolean {
            return this.history.isEmpty()
        }

        fun append_undoer(token: HistoryToken, args: List<Any>) {
            if (this.isLocked()) {
                return
            }
            val new_node = HistoryNode(token, args)

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
            } catch (e: Exception) {
                this.unlock()
                throw e
            }
        }

        fun open_multi() {
            if (this.isLocked()) {
                return
            }

            val next_node = HistoryNode(HistoryToken.MULTI, listOf())

            if (this.working_node != null) {
                next_node.parent = this.working_node
                this.working_node!!.children.add(next_node)
            } else {
                this.history.add(next_node)
            }
            this.working_node = next_node
        }

        fun close_multi() {
            if (this.isLocked()) {
                return
            }

            if (this.working_node != null) {
                this.working_node = this.working_node!!.parent
            }
        }

        private fun cancel_multi() {
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
    var history_cache = HistoryCache()
    private var save_point_popped = false

    inline fun <reified T> checked_cast(value: Any): T {
        if (value is T) {
            return value
        }  else {
            throw ClassCastException()
        }
    }

    open fun push_to_history_stack(token: HistoryToken, args: List<Any>) {
        this.history_cache.append_undoer(token, args)
    }

    open fun apply_history_node(current_node: HistoryCache.HistoryNode, depth: Int = 0) {
        try {
            when (current_node.token) {
                HistoryToken.SPLIT_TREE -> {
                    this.split_tree(
                        current_node.args[0] as BeatKey,
                        this.checked_cast<List<Int>>(current_node.args[1]),
                        current_node.args[2] as Int
                    )
                }

                HistoryToken.SET_PROJECT_NAME -> {
                    this.set_project_name(current_node.args[0] as String)
                }

                HistoryToken.SET_LINE_VOLUME -> {
                    this.set_line_volume(
                        current_node.args[0] as Int,
                        current_node.args[1] as Int,
                        current_node.args[2] as Int
                    )
                }

                HistoryToken.UNLINK_BEAT -> {
                    this.unlink_beat(current_node.args[0] as BeatKey)
                }

                HistoryToken.RESTORE_LINK_POOLS -> {
                    var pools = this.checked_cast<List<Set<BeatKey>>>(current_node.args[0])
                    this.link_pools.clear()
                    this.link_pool_map.clear()
                    pools.forEachIndexed { i: Int, pool: Set<BeatKey> ->
                        for (beat_key in pool) {
                            this.link_pool_map[beat_key] = i
                        }
                        this.link_pools.add(pool.toMutableSet())
                    }
                }

                HistoryToken.LINK_BEATS -> {
                    this.link_beats(
                        current_node.args[0] as BeatKey,
                        current_node.args[1] as BeatKey
                    )
                }

                HistoryToken.LINK_BEAT_TO_POOL -> {
                    // No need to overwrite in history
                    val beat_key = current_node.args[0] as BeatKey
                    val pool_index = current_node.args[1] as Int
                    this.link_pool_map[beat_key] = pool_index
                    this.link_pools[pool_index].add(beat_key)
                }

                HistoryToken.CREATE_LINK_POOL -> {
                    this.create_link_pool(this.checked_cast<LinkedHashSet<BeatKey>>(current_node.args[0]).toList())
                }

                HistoryToken.SET_EVENT -> {
                    this.set_event(
                        current_node.args[0] as BeatKey,
                        this.checked_cast<List<Int>>(current_node.args[1]),
                        current_node.args[2] as OpusEvent
                    )
                }

                HistoryToken.SET_PERCUSSION_EVENT -> {
                    this.set_percussion_event(
                        current_node.args[0] as BeatKey,
                        this.checked_cast<List<Int>>(current_node.args[1])
                    )
                }

                HistoryToken.UNSET -> {
                    this.unset(
                        current_node.args[0] as BeatKey,
                        this.checked_cast<List<Int>>(current_node.args[1])
                    )
                }

                HistoryToken.REPLACE_TREE -> {
                    val beatkey = current_node.args[0] as BeatKey
                    val position = this.checked_cast<List<Int>>(current_node.args[1])
                    val tree = this.checked_cast<OpusTree<OpusEvent>>(current_node.args[2])

                    this.replace_tree(beatkey, position, tree)
                }

                HistoryToken.REMOVE_LINE -> {
                    this.remove_line(
                        current_node.args[0] as Int,
                        current_node.args[1] as Int
                    )
                }

                HistoryToken.MOVE_LINE -> {
                    this.move_line(
                        current_node.args[0] as Int,
                        current_node.args[1] as Int,
                        current_node.args[2] as Int,
                        current_node.args[3] as Int
                    )
                }

                HistoryToken.INSERT_TREE -> {
                    val beat_key = current_node.args[0] as BeatKey
                    val position = this.checked_cast<List<Int>>(current_node.args[1])
                    val insert_tree = this.checked_cast<OpusTree<OpusEvent>>(current_node.args[2])
                    this.insert(beat_key, position)
                    this.replace_tree(beat_key, position, insert_tree)
                }

                HistoryToken.INSERT_LINE -> {
                    this.insert_line(
                        current_node.args[0] as Int,
                        current_node.args[1] as Int,
                        this.checked_cast<MutableList<OpusTree<OpusEvent>>>(current_node.args[2])
                    )
                }

                HistoryToken.REMOVE_CHANNEL -> {
                    this.remove_channel_by_uuid(current_node.args[0] as Int)
                }

                HistoryToken.NEW_CHANNEL -> {
                    this.new_channel(current_node.args[0] as Int)
                }

                HistoryToken.REMOVE -> {
                    this.remove(
                        current_node.args[0] as BeatKey,
                        this.checked_cast<List<Int>>(current_node.args[1])
                    )
                }

                HistoryToken.REMOVE_BEAT -> {
                    this.remove_beat(current_node.args[0] as Int)
                }

                HistoryToken.INSERT_BEAT -> {
                    this.insert_beat(
                        current_node.args[0] as Int,
                        this.checked_cast<List<OpusTree<OpusEvent>>>(current_node.args[1])
                    )
                }

                HistoryToken.SET_TRANSPOSE -> {
                    this.set_transpose(current_node.args[0] as Int)
                }

                HistoryToken.SET_TEMPO -> {
                    this.set_tempo(current_node.args[0] as Float)
                }

                HistoryToken.SET_CHANNEL_INSTRUMENT -> {
                    this.set_channel_instrument(
                        current_node.args[0] as Int,
                        this.checked_cast<Pair<Int, Int>>(current_node.args[1])
                    )
                }

                HistoryToken.SET_PERCUSSION_INSTRUMENT -> {
                    this.set_percussion_instrument(
                        current_node.args[0] as Int, // line
                        current_node.args[1] as Int // Instrument
                    )
                }

                else -> {}
            }
        } catch (e: ClassCastException) {
            // pass
        }

        if (current_node.children.isNotEmpty()) {
            current_node.children.asReversed().forEach { child: HistoryCache.HistoryNode ->
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

        // Skip special case HistoryToken.SAVE_POINT
        if (node.token == HistoryToken.SAVE_POINT) {
            this.save_point_popped = true
            this.history_cache.unlock()
            this.apply_undo()
            return
        } else if (node.token == HistoryToken.MULTI && node.children.isEmpty()) {
            // If the node was an empty 'multi'  node, try the next one
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

    override fun insert_line(channel: Int, line_offset: Int, line: MutableList<OpusTree<OpusEvent>>) {
        this.history_cache.remember {
            super.insert_line(channel, line_offset, line)
            this.push_remove_line(channel, line_offset)
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
    override fun insert(beat_key: BeatKey, position: List<Int>) {
        this.history_cache.remember {
            super.insert(beat_key, position)
            this.push_remove(beat_key, position)
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.history_cache.remember {
            this.push_replace_tree(beat_key, position)
            super.split_tree(beat_key, position, splits)
        }
    }

    fun remove(beat_key: BeatKey, position: List<Int>, count: Int) {
        for (i in 0 until count) {
            this.remove(beat_key, position)
        }
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.history_cache.remember {
            val old_tree = this.get_tree(beat_key, position)

            this.push_to_history_stack(HistoryToken.INSERT_TREE, listOf(beat_key, position, old_tree))
            val parent_size = old_tree.parent!!.size
            super.remove(beat_key, position)

            // Pushing the replace_tree AFTER the target has been removed allows for HistoryToken.INSERT_TREE
            // to be called on apply-history
            if (parent_size == 2) {
                val parent_position = position.toMutableList()
                parent_position.removeLast()
                this.push_replace_tree(beat_key, parent_position)
            }
        }
    }

    override fun insert_beat(beat_index: Int, count: Int) {
        this.history_cache.remember {
            super.insert_beat(beat_index, count)
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        super.insert_beat(beat_index, beats_in_column)
        this.push_to_history_stack( HistoryToken.REMOVE_BEAT, listOf(beat_index) )
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
            val beat_cells = mutableListOf<OpusTree<OpusEvent>>()
            for (channel in 0 until this.channels.size) {
                val line_count = this.channels[channel].size
                for (j in 0 until line_count) {
                    beat_cells.add(
                        this.get_beat_tree(
                            BeatKey(channel, j, beat_index)
                        )
                    )
                }
            }

            super.remove_beat(beat_index)

            this.push_to_history_stack(
                HistoryToken.INSERT_BEAT,
                listOf(beat_index, beat_cells)
            )
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

    private fun push_replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>? = null) {
        if (!this.history_cache.isLocked()) {
            val use_tree = tree ?: this.get_tree(beat_key, position).copy()

            this.push_to_history_stack(
                HistoryToken.REPLACE_TREE,
                listOf(beat_key.copy(), position.toList(), use_tree)
            )
        }
    }

    private fun push_new_channel(channel: Int) {
        this.history_cache.remember {
            for (i in this.channels[channel].size - 1 downTo 0) {
                this.push_rebuild_line(channel, i)
            }

            this.push_to_history_stack(
                HistoryToken.NEW_CHANNEL,
                listOf(channel)
            )
        }

    }

    private fun push_rebuild_line(channel: Int, line_offset: Int) {
        this.push_to_history_stack(
            HistoryToken.INSERT_LINE,
            listOf(
                channel,
                line_offset,
                this.channels[channel].lines[line_offset].beats.toMutableList()
            )
        )
    }

    private fun push_remove(beat_key: BeatKey, position: List<Int>) {
        if (position.isNotEmpty()) {
            val stamp_position = position.toMutableList()
            val parent = this.get_tree(beat_key, position).parent!!
            if (stamp_position.last() >= parent.size - 1 && parent.size > 1) {
                stamp_position[stamp_position.size - 1] = parent.size - 2
            //} else if (parent.size <= 1) {
                // Shouldn't be Possible
            }
            this.push_to_history_stack( HistoryToken.REMOVE, listOf(beat_key.copy(), position) )
        }
    }


    private fun push_set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.push_to_history_stack( HistoryToken.SET_EVENT, listOf(beat_key.copy(), position, event.copy()) )
    }

    private fun push_set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this.push_to_history_stack( HistoryToken.SET_PERCUSSION_EVENT, listOf(beat_key.copy(), position.toList()) )
    }

    private fun push_unset(beat_key: BeatKey, position: List<Int>) {
        this.push_to_history_stack(
            HistoryToken.UNSET,
            listOf(beat_key.copy(), position.toList())
        )
    }

    private fun push_remove_channel(channel: Int) {
        this.push_to_history_stack(
            HistoryToken.REMOVE_CHANNEL,
            listOf(this.channels[channel].uuid)
        )
    }

    private fun push_remove_line(channel: Int, index: Int) {
        this.push_to_history_stack( HistoryToken.REMOVE_LINE, listOf(channel, index) )
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
            this.push_to_history_stack(HistoryToken.CREATE_LINK_POOL, listOf(old_link_pool))
            for (beat_key in this.link_pools[index_first]) {
                this.push_to_history_stack(HistoryToken.UNLINK_BEAT, listOf(beat_key))
            }

            super.merge_link_pools(index_first, index_second)
        }
    }

    override fun link_beat_into_pool(beat_key: BeatKey, index: Int, overwrite_pool: Boolean) {
        this.history_cache.remember {
            super.link_beat_into_pool(beat_key, index, overwrite_pool)
            this.push_to_history_stack(HistoryToken.UNLINK_BEAT, listOf(beat_key))
        }
    }

    override fun create_link_pool(beat_keys: List<BeatKey>) {
        this.history_cache.remember {
            // Do not unlink last. it is automatically unlinked by the penultimate
            beat_keys.forEachIndexed { i: Int, beat_key ->
                if (i == beat_keys.size - 1) {
                    return@forEachIndexed
                }
                this.push_to_history_stack(HistoryToken.UNLINK_BEAT, listOf(beat_key))
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

    override fun save(path: String?) {
        super.save(path)
        this.save_point_popped = false
        if (this.has_changed_since_save()) {
            this.push_to_history_stack(HistoryToken.SAVE_POINT, listOf())
        }
    }

    fun has_changed_since_save(): Boolean {
        val node = this.history_cache.peek()
        return (this.save_point_popped || (node != null && node.token != HistoryToken.SAVE_POINT))
    }

    override fun set_line_volume(channel: Int, line_offset: Int, volume: Int) {
        val current_volume = this.get_line_volume(channel, line_offset)
        this.push_to_history_stack(HistoryToken.SET_LINE_VOLUME, listOf(channel, line_offset, current_volume))
        super.set_line_volume(channel, line_offset, volume)
    }

    override fun set_project_name(new_name: String) {
        this.push_to_history_stack(HistoryToken.SET_PROJECT_NAME, listOf(this.project_name))
        super.set_project_name(new_name)
    }

    override fun set_transpose(new_transpose: Int) {
        this.push_to_history_stack(HistoryToken.SET_TRANSPOSE, listOf(this.transpose))
        super.set_transpose(new_transpose)
    }

    override fun set_tempo(new_tempo: Float) {
        this.push_to_history_stack(HistoryToken.SET_TEMPO, listOf(this.tempo))
        super.set_tempo(new_tempo)
    }

    override fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        this.push_move_line_back(channel_old, line_old, channel_new, line_new)
        this.history_cache.forget {
            super.move_line(channel_old, line_old, channel_new, line_new)
        }
    }

    private fun push_move_line_back(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        if (this.history_cache.isLocked()) {
            return
        }
        this.history_cache.remember {
            var restore_old_line = false
            val return_from_line = if (channel_old == channel_new) {
                if (line_old < line_new) {
                    line_new - 1
                } else {
                    line_new
                }
            } else {
                line_new
            }

            val return_to_line = if (channel_old == channel_new) {
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


            this.push_to_history_stack(HistoryToken.MOVE_LINE, listOf(channel_new, return_from_line, channel_old, return_to_line))
            if (restore_old_line) {
                this.push_to_history_stack(HistoryToken.REMOVE_LINE, listOf(channel_old, line_old))
            }
        }
    }

    override fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>) {
        this.history_cache.remember {
            this.push_to_history_stack(
                HistoryToken.SET_CHANNEL_INSTRUMENT,
                listOf(channel, this.channels[channel].get_instrument())
            )
            super.set_channel_instrument(channel, instrument)
        }
    }

    override fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        this.history_cache.remember {
            val current = this.get_percussion_instrument(line_offset)
            this.push_to_history_stack(HistoryToken.SET_PERCUSSION_INSTRUMENT, listOf(line_offset, current))
            super.set_percussion_instrument(line_offset, instrument)
        }
    }

    override fun unlink_beat(beat_key: BeatKey) {
        val pool = this.link_pools[this.link_pool_map[beat_key]!!]
        for (linked_key in pool) {
            if (beat_key != linked_key) {
                this.push_to_history_stack(HistoryToken.LINK_BEATS, listOf(beat_key, linked_key))
                break
            }
        }
        super.unlink_beat(beat_key)
    }

    override fun link_column(column: Int, beat_key: BeatKey) {
        this.history_cache.remember {
            super.link_column(column, beat_key)
        }
    }

    override fun link_row(channel: Int, line_offset: Int, beat_key: BeatKey) {
        this.history_cache.remember {
            super.link_row(channel, line_offset, beat_key)
        }
    }

    override fun remove_link_pool(index: Int) {
        this.history_cache.remember {
            this.push_to_history_stack(HistoryToken.CREATE_LINK_POOL, listOf(this.link_pools[index]))
            super.remove_link_pool(index)
        }
    }

    override fun link_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {
        this.history_cache.remember {
            super.link_beat_range_horizontally(channel, line_offset, first_key, second_key)
        }
    }

    override fun overwrite_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        this.history_cache.remember {
            super.overwrite_beat_range(beat_key, first_corner, second_corner)
        }

    }

    override fun clear_link_pools_by_range(first_key: BeatKey, second_key: BeatKey) {
        this.history_cache.remember {
            super.clear_link_pools_by_range(first_key, second_key)
        }
    }

    override fun remap_links(remap_hook: (beat_key: BeatKey) -> BeatKey?) {
        this.push_to_history_stack(HistoryToken.RESTORE_LINK_POOLS, listOf(this.link_pools.toList()))
        super.remap_links(remap_hook)
    }
}
