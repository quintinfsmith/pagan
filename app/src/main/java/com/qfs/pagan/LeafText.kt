package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper

class LeafText(context: Context, attrs: AttributeSet? = null): androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    init {
        this._setup_colors()
    }
    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 5)
        var parent = this.parent ?: return drawableState
        while (parent !is LeafButton) {
            parent = parent.parent
        }
        return parent.drawableState
    }

    private fun _setup_colors() {
        val activity = this.get_activity()
        val palette = activity.view_model.palette!!

        val states = arrayOf(
            //------------------------------
            intArrayOf(R.attr.state_invalid),
            //----------------------------
            intArrayOf(
                -R.attr.state_invalid,
                -R.attr.state_linked,
                R.attr.state_active,
                R.attr.state_focused
            ),
            intArrayOf(
                -R.attr.state_invalid,
                -R.attr.state_linked,
                R.attr.state_active,
                -R.attr.state_focused
            ),
            // ------------------------
            intArrayOf(
                -R.attr.state_invalid,
                R.attr.state_linked,
                R.attr.state_active,
                R.attr.state_focused
            ),
            intArrayOf(
                -R.attr.state_invalid,
                R.attr.state_linked,
                R.attr.state_active,
                -R.attr.state_focused
            )
        )

        this.setTextColor(
            ColorStateList(
                states,
                intArrayOf(
                    palette.leaf_invalid_text,
                    palette.leaf_selected_text,
                    palette.leaf_text,
                    palette.link_selected_text,
                    palette.link_text
                )
            )
        )
    }

    fun get_activity(): MainActivity {
        return (this.context as ContextThemeWrapper).baseContext as MainActivity
    }
}

