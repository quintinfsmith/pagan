package com.qfs.radixulous

import android.text.InputFilter
import android.text.Spanned
import java.lang.Float.parseFloat

// Custom class to define min and max for the edit text
class RangeFilter(var minimum: Float = 0F, var maximum: Float = 0F) : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dStart: Int, dEnd: Int): CharSequence? {
        try {
            val input = parseFloat(dest.toString() + source.toString())
            if (input in minimum..maximum) {
                return null
            }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return ""
    }
}

