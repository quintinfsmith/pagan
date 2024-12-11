package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper

class LeafText(context: Context, attrs: AttributeSet? = null): androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 5)
        var parent = this.parent ?: return drawableState
        while (parent !is LeafButton) {
            if (parent.parent == null) {
                return drawableState
            }
            parent = parent.parent
        }
        return parent.drawableState
    }

    fun get_activity(): MainActivity {
        var working_context = this.context
        while (working_context is ContextThemeWrapper) {
            working_context = working_context.baseContext
        }
        return working_context as MainActivity
    }
}

