package com.qfs.pagan

import android.annotation.SuppressLint
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

@SuppressLint("ViewConstructor")
class CompoundScrollView(var editor_table: EditorTable): HorizontalScrollView(editor_table.context) {
    class ColumnsLayout(var editor_table: EditorTable): LinearLayout(editor_table.context) {
        fun insert_row(y: Int) {
            for (i in 0 until this.childCount) {
                val column = this.get_column(i)
                column.insert_cells(y, 1)
            }
        }

        fun remove_rows(y: Int, count: Int = 1) {
            for (i in 0 until this.childCount) {
                val column = this.get_column(i)
                column.remove_cells(y, count)
            }
        }

        fun add_column(x: Int) {
            this.addView(ColumnLayout(this.editor_table), x)
        }

        fun add_columns(x: Int, count: Int) {
            for (i in x until count) {
                this.add_column(x)
            }
        }

        fun remove_column(x: Int) {
            this.removeViewAt(x)
        }

        fun notify_cell_changed(y: Int, x: Int, state_only: Boolean = false) {
            val column = this.get_column(x)
            column.notify_item_changed(y, state_only)
        }

        fun notify_column_changed(x: Int, state_only: Boolean = false) {
            val column = this.get_column(x)
            if (state_only) {
                column.notify_state_changed()
            } else {
                column.rebuild()
            }
        }

        fun notify_row_change(y: Int, state_only: Boolean = false) {
            for (x in 0 until this.childCount) {
                this.get_column(x).notify_item_changed(y, state_only)
            }
        }

        fun get_column(x: Int): ColumnLayout {
            return this.getChildAt(x) as ColumnLayout
        }
        fun clear() {
            this.removeAllViews()
        }
    }

    private val _column_label_recycler = editor_table.column_label_recycler
    val column_container = ColumnsLayout(editor_table)
    private var _scroll_locked: Boolean = false
    //val column_recycler = ColumnRecycler(editor_table)

    private val _line_label_layout = editor_table.get_line_label_layout()
    private var _last_x_position: Float? = null
    init {
        this.addView(this.column_container)

        this.overScrollMode = OVER_SCROLL_NEVER
        this.isVerticalScrollBarEnabled = false
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