package com.qfs.pagan.numberinput

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.withStyledAttributes
import com.qfs.pagan.NumeralFilter
import com.qfs.pagan.R
import com.qfs.pagan.RangedTextWatcher
import kotlin.math.min

abstract class RangedNumberInput<T: Number>(context: Context, attrs: AttributeSet? = null): AppCompatEditText(ContextThemeWrapper(context, R.style.Theme_Pagan_EditText), attrs) {
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

        this.context.withStyledAttributes(attrs, R.styleable.Ranged) {
            this@RangedNumberInput.confirm_required = getBoolean(R.styleable.Ranged_require_confirm, true)
            this@RangedNumberInput.confirm_on_unfocus = getBoolean(R.styleable.Ranged_confirm_on_unfocus, false)
            this@RangedNumberInput.set_auto_resize(getBoolean(R.styleable.Ranged_auto_resize, false))
        }

        this.init_range(attrs)


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

    abstract fun init_range(attrs: AttributeSet?)

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
            this.setSelection(
                min(this.text?.length ?: 0, backup_selection.first),
                min(this.text?.length ?: 0, backup_selection.second)
            )
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previous_rect: Rect?) {
        super.onFocusChanged(focused, direction, previous_rect)
        if (!focused && this.confirm_on_unfocus) {
            this.callback()
        }
    }
}