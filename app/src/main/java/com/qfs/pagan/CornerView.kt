package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import android.widget.LinearLayout

class CornerView(context: Context, attrs: AttributeSet? = null): LinearLayout(ContextThemeWrapper(context, R.style.corner_view), attrs) {
    init {
        val inner_view = ImageView(ContextThemeWrapper(context, R.style.button), null)

        this.addView(inner_view)
        inner_view.layoutParams.width = MATCH_PARENT
        inner_view.layoutParams.height = MATCH_PARENT
        inner_view.setImageResource(R.drawable.baseline_shortcut_24)
        inner_view.contentDescription = resources.getString(R.string.label_shortcut)

    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        //val activity = (this.context as ContextThemeWrapper).baseContext as MainActivity
        //val color_map = activity.view_model.color_map
        //val background = (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
        //background.setTint(color_map[Palette.Background])
        //val table_lines = (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_lines)
        //table_lines.setTint(color_map[Palette.Lines])
    }
}