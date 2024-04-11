package com.qfs.pagan

import android.text.InputFilter
import android.text.Spanned

class NumeralFilter: InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        try {
            // append '0' to account for "1." situations
            val test_value = "${dest.substring(0, dstart)}$source${dest.substring(dend)}0"
            test_value.toFloat()
            return null
        } catch (_: NumberFormatException) { }
        return ""
    }
}