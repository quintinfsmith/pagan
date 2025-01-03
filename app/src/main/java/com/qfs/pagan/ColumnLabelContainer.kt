package com.qfs.pagan

import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import kotlin.math.roundToInt

class ColumnLabelContainer(val editor_table: EditorTable): HorizontalScrollView(editor_table.context) {
    private var _scroll_locked = false
    class ColumnLabelContainerInner(val editor_table: EditorTable): LinearLayout(editor_table.context) {
        val paint = Paint()
        val text_paint = Paint()
        init {
            this.orientation = HORIZONTAL
            this.paint.color = resources.getColor(R.color.table_lines)
            this.paint.strokeWidth = 3F
            this.text_paint.textSize = resources.getDimension(R.dimen.text_size_offset)
            this.text_paint.color = resources.getColor(R.color.table_lines)
            this.text_paint.strokeWidth = 3F
            this.setWillNotDraw(false)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val base_width = resources.getDimension(R.dimen.base_leaf_width)
            val first_x = this.editor_table.get_first_visible_column_index()
            val last_x = this.editor_table.get_last_visible_column_index()
            var offset = (this.editor_table.get_column_rect(first_x)?.x ?: 0).toFloat()
            val initial_offset = offset

            for (i in first_x .. last_x) {
                canvas.drawText("$i", offset, (canvas.height / 2).toFloat(), this.text_paint)
                offset += (this.editor_table.get_column_width(i) * base_width).roundToInt()
                canvas.drawLine(
                    offset,
                    0F,
                    offset,
                    canvas.height.toFloat(),
                    this.paint
                )

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
        }

        fun get_column_label(index: Int): ColumnLabelView {
            return (this as ViewGroup).getChildAt(index) as ColumnLabelView
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

    fun get_column_label(index: Int): ColumnLabelView {
        return this.inner_container.get_column_label(index)
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

    private fun _get_compound_scrollview(): CompoundScrollView {
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