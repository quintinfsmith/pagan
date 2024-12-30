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

    private fun _get_compound_scrollview(): CompoundScrollView {
        return this._get_editor_table().get_scroll_view()
    }

    private fun _get_editor_table(): EditorTable {
        return this.parent!!.parent!! as EditorTable
    }

    override fun onScrolled(dx: Int, dy: Int) {
        val compound_scrollview = this._get_compound_scrollview()
        if (! this.is_scroll_locked()) {
            compound_scrollview.lock_scroll()
            compound_scrollview.scrollBy(dx, 0)
            compound_scrollview.unlock_scroll()
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