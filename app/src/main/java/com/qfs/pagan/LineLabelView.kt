package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout

class LineLabelView(context: Context): LinearLayout(ContextThemeWrapper(context, R.style.line_label_outer)) {
    class InnerView(context: Context): androidx.appcompat.widget.AppCompatTextView(ContextThemeWrapper(context, R.style.line_label_inner)) {
        //override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        //    val drawableState = super.onCreateDrawableState(extraSpace + 1)
        //    return (this.parent as LineLabelView).build_drawable_state(drawableState)
        //}
    }
    var viewHolder: LineLabelViewHolder? = null
    private var textView = InnerView(context)
    /*
     * update_queued exists to handle the liminal state between being detached and being destroyed
     * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
     */
    var update_queued = false
    init {
        this.addView(this.textView)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.textView.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
        val line_height = resources.getDimension(R.dimen.line_height)
        this.layoutParams.height = line_height.toInt()
        this.layoutParams.width = WRAP_CONTENT
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        this.update_queued = true
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
        return drawableState
    }

    fun set_text(text: String) {
        this.textView.text = text
        this.contentDescription = text
    }
}