package com.qfs.pagan

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat

class ButtonLabelledIcon(context: Context, attrs: AttributeSet? = null): LinearLayoutCompat(context, attrs) {
    class LabelView(context: Context, attrs: AttributeSet? = null): AppCompatTextView(context, attrs) {
        override fun drawableStateChanged() {
            super.drawableStateChanged()
            var context = this.context
            while (context !is MainActivity) {
                context = (context as ContextThemeWrapper).baseContext
            }

            this.setTextColor(context.view_model.color_map[ColorMap.Palette.ButtonText])
        }
    }

    class IconView(context: Context, attrs: AttributeSet? = null): AppCompatImageView(context, attrs) {
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

    val label: LabelView
    val icon: IconView
    val inner: LinearLayoutCompat

    init {
        this.orientation = HORIZONTAL
        this.background = AppCompatResources.getDrawable(context, R.drawable.button_icon)
        this.inner = LayoutInflater.from(context)
            .inflate(
                R.layout.button_labelled_icon,
                this.parent as ViewGroup?,
                false
            ) as LinearLayoutCompat
        this.addView(this.inner)
        this.icon = this.findViewById(R.id.icon)
        this.label = this.findViewById(R.id.label)
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