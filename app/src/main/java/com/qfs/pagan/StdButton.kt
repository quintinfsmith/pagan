package com.qfs.pagan

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper

class StdButton(context: Context, attrs: AttributeSet?): androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    // NOTE: this logic exists in drawableStateChanged() rather than init since palette isn't guaranteed
    // to exist on init()
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }

        val color_map = context.view_model.color_map
        val index = (this.background as StateListDrawable).findStateDrawableIndex(this.drawableState)
        val background = ((this.background as StateListDrawable).getStateDrawable(index) as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
        background?.setTint(color_map[Palette.Button])
        this.setTextColor(color_map[Palette.ButtonText])
    }
}