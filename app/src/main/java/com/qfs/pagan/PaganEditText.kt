package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper

open class PaganEditText(context: Context, attrs: AttributeSet? = null): androidx.appcompat.widget.AppCompatEditText(context, attrs) {
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }

        var state = 0
        for (item in this.drawableState) {
            state += when (item) {
                android.R.attr.state_focused -> 1
                else -> 0
            }
        }

        val palette = context.view_model.palette!!
        this.setTextColor(palette.foreground)

        this.background.setTint(when (state) {
            0 -> palette.foreground
            else -> palette.selection
        })
    }
}