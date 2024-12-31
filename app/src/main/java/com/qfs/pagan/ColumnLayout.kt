package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.core.view.children
import kotlin.math.min
import com.qfs.pagan.OpusLayerInterface as OpusManager

class ColumnLayout(var editor_table: EditorTable, index: Int): LinearLayout(editor_table.context) {
    class ColumnDetachedException: Exception()
    private var _populated = false
    var column_width_factor = 1
    init {
        this.orientation = VERTICAL
        this.overScrollMode = View.OVER_SCROLL_NEVER
    }


    private fun _populate() {
        this._populated = true
        val opus_manager = this.get_opus_manager()
        for (y in 0 until opus_manager.get_row_count()) {
            this.addView(CellLayout(this, y), y)
        }
    }

    fun rebuild() {
        this.clear()
        this.column_width_factor = editor_table.get_column_width(this.get_beat())
        this._populate()
    }

    fun get_item_count(): Int {
        return this.childCount
    }

    fun insert_cells(y: Int, count: Int) {
        for (i in y until y + count) {
            this.addView(CellLayout(this, i), i)
        }
    }

    fun remove_cells(y: Int, count: Int) {
        this.removeViews(y, min(count, this.get_item_count() - y))
    }

    fun notify_state_changed() {
        this.notify_item_range_changed(0, this.get_item_count(), true)
    }

    fun notify_item_changed(y: Int, state_only: Boolean = false) {
        this.notify_item_range_changed(y, 1, state_only)
    }

    fun notify_item_range_changed(y: Int, count: Int, state_only: Boolean = false) {
        if (state_only) {
            for (i in 0 until count) {
                if (this.get_item_count() <= y + i) {
                    continue
                }
                (this.getChildAt(y + i) as CellLayout).invalidate_all()
            }
        } else {
            this.remove_cells(y, count)
            this.insert_cells(y, count)
        }
    }

    fun clear() {
        this.removeAllViews()
    }

    fun get_opus_manager(): OpusManager {
        return this.editor_table.get_opus_manager()
    }

    fun get_beat(): Int {
        // TODO: Probably slow
        val output = (this.parent as ViewGroup).children.indexOf(this)
        println("BEAT: $output")
        return output
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.height = MATCH_PARENT
        this.layoutParams.width = WRAP_CONTENT
        this.column_width_factor = editor_table.get_column_width(this.get_beat())
        this._populate()
    }
}
