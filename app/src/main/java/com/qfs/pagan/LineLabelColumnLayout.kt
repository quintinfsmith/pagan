package com.qfs.pagan

import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import com.qfs.pagan.InterfaceLayer as OpusManager

class LineLabelColumnLayout(editor_table: EditorTable): ScrollView(editor_table.context) {
    // BackLink so I can get the x offset from a view in the view holder
    var dragging_position: Pair<Int, Int>? = null
    var inner_wrapper = LinearLayout(editor_table.context)
    init {
        this.addView(this.inner_wrapper)
        this.inner_wrapper.layoutParams.width = WRAP_CONTENT
        this.inner_wrapper.layoutParams.height = WRAP_CONTENT
        this.populate()
    }

    fun populate() {
        for (y in 0 until this.get_opus_manager().get_visible_line_count()) {
            this.insert_label(y)
        }
    }

    fun insert_label(y: Int) {
        val label_view = LineLabelView(this.context)
        this.inner_wrapper.addView(label_view)

        val (channel, line_offset) = this.get_opus_manager().get_std_offset(y)
        val label = this.get_label_text(channel, line_offset)
        label_view.set_text(label)
    }

    private fun get_label_text(channel: Int, line_offset: Int): String {
        val opus_manager = this.get_opus_manager()
        return if (!opus_manager.is_percussion(channel)) {
            "$channel::$line_offset"
        } else {
            val instrument = opus_manager.get_percussion_instrument(line_offset)
            "!$instrument"
        }
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
        this.inner_wrapper.removeAllViews()
    }

    // Prevents this from intercepting linelabel touch events (disables manual scrolling)
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return false
    }
}