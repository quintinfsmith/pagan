package com.qfs.pagan

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout

class CornerView(context: Context, attrs: AttributeSet? = null): LinearLayout(ContextThemeWrapper(context, R.style.corner_view), attrs) {
    init {
        val inner_view = object:androidx.appcompat.widget.AppCompatImageView(context) {
            override fun drawableStateChanged() {
                super.drawableStateChanged()
                var context = this.context
                while (context !is MainActivity) {
                    context = (context as ContextThemeWrapper).baseContext
                }
                val palette = context.view_model.palette!!
                this.setColorFilter(palette.foreground)
            }
        }

        this.addView(inner_view)
        inner_view.layoutParams.width = MATCH_PARENT
        inner_view.layoutParams.height = MATCH_PARENT
        inner_view.setImageResource(R.drawable.baseline_shortcut_24)
        inner_view.contentDescription = resources.getString(R.string.label_shortcut)

    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        val activity = (this.context as ContextThemeWrapper).baseContext as MainActivity
        val palette = activity.view_model.palette!!
        val background = (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
        background.setTint(palette.background)
        val table_lines = (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_lines)
        table_lines.setTint(palette.lines)
    }
}