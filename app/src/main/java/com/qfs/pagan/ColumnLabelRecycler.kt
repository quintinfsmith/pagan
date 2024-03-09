package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ColumnLabelRecycler(context: Context, attrs: AttributeSet? = null): RecyclerView(context, attrs) {
    private var _scroll_locked = false
    init {
        this.layoutManager = LeftAlignedLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        this.itemAnimator = null
        this.overScrollMode = View.OVER_SCROLL_NEVER
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.width = MATCH_PARENT
        this.layoutParams.height = WRAP_CONTENT
    }

    private fun _get_column_recycler(): ColumnRecycler {
        return this._get_editor_table().get_column_recycler()
    }

    private fun _get_editor_table(): EditorTable {
        return this.parent!!.parent!! as EditorTable
    }

    override fun onScrolled(dx: Int, dy: Int) {
        val column_recycler = this._get_column_recycler()
        if (! this.is_scroll_locked()) {
            column_recycler.lock_scroll()
            column_recycler.scrollBy(dx, 0)
            column_recycler.unlock_scroll()
        }
        super.onScrolled(dx, dy)
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
}