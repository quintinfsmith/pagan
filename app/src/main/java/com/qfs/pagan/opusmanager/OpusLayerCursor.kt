package com.qfs.pagan.opusmanager
import com.qfs.pagan.structure.OpusTree
import java.lang.Integer.max
import java.lang.Integer.min

open class OpusLayerCursor: OpusLayerHistory() {
    class InvalidCursorState: Exception()
    var cursor = OpusManagerCursor()
    private var _queued_cursor_selection: OpusManagerCursor? = null

    override fun insert_line(channel: Int, line_offset: Int, line: OpusLineAbstract<*>) {
        super.insert_line(channel, line_offset, line)
    }

    override fun new_line(channel: Int, line_offset: Int?): OpusLineAbstract<*> {
        return super.new_line(channel, line_offset)
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        super.swap_lines(channel_a, line_a, channel_b, line_b)
        this.cursor_select_line(channel_b, line_b)
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusLineAbstract<*> {
        // Need to clear cursor before change since the way the editor_table updates
        // Cursors doesn't take into account changes to row count
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        val output = try {
            super.remove_line(channel, line_offset)
        } catch (e: OpusChannelAbstract.LastLineException) {
            throw e
        }

        if (bkp_cursor.mode == OpusManagerCursor.CursorMode.Line) {
            val working_channel = this.get_channel(bkp_cursor.channel)
            if (bkp_cursor.line_offset < working_channel.size) {
                this.cursor_select_line(bkp_cursor.channel, bkp_cursor.line_offset)
            } else {
                this.cursor_select_line(bkp_cursor.channel, working_channel.size - 1)
            }
        }

        return output
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        super.new_channel(channel, lines, uuid)

        val compare_channel = channel ?: (this.get_channel_count() - 1)
        when (bkp_cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor_select_column(bkp_cursor.beat)
            }
            OpusManagerCursor.CursorMode.Line -> {
                val new_channel = if (compare_channel <= bkp_cursor.channel) {
                    bkp_cursor.channel + 1
                } else {
                    bkp_cursor.channel
                }
                this.cursor_select_line(new_channel, bkp_cursor.line_offset)
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
        this.cursor_clear()
        super.remove_channel(channel)
    }

    override fun remove_beat(beat_index: Int, count: Int) {
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

        super.remove_beat(beat_index, count)
    }

    override fun insert_beats(beat_index: Int, count: Int) {
        val bkp_cursor = this.cursor.copy()

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
            OpusManagerCursor.CursorMode.Line -> {
                this.cursor_select_line(
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
            else -> { }
        }
        super.apply_history_node(current_node, depth)
    }

    private fun remember_cursor() {
        when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.push_to_history_stack(
                    HistoryToken.CURSOR_SELECT_COLUMN,
                    listOf(this.cursor.beat)
                )
            }
            OpusManagerCursor.CursorMode.Line -> {
                when (this.cursor.ctl_level) {
                    CtlLineLevel.Line -> {
                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_LINE_CTL_ROW,
                            listOf(
                                this.cursor.ctl_type!!,
                                this.cursor.channel,
                                this.cursor.line_offset
                            )
                        )
                    }
                    CtlLineLevel.Channel -> {
                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_CHANNEL_CTL_ROW,
                            listOf(
                                this.cursor.ctl_type!!,
                                this.cursor.channel
                            )
                        )
                    }
                    CtlLineLevel.Global -> {
                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_GLOBAL_CTL_ROW,
                            listOf(this.cursor.ctl_type!!)
                        )
                    }
                    null -> {
                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_LINE,
                            listOf(
                                this.cursor.channel,
                                this.cursor.line_offset
                            )
                        )
                    }
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                this.push_to_history_stack(
                    HistoryToken.CURSOR_SELECT_RANGE,
                    listOf(
                        this.cursor.range!!.first.channel,
                        this.cursor.range!!.first.line_offset,
                        this.cursor.range!!.first.beat,
                        this.cursor.range!!.second.channel,
                        this.cursor.range!!.second.line_offset,
                        this.cursor.range!!.second.beat
                    )
                )
            }
            OpusManagerCursor.CursorMode.Single -> {
                when (this.cursor.ctl_level) {
                    CtlLineLevel.Line -> {
                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_LINE_CTL,
                            listOf(
                                this.cursor.ctl_type!!,
                                this.cursor.get_beatkey(),
                                this.cursor.get_position()
                            )
                        )
                    }
                    CtlLineLevel.Channel -> {
                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_CHANNEL_CTL,
                            listOf(
                                this.cursor.ctl_type!!,
                                this.cursor.channel,
                                this.cursor.beat,
                                this.cursor.get_position()
                            )
                        )
                    }
                    CtlLineLevel.Global -> {
                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT_GLOBAL_CTL,
                            listOf(
                                this.cursor.ctl_type!!,
                                this.cursor.beat,
                                this.cursor.get_position()
                            )
                        )
                    }
                    null -> {
                        this.push_to_history_stack(
                            HistoryToken.CURSOR_SELECT,
                            listOf(
                                this.cursor.get_beatkey(),
                                this.cursor.get_position()
                            )
                        )
                    }
                }
            }
            else -> {}
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
                        val real_position = this.get_original_position(beat_key, position)

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

    open fun set_event_at_cursor(event: OpusControlEvent) {
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

    open fun set_event_at_cursor(event: InstrumentEvent) {
        when (this.cursor.ctl_level) {
            null -> {
                val original = this.get_original_position(
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
            this.cursor_select(beat_key, this.get_first_position(beat_key))
        } else if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            val (first, second) = this.cursor.get_ordered_range()!!
            this.unlink_range(first, second)

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
            if (first != second) {
                this.link_beat_range(beat_key, first, second)
            } else {
            this.link_beats(beat_key, first)
            }
        } else {
            throw InvalidCursorState()
        }
    }

    fun merge_into_beat(beat_key: BeatKey) {
        if (this.cursor.is_linking_range()) {
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
        if (this.cursor.is_linking_range()) {
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
        if (this.cursor.is_linking_range()) {
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

    fun copy_global_ctl_to_beat(beat: Int) {
        if (this.cursor.ctl_level != CtlLineLevel.Global) {
            throw InvalidOverwriteCall()
        }

        if (this.cursor.is_linking_range()) {
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
                false
            }
            OpusManagerCursor.CursorMode.Line -> {
                this.cursor.channel == beat_key.channel && this.cursor.line_offset == beat_key.line_offset
            }
            OpusManagerCursor.CursorMode.Range -> {
                false
            }
            OpusManagerCursor.CursorMode.Single -> {
                val cbeat_key = this.cursor.get_beatkey()
                val cposition = this.cursor.get_position()
                cbeat_key == beat_key && position.size >= cposition.size && position.subList(0, cposition.size) == cposition
            }
            OpusManagerCursor.CursorMode.Unset -> {
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

                var output = false
                for ((working_key, working_position) in this.get_all_blocked_positions(beat_key, position)) {
                    if (cbeat_key == working_key && working_position.size >= cposition.size && working_position.subList(0, cposition.size) == cposition) {
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
            else -> {
                false
            }
        }

    }

    fun is_global_control_selected(control_type: ControlEventType, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat
            }
            OpusManagerCursor.CursorMode.Line -> {
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
            OpusManagerCursor.CursorMode.Line -> {
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
            OpusManagerCursor.CursorMode.Line -> {
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

    override fun on_remember() {
        this.remember_cursor()
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




    /* Not Currently In Use. */
    //fun link_alike() {
    //    if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
    //        this.link_alike(this.cursor.range!!.first, this.cursor.range!!.second)
    //    }
    //}
}
