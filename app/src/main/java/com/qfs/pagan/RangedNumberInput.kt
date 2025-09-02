package com.qfs.pagan

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.text.InputType
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.widget.TextView
import java.util.Locale
import kotlin.math.max

abstract class RangedNumberInput<T: Number>(context: Context, attrs: AttributeSet? = null): androidx.appcompat.widget.AppCompatEditText(ContextThemeWrapper(context, R.style.Theme_Pagan_EditText), attrs) {
    var max: T? = null
    var min: T? = null
    var value_set_callback: ((T?) -> Unit)? = null
    abstract var _watcher: RangedTextWatcher<T>
    var confirm_required = true
    var auto_resize = false
    var confirm_on_unfocus = false

    init {
        /*
            Use filters to ensure a number is input
            Then the listener to ensure the value is <= the maximum_value
            THEN on close, return the max(min_value, output)
         */
        this.filters = arrayOf(NumeralFilter())
        this.typeface = Typeface.MONOSPACE
        this.setSelectAllOnFocus(true)

        this.textAlignment = TEXT_ALIGNMENT_TEXT_END

        this.init_range()


        this.setOnEditorActionListener { _: TextView?, action_id: Int?, _: KeyEvent? ->
            if (action_id != null) {
                if (this.confirm_required && this.value_set_callback != null) {
                    this.callback()
                }
                false
            } else {
                true
            }
        }
    }

    abstract fun init_range()

    fun set_auto_resize(value: Boolean) {
        if (value) {
            this.textAlignment = TEXT_ALIGNMENT_CENTER
        } else {
            this.textAlignment = TEXT_ALIGNMENT_TEXT_END
        }
    }

    fun set_range(new_min: T?, new_max: T? = null) {
        this.min = new_min
        this.max = new_max
        this._watcher.min_value = new_min
        this._watcher.max_value = new_max
    }

    fun set_value(new_value: T) {
        this._watcher.lockout = true
        this._set_value(new_value)
        this._watcher.lockout = false
    }

    abstract fun _set_value(new_value: T)
    abstract fun get_value(): T?

    fun callback() {
        this.value_set_callback?.let {
            val backup_selection = Pair(this.selectionStart, this.selectionEnd)
            it(this.get_value())
            this.setSelection(backup_selection.first, backup_selection.second)
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previous_rect: Rect?) {
        super.onFocusChanged(focused, direction, previous_rect)
        if (!focused && this.confirm_on_unfocus) {
            this.callback()
        }
    }
}

class RangedIntegerInput(context: Context, attrs: AttributeSet? = null): RangedNumberInput<Int>(context, attrs) {
    override var _watcher = object: RangedTextWatcher<Int>(this, this.min, this.max) {
        override fun gt(value: Int, max: Int): Boolean {
            return value > max
        }
        override fun lt(value: Int, min: Int): Boolean {
            return value < min
        }
    }
    init {
        this.inputType = InputType.TYPE_CLASS_NUMBER
    }

    override fun get_value(): Int? {
        return try {
            val current_value = this.text.toString().toInt()
            max(this.min ?: current_value, current_value)
        } catch (nfe: NumberFormatException) {
            null
        }
    }
    override fun _set_value(new_value: Int) {
        this.setText(String.format(Locale.getDefault(), "%d", new_value))
    }

    override fun init_range() {
        val styled_attributes = this.context.theme.obtainStyledAttributes(null, R.styleable.Ranged, 0, 0)

        try {
            this.max = styled_attributes.getInteger(R.styleable.Ranged_max, 1)
            this.min = styled_attributes.getInteger(R.styleable.Ranged_min, 0)
        } finally {
            styled_attributes.recycle()
        }
    }
}

class RangedFloatInput(context: Context, attrs: AttributeSet? = null): RangedNumberInput<Float>(context, attrs) {
    override var _watcher = object: RangedTextWatcher<Float>(this, this.min, this.max) {
        override fun gt(value: Float, max: Float): Boolean {
            return value > max
        }

        override fun lt(value: Float, min: Float): Boolean {
            return value < min
        }
    }

    init {
        this.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    override fun get_value(): Float? {
        return try {
            val current_value = this.text.toString().toFloat()
            max(this.min ?: current_value, current_value)
        } catch (nfe: NumberFormatException) {
            null
        }
    }

    override fun init_range() {
        val styled_attributes = this.context.theme.obtainStyledAttributes(null, R.styleable.Ranged, 0, 0)
        try {
            this.max = styled_attributes.getFloat(R.styleable.Ranged_maxf, 1f)
            this.min = styled_attributes.getFloat(R.styleable.Ranged_minf, 0f)
        } finally {
            styled_attributes.recycle()
        }
    }
    override fun _set_value(new_value: Float) {
        this.setText(String.format(Locale.getDefault(), "%f", new_value))
    }
}
