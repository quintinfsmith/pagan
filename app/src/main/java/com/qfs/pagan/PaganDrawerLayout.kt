package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.LinearLayout
import com.qfs.pagan.ColorMap.Palette

class PaganDrawerLayout(context: Context, attrs: AttributeSet?): LinearLayout(context, attrs) {
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }

        this.setBackgroundColor(context.view_model.color_map[Palette.Background])
    }
}