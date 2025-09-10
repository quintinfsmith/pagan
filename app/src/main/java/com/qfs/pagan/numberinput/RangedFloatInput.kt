package com.qfs.pagan.numberinput

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import com.qfs.pagan.R
import com.qfs.pagan.RangedTextWatcher
import java.util.Locale
import kotlin.math.max

class RangedFloatInput(context: Context, attrs: AttributeSet? = null): RangedNumberInput<Float>(context, attrs) {
    var precision: Int? = null
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

    fun set_precision(precision: Int) {
        this.precision = precision
    }


    override fun get_value(): Float? {
        return try {
            val current_value = this.text.toString().toFloat()
            max(this.min ?: current_value, current_value)
        } catch (nfe: NumberFormatException) {
            null
        }
    }

    override fun init_range(attrs: AttributeSet?) {
        this.context.withStyledAttributes(attrs, R.styleable.Ranged) {
            this@RangedFloatInput.max = getFloat(R.styleable.Ranged_fmax, 1F)
            this@RangedFloatInput.min = getFloat(R.styleable.Ranged_fmin, 0F)
            this@RangedFloatInput.set_precision(getInteger(R.styleable.Ranged_precision, 2))
        }
    }

    override fun _set_value(new_value: Float) {
        val fmt = "%.${this.precision  ?: 0}f"
        this.setText(String.Companion.format(Locale.getDefault(), fmt, new_value))
    }
}