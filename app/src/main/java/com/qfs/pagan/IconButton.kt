package com.qfs.pagan

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import androidx.appcompat.content.res.AppCompatResources

class IconButton(context: Context, attrs: AttributeSet?): androidx.appcompat.widget.AppCompatImageView(context, attrs) {
    init {
        this.background = AppCompatResources.getDrawable(context, R.drawable.button)
    }
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }

        val palette = context.view_model.palette!!
        val background = (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
        val stroke = (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_stroke)
        background.setTint(palette.button)
        stroke.setTint(palette.button_stroke)
        this.setColorFilter(palette.button_text)
    }
}