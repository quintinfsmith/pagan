package com.qfs.pagan.opusmanager
import com.qfs.apres.Midi
import com.qfs.json.JSONHashMap
import com.qfs.pagan.structure.OpusTree
import kotlin.math.min

open class OpusLayerHistory: OpusLayerCursor() {
    var history_cache = HistoryCache()

    override fun new_line_repeat(channel: Int, line_offset: Int, count: Int) {
        this._remember {
            super.new_line_repeat(channel, line_offset, count)
        }
    }

    override fun insert_after_repeat(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        this._remember {
            super.insert_after_repeat(beat_key, position, repeat)
        }
    }

    override fun insert_repeat(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        this._remember {
            super.insert_repeat(beat_key, position, repeat)
        }
    }

    fun controller_global_insert_after(type: ControlEventType, beat: Int, position: List<Int>, repeat: Int) {
        this._remember {
            for (i in 0 until repeat) {
                this.controller_global_insert_after(type, beat, position)
            }
        }
    }

    fun controller_channel_insert_after(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, repeat: Int) {
        this._remember {
            for (i in 0 until repeat) {
                this.controller_channel_insert_after(type, channel, beat, position)
            }
        }
    }

    fun controller_line_insert_after(type: ControlEventType, beat_key: BeatKey, position: List<Int>, repeat: Int) {
        this._remember {
            for (i in 0 until repeat) {
                this.controller_line_insert_after(type, beat_key, position)
            }
        }
    }

    override fun remove_repeat(beat_key: BeatKey, position: List<Int>, count: Int) {
        this._remember {
            super.remove_repeat(beat_key, position, count)
        }
    }

    override fun repeat_controller_channel_remove(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, repeat: Int) {
        this._remember {
            super.repeat_controller_channel_remove(type, channel, beat, position, repeat)
        }
    }

    override fun repeat_controller_line_remove(type: ControlEventType, beat_key: BeatKey, position: List<Int>, count: Int) {
        this._remember {
            super.repeat_controller_line_remove(type, beat_key, position, count)
        }
    }

    override fun repeat_controller_global_remove(type: ControlEventType, beat: Int, position: List<Int>, count: Int) {
        this._remember {
            super.repeat_controller_global_remove(type, beat, position, count)
        }
    }

    fun controller_line_remove(type: ControlEventType, beat_key: BeatKey, position: List<Int>, count: Int) {
        this._remember {
            val adj_position = position.toMutableList()
            for (i in 0 until count) {
                val tree = this.get_line_ctl_tree<OpusControlEvent>(type, beat_key, adj_position)
                val parent_size = tree.parent?.size ?: 0

                this.controller_line_remove(type, beat_key, adj_position)
                if (parent_size <= 2) { // Will be pruned
                    adj_position.removeLast()
                } else if (adj_position.last() == parent_size - 1) {
                    adj_position[adj_position.size - 1] -= 1
                }
            }
        }
    }

    fun clear_history() {
        this.history_cache.clear()
    }

