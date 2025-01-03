package com.qfs.pagan

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.PercussionEvent
import com.qfs.pagan.opusmanager.RelativeNoteEvent
import com.qfs.pagan.opusmanager.TunedInstrumentEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class CompoundScrollView(var editor_table: EditorTable): HorizontalScrollView(editor_table.context) {
    class ColumnsLayout(var editor_table: EditorTable): LinearLayout(editor_table.context) {
        val paint = Paint()
        val text_paint_offset = Paint()
        val text_paint_octave = Paint()
        var touch_position_x = 0F
        var touch_position_y = 0F
        init {
            this.paint.color = resources.getColor(R.color.table_lines)
            this.paint.strokeWidth = 3F
            this.text_paint_offset.textSize = resources.getDimension(R.dimen.text_size_offset)
            this.text_paint_offset.textAlign = Paint.Align.CENTER
            this.text_paint_octave.textSize = resources.getDimension(R.dimen.text_size_octave)
            this.text_paint_octave.textAlign = Paint.Align.CENTER
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
            val min_leaf_width = resources.getDimension(R.dimen.base_leaf_width).roundToInt()
            val reduced_x = x / min_leaf_width
            val column_position = this.editor_table.get_column_from_leaf(reduced_x.toInt())

            var row_position = this.editor_table.get_visible_row_from_pixel(y)
            println("CLICKED: $column_position, $row_position")
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

        fun <T: OpusEvent> draw_tree(canvas: Canvas, tree: OpusTree<T>, position: List<Int>, x: Float, y: Float, width: Float, height: Float, callback: (T?, List<Int>, Canvas, Float, Float, Float, Float) -> Unit) {
            if (tree.is_leaf()) {
                callback(tree.get_event(), position, canvas, x, y, width, height)
            } else {
                val new_width = width / tree.size
                for (i in 0 until tree.size) {
                    val child = tree[i]
                    val next_position = OpusLayerBase.next_position(position, i)
                    this.draw_tree<T>(canvas, child, next_position, x + (i * new_width), y, new_width, height, callback)
                }
            }
        }

        override fun onDraw(canvas: Canvas) {
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

            for (i in first_x .. last_x) {
                val beat_width = (this.editor_table.get_column_width(i) * base_width)

                var y_offset = 0F
                for (j in channels.indices) {
                    val channel = channels[j]
                    if (!channel.visible) {
                        continue
                    }
                    for (k in channel.lines.indices) {
                        val line = channel.lines[k]
                        val beat_key = BeatKey(j, k, i)
                        val tree = opus_manager.get_tree(beat_key, listOf())
                        this.draw_tree(canvas, tree, listOf(), offset, y_offset, beat_width, line_height) { event, position, canvas, x, y, width, height ->
                            val leaf_drawable = resources.getDrawable(R.drawable.leaf)
                            leaf_drawable.setState(this.get_standard_leaf_state(beat_key, position))
                            leaf_drawable.setBounds(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt())
                            leaf_drawable.draw(canvas)

                            when (event) {
                                is AbsoluteNoteEvent -> {
                                    canvas.drawText(
                                        "${event.note / opus_manager.tuning_map.size}",
                                        x + (width / 3),
                                        y + height,
                                        this.text_paint_octave
                                    )
                                    canvas.drawText(
                                        "${event.note % opus_manager.tuning_map.size}",
                                        x + (width / 2),
                                        y + ((height + resources.getDimension(R.dimen.text_size_offset)) / 2),
                                        this.text_paint_offset
                                    )
                                }
                                is RelativeNoteEvent -> {
                                    canvas.drawText(
                                        "${event.offset % opus_manager.tuning_map.size}",
                                        x + (width / 2),
                                        y + resources.getDimension(R.dimen.text_size_offset),
                                        this.text_paint_offset
                                    )
                                }
                                is PercussionEvent -> {
                                    canvas.drawText(
                                        "X",
                                        x + (width / 2),
                                        y + resources.getDimension(R.dimen.text_size_offset),
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
                            this.draw_tree(canvas, tree, listOf(), offset, y_offset, beat_width, ctl_line_height) { event, position, canvas, x, y, width, height ->
                                val ctl_drawable = resources.getDrawable(R.drawable.ctl_leaf)
                                ctl_drawable.setState(this.get_line_control_leaf_state(type, beat_key, position))
                                ctl_drawable.setBounds(offset.toInt(), y_offset.toInt(), (offset + beat_width).toInt(), (y_offset + ctl_line_height).toInt())
                                ctl_drawable.draw(canvas)
                            }

                            y_offset += ctl_line_height
                        }
                    }

                    for ((type, controller) in channel.controllers.get_all()) {
                        if (!controller.visible) {
                            continue
                        }
                        val tree = controller.get_tree(i, listOf())
                        this.draw_tree(canvas, tree, listOf(), offset, y_offset, beat_width, ctl_line_height) { event, position, canvas, x, y, width, height ->
                            val ctl_drawable = resources.getDrawable(R.drawable.ctl_leaf)
                            ctl_drawable.setState(this.get_channel_control_leaf_state(type, j, i, position))
                            ctl_drawable.setBounds(offset.toInt(), y_offset.toInt(), (offset + beat_width).toInt(), (y_offset + ctl_line_height).toInt())
                            ctl_drawable.draw(canvas)
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
                    this.draw_tree(canvas, tree, listOf(), offset, y_offset, beat_width, ctl_line_height) { event, position, canvas, x, y, width, height ->
                        val ctl_drawable = resources.getDrawable(R.drawable.ctl_leaf)
                        ctl_drawable.setState(this.get_global_control_leaf_state(type, i, position))
                        ctl_drawable.setBounds(offset.toInt(), y_offset.toInt(), (offset + beat_width).toInt(), (y_offset + ctl_line_height).toInt())
                        ctl_drawable.draw(canvas)
                    }

                    y_offset += ctl_line_height
                }
                offset += beat_width
            }
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
            this.refreshDrawableState()
        }

        fun notify_column_changed(x: Int, state_only: Boolean = false) {
            //val column = this.get_column(x)
            //if (state_only) {
            //    column.notify_state_changed()
            //} else {
            //    column.rebuild()
            //}
            this.refreshDrawableState()
        }

        fun notify_row_change(y: Int, state_only: Boolean = false) {
            //for (x in 0 until this.childCount) {
            //    this.get_column(x).notify_item_changed(y, state_only)
            //}
            this.refreshDrawableState()
        }

        fun clear() {
            this.removeAllViews()
        }

    }

    private val _column_label_recycler = editor_table.column_label_container
    val column_container = ColumnsLayout(editor_table)
    private var _scroll_locked: Boolean = false
    //val column_recycler = ColumnRecycler(editor_table)

    private val _line_label_layout = editor_table.get_line_label_layout()
    val vertical_scroll_view = ScrollView(this.context)
    init {
        this.vertical_scroll_view.overScrollMode = OVER_SCROLL_NEVER
        this.vertical_scroll_view.isVerticalScrollBarEnabled = false
        this.vertical_scroll_view.addView(this.column_container)
        this.addView(this.vertical_scroll_view)

        this.vertical_scroll_view.layoutParams.height = MATCH_PARENT
        this.vertical_scroll_view.layoutParams.width = WRAP_CONTENT

        this.overScrollMode = OVER_SCROLL_NEVER
        //this.isHorizontalScrollBarEnabled = false
    }

    fun clear() {
        this.vertical_scroll_view.scrollX = 0
        this.vertical_scroll_view.scrollY = 0
        this.scrollX = 0
        this.scrollY = 0
        this.column_container.clear()

    }

    override fun onScrollChanged(x: Int, y: Int, old_x: Int, old_y: Int) {
        this._line_label_layout.scrollTo(x, y)
        if (!this.is_scroll_locked()) {
            this._column_label_recycler.lock_scroll()
            this._column_label_recycler.scrollTo(x, 0)
            this._column_label_recycler.unlock_scroll()
        }
        this.column_container.invalidate()
        super.onScrollChanged(x, y, old_x, old_y)
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

}