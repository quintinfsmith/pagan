package com.qfs.pagan.opusmanager
import com.qfs.pagan.structure.OpusTree
import java.lang.Integer.max
import java.lang.Integer.min

open class OpusLayerCursor(): OpusLayerHistory() {
    var cursor = OpusManagerCursor()
    private var _queued_cursor_selection: Pair<HistoryToken, List<Int>>? = null


    override fun insert_line(channel: Int, line_offset: Int, line: OpusChannel.OpusLine) {
        // Need to clear cursor before change since the way the editor_table updates
        // Cursors doesn't take into account changes to row count
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        super.insert_line(channel, line_offset, line)

        if (bkp_cursor.mode == OpusManagerCursor.CursorMode.Row) {
            this.cursor_select_row(bkp_cursor.channel, bkp_cursor.line_offset)
        }
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        super.swap_lines(channel_a, line_a, channel_b, line_b)
        this.cursor_select_row(channel_b, line_b)
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusChannel.OpusLine {
        // Need to clear cursor before change since the way the editor_table updates
        // Cursors doesn't take into account changes to row count
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        val output = try {
            super.remove_line(channel, line_offset)
        } catch (e: OpusChannel.LastLineException) {
            throw e
        }

        if (bkp_cursor.mode == OpusManagerCursor.CursorMode.Row) {
            if (bkp_cursor.channel < this.channels.size) {
                if (bkp_cursor.line_offset < this.channels[bkp_cursor.channel].size) {
                    this.cursor_select_row(bkp_cursor.channel, bkp_cursor.line_offset)
                } else {
                    this.cursor_select_row(bkp_cursor.channel, this.channels[bkp_cursor.channel].size - 1)
                }
            } else {
                this.cursor_select_row(this.channels.size - 1, this.channels.last().size - 1)
            }
        }

        return output
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        super.new_channel(channel, lines, uuid)

        val compare_channel = channel ?: (this.channels.size - 2)
        when (bkp_cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor_select_column(bkp_cursor.beat)
            }
            OpusManagerCursor.CursorMode.Row -> {
                val new_channel = if (compare_channel <= bkp_cursor.channel) {
                    bkp_cursor.channel + 1
                } else {
                    bkp_cursor.channel
                }
                this.cursor_select_row(new_channel, bkp_cursor.line_offset)
            }
            OpusManagerCursor.CursorMode.Range -> {
                val new_first = if (bkp_cursor.range!!.first.channel >= compare_channel) {
                    bkp_cursor.range!!.first.channel + 1
                } else {
                    bkp_cursor.range!!.first.channel
                }

                val new_second = if (bkp_cursor.range!!.second.channel >= compare_channel) {
                    bkp_cursor.range!!.second.channel + 1
                } else {
                    bkp_cursor.range!!.second.channel
                }

                this.cursor_select_range(
                    BeatKey(
                        new_first,
                        bkp_cursor.range!!.first.line_offset,
                        bkp_cursor.range!!.first.beat
                    ),
                    BeatKey(
                        new_second,
                        bkp_cursor.range!!.second.line_offset,
                        bkp_cursor.range!!.second.beat
                    )
                )
            }
            OpusManagerCursor.CursorMode.Single -> {
                val new_channel = if (compare_channel <= bkp_cursor.channel) {
                    bkp_cursor.channel + 1
                } else {
                    bkp_cursor.channel
                }
                this.cursor_select(
                    BeatKey(
                        new_channel,
                        bkp_cursor.line_offset,
                        bkp_cursor.beat
                    ),
                    bkp_cursor.position
                )
            }
            else -> {}
        }
    }

