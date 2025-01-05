package com.qfs.pagan

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.OpusPanEvent
import com.qfs.pagan.opusmanager.OpusReverbEvent
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.OpusVolumeEvent
import com.qfs.pagan.opusmanager.PercussionEvent
import com.qfs.pagan.opusmanager.RelativeNoteEvent
import com.qfs.pagan.opusmanager.TunedInstrumentEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.abs
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class CompoundScrollView(var editor_table: EditorTable): HorizontalScrollView(editor_table.context) {
    class ColumnsLayout(var editor_table: EditorTable): LinearLayout(editor_table.context) {
        val paint = Paint()
        val text_paint_offset = Paint()
        val text_paint_octave = Paint()
        val text_paint_ctl = Paint()
        val text_paint_column = Paint()
        var touch_position_x = 0F
        var touch_position_y = 0F
        init {
            this.paint.color = ContextCompat.getColor(context, R.color.table_lines)
            this.paint.strokeWidth = 3F

            this.text_paint_offset.textSize = resources.getDimension(R.dimen.text_size_offset)
            this.text_paint_offset.color = ContextCompat.getColor(context, R.color.leaf_text_selector)
            this.text_paint_offset.isFakeBoldText = true
            this.text_paint_offset.isAntiAlias = true

            this.text_paint_octave.textSize = resources.getDimension(R.dimen.text_size_octave)
            this.text_paint_octave.color = ContextCompat.getColor(context, R.color.leaf_text_selector)
            this.text_paint_octave.isAntiAlias = true

            this.text_paint_ctl.textSize = resources.getDimension(R.dimen.text_size_ctl)
            this.text_paint_ctl.color = ContextCompat.getColor(context, R.color.ctl_leaf_text_selector)
            this.text_paint_ctl.isAntiAlias = true

            this.text_paint_column.textSize = resources.getDimension(R.dimen.text_size_octave)
            this.text_paint_column.isFakeBoldText = true
            this.text_paint_column.isAntiAlias = true
            this.text_paint_column.strokeWidth = 3F

            this.setWillNotDraw(false)

            this.setOnTouchListener { view: View?, touchEvent: MotionEvent? ->
                if (touchEvent != null) {
                    this.touch_position_y = touchEvent.y
                    this.touch_position_x = touchEvent.x
                }
                false
            }
            this.setOnClickListener {
                this.on_click_listener(this.touch_position_x, this.touch_position_y)
            }
        }


        fun on_click_listener(x: Float, y: Float) {
            var row_position = this.editor_table.get_visible_row_from_pixel(y) ?: return

            val opus_manager = this.editor_table.get_opus_manager()
            val min_leaf_width = resources.getDimension(R.dimen.base_leaf_width).toInt()
            val reduced_x = x / min_leaf_width
            val beat = this.editor_table.get_column_from_leaf(reduced_x.toInt())

            val inner_offset = x - this.editor_table.get_column_offset(beat)
            val column_width = this.editor_table.get_column_width(beat) * min_leaf_width


            if (row_position == -1) {
                opus_manager.cursor_select_column(beat)
            } else {
                val (pointer, ctl_line_level, ctl_type) = opus_manager.get_ctl_line_info(opus_manager.get_ctl_line_from_row(row_position))
                when (ctl_line_level) {
                    null -> {
                        val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                        val beat_key = BeatKey(channel, line_offset, beat)
                        val position = this.calc_position(opus_manager.get_tree(beat_key), column_width, inner_offset)
                        opus_manager.cursor_select(BeatKey(channel, line_offset, beat), position)
                    }

                    CtlLineLevel.Line -> {
                        val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                        val beat_key = BeatKey(channel, line_offset, beat)
                        val position = this.calc_position(opus_manager.get_line_ctl_tree(ctl_type!!, beat_key), column_width, inner_offset)
                        opus_manager.cursor_select_ctl_at_line(ctl_type, beat_key, position)
                    }

                    CtlLineLevel.Channel -> {
                        val position = this.calc_position(opus_manager.get_channel_ctl_tree(ctl_type!!, pointer, beat), column_width, inner_offset)
                        opus_manager.cursor_select_ctl_at_channel(ctl_type, pointer, beat, position)
                    }

                    CtlLineLevel.Global -> {
                        val position = this.calc_position(opus_manager.get_global_ctl_tree(ctl_type!!, beat), column_width, inner_offset)
                        opus_manager.cursor_select_ctl_at_global(ctl_type, beat, position)
                    }
                }
            }
        }

        fun <T: OpusEvent> calc_position(tree: OpusTree<T>, initial_width: Int, target_x: Float): List<Int> {
            var working_width = initial_width.toFloat()
            var working_tree = tree
            var working_x = target_x.toInt()
            val output = mutableListOf<Int>()
            while (!working_tree.is_leaf()) {
                val child_pos = (working_x * working_tree.size / working_width).toInt()
                output.add(child_pos)
                working_width /= working_tree.size
                working_x %= working_width.toInt()
                working_tree = working_tree[child_pos]
            }
            return output
        }

        fun get_standard_leaf_state(beat_key: BeatKey, position: List<Int>): IntArray {
            val opus_manager = this.editor_table.get_opus_manager()

            val tree = opus_manager.get_tree(beat_key, position)
            val original_position = opus_manager.get_actual_position(beat_key, position)
            val tree_original = opus_manager.get_tree(original_position.first, original_position.second)

            val new_state = mutableListOf<Int>()
            if (tree.is_event()) {
                new_state.add(R.attr.state_active)
                val match_cursor = OpusManagerCursor(
                    OpusManagerCursor.CursorMode.Single,
                    original_position.first.channel,
                    original_position.first.line_offset,
                    original_position.first.beat
                )

                if (opus_manager.temporary_blocker == match_cursor) {
                    new_state.add(R.attr.state_invalid)
                } else {
                    when (tree.get_event()) {
                        is RelativeNoteEvent -> {
                            val abs_value = opus_manager.get_absolute_value(beat_key, position)
                            if (abs_value == null || abs_value < 0 || abs_value >= opus_manager.tuning_map.size * 8) {
                                new_state.add(R.attr.state_invalid)
                            }
                        }
                        else -> {}
                    }
                }
            } else if (tree_original != tree) {
                when (tree_original.get_event()) {
                    is RelativeNoteEvent -> {
                        val abs_value = opus_manager.get_absolute_value(original_position.first, original_position.second)
                        if (abs_value == null || abs_value < 0 || abs_value >= opus_manager.tuning_map.size * 8) {
                            new_state.add(R.attr.state_invalid)
                        }
                    }
                    else -> {}
                }
                new_state.add(R.attr.state_spill)
            }

            if (opus_manager.is_selected(beat_key, position)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_secondary_selection(beat_key, position)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            if (beat_key.channel % 2 == 0) {
                new_state.add(R.attr.state_alternate)
            }

            return new_state.toIntArray()
        }

        fun get_global_control_leaf_state(type: ControlEventType, beat: Int, position: List<Int>): IntArray {
            val new_state = mutableListOf<Int>()
            val opus_manager = this.editor_table.get_opus_manager()
            val controller = opus_manager.controllers.get_controller<OpusControlEvent>(type)

            val tree = controller.get_tree(beat, position)
            val original_position = controller.get_blocking_position(beat, position) ?: Pair(beat, position)
            val tree_original = controller.get_tree(original_position.first, original_position.second)

            if (tree.is_event()) {
                new_state.add(R.attr.state_active)
            } else if (tree_original != tree) {
                new_state.add(R.attr.state_spill)
            }

            if (opus_manager.is_global_control_selected(type, beat, position)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_global_control_secondary_selected(type, beat, position)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            return new_state.toIntArray()
        }

        fun get_channel_control_leaf_state(type: ControlEventType, channel: Int, beat: Int, position: List<Int>): IntArray {
            val new_state = mutableListOf<Int>()
            val opus_manager = this.editor_table.get_opus_manager()
            val controller = opus_manager.get_all_channels()[channel].controllers.get_controller<OpusControlEvent>(type)

            val tree = controller.get_tree(beat, position)
            val original_position = controller.get_blocking_position(beat, position) ?: Pair(beat, position)
            val tree_original = controller.get_tree(original_position.first, original_position.second)

            if (tree.is_event()) {
                new_state.add(R.attr.state_active)
            } else if (tree_original != tree) {
                new_state.add(R.attr.state_spill)
            }

            if (opus_manager.is_channel_control_selected(type, channel, beat, position)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_channel_control_secondary_selected(type, channel, beat, position)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            return new_state.toIntArray()
        }

        fun get_line_control_leaf_state(type: ControlEventType, beat_key: BeatKey, position: List<Int>): IntArray {
            val new_state = mutableListOf<Int>()
            val opus_manager = this.editor_table.get_opus_manager()
            val controller = opus_manager.get_line_controller<OpusControlEvent>(type, beat_key.channel, beat_key.line_offset)

            val beat = beat_key.beat
            val tree = controller.get_tree(beat, position)
            val original_position = controller.get_blocking_position(beat, position) ?: Pair(beat, position)
            val tree_original = controller.get_tree(original_position.first, original_position.second)

            if (tree.is_event()) {
                new_state.add(R.attr.state_active)
            } else if (tree_original != tree) {
                new_state.add(R.attr.state_spill)
            }

            if (opus_manager.is_line_control_selected(type, beat_key, position)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_line_control_secondary_selected(type, beat_key, position)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            return new_state.toIntArray()
        }

        fun <T: TunedInstrumentEvent> draw_event(event: T, canvas: Canvas, x: Float, y: Float, width: Float, height: Float) {
            canvas.drawText(
                "$event",
                x + (width / 2),
                y + resources.getDimension(R.dimen.text_size_octave),
                this.text_paint_offset
            )
        }

        fun <T: OpusEvent> draw_tree(canvas: Canvas, tree: OpusTree<T>, position: List<Int>, x: Float, y: Float, width: Float, callback: (T?, List<Int>, Canvas, Float, Float, Float) -> Unit) {
            if (tree.is_leaf()) {
                callback(tree.get_event(), position, canvas, x, y, width)
            } else {
                val new_width = width / tree.size
                for (i in 0 until tree.size) {
                    val child = tree[i]
                    val next_position = OpusLayerBase.next_position(position, i)
                    this.draw_tree<T>(canvas, child, next_position, x + (i * new_width), y, new_width, callback)
                }
            }
        }

        override fun onDraw(canvas: Canvas) {
            // TODO: deal with draw Allocations. preallocate in different function?
            super.onDraw(canvas)
            val base_width = resources.getDimension(R.dimen.base_leaf_width)
            val line_height = resources.getDimension(R.dimen.line_height)
            val ctl_line_height = resources.getDimension(R.dimen.ctl_line_height)
            val channel_gap_height = resources.getDimension(R.dimen.channel_gap_size)

            val first_x = this.editor_table.get_first_visible_column_index()
            val last_x = this.editor_table.get_last_visible_column_index()
            var offset = (this.editor_table.get_column_rect(first_x)?.x ?: 0).toFloat()
            val opus_manager = this.editor_table.get_opus_manager()
            val channels = opus_manager.get_all_channels()

            val column_label_y = (this.parent as ViewGroup).scrollY

            for (i in first_x .. last_x) {
                val beat_width = (this.editor_table.get_column_width(i) * base_width)

                var y_offset = line_height
                for (j in channels.indices) {
                    val channel = channels[j]
                    if (!channel.visible) {
                        continue
                    }
                    for (k in channel.lines.indices) {
                        val line = channel.lines[k]
                        val beat_key = BeatKey(j, k, i)
                        val tree = opus_manager.get_tree(beat_key, listOf())
                        this.draw_tree(canvas, tree, listOf(), offset, y_offset, beat_width) { event, position, canvas, x, y, width ->
                            val state = this.get_standard_leaf_state(beat_key, position)
                            val leaf_drawable = resources.getDrawable(R.drawable.leaf)
                            leaf_drawable.setState(state)
                            leaf_drawable.setBounds(x.toInt(), y.toInt(), (x + width).toInt(), (y + line_height).toInt())
                            leaf_drawable.draw(canvas)

                            val color_list = resources.getColorStateList(R.color.leaf_text_selector)!!
                            this.text_paint_octave.color = color_list.getColorForState(state, Color.MAGENTA)
                            this.text_paint_offset.color = color_list.getColorForState(state, Color.MAGENTA)

                            when (event) {
                                is AbsoluteNoteEvent -> {
                                    val offset_text = "${event.note % opus_manager.tuning_map.size}"
                                    val offset_text_bounds = Rect()
                                    this.text_paint_offset.getTextBounds(offset_text, 0, offset_text.length, offset_text_bounds)

                                    val octave_text = "${event.note / opus_manager.tuning_map.size}"
                                    val octave_text_bounds = Rect()
                                    this.text_paint_octave.getTextBounds(octave_text, 0, octave_text.length, octave_text_bounds)

                                    val padding_y = resources.getDimension(R.dimen.octave_label_padding_y)
                                    val padding_x = resources.getDimension(R.dimen.octave_label_padding_x)
                                    val octave_text_y = y + ((line_height + offset_text_bounds.height()) / 2)

                                    canvas.drawText(
                                        offset_text,
                                        x + ((width - offset_text_bounds.width()) / 2),
                                        octave_text_y,
                                        this.text_paint_offset
                                    )

                                    canvas.drawText(
                                        octave_text,
                                        x + ((width - base_width) / 2) + padding_x,
                                        y + line_height - padding_y,
                                        this.text_paint_octave
                                    )
                                }

                                is RelativeNoteEvent -> {
                                    val offset_text = "${abs(event.offset) % opus_manager.tuning_map.size}"
                                    val offset_text_bounds = Rect()
                                    this.text_paint_offset.getTextBounds(offset_text, 0, offset_text.length, offset_text_bounds)

                                    val octave_text = "${abs(event.offset) / opus_manager.tuning_map.size}"
                                    val octave_text_bounds = Rect()
                                    this.text_paint_octave.getTextBounds(octave_text, 0, octave_text.length, octave_text_bounds)

                                    val prefix_text = if (event.offset < 0) {
                                        context.getString(R.string.pfx_subtract)
                                    } else {
                                        context.getString(R.string.pfx_add)
                                    }
                                    val prefix_text_bounds = Rect()
                                    this.text_paint_octave.getTextBounds(prefix_text, 0, prefix_text.length, prefix_text_bounds)

                                    val padding_y = resources.getDimension(R.dimen.octave_label_padding_y)
                                    val padding_x = resources.getDimension(R.dimen.octave_label_padding_x)
                                    val octave_text_y = y + ((line_height + offset_text_bounds.height()) / 2)

                                    canvas.drawText(
                                        offset_text,
                                        x + ((width - offset_text_bounds.width()) / 2),
                                        octave_text_y,
                                        this.text_paint_offset
                                    )

                                    canvas.drawText(
                                        octave_text,
                                        x + ((width - base_width) / 2) + padding_x,
                                        y + line_height - padding_y,
                                        this.text_paint_octave
                                    )

                                    canvas.drawText(
                                        prefix_text,
                                        (x + ((width - base_width) / 2) + padding_x) + ((offset_text_bounds.width() - prefix_text_bounds.width()) / 2),
                                        octave_text_y - octave_text_bounds.height(),
                                        this.text_paint_octave
                                    )

                                }

                                is PercussionEvent -> {
                                    val offset_text = resources.getString(R.string.percussion_label)
                                    val bounds = Rect()
                                    this.text_paint_offset.getTextBounds(offset_text, 0, offset_text.length, bounds)
                                    canvas.drawText(
                                        offset_text,
                                        x + ((width - bounds.width()) / 2),
                                        y + ((line_height + bounds.height()) / 2),
                                        this.text_paint_offset
                                    )
                                }
                            }
                        }

                        y_offset += line_height

                        for ((type, controller) in line.controllers.get_all()) {
                            if (!controller.visible) {
                                continue
                            }
                            val tree = controller.get_tree(i, listOf())
                            this.draw_tree(canvas, tree, listOf(), offset, y_offset, beat_width) { event, position, canvas, x, y, width ->
                                val state = this.get_line_control_leaf_state(type, beat_key, position)
                                this.process_ctl_event_layout(state, event, canvas, x, y, width, ctl_line_height)
                            }

                            y_offset += ctl_line_height
                        }
                    }

                    for ((type, controller) in channel.controllers.get_all()) {
                        if (!controller.visible) {
                            continue
                        }
                        val tree = controller.get_tree(i, listOf())
                        this.draw_tree(canvas, tree, listOf(), offset, y_offset, beat_width) { event, position, canvas, x, y, width ->
                            val state = this.get_channel_control_leaf_state(type, j, i, position)
                            this.process_ctl_event_layout(state, event, canvas, x, y, width, ctl_line_height)
                        }
                        y_offset += ctl_line_height
                    }
                    canvas.drawRect(offset, y_offset, offset + beat_width, y_offset + channel_gap_height, this.paint)
                    y_offset += channel_gap_height
                }

                for ((type, controller) in opus_manager.controllers.get_all()) {
                    if (!controller.visible) {
                        continue
                    }
                    val tree = controller.get_tree(i, listOf())
                    if (i == 1) {
                        println("${tree.size} OK?")
                    }
                    this.draw_tree(canvas, tree, listOf(), offset, y_offset, beat_width) { event, position, canvas, x, y, width ->
                        val state = this.get_global_control_leaf_state(type, i, position)
                        this.process_ctl_event_layout(state, event, canvas, x, y, width, ctl_line_height )
                    }

                    y_offset += ctl_line_height
                }

                val color_list = resources.getColorStateList(R.color.column_label_text)!!
                val state = this.get_column_label_state(i)
                this.text_paint_column.color = color_list.getColorForState(state, Color.MAGENTA)

                val column_width = this.editor_table.get_column_width(i) * base_width
                val drawable = resources.getDrawable(R.drawable.editor_label_column)
                drawable.setState(state)
                drawable.setBounds(offset.toInt(), column_label_y, (offset + column_width).toInt(), (column_label_y + line_height).toInt())
                drawable.draw(canvas)

                val column_text = "$i"
                val bounds = Rect()
                this.text_paint_column.getTextBounds(column_text, 0, column_text.length, bounds)

                canvas.drawText(
                    "$i",
                    offset - bounds.left + ((column_width - bounds.width()) / 2),
                    column_label_y + ((line_height + bounds.height()) / 2),
                    this.text_paint_column
                )

                offset += beat_width
            }

        }

        private fun get_column_label_state(x: Int): IntArray {
            val new_state = mutableSetOf<Int>()

            val opus_manager = this.editor_table.get_opus_manager()
            if (opus_manager.is_beat_selected(x)) {
                new_state.add(R.attr.state_focused)
            }

            return new_state.toIntArray()
        }

        fun process_ctl_event_layout(state: IntArray, event: OpusControlEvent?, canvas: Canvas, x: Float, y: Float, width: Float, ctl_line_height: Float) {
            val ctl_drawable = resources.getDrawable(R.drawable.ctl_leaf)
            ctl_drawable.setState(state)
            ctl_drawable.setBounds(x.toInt(), y.toInt(), (x + width).toInt(), (y + ctl_line_height).toInt())
            ctl_drawable.draw(canvas)

            val color_list = resources.getColorStateList(R.color.ctl_leaf_text_selector)!!
            this.text_paint_ctl.color = color_list.getColorForState(state, Color.MAGENTA)

            val text = when (event) {
                null -> return
                is OpusVolumeEvent -> (event.value).toString()
                is OpusTempoEvent -> event.value.roundToInt().toString()
                is OpusReverbEvent -> "TODO"
                is OpusPanEvent -> {
                    if (event.value > 0F) {
                        val n = (event.value * 10).roundToInt()
                        "<$n"
                    } else if (event.value < 0F) {
                        val n = 0 - (event.value * 10).roundToInt()
                        "$n>"
                    } else {
                        "0"
                    }
                }
                else -> "???"
            }

            val text_bounds = Rect()
            this.text_paint_ctl.getTextBounds(text, 0, text.length, text_bounds)

            canvas.drawText(
                text,
                x - text_bounds.left + ((width - text_bounds.width()) / 2),
                y + ((ctl_line_height + text_bounds.height()) / 2),
                this.text_paint_ctl
            )
        }


        fun insert_row(y: Int) {
            //for (i in 0 until this.childCount) {
            //    val column = this.get_column(i)
            //    column.insert_cells(y, 1)
            //}
        }

        fun remove_rows(y: Int, count: Int = 1) {
            //for (i in 0 until this.childCount) {
            //    val column = this.get_column(i)
            //    column.remove_cells(y, count)
            //}
        }

        fun add_column(x: Int) {
            //val new_column = ColumnLayout(this.editor_table, x)
            //this.addView(new_column, x)
            this.refreshDrawableState()
        }

        fun add_columns(x: Int, count: Int) {
            //for (i in x until count) {
            //    this.add_column(i)
            //}
            this.refreshDrawableState()
        }

        fun remove_column(x: Int) {
            //this.removeViewAt(x)
            this.refreshDrawableState()
        }

        fun notify_cell_changed(y: Int, x: Int, state_only: Boolean = false) {
           // val column = this.get_column(x)
           // column.notify_item_changed(y, state_only)
            this.invalidate()
        }

        fun notify_column_changed(x: Int, state_only: Boolean = false) {
            //val column = this.get_column(x)
            //if (state_only) {
            //    column.notify_state_changed()
            //} else {
            //    column.rebuild()
            //}
            this.invalidate()
        }

        fun notify_row_change(y: Int, state_only: Boolean = false) {
            //for (x in 0 until this.childCount) {
            //    this.get_column(x).notify_item_changed(y, state_only)
            //}
            this.invalidate()
        }

        fun clear() {
            this.removeAllViews()
        }

    }

    val column_container = ColumnsLayout(editor_table)
    private var _scroll_locked: Boolean = false
    //val column_recycler = ColumnRecycler(editor_table)

    private val _line_label_layout = editor_table.get_line_label_layout()
    val vertical_scroll_view = object : ScrollView(this.context) {
        override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
            super.onScrollChanged(l, t, oldl, oldt)
            this@CompoundScrollView.column_container.invalidate()
        }
    }
    init {
        this.vertical_scroll_view.overScrollMode = OVER_SCROLL_NEVER
        this.vertical_scroll_view.isVerticalScrollBarEnabled = false
        this.vertical_scroll_view.addView(this.column_container)
        this.addView(this.vertical_scroll_view)

        this.vertical_scroll_view.layoutParams.height = MATCH_PARENT
        this.vertical_scroll_view.layoutParams.width = WRAP_CONTENT

        this.overScrollMode = OVER_SCROLL_NEVER
        this.isHorizontalScrollBarEnabled = false
    }

    fun clear() {
        this.vertical_scroll_view.scrollX = 0
        this.vertical_scroll_view.scrollY = 0
        this.scrollX = 0
        this.scrollY = 0
        this.column_container.clear()

    }


    fun lock_scroll() {
        this._scroll_locked = true
    }

    fun unlock_scroll() {
        this._scroll_locked = false
    }

    private fun is_scroll_locked(): Boolean {
        return this._scroll_locked
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        this.column_container.invalidate()
    }
}