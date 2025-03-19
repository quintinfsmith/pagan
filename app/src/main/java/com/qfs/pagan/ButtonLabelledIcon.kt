package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat

class ButtonLabelledIcon(context: Context, attrs: AttributeSet? = null): LinearLayoutCompat(ContextThemeWrapper(context, R.style.icon_button), attrs) {
    val label: TextView
    val icon: ImageView
    val inner: LinearLayoutCompat

    init {
        this.orientation = HORIZONTAL
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

}