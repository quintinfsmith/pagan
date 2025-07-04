package com.qfs.pagan

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import androidx.core.view.children
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.LineLabel.LineLabelView

enum class CompatibleFileType {
    Midi1,
    Pagan
}

class LineLabelColumnLayout(editor_table: EditorTable): ScrollView(editor_table.context) {
    // BackLink so I can get the x offset from a view in the view holder
    var dragging_position: Pair<Int, Int>? = null
    private var _inner_wrapper = LinearLayout(editor_table.context)

    init {
        this._inner_wrapper.orientation = LinearLayout.VERTICAL

        // Add padding layer so we can scroll the bottom of the table to the middle of the screen
        val padding_layer = LinearLayout(editor_table.context)
        padding_layer.orientation = LinearLayout.VERTICAL
        padding_layer.addView(this._inner_wrapper)

        val padder = Space(this.context)
        padding_layer.addView(padder)
        this.addView(padding_layer)

        val activity = editor_table.get_activity()
        padder.layoutParams.height = activity.get_bottom_padding()

        this._inner_wrapper.layoutParams.width = LayoutParams.WRAP_CONTENT
        this._inner_wrapper.layoutParams.height = LayoutParams.WRAP_CONTENT
        this.isVerticalScrollBarEnabled = false
        this.isHorizontalScrollBarEnabled = false
        this.overScrollMode = OVER_SCROLL_NEVER
    }


    fun insert_label(y: Int? = null) {
        val adj_y = y ?: this.get_count()
        val label_view = LineLabelView(this.context, adj_y)
        this._inner_wrapper.addView(label_view, adj_y)

        this._notify_item_range_changed(adj_y, this.get_count() - adj_y)
    }

    fun insert_labels(y: Int, count: Int) {
        for (i in 0 until count) {
            val label_view = LineLabelView(this.context, y + i)
            this._inner_wrapper.addView(label_view, y + i)
        }

        this._notify_item_range_changed(y , this.get_count() - y)

    }

    fun remove_labels(y: Int, count: Int) {
        val original_child_count = this.get_count()
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

    fun get_activity(): ActivityEditor {
        return this.context as ActivityEditor
    }

    fun get_opus_manager(): OpusLayerInterface {
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
        if (y >= this.get_count()) {
            // Nothing to change
            return
        }

        val view_stack = mutableListOf<View>()

        for (i in 0 until count) {
            if (i + y < this.get_count()) {
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
            current_view.invalidate()
            current_view.refreshDrawableState()
        }
    }
}