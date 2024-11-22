package com.qfs.pagan

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.Space
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.children

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

    val label = LabelView(ContextThemeWrapper((context as ContextThemeWrapper).baseContext, R.style.tempo_widget_button_text))
    val icon = IconView((context as ContextThemeWrapper).baseContext)

    init {
        this.orientation = HORIZONTAL
        this.background = AppCompatResources.getDrawable(context, R.drawable.button_icon)

        val first_layout = LinearLayoutCompat((context as ContextThemeWrapper).baseContext)
        val second_layout = LinearLayoutCompat((context as ContextThemeWrapper).baseContext)

        this.addView(first_layout)
        this.addView(second_layout)

        first_layout.addView(Space(context))
        first_layout.addView(this.icon)
        first_layout.addView(Space(context))
        second_layout.addView(Space(context))
        second_layout.addView(this.label)
        second_layout.addView(Space(context))

        first_layout.orientation = VERTICAL
        first_layout.layoutParams.height = MATCH_PARENT
        first_layout.children.first().layoutParams.height = 0
        (first_layout.children.first().layoutParams as LinearLayout.LayoutParams).weight = 1f
        first_layout.children.last().layoutParams.height = 0
        (first_layout.children.last().layoutParams as LinearLayout.LayoutParams).weight = 1f
        second_layout.orientation = VERTICAL
        second_layout.layoutParams.height = MATCH_PARENT
        second_layout.children.first().layoutParams.height = 0
        (second_layout.children.first().layoutParams as LinearLayout.LayoutParams).weight = 1f
        second_layout.children.last().layoutParams.height = 0
        (second_layout.children.last().layoutParams as LinearLayout.LayoutParams).weight = 1f


        this.icon.layoutParams.width = WRAP_CONTENT
        this.icon.layoutParams.height = WRAP_CONTENT

        this.label.layoutParams.height = WRAP_CONTENT
        this.label.layoutParams.width = WRAP_CONTENT
        this.label.textAlignment = TEXT_ALIGNMENT_CENTER
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