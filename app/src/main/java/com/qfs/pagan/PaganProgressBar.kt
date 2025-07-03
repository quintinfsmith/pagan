package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.ProgressBar
import com.qfs.pagan.Activity.ActivityEditor

open class PaganProgressBar(context: Context, attrs: AttributeSet? = null): ProgressBar(ContextThemeWrapper(context, R.style.progress_bar), attrs, android.R.attr.progressBarStyle) {
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var context = this.context
        while (context !is ActivityEditor) {
            context = (context as ContextThemeWrapper).baseContext
        }
    }
}