    private fun <T> push_replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<out InstrumentEvent>? = null, callback: () -> T): T {
        return if (!this.history_cache.isLocked()) {
            val use_tree = tree ?: this.get_tree_copy(beat_key, position)
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

    private fun <T> push_replace_global_ctl(type: ControlEventType, beat: Int, position: List<Int>, callback: () -> T): T {
        return if (!this.history_cache.isLocked()) {
            val use_tree = this.get_global_ctl_tree<OpusControlEvent>(type, beat, position).copy()

            val output = callback()

            this.push_to_history_stack(
                HistoryToken.REPLACE_GLOBAL_CTL_TREE,
                listOf(type, beat, position.toList(), use_tree)
            )

            output
        } else {
            callback()
        }
    }

    private fun <T> push_replace_channel_ctl(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, callback: () -> T): T {
        return if (!this.history_cache.isLocked()) {
            val use_tree = this.get_channel_ctl_tree<OpusControlEvent>(type, channel, beat, position).copy()

            val output = callback()

            this.push_to_history_stack(
                HistoryToken.REPLACE_CHANNEL_CTL_TREE,
                listOf(type, channel, beat, position.toList(), use_tree)
            )
            output
        } else {
            callback()
        }
    }

    private fun <T> push_replace_line_ctl(type: ControlEventType, beat_key: BeatKey, position: List<Int>, callback: () -> T): T {
        return if (!this.history_cache.isLocked()) {
            val use_tree = this.get_line_ctl_tree<OpusControlEvent>(type, beat_key, position).copy()

            val output = callback()

            this.push_to_history_stack(
                HistoryToken.REPLACE_LINE_CTL_TREE,
                listOf(type, beat_key.copy(), position.toList(), use_tree)
            )

            output
        } else {
            callback()
        }
    }

    private fun <T> push_rebuild_channel(channel: Int, callback: () -> T): T {
        // Should Never be called on percussion channel so no need to do percussion checks
        return this._remember {
            val tmp_history_nodes = mutableListOf<Pair<HistoryToken, List<Any>>>()
            val working_channel = if (this.is_percussion(channel)) {
                // TODO: Specify Exception
                throw Exception()
            } else {
                this.channels[channel]
            }

            val line_count = working_channel.lines.size

            // Will be an extra empty line that needs to be removed
            tmp_history_nodes.add(Pair(HistoryToken.REMOVE_LINE, listOf(channel, line_count)))

            for (i in line_count - 1 downTo 0) {
                tmp_history_nodes.add(
                    Pair(
                        HistoryToken.INSERT_LINE,
                        listOf(channel, i, working_channel.lines[i])
                    )
                )
            }



            tmp_history_nodes.add(
                Pair(
                    HistoryToken.NEW_CHANNEL,
                    listOf(
                        channel,
                        working_channel.uuid,
                        working_channel.get_midi_bank(),
                        working_channel.get_midi_program()
                    )
                )
            )

            for ((token, args) in tmp_history_nodes) {
                this.push_to_history_stack(token, args)
            }

            callback()
        }
    }

    private fun push_controller_global_remove(type: ControlEventType, beat: Int, position: List<Int>) {
        if (position.isNotEmpty()) {
            val stamp_position = position.toMutableList()
            val parent_position = position.subList(0, position.size - 1)
            val parent = this.get_global_ctl_tree<OpusControlEvent>(type, beat, parent_position)
            if (stamp_position.last() >= parent.size - 1 && parent.size > 1) {
                stamp_position[stamp_position.size - 1] = parent.size - 2
            }
            this.push_to_history_stack(HistoryToken.REMOVE_CTL_GLOBAL, listOf(type, beat, position))
        }
    }

    private fun push_controller_channel_remove(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        if (position.isNotEmpty()) {
            val stamp_position = position.toMutableList()
            val parent_position = position.subList(0, position.size - 1)
            val parent = this.get_channel_ctl_tree<OpusControlEvent>(type, channel, beat, parent_position)
            if (stamp_position.last() >= parent.size - 1 && parent.size > 1) {
                stamp_position[stamp_position.size - 1] = parent.size - 2
            }
            this.push_to_history_stack(HistoryToken.REMOVE_CTL_CHANNEL, listOf(type, channel, beat, position))
        }
    }

    private fun push_controller_line_remove(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        if (position.isNotEmpty()) {
            val stamp_position = position.toMutableList()
            val parent_position = position.subList(0, position.size - 1)
            val parent = this.get_line_ctl_tree<OpusControlEvent>(type, beat_key, parent_position)
            if (stamp_position.last() >= parent.size - 1 && parent.size > 1) {
                stamp_position[stamp_position.size - 1] = parent.size - 2
            }
            this.push_to_history_stack(HistoryToken.REMOVE_CTL_LINE, listOf(type, beat_key, position))
        }
    }

    private fun push_remove(beat_key: BeatKey, position: List<Int>) {
        // Call AFTER insert

        if (position.isNotEmpty()) {
            val stamp_position = position.toMutableList()
            val parent_position = position.subList(0, position.size - 1)
            val parent = this.get_tree(beat_key, parent_position)
            //if (stamp_position.last() >= parent.size - 1 && parent.size > 1) {
            //    stamp_position[stamp_position.size - 1] = parent.size - 2
            //}
            this.push_to_history_stack( HistoryToken.REMOVE, listOf(beat_key.copy(), stamp_position) )
        }
    }

    private fun push_set_event(beat_key: BeatKey, position: List<Int>, event: InstrumentEvent) {
        this.push_to_history_stack( HistoryToken.SET_EVENT, listOf(beat_key.copy(), position, event.copy()) )
    }

    private fun push_set_percussion_event(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this.push_to_history_stack( HistoryToken.SET_PERCUSSION_EVENT, listOf(beat_key.copy(), position.toList(), duration) )
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

    private fun <T> _remember(callback: () -> T): T {
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

    private fun <T> _forget(callback: () -> T): T {
        return this.history_cache.forget {
            callback()
        }
    }

    open fun push_to_history_stack(token: HistoryToken, args: List<Any>) {
        this.history_cache.append_undoer(token, args)
    }

    open fun apply_history_node(current_node: HistoryCache.HistoryNode, depth: Int = 0) {
        try {
            when (current_node.token) {
                HistoryToken.SET_PROJECT_NAME -> {
                    this.set_project_name(current_node.args[0] as String)
                }

                HistoryToken.UNSET_PROJECT_NAME -> {
                    this.set_project_name(null)
                }

                HistoryToken.SET_EVENT -> {
                    this.set_event(
                        current_node.args[0] as BeatKey,
                        checked_cast<List<Int>>(current_node.args[1]),
                        current_node.args[2] as InstrumentEvent
                    )
                }

                HistoryToken.SET_PERCUSSION_EVENT -> {
                    val beat_key = current_node.args[0] as BeatKey
                    val position = checked_cast<List<Int>>(current_node.args[1])
                    this.percussion_set_event(beat_key, position)
                    this.set_duration(
                        beat_key,
                        position,
                        current_node.args[2] as Int
                    )
                }

                HistoryToken.UNSET -> {
                    this.unset(
                        current_node.args[0] as BeatKey,
                        checked_cast<List<Int>>(current_node.args[1])
                    )
                }

                HistoryToken.REPLACE_TREE -> {
                    val beatkey = current_node.args[0] as BeatKey
                    val position = checked_cast<List<Int>>(current_node.args[1]).toList()
                    val tree = checked_cast<OpusTree<InstrumentEvent>>(current_node.args[2])
                    this.replace_tree(beatkey, position, tree)
                }

                HistoryToken.REPLACE_GLOBAL_CTL_TREE -> {
                    this.controller_global_replace_tree(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as Int,
                        checked_cast<List<Int>>(current_node.args[2]).toList(),
                        checked_cast<OpusTree<OpusControlEvent>>(current_node.args[3])
                    )
                }

                HistoryToken.REPLACE_CHANNEL_CTL_TREE -> {
                    this.controller_channel_replace_tree(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as Int,
                        current_node.args[2] as Int,
                        checked_cast<List<Int>>(current_node.args[3]).toList(),
                        checked_cast<OpusTree<OpusControlEvent>>(current_node.args[4])
                    )
                }

                HistoryToken.REPLACE_LINE_CTL_TREE -> {
                    this.controller_line_replace_tree(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as BeatKey,
                        checked_cast<List<Int>>(current_node.args[2]).toList(),
                        checked_cast<OpusTree<OpusControlEvent>>(current_node.args[3])
                    )
                }

                HistoryToken.SET_GLOBAL_CTL_INITIAL_EVENT -> {
                    this.controller_global_set_initial_event(
                        current_node.args[0] as ControlEventType,
                        checked_cast<OpusControlEvent>(current_node.args[1])
                    )
                }
                HistoryToken.SET_CHANNEL_CTL_INITIAL_EVENT -> {
                    this.controller_channel_set_initial_event(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as Int,
                        checked_cast<OpusControlEvent>(current_node.args[2])
                    )
                }

                HistoryToken.SET_LINE_CTL_INITIAL_EVENT -> {
                    this.controller_line_set_initial_event(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as Int,
                        current_node.args[2] as Int,
                        checked_cast<OpusControlEvent>(current_node.args[3])
                    )
                }

                HistoryToken.REMOVE_LINE -> {
                    this.remove_line(
                        current_node.args[0] as Int,
                        current_node.args[1] as Int
                    )
                }

                HistoryToken.INSERT_LINE -> {
                    this.insert_line(
                        current_node.args[0] as Int,
                        current_node.args[1] as Int,
                        checked_cast<OpusLine>(current_node.args[2])
                    )
                }
                HistoryToken.INSERT_LINE_PERCUSSION -> {
                    this.insert_line(
                        current_node.args[0] as Int,
                        current_node.args[1] as Int,
                        checked_cast<OpusLinePercussion>(current_node.args[2])
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

                    this.channel_set_instrument(channel, Pair(current_node.args[2] as Int, current_node.args[3] as Int))
                }

                HistoryToken.REMOVE -> {
                    this.remove(
                        current_node.args[0] as BeatKey,
                        checked_cast<List<Int>>(current_node.args[1])
                    )
                }

                HistoryToken.INSERT -> {
                    val beat_key = current_node.args[0] as BeatKey
                    val position = checked_cast<List<Int>>(current_node.args[1])
                    this.insert(beat_key, position)
                    this.replace_tree(
                        beat_key,
                        position,
                        checked_cast<OpusTree<InstrumentEvent>>(current_node.args[2]).copy()
                    )
                }

                HistoryToken.INSERT_CTL_GLOBAL -> {
                    val type = checked_cast<ControlEventType>(current_node.args[0])
                    val beat = current_node.args[1] as Int
                    val position = checked_cast<List<Int>>(current_node.args[2])
                    this.controller_global_insert(type, beat, position)
                    this.controller_global_replace_tree(
                        type,
                        beat,
                        position,
                        checked_cast<OpusTree<OpusControlEvent>>(current_node.args[3]).copy()
                    )
                }

                HistoryToken.INSERT_CTL_CHANNEL -> {
                    val type = checked_cast<ControlEventType>(current_node.args[0])
                    val channel = current_node.args[1] as Int
                    val beat = current_node.args[2] as Int
                    val position = checked_cast<List<Int>>(current_node.args[3])

                    this.controller_channel_insert(type, channel, beat, position)
                    this.controller_channel_replace_tree(
                        type,
                        channel,
                        beat,
                        position,
                        checked_cast<OpusTree<OpusControlEvent>>(current_node.args[4]).copy()
                    )
                }

                HistoryToken.INSERT_CTL_LINE -> {
                    val type = checked_cast<ControlEventType>(current_node.args[0])
                    val beat_key = checked_cast<BeatKey>(current_node.args[1])
                    val position = checked_cast<List<Int>>(current_node.args[2])

                    this.controller_line_insert(type, beat_key, position)
                    this.controller_line_replace_tree(
                        type,
                        beat_key,
                        position,
                        checked_cast<OpusTree<OpusControlEvent>>(current_node.args[3]).copy()
                    )
                }

                HistoryToken.REMOVE_CTL_GLOBAL -> {
                    this.controller_global_remove(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as Int,
                        checked_cast<List<Int>>(current_node.args[2])
                    )
                }
                HistoryToken.REMOVE_CTL_CHANNEL -> {
                    this.controller_channel_remove(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as Int,
                        current_node.args[2] as Int,
                        checked_cast<List<Int>>(current_node.args[3])
                    )
                }
                HistoryToken.REMOVE_CTL_LINE -> {
                    this.controller_line_remove(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as BeatKey,
                        checked_cast<List<Int>>(current_node.args[2])
                    )
                }


                HistoryToken.REMOVE_BEATS -> {
                    this.remove_beat(
                        current_node.args[0] as Int,
                        current_node.args[1] as Int
                    )
                }

                HistoryToken.INSERT_BEAT -> {
                    val instrument_events = checked_cast<List<OpusTree<OpusEvent>>>(current_node.args[1])
                    val control_events = checked_cast<Triple<List<Triple<Pair<Int, Int>, ControlEventType, OpusTree<OpusControlEvent>>>, List<Triple<Int, ControlEventType, OpusTree<OpusControlEvent>>>, List<Pair<ControlEventType, OpusTree<OpusControlEvent>>>>>(current_node.args[2])
                    val beat_index = current_node.args[0] as Int
                    this.insert_beat(beat_index, instrument_events)

                    // re-add line control events
                    for ((line_pair, type, tree) in control_events.first) {
                        this.controller_line_replace_tree(type, BeatKey(line_pair.first, line_pair.second, beat_index), listOf(), tree)
                    }

                    // re-add channel control events
                    for ((channel, type, tree) in control_events.second) {
                        this.controller_channel_replace_tree(type, channel, beat_index, listOf(), tree)
                    }

                    // re-add global control events
                    for ((type, tree) in control_events.third) {
                        this.controller_global_replace_tree(type, beat_index, listOf(), tree)
                    }

                    // Need to manually set the column
                    this.cursor_select_column(beat_index)
                }

                HistoryToken.SET_TRANSPOSE -> {
                    this.set_transpose(current_node.args[0] as Int)
                }

                HistoryToken.SET_TUNING_MAP -> {
                    this.set_tuning_map(
                        checked_cast<Array<Pair<Int, Int>>>(current_node.args[0]),
                        false
                    )
                }

                HistoryToken.SET_CHANNEL_INSTRUMENT -> {
                    this.channel_set_instrument(
                        current_node.args[0] as Int,
                        checked_cast<Pair<Int, Int>>(current_node.args[1])
                    )
                }

                HistoryToken.SET_PERCUSSION_INSTRUMENT -> {
                    this.percussion_set_instrument(
                        current_node.args[0] as Int, // line
                        current_node.args[1] as Int // Instrument
                    )
                }

                HistoryToken.SET_EVENT_DURATION -> {
                    this.set_duration(
                        current_node.args[0] as BeatKey,
                        checked_cast<List<Int>>(current_node.args[1]),
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

                HistoryToken.SET_GLOBAL_CTL_VISIBILITY -> {
                    this.set_global_controller_visibility(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as Boolean
                    )
                }
                HistoryToken.SET_CHANNEL_CTL_VISIBILITY -> {
                    this.set_channel_controller_visibility(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as Int,
                        current_node.args[2] as Boolean
                    )
                }
                HistoryToken.SET_LINE_CTL_VISIBILITY -> {
                    this.set_line_controller_visibility(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as Int,
                        current_node.args[2] as Int,
                        current_node.args[3] as Boolean
                    )
                }
                HistoryToken.REMOVE_LINE_CONTROLLER -> {
                    this.remove_line_controller(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as Int,
                        current_node.args[2] as Int
                    )
                }
                HistoryToken.REMOVE_GLOBAL_CONTROLLER -> {
                    this.remove_global_controller(
                        current_node.args[0] as ControlEventType
                    )
                }
                HistoryToken.REMOVE_CHANNEL_CONTROLLER -> {
                    this.remove_channel_controller(
                        current_node.args[0] as ControlEventType,
                        current_node.args[1] as Int
                    )
                }
                HistoryToken.NEW_LINE_CONTROLLER -> {
                    val type = current_node.args[0] as ControlEventType
                    val channel = current_node.args[1] as Int
                    val line_offset = current_node.args[2] as Int
                    this.new_line_controller(type, channel, line_offset)
                    this.set_line_controller_visibility(type, channel, line_offset, current_node.args[3] as Boolean)
                }
                HistoryToken.NEW_GLOBAL_CONTROLLER -> {
                    this.new_global_controller(
                        current_node.args[0] as ControlEventType
                    )
                }
                HistoryToken.NEW_CHANNEL_CONTROLLER -> {
                    val type = current_node.args[0] as ControlEventType
                    val channel = current_node.args[1] as Int
                    this.new_channel_controller(type, channel)
                    this.set_channel_controller_visibility(type, channel, current_node.args[2] as Boolean)
                }
                HistoryToken.SET_CHANNEL_VISIBILITY -> {
                    this.set_channel_visibility(
                        current_node.args[0] as Int,
                        current_node.args[1] as Boolean
                    )
                }

                //HistoryToken.CURSOR_SELECT_RANGE -> {
                //    var args = checked_cast<List<Int>>(current_node.args)
                //    val beat_key_a = BeatKey(args[0], args[1], args[2])
                //    val beat_key_b = BeatKey(args[3], args[4], args[5])
                //    if (beat_key_a != beat_key_b) {
                //        this.queue_cursor_select(
                //            OpusManagerCursor(
                //                mode = OpusManagerCursor.CursorMode.Range,
                //                range = Pair(beat_key_a, beat_key_b)
                //            )
                //        )
                //    } else {
                //        this.queue_cursor_select(
                //            OpusManagerCursor(
                //                mode = OpusManagerCursor.CursorMode.Single,
                //                channel = beat_key_a.channel,
                //                line_offset = beat_key_a.line_offset,
                //                beat = beat_key_a.beat
                //            )
                //        )
                //    }
                //}
                //HistoryToken.CURSOR_SELECT_LINE -> {
                //    var args = checked_cast<List<Int>>(current_node.args)
                //    this.queue_cursor_select(
                //        OpusManagerCursor(
                //            mode = OpusManagerCursor.CursorMode.Line,
                //            channel = args[0],
                //            line_offset =  args[1]
                //        )
                //    )
                //}

                //HistoryToken.CURSOR_SELECT -> {
                //    val beat_key = current_node.args[0] as BeatKey
                //    val args = mutableListOf<Int>(beat_key.channel, beat_key.line_offset, beat_key.beat)
                //    val position = checked_cast<List<Int>>(current_node.args[1])
                //    args.addAll(position)

                //    this.queue_cursor_select(
                //        OpusManagerCursor(
                //            mode = OpusManagerCursor.CursorMode.Single,
                //            channel = beat_key.channel,
                //            line_offset = beat_key.line_offset,
                //            beat = beat_key.beat,
                //            position = position
                //        )
                //    )
                //}

                //HistoryToken.CURSOR_SELECT_COLUMN -> {
                //    this.queue_cursor_select(
                //        OpusManagerCursor(
                //            mode = OpusManagerCursor.CursorMode.Column,
                //            beat = current_node.args[0] as Int
                //        )
                //    )
                //}

                //HistoryToken.CURSOR_SELECT_GLOBAL_CTL -> {
                //    this.queue_cursor_select(
                //        OpusManagerCursor(
                //            mode = OpusManagerCursor.CursorMode.Single,
                //            beat = current_node.args[1] as Int,
                //            position = checked_cast<List<Int>>(current_node.args[2]),
                //            ctl_level = CtlLineLevel.Global,
                //            ctl_type = checked_cast<ControlEventType>(current_node.args[0])
                //        )
                //    )
                //}
                //HistoryToken.CURSOR_SELECT_CHANNEL_CTL -> {
                //    this.queue_cursor_select(
                //        OpusManagerCursor(
                //            mode = OpusManagerCursor.CursorMode.Single,
                //            channel = current_node.args[1] as Int,
                //            beat = current_node.args[2] as Int,
                //            position = checked_cast<List<Int>>(current_node.args[3]),
                //            ctl_level = CtlLineLevel.Channel,
                //            ctl_type = checked_cast<ControlEventType>(current_node.args[0])
                //        )
                //    )
                //}
                //HistoryToken.CURSOR_SELECT_LINE_CTL -> {
                //    val beat_key = checked_cast<BeatKey>(current_node.args[1])
                //    this.queue_cursor_select(
                //        OpusManagerCursor(
                //            mode = OpusManagerCursor.CursorMode.Single,
                //            channel = beat_key.channel,
                //            line_offset = beat_key.line_offset,
                //            beat = beat_key.beat,
                //            position = checked_cast<List<Int>>(current_node.args[2]),
                //            ctl_level = CtlLineLevel.Line,
                //            ctl_type = checked_cast<ControlEventType>(current_node.args[0])
                //        )
                //    )
                //}
                //HistoryToken.CURSOR_SELECT_LINE_CTL_ROW -> {
                //    this.queue_cursor_select(
                //        OpusManagerCursor(
                //            mode = OpusManagerCursor.CursorMode.Single,
                //            channel = current_node.args[1] as Int,
                //            line_offset = current_node.args[2] as Int,
                //            ctl_level = CtlLineLevel.Line,
                //            ctl_type = checked_cast<ControlEventType>(current_node.args[0])
                //        )
                //    )
                //}
                //HistoryToken.CURSOR_SELECT_CHANNEL_CTL_ROW -> {
                //    this.queue_cursor_select(
                //        OpusManagerCursor(
                //            mode = OpusManagerCursor.CursorMode.Line,
                //            ctl_level = CtlLineLevel.Channel,
                //            channel = current_node.args[1] as Int,
                //            ctl_type = checked_cast<ControlEventType>(current_node.args[0])
                //        )
                //    )
                //}
                //HistoryToken.CURSOR_SELECT_GLOBAL_CTL_ROW -> {
                //    this.queue_cursor_select(
                //        OpusManagerCursor(
                //            mode = OpusManagerCursor.CursorMode.Line,
                //            ctl_level = CtlLineLevel.Global,
                //            ctl_type = checked_cast<ControlEventType>(current_node.args[0])
                //        )
                //    )
                //}
                //HistoryToken.CURSOR_SELECT_CHANNEL -> {
                //    this.queue_cursor_select(
                //        OpusManagerCursor(
                //            mode = OpusManagerCursor.CursorMode.Channel,
                //            channel = current_node.args[0] as Int,
                //            ctl_level = null,
                //            ctl_type = null
                //        )
                //    )
                //}

                HistoryToken.MULTI -> { /* Nothing */ }
                HistoryToken.SAVE_POINT -> TODO()
                HistoryToken.INSERT_TREE -> TODO()
                HistoryToken.MOVE_LINE -> TODO()
                //HistoryToken.CURSOR_SELECT -> TODO()
                //HistoryToken.CURSOR_SELECT_COLUMN -> TODO()
                //HistoryToken.CURSOR_SELECT_LINE -> TODO()
                //HistoryToken.CURSOR_SELECT_GLOBAL_CTL -> TODO()
                //HistoryToken.CURSOR_SELECT_CHANNEL_CTL -> TODO()
                //HistoryToken.CURSOR_SELECT_LINE_CTL -> TODO()
                //HistoryToken.CURSOR_SELECT_GLOBAL_CTL_ROW -> TODO()
                //HistoryToken.CURSOR_SELECT_CHANNEL_CTL_ROW -> TODO()
                //HistoryToken.CURSOR_SELECT_LINE_CTL_ROW -> TODO()
                //HistoryToken.CURSOR_SELECT_RANGE -> TODO()

                else -> {}
            }
        } catch (e: ClassCastException) {
            // pass
        }

        if (current_node.children.isNotEmpty()) {
            for (child in current_node.children.asReversed()) {
                this.apply_history_node(child, depth + 1)
            }
        }
    }

    open fun apply_undo(repeat: Int = 1) {
        this.history_cache.lock()

        for (i in 0 until repeat) {
            val node = this.history_cache.pop()
            if (node == null) {
                this.history_cache.unlock()
                return
            } else if (node.token == HistoryToken.MULTI && node.children.isEmpty()) {
                // If the node was an empty 'multi'  node, try the next one
                this.history_cache.unlock()
                this.apply_undo()
                return
            }

            this.apply_history_node(node)
        }

        this.history_cache.unlock()
    }

    override fun remove_line_repeat(channel: Int, line_offset: Int, count: Int) {
        this._remember {
            super.remove_line_repeat(channel, line_offset, count)
        }
    }

    // Need a compound function so history can manage both at the same time
    open fun set_tuning_map_and_transpose(tuning_map: Array<Pair<Int, Int>>, transpose: Int) {
        this._remember {
            this.set_tuning_map(tuning_map)
            this.set_transpose(transpose)
        }
    }

    // BASE FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    override fun new_line(channel: Int, line_offset: Int?) {
        this._remember {
            super.new_line(channel, line_offset)
            this.push_remove_line(
                channel,
                line_offset ?: (this.get_channel(channel).size - 1)
            )
        }
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusLineAbstract<*>) {
        this._remember {
            this.push_remove_line(channel, line_offset)
            super.insert_line(channel, line_offset, line)
        }
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {

        this._remember {
            super.swap_lines(channel_a, line_a, channel_b, line_b)
            this.push_to_history_stack(
                HistoryToken.SWAP_LINES,
                listOf(channel_a, line_a, channel_b, line_b)
            )
        }
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusLineAbstract<*> {
        return this._remember {
            val line = super.remove_line(channel, line_offset)

            if (this.is_percussion(channel)) {
                this.push_to_history_stack(
                    HistoryToken.INSERT_LINE_PERCUSSION,
                    listOf(channel, line_offset, line)
                )
            } else {
                this.push_to_history_stack(
                    HistoryToken.INSERT_LINE,
                    listOf(channel, line_offset, line)
                )
            }

            line
        }
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this._remember {
            super.insert_after(beat_key, position)

            val remove_position = position.toMutableList()
            remove_position[remove_position.size - 1] += 1
            this.push_remove(beat_key, remove_position)
        }
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        this._remember {
            super.insert(beat_key, position)
            this.push_remove(beat_key, position)
        }
    }

    override fun controller_line_insert(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_insert(type, beat_key, position)
    }

    override fun controller_channel_insert(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_insert(type, channel, beat, position)
    }

    override fun controller_global_insert(type: ControlEventType, beat: Int, position: List<Int>) {
        super.controller_global_insert(type, beat, position)
    }

    override fun controller_global_insert_after(type: ControlEventType, beat: Int, position: List<Int>) {
        this._remember {
            super.controller_global_insert_after(type, beat, position)
            val remove_position = position.toMutableList()
            remove_position[remove_position.size - 1] += 1
            this.push_controller_global_remove(type, beat, remove_position)
        }
    }

    override fun controller_channel_insert_after(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this._remember {
            super.controller_channel_insert_after(type, channel, beat, position)
            val remove_position = position.toMutableList()
            remove_position[remove_position.size - 1] += 1
            this.push_controller_channel_remove(type, channel, beat, remove_position)
        }
    }

    override fun controller_line_insert_after(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this._remember {
            super.controller_line_insert_after(type, beat_key, position)
            val remove_position = position.toMutableList()
            remove_position[remove_position.size - 1] += 1
            this.push_controller_line_remove(type, beat_key, remove_position)
        }
    }

    override fun cursor_apply(cursor: OpusManagerCursor) {
        super.cursor_apply(cursor)
    }

    override fun cursor_clear() {
        super.cursor_clear()
    }

    override fun cursor_select_channel(channel: Int) {
        super.cursor_select_channel(channel)
    }

    override fun cursor_select_line(channel: Int, line_offset: Int) {
        super.cursor_select_line(channel, line_offset)
    }

    override fun cursor_select_line_ctl_line(ctl_type: ControlEventType, channel: Int, line_offset: Int) {
        super.cursor_select_line_ctl_line(ctl_type, channel, line_offset)
    }

    override fun cursor_select_channel_ctl_line(ctl_type: ControlEventType, channel: Int) {
        super.cursor_select_channel_ctl_line(ctl_type, channel)
    }

    override fun cursor_select_global_ctl_line(ctl_type: ControlEventType) {
        super.cursor_select_global_ctl_line(ctl_type)
    }

    override fun cursor_select_column(beat: Int) {
        super.cursor_select_column(beat)
    }

    override fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        super.cursor_select(beat_key, position)
    }

    override fun cursor_select_ctl_at_line(ctl_type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.cursor_select_ctl_at_line(ctl_type, beat_key, position)
    }

    override fun cursor_select_ctl_at_channel(ctl_type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.cursor_select_ctl_at_channel(ctl_type, channel, beat, position)
    }

    override fun cursor_select_ctl_at_global(ctl_type: ControlEventType, beat: Int, position: List<Int>) {
        super.cursor_select_ctl_at_global(ctl_type, beat, position)
    }

    override fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        super.cursor_select_range(beat_key_a, beat_key_b)
    }

    override fun cursor_select_global_ctl_range(type: ControlEventType, first: Int, second: Int) {
        super.cursor_select_global_ctl_range(type, first, second)
    }

    override fun cursor_select_channel_ctl_range(type: ControlEventType, channel: Int, first: Int, second: Int) {
        super.cursor_select_channel_ctl_range(type, channel, first, second)
    }

    override fun cursor_select_line_ctl_range(type: ControlEventType, beat_key_a: BeatKey, beat_key_b: BeatKey) {
        super.cursor_select_line_ctl_range(type, beat_key_a, beat_key_b)
    }

    override fun remove_at_cursor(count: Int) {
        super.remove_at_cursor(count)
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this._remember {
            this.push_replace_tree(beat_key, position) {
                super.split_tree(beat_key, position, splits, move_event_to_end)
            }
        }
    }

    override fun controller_channel_split_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this._remember {
            this.push_replace_channel_ctl(type, channel, beat, position) {
                super.controller_channel_split_tree(type, channel, beat, position, splits, move_event_to_end)
            }
        }
    }

    override fun controller_global_split_tree(type: ControlEventType, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this._remember {
            this.push_replace_global_ctl(type, beat, position) {
                super.controller_global_split_tree(type, beat, position, splits, move_event_to_end)
            }
        }
    }

    override fun controller_line_split_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this._remember {
            this.push_replace_line_ctl(type, beat_key, position) {
                super.controller_line_split_tree(type, beat_key, position, splits, move_event_to_end)
            }
        }
    }

    override fun controller_global_remove_one_of_two(type: ControlEventType, beat: Int, position: List<Int>) {
        val parent_position = position.subList(0, position.size - 1)
        val use_tree = this.get_global_ctl_tree<OpusControlEvent>(type, beat, parent_position).copy()

        this._forget {
            super.controller_global_remove_one_of_two(type, beat, position)
        }

        this.push_to_history_stack(
            HistoryToken.REPLACE_GLOBAL_CTL_TREE,
            listOf(type, beat, parent_position.toList(), use_tree)
        )
    }

    override fun controller_channel_remove_one_of_two(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        val parent_position = position.subList(0, position.size - 1)
        val use_tree = this.get_channel_ctl_tree<OpusControlEvent>(type, channel, beat, parent_position).copy()

        this._forget {
            super.controller_channel_remove_one_of_two(type, channel, beat, position)
        }
        this.push_to_history_stack(
            HistoryToken.REPLACE_CHANNEL_CTL_TREE,
            listOf(type, channel, beat, parent_position.toList(), use_tree)
        )
    }

    override fun controller_line_remove_one_of_two(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        val parent_position = position.subList(0, position.size - 1)
        val use_tree = this.get_line_ctl_tree<OpusControlEvent>(type, beat_key, parent_position).copy()

        this._forget {
            super.controller_line_remove_one_of_two(type, beat_key, position)
        }

        this.push_to_history_stack(
            HistoryToken.REPLACE_LINE_CTL_TREE,
            listOf(type, beat_key, parent_position.toList(), use_tree)
        )
    }

    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        this._remember {
            val tree = this.get_tree_copy(beat_key, position)
            super.remove_standard(beat_key, position)
            this.push_to_history_stack(
                HistoryToken.INSERT,
                listOf(beat_key, position.toList(), tree)
            )
        }
    }

    override fun controller_global_remove_standard(type: ControlEventType, beat: Int, position: List<Int>) {
        this._remember {
            val tree = this.get_global_ctl_tree<OpusControlEvent>(type, beat, position).copy()
            super.controller_global_remove_standard(type, beat, position)
            this.push_to_history_stack(
                HistoryToken.INSERT_CTL_GLOBAL,
                listOf(type, beat, position.toList(), tree)
            )
        }
    }

    override fun controller_channel_remove_standard(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this._remember {
            val tree = this.get_channel_ctl_tree<OpusControlEvent>(type, channel, beat, position).copy()
            super.controller_channel_remove_standard(type, channel, beat, position)
            this.push_to_history_stack(
                HistoryToken.INSERT_CTL_CHANNEL,
                listOf(type, channel, beat, position.toList(), tree)
            )
        }
    }

    override fun controller_line_remove_standard(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this._remember {
            val tree = this.get_line_ctl_tree<OpusControlEvent>(type, beat_key, position).copy()
            super.controller_line_remove_standard(type, beat_key, position)
            this.push_to_history_stack(
                HistoryToken.INSERT_CTL_LINE,
                listOf(type, beat_key, position.toList(), tree)
            )
        }
    }

    override fun insert_beats(beat_index: Int, count: Int) {
        this._remember {
            this._forget {
                super.insert_beats(beat_index, count)
            }
            this.push_to_history_stack( HistoryToken.REMOVE_BEATS, listOf(beat_index, count) )
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        super.insert_beat(beat_index, beats_in_column)
        this.push_to_history_stack( HistoryToken.REMOVE_BEATS, listOf(beat_index, 1) )
    }

    override fun remove_beat(beat_index: Int, count: Int) {
        this._remember {
            val working_beat_index = min(beat_index + (count - 1), this.beat_count - 1)
            val beat_cells = List(count) { i: Int ->
                val working_list = mutableListOf<OpusTree<out InstrumentEvent>>()
                val working_line_controller_list = mutableListOf<Triple<Pair<Int, Int>, ControlEventType, OpusTree<out OpusControlEvent>>>()
                val working_channel_controller_list = mutableListOf<Triple<Int, ControlEventType, OpusTree<out OpusControlEvent>>>()
                val working_global_controller_list = mutableListOf<Pair<ControlEventType, OpusTree<out OpusControlEvent>>>()
                this.get_all_channels().forEachIndexed { c: Int, channel: OpusChannelAbstract<*,*> ->
                    val line_count = channel.size
                    for (j in 0 until line_count) {
                        working_list.add(
                            this.get_tree_copy(BeatKey(c, j, working_beat_index - i))
                        )
                        val controllers = channel.lines[j].controllers
                        for ((type, controller) in controllers.get_all()) {
                            working_line_controller_list.add(
                                Triple(
                                    Pair(c, j),
                                    type,
                                    controller.get_tree(working_beat_index - i)
                                )
                            )
                        }
                    }
                    val controllers = channel.controllers
                    for ((type, controller) in controllers.get_all()) {
                        working_channel_controller_list.add(Triple(c, type, controller.get_tree(working_beat_index - i)))
                    }
                }

                val controllers = this.controllers
                for ((type, controller) in controllers.get_all()) {
                    working_global_controller_list.add(Pair(type, controller.get_tree(working_beat_index - i)))
                }

                Pair(working_list, Triple(working_line_controller_list, working_channel_controller_list, working_global_controller_list))
            }

            super.remove_beat(beat_index, count)

            for (i in beat_cells.indices.reversed()) {
                this.push_to_history_stack(
                    HistoryToken.INSERT_BEAT,
                    listOf(working_beat_index - (count - 1), beat_cells[i].first, beat_cells[i].second)
                )
            }
        }
    }

    override fun <T: OpusControlEvent> controller_channel_replace_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>?, tree: OpusTree<T>) {
        this._remember {
            this.push_replace_channel_ctl(type, channel,beat, position ?: listOf()) {
                super.controller_channel_replace_tree(type, channel, beat, position, tree)
            }
        }
    }

    override fun <T: OpusControlEvent> controller_global_replace_tree(type: ControlEventType, beat: Int, position: List<Int>?, tree: OpusTree<T>) {
        this._remember {
            this.push_replace_global_ctl(type, beat, position ?: listOf()) {
                super.controller_global_replace_tree(type, beat, position, tree)
            }
        }
    }

    override fun <T: OpusControlEvent> controller_line_replace_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>?, tree: OpusTree<T>) {
        this._remember {
            this.push_replace_line_ctl(type, beat_key, position ?: listOf()) {
                super.controller_line_replace_tree(type, beat_key, position, tree)
            }
        }
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<out InstrumentEvent>) {
        this._remember {
            this.push_replace_tree(beat_key, position) {
                super.replace_tree(beat_key, position, tree)
            }
        }
    }

    override fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        this._remember {
            super.move_leaf(beatkey_from, position_from, beatkey_to, position_to)
        }
    }

    override fun controller_channel_move_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        this._remember {
            super.controller_channel_move_leaf(type, channel_from, beat_from, position_from, channel_to, beat_to, position_to)
        }
    }

    override fun controller_channel_to_line_move_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        this._remember {
            super.controller_channel_to_line_move_leaf(type, channel_from, beat_from, position_from, beat_key_to, position_to)
        }
    }

    override fun controller_global_to_line_move_leaf(type: ControlEventType, beat: Int, position: List<Int>, target_key: BeatKey, target_position: List<Int>) {
        this._remember {
            super.controller_global_to_line_move_leaf(type, beat, position, target_key, target_position)
        }
    }

    override fun controller_channel_to_global_move_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        super.controller_channel_to_global_move_leaf(type, channel_from, beat_from, position_from, target_beat, target_position)
    }

    override fun controller_global_move_leaf(type: ControlEventType, beat_from: Int, position_from: List<Int>, beat_to: Int, position_to: List<Int>) {
        this._remember {
            super.controller_global_move_leaf(type, beat_from, position_from, beat_to, position_to)
        }
    }

    override fun controller_line_to_global_move_leaf(type: ControlEventType, beatkey_from: BeatKey, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        super.controller_line_to_global_move_leaf(type, beatkey_from, position_from, target_beat, target_position)
    }

    override fun controller_global_to_channel_move_leaf(type: ControlEventType, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        super.controller_global_to_channel_move_leaf(type, beat_from, position_from, channel_to, beat_to, position_to)
    }

    override fun controller_line_move_leaf(type: ControlEventType, beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        this._remember {
            super.controller_line_move_leaf(type, beatkey_from, position_from, beatkey_to, position_to)
        }
    }

    override fun _controller_global_copy_range(type: ControlEventType, target: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        this._remember {
            super._controller_global_copy_range(type, target, point_a, point_b, unset_original)
        }
    }

    override fun _controller_global_to_channel_copy_range(type: ControlEventType, target_channel: Int, target_beat: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        this._remember {
            super._controller_global_to_channel_copy_range(type, target_channel, target_beat, point_a, point_b, unset_original)
        }
    }

    override fun _controller_global_to_line_copy_range(type: ControlEventType, beat_a: Int, beat_b: Int, target_key: BeatKey, unset_original: Boolean) {
        this._remember {
            super._controller_global_to_line_copy_range(type, beat_a, beat_b, target_key, unset_original)
        }
    }

    override fun _controller_channel_to_global_copy_range(type: ControlEventType, target_beat: Int, original_channel: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        this._remember {
            super._controller_channel_to_global_copy_range(type, target_beat, original_channel, point_a, point_b, unset_original)
        }
    }

    override fun _controller_channel_copy_range(type: ControlEventType, target_channel: Int, target_beat: Int, original_channel: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        this._remember {
            super._controller_channel_copy_range(type, target_channel, target_beat, original_channel, point_a, point_b, unset_original)
        }
    }

    override fun _controller_channel_to_line_copy_range(type: ControlEventType, channel_from: Int, beat_a: Int, beat_b: Int, target_key: BeatKey, unset_original: Boolean) {
        this._remember {
            super._controller_channel_to_line_copy_range(type, channel_from, beat_a, beat_b, target_key, unset_original)
        }
    }

    override fun _controller_line_to_global_copy_range(type: ControlEventType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_beat: Int, unset_original: Boolean) {
        this._remember {
            super._controller_line_to_global_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_beat, unset_original)
        }
    }

    override fun _controller_line_to_channel_copy_range(type: ControlEventType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_channel: Int, target_beat: Int, unset_original: Boolean) {
        this._remember {
            super._controller_line_to_channel_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_channel, target_beat, unset_original)
        }
    }

    override fun _controller_line_copy_range(type: ControlEventType, beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey, unset_original: Boolean) {
        this._remember {
            super._controller_line_copy_range(type, beat_key, first_corner, second_corner, unset_original)
        }
    }

    override fun controller_line_to_channel_move_leaf(type: ControlEventType, beatkey_from: BeatKey, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        this._remember {
            super.controller_line_to_channel_move_leaf(type, beatkey_from, position_from, channel_to, beat_to, position_to)
        }
    }

    override fun move_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        this._remember {
            super.move_beat_range(beat_key, first_corner, second_corner)
        }
    }

    override fun unset_line(channel: Int, line_offset: Int) {
        this._remember {
            super.unset_line(channel, line_offset)
        }
    }

    override fun controller_line_unset_line(type: ControlEventType, channel: Int, line_offset: Int) {
        this._remember {
            super.controller_line_unset_line(type, channel, line_offset)
        }
    }

    override fun controller_channel_unset_line(type: ControlEventType, channel: Int) {
        this._remember {
            super.controller_channel_unset_line(type, channel)
        }
    }

    override fun controller_global_unset_line(type: ControlEventType) {
        this._remember {
            super.controller_global_unset_line(type)
        }
    }

    override fun unset_range(first_corner: BeatKey, second_corner: BeatKey) {
        this._remember {
            super.unset_range(first_corner, second_corner)
        }
    }

    override fun controller_channel_unset_range(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int) {
        this._remember {
            super.controller_channel_unset_range(type, channel, first_beat, second_beat)
        }
    }

    override fun controller_global_unset_range(type: ControlEventType, first_beat: Int, second_beat: Int) {
        this._remember {
            super.controller_global_unset_range(type, first_beat, second_beat)
        }
    }

    override fun controller_line_unset_range(type: ControlEventType, first_corner: BeatKey, second_corner: BeatKey) {
        this._remember {
            super.controller_line_unset_range(type, first_corner, second_corner)
        }
    }

    override fun <T: InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        this._remember {
            this.push_replace_tree(beat_key, position) {
                super.set_event(beat_key, position, event)
            }
        }
    }

    override fun <T: OpusControlEvent> controller_line_set_event(type: ControlEventType, beat_key: BeatKey, position: List<Int>, event: T) {
        // Trivial?
        if (this.get_line_ctl_tree<T>(type, beat_key, position).get_event() == event) {
            return
        }

        this._remember {
            this.push_replace_line_ctl(type, beat_key, position) {
                super.controller_line_set_event(type, beat_key, position, event)
            }
        }
    }

    override fun <T: OpusControlEvent> controller_channel_set_event(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, event: T) {
        // Trivial?
        if (this.get_channel_ctl_tree<T>(type, channel, beat, position).get_event() == event) {
            return
        }

        this._remember {
            this.push_replace_channel_ctl(type, channel, beat, position) {
                super.controller_channel_set_event(type, channel, beat, position, event)
            }
        }
    }

    override fun <T: OpusControlEvent> controller_global_set_event(type: ControlEventType, beat: Int, position: List<Int>, event: T) {
        this._remember {
            this.push_replace_global_ctl(type, beat, position) {
                super.controller_global_set_event(type, beat, position, event)
            }
        }
    }

    override fun percussion_set_event(beat_key: BeatKey, position: List<Int>) {
        this._remember {
            super.percussion_set_event(beat_key, position)
            this.push_unset(beat_key, position)
        }
    }

    override fun unset_beat(beat: Int) {
        this._remember {
            super.unset_beat(beat)
        }
    }

    override fun controller_channel_unset(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this._remember {
            this.push_replace_channel_ctl(type, channel, beat, position) {
                super.controller_channel_unset(type, channel, beat, position)
            }
        }
    }

    override fun controller_global_unset(type: ControlEventType, beat: Int, position: List<Int>) {
        this._remember {
            this.push_replace_global_ctl(type, beat, position) {
                super.controller_global_unset(type, beat, position)
            }
        }
    }

    override fun controller_line_unset(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this._remember {
            this.push_replace_line_ctl(type, beat_key, position) {
                super.controller_line_unset(type, beat_key, position)
            }
        }
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this._remember {
            val tree = this.get_tree_copy(beat_key, position)
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
                    val duration = (tree.get_event() as InstrumentEvent).duration
                    super.unset(beat_key, position)
                    this.push_set_percussion_event(beat_key, position, duration)
                }
            } else if (!tree.is_leaf()) {
                this.push_replace_tree(beat_key, position, tree) {
                    super.unset(beat_key, position)
                }
            } else {
                super.unset(beat_key, position)
            }
        }
    }

    override fun project_change_wrapper(callback: () -> Unit) {
        this.history_cache.forget {
            super.project_change_wrapper(callback)
        }
    }

    override fun clear() {
        this.clear_history()
        this._forget {
            super.clear()
        }
    }

    override fun on_project_changed() {
        super.on_project_changed()
    }

    override fun load(bytes: ByteArray, new_path: String?) {
        super.load(bytes, new_path)
    }

    override fun remove_channel(channel: Int) {
        this.push_rebuild_channel(channel) {
            //this.history_cache.lock()
            super.remove_channel(channel)
            // this.history_cache.unlock()
        }
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        this._remember {
            val channel_to_remove = channel ?: this.channels.size

            super.new_channel(channel, lines, uuid)

            this.push_to_history_stack(
                HistoryToken.REMOVE_CHANNEL,
                listOf(this.channels[channel_to_remove].uuid)
            )
        }
    }

    override fun set_project_name(new_name: String?) {
        if (this.project_name == null) {
            this.push_to_history_stack(HistoryToken.UNSET_PROJECT_NAME, listOf())
        } else {
            this.push_to_history_stack(HistoryToken.SET_PROJECT_NAME, listOf(this.project_name!!))
        }
        super.set_project_name(new_name)
    }

    override fun set_transpose(new_transpose: Int) {
        this.push_to_history_stack(HistoryToken.SET_TRANSPOSE, listOf(this.transpose))
        super.set_transpose(new_transpose)
    }

    override fun channel_set_instrument(channel: Int, instrument: Pair<Int, Int>) {
        this._remember {
            this.push_to_history_stack(
                HistoryToken.SET_CHANNEL_INSTRUMENT,
                listOf(channel, this.get_channel_instrument(channel))
            )
            super.channel_set_instrument(channel, instrument)
        }
    }

    override fun percussion_set_instrument(line_offset: Int, instrument: Int) {
        this._remember {
            val current = this.get_percussion_instrument(line_offset)
            this.push_to_history_stack(HistoryToken.SET_PERCUSSION_INSTRUMENT, listOf(line_offset, current))
            super.percussion_set_instrument(line_offset, instrument)
        }
    }

    override fun overwrite_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        this._remember {
            super.overwrite_beat_range(beat_key, first_corner, second_corner)
        }

    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this._remember {
            val tree = this.get_tree(beat_key, position)
            if (tree.is_event()) {
                val event = tree.get_event()
                this.push_to_history_stack(HistoryToken.SET_EVENT_DURATION, listOf(beat_key, position, event!!.duration))
            }

            super.set_duration(beat_key, position, duration)
        }
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)
    }

    override fun controller_line_remove(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_remove(type, beat_key, position)
    }

    override fun controller_channel_remove(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_remove(type, channel, beat, position)
    }

    override fun controller_global_remove(type: ControlEventType, beat: Int, position: List<Int>) {
        super.controller_global_remove(type, beat, position)
    }

    override fun overwrite_line(channel: Int, line_offset: Int, beat_key: BeatKey) {
        this._remember {
            super.overwrite_line(channel, line_offset, beat_key)
        }
    }

    override fun controller_line_overwrite_line(type: ControlEventType, channel: Int, line_offset: Int, beat_key: BeatKey) {
        this._remember {
            super.controller_line_overwrite_line(type, channel, line_offset, beat_key)
        }
    }

    override fun controller_channel_overwrite_line(type: ControlEventType, target_channel: Int, original_channel: Int, original_beat: Int) {
        this._remember {
            super.controller_channel_overwrite_line(type, target_channel, original_channel, original_beat)
        }
    }

    override fun controller_line_to_channel_overwrite_line(type: ControlEventType, target_channel: Int, original_key: BeatKey) {
        super.controller_line_to_channel_overwrite_line(type, target_channel, original_key)
    }

    override fun controller_global_to_channel_overwrite_line(type: ControlEventType, target_channel: Int, beat: Int) {
        super.controller_global_to_channel_overwrite_line(type, target_channel, beat)
    }

    override fun controller_global_overwrite_line(type: ControlEventType, beat: Int) {
        this._remember {
            super.controller_global_overwrite_line(type, beat)
        }
    }

    override fun controller_channel_to_global_overwrite_line(type: ControlEventType, channel: Int, beat: Int) {
        this._remember {
            super.controller_channel_to_global_overwrite_line(type, channel, beat)
        }
    }

    override fun controller_line_to_global_overwrite_line(type: ControlEventType, beat_key: BeatKey) {
        this._remember {
            super.controller_line_to_global_overwrite_line(type, beat_key)
        }
    }

    override fun controller_global_to_line_overwrite_line(type: ControlEventType, from_beat: Int, target_channel: Int, target_line_offset: Int) {

        this._remember {
            super.controller_global_to_line_overwrite_line(type, from_beat, target_channel, target_line_offset)
        }
    }

    override fun controller_channel_to_line_overwrite_line(type: ControlEventType, target_channel: Int, target_line_offset: Int, original_channel: Int, original_beat: Int) {
        this._remember {
            super.controller_channel_to_line_overwrite_line(type, target_channel, target_line_offset, original_channel, original_beat)
        }
    }

    override fun overwrite_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {
        this._remember {
            super.overwrite_beat_range_horizontally(channel, line_offset, first_key, second_key)
        }
    }

    override fun controller_global_overwrite_range_horizontally(type: ControlEventType, first_beat: Int, second_beat: Int) {
        this._remember {
            super.controller_global_overwrite_range_horizontally(type, first_beat, second_beat)
        }
    }

    override fun controller_global_to_line_overwrite_range_horizontally(type: ControlEventType, target_channel: Int, target_line_offset: Int, first_beat: Int, second_beat: Int) {
        this._remember {
            super.controller_global_to_line_overwrite_range_horizontally(type, target_channel, target_line_offset, first_beat, second_beat)
        }
    }

    override fun controller_line_to_channel_overwrite_range_horizontally(type: ControlEventType, channel: Int, first_key: BeatKey, second_key: BeatKey) {
        this._remember {
            super.controller_line_to_channel_overwrite_range_horizontally(type, channel, first_key, second_key)
        }
    }

    override fun controller_global_to_channel_overwrite_range_horizontally(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int) {
        this._remember {
            super.controller_global_to_channel_overwrite_range_horizontally(type, channel, first_beat, second_beat)
        }
    }

    override fun controller_line_overwrite_range_horizontally(type: ControlEventType, channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {
        this._remember {
            super.controller_line_overwrite_range_horizontally(type, channel, line_offset, first_key, second_key)
        }
    }

    override fun controller_line_to_global_overwrite_range_horizontally(type: ControlEventType, channel: Int, line_offset: Int, first_beat: Int, second_beat: Int) {
        this._remember {
            super.controller_line_to_global_overwrite_range_horizontally(type, channel, line_offset, first_beat, second_beat)
        }
    }

    override fun controller_channel_to_global_overwrite_range_horizontally(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int) {
        this._remember {
            super.controller_channel_to_global_overwrite_range_horizontally(type, channel, first_beat, second_beat)
        }
    }

    override fun controller_channel_overwrite_range_horizontally(type: ControlEventType, target_channel: Int, from_channel: Int, first_beat: Int, second_beat: Int) {
        this._remember {
            super.controller_channel_overwrite_range_horizontally(type, target_channel, from_channel, first_beat, second_beat)
        }
    }

    override fun controller_channel_to_line_overwrite_range_horizontally(type: ControlEventType, target_channel: Int, target_line_offset: Int, from_channel: Int, first_beat: Int, second_beat: Int) {
        this._remember {
            super.controller_channel_to_line_overwrite_range_horizontally(type, target_channel, target_line_offset, from_channel, first_beat, second_beat)
        }
    }

    override fun set_tuning_map(new_map: Array<Pair<Int, Int>>, mod_events: Boolean) {
        this._remember {
            val original_map = this.tuning_map.clone()
            super.set_tuning_map(new_map, mod_events)
            this.push_to_history_stack(HistoryToken.SET_TUNING_MAP, listOf(original_map))
        }
    }

    override fun <T: OpusControlEvent> controller_global_set_initial_event(type: ControlEventType, event: T) {
        this._remember {
            this.push_to_history_stack(
                HistoryToken.SET_GLOBAL_CTL_INITIAL_EVENT,
                listOf(
                    type,
                    this.controllers.get_controller<T>(type).initial_event
                )
            )
            super.controller_global_set_initial_event(type, event)
        }
    }

    override fun <T: OpusControlEvent> controller_channel_set_initial_event(type: ControlEventType, channel: Int, event: T) {
        this._remember {
            this.push_to_history_stack(
                HistoryToken.SET_CHANNEL_CTL_INITIAL_EVENT,
                listOf(
                    type,
                    channel,
                    this.get_channel(channel).controllers.get_controller<T>(type).initial_event
                )
            )
            super.controller_channel_set_initial_event(type, channel, event)
        }
    }

    override fun <T: OpusControlEvent> controller_line_set_initial_event(type: ControlEventType, channel: Int, line_offset: Int, event: T) {
        this._remember {
            this.push_to_history_stack(
                HistoryToken.SET_LINE_CTL_INITIAL_EVENT,
                listOf(
                    type,
                    channel,
                    line_offset,
                    this.get_channel(channel).lines[line_offset].controllers.get_controller<T>(type).initial_event
                )
            )
            super.controller_line_set_initial_event(type, channel, line_offset, event)
        }
    }

    override fun <T : OpusLayerBase> import_from_other(other: T) {
        super.import_from_other(other)
    }

    override fun on_action_blocked(blocker_key: BeatKey, blocker_position: List<Int>) {
        super.on_action_blocked(blocker_key, blocker_position)
    }

    override fun on_action_blocked_global_ctl(type: ControlEventType, blocker_beat: Int, blocker_position: List<Int>) {
        super.on_action_blocked_global_ctl(type, blocker_beat, blocker_position)
    }

    override fun on_action_blocked_channel_ctl(type: ControlEventType, blocker_channel: Int, blocker_beat: Int, blocker_position: List<Int>) {
        super.on_action_blocked_channel_ctl(type, blocker_channel, blocker_beat, blocker_position)
    }

    override fun on_action_blocked_line_ctl(type: ControlEventType, blocker_key: BeatKey, blocker_position: List<Int>) {
        super.on_action_blocked_line_ctl(type, blocker_key, blocker_position)
    }

    override fun _project_change_new() {
        super._project_change_new()
    }

    override fun merge_leafs(beat_key_from: BeatKey, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        this._remember {
            super.merge_leafs(beat_key_from, position_from, beat_key_to, position_to)
        }
    }

    override fun set_beat_count(new_count: Int) {
        super.set_beat_count(new_count)
    }

    override fun save(path: String?) {
        super.save(path)
    }

    override fun to_json(): JSONHashMap {
        return super.to_json()
    }

    override fun _project_change_json(json_data: JSONHashMap) {
        super._project_change_json(json_data)
    }

    override fun _project_change_midi(midi: Midi) {
        super._project_change_midi(midi)
    }

    override fun recache_line_maps() {
        super.recache_line_maps()
    }

    override fun remove_global_controller(type: ControlEventType) {
        this._remember {
            if (this.has_global_controller(type)) {
                this.push_to_history_stack(
                    HistoryToken.NEW_GLOBAL_CONTROLLER,
                    listOf(type)
                )
            }
            super.remove_global_controller(type)
        }
    }

    override fun remove_line_controller(type: ControlEventType, channel_index: Int, line_offset: Int) {
        this._remember {
            if (this.has_line_controller(type, channel_index, line_offset)) {
                val controller = this.get_all_channels()[channel_index].lines[line_offset].get_controller<OpusControlEvent>(type)
                for (beat in controller.beats.indices) {
                    if (controller.beats[beat].is_leaf() && !controller.beats[beat].is_event()) {
                        continue
                    }

                    this.push_to_history_stack(
                        HistoryToken.REPLACE_LINE_CTL_TREE,
                        listOf(type, BeatKey(channel_index, line_offset, beat), listOf<Int>(), controller.beats[beat])
                    )
                }
                this.push_to_history_stack(
                    HistoryToken.NEW_LINE_CONTROLLER,
                    listOf(type, channel_index, line_offset, controller.visible)
                )
            }
            super.remove_line_controller(type, channel_index, line_offset)
        }
    }

    override fun remove_channel_controller(type: ControlEventType, channel_index: Int) {
        this._remember {
            if (this.has_channel_controller(type, channel_index)) {
                this.push_to_history_stack(
                    HistoryToken.NEW_CHANNEL_CONTROLLER,
                    listOf(type, channel_index, this.get_all_channels()[channel_index].controllers.get_controller<OpusControlEvent>(type).visible)
                )
            }
            super.remove_channel_controller(type, channel_index)
        }
    }

    override fun set_global_controller_visibility(type: ControlEventType, visibility: Boolean) {
        this._remember {
            val controller = this.controllers.get_controller<OpusControlEvent>(type)
            this.push_to_history_stack(
                HistoryToken.SET_GLOBAL_CTL_VISIBILITY,
                listOf(type, controller.visible)
            )

            super.set_global_controller_visibility(type, visibility)
        }
    }

    override fun set_channel_controller_visibility(type: ControlEventType, channel_index: Int, visibility: Boolean) {
        this._remember {
            val controller = this.get_all_channels()[channel_index].controllers.get_controller<OpusControlEvent>(type)
            this.push_to_history_stack(
                HistoryToken.SET_CHANNEL_CTL_VISIBILITY,
                listOf(type, channel_index, controller.visible)
            )

            super.set_channel_controller_visibility(type, channel_index, visibility)
        }
    }

    override fun set_line_controller_visibility(type: ControlEventType, channel_index: Int, line_offset: Int, visibility: Boolean) {
        this._remember {
            val controller = this.get_all_channels()[channel_index].lines[line_offset].controllers.get_controller<OpusControlEvent>(type)

            this.push_to_history_stack(
                HistoryToken.SET_LINE_CTL_VISIBILITY,
                listOf(type, channel_index, line_offset, controller.visible)
            )

            super.set_line_controller_visibility(type, channel_index, line_offset, visibility)
        }
    }

    override fun _apply_column_trees(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>) {
        super._apply_column_trees(beat_index, beats_in_column)
    }

    override fun new_channel_controller(type: ControlEventType, channel_index: Int) {
        this._remember {
            if (!this.has_channel_controller(type, channel_index)) {
                this.push_to_history_stack(
                    HistoryToken.REMOVE_CHANNEL_CONTROLLER,
                    listOf(type, channel_index)
                )

            }
            super.new_channel_controller(type, channel_index)
        }
    }

    override fun new_line_controller(type: ControlEventType, channel_index: Int, line_offset: Int) {
        this._remember {
            if (!this.has_line_controller(type, channel_index, line_offset)) {
                this.push_to_history_stack(
                    HistoryToken.REMOVE_LINE_CONTROLLER,
                    listOf(type, channel_index, line_offset)
                )
            }
            super.new_line_controller(type, channel_index, line_offset)
        }
    }

    override fun new_global_controller(type: ControlEventType) {
        this._remember {
            if (!this.has_global_controller(type)) {
                this.push_to_history_stack(
                    HistoryToken.REMOVE_GLOBAL_CONTROLLER,
                    listOf(type)
                )
            }
            super.new_global_controller(type)
        }
    }

    override fun set_channel_visibility(channel_index: Int, visibility: Boolean) {
        this._remember {
            if (this.get_all_channels()[channel_index].visible != visibility) {
                this.push_to_history_stack(
                    HistoryToken.SET_CHANNEL_VISIBILITY,
                    listOf(channel_index, !visibility)
                )
            }
            super.set_channel_visibility(channel_index, visibility)
        }
    }

    override fun convert_events_in_line_to_absolute(channel: Int, line_offset: Int) {
        this._remember {
            super.convert_events_in_line_to_absolute(channel, line_offset)
        }
    }

    override fun convert_events_in_tree_to_absolute(beat_key: BeatKey, position: List<Int>) {
        this._remember {
            super.convert_events_in_tree_to_absolute(beat_key, position)
        }
    }

    override fun convert_events_in_beat_to_absolute(beat: Int) {
        this._remember {
            super.convert_events_in_beat_to_absolute(beat)
        }
    }

    override fun convert_events_in_line_to_relative(channel: Int, line_offset: Int) {
        this._remember {
            super.convert_events_in_line_to_relative(channel, line_offset)
        }
    }

    override fun convert_events_in_tree_to_relative(beat_key: BeatKey, position: List<Int>) {
        this._remember {
            super.convert_events_in_tree_to_relative(beat_key, position)
        }
    }

    override fun convert_events_in_beat_to_relative(beat: Int) {
        this._remember {
            super.convert_events_in_beat_to_relative(beat)
        }
    }

    override fun convert_event_to_absolute(beat_key: BeatKey, position: List<Int>) {
        this._remember {
            super.convert_event_to_absolute(beat_key, position)
        }
    }

    override fun convert_event_to_relative(beat_key: BeatKey, position: List<Int>) {
        this._remember {
            super.convert_event_to_relative(beat_key, position)
        }
    }

    override fun toggle_line_controller_visibility(type: ControlEventType, channel_index: Int, line_offset: Int) {
        this._remember {
            super.toggle_line_controller_visibility(type, channel_index, line_offset)
        }
    }

    override fun toggle_global_control_visibility(type: ControlEventType) {
        this._remember {
            super.toggle_global_control_visibility(type)
        }
    }

    override fun toggle_channel_controller_visibility(type: ControlEventType, channel_index: Int) {
        this._remember {
            super.toggle_channel_controller_visibility(type, channel_index)
        }
    }

    // BASE FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    // HISTORY FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    // HISTORY FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

}
