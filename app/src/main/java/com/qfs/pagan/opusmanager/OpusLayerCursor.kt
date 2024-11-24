package com.qfs.pagan.opusmanager
import com.qfs.pagan.structure.OpusTree
import java.lang.Integer.max
import java.lang.Integer.min

open class OpusLayerCursor: OpusLayerHistory() {
    class InvalidCursorState: Exception()
    var cursor = OpusManagerCursor()
    private var _queued_cursor_selection: OpusManagerCursor? = null

    // BASE FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.split_tree(beat_key, position, splits, move_event_to_end)

        if (this.is_selected(beat_key, position) && this.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            val new_position = position.toMutableList()
            new_position.add(0)
            this.cursor_select(beat_key, new_position)
        }
    }
    override fun split_global_ctl_tree(type: ControlEventType, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.split_global_ctl_tree(type, beat, position, splits, move_event_to_end)
        if (this.is_global_control_selected(type, beat, position) && this.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            val new_position = position.toMutableList()
            new_position.add(0)
            this.cursor_select_ctl_at_global(type, beat, new_position)
        }
    }
    override fun split_channel_ctl_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.split_channel_ctl_tree(type, channel, beat, position, splits, move_event_to_end)
        if (this.is_channel_control_selected(type, channel, beat, position) && this.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            val new_position = position.toMutableList()
            new_position.add(0)
            this.cursor_select_ctl_at_channel(type, channel, beat, new_position)
        }
    }
    override fun split_line_ctl_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.split_line_ctl_tree(type, beat_key, position, splits, move_event_to_end)
        if (this.is_line_control_selected(type, beat_key, position) && this.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            val new_position = position.toMutableList()
            new_position.add(0)
            this.cursor_select_ctl_at_line(type, beat_key, new_position)
        }
    }

    override fun new_line(channel: Int, line_offset: Int?) {
        super.new_line(channel, line_offset)
        this._post_new_line(channel, line_offset ?: (this.get_all_channels()[channel].lines.size - 1))
    }
    override fun insert_line(channel: Int, line_offset: Int, line: OpusLineAbstract<*>) {
        super.insert_line(channel, line_offset, line)
        this._post_new_line(channel, line_offset)
    }
    override fun set_channel_visibility(channel_index: Int, visibility: Boolean) {
        super.set_channel_visibility(channel_index, visibility)
        if (!visibility) {
            when (this.cursor.mode) {
                OpusManagerCursor.CursorMode.Line,
                OpusManagerCursor.CursorMode.Channel -> {
                    if (channel_index < this.cursor.channel) {
                        this.cursor.channel -= 1
                    } else if (channel_index == this.cursor.channel) {
                        val channels = this.get_all_channels()
                        var found_channel = false
                        while (this.cursor.channel < this.channels.size) {
                            if (channels[this.cursor.channel].visible) {
                                found_channel = true
                                break
                            }
                            this.cursor.channel += 1
                        }

                        if (!found_channel) {
                            while (this.cursor.channel > 0) {
                                if (channels[this.cursor.channel].visible) {
                                    found_channel = true
                                    break
                                }
                                this.cursor.channel -= 1
                            }
                        }
                    }
                }
                OpusManagerCursor.CursorMode.Single -> {
                    if (channel_index < this.cursor.channel) {
                        this.cursor.channel -= 1
                    } else if (channel_index == this.cursor.channel) {
                        val channels = this.get_all_channels()
                        var found_channel = false
                        while (this.cursor.channel < this.channels.size + 1) {
                            if (channels[this.cursor.channel].visible) {
                                found_channel = true
                                break
                            }
                            this.cursor.channel += 1
                        }

                        if (!found_channel) {
                            while (this.cursor.channel > 0) {
                                if (channels[this.cursor.channel].visible) {
                                    found_channel = true
                                    break
                                }
                                this.cursor.channel -= 1
                            }
                        }
                        this.cursor.line_offset = 0
                        this.cursor.position = this.get_first_position(this.cursor.get_beatkey(), listOf())
                    }
                }
                OpusManagerCursor.CursorMode.Range -> this.cursor_clear()
                OpusManagerCursor.CursorMode.Column,
                OpusManagerCursor.CursorMode.Unset -> {
                    return
                }
            }
            this.cursor_apply(this.cursor.copy())
        }
    }

    override fun remove_line_controller(type: ControlEventType, channel_index: Int, line_offset: Int) {
        super.remove_line_controller(type, channel_index, line_offset)

        if (type == this.cursor.ctl_type) {
            when (this.cursor.mode) {
                OpusManagerCursor.CursorMode.Line -> {
                    if (this.cursor.channel == channel_index && this.cursor.line_offset == line_offset) {
                        this.cursor_select_line(channel_index, line_offset)
                    }
                }
                OpusManagerCursor.CursorMode.Single -> {
                    if (this.cursor.channel == channel_index && this.cursor.line_offset == line_offset) {
                        this.cursor_select(
                            this.cursor.get_beatkey(),
                            this.get_first_position(this.cursor.get_beatkey(), listOf())
                        )
                    }
                }
                OpusManagerCursor.CursorMode.Column,
                OpusManagerCursor.CursorMode.Range,
                OpusManagerCursor.CursorMode.Channel,
                OpusManagerCursor.CursorMode.Unset -> {}
            }
        }
    }
    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        super.swap_lines(channel_a, line_a, channel_b, line_b)
        when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Single -> {
                if (channel_a == this.cursor.channel) {
                    this.cursor.channel = channel_b
                    this.cursor_apply(this.cursor.copy())
                } else if (channel_b == this.cursor.channel) {
                    this.cursor.channel = channel_a
                    this.cursor_apply(this.cursor.copy())
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                // TODO: I should probably calculate if the cursor has to be clear. not a priority right now
                this.cursor.clear()
            }
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Unset -> {}
        }
    }
    override fun remove_line(channel: Int, line_offset: Int): OpusLineAbstract<*> {
        val output = try {
            super.remove_line(channel, line_offset)
        } catch (e: OpusChannelAbstract.LastLineException) {
            throw e
        }

        when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line -> {
                if (this.cursor.channel == channel) {
                    if (this.cursor.line_offset > line_offset) {
                        this.cursor.line_offset -= 1
                        this.cursor_apply(this.cursor.copy())
                    } else if (this.cursor.line_offset == line_offset) {
                        this.cursor.line_offset = max(0, min(this.cursor.line_offset, this.get_all_channels()[channel].lines.size - 1))
                        this.cursor.ctl_type = null
                        this.cursor.ctl_level = null
                        this.cursor_apply(this.cursor.copy())
                    }
                }
            }
            OpusManagerCursor.CursorMode.Single -> {
                if (this.cursor.channel == channel) {
                    if (this.cursor.line_offset > line_offset) {
                        this.cursor.line_offset -= 1
                        this.cursor_apply(this.cursor.copy())
                    } else if (this.cursor.line_offset == line_offset) {
                        this.cursor.line_offset = max(0, min(this.cursor.line_offset - 1, this.get_all_channels()[channel].lines.size - 1))
                        this.cursor.position = this.get_first_position(
                            this.cursor.get_beatkey(),
                            listOf()
                        )
                        this.cursor.ctl_type = null
                        this.cursor.ctl_level = null
                        this.cursor_apply(this.cursor.copy())
                    }
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                this.cursor_clear()
            }
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Unset -> {}
        }

        return output
    }
    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        super.new_channel(channel, lines, uuid)

        val working_channel = channel ?: this.channels.size - 1
        when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Channel -> {
                if (this.cursor.channel >= working_channel) {
                    this.cursor.channel += 1
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.range!!
                if (first.channel >= working_channel) {
                    first.channel += 1
                }
                if (second.channel >= working_channel) {
                    second.channel += 1
                }
            }
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Unset -> {
                // Nothing
            }
        }
        this.cursor_apply(this.cursor.copy())
    }
    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)

        when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Channel -> {
                if (channel < this.cursor.channel) {
                    this.cursor.channel -= 1
                } else if (channel == this.cursor.channel) {
                    val channels = this.get_all_channels()
                    var found_channel = false
                    while (this.cursor.channel < this.channels.size + 1) {
                        if (channels[this.cursor.channel].visible) {
                            found_channel = true
                            break
                        }
                        this.cursor.channel += 1
                    }

                    if (!found_channel) {
                        while (this.cursor.channel > 0) {
                            if (channels[this.cursor.channel].visible) {
                                found_channel = true
                                break
                            }
                            this.cursor.channel -= 1
                        }
                    }
                }
            }
            OpusManagerCursor.CursorMode.Single -> {
                if (channel < this.cursor.channel) {
                    this.cursor.channel -= 1
                } else if (channel == this.cursor.channel) {
                    val channels = this.get_all_channels()
                    var found_channel = false
                    while (this.cursor.channel < this.channels.size + 1) {
                        if (channels[this.cursor.channel].visible) {
                            found_channel = true
                            break
                        }
                        this.cursor.channel += 1
                    }

                    if (!found_channel) {
                        while (this.cursor.channel > 0) {
                            if (channels[this.cursor.channel].visible) {
                                found_channel = true
                                break
                            }
                            this.cursor.channel -= 1
                        }
                    }
                    this.cursor.line_offset = 0
                    this.cursor.position = this.get_first_position(this.cursor.get_beatkey(), listOf())
                }
            }
            OpusManagerCursor.CursorMode.Range -> this.cursor_clear()
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Unset -> {
                // Nothing
            }
        }
        this.cursor_apply(this.cursor.copy())
    }
    override fun remove_beat(beat_index: Int, count: Int) {
        super.remove_beat(beat_index, count)
        when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat = if (this.cursor.beat >= beat_index + count) {
                    this.cursor.beat - count
                } else if (this.cursor.beat == beat_index) {
                    min(max(0, this.cursor.beat), this.beat_count - count - 1)
                } else {
                    this.cursor.beat
                }
            }

            OpusManagerCursor.CursorMode.Range -> {
                val first_corner = this.cursor.range!!.first
                first_corner.beat = if (first_corner.beat >= beat_index + count) {
                    first_corner.beat - count
                } else if (first_corner.beat == beat_index) {
                    min(max(0, first_corner.beat), this.beat_count - count - 1)
                } else {
                    first_corner.beat
                }

                val second_corner = this.cursor.range!!.second
                second_corner.beat = if (second_corner.beat >= beat_index + count) {
                    second_corner.beat - count
                } else if (second_corner.beat == beat_index) {
                    min(max(0, second_corner.beat), this.beat_count - count - 1)
                } else {
                    second_corner.beat
                }
                this.cursor.range = Pair(first_corner, second_corner)
            }

            else -> { }
        }

        this.cursor_apply(this.cursor.copy())
    }
    override fun insert_beats(beat_index: Int, count: Int) {

        super.insert_beats(beat_index, count)

        when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor_select_column(if (this.cursor.beat > beat_index) {
                    this.cursor.beat + 1
                } else {
                    this.cursor.beat
                })
            }
            OpusManagerCursor.CursorMode.Single -> {
                val new_beat = if (this.cursor.beat > beat_index) {
                    this.cursor.beat + 1
                } else {
                    this.cursor.beat
                }
                this.cursor_select(
                    BeatKey(
                        this.cursor.channel,
                        this.cursor.line_offset,
                        new_beat
                    ),
                    this.cursor.position
                )
            }
            OpusManagerCursor.CursorMode.Range -> {
                this.cursor_select_range(
                    BeatKey(
                        this.cursor.range!!.first.channel,
                        this.cursor.range!!.first.line_offset,
                        if (this.cursor.range!!.first.beat > beat_index) {
                            this.cursor.range!!.first.beat + 1
                        } else {
                            this.cursor.range!!.first.beat
                        }
                    ),
                    BeatKey(
                        this.cursor.range!!.second.channel,
                        this.cursor.range!!.second.line_offset,
                        if (this.cursor.range!!.second.beat > beat_index) {
                            this.cursor.range!!.second.beat + 1
                        } else {
                            this.cursor.range!!.second.beat
                        }
                    )
                )
            }
            OpusManagerCursor.CursorMode.Line -> {
                this.cursor_select_line(
                    this.cursor.channel,
                    this.cursor.line_offset
                )
            }
            else -> { }
        }
        this.cursor_apply(this.cursor.copy())
    }
    override fun clear() {
        this.cursor_clear()
        super.clear()
    }
    override fun on_project_changed() {
        super.on_project_changed()
        this.cursor_clear()
    }

    override fun toggle_channel_visibility(channel_index: Int) {
        super.toggle_channel_visibility(channel_index)
        this.cursor_apply(this.cursor.copy())
    }

    override fun set_global_controller_visibility(type: ControlEventType, visibility: Boolean) {
        super.set_global_controller_visibility(type, visibility)
        if (this.cursor.ctl_type == type && !visibility && this.cursor.ctl_level == CtlLineLevel.Global) {
            when (this.cursor.mode) {
                OpusManagerCursor.CursorMode.Line -> {
                    this.cursor.ctl_type = null
                    this.cursor.ctl_level = null
                    this.cursor_apply(this.cursor.copy())
                }
                OpusManagerCursor.CursorMode.Single,
                OpusManagerCursor.CursorMode.Range -> {
                    this.cursor_clear()
                }
                OpusManagerCursor.CursorMode.Column,
                OpusManagerCursor.CursorMode.Channel,
                OpusManagerCursor.CursorMode.Unset -> {}
            }
        }
    }

    override fun set_channel_controller_visibility(type: ControlEventType, channel_index: Int, visibility: Boolean) {
        super.set_channel_controller_visibility(type, channel_index, visibility)
        if (this.cursor.ctl_type == type && !visibility && this.cursor.ctl_level == CtlLineLevel.Channel) {
            when (this.cursor.mode) {
                OpusManagerCursor.CursorMode.Line -> {
                    if (this.cursor.channel == channel_index) {
                        this.cursor.ctl_type = null
                        this.cursor.ctl_level = null
                        this.cursor_apply(this.cursor.copy())
                    }
                }
                OpusManagerCursor.CursorMode.Single -> {
                    if (this.cursor.channel == channel_index) {
                        this.cursor_clear()
                    }
                }
                OpusManagerCursor.CursorMode.Range -> {
                    this.cursor_clear()
                }
                OpusManagerCursor.CursorMode.Column,
                OpusManagerCursor.CursorMode.Channel,
                OpusManagerCursor.CursorMode.Unset -> {}
            }
        }
    }

    override fun set_line_controller_visibility(type: ControlEventType, channel_index: Int, line_offset: Int, visibility: Boolean) {
        super.set_line_controller_visibility(type, channel_index, line_offset, visibility)
        if (this.cursor.ctl_type == type && !visibility && this.cursor.ctl_level == CtlLineLevel.Line) {
            when (this.cursor.mode) {
                OpusManagerCursor.CursorMode.Line -> {
                    if (this.cursor.channel == channel_index && this.cursor.line_offset == line_offset) {
                        this.cursor.ctl_type = null
                        this.cursor.ctl_level = null
                        this.cursor_apply(this.cursor.copy())
                    }
                }
                OpusManagerCursor.CursorMode.Single -> {
                    if (this.cursor.channel == channel_index && this.cursor.line_offset == line_offset) {
                        this.cursor.ctl_type = null
                        this.cursor.ctl_level = null
                        this.cursor.position = this.get_first_position(
                            this.cursor.get_beatkey(),
                            listOf()
                        )
                        this.cursor_apply(this.cursor.copy())
                    }
                }
                OpusManagerCursor.CursorMode.Range -> {
                    this.cursor_clear()
                }
                OpusManagerCursor.CursorMode.Column,
                OpusManagerCursor.CursorMode.Channel,
                OpusManagerCursor.CursorMode.Unset -> {}
            }
        }
    }
    // BASE FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    // HISTORY FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    override fun apply_undo(repeat: Int) {
        if (!this.history_cache.isEmpty()) {
            this.cursor_clear()
        }

        super.apply_undo(repeat)
        this.apply_queued_cursor_select()
    }
    override fun apply_history_node(current_node: HistoryCache.HistoryNode, depth: Int) {
        when (current_node.token) {
            HistoryToken.CURSOR_SELECT_RANGE -> {
                var args = this.checked_cast<List<Int>>(current_node.args)
                val beat_key_a = BeatKey(args[0], args[1], args[2])
                val beat_key_b = BeatKey(args[3], args[4], args[5])
                if (beat_key_a != beat_key_b) {
                    this.queue_cursor_select(
                        OpusManagerCursor(
                            mode = OpusManagerCursor.CursorMode.Range,
                            range = Pair(beat_key_a, beat_key_b)
                        )
                    )
                } else {
                    this.queue_cursor_select(
                        OpusManagerCursor(
                            mode = OpusManagerCursor.CursorMode.Single,
                            channel = beat_key_a.channel,
                            line_offset = beat_key_a.line_offset,
                            beat = beat_key_a.beat
                        )
                    )
                }
            }
            HistoryToken.CURSOR_SELECT_LINE -> {
                var args = this.checked_cast<List<Int>>(current_node.args)
                this.queue_cursor_select(
                    OpusManagerCursor(
                        mode = OpusManagerCursor.CursorMode.Line,
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
                        mode = OpusManagerCursor.CursorMode.Line,
                        ctl_level = CtlLineLevel.Channel,
                        channel = current_node.args[1] as Int,
                        ctl_type = this.checked_cast<ControlEventType>(current_node.args[0])
                    )
                )
            }
            HistoryToken.CURSOR_SELECT_GLOBAL_CTL_ROW -> {
                this.queue_cursor_select(
                    OpusManagerCursor(
                        mode = OpusManagerCursor.CursorMode.Line,
                        ctl_level = CtlLineLevel.Global,
                        ctl_type = this.checked_cast<ControlEventType>(current_node.args[0])
                    )
                )
            }
            HistoryToken.CURSOR_SELECT_CHANNEL -> {
                this.queue_cursor_select(
                    OpusManagerCursor(
                        mode = OpusManagerCursor.CursorMode.Channel,
                        channel = current_node.args[0] as Int,
                        ctl_level = null,
                        ctl_type = null
                    )
                )
            }
            else -> { }
        }
        super.apply_history_node(current_node, depth)
    }
    // HISTORY FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

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

    // Cursor Functions ////////////////////////////////////////////////////////////////////////////
    open fun cursor_apply(cursor: OpusManagerCursor) {
        this.cursor.clear()
        this.cursor = cursor
    }
    open fun cursor_clear() {
        this.cursor.clear()
    }
    open fun cursor_select_channel(channel: Int) {
        this.cursor.select_channel(channel)
    }
    open fun cursor_select_line(channel: Int, line_offset: Int) {
        this.cursor.select_line(channel, line_offset)
    }
    open fun cursor_select_line_ctl_line(ctl_type: ControlEventType, channel: Int, line_offset: Int) {
        this.cursor.select_line_ctl_line(channel, line_offset, ctl_type)
    }
    open fun cursor_select_channel_ctl_line(ctl_type: ControlEventType, channel: Int) {
        this.cursor.select_channel_ctl_line(channel, ctl_type)
    }
    open fun cursor_select_global_ctl_line(ctl_type: ControlEventType) {
        this.cursor.select_global_ctl_line(ctl_type)
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
    open fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.cursor.select_range(beat_key_a, beat_key_b)
    }
    open fun cursor_select_global_ctl_range(type:ControlEventType, first: Int, second: Int) {
        this.cursor.select_global_ctl_range(type, first, second)
    }
    open fun cursor_select_channel_ctl_range(type:ControlEventType, channel: Int, first: Int, second: Int) {
        this.cursor.select_channel_ctl_range(type, channel, first, second)
    }
    open fun cursor_select_line_ctl_range(type: ControlEventType, beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.cursor.select_line_ctl_range(type, beat_key_a, beat_key_b)
    }

    fun set_event_at_cursor(event: OpusControlEvent) {
        when (this.cursor.ctl_level) {
            null -> {
                // TODO: SPECIFY Exception
                throw Exception()
            }
            CtlLineLevel.Global -> {
                this.set_global_ctl_event(
                    this.cursor.ctl_type!!,
                    this.cursor.beat,
                    this.cursor.position,
                    event
                )
            }
            CtlLineLevel.Channel -> {
                this.set_channel_ctl_event(
                    this.cursor.ctl_type!!,
                    this.cursor.channel,
                    this.cursor.beat,
                    this.cursor.position,
                    event
                )
            }
            CtlLineLevel.Line -> {
                this.set_line_ctl_event(
                    this.cursor.ctl_type!!,
                    this.cursor.get_beatkey(),
                    this.cursor.position,
                    event
                )
            }
        }
    }
    fun set_event_at_cursor(event: InstrumentEvent) {
        when (this.cursor.ctl_level) {
            null -> {
                val original = this.get_actual_position(
                    this.cursor.get_beatkey(),
                    this.cursor.get_position()
                )
                this.set_event(
                    original.first,
                    original.second,
                    event as InstrumentEvent
                )
            }
            else -> {
                // TODO: Specifiy Exception
                throw Exception()
            }
        }
    }
    fun set_percussion_event_at_cursor() {
        this.set_percussion_event(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }
    open fun remove_at_cursor(count: Int) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            null -> {
                val beat_key = cursor.get_beatkey()
                val position = cursor.get_position().toMutableList()

                var working_tree = this.get_tree(beat_key).copy()
                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, position, count)

                this.remove(beat_key, position, real_count)

                this.cursor_select(
                    beat_key,
                    this.get_first_position(beat_key, cursor_position)
                )
            }

            CtlLineLevel.Global -> {
                val working_tree = this.get_global_ctl_tree<OpusControlEvent>(cursor.ctl_type!!, cursor.beat).copy()
                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, cursor.get_position(), count)

                this.remove_global_ctl(cursor.ctl_type!!, cursor.beat, cursor.position, real_count)

                this.cursor_select_ctl_at_global(
                    cursor.ctl_type!!,
                    cursor.beat,
                    this.get_first_position_global_ctl(cursor.ctl_type!!, cursor.beat, cursor_position)
                )
            }

            CtlLineLevel.Channel -> {
                val working_tree = this.get_channel_ctl_tree<OpusControlEvent>(cursor.ctl_type!!, cursor.channel, cursor.beat).copy()
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
                val working_tree = this.get_line_ctl_tree<OpusControlEvent>(cursor.ctl_type!!, beat_key).copy()
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

    private fun _post_new_line(channel: Int, line_offset: Int) {
        when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single -> {
                if (this.cursor.channel == channel) {
                    if (this.cursor.line_offset >= line_offset) {
                        this.cursor.line_offset += 1
                    }
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.range!!
                if (first.channel == channel) {
                    if (first.line_offset >= line_offset) {
                        first.line_offset += 1
                    }
                }
                if (second.channel == channel) {
                    if (second.line_offset >= line_offset) {
                        second.line_offset += 1
                    }
                }
                this.cursor.range = Pair(first, second)
            }
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Unset -> return
        }
        this.cursor_apply(this.cursor.copy())
    }

    fun get_tree(): OpusTree<out InstrumentEvent> {
        return this.get_tree(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }
    fun unset() {
        when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Range -> {
                when (this.cursor.ctl_level) {
                    null -> {
                        val (first_key, second_key) = this.cursor.get_ordered_range()!!
                        this.unset_range(first_key, second_key)
                        this.cursor_select(first_key, listOf())
                    }
                    CtlLineLevel.Global -> {
                        val (key_a, key_b) = this.cursor.range!!
                        val start = min(key_a.beat, key_b.beat)
                        val end = max(key_a.beat, key_b.beat)
                        this.unset_global_ctl_range(this.cursor.ctl_type!!, start, end)
                        this.cursor_select_ctl_at_global(this.cursor.ctl_type!!, start, listOf())
                    }
                    CtlLineLevel.Channel -> {
                        val (key_a, key_b) = this.cursor.range!!
                        val start = min(key_a.beat, key_b.beat)
                        val end = max(key_a.beat, key_b.beat)
                        this.unset_channel_ctl_range(this.cursor.ctl_type!!, key_a.channel, start, end)
                        this.cursor_select_ctl_at_channel(this.cursor.ctl_type!!, key_a.channel, start, listOf())
                    }
                    CtlLineLevel.Line -> {
                        val (first_key, second_key) = this.cursor.get_ordered_range()!!
                        this.unset_line_ctl_range(this.cursor.ctl_type!!, first_key, second_key)
                        this.cursor_select_ctl_at_line(this.cursor.ctl_type!!, first_key, listOf())
                    }
                }
            }
            OpusManagerCursor.CursorMode.Single -> {
                when (this.cursor.ctl_level) {
                    null -> {
                        val beat_key = this.cursor.get_beatkey()
                        val position = this.cursor.get_position()
                        val real_position = this.get_actual_position(beat_key, position)

                        this.unset(real_position.first, real_position.second)
                        this.cursor_select(beat_key, position)
                    }
                    CtlLineLevel.Global -> {
                        val beat = this.cursor.beat
                        val position = this.cursor.get_position()

                        this.unset_global_ctl(this.cursor.ctl_type!!, beat, position)
                        this.cursor_select_ctl_at_global(this.cursor.ctl_type!!, beat, position)
                    }
                    CtlLineLevel.Channel -> {
                        val channel = this.cursor.channel
                        val beat = this.cursor.beat
                        val position = this.cursor.get_position()

                        this.unset_channel_ctl(this.cursor.ctl_type!!, channel, beat, position)
                        this.cursor_select_ctl_at_channel(this.cursor.ctl_type!!, channel, beat, position)
                    }
                    CtlLineLevel.Line -> {
                        val beat_key = this.cursor.get_beatkey()
                        val position = this.cursor.get_position()

                        this.unset_line_ctl(this.cursor.ctl_type!!, beat_key, position)
                        this.cursor_select_ctl_at_line(this.cursor.ctl_type!!, beat_key, position)
                    }
                }

            }
            OpusManagerCursor.CursorMode.Column -> {
                this.unset_beat(this.cursor.beat)
            }
            OpusManagerCursor.CursorMode.Line -> {
                when (this.cursor.ctl_level) {
                    null -> {
                        this.unset_line(this.cursor.channel, this.cursor.line_offset)
                    }
                    CtlLineLevel.Global -> {
                        this.unset_global_ctl_line(this.cursor.ctl_type!!)
                    }
                    CtlLineLevel.Channel -> {
                        val channel = this.cursor.channel
                        this.unset_channel_ctl_line(this.cursor.ctl_type!!, channel)
                    }
                    CtlLineLevel.Line -> {
                        this.unset_line_ctl_line(this.cursor.ctl_type!!, this.cursor.channel, this.cursor.line_offset)
                    }
                }
            }
            OpusManagerCursor.CursorMode.Channel -> {
                TODO()
            }
            OpusManagerCursor.CursorMode.Unset -> {}
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
    fun set_percussion_instrument(instrument: Int) {
        this.set_percussion_instrument(
            this.cursor.line_offset,
            instrument
        )
    }
    fun split_tree_at_cursor(splits: Int, move_event_to_end: Boolean = false) {
        this.split_tree(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            splits,
            move_event_to_end
        )
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
    fun insert_line_at_cursor(count: Int) {
        this.new_lines(
            this.cursor.channel,
            this.cursor.line_offset + 1,
            count
        )
    }
    fun remove_line_at_cursor(count: Int) {
        this.remove_lines(
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
    fun merge_into_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            if (first != second) {
                TODO()
            } else {
                if (this.is_percussion(first.channel) != this.is_percussion(beat_key.channel)) {
                    throw MixedInstrumentException(first, beat_key)
                }
                this.merge_leafs(first, listOf(), beat_key, listOf())
            }
        } else {
            throw InvalidCursorState()
        }
    }
    fun move_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            if (first != second) {
                this.move_beat_range(beat_key, first, second)
            } else {
                if (this.is_percussion(first.channel) != this.is_percussion(beat_key.channel)) {
                    throw MixedInstrumentException(first, beat_key)
                }
                this.move_leaf(first, listOf(), beat_key, listOf())
            }
        } else {
            throw InvalidCursorState()
        }
    }
    fun copy_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            if (first != second) {
                this.overwrite_beat_range(beat_key, first, second)
            } else {
                if (this.is_percussion(first.channel) != this.is_percussion(beat_key.channel)) {
                    throw MixedInstrumentException(first, beat_key)
                }
                this.replace_tree(
                    beat_key,
                    listOf(),
                    this.get_tree_copy(
                        first,
                        listOf()
                    )
                )
            }
        } else {
            throw InvalidCursorState()
        }
    }
    fun copy_line_ctl_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            if (first != second) {
                this.overwrite_line_ctl_range(this.cursor.ctl_type!!, beat_key, first, second)
            } else {
                if (this.is_percussion(first.channel) != this.is_percussion(beat_key.channel)) {
                    throw MixedInstrumentException(first, beat_key)
                }
                this.replace_line_ctl_tree(
                    this.cursor.ctl_type!!,
                    beat_key,
                    listOf(),
                    this.get_line_ctl_tree_copy(this.cursor.ctl_type!!, first, listOf())
                )
            }
        } else {
            throw InvalidCursorState()
        }

        val tree = this.get_line_ctl_tree<OpusControlEvent>(
            this.cursor.ctl_type!!,
            beat_key,
            listOf()
        )

        this.cursor_select_ctl_at_line(
            this.cursor.ctl_type!!,
            beat_key,
            tree.get_first_event_tree_position() ?: listOf()
        )
    }
    fun move_line_ctl_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            if (first != second) {
                this.move_line_ctl_range(this.cursor.ctl_type!!, beat_key, first, second)
            } else {
                this.move_line_ctl_leaf(this.cursor.ctl_type!!, beat_key, listOf(), first, listOf())
            }
        } else {
            throw InvalidCursorState()
        }

        val tree = this.get_line_ctl_tree<OpusControlEvent>(
            this.cursor.ctl_type!!,
            beat_key,
            listOf()
        )

        this.cursor_select_ctl_at_line(
            this.cursor.ctl_type!!,
            beat_key,
            tree.get_first_event_tree_position() ?: listOf()
        )
    }
    fun copy_global_ctl_to_beat(beat: Int) {
        if (this.cursor.ctl_level != CtlLineLevel.Global) {
            throw InvalidOverwriteCall()
        }

        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            if (first != second) {
                this.overwrite_global_ctl_range(
                    this.cursor.ctl_type!!,
                    beat,
                    min(first.beat, second.beat),
                    max(first.beat, second.beat)
                )
            } else {
                this.replace_global_ctl_tree(
                    this.cursor.ctl_type!!,
                    beat,
                    listOf(),
                    this.get_global_ctl_tree(
                        this.cursor.ctl_type!!,
                        first.beat,
                        listOf()
                    )
                )
            }
        } else {
            throw InvalidCursorState()
        }

        val tree = this.get_global_ctl_tree<OpusControlEvent>(
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

        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            if (first != second) {
                this.move_global_ctl_range(
                    this.cursor.ctl_type!!,
                    beat,
                    first.beat,
                    second.beat
                )
            } else {
                this.move_global_ctl_leaf(
                    this.cursor.ctl_type!!,
                    first.beat,
                    listOf(),
                    beat,
                    listOf()
                )
            }
        } else {
            throw InvalidCursorState()
        }

        val tree = this.get_global_ctl_tree<OpusControlEvent>(
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

    fun move_to_previous_visible_line(repeat: Int = 1) {
        val cursor = this.cursor
        if (cursor.mode != OpusManagerCursor.CursorMode.Line) {
            throw Exception("Incorrect Cursor Mode ${cursor.mode}")
        }

        var visible_row = when (cursor.ctl_level) {
            null -> {
                this.get_visible_row_from_ctl_line(
                    this.get_actual_line_index(
                        this.get_instrument_line_index(
                            cursor.channel,
                            cursor.line_offset
                        )
                    )
                )

            }
            CtlLineLevel.Line -> {
                this.get_visible_row_from_ctl_line_line(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.line_offset
                )
            }
            CtlLineLevel.Channel -> {
                this.get_visible_row_from_ctl_line_channel(
                    cursor.ctl_type!!,
                    cursor.channel
                )
            }
            CtlLineLevel.Global -> this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
        }!!

        visible_row = kotlin.math.max(0, visible_row - repeat)

        val (pointer, control_level, control_type) = this.get_ctl_line_info(
            this.get_ctl_line_from_row(visible_row)
        )

        when (control_level) {
            null -> {
                val (new_channel, new_line_offset) = this.get_channel_and_line_offset(pointer)
                this.cursor_select_line(new_channel, new_line_offset)

            }
            CtlLineLevel.Line -> {
                val (new_channel, new_line_offset) = this.get_channel_and_line_offset(pointer)
                this.cursor_select_line_ctl_line(
                    control_type!!,
                    new_channel,
                    new_line_offset,
                )
            }
            CtlLineLevel.Channel -> {
                this.cursor_select_channel_ctl_line(
                    control_type!!,
                    pointer
                )
            }
            CtlLineLevel.Global -> this.cursor_select_global_ctl_line(control_type!!)
        }
    }
    fun move_to_next_visible_line(repeat: Int = 1) {
        val cursor = this.cursor
        if (cursor.mode != OpusManagerCursor.CursorMode.Line) {
            throw Exception("Incorrect Cursor Mode ${cursor.mode}")
        }

        var visible_row = when (cursor.ctl_level) {
            null -> {
                this.get_visible_row_from_ctl_line(
                    this.get_actual_line_index(
                        this.get_instrument_line_index(
                            cursor.channel,
                            cursor.line_offset
                        )
                    )
                )

            }
            CtlLineLevel.Line -> {
                this.get_visible_row_from_ctl_line_line(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.line_offset
                )
            }
            CtlLineLevel.Channel -> {
                this.get_visible_row_from_ctl_line_channel(
                    cursor.ctl_type!!,
                    cursor.channel
                )
            }
            CtlLineLevel.Global -> this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
        }!!

        visible_row = kotlin.math.max(0, kotlin.math.min(this.get_total_line_count() - 1, visible_row + repeat))

        val (pointer, control_level, control_type) = this.get_ctl_line_info(
            this.get_ctl_line_from_row(visible_row)
        )

        when (control_level) {
            null -> {
                val (new_channel, new_line_offset) = this.get_channel_and_line_offset(pointer)
                this.cursor_select_line(new_channel, new_line_offset)

            }
            CtlLineLevel.Line -> {
                val (new_channel, new_line_offset) = this.get_channel_and_line_offset(pointer)
                this.cursor_select_line_ctl_line(
                    control_type!!,
                    new_channel,
                    new_line_offset,
                )
            }
            CtlLineLevel.Channel -> {
                this.cursor_select_channel_ctl_line(
                    control_type!!,
                    pointer
                )
            }
            CtlLineLevel.Global -> this.cursor_select_global_ctl_line(control_type!!)
        }
    }
    fun select_next_leaf(repeat: Int) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_line_ctl_proceding_leaf_position(
                        cursor.ctl_type!!,
                        working_beat_key,
                        working_position
                    ) ?: break

                    working_beat_key.beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(cursor.ctl_type!!, working_beat_key, working_position)
            }

            CtlLineLevel.Channel -> {
                var working_beat = cursor.beat
                val channel = cursor.channel
                var working_position = cursor.position
                val controller = this.channels[channel].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_proceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_channel(cursor.ctl_type!!, channel, working_beat, working_position)
            }

            CtlLineLevel.Global -> {
                var working_beat = cursor.beat
                var working_position = cursor.position
                for (i in 0 until repeat) {
                    val next_pair = this.get_global_ctl_proceding_leaf_position(
                        cursor.ctl_type!!,
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_global(cursor.ctl_type!!, working_beat, working_position)

            }
            null -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_proceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select(working_beat_key, working_position)
            }
        }
    }
    fun select_previous_leaf(repeat: Int) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()
                val controller = this.channels[working_beat_key.channel].lines[working_beat_key.line_offset].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat_key.beat,
                        working_position
                    ) ?: break

                    working_beat_key.beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(cursor.ctl_type!!, working_beat_key, working_position)
            }

            CtlLineLevel.Channel -> {
                var working_beat = cursor.beat
                val channel = cursor.channel
                var working_position = cursor.position
                val controller = this.channels[channel].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_channel(cursor.ctl_type!!, channel, working_beat, working_position)
            }

            CtlLineLevel.Global -> {
                var working_beat = cursor.beat
                var working_position = cursor.position
                val controller = this.controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)
                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_global(cursor.ctl_type!!, working_beat, working_position)

            }
            null -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_preceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select(working_beat_key, working_position)
            }
        }
    }
    fun select_first_leaf_in_previous_beat(repeat: Int = 1) {
        when (this.cursor.ctl_level) {
            CtlLineLevel.Line -> {
                var working_beat = this.cursor.beat
                var working_position = this.cursor.position
                val controller = this.channels[this.cursor.channel].lines[this.cursor.line_offset].controllers.get_controller<OpusControlEvent>(this.cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(this.cursor.ctl_type!!, BeatKey(working_beat, this.cursor.channel, this.cursor.line_offset), working_position)
            }
            CtlLineLevel.Channel -> {
                var working_beat = this.cursor.beat
                val channel = this.cursor.channel
                var working_position = this.cursor.position
                val controller = this.channels[channel].controllers.get_controller<OpusControlEvent>(this.cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_channel(this.cursor.ctl_type!!, channel, working_beat, working_position)
            }
            CtlLineLevel.Global -> {
                var working_beat = this.cursor.beat
                var working_position = this.cursor.position
                val controller = this.controllers.get_controller<OpusControlEvent>(this.cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_global(this.cursor.ctl_type!!, working_beat, working_position)

            }
            null -> {
                var working_beat_key = this.cursor.get_beatkey()
                var working_position = this.cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_preceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select(working_beat_key, working_position)
            }
        }
    }
    fun select_first_leaf_in_next_beat(repeat: Int = 1) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_line_ctl_proceding_leaf_position(
                        cursor.ctl_type!!,
                        working_beat_key,
                        working_position
                    ) ?: break

                    working_beat_key.beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(cursor.ctl_type!!, working_beat_key, working_position)
            }
            CtlLineLevel.Channel -> {
                var working_beat = cursor.beat
                val channel = cursor.channel
                var working_position = cursor.position
                val controller = this.channels[channel].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_proceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_channel(cursor.ctl_type!!, channel, working_beat, working_position)
            }
            CtlLineLevel.Global -> {
                var working_beat = cursor.beat
                var working_position = cursor.position
                for (i in 0 until repeat) {
                    val next_pair = this.get_global_ctl_proceding_leaf_position(
                        cursor.ctl_type!!,
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_global(cursor.ctl_type!!, working_beat, working_position)

            }
            null -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_proceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select(working_beat_key, working_position)
            }
        }
    }
    // End Cursor Functions ////////////////////////////////////////////////////////////////////////

    fun is_selected(beat_key: BeatKey, position: List<Int>): Boolean {
        if (this.cursor.ctl_level != null) {
            return false
        }

        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line -> {
                false
            }
            OpusManagerCursor.CursorMode.Single -> {
                val cbeat_key = this.cursor.get_beatkey()
                val cposition = this.cursor.get_position()
                cbeat_key == beat_key && position.size >= cposition.size && position.subList(0, cposition.size) == cposition
            }
            OpusManagerCursor.CursorMode.Range,
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }

            OpusManagerCursor.CursorMode.Channel -> {
                false
            }
        }
    }
    fun is_secondary_selection(beat_key: BeatKey, position: List<Int>): Boolean {
        if (this.cursor.ctl_level != null) {
            return false
        }

        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                val cbeat_key = this.cursor.get_beatkey()
                val cposition = this.cursor.get_position()
                if (cbeat_key == beat_key && position.size >= cposition.size && position.subList(0, cposition.size) == cposition) {
                    return false
                }
                if (cbeat_key.channel != beat_key.channel || cbeat_key.line_offset != beat_key.line_offset) {
                    return false
                }

                var output = false
                val line = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset]
                for ((working_beat, working_position) in line.get_all_blocked_positions(beat_key.beat, position)) {
                    if (working_beat == beat_key.beat && position == working_position) {
                        continue
                    }
                    if (cbeat_key.beat == working_beat && working_position.size >= cposition.size && working_position.subList(0, cposition.size) == cposition) {
                        output = true
                        break
                    }
                }
                output
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                beat_key in this.get_beatkeys_in_range(first, second)
            }
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat_key.beat
            }
            OpusManagerCursor.CursorMode.Line -> {
                this.cursor.line_offset == beat_key.line_offset && this.cursor.channel == beat_key.channel
            }
            OpusManagerCursor.CursorMode.Channel -> {
                beat_key.channel == this.cursor.channel
            }
            else -> {
                false
            }
        }

    }
    fun is_global_control_selected(control_type: ControlEventType, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                if (this.cursor.ctl_level == CtlLineLevel.Global && control_type == this.cursor.ctl_type) {
                    val cposition = this.cursor.get_position()
                    beat == this.cursor.beat && position.size >= cposition.size && position.subList(0, cposition.size) == cposition
                } else {
                    false
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val first_beat = min(this.cursor.range!!.first.beat, this.cursor.range!!.second.beat)
                val second_beat = max(this.cursor.range!!.first.beat, this.cursor.range!!.second.beat)

                (this.cursor.ctl_level == CtlLineLevel.Global && control_type == this.cursor.ctl_type) && (beat == second_beat || beat == first_beat)
            }
            OpusManagerCursor.CursorMode.Unset,
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Line -> {
                false
            }

        }
    }
    fun is_global_control_secondary_selected(control_type: ControlEventType, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                if (this.cursor.ctl_level != CtlLineLevel.Global || control_type != this.cursor.ctl_type) {
                    false
                } else {
                    val cbeat = this.cursor.beat
                    val cposition = this.cursor.get_position()
                    if (cbeat == beat && position.size >= cposition.size && position.subList(0, cposition.size) == cposition) {
                        return false
                    }

                    var output = false
                    val controller = this.controllers.get_controller<OpusControlEvent>(control_type)
                    for ((working_beat, working_position) in controller.get_all_blocked_positions(beat, position)) {
                        if (working_beat == beat && position == working_position) {
                            continue
                        }

                        if (cbeat == working_beat && working_position.size >= cposition.size && working_position.subList(0, cposition.size) == cposition) {
                            output = true
                            break
                        }
                    }
                    output
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                (this.cursor.ctl_level == CtlLineLevel.Global && control_type == this.cursor.ctl_type) && (beat in first.beat + 1 until second.beat)
            }
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat
            }
            OpusManagerCursor.CursorMode.Line -> {
                (this.cursor.ctl_level == CtlLineLevel.Global && control_type == this.cursor.ctl_type)
            }
            else -> {
                false
            }
        }
    }
    fun is_channel_control_selected(control_type: ControlEventType, channel: Int, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                val cposition = this.cursor.get_position()
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Channel
                        && this.cursor.channel == channel
                        && beat == this.cursor.beat
                        && position.size >= cposition.size
                        && position.subList(0, cposition.size) == cposition
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                (beat == first.beat || beat == second.beat) && (this.cursor.ctl_level == CtlLineLevel.Channel && this.cursor.ctl_type == control_type)
            }
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }

        }
    }
    fun is_channel_control_secondary_selected(control_type: ControlEventType, channel: Int, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                beat == this.cursor.beat
            }
            OpusManagerCursor.CursorMode.Line -> {
                this.cursor.channel == channel && this.cursor.ctl_level == CtlLineLevel.Channel && this.cursor.ctl_type == control_type
            }
            OpusManagerCursor.CursorMode.Single -> {
                if (this.cursor.ctl_level != CtlLineLevel.Channel || this.cursor.ctl_type != control_type) {
                    false
                } else {
                    val cbeat = this.cursor.beat
                    val cposition = this.cursor.get_position()
                    if (cbeat == beat && position.size >= cposition.size && position.subList(0, cposition.size) == cposition) {
                        return false
                    }

                    var output = false
                    val controller = this.get_all_channels()[channel].controllers.get_controller<OpusControlEvent>(control_type)
                    for ((working_beat, working_position) in controller.get_all_blocked_positions(beat, position)) {
                        if (working_beat == beat && position == working_position) {
                            continue
                        }

                        if (cbeat == working_beat && working_position.size >= cposition.size && working_position.subList(0, cposition.size) == cposition) {
                            output = true
                            break
                        }
                    }
                    output
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                beat in first.beat + 1 until second.beat && this.cursor.ctl_level == CtlLineLevel.Channel && this.cursor.ctl_type == control_type
            }

            OpusManagerCursor.CursorMode.Channel -> {
                this.cursor.ctl_level != CtlLineLevel.Global && this.cursor.channel == channel
            }
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }
        }
    }
    fun is_line_control_selected(control_type: ControlEventType, beat_key: BeatKey, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                val cposition = this.cursor.get_position()
                this.cursor.channel == beat_key.channel
                        && control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Line
                        && this.cursor.line_offset == beat_key.line_offset
                        && beat_key.beat == this.cursor.beat
                        && position.size >= cposition.size
                        && position.subList(0, cposition.size) == cposition
            }
            OpusManagerCursor.CursorMode.Range -> {
                (beat_key == this.cursor.range!!.first || beat_key == this.cursor.range!!.second) && control_type == this.cursor.ctl_type && this.cursor.ctl_level == CtlLineLevel.Line
            }
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Unset,
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Line -> {
                false
            }
        }
    }
    fun is_line_control_secondary_selected(control_type: ControlEventType, beat_key: BeatKey, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat_key.beat
            }
            OpusManagerCursor.CursorMode.Line -> {
                this.cursor.channel == beat_key.channel
                        && this.cursor.line_offset == beat_key.line_offset
                        && control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Line
            }
            OpusManagerCursor.CursorMode.Single -> {
                if (this.cursor.ctl_level != CtlLineLevel.Line || control_type != this.cursor.ctl_type) {
                     false
                } else {
                    val cbeat = this.cursor.beat
                    val cposition = this.cursor.get_position()
                    val beat = beat_key.beat
                    if (cbeat == beat && position.size >= cposition.size && position.subList(0, cposition.size) == cposition) {
                        return false
                    }

                    var output = false
                    val controller = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].controllers.get_controller<OpusControlEvent>(control_type)
                    for ((working_beat, working_position) in controller.get_all_blocked_positions(beat, position)) {
                        if (working_beat == beat && position == working_position) {
                            continue
                        }

                        if (cbeat == working_beat && working_position.size >= cposition.size && working_position.subList(0, cposition.size) == cposition) {
                            output = true
                            break
                        }
                    }
                    output
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                control_type == this.cursor.ctl_type && this.cursor.ctl_level == CtlLineLevel.Line && beat_key in this.get_beatkeys_in_range(first, second)
            }
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }

            OpusManagerCursor.CursorMode.Channel -> {
                this.cursor.channel == beat_key.channel
            }
        }
    }
    fun is_line_control_line_selected(control_type: ControlEventType, channel: Int, line_offset: Int): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single -> {
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Line
                        && this.cursor.channel == channel
                        && this.cursor.line_offset == line_offset
            }
            OpusManagerCursor.CursorMode.Range -> {
                val target = this.get_instrument_line_index(channel, line_offset)
                val first = this.get_instrument_line_index(this.cursor.range!!.first.channel, this.cursor.range!!.first.line_offset)
                val second = this.get_instrument_line_index(this.cursor.range!!.second.channel, this.cursor.range!!.second.line_offset)
                (first .. second).contains(target)
            }
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Unset -> false
            OpusManagerCursor.CursorMode.Channel -> {
                channel == this.cursor.channel
            }

        }
    }
    fun is_channel_control_line_selected(control_type: ControlEventType, channel: Int): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single -> {
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Channel
                        && this.cursor.channel == channel
            }
            OpusManagerCursor.CursorMode.Channel -> {
                channel == this.cursor.channel
            }
            OpusManagerCursor.CursorMode.Range,
            OpusManagerCursor.CursorMode.Unset,
            OpusManagerCursor.CursorMode.Column -> false
        }
    }
    fun is_global_control_line_selected(control_type: ControlEventType): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Range -> {
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Global
            }
            OpusManagerCursor.CursorMode.Channel,
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
    fun select_first_in_beat(beat: Int) {
        when (this.cursor.ctl_level) {
            null -> {
                val new_beat_key = BeatKey(this.cursor.channel, cursor.line_offset, beat)
                val new_position = this.get_first_position(new_beat_key, listOf())
                this.cursor_select(
                    new_beat_key,
                    new_position
                )
            }
            CtlLineLevel.Line -> {
                val new_beat_key = BeatKey(this.cursor.channel, cursor.line_offset, beat)
                val new_position = this.get_first_position_line_ctl(this.cursor.ctl_type!!, new_beat_key, listOf())
                this.cursor_select_ctl_at_line(
                    this.cursor.ctl_type!!,
                    new_beat_key,
                    new_position
                )
            }
            CtlLineLevel.Channel -> {
                val new_position = this.get_first_position_channel_ctl(this.cursor.ctl_type!!, cursor.channel, beat, listOf())
                this.cursor_select_ctl_at_channel(
                    this.cursor.ctl_type!!,
                    this.cursor.channel,
                    beat,
                    new_position
                )
            }
            CtlLineLevel.Global -> {
                val new_position = this.get_first_position_global_ctl(this.cursor.ctl_type!!, beat, listOf())
                this.cursor_select_ctl_at_global(
                    this.cursor.ctl_type!!,
                    beat,
                    new_position
                )
            }
        }
    }

    fun <T: OpusControlEvent> set_initial_event(event: T) {
        when (this.cursor.ctl_level) {
            null -> return
            CtlLineLevel.Line -> this.set_line_controller_initial_event(this.cursor.ctl_type!!, this.cursor.channel, this.cursor.line_offset, event)
            CtlLineLevel.Channel -> this.set_channel_controller_initial_event(this.cursor.ctl_type!!, this.cursor.channel, event)
            CtlLineLevel.Global -> this.set_global_controller_initial_event(this.cursor.ctl_type!!, event)
        }
    }

    fun get_active_active_control_set(): ActiveControlSet? {
        val channels = this.get_all_channels()

        val cursor = this.cursor
        return when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                channels[cursor.channel].lines[cursor.line_offset].controllers
            }
            CtlLineLevel.Channel -> {
                val channel = cursor.channel
                channels[channel].controllers
            }
            CtlLineLevel.Global -> {
                this.controllers
            }
            else -> null
        }
    }

    fun get_nth_next_channel_at_cursor(n: Int): Int? {
        return when (cursor.mode) {
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single -> {
                val start_channel = when (cursor.ctl_level) {
                    CtlLineLevel.Global -> 0
                    null,
                    CtlLineLevel.Line,
                    CtlLineLevel.Channel -> cursor.channel
                }

                kotlin.math.max(0, kotlin.math.min(start_channel + n, this.get_visible_channel_count() - 1))
            }

            OpusManagerCursor.CursorMode.Column -> {
                kotlin.math.max(0, kotlin.math.min(n - 1, this.get_visible_channel_count() - 1))
            }

            OpusManagerCursor.CursorMode.Range,
            OpusManagerCursor.CursorMode.Unset -> null
        }
    }

}
