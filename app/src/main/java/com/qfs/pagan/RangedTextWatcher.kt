package com.qfs.pagan

import android.text.Editable
import android.text.TextWatcher

abstract class RangedTextWatcher<T: Number>(private var _number_input: RangedNumberInput<T>, var min_value: T?, var max_value: T?): TextWatcher {
    var lockout = false
    init {
        this._number_input.addTextChangedListener(this)
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (this.lockout) {
            return
        }
        if (! (this._number_input as RangedNumberInput<*>).confirm_required) {
            return
        }
        if (p0 == "\n") {
            this._number_input.callback()
        }
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

    override fun afterTextChanged(p0: Editable?) {
        if (this.lockout) {
            return
        }

        val value: T? = this._number_input.get_value()

        this.min_value?.let {
            if (value == null || this.lt(value, it)) {
                this._number_input.setText("$it")
                this._number_input.selectAll()
            }
        }
        if (this._gt(value, this.max_value)) {
            this._number_input.setText("${this.max_value}")
            this._number_input.selectAll()
        }

        if (! (this._number_input as RangedNumberInput<*>).confirm_required) {
            this._number_input.callback()
        }
    }
    private fun _gt(value: T?, max: T?): Boolean {
        return (value == null || max == null) || this.gt(value, max)
    }

    private fun _lt(value: T?, min: T?): Boolean {
        return (value == null || min == null) || this.lt(value, min)
    }

    abstract fun gt(value: T, max: T): Boolean
    abstract fun lt(value: T, min: T): Boolean
}