    override fun remove_channel(channel: Int) {
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        super.remove_channel(channel)

        when (bkp_cursor.mode) {
            OpusManagerCursor.CursorMode.Row -> {
                val (new_channel, new_line_offset) = if (bkp_cursor.channel > channel) {
                    Pair(
                        bkp_cursor.channel - 1,
                        bkp_cursor.line_offset
                    )
                } else if (bkp_cursor.channel == channel) {
                    Pair(
                        min(bkp_cursor.channel, this.channels.size - 1),
                        0
                    )
                } else {
                    Pair(
                        bkp_cursor.channel,
                        bkp_cursor.line_offset
                    )
                }

                this.cursor_select_row(new_channel, new_line_offset)
            }
            OpusManagerCursor.CursorMode.Range -> {
                val first_corner = bkp_cursor.range!!.first
                val (first_channel, first_line_offset) = if (first_corner.channel > channel) {
                    Pair(first_corner.channel - 1, first_corner.line_offset)
                } else if (first_corner.channel == channel) {
                    Pair(min(first_corner.channel, this.channels.size - 1), 0)
                } else {
                    Pair(first_corner.channel, first_corner.line_offset)
                }

                val second_corner = bkp_cursor.range!!.second
                val (second_channel, second_line_offset) = if (second_corner.channel > channel) {
                    Pair(second_corner.channel - 1, second_corner.line_offset)
                } else if (second_corner.channel == channel) {
                    Pair(min(second_corner.channel, this.channels.size - 1), 0)
                } else {
                    Pair(second_corner.channel, second_corner.line_offset)
                }

                this.cursor_select_range(
                    BeatKey(first_channel, first_line_offset, first_corner.beat),
                    BeatKey(second_channel, second_line_offset, second_corner.beat)
                )
            }
            OpusManagerCursor.CursorMode.Single -> {
                val (new_channel, new_line_offset, new_position) = if (bkp_cursor.channel > channel) {
                    Triple(bkp_cursor.channel - 1, bkp_cursor.line_offset, bkp_cursor.position)
                } else if (bkp_cursor.channel == channel) {
                    val tmp_a = min(bkp_cursor.channel, this.channels.size - 1)
                    Triple(
                        tmp_a,
                        0,
                        this.get_first_position(
                            BeatKey(tmp_a, 0, bkp_cursor.beat),
                            listOf()
                        )
                    )
                } else {
                    Triple(bkp_cursor.channel, bkp_cursor.line_offset, bkp_cursor.position)
                }

                this.cursor_select(
                    BeatKey(
                        new_channel,
                        new_line_offset,
                        bkp_cursor.beat
                    ),
                    new_position
                )
            }

            OpusManagerCursor.CursorMode.Column -> {
                this.cursor_select_column(bkp_cursor.beat)
            }
            else -> {}
        }
    }

