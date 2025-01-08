package com.qfs.pagan

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

class ColumnLabelContainer(val editor_table: EditorTable): HorizontalScrollView(editor_table.context) {
    private var _scroll_locked = false
    class ColumnLabelContainerInner(val editor_table: EditorTable): LinearLayout(editor_table.context) {
        val paint = Paint()
        val text_paint = Paint()
        var touch_position_x = 0F
        var touch_position_y = 0F
        init {
            this.orientation = HORIZONTAL
            this.paint.color = resources.getColor(R.color.table_lines)
            this.paint.strokeWidth = 3F
            this.text_paint.textSize = resources.getDimension(R.dimen.text_size_octave)
            this.text_paint.isFakeBoldText = true
            this.text_paint.isAntiAlias = true
            this.text_paint.strokeWidth = 3F
            this.setWillNotDraw(false)

            this.setOnTouchListener { view: View?, touchEvent: MotionEvent? ->
                if (touchEvent != null) {
                    this.touch_position_y = touchEvent.y
                    this.touch_position_x = touchEvent.x
                }
                false
            }

            this.setOnClickListener {
                val opus_manager = this.editor_table.get_opus_manager()
                val min_leaf_width = resources.getDimension(R.dimen.base_leaf_width).toInt()
                val reduced_x = this.touch_position_x / min_leaf_width

                val beat = this.editor_table.get_column_from_leaf(reduced_x.toInt())
                opus_manager.cursor_select_column(beat)
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

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val base_width = resources.getDimension(R.dimen.base_leaf_width)
            val first_x = this.editor_table.get_first_visible_column_index()
            val last_x = this.editor_table.get_last_visible_column_index()
            var offset = (this.editor_table.get_column_rect(first_x)?.x ?: 0).toFloat()
            val initial_offset = offset


            val color_list = resources.getColorStateList(R.color.column_label_text)!!
            for (i in first_x .. last_x) {
                val state = this.get_column_label_state(i)
                this.text_paint.color = color_list.getColorForState(state, Color.MAGENTA)

                val column_width = this.editor_table.get_column_width(i) * base_width
                val drawable = resources.getDrawable(R.drawable.editor_label_column)
                drawable.setState(state)
                drawable.setBounds(offset.toInt(), 0, (offset + column_width).toInt(), canvas.height)
                drawable.draw(canvas)

                val column_text = "$i"
                val bounds = Rect()
                this.text_paint.getTextBounds(column_text, 0, column_text.length, bounds)
                canvas.drawText(
                    "$i",
                    offset - bounds.left + ((column_width - bounds.width()) / 2),
                    0 - bounds.top + ((this.height  - bounds.height()) / 2).toFloat(),
                    this.text_paint
                )

                offset += column_width
            }
        }

        fun clear() {
            //this.removeAllViews()
        }

        fun add_column(i: Int) {
            //this.addView(ColumnLabelView(this.editor_table), i)
            //for (x in i until this.childCount) {
            //    this.notify_column_changed(x, false)
            //}
        }

        fun remove_column(index: Int) {
            //this.removeViewAt(index)
            //for (x in index until this.childCount) {
            //    this.notify_column_changed(x, false)
            //}
        }

        fun notify_column_changed(x: Int, state_only: Boolean = false) {
            //val column_label = this.get_column_label(x)
            //if (state_only) {
            //    column_label.refreshDrawableState()
            //} else {
            //    column_label.rebuild()
            //}
            this.invalidate()
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            this.layoutParams.width = WRAP_CONTENT
            this.layoutParams.height = WRAP_CONTENT
        }
    }

    internal val inner_container = ColumnLabelContainerInner(editor_table)
    init {
        this.isHorizontalScrollBarEnabled = false
        this.overScrollMode = View.OVER_SCROLL_NEVER
        this.addView(this.inner_container)
    }

    fun clear() {
        this.scrollX = 0
        this.scrollY = 0
        this.inner_container.clear()
    }

    fun add_column(i: Int) {
        this.inner_container.add_column(i)
    }

    fun remove_column(index: Int) {
        this.inner_container.remove_column(index)
    }

    fun notify_column_changed(x: Int, state_only: Boolean = false) {
        this.inner_container.notify_column_changed(x, state_only)
    }

    fun lock_scroll() {
        this._scroll_locked = true
    }

    fun unlock_scroll() {
        this._scroll_locked = false
    }

    fun is_scroll_locked(): Boolean {
        return this._scroll_locked
    }

    private fun _get_compound_scrollview(): TableUI {
        return this.editor_table.get_scroll_view()
    }

    override fun onScrollChanged(x: Int, y: Int, old_x: Int, old_y: Int) {
        val compound_scrollview = this._get_compound_scrollview()
        if (!this.is_scroll_locked()) {
            compound_scrollview.lock_scroll()
            compound_scrollview.scrollTo(x, 0)
            compound_scrollview.unlock_scroll()
        }
        this.inner_container.invalidate()
        super.onScrollChanged(x, y, old_x, old_y)
    }
}