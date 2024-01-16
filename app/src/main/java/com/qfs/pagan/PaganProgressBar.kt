package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.ProgressBar

open class PaganProgressBar(context: Context, attrs: AttributeSet? = null): ProgressBar(ContextThemeWrapper(context, R.style.progress_bar), attrs, android.R.attr.progressBarStyle) {
    init {
        super.drawableStateChanged()
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }

        val palette = context.view_model.palette!!
        this.indeterminateDrawable.setTint(palette.leaf)
    }
}