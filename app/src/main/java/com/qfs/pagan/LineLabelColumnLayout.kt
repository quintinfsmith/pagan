package com.qfs.pagan

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.view.children
import com.qfs.pagan.OpusLayerInterface as OpusManager

class LineLabelColumnLayout(editor_table: EditorTable): ScrollView(editor_table.context) {
    // BackLink so I can get the x offset from a view in the view holder
    var dragging_position: Pair<Int, Int>? = null
    private var _inner_wrapper = LinearLayout(editor_table.context)

    init {
        this._inner_wrapper.orientation = LinearLayout.VERTICAL
        this.addView(this._inner_wrapper)
        this._inner_wrapper.layoutParams.width = WRAP_CONTENT
        this._inner_wrapper.layoutParams.height = WRAP_CONTENT
        this.isVerticalScrollBarEnabled = false
        this.isHorizontalScrollBarEnabled = false
        this.overScrollMode = OVER_SCROLL_NEVER
    }

    fun insert_label(y: Int? = null) {
        val adj_y = y ?: this._inner_wrapper.childCount
        val label_view = LineLabelView(this.context, adj_y)
        this._inner_wrapper.addView(label_view, adj_y)

        this._notify_item_range_changed(adj_y, this._inner_wrapper.childCount - adj_y)
    }

    fun insert_labels(y: Int, count: Int) {
        for (i in 0 until count) {
            val label_view = LineLabelView(this.context, y + i)
            this._inner_wrapper.addView(label_view, y + i)
        }

        this._notify_item_range_changed(y , this._inner_wrapper.childCount - y)

    }

    fun remove_label(y: Int) {
        this._inner_wrapper.removeViewAt(y)
        this._notify_item_range_changed(y, this._inner_wrapper.childCount - y)
    }

    fun remove_labels(y: Int, count: Int) {
        val original_child_count = this._inner_wrapper.childCount
        val adj_count = if (y + count < original_child_count) {
            count
        } else {
            original_child_count - y
        }

        this._inner_wrapper.removeViews(y, adj_count)
        this._notify_item_range_changed(y, original_child_count - adj_count - y)
    }

    fun set_dragging_line(channel: Int, line_offset:Int) {
        this.dragging_position = Pair(channel, line_offset)
    }
    fun is_dragging(): Boolean {

        return this.dragging_position != null
    }
    fun stop_dragging() {
        this.dragging_position = null
    }

    fun get_activity(): MainActivity {
        return this.context as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }

    fun clear() {
        this._inner_wrapper.removeAllViews()
    }

    // Prevents this from intercepting linelabel touch events (disables manual scrolling)
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return false
    }

    fun get_count(): Int {
        return this._inner_wrapper.childCount
    }

    fun notify_item_changed(y: Int) {
        this._notify_item_range_changed(y, 1)
    }

    private fun _notify_item_range_changed(y: Int, count: Int) {
        if (y >= this._inner_wrapper.childCount) {
            // Nothing to change
            return
        }

        val view_stack = mutableListOf<View>()

        for (i in 0 until count) {
            if (i + y < this._inner_wrapper.childCount) {
                val label = this._inner_wrapper.getChildAt(i + y) as LineLabelView
                try {
                    label.reset_row(i + y)
                    view_stack.add(label)
                } catch (_: NullPointerException) {

                }
            } else {
                break
            }
        }

        while (view_stack.isNotEmpty()) {
            val current_view = view_stack.removeAt(0)
            if (current_view is ViewGroup) {
                for (child in current_view.children) {
                    view_stack.add(child)
                }
            }
            current_view.postInvalidate()
            current_view.refreshDrawableState()
        }
    }
}
