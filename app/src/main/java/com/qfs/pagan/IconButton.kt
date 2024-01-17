package com.qfs.pagan

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import androidx.appcompat.content.res.AppCompatResources

class IconButton(context: Context, attrs: AttributeSet?): androidx.appcompat.widget.AppCompatImageView(context, attrs) {
    init {
        this.background = AppCompatResources.getDrawable(context, R.drawable.button)
    }
    // NOTE: this logic exists in drawableStateChanged() rather than init since palette isn't guaranteed
    // to exist on init()
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }

        val palette = context.view_model.palette!!
        val index = (this.background as StateListDrawable).findStateDrawableIndex(this.drawableState)
        val background = ((this.background as StateListDrawable).getStateDrawable(index) as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
        background?.setTint(palette.button)
        this.setColorFilter(palette.button_text)
    }
}