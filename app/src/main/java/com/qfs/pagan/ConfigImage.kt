package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import com.qfs.pagan.Activity.ActivityEditor

class ConfigImage(context: Context, attrs: AttributeSet?): androidx.appcompat.widget.AppCompatImageView(context, attrs) {
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var context = this.context
        while (context !is ActivityEditor) {
            context = (context as ContextThemeWrapper).baseContext
        }
    }
}