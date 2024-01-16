package com.qfs.pagan

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper

open class PaganSeekBar(context: Context, attrs: AttributeSet? = null): androidx.appcompat.widget.AppCompatSeekBar(context, attrs) {
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }

        val palette = context.view_model.palette!!
        this.thumb.setTint(palette.selection)
        (this.progressDrawable as LayerDrawable).findDrawableByLayerId(android.R.id.progress).setTint(palette.selection)
    }
}