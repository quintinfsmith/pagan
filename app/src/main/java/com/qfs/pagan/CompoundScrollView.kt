package com.qfs.pagan

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.widget.HorizontalScrollView

@SuppressLint("ViewConstructor")
class CompoundScrollView(var editor_table: EditorTable): HorizontalScrollView(editor_table.context) {
    private val _column_label_recycler = editor_table.column_label_recycler
    private var _scroll_locked: Boolean = false
    val column_recycler = ColumnRecycler(editor_table)
    class TestView(var editor_table: EditorTable): View(editor_table.context) {
        val line_paint = Paint()
        init {
            this.line_paint.color = resources.getColor(R.color.table_lines)
            this.line_paint.strokeWidth = 3F
            this.setWillNotDraw(false)
        }

        override fun onDraw(canvas: Canvas) {
            val opus_manager = this.editor_table.get_opus_manager()
            val beat_count = this.editor_table.get_column_map_size()
            val beat_width = context.resources.getDimension(R.dimen.base_leaf_width)
            val line_height = context.resources.getDimension(R.dimen.line_height)
            val channel_gap = context.resources.getDimension(R.dimen.channel_gap_size)
            val controller_height = context.resources.getDimension(R.dimen.ctl_line_height)

            val column_width_map = this.editor_table.get_column_width_map()
            var running_position = 0F

            for (x in column_width_map.indices) {
                val column = column_width_map[x]
                val column_width = this.editor_table.get_column_width(x)
                var running_y = 0F
                var y = 0
                for (channel in opus_manager.get_all_channels()) {
                    if (!channel.visible) {
                        continue
                    }
                    for (line in channel.lines) {
                        var running_inner_position = running_position
                        val leaf_width = beat_width * column_width / column[y]
                        for (i in 0 until column[y]) {
                            running_inner_position += leaf_width
                            canvas.drawLine(
                                running_inner_position,
                                running_y,
                                running_inner_position,
                                running_y + line_height,
                                this.line_paint
                            )
                        }

                        running_y += line_height
                        y++
                        for ((type, controller) in line.controllers.get_all()) {
                            if (!controller.visible) {
                                continue
                            }
                            var running_inner_position = running_position
                            val leaf_width = beat_width * column_width / column[y]
                            for (i in 0 until column[y]) {
                                running_inner_position += leaf_width
                                canvas.drawLine(
                                    running_inner_position,
                                    running_y,
                                    running_inner_position,
                                    running_y + controller_height,
                                    this.line_paint
                                )
                            }
                            running_y += controller_height

                            y++
                        }
                    }

                    for ((type, controller) in channel.controllers.get_all()) {
                        if (!controller.visible) {
                            continue
                        }
                        var running_inner_position = running_position
                        val leaf_width = beat_width * column_width / column[y]
                        for (i in 0 until column[y]) {
                            running_inner_position += leaf_width
                            canvas.drawLine(
                                running_inner_position,
                                running_y,
                                running_inner_position,
                                running_y + controller_height,
                                this.line_paint
                            )
                        }
                        running_y += controller_height

                        y++
                    }

                    running_y += channel_gap
                }

                for ((type, controller) in opus_manager.controllers.get_all()) {
                    if (!controller.visible) {
                        continue
                    }
                    var running_inner_position = running_position
                    val leaf_width = beat_width * column_width / column[y]
                    for (i in 0 until column[y]) {
                        running_inner_position += leaf_width
                        canvas.drawLine(
                            running_inner_position,
                            running_y,
                            running_inner_position,
                            running_y + controller_height,
                            this.line_paint
                        )
                    }
                    running_y += controller_height

                    y++
                }

                //for (y in column.indices) {
                //    running_y += line_height
                //    canvas.drawLine(
                //        running_position,
                //        running_y,
                //        running_position + (column_width * beat_width),
                //        running_y,
                //        this.line_paint
                //    )
                //}
                running_position += column_width * beat_width
            }

            super.onDraw(canvas)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            this.set_size()
        }

        fun set_size() {
            val (width, height) = this.editor_table._calculate_table_size()
            this.layoutParams.width = width
            this.layoutParams.height = height
            this.minimumWidth = width
            this.minimumHeight = height
            println("RESIZED: $width, $height")
            this.invalidate()
        }
    }

    val main_grid = TestView(editor_table)

    private val _line_label_layout = editor_table.get_line_label_layout()
    private var _last_x_position: Float? = null
    init {
        this.addView(this.main_grid)
        this.main_grid.set_size()

        this.setBackgroundColor(Color.DKGRAY)

        this.overScrollMode = OVER_SCROLL_NEVER
        this.isVerticalScrollBarEnabled = false
        // this.isScrollContainer = true
        // this.canScrollVertically(1)
        // this.canScrollHorizontally(1)
    }


    //@SuppressLint("ClickableViewAccessibility")
    //override fun onTouchEvent(motion_event: MotionEvent?): Boolean {
    //    if (motion_event  == null) {
    //        // pass
    //    } else if (motion_event.action == MotionEvent.ACTION_UP) {
    //        this._last_x_position = null
    //    } else if (motion_event.action == MotionEvent.ACTION_MOVE) {
    //        if (this._last_x_position == null) {
    //            this._last_x_position = (motion_event.x - this.column_recycler.x) - this.column_recycler.scrollY.toFloat()
    //        }

    //        val rel_x = (motion_event.x - this.column_recycler.x) - this.column_recycler.scrollY
    //        val delta_x = this._last_x_position!! - rel_x

    //        this.column_recycler.scrollBy(delta_x.toInt(), 0)
    //        this._last_x_position = rel_x
    //    } else {
    //        // pass
    //    }

    //    return super.onTouchEvent(motion_event)
    //}

    fun set_grid_size() {
        this.main_grid.set_size()
    }

    override fun onScrollChanged(x: Int, y: Int, old_x: Int, old_y: Int) {
        this._line_label_layout.scrollTo(x, y)
        if (!this.is_scroll_locked()) {
            this._column_label_recycler.lock_scroll()
            this._column_label_recycler.scrollBy(x - old_x, 0)
            this._column_label_recycler.unlock_scroll()
        }
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