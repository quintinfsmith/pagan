package com.qfs.pagan

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class RangedTextWatcher(private var _number_input: EditText, var min_value: Int, var max_value: Int, var on_enter: () -> Unit): TextWatcher {
    var lockout = false
    init {
        this._number_input.addTextChangedListener(this)
    }
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (this.lockout) {
            return
        }
        if (! (this._number_input as RangedNumberInput).confirm_required) {
            return
        }
        if (p0 == "\n") {
            this.on_enter()
        }
    }
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun afterTextChanged(p0: Editable?) {
        if (this.lockout) {
            return
        }

        if (p0 == null || p0.toString() == "") {
            this._number_input.setText("${this.min_value}")
            this._number_input.selectAll()
        } else {
            val value = p0.toString().toInt()
            if (value > this.max_value) {
                this._number_input.setText("${this.max_value}")
                this._number_input.selectAll()
            }
        }

        if (! (this._number_input as RangedNumberInput).confirm_required) {
            this.on_enter()
        }
    }
}