package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.InterfaceLayer as OpusManager

class ColumnLabelView(val viewHolder: RecyclerView.ViewHolder): RelativeLayout(ContextThemeWrapper(viewHolder.itemView.context, R.style.column_label_outer)) {
    class InnerView(context: Context): androidx.appcompat.widget.AppCompatTextView(ContextThemeWrapper(context, R.style.line_label_inner)) {
        override fun onCreateDrawableState(extraSpace: Int): IntArray? {
            val drawableState = super.onCreateDrawableState(extraSpace + 1)
            return if (this.parent == null) {
                drawableState
            } else {
                (this.parent as ColumnLabelView).build_drawable_state(drawableState)
            }
        }
    }
    private val STATE_FOCUSED = intArrayOf(R.attr.state_focused)
    private var textView = InnerView(this.context)
    /*
     * update_queued exists to handle the liminal state between being detached and being destroyed
     * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
     */
    init {
        this.addView(this.textView)
        (this.viewHolder.itemView as ViewGroup).removeAllViews()
        (this.viewHolder.itemView as ViewGroup).addView(this)
        this.setOnClickListener {
            var opus_manager = this.get_opus_manager()
            opus_manager.cursor_select_column(this.viewHolder.bindingAdapterPosition)
        }
    }

    //override fun onDetachedFromWindow() {
    //    this.update_queued = true
    //    super.onDetachedFromWindow()
    //}

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.textView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        this.textView.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
        this.layoutParams.height = WRAP_CONTENT
    }

    fun set_text(text: String) {
        this.textView.text = text
        this.contentDescription = "Column $text"
    }
    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        return this.build_drawable_state(drawableState)
    }

    // Prevents the child labels from blocking the parent onTouchListener events
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return true
    }

    fun build_drawable_state(drawableState: IntArray?): IntArray? {
        val opus_manager = this.get_opus_manager()
        val beat = this.viewHolder.bindingAdapterPosition
        when (opus_manager.cursor.mode) {
            Cursor.CursorMode.Single,
            Cursor.CursorMode.Column -> {
                if (opus_manager.cursor.beat == beat) {
                    mergeDrawableStates(drawableState, this.STATE_FOCUSED)
                }
            }
            Cursor.CursorMode.Range -> {
                val (first, second) = opus_manager.cursor.range!!
                if (first.beat <= beat && second.beat >= beat) {
                    mergeDrawableStates(drawableState, this.STATE_FOCUSED)
                }
            }
            else -> {}
        }
        return drawableState
    }

    fun get_opus_manager(): OpusManager {
        return (this.viewHolder.bindingAdapter as ColumnLabelAdapter).get_opus_manager()
    }
}
