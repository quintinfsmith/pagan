package com.qfs.pagan

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

@SuppressLint("ViewConstructor")
class ColumnRecycler(editor_table: EditorTable): RecyclerView(editor_table.context) {
    private val _column_label_recycler = editor_table.column_label_recycler
    private var _last_y_position: Float? = null
    private var _scroll_locked: Boolean = false
    init {
        this.layoutManager = LeftAlignedLayoutManager(this, HORIZONTAL, false)
        this.itemAnimator = null
        this.overScrollMode = View.OVER_SCROLL_NEVER
        this.adapter = ColumnRecyclerAdapter(this, editor_table)
    }

    override fun onScrolled(dx: Int, dy: Int) {
        if (!this.is_scroll_locked()) {
            this._column_label_recycler.lock_scroll()
            this._column_label_recycler.scrollBy(dx, 0)
            this._column_label_recycler.unlock_scroll()
        }
        super.onScrolled(dx, dy)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(motion_event: MotionEvent?): Boolean {
        /* Allow Scrolling on the y axis when scrolling in the main_recycler */
        if (motion_event == null) {
            // pass
        } else if (motion_event.action == 1) {
            this._last_y_position = null
        } else if (motion_event.action != MotionEvent.ACTION_MOVE) {
            // pass
        } else {
            val scroll_view = this.parent as CompoundScrollView

            if (this._last_y_position == null) {
                this._last_y_position = (motion_event.y - scroll_view.y) - scroll_view.scrollY.toFloat()
            }

            val rel_y = (motion_event.y - scroll_view.y) - scroll_view.scrollY
            val delta_y = this._last_y_position!! - rel_y

            scroll_view.scrollBy(0, delta_y.toInt())
            this._last_y_position = rel_y
        }
        return super.onTouchEvent(motion_event)
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