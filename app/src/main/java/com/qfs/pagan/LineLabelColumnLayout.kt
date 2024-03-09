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
    }

    fun insert_label(y: Int? = null) {
        val adj_y = y ?: this._inner_wrapper.childCount
        val (channel, line_offset) = this.get_opus_manager().get_std_offset(adj_y)

        val label_view = LineLabelView(this.context, channel, line_offset)

        this._inner_wrapper.addView(label_view, adj_y)

        this._notify_item_range_changed(adj_y + 1, this._inner_wrapper.childCount - (adj_y + 1))
    }

    fun insert_labels(y: Int, count: Int) {
        val opus_manager = this.get_opus_manager()
        var (channel, line_offset) = opus_manager.get_std_offset(y)
        for (i in 0 until count) {
            val label_view = LineLabelView(this.context, channel, line_offset)
            this._inner_wrapper.addView(label_view, y + i)
            if (line_offset < opus_manager.channels[channel].size - 1) {
                line_offset += 1
            } else {
                channel += 1
                line_offset = 0
            }
        }

        this._notify_item_range_changed(y + count , this._inner_wrapper.childCount - y)

    }
    fun remove_label(y: Int) {
        this._inner_wrapper.removeViewAt(y)
        this._notify_item_range_changed(y, this._inner_wrapper.childCount - y)
    }

    fun remove_labels(y: Int, count: Int) {
        this._inner_wrapper.removeViews(y, count)
        this._notify_item_range_changed(y, this._inner_wrapper.childCount - y)
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

    // TODO: THis feels like it could be much tighter
    private fun _notify_item_range_changed(y: Int, count: Int) {
        if (y > this._inner_wrapper.childCount) {
            // Nothing to change
            return
        }

        val view_stack = mutableListOf<View>()
        val opus_manager = this.get_opus_manager()

        // calculate the new channel/line_offset by it's previous neighbour
        var (channel, line_offset) = if (y > 0) {
            val prev_label = this._inner_wrapper.getChildAt(y - 1) as LineLabelView
            val prev = prev_label.get_row()
            if (prev.second < opus_manager.channels[prev.first].size - 1) {
                Pair(prev.first, prev.second + 1)
            } else {
                Pair(prev.first + 1, 0)
            }
        } else {
            Pair(0,0)
        }

        for (i in 0 until count) {
            if (i + y < this._inner_wrapper.childCount) {
                val label = this._inner_wrapper.getChildAt(i + y) as LineLabelView
                label.channel = channel
                label.line_offset = line_offset

                if (channel >= opus_manager.channels.size) {
                    break
                } else if (line_offset + 1 >= opus_manager.channels[channel].size) {
                    line_offset = 0
                    channel += 1
                } else {
                    line_offset += 1
                }

                label.set_text()
                view_stack.add( label )
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