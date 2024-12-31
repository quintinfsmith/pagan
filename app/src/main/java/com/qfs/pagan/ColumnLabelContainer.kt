package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

class ColumnLabelContainer(val editor_table: EditorTable): HorizontalScrollView(editor_table.context) {
    private var _scroll_locked = false
    class ColumnLabelContainerInner(val editor_table: EditorTable): LinearLayout(editor_table.context) {
        init {
            this.orientation = HORIZONTAL
        }

        fun clear() {
            this.removeAllViews()
        }

        fun add_column(i: Int) {
            this.addView(ColumnLabelView(this.editor_table), i)
            for (x in i until this.childCount) {
                this.notify_column_changed(x, false)
            }
        }

        fun remove_column(index: Int) {
            this.removeViewAt(index)
            for (x in index until this.childCount) {
                this.notify_column_changed(x, false)
            }
        }

        fun notify_column_changed(x: Int, state_only: Boolean = false) {
            val column_label = this.get_column_label(x)
            if (state_only) {
                column_label.refreshDrawableState()
            } else {
                column_label.rebuild()
            }
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

    private val inner_container = ColumnLabelContainerInner(editor_table)
    init {
        this.isHorizontalScrollBarEnabled = false
        this.overScrollMode = View.OVER_SCROLL_NEVER
        this.addView(this.inner_container)
    }

    fun clear() {
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
        super.onScrollChanged(x, y, old_x, old_y)
    }
}