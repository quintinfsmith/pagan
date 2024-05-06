package com.qfs.pagan

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity.CENTER
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat

class ButtonLabelledIcon(context: Context, attrs: AttributeSet? = null): LinearLayoutCompat(context, attrs) {
    class LabelView(context: Context): AppCompatTextView(context) {
        override fun drawableStateChanged() {
            super.drawableStateChanged()
            var context = this.context
            while (context !is MainActivity) {
                context = (context as ContextThemeWrapper).baseContext
            }

            this.setTextColor(context.view_model.color_map[ColorMap.Palette.ButtonText])
        }
    }

    class IconView(context: Context): AppCompatImageView(context) {
        // NOTE: this logic exists in drawableStateChanged() rather than init since palette isn't guaranteed
        // to exist on init()
        override fun drawableStateChanged() {
            super.drawableStateChanged()
            var context = this.context
            while (context !is MainActivity) {
                context = (context as ContextThemeWrapper).baseContext
            }

            val color_map = context.view_model.color_map
            this.setColorFilter(color_map[ColorMap.Palette.ButtonText])
        }
    }

    val label = LabelView(context)
    val icon = IconView(context)

    init {
        this.background = AppCompatResources.getDrawable(context, R.drawable.button_icon)
        this.addView(this.icon)
        this.addView(this.label)

        (this.icon.layoutParams as LinearLayoutCompat.LayoutParams).gravity = CENTER
        this.icon.layoutParams.width = WRAP_CONTENT
        this.icon.layoutParams.height = WRAP_CONTENT

        (this.label.layoutParams as LinearLayoutCompat.LayoutParams).gravity = CENTER
        this.label.layoutParams.height = WRAP_CONTENT
        this.label.layoutParams.width = 0
        this.label.textAlignment = TEXT_ALIGNMENT_CENTER
        (this.label.layoutParams as LinearLayoutCompat.LayoutParams).weight = 1f
    }

    fun set_text(text: String) {
        this.label.text = text
    }

    fun set_icon(icon_id: Int) {
        this.icon.setImageResource(icon_id)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }

        val color_map = context.view_model.color_map
        val index = (this.background as StateListDrawable).findStateDrawableIndex(this.drawableState)
        val background = ((this.background as StateListDrawable).getStateDrawable(index) as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
        background?.setTint(color_map[ColorMap.Palette.Button])
        this.alpha = if (this.isEnabled) {
            1f
        } else {
            .5f
        }
    }
}