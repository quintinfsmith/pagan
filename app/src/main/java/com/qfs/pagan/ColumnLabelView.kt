package com.qfs.pagan

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt
import com.qfs.pagan.OpusLayerInterface as OpusManager

class ColumnLabelView(private val _view_holder: RecyclerView.ViewHolder): AppCompatTextView(ContextThemeWrapper(_view_holder.itemView.context, R.style.column_label)) {
    /*
     * update_queued exists to handle the liminal state between being detached and being destroyed
     * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
     */
    init {
        (this._view_holder.itemView as ViewGroup).removeAllViews()
        (this._view_holder.itemView as ViewGroup).addView(this)

        val beat = (this._view_holder as ColumnLabelViewHolder).bindingAdapterPosition
        val editor_table = (this._view_holder.bindingAdapter as ColumnLabelAdapter).get_editor_table()
        val new_width = editor_table!!.get_column_width(beat)

        this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
        // Adjust for rounding that occurs from dividing the width
        this.layoutParams.width = (new_width * resources.getDimension(R.dimen.base_leaf_width).roundToInt())

        this.setOnClickListener {
            val opus_manager = this.get_opus_manager()
            opus_manager.cursor_select_column(this._view_holder.bindingAdapterPosition)
        }

        // Kludge: Needs to be called here or else will be unreliable
        this.refreshDrawableState()
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        return this._build_drawable_state(drawableState)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val beat = (this._view_holder as ColumnLabelViewHolder).bindingAdapterPosition
        this.contentDescription = resources.getString(R.string.desc_column_label, beat)
        this.text = beat.toString()
    }

    private fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        val beat = try {
            this._view_holder.bindingAdapterPosition
        } catch (e: NullPointerException) {
            return drawableState
        }
        val new_state = mutableSetOf<Int>()

        val opus_manager = this.get_opus_manager()
        if (opus_manager.is_beat_selected(beat)) {
            new_state.add(R.attr.state_focused)
        }

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }

    fun get_opus_manager(): OpusManager {
        return (this._view_holder.bindingAdapter as ColumnLabelAdapter).get_opus_manager()
    }
}
