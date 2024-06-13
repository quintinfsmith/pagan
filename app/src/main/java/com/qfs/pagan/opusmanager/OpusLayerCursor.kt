package com.qfs.pagan.opusmanager
import com.qfs.pagan.structure.OpusTree
import java.lang.Integer.max
import java.lang.Integer.min

open class OpusLayerCursor: OpusLayerHistory() {
    class InvalidCursorState: Exception()
    var cursor = OpusManagerCursor()
    private var _queued_cursor_selection: OpusManagerCursor? = null

    override fun insert_line(channel: Int, line_offset: Int, line: OpusLine) {
        // Need to clear cursor before change since the way the editor_table updates
        // Cursors doesn't take into account changes to row count
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        super.insert_line(channel, line_offset, line)

        if (bkp_cursor.mode == OpusManagerCursor.CursorMode.Row) {
            this.cursor_select_row(bkp_cursor.channel, bkp_cursor.line_offset)
        }
    }

    override fun new_line(channel: Int, line_offset: Int?): OpusLine {
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        val output = super.new_line(channel, line_offset)

        if (bkp_cursor.mode == OpusManagerCursor.CursorMode.Row) {
            this.cursor_select_row(bkp_cursor.channel, bkp_cursor.line_offset)
        }

        return output
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        super.swap_lines(channel_a, line_a, channel_b, line_b)
        this.cursor_select_row(channel_b, line_b)
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusLine {
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

        //when (bkp_cursor.mode) {
        //    OpusManagerCursor.CursorMode.Row -> {
        //        val (new_channel, new_line_offset) = if (bkp_cursor.channel > channel) {
        //            Pair(
        //                bkp_cursor.channel - 1,
        //                bkp_cursor.line_offset
        //            )
        //        } else if (bkp_cursor.channel == channel) {
        //            Pair(
        //                min(bkp_cursor.channel, this.channels.size - 1),
        //                0
        //            )
        //        } else {
        //            Pair(
        //                bkp_cursor.channel,
        //                bkp_cursor.line_offset
        //            )
        //        }

        //        //this.cursor_select_row(new_channel, new_line_offset)
        //    }
        //    OpusManagerCursor.CursorMode.Range -> {
        //        val first_corner = bkp_cursor.range!!.first
        //        val (first_channel, first_line_offset) = if (first_corner.channel > channel) {
        //            Pair(first_corner.channel - 1, first_corner.line_offset)
        //        } else if (first_corner.channel == channel) {
        //            Pair(min(first_corner.channel, this.channels.size - 1), 0)
        //        } else {
        //            Pair(first_corner.channel, first_corner.line_offset)
        //        }

        //        val second_corner = bkp_cursor.range!!.second
        //        val (second_channel, second_line_offset) = if (second_corner.channel > channel) {
        //            Pair(second_corner.channel - 1, second_corner.line_offset)
        //        } else if (second_corner.channel == channel) {
        //            Pair(min(second_corner.channel, this.channels.size - 1), 0)
        //        } else {
        //            Pair(second_corner.channel, second_corner.line_offset)
        //        }

        //       // this.cursor_select_range(
        //       //     BeatKey(first_channel, first_line_offset, first_corner.beat),
        //       //     BeatKey(second_channel, second_line_offset, second_corner.beat)
        //       // )
        //    }
        //    OpusManagerCursor.CursorMode.Single -> {
        //        val (new_channel, new_line_offset, new_position) = if (bkp_cursor.channel > channel) {
        //            Triple(bkp_cursor.channel - 1, bkp_cursor.line_offset, bkp_cursor.position)
        //        } else if (bkp_cursor.channel == channel) {
        //            val tmp_a = min(bkp_cursor.channel, this.channels.size - 1)
        //            Triple(
        //                tmp_a,
        //                0,
        //                this.get_first_position(
        //                    BeatKey(tmp_a, 0, bkp_cursor.beat),
        //                    listOf()
        //                )
        //            )
        //        } else {
        //            Triple(bkp_cursor.channel, bkp_cursor.line_offset, bkp_cursor.position)
        //        }

        //        this.cursor_select(
        //            BeatKey(
        //                new_channel,
        //                new_line_offset,
        //                bkp_cursor.beat
        //            ),
        //            new_position
        //        )
        //    }

        //    OpusManagerCursor.CursorMode.Column -> {
        //        this.cursor_select_column(bkp_cursor.beat)
        //    }
        //    else -> {}
        //}
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

    override fun insert_beats(beat_index: Int, count: Int) {
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        super.insert_beats(beat_index, count)

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
    private fun queue_cursor_select(cursor: OpusManagerCursor) {
        this._queued_cursor_selection = cursor
    }

    private fun apply_queued_cursor_select() {
        if (this._queued_cursor_selection == null) {
            return
        }

        val new_cursor = this._queued_cursor_selection!!
        this._queued_cursor_selection = null

        this.cursor_apply(new_cursor)
    }

    override fun apply_undo(repeat: Int) {
        if (!this.history_cache.isEmpty()) {
            this.cursor_clear()
        }
        super.apply_undo(repeat)
        this.apply_queued_cursor_select()
    }

    override fun apply_history_node(current_node: HistoryCache.HistoryNode, depth: Int) {
        when (current_node.token) {
            HistoryToken.CURSOR_SELECT_ROW -> {
                var args = this.checked_cast<List<Int>>(current_node.args)
                this.queue_cursor_select(
                    OpusManagerCursor(
                        mode = OpusManagerCursor.CursorMode.Row,
                        channel = args[0],
                        line_offset =  args[1]
                    )
                )
            }

            HistoryToken.CURSOR_SELECT -> {
                val beat_key = current_node.args[0] as BeatKey
                val args = mutableListOf<Int>(beat_key.channel, beat_key.line_offset, beat_key.beat)
                val position = this.checked_cast<List<Int>>(current_node.args[1])
                args.addAll(position)

                this.queue_cursor_select(
                    OpusManagerCursor(
                        mode = OpusManagerCursor.CursorMode.Single,
                        channel = beat_key.channel,
                        line_offset = beat_key.line_offset,
                        beat = beat_key.beat,
                        position = position
                    )
                )
            }

            HistoryToken.CURSOR_SELECT_COLUMN -> {
                this.queue_cursor_select(
                    OpusManagerCursor(
                        mode = OpusManagerCursor.CursorMode.Column,
                        beat = current_node.args[0] as Int
                    )
                )
            }

            HistoryToken.CURSOR_SELECT_GLOBAL_CTL -> {
                this.queue_cursor_select(
                    OpusManagerCursor(
                        mode = OpusManagerCursor.CursorMode.Single,
                        beat = current_node.args[1] as Int,
                        position = this.checked_cast<List<Int>>(current_node.args[2]),
                        ctl_level = CtlLineLevel.Global,
                        ctl_type = this.checked_cast<ControlEventType>(current_node.args[0])
                    )
                )
            }
            HistoryToken.CURSOR_SELECT_CHANNEL_CTL -> {
                this.queue_cursor_select(
                    OpusManagerCursor(
                        mode = OpusManagerCursor.CursorMode.Single,
                        channel = current_node.args[1] as Int,
                        beat = current_node.args[2] as Int,
                        position = this.checked_cast<List<Int>>(current_node.args[3]),
                        ctl_level = CtlLineLevel.Channel,
                        ctl_type = this.checked_cast<ControlEventType>(current_node.args[0])
                    )
                )
            }
            HistoryToken.CURSOR_SELECT_LINE_CTL -> {
                val beat_key = this.checked_cast<BeatKey>(current_node.args[1])
                this.queue_cursor_select(
                    OpusManagerCursor(
                        mode = OpusManagerCursor.CursorMode.Single,
                        channel = beat_key.channel,
                        line_offset = beat_key.line_offset,
                        beat = beat_key.beat,
                        position = this.checked_cast<List<Int>>(current_node.args[2]),
                        ctl_level = CtlLineLevel.Line,
                        ctl_type = this.checked_cast<ControlEventType>(current_node.args[0])
                    )
                )
            }
            HistoryToken.CURSOR_SELECT_LINE_CTL_ROW -> {
                this.queue_cursor_select(
                    OpusManagerCursor(
                        mode = OpusManagerCursor.CursorMode.Single,
                        channel = current_node.args[1] as Int,
                        line_offset = current_node.args[2] as Int,
                        ctl_level = CtlLineLevel.Line,
                        ctl_type = this.checked_cast<ControlEventType>(current_node.args[0])
                    )
                )
            }
            HistoryToken.CURSOR_SELECT_CHANNEL_CTL_ROW -> {
                this.queue_cursor_select(
                    OpusManagerCursor(
                        mode = OpusManagerCursor.CursorMode.Row,
                        ctl_level = CtlLineLevel.Channel,
                        channel = current_node.args[1] as Int,
                        ctl_type = this.checked_cast<ControlEventType>(current_node.args[0])
                    )
                )
            }
            HistoryToken.CURSOR_SELECT_GLOBAL_CTL_ROW -> {
                this.queue_cursor_select(
                    OpusManagerCursor(
                        mode = OpusManagerCursor.CursorMode.Row,
                        ctl_level = CtlLineLevel.Global,
                        ctl_type = this.checked_cast<ControlEventType>(current_node.args[0])
                    )
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
        if (this.link_lock < 2) { // Don't move cursor to links (0: no lock applied, 1: lock applied but still consideried original, 2: lock applied & working with linked)
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
                        var tree = this.checked_cast<OpusTree<OpusEventSTD>>(args[2])
                        while (!tree.is_leaf()) {
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

                    HistoryToken.REPLACE_GLOBAL_CTL_TREE -> {
                        val new_position = this.checked_cast<List<Int>>(args[2]).toMutableList()
                        var tree = this.checked_cast<OpusTree<ControlEventType>>(args[3])
                        while (!tree.is_leaf()) {
                            new_position.add(0)
                            tree = tree[0]
                        }

                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_GLOBAL_CTL,
                            listOf(
                                args[0] as ControlEventType,
                                args[1] as Int,
                                new_position,
                                tree
                            )
                        )
                    }
                    HistoryToken.REPLACE_CHANNEL_CTL_TREE -> {
                        val new_position = this.checked_cast<List<Int>>(args[3]).toMutableList()
                        var tree = this.checked_cast<OpusTree<ControlEventType>>(args[4])
                        while (!tree.is_leaf()) {
                            new_position.add(0)
                            tree = tree[0]
                        }

                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_CHANNEL_CTL,
                            listOf(
                                args[0] as ControlEventType,
                                args[1] as Int,
                                args[1] as Int,
                                new_position,
                                tree
                            )
                        )
                    }

                    HistoryToken.REPLACE_LINE_CTL_TREE -> {
                        val new_position = this.checked_cast<List<Int>>(args[2]).toMutableList()
                        var tree = this.checked_cast<OpusTree<ControlEventType>>(args[3])
                        while (!tree.is_leaf()) {
                            new_position.add(0)
                            tree = tree[0]
                        }

                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_LINE_CTL,
                            listOf(
                                args[0] as ControlEventType,
                                args[1] as BeatKey,
                                new_position,
                                tree
                            )
                        )
                    }

                    HistoryToken.SET_PERCUSSION_INSTRUMENT -> {
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

                    HistoryToken.REMOVE_BEATS -> {
                        val target_x = args[0] as Int
                        val repeat = args[1] as Int
                        val x = max(0, min(target_x, this.beat_count - 1 - repeat))

                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_COLUMN,
                            listOf(x)
                        )
                    }

                    HistoryToken.LINK_BEATS -> {
                        val beat_key = args[0] as BeatKey
                        val position = mutableListOf<Int>()
                        var tree = this.get_tree(beat_key, position)
                        while (!tree.is_leaf()) {
                            tree = tree[0]
                            position.add(0)
                        }
                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT,
                            listOf(beat_key, position)
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

                    HistoryToken.SET_GLOBAL_CTL_INITIAL_EVENT -> {
                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_GLOBAL_CTL_ROW,
                            listOf(this.checked_cast<ControlEventType>(args[0]))
                        )
                    }
                    HistoryToken.SET_CHANNEL_CTL_INITIAL_EVENT -> {
                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_CHANNEL_CTL_ROW,
                            listOf(
                                this.checked_cast<ControlEventType>(args[0]),
                                args[1] as Int
                            )
                        )
                    }
                    HistoryToken.SET_LINE_CTL_INITIAL_EVENT -> {
                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_LINE_CTL_ROW,
                            listOf(
                                this.checked_cast<ControlEventType>(args[0]),
                                args[1] as Int,
                                args[2] as Int
                            )
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
        } else {
            has_cursor_action = false
        }

        if (! has_cursor_action) {
            //this.history_cache.pop()
            super.push_to_history_stack(token, args)
        }
    }

    override fun on_project_changed() {
        super.on_project_changed()
        this.cursor_clear()
    }

    // Cursor Functions ////////////////////////////////////////////////////////////////////////////
    open fun cursor_apply(cursor: OpusManagerCursor) {
        this.cursor.clear()
        this.cursor = cursor
    }

    open fun cursor_clear() {
        this.cursor.clear()
    }

    open fun cursor_select_row(channel: Int, line_offset: Int) {
        this.cursor.select_row(channel, line_offset)
    }

    open fun cursor_select_ctl_row_at_line(ctl_type: ControlEventType, channel: Int, line_offset: Int) {
        this.cursor.select_ctl_row_at_line(channel, line_offset, ctl_type)
    }

    open fun cursor_select_ctl_row_at_channel(ctl_type: ControlEventType, channel: Int) {
        this.cursor.select_ctl_row_at_channel(channel, ctl_type)
    }

    open fun cursor_select_ctl_row_at_global(ctl_type: ControlEventType) {
        this.cursor.select_ctl_row_at_global(ctl_type)
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

    open fun cursor_select_ctl_at_line(ctl_type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.cursor.select_ctl_at_line(beat_key, position, ctl_type)
    }

    open fun cursor_select_ctl_at_channel(ctl_type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.cursor.select_ctl_at_channel(channel, beat, position, ctl_type)
    }

    open fun cursor_select_ctl_at_global(ctl_type: ControlEventType, beat: Int, position: List<Int>) {
        this.cursor.select_ctl_at_global(beat, position, ctl_type)
    }

    open fun cursor_select_first_corner(beat_key: BeatKey) {
        this.cursor.select_first_corner(beat_key)
    }

    open fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.cursor.select_range(beat_key_a, beat_key_b)
    }

    open fun cursor_select_global_ctl_range(type:ControlEventType, first: Int, second: Int) {
        this.cursor.select_global_ctl_range(type, first, second)
    }
    open fun cursor_select_global_ctl_end_point(type: ControlEventType, beat: Int) {
        this.cursor.select_global_ctl_end_point(type, beat)
    }

    open fun cursor_select_channel_ctl_range(type:ControlEventType, channel: Int, first: Int, second: Int) {
        this.cursor.select_channel_ctl_range(type, channel, first, second)
    }
    open fun cursor_select_channel_ctl_end_point(type: ControlEventType, channel: Int, beat: Int) {
        this.cursor.select_channel_ctl_end_point(type, channel, beat)
    }

    open fun cursor_select_line_ctl_first_corner(type: ControlEventType, beat_key: BeatKey) {
        this.cursor.select_line_ctl_first_corner(type, beat_key)
    }

    open fun cursor_select_line_ctl_range(type: ControlEventType, beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.cursor.select_line_ctl_range(type, beat_key_a, beat_key_b)
    }


    fun get_tree(): OpusTree<OpusEventSTD> {
        return this.get_tree(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }

    fun unset() {
        when (cursor.ctl_level) {
            null -> {
                if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                    val (first_key, second_key) = this.cursor.get_ordered_range()!!

                    this.unset_range(first_key, second_key)
                    this.cursor_select(first_key, listOf())
                } else {
                    val beat_key = this.cursor.get_beatkey()
                    val position = this.cursor.get_position()

                    this.unset(beat_key, position)
                    this.cursor_select(beat_key, position)
                }
            }
            CtlLineLevel.Global -> {
                val ctl_type = this.cursor.ctl_type!!
                if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                    val (key_a, key_b) = this.cursor.range!!
                    val start = min(key_a.beat, key_b.beat)
                    val end = max(key_a.beat, key_b.beat)
                    this.unset_global_ctl_range(ctl_type, start, end)
                    this.cursor_select_ctl_at_global(ctl_type, start, listOf())
                } else {
                    val beat = this.cursor.beat
                    val position = this.cursor.get_position()

                    this.unset_global_ctl(ctl_type, beat, position)
                    this.cursor_select_ctl_at_global(ctl_type, beat, position)
                }
            }
            CtlLineLevel.Channel -> {
                val ctl_type = this.cursor.ctl_type!!
                if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                    val (key_a, key_b) = this.cursor.range!!
                    val start = min(key_a.beat, key_b.beat)
                    val end = max(key_a.beat, key_b.beat)
                    this.unset_channel_ctl_range(ctl_type, key_a.channel, start, end)
                    this.cursor_select_ctl_at_channel(ctl_type, key_a.channel, start, listOf())
                } else {
                    val channel = this.cursor.channel
                    val beat = this.cursor.beat
                    val position = this.cursor.get_position()

                    this.unset_channel_ctl(ctl_type, channel, beat, position)
                    this.cursor_select_ctl_at_channel(ctl_type, channel, beat, position)
                }
            }
            CtlLineLevel.Line -> {
                val ctl_type = this.cursor.ctl_type!!
                if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                    val (first_key, second_key) = this.cursor.get_ordered_range()!!

                    this.unset_line_ctl_range(ctl_type, first_key, second_key)
                    this.cursor_select_ctl_at_line(ctl_type, first_key, listOf())
                } else {
                    val beat_key = this.cursor.get_beatkey()
                    val position = this.cursor.get_position()

                    this.unset_line_ctl(ctl_type, beat_key, position)
                    this.cursor_select_ctl_at_line(ctl_type, beat_key, position)
                }
            }
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

    open fun set_event_at_cursor(event: OpusEvent) {
        when (this.cursor.ctl_level) {
            null -> {
                this.set_event(
                    this.cursor.get_beatkey(),
                    this.cursor.get_position(),
                    event as OpusEventSTD
                )
            }
            CtlLineLevel.Global -> {
                this.set_global_ctl_event(
                    this.cursor.ctl_type!!,
                    this.cursor.beat,
                    this.cursor.position,
                    event as OpusControlEvent
                )
            }
            CtlLineLevel.Channel -> {
                this.set_channel_ctl_event(
                    this.cursor.ctl_type!!,
                    this.cursor.channel,
                    this.cursor.beat,
                    this.cursor.position,
                    event as OpusControlEvent
                )
            }
            CtlLineLevel.Line -> {
                this.set_line_ctl_event(
                    this.cursor.ctl_type!!,
                    this.cursor.get_beatkey(),
                    this.cursor.position,
                    event as OpusControlEvent
                )
            }
        }
    }

    open fun set_percussion_event_at_cursor() {
        this.set_percussion_event(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }

    fun unlink_beat() {
        if (this.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            this.unlink_beat(this.cursor.get_beatkey())
            val beat_key = this.cursor.get_beatkey()
            this.cursor.selecting_range = false
            this.cursor_select(beat_key, this.get_first_position(beat_key))
        } else if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            val (first, second) = this.cursor.get_ordered_range()!!
            this.unlink_range(first, second)

            this.cursor.selecting_range = false
            this.cursor_select(first, this.get_first_position(first))
        }
    }

    fun clear_link_pool() {
        if (this.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            val beat_key = this.cursor.get_beatkey()
            this.clear_link_pool(beat_key)
        } else if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            val (first_key, second_key) = this.cursor.get_ordered_range()!!
            this.clear_link_pools_by_range(first_key, second_key)
        }

        this.cursor.selecting_range = false
        when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                val beat_key = this.cursor.get_beatkey()
                this.cursor_select(beat_key, this.get_first_position(beat_key))
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first_key, second_key) = this.cursor.get_ordered_range()!!
                this.cursor_select(first_key, this.get_first_position(first_key))
            }
            else -> {}
        }
    }

    fun set_percussion_instrument(instrument: Int) {
        this.set_percussion_instrument(
            this.cursor.line_offset,
            instrument
        )
    }

    fun split_tree(splits: Int, move_event_to_end: Boolean = false) {
        this.split_tree(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            splits,
            move_event_to_end
        )
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.split_tree(beat_key, position, splits, move_event_to_end)
        val new_position = position.toMutableList()
        new_position.add(0)
        this.cursor_select(beat_key, new_position)
    }

    override fun split_global_ctl_tree(type: ControlEventType, beat: Int, position: List<Int>, splits: Int) {
        super.split_global_ctl_tree(type, beat, position, splits)
        val new_position = position.toMutableList()
        new_position.add(0)
        this.cursor_select_ctl_at_global(type, beat, new_position)
    }
    override fun split_channel_ctl_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, splits: Int) {
        super.split_channel_ctl_tree(type, channel, beat, position, splits)
        val new_position = position.toMutableList()
        new_position.add(0)
        this.cursor_select_ctl_at_channel(type, channel, beat, new_position)
    }
    override fun split_line_ctl_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>, splits: Int) {
        super.split_line_ctl_tree(type, beat_key, position, splits)
        val new_position = position.toMutableList()
        new_position.add(0)
        this.cursor_select_ctl_at_line(type, beat_key, new_position)
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        super.insert(beat_key, position)
        this.cursor_select(beat_key, position)
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        super.insert_after(beat_key, position)
        this.cursor_select(beat_key, position)
    }

    fun insert_after(count: Int) {
        this.insert_after(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            count
        )
    }

    fun insert(count: Int) {
        this.insert(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            count
        )
    }

    fun <T> _calculate_new_position_after_remove(working_tree: OpusTree<T>, position: List<Int>, count: Int): Pair<Int, List<Int>> {
        val cursor_position = position.toMutableList()
        var real_count = 0
        for (i in 0 until count) {
            if (cursor_position.isEmpty()) {
                break
            }

            val parent = working_tree.get(cursor_position.subList(0, cursor_position.size - 1))
            if (parent.size == 2) {
                parent.set_event(null)
                cursor_position.removeLast()
            } else if (cursor_position.last() == parent.size - 1) {
                parent[cursor_position.last()].detach()
                cursor_position[cursor_position.size - 1] -= 1
            } else {
                parent[cursor_position.last()].detach()
            }
            real_count += 1
        }
        return Pair(real_count, cursor_position)
    }

    fun remove(count: Int) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            null -> {
                val beat_key = cursor.get_beatkey()
                val position = cursor.get_position().toMutableList()

                var working_tree = this.get_tree(beat_key).copy()

                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, cursor.get_position(), count)

                this.remove(beat_key, position, real_count)

                this.cursor_select(
                    beat_key,
                    this.get_first_position(beat_key, cursor_position)
                )
            }

            CtlLineLevel.Global -> {
                val working_tree = this.get_global_ctl_tree(cursor.ctl_type!!, cursor.beat).copy()
                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, cursor.get_position(), count)

                this.remove_global_ctl(cursor.ctl_type!!, cursor.beat, cursor.position, real_count)

                this.cursor_select_ctl_at_global(
                    cursor.ctl_type!!,
                    cursor.beat,
                    this.get_first_position_global_ctl(cursor.ctl_type!!, cursor.beat, cursor_position)
                )
            }

            CtlLineLevel.Channel -> {
                val working_tree = this.get_channel_ctl_tree(cursor.ctl_type!!, cursor.channel, cursor.beat)
                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, cursor.get_position(), count)

                this.remove_channel_ctl(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position, real_count)

                this.cursor_select_ctl_at_channel(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.beat,
                    this.get_first_position_channel_ctl(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor_position)
                )
            }

            CtlLineLevel.Line -> {
                val beat_key = cursor.get_beatkey()
                val working_tree = this.get_line_ctl_tree(cursor.ctl_type!!, beat_key)
                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, cursor.get_position(), count)

                this.remove_line_ctl(cursor.ctl_type!!, beat_key, cursor.position, real_count)

                this.cursor_select_ctl_at_line(
                    cursor.ctl_type!!,
                    beat_key,
                    this.get_first_position_line_ctl(cursor.ctl_type!!, beat_key, cursor_position)
                )
            }
        }
    }

    fun remove_global_ctl(count: Int) {
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

        this.cursor_select(beat_key, this.get_first_position(beat_key, cursor_position))
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
        this.insert_beats(this.cursor.beat + 1, count)
    }

    fun insert_beat_at_cursor(count: Int) {
        this.insert_beats(this.cursor.beat, count)
    }


    fun link_beat(beat_key: BeatKey) {
        if (this.cursor.is_linking_range()) {
            val (first, second) = this.cursor.range!!
            this.link_beat_range(beat_key, first, second)
        } else if (this.cursor.selecting_range) {
            this.link_beats(beat_key, this.cursor.get_beatkey())
        } else {
            throw InvalidCursorState()
        }
    }

    fun merge_into_beat(beat_key: BeatKey) {
        if (this.cursor.is_linking_range()) {
            TODO()
        } else if (this.cursor.selecting_range) {
            if (this.is_percussion(this.cursor.get_beatkey().channel) != this.is_percussion(beat_key.channel)) {
                throw MixedInstrumentException(this.cursor.get_beatkey(), beat_key)
            }
            this.merge_leafs(this.cursor.get_beatkey(), listOf(), beat_key, listOf())
        } else {
            throw InvalidCursorState()
        }
    }

    fun move_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_linking_range()) {
            val (first, second) = this.cursor.range!!
            this.move_beat_range(beat_key, first, second)
        } else if (this.cursor.selecting_range) {
            if (this.is_percussion(this.cursor.get_beatkey().channel) != this.is_percussion(beat_key.channel)) {
                throw MixedInstrumentException(this.cursor.get_beatkey(), beat_key)
            }
            this.move_leaf(this.cursor.get_beatkey(), listOf(), beat_key, listOf())
        } else {
            throw InvalidCursorState()
        }
    }

    fun copy_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_linking_range()) {
            val (first, second) = this.cursor.range!!
            this.overwrite_beat_range(beat_key, first, second)
        } else if (this.cursor.selecting_range) {
            if (this.is_percussion(this.cursor.get_beatkey().channel) != this.is_percussion(beat_key.channel)) {
                throw MixedInstrumentException(this.cursor.get_beatkey(), beat_key)
            }
            this.replace_tree(
                beat_key,
                listOf(),
                this.get_tree(
                    this.cursor.get_beatkey(),
                    listOf()
                )
            )
        } else {
            throw InvalidCursorState()
        }
    }

    fun copy_global_ctl_to_beat(beat: Int) {
        if (this.cursor.ctl_level != CtlLineLevel.Global) {
            throw InvalidOverwriteCall()
        }

        if (this.cursor.is_linking_range()) {
            val (first, second) = this.cursor.range!!
            this.overwrite_global_ctl_range(
                this.cursor.ctl_type!!,
                beat,
                min(first.beat, second.beat),
                max(first.beat, second.beat)
            )
        } else if (this.cursor.selecting_range) {
            this.replace_global_ctl_tree(
                this.cursor.ctl_type!!,
                beat,
                listOf(),
                this.get_global_ctl_tree(
                    this.cursor.ctl_type!!,
                    this.cursor.beat,
                    listOf()
                )
            )
        } else {
            throw InvalidCursorState()
        }

        val tree = this.get_global_ctl_tree(
            this.cursor.ctl_type!!,
            beat,
            listOf()
        )

        this.cursor_select_ctl_at_global(
            this.cursor.ctl_type!!,
            beat,
            tree.get_first_event_tree_position() ?: listOf()
        )
    }

    fun move_global_ctl_to_beat(beat: Int) {
        if (this.cursor.ctl_level != CtlLineLevel.Global) {
            throw InvalidOverwriteCall()
        }

        if (this.cursor.is_linking_range()) {
            val (first, second) = this.cursor.range!!
            this.move_global_ctl_range(
                this.cursor.ctl_type!!,
                beat,
                first.beat,
                second.beat
            )
        } else if (this.cursor.selecting_range) {
            this.move_global_ctl_leaf(
                this.cursor.ctl_type!!,
                this.cursor.beat,
                listOf(),
                beat,
                listOf()
            )
        } else {
            throw InvalidCursorState()
        }

        val tree = this.get_global_ctl_tree(
            this.cursor.ctl_type!!,
            beat,
            listOf()
        )

        this.cursor_select_ctl_at_global(
            this.cursor.ctl_type!!,
            beat,
            tree.get_first_event_tree_position() ?: listOf()
        )
    }
    // End Cursor Functions ////////////////////////////////////////////////////////////////////////
    fun is_selected(beat_key: BeatKey, position: List<Int>): Boolean {
        if (this.cursor.ctl_level != null) {
            return false
        }

        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat_key.beat
            }
            OpusManagerCursor.CursorMode.Row -> {
                this.cursor.channel == beat_key.channel && this.cursor.line_offset == beat_key.line_offset
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                beat_key in this.get_beatkeys_in_range(first, second)
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

    fun is_global_control_selected(control_type: ControlEventType, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat
            }
            OpusManagerCursor.CursorMode.Row -> {
                control_type == this.cursor.ctl_type && this.cursor.ctl_level == CtlLineLevel.Global
            }
            OpusManagerCursor.CursorMode.Single -> {
                val cposition = this.cursor.get_position()
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Global
                        && beat == this.cursor.beat
                        && position.size >= cposition.size
                        && position.subList(0, cposition.size) == cposition
            }
            OpusManagerCursor.CursorMode.Range -> {
                val first_beat = min(this.cursor.range!!.first.beat, this.cursor.range!!.second.beat)
                val second_beat = max(this.cursor.range!!.first.beat, this.cursor.range!!.second.beat)

                val range = first_beat .. second_beat
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Global
                        && range.contains(beat)
            }
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }
        }
    }

    fun is_channel_control_selected(control_type: ControlEventType, channel: Int, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat
            }
            OpusManagerCursor.CursorMode.Row -> {
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Channel
                        && this.cursor.channel == channel
            }
            OpusManagerCursor.CursorMode.Single -> {
                val cposition = this.cursor.get_position()
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Channel
                        && this.cursor.channel == channel
                        && beat == this.cursor.beat
                        && position.size >= cposition.size
                        && position.subList(0, cposition.size) == cposition
            }
            OpusManagerCursor.CursorMode.Range,
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }
        }
    }

    fun is_line_control_selected(control_type: ControlEventType, beat_key: BeatKey, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat_key.beat
            }
            OpusManagerCursor.CursorMode.Row -> {
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Line
                        && this.cursor.channel == beat_key.channel
                        && this.cursor.line_offset == beat_key.line_offset
            }
            OpusManagerCursor.CursorMode.Single -> {
                val cposition = this.cursor.get_position()
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Line
                        && this.cursor.channel == beat_key.channel
                        && this.cursor.line_offset == beat_key.line_offset
                        && beat_key.beat == this.cursor.beat
                        && position.size >= cposition.size
                        && position.subList(0, cposition.size) == cposition
            }
            OpusManagerCursor.CursorMode.Range -> {
                beat_key in this.get_beatkeys_in_range(this.cursor.range!!.first, this.cursor.range!!.second)
            }
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }
        }
    }

    fun is_line_control_line_selected(control_type: ControlEventType, channel: Int, line_offset: Int): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Row,
            OpusManagerCursor.CursorMode.Single -> {
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Line
                        && this.cursor.channel == channel
                        && this.cursor.line_offset == line_offset
            }
            OpusManagerCursor.CursorMode.Range -> {
                val target = this.get_abs_offset(channel, line_offset)
                val first = this.get_abs_offset(this.cursor.range!!.first.channel, this.cursor.range!!.first.line_offset)
                val second = this.get_abs_offset(this.cursor.range!!.second.channel, this.cursor.range!!.second.line_offset)
                (first .. second).contains(target)
            }
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Unset -> false
        }
    }

    fun is_channel_control_line_selected(control_type: ControlEventType, channel: Int): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Row,
            OpusManagerCursor.CursorMode.Single -> {
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Channel
                        && this.cursor.channel == channel
            }
            OpusManagerCursor.CursorMode.Range,
            OpusManagerCursor.CursorMode.Unset,
            OpusManagerCursor.CursorMode.Column -> false
        }
    }

    fun is_global_control_line_selected(control_type: ControlEventType): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Row,
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Range -> {
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Global
            }
            OpusManagerCursor.CursorMode.Unset,
            OpusManagerCursor.CursorMode.Column -> false
        }

    }

    fun is_beat_selected(beat: Int): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                (min(first.beat, second.beat) .. max(first.beat, second.beat)).contains(beat)
            }
            else -> false
        }
    }

    /* Not Currently In Use. */
    //fun link_alike() {
    //    if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
    //        this.link_alike(this.cursor.range!!.first, this.cursor.range!!.second)
    //    }
    //}


}
