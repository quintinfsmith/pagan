package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.ColorMap.Palette

class LeafText(context: Context, attrs: AttributeSet? = null): androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    init {
        this._setup_colors()
    }
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

    private fun _setup_colors() {
        val activity = this.get_activity()
        val color_map = activity.view_model.color_map

        val states = arrayOf(
            //------------------------------
            intArrayOf(
                R.attr.state_invalid,
                R.attr.state_focused
            ),
            intArrayOf(
                R.attr.state_invalid,
                -R.attr.state_focused
            ),
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
                    color_map[Palette.LeafInvalidSelectedText],
                    color_map[Palette.LeafInvalidText],
                    color_map[Palette.LeafSelectedText],
                    color_map[Palette.LeafText],
                    color_map[Palette.LinkSelectedText],
                    color_map[Palette.LinkText]
                )
            )
        )
    }

    fun get_activity(): MainActivity {
        var working_context = this.context
        while (working_context is ContextThemeWrapper) {
            working_context = working_context.baseContext
        }
        return working_context as MainActivity
    }
}

