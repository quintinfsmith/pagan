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
            "${dest.substring(0, dstart)}$source${dest.substring(dend)}".toInt()
            return null
        } catch (_: NumberFormatException) { }
        return ""
    }
}