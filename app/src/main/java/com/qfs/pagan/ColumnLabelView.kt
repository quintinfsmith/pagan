package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.RelativeLayout

class ColumnLabelView(context: Context): RelativeLayout(ContextThemeWrapper(context, R.style.column_label_outer)) {
    class InnerView(context: Context): androidx.appcompat.widget.AppCompatTextView(ContextThemeWrapper(context, R.style.line_label_inner)) {
        //override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        //    val drawableState = super.onCreateDrawableState(extraSpace + 1)
        //    return (this.parent as LineLabelView).build_drawable_state(drawableState)
        //}
    }
    var viewHolder: ColumnLabelViewHolder? = null
    private var textView = InnerView(context)
    /*
     * update_queued exists to handle the liminal state between being detached and being destroyed
     * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
     */
    var update_queued = false
    init {
        this.addView(this.textView)
    }

    //override fun onDetachedFromWindow() {
    //    this.update_queued = true
    //    super.onDetachedFromWindow()
    //}

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.textView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
    }

    fun set_text(text: String) {
        this.textView.text = text
        this.contentDescription = "Column $text"
    }

    //fun set_focused(value: Boolean) {
    //    this.textView.set_focused(value)
    //    this.refreshDrawableState()
    //}

}
