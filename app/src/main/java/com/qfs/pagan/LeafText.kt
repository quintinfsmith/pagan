package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper

class LeafText(context: Context, attrs: AttributeSet? = null): androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 5)
        var parent = this.parent ?: return drawableState
        while (parent !is LeafButton) {
            parent = parent.parent
        }
        return parent.drawableState
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var state = 0

        for (item in this.drawableState) {
            state += when (item) {
                R.attr.state_invalid -> 1
                R.attr.state_active -> 2
                R.attr.state_focused -> 4
                R.attr.state_linked -> 8
                R.attr.state_channel_even -> 16
                else -> 0
            }
        }

        val activity = this.get_activity()
        this.setTextColor(when (state) {
            0 -> activity.palette.channel_odd
            2,18 -> activity.palette.leaf_text
            4,20 -> 0 // No text
            6,22 -> activity.palette.leaf_selected_text
            8,24 -> 0 // No Text
            10,26 -> activity.palette.link_text
            12, 28 -> 0 // No Text
            14, 30 -> activity.palette.link_selected_text
            16 ->  0 // No Text
            else -> activity.palette.leaf_invalid
        })
    }

    fun get_activity(): MainActivity {
        return (this.context as ContextThemeWrapper).baseContext as MainActivity
    }
}

