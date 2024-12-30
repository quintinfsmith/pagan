package com.qfs.pagan

import android.annotation.SuppressLint
import android.graphics.Color
import android.widget.HorizontalScrollView

@SuppressLint("ViewConstructor")
class CompoundScrollView(var editor_table: EditorTable): HorizontalScrollView(editor_table.context) {
    private val _column_label_recycler = editor_table.column_label_recycler
    private var _scroll_locked: Boolean = false
    val column_recycler = ColumnRecycler(editor_table)

    private val _line_label_layout = editor_table.get_line_label_layout()
    private var _last_x_position: Float? = null
    init {
        this.addView(this.column_recycler)
        this.setBackgroundColor(Color.DKGRAY)

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