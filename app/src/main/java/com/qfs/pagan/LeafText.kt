package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet

class LeafText(context: Context, attrs: AttributeSet): androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 5)
        var parent = this.parent ?: return drawableState
        while (parent !is LeafButton) {
            parent = parent.parent
        }
        return parent.drawableState
    }
}

