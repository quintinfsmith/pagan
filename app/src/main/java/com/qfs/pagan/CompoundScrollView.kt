package com.qfs.pagan

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ScrollView

@SuppressLint("ViewConstructor")
class CompoundScrollView(editor_table: EditorTable): ScrollView(editor_table.context) {
    val column_recycler = ColumnRecycler(editor_table)
    private val _line_label_layout = editor_table.get_line_label_layout()
    private var _last_x_position: Float? = null

    init {
        this.addView(this.column_recycler)
        this.column_recycler.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        this.column_recycler.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

        this.isVerticalScrollBarEnabled = false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(motion_event: MotionEvent?): Boolean {
        if (motion_event  == null) {
            // pass
        } else if (motion_event.action == MotionEvent.ACTION_UP) {
            this._last_x_position = null
        } else if (motion_event.action == MotionEvent.ACTION_MOVE) {
            if (this._last_x_position == null) {
                this._last_x_position = (motion_event.x - this.column_recycler.x) - this.column_recycler.scrollY.toFloat()
            }

            val rel_x = (motion_event.x - this.column_recycler.x) - this.column_recycler.scrollY
            val delta_x = this._last_x_position!! - rel_x

            this.column_recycler.scrollBy(delta_x.toInt(), 0)
            this._last_x_position = rel_x
        } else {
            // pass
        }

        return super.onTouchEvent(motion_event)
    }


    override fun onScrollChanged(x: Int, y: Int, old_x: Int, old_y: Int) {
        this._line_label_layout.scrollTo(x, y)
        super.onScrollChanged(x, y, old_x, old_y)
    }

}