    override fun remove_beat(beat_index: Int) {
        // Need to clear cursor before change since the way the editor_table updates
        // Cursors doesn't take into account changes to column count
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        super.remove_beat(beat_index)

        when (bkp_cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor_select_column(
                    if (bkp_cursor.beat <= beat_index) {
                        min(bkp_cursor.beat, this.beat_count - 1)
                    } else {
                        max(0, bkp_cursor.beat - 1)
                    }
                )
            }

            OpusManagerCursor.CursorMode.Row -> {
                this.cursor_select_row(
                    bkp_cursor.channel,
                    bkp_cursor.line_offset
                )
            }

            OpusManagerCursor.CursorMode.Single -> {
                val (new_beat, new_position) = if (bkp_cursor.beat < beat_index) {
                    Pair(bkp_cursor.beat, bkp_cursor.position)
                } else if (bkp_cursor.beat == beat_index) {
                    Pair(min(bkp_cursor.beat, this.beat_count - 1), listOf())
                } else {
                    Pair(max(0, bkp_cursor.beat - 1), bkp_cursor.position)
                }

                this.cursor_select(
                    BeatKey(
                        bkp_cursor.channel,
                        bkp_cursor.line_offset,
                        new_beat
                    ),
                    new_position
                )
            }
            OpusManagerCursor.CursorMode.Range -> {
                val first_corner = bkp_cursor.range!!.first
                val new_first_beat = if (first_corner.beat <= beat_index) {
                    min(first_corner.beat, this.beat_count - 1)
                } else {
                    max(0, first_corner.beat - 1)
                }
                val second_corner = bkp_cursor.range!!.second
                val new_second_beat = if (second_corner.beat <= beat_index) {
                    min(second_corner.beat, this.beat_count - 1)
                } else {
                    max(0, second_corner.beat - 1)
                }
                this.cursor_select_range(
                    BeatKey(
                        first_corner.channel,
                        first_corner.line_offset,
                        new_first_beat
                    ),
                    BeatKey(
                        second_corner.channel,
                        second_corner.line_offset,
                        new_second_beat
                    )
                )
            }

            else -> {}
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        super.insert_beat(beat_index, beats_in_column)

        when (bkp_cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor_select_column(if (bkp_cursor.beat > beat_index) {
                    bkp_cursor.beat + 1
                } else {
                    bkp_cursor.beat
                })
            }
            OpusManagerCursor.CursorMode.Single -> {
                val new_beat = if (bkp_cursor.beat > beat_index) {
                    bkp_cursor.beat + 1
                } else {
                    bkp_cursor.beat
                }
                this.cursor_select(
                    BeatKey(
                        bkp_cursor.channel,
                        bkp_cursor.line_offset,
                        new_beat
                    ),
                    bkp_cursor.position
                )
            }
            OpusManagerCursor.CursorMode.Range -> {
                this.cursor_select_range(
                    BeatKey(
                        bkp_cursor.range!!.first.channel,
                        bkp_cursor.range!!.first.line_offset,
                        if (bkp_cursor.range!!.first.beat > beat_index) {
                            bkp_cursor.range!!.first.beat + 1
                        } else {
                            bkp_cursor.range!!.first.beat
                        }
                    ),
                    BeatKey(
                        bkp_cursor.range!!.second.channel,
                        bkp_cursor.range!!.second.line_offset,
                        if (bkp_cursor.range!!.second.beat > beat_index) {
                            bkp_cursor.range!!.second.beat + 1
                        } else {
                            bkp_cursor.range!!.second.beat
                        }
                    )
                )
            }
            OpusManagerCursor.CursorMode.Row -> {
                this.cursor_select_row(
                    bkp_cursor.channel,
                    bkp_cursor.line_offset
                )
            }


            OpusManagerCursor.CursorMode.Unset -> { }
        }
    }

    override fun clear() {
        this.cursor_clear()
        super.clear()
    }

    // For history when we don't want to worry about the cursor
    private fun queue_cursor_select(token: HistoryToken, args: List<Int>) {
        this._queued_cursor_selection = Pair(token, args)
    }

    private fun apply_queued_cursor_select() {
        if (this._queued_cursor_selection == null) {
            return
        }
        val (token, args) = this._queued_cursor_selection!!
        this._queued_cursor_selection = null
        when (token) {
            HistoryToken.CURSOR_SELECT_ROW -> {
                this.cursor_select_row(args[0], args[1])
            }

            HistoryToken.CURSOR_SELECT_COLUMN -> {
                this.cursor_select_column(args[0])
            }

            HistoryToken.CURSOR_SELECT -> {
                this.cursor_select(
                    BeatKey(
                        args[0],
                        args[1],
                        args[2]
                    ),
                    args.subList(3, args.size)
                )
            }
            else -> {}
        }
    }

    override fun apply_undo() {
        if (!this.history_cache.isEmpty()) {
            this.cursor_clear()
        }
        super.apply_undo()
        this.apply_queued_cursor_select()
    }

    override fun apply_history_node(current_node: HistoryCache.HistoryNode, depth: Int)  {
        when (current_node.token) {
            HistoryToken.CURSOR_SELECT_ROW -> {
                this.queue_cursor_select(
                    current_node.token,
                    this.checked_cast<List<Int>>(current_node.args)
                )
            }

            HistoryToken.CURSOR_SELECT -> {
                val beat_key = current_node.args[0] as BeatKey
                val args = mutableListOf<Int>(beat_key.channel, beat_key.line_offset, beat_key.beat)
                val position = this.checked_cast<List<Int>>(current_node.args[1])
                args.addAll(position)

                this.queue_cursor_select(
                    current_node.token,
                    args
                )
            }

            HistoryToken.CURSOR_SELECT_COLUMN -> {
                this.queue_cursor_select(
                    current_node.token,
                    this.checked_cast<List<Int>>(current_node.args)
                )
            }
            else -> { }
        }
        super.apply_history_node(current_node, depth)
    }

    override fun push_to_history_stack(token: HistoryToken, args: List<Any>) {
        if (this.history_cache.isLocked()) {
            return
        }

        var has_cursor_action = true
        this.history_cache.remember {
            when (token) {
                HistoryToken.MOVE_LINE -> {
                    val from_channel = args[0] as Int
                    val from_line = args[1] as Int
                    val to_channel = args[2] as Int
                    val to_line = args[3] as Int
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_ROW,
                        listOf(
                            to_channel,
                            if (from_channel == to_channel && to_line >= from_line) {
                                to_line - 1
                            } else {
                                to_line
                            }
                        )
                    )
                }
                HistoryToken.INSERT_LINE -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_ROW,
                        listOf(
                            args[0] as Int,
                            args[1] as Int
                        )
                    )
                }
                HistoryToken.REMOVE_LINE -> {
                    val channel = args[0] as Int
                    val line_offset = min(
                        args[1] as Int,
                        this.channels[channel].size - 1
                    )
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_ROW,
                        listOf(
                            channel,
                            max(0, line_offset - 1)
                        )
                    )
                }
                HistoryToken.REPLACE_TREE -> {
                    val new_position = this.checked_cast<List<Int>>(args[1]).toMutableList()
                    var tree = this.checked_cast<OpusTree<OpusEvent>>(args[2])
                    while (! tree.is_leaf()) {
                        new_position.add(0)
                        tree = tree[0]
                    }

                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(
                            args[0] as BeatKey,
                            new_position
                        )
                    )
                }
                HistoryToken.SET_PERCUSSION_INSTRUMENT ->{
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_ROW,
                        listOf(
                            this.channels.size - 1,
                            args[0] as Int
                        )
                    )
                }
                HistoryToken.SET_CHANNEL_INSTRUMENT -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_ROW,
                        listOf(args[0], 0)
                    )
                }
                HistoryToken.UNSET -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(
                            args[0] as BeatKey,
                            this.checked_cast<List<Int>>(args[1])
                        )
                    )
                }
                HistoryToken.SET_EVENT -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(
                            args[0] as BeatKey,
                            this.checked_cast<List<Int>>(args[1])
                        )
                    )
                }
                HistoryToken.SET_EVENT_DURATION -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(
                            args[0] as BeatKey,
                            this.checked_cast<List<Int>>(args[1])
                        )
                    )
                }
                HistoryToken.SET_PERCUSSION_EVENT -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(
                            args[0] as BeatKey,
                            this.checked_cast<List<Int>>(args[1])
                        )
                    )
                }
                HistoryToken.INSERT_BEAT -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_COLUMN,
                        listOf(args[0] as Int)
                    )
                }
                HistoryToken.REMOVE_BEAT -> {
                    val x = max(0, min(args[0] as Int, this.beat_count - 2))
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_COLUMN,
                        listOf(x)
                    )
                }
                HistoryToken.LINK_BEATS -> {
                    val beat_key = args[0] as BeatKey
                    val position = mutableListOf<Int>()
                    var tree = this.get_tree(beat_key,position)
                    while (! tree.is_leaf()) {
                        tree = tree[0]
                        position.add(0)
                    }
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf( beat_key, position )
                    )
                }
                HistoryToken.REMOVE -> {
                    val beat_key = args[0] as BeatKey
                    val position = this.checked_cast<List<Int>>(args[1])

                    val tree = this.get_tree(beat_key, position)
                    val cursor_position = position.toMutableList()
                    if (tree.parent!!.size <= 2) { // Will be pruned
                        cursor_position.removeLast()
                    } else if (position.last() == tree.parent!!.size - 1) {
                        cursor_position[cursor_position.size - 1] -= 1
                    }

                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(beat_key, cursor_position)
                    )
                }
                HistoryToken.INSERT_TREE -> {
                    val beat_key = args[0] as BeatKey
                    val position = this.checked_cast<List<Int>>(args[1])
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(beat_key, position)
                    )
                }
                else -> {
                    has_cursor_action = false
                }
            }
            if (has_cursor_action) {
                super.push_to_history_stack(token, args)
            }
        }
        if (! has_cursor_action) {
            //this.history_cache.pop()
            super.push_to_history_stack(token, args)
        }
    }

    // Cursor Functions ////////////////////////////////////////////////////////////////////////////
    open fun cursor_clear() {
        this.cursor.clear()
    }

    open fun cursor_select_row(channel: Int, line_offset: Int) {
        this.cursor.select_row(channel, line_offset)
    }

    open fun cursor_select_column(beat: Int) {
        if (beat >= this.beat_count) {
            return
        }

        this.cursor.select_column(beat)
    }

    open fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        this.cursor.select(beat_key, position)
    }

    // TODO: Change Name? select_first_corner?
    open fun cursor_select_to_link(beat_key: BeatKey) {
        this.cursor.select_to_link(beat_key)
    }
    open fun cursor_select_range_to_link(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.cursor.select_range(beat_key_a, beat_key_b)
    }

    open fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.cursor.select_range(beat_key_a, beat_key_b)
    }

    fun get_tree(): OpusTree<OpusEvent> {
        return this.get_tree(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }

    fun unset() {
        if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            val beat_key = this.cursor.range!!.first
            this.unset_range(beat_key, this.cursor.range!!.second)
            this.cursor_select(beat_key, listOf())
        } else {
            val beat_key = this.cursor.get_beatkey()
            val position = this.cursor.get_position()
            this.unset(beat_key, position)
            this.cursor_select(beat_key, position)
        }
    }

    fun convert_event_to_absolute() {
        this.convert_event_to_absolute(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }

    fun convert_event_to_relative() {
        this.convert_event_to_relative(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }

    fun set_event(event: OpusEvent) {
        this.set_event(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            event
        )
    }

    fun set_percussion_event() {
        this.set_percussion_event(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }

    fun unlink_beat() {
        if (this.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            this.unlink_beat(this.cursor.get_beatkey())
        } else if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            this.unlink_range(cursor.range!!.first, cursor.range!!.second)
        }
    }

    fun clear_link_pool() {
        if (this.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            val beat_key = this.cursor.get_beatkey()
            this.clear_link_pool(beat_key)
        } else if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            val beat_key = this.cursor.range!!.first
            this.clear_link_pools_by_range(
                beat_key,
                this.cursor.range!!.second
            )
        }
    }

    fun set_percussion_instrument(instrument: Int) {
        this.set_percussion_instrument(
            this.cursor.line_offset,
            instrument
        )
    }

    fun split_tree(splits: Int) {
        this.split_tree(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            splits
        )
    }

    fun insert_after(count: Int) {
        this.insert_after(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            count
        )
    }

    fun remove(count: Int) {
        val cursor = this.cursor
        val beat_key = cursor.get_beatkey()
        val position = cursor.get_position().toMutableList()

        val tree = this.get_tree()
        val cursor_position = position.toMutableList()
        if (tree.parent!!.size <= 2) { // Will be pruned
            cursor_position.removeLast()
        } else if (position.last() == tree.parent!!.size - 1) {
            cursor_position[cursor_position.size - 1] -= 1
        }

        this.remove(beat_key, position, count)

        this.cursor_select(
            beat_key,
            this.get_first_position(beat_key, cursor_position)
        )
    }

    fun insert_line(count: Int) {
        this.new_line(
            this.cursor.channel,
            this.cursor.line_offset + 1,
            count
        )
    }

    fun remove_line(count: Int) {
        this.remove_line(
            this.cursor.channel,
            this.cursor.line_offset,
            count
        )
    }

    fun remove_beat_at_cursor(count: Int) {
        this.remove_beat(this.cursor.beat, count)
    }

    fun insert_beat_after_cursor(count: Int) {
        this.insert_beat(this.cursor.beat + 1, count)
    }

    fun link_beat(beat_key: BeatKey) {
        if (this.cursor.is_linking_range()) {
            val (first, second) = this.cursor.range!!
            this.link_beat_range(beat_key, first, second)
        } else if (this.cursor.is_linking) {
            this.link_beats(beat_key, this.cursor.get_beatkey())
        } else {
            // TODO: Raise Error
        }
    }

    fun move_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_linking_range()) {
            val (first, second) = this.cursor.range!!
            this.move_beat_range(beat_key, first, second)
        } else if (this.cursor.is_linking) {
            this.move_leaf(this.cursor.get_beatkey(), listOf(), beat_key, listOf())
        } else {
            // TODO: Raise Error
        }
    }

    fun copy_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_linking_range()) {
            val (first, second) = this.cursor.range!!
            this.overwrite_beat_range(beat_key, first, second)
        } else if (this.cursor.is_linking) {
            this.replace_tree(
                beat_key,
                listOf(),
                this.get_tree(
                    this.cursor.get_beatkey(),
                    listOf()
                )
            )
        } else {
            // TODO: Raise Error
        }
    }
    // End Cursor Functions ////////////////////////////////////////////////////////////////////////

    fun is_selected(beat_key: BeatKey, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat_key.beat
            }
            OpusManagerCursor.CursorMode.Row -> {
                this.cursor.channel == beat_key.channel && this.cursor.line_offset == beat_key.line_offset
            }
            OpusManagerCursor.CursorMode.Range -> {
                beat_key in this.get_beatkeys_in_range(this.cursor.range!!.first, this.cursor.range!!.second)
            }
            OpusManagerCursor.CursorMode.Single -> {
                val cposition = this.cursor.get_position()
                this.cursor.get_beatkey() == beat_key && position.size >= cposition.size && position.subList(0, cposition.size) == cposition
            }
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }
        }
    }

    /* Not Currently In Use. */
    fun link_alike() {
        if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            this.link_alike(this.cursor.range!!.first, this.cursor.range!!.second)
        }
    }
}