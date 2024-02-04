package com.qfs.pagan.opusmanager
import com.qfs.apres.Midi
import com.qfs.pagan.structure.OpusTree
import kotlin.math.min

open class OpusLayerHistory : OpusLayerLinks() {
    var history_cache = HistoryCache()

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
                    val pools = this.checked_cast<List<Set<BeatKey>>>(current_node.args[0])
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
                        this.checked_cast<OpusChannel.OpusLine>(current_node.args[2])
                    )
                }

                HistoryToken.REMOVE_CHANNEL -> {

                    val uuid = current_node.args[0] as Int
                    this.remove_channel_by_uuid(uuid)
                }

                HistoryToken.NEW_CHANNEL -> {
                    val channel = current_node.args[0] as Int
                    this.new_channel(
                        channel = channel,
                        uuid = current_node.args[1] as Int
                    )
                    this.set_channel_instrument(channel, Pair(current_node.args[2] as Int, current_node.args[3] as Int))
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

                HistoryToken.SET_TUNING_MAP -> {
                    this.set_tuning_map(
                        this.checked_cast<Array<Pair<Int, Int>>>(current_node.args[0]),
                        false
                    )
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

                HistoryToken.SET_EVENT_DURATION -> {
                    this.set_duration(
                        current_node.args[0] as BeatKey,
                        this.checked_cast<List<Int>>(current_node.args[1]),
                        current_node.args[2] as Int
                    )
                }

                HistoryToken.SWAP_LINES -> {
                    this.swap_lines(
                        current_node.args[0] as Int,
                        current_node.args[1] as Int,
                        current_node.args[2] as Int,
                        current_node.args[3] as Int
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
        this.lock_links {
            this.history_cache.lock()


            val node = this.history_cache.pop()
            if (node == null) {
                this.history_cache.unlock()
                return@lock_links
            }

            if (node.token == HistoryToken.MULTI && node.children.isEmpty()) {
                // If the node was an empty 'multi'  node, try the next one
                this.history_cache.unlock()
                this.apply_undo()
                return@lock_links
            }

            this.apply_history_node(node)

            this.history_cache.unlock()
        }
    }


    fun new_line(channel: Int, line_offset: Int, count: Int): List<OpusChannel.OpusLine> {
        return this.remember {
            val output: MutableList<OpusChannel.OpusLine> = mutableListOf()
            for (i in 0 until count) {
                output.add(this.new_line(channel, line_offset))
            }
            output
        }
    }

    override fun new_line(channel: Int, line_offset: Int?): OpusChannel.OpusLine {
        return this.remember {
            val output = super.new_line(channel, line_offset)
            this.push_remove_line(channel, line_offset ?: (this.channels[channel].size))
            output
        }
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusChannel.OpusLine) {
        this.remember {
            this.push_remove_line(channel, line_offset)
            super.insert_line(channel, line_offset, line)
        }
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {

        this.remember {
            super.swap_lines(channel_a, line_a, channel_b, line_b)
            this.push_to_history_stack(
                HistoryToken.SWAP_LINES,
                listOf(channel_a, line_a, channel_b, line_b)
            )
        }
    }

    open fun remove_line(channel: Int, line_offset: Int, count: Int) {
        this.remember {
            for (i in 0 until count) {
                if (this.channels[channel].size == 0) {
                    break
                }
                try {
                    this.remove_line(
                        channel,
                        min(line_offset, this.channels[channel].size - 1)
                    )
                } catch (e: OpusChannel.LastLineException) {
                    break
                }
            }
        }
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusChannel.OpusLine {
        return this.remember {
            val line = super.remove_line(channel, line_offset)

            this.push_to_history_stack(
                HistoryToken.INSERT_LINE,
                listOf(channel, line_offset, line)
            )

            line
        }
    }

    fun insert_after(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        this.remember {
            for (i in 0 until repeat) {
                this.insert_after(beat_key, position)
            }
        }
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.remember {
            val remove_position = position.toMutableList()
            remove_position[remove_position.size - 1] += 1
            super.insert_after(beat_key, position)
            this.push_remove(beat_key, remove_position)
        }
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        this.remember {
            super.insert(beat_key, position)
            this.push_remove(beat_key, position)
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.remember {
            this.push_replace_tree(beat_key, position) {
                super.split_tree(beat_key, position, splits)
            }
        }
    }

    fun remove(beat_key: BeatKey, position: List<Int>, count: Int) {
        this.remember {
            for (i in 0 until count) {
                this.remove(beat_key, position)
            }
        }
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        val old_tree = this.get_tree(beat_key, position.subList(0, position.size - 1)).copy()
        this.push_replace_tree(beat_key, position.subList(0, position.size - 1), old_tree) {
            this.remember {
                super.remove(beat_key, position)
            }
        }
    }

    override fun remove_one_of_two(beat_key: BeatKey, position: List<Int>) {
        this.forget {
            super.remove_one_of_two(beat_key, position)
        }
    }

    override fun remove_only(beat_key: BeatKey, position: List<Int>) {
        this.forget {
            super.remove_only(beat_key, position)
        }
    }

    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        this.forget {
            super.remove_standard(beat_key, position)
        }
    }

    override fun insert_beat(beat_index: Int, count: Int) {
        this.remember {
            super.insert_beat(beat_index, count)
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        super.insert_beat(beat_index, beats_in_column)
        this.push_to_history_stack( HistoryToken.REMOVE_BEAT, listOf(beat_index) )
    }

    fun remove_beat(beat_index: Int, count: Int) {
        this.remember {
            for (i in 0 until count) {
                if (this.beat_count > 1) {
                    this.remove_beat(min(beat_index, this.beat_count - 1))
                } else {
                    break
                }
            }
        }
    }

    override fun remove_beat(beat_index: Int) {
        this.remember {
            val beat_cells = mutableListOf<OpusTree<OpusEvent>>()
            for (channel in 0 until this.channels.size) {
                val line_count = this.channels[channel].size
                for (j in 0 until line_count) {
                    beat_cells.add(
                        this.get_tree(BeatKey(channel, j, beat_index))
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

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<OpusEvent>) {
        try {
            this.remember {
                this.push_replace_tree(beat_key, position) {
                    super.replace_tree(beat_key, position, tree)
                }
            }
        } catch (e: TrivialActionException) {
            // pass
        }
    }

    override fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        this.remember {
            super.move_leaf(beatkey_from, position_from, beatkey_to, position_to)
        }
    }

    override fun move_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        this.remember {
            super.move_beat_range(beat_key, first_corner, second_corner)
        }
    }

    override fun unset_range(first_corner: BeatKey, second_corner: BeatKey) {
        this.remember {
            super.unset_range(first_corner, second_corner)
        }
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.remember {
            val tree = this.get_tree(beat_key, position).copy()
            this.push_replace_tree(beat_key, position, tree) {
                super.set_event(beat_key, position, event)
            }
        }
    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this.remember {
            super.set_percussion_event(beat_key, position)

            val tree = this.get_tree(beat_key, position)
            if (!tree.is_event()) {
                this.push_set_percussion_event(beat_key, position)
            } else {
                this.push_unset(beat_key, position)
            }
        }
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.remember {
            val tree = this.get_tree(beat_key, position)
            if (tree.is_event()) {
                val original_event = tree.get_event()!!
                if (!this.is_percussion(beat_key.channel)) {
                    super.unset(beat_key, position)
                    this.push_set_event(
                        beat_key,
                        position,
                        original_event
                    )
                } else {
                    super.unset(beat_key, position)
                    this.push_set_percussion_event(beat_key, position)
                }
            } else if (!tree.is_leaf()) {
                this.push_replace_tree(beat_key, position, tree.copy()) {
                    super.unset(beat_key, position)
                }
            } else {
                super.unset(beat_key, position)
            }
        }
    }

    override fun load_json(json_data: LoadedJSONData) {
        this.history_cache.forget {
            super.load_json(json_data)
        }
    }

    override fun new() {
        this.history_cache.forget {
            super.new()
        }
    }

    override fun import_midi(midi: Midi) {
        this.history_cache.forget {
            super.import_midi(midi)
        }
    }

    override fun clear() {
        this.clear_history()
        this.forget {
            super.clear()
        }
    }

    fun clear_history() {
        this.history_cache.clear()
    }

    private fun <T> push_replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<OpusEvent>? = null, callback: () -> T): T {
        return if (!this.history_cache.isLocked()) {
            val use_tree = tree ?: this.get_tree(beat_key, position).copy()

            val output = callback()

            this.push_to_history_stack(
                HistoryToken.REPLACE_TREE,
                listOf(beat_key.copy(), position?.toList() ?: listOf<Int>(), use_tree)
            )
            output
        } else {
            callback()
        }
    }

    private fun <T> push_rebuild_channel(channel: Int, callback: () -> T): T {
        return this.remember {
            val tmp_history_nodes = mutableListOf<Pair<HistoryToken, List<Any>>>()
            val line_count = this.channels[channel].lines.size
            // Will be an extra empty line that needs to be removed
            tmp_history_nodes.add(Pair( HistoryToken.REMOVE_LINE, listOf(channel, line_count) ))
            for (i in line_count - 1 downTo 0) {
                tmp_history_nodes.add(
                    Pair(
                        HistoryToken.INSERT_LINE,
                        listOf( channel, i, this.channels[channel].lines[i] )
                    )
                )
            }

            tmp_history_nodes.add(
                Pair(
                    HistoryToken.NEW_CHANNEL,
                    listOf(
                        channel,
                        this.channels[channel].uuid,
                        this.channels[channel].midi_bank,
                        this.channels[channel].midi_program
                    )
                )
            )

            val output = callback()
            for ((token, args) in tmp_history_nodes) {
                this.push_to_history_stack(token, args)
            }

            output
        }
    }

    private fun push_remove(beat_key: BeatKey, position: List<Int>) {
        if (position.isNotEmpty()) {
            val stamp_position = position.toMutableList()
            val parent_position = position.subList(0, position.size - 1)
            val parent = this.get_tree(beat_key, parent_position)
            if (stamp_position.last() >= parent.size - 1 && parent.size > 1) {
                stamp_position[stamp_position.size - 1] = parent.size - 2
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

    private fun push_remove_line(channel: Int, index: Int) {
        this.push_to_history_stack( HistoryToken.REMOVE_LINE, listOf(channel, index) )
    }

    override fun link_beats(beat_key: BeatKey, target: BeatKey) {
        this.remember {
            super.link_beats(beat_key, target)
        }
    }

    override fun merge_link_pools(index_first: Int, index_second: Int) {
        this.remember {

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
        this.remember {
            super.link_beat_into_pool(beat_key, index, overwrite_pool)
            this.push_to_history_stack(HistoryToken.UNLINK_BEAT, listOf(beat_key))
        }
    }

    override fun create_link_pool(beat_keys: List<BeatKey>) {
        this.remember {
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
        this.remember {
            super.batch_link_beats(beat_key_pairs)
        }
    }

    override fun remove_channel(channel: Int) {
        this.remember {
            this.push_rebuild_channel(channel) {
                this.history_cache.lock()
                super.remove_channel(channel)
                this.history_cache.unlock()
            }
        }
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        this.remember {
            val channel_to_remove = channel ?: if (this.channels.isNotEmpty()) {
                this.channels.size - 1
            } else {
                0
            }

            super.new_channel(channel, lines, uuid)

            this.push_to_history_stack(
                HistoryToken.REMOVE_CHANNEL,
                listOf(this.channels[channel_to_remove].uuid)
            )
        }
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

    override fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>) {
        this.remember {
            this.push_to_history_stack(
                HistoryToken.SET_CHANNEL_INSTRUMENT,
                listOf(channel, this.channels[channel].get_instrument())
            )
            super.set_channel_instrument(channel, instrument)
        }
    }

    override fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        this.remember {
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
    override fun unlink_range(first_key: BeatKey, second_key: BeatKey) {
        this.remember {
            super.unlink_range(first_key, second_key)
        }
    }

    override fun link_column(column: Int, beat_key: BeatKey) {
        this.remember {
            super.link_column(column, beat_key)
        }
    }

    override fun link_row(channel: Int, line_offset: Int, beat_key: BeatKey) {
        this.remember {
            this.clear_link_pools_by_range(
                BeatKey(channel, line_offset, 0),
                BeatKey(channel, line_offset, this.beat_count - 1)
            )
            super.link_row(channel, line_offset, beat_key)
        }
    }

    override fun remove_link_pool(index: Int) {
        this.remember {
            this.push_to_history_stack(HistoryToken.CREATE_LINK_POOL, listOf(this.link_pools[index]))
            super.remove_link_pool(index)
        }
    }

    override fun link_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {
        this.remember {
            val (from_key, _) = this.get_ordered_beat_key_pair(first_key, second_key)
            if (from_key.channel != channel || from_key.line_offset != line_offset || from_key.beat != 0) {
                throw BadRowLink(from_key, channel, line_offset)
            }

            val y_top = this.get_abs_offset(first_key.channel, first_key.line_offset)
            val y_bottom = this.get_abs_offset(second_key.channel, second_key.line_offset)
            val y_link_top = this.get_abs_offset(channel, line_offset)
            val y_link_bottom = y_link_top + (y_bottom - y_top)


            val (bottom_channel, bottom_line_offset) = try {
                this.get_std_offset(y_link_bottom)
            } catch (e: IndexOutOfBoundsException) {
                throw BadRowLink(first_key, channel, line_offset)
            }

            val clear_beat_key_top = BeatKey(channel, line_offset, 0)
            val clear_beat_key_bottom = BeatKey(bottom_channel, bottom_line_offset, this.beat_count -1)

            this.clear_link_pools_by_range(
                clear_beat_key_top,
                clear_beat_key_bottom
            )
            super.link_beat_range_horizontally(channel, line_offset, first_key, second_key)
        }
    }

    override fun overwrite_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        this.remember {
            super.overwrite_beat_range(beat_key, first_corner, second_corner)
        }

    }

    override fun clear_link_pools_by_range(first_key: BeatKey, second_key: BeatKey) {
        this.remember {
            super.clear_link_pools_by_range(first_key, second_key)
        }
    }

    override fun remap_links(remap_hook: (beat_key: BeatKey) -> BeatKey?) {
        val original_link_pools = this.link_pools.toList()
        this.push_to_history_stack(HistoryToken.RESTORE_LINK_POOLS, listOf(original_link_pools))
        super.remap_links(remap_hook)
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this.remember {
            val tree = this.get_tree(beat_key, position)
            if (tree.is_event()) {
                val event = tree.get_event()
                this.push_to_history_stack(HistoryToken.SET_EVENT_DURATION, listOf(beat_key, position, event!!.duration))
            }

            super.set_duration(beat_key, position, duration)
        }

    }

    fun <T> remember(callback: () -> T): T {
        return try {
            this.history_cache.remember {
                callback()
            }
        } catch (history_error: HistoryCache.HistoryError) {
            val real_exception = history_error.e
            var tmp_error: Exception = history_error
            var node: HistoryCache.HistoryNode? = null

            while (tmp_error is HistoryCache.HistoryError) {
                node = tmp_error.failed_node
                tmp_error = tmp_error.e
            }
            if (node != null) {
                this.history_cache.forget {
                    this.apply_history_node(node)
                }
            }
            throw real_exception
        }
    }
    private fun <T> forget(callback: () -> T): T {
        return this.history_cache.forget {
            callback()
        }
    }

    override fun overwrite_row(channel: Int, line_offset: Int, beat_key: BeatKey) {
        this.remember {
            super.overwrite_row(channel, line_offset, beat_key)
        }
    }

    override fun overwrite_beat_range_horizontally(
        channel: Int,
        line_offset: Int,
        first_key: BeatKey,
        second_key: BeatKey
    ) {
        this.remember {
            super.overwrite_beat_range_horizontally(channel, line_offset, first_key, second_key)
        }
    }

    override fun set_tuning_map(new_map: Array<Pair<Int, Int>>, mod_events: Boolean) {
        this.remember {
            val original_map = this.tuning_map.clone()
            super.set_tuning_map(new_map, mod_events)
            this.push_to_history_stack(HistoryToken.SET_TUNING_MAP, listOf(original_map))
        }
    }

    // Need a compound function so history can manage both at the same time
    open fun set_tuning_map_and_transpose(tuning_map: Array<Pair<Int, Int>>, transpose: Int) {
            this.remember {
                this.set_tuning_map(tuning_map)
                this.set_transpose(transpose)
            }
    }
}