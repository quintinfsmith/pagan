package com.qfs.pagan

import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.TextView
import kotlin.math.max

class RangedNumberInput(context: Context, attrs: AttributeSet? = null): PaganEditText(context, attrs) {
    var max: Int
    var min: Int
    var value_set_callback: ((RangedNumberInput) -> Unit)? = null
    var watcher: RangedTextWatcher
    var confirm_required = true
    init {
        /*
            Use filters to ensure a number is input
            Then the listener to ensure the value is <= the maximum_value
            THEN on close, return the max(min_value, output)
         */
        this.inputType = InputType.TYPE_CLASS_NUMBER
        this.filters = arrayOf(NumeralFilter())
        this.typeface = Typeface.MONOSPACE
        this.setSelectAllOnFocus(true)
        this.textAlignment = TEXT_ALIGNMENT_TEXT_END

        val styled_attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.Ranged, 0, 0)

        try {
            this.max = styled_attributes.getInteger(R.styleable.Ranged_max, 2)
            this.min = styled_attributes.getInteger(R.styleable.Ranged_min, 0)
        } finally {
            styled_attributes.recycle()
        }

        this.watcher = RangedTextWatcher(this, this.min, this.max) {
            if (this.value_set_callback != null) {
                this.value_set_callback!!(this)
            }
        }

        this.setOnEditorActionListener { _: TextView?, action_id: Int?, _: KeyEvent? ->
            if (action_id != null) {
                if (this.confirm_required && this.value_set_callback != null) {
                    this.value_set_callback!!(this)
                }
                false
            } else {
                true
            }
        }
    }

    fun set_range(new_min: Int, new_max: Int) {
        this.min = new_min
        this.max = new_max
        this.watcher.min_value = new_min
        this.watcher.max_value = new_max
    }

    fun get_value(): Int? {
        return try {
            max(this.min, this.text.toString().toInt())
        } catch (nfe: NumberFormatException) {
            null
        }
    }

    fun set_value(new_value: Int) {
        this.watcher.lockout = true
        this.setText(new_value.toString())
        this.watcher.lockout = false
    }
}