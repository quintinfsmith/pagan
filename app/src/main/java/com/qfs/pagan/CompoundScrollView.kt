package com.qfs.pagan

import android.annotation.SuppressLint
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView

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
            val new_column = ColumnLayout(this.editor_table, x)
            this.addView(new_column, x)
        }

        fun add_columns(x: Int, count: Int) {
            for (i in x until count) {
                this.add_column(i)
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
        this.isHorizontalScrollBarEnabled = false
    }

    override fun onScrollChanged(x: Int, y: Int, old_x: Int, old_y: Int) {
        this._line_label_layout.scrollTo(x, y)
        if (!this.is_scroll_locked()) {
            this._column_label_recycler.lock_scroll()
            this._column_label_recycler.scrollTo(x, 0)
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