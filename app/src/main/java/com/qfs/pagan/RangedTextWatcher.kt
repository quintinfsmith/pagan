package com.qfs.pagan

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class RangedTextWatcher(var number_input: EditText, var min_value: Int, var max_value: Int, var onEnter: () -> Unit): TextWatcher {
    var lockout = false
    init {
        this.number_input.addTextChangedListener(this)
    }
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (this.lockout) {
            return
        }
        if (! (this.number_input as RangedNumberInput).confirm_required) {
            return
        }
        if (p0 == "\n") {
            this.onEnter()
        }
    }
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun afterTextChanged(p0: Editable?) {
        if (this.lockout) {
            return
        }

        if (p0 == null || p0.toString() == "") {
            this.number_input.setText("${this.min_value}")
            this.number_input.selectAll()
        } else {
            val value = p0.toString().toInt()
            if (value > this.max_value) {
                this.number_input.setText("${this.max_value}")
                this.number_input.selectAll()
            }
        }

        if (! (this.number_input as RangedNumberInput).confirm_required) {
            this.onEnter()
        }
    }
}