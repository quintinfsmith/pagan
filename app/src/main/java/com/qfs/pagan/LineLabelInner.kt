package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView

open class LineLabelInner(context: Context): AppCompatTextView(context), View.OnTouchListener {
    init {
        this._set_colors()
        this.setOnClickListener {
            this.on_click()
        }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 2)
        return this._build_drawable_state(drawableState)
    }

    open fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        return null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.set_height()
        this.set_text()
    }

    open fun _set_colors() {}
    open fun get_height(): Float {
        return 0F
    }

    open fun get_label_text(): String {
        return "TODO"
    }

    open fun on_click() { }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        TODO("Not yet implemented")
    }

    private fun set_height() {
        this.layoutParams.height = this.get_height().toInt()
    }

    private fun set_text() {
        val text = this.get_label_text()
        this.text = text
        this.contentDescription = text
    }

    fun get_opus_manager(): OpusLayerInterface {
        return (this.parent as LineLabelView).get_opus_manager()
    }

    fun get_activity(): MainActivity {
        return (this.context as ContextThemeWrapper).baseContext as MainActivity
    }
}
