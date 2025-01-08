package com.qfs.pagan

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class ColumnRecycler(var editor_table: EditorTable): RecyclerView(editor_table.context) {
    private var _last_y_position: Float? = null

    init {
        this.layoutManager = LeftAlignedLayoutManager(this, HORIZONTAL, false)
        this.itemAnimator = null
        this.adapter = ColumnRecyclerAdapter(this, editor_table)
        this.overScrollMode = View.OVER_SCROLL_NEVER
    }

    fun get_first_column_test() {
        val x = this.computeHorizontalScrollOffset()
        val min_leaf_width = resources.getDimension(R.dimen.base_leaf_width).roundToInt()
        val reduced_x = x / min_leaf_width
        val column = this.editor_table.get_column_from_leaf(reduced_x)
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
            val scroll_view = this.parent as TableUI

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

}