package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper

class PaganTextView(context: Context, attrs: AttributeSet? = null): androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }

        this.setTextColor(context.view_model.color_map[Palette.Foreground])
    }
}