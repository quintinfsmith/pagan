package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.TextView

class PaganTextView(context: Context, attrs: AttributeSet?): androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }

        val palette = context.view_model.palette!!
        this.setTextColor(palette.foreground)
    }
}