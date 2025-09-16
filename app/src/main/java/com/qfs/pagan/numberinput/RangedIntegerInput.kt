package com.qfs.pagan.numberinput

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import com.qfs.pagan.R
import com.qfs.pagan.RangedTextWatcher
import java.util.Locale
import kotlin.math.max

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
            this.text.toString().toInt()
        } catch (nfe: NumberFormatException) {
            null
        }
    }
    override fun _set_value(new_value: Int) {
        this.setText(String.Companion.format(Locale.getDefault(), "%d", new_value))
    }

    override fun init_range(attrs: AttributeSet?) {
        this.context.withStyledAttributes(attrs, R.styleable.Ranged) {
            this@RangedIntegerInput.max = getInteger(R.styleable.Ranged_max, 1)
            this@RangedIntegerInput.min = getInteger(R.styleable.Ranged_min, 1)
        }
    }
}