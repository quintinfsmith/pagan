package com.qfs.pagan

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt
import com.qfs.pagan.InterfaceLayer as OpusManager

class ColumnLabelView(val view_holder: RecyclerView.ViewHolder): AppCompatTextView(ContextThemeWrapper(view_holder.itemView.context, R.style.column_label)) {
    companion object {
        private val STATE_FOCUSED = intArrayOf(R.attr.state_focused)
    }
    /*
     * update_queued exists to handle the liminal state between being detached and being destroyed
     * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
     */
    init {
        (this.view_holder.itemView as ViewGroup).removeAllViews()
        (this.view_holder.itemView as ViewGroup).addView(this)

        val beat = (this.view_holder as ColumnLabelViewHolder).bindingAdapterPosition
        val editor_table = (this.view_holder.bindingAdapter as ColumnLabelAdapter).get_editor_table()
        val new_width = editor_table!!.get_column_width(beat)

        this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
        // Adjust for rounding that occurs from dividing the width
        this.layoutParams.width = (new_width * resources.getDimension(R.dimen.base_leaf_width).roundToInt())

        this.setOnClickListener {
            val opus_manager = this.get_opus_manager()
            opus_manager.cursor_select_column(this.view_holder.bindingAdapterPosition)
        }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        return this.build_drawable_state(drawableState)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val beat = (this.view_holder as ColumnLabelViewHolder).bindingAdapterPosition
        this.contentDescription = resources.getString(R.string.desc_column_label, beat)
        this.text = beat.toString()
    }

    fun build_drawable_state(drawableState: IntArray?): IntArray? {
        if (this.parent == null) {
            return drawableState
        }
        val opus_manager = this.get_opus_manager()
        val beat = this.view_holder.bindingAdapterPosition
        when (opus_manager.cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Column -> {
                if (opus_manager.cursor.beat == beat) {
                    mergeDrawableStates(drawableState, ColumnLabelView.STATE_FOCUSED)
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = opus_manager.cursor.range!!
                if (first.beat <= beat && second.beat >= beat) {
                    mergeDrawableStates(drawableState, ColumnLabelView.STATE_FOCUSED)
                }
            }
            else -> {}
        }
        return drawableState
    }

    fun get_opus_manager(): OpusManager {
        return (this.view_holder.bindingAdapter as ColumnLabelAdapter).get_opus_manager()
    }
}
