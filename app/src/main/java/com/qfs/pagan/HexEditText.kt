package com.qfs.pagan

import android.content.Context
import android.graphics.Typeface
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.util.AttributeSet

open class HexEditText(context: Context, attrs: AttributeSet? = null): PaganEditText(context, attrs) {
    class HexFilter: InputFilter {
        override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            try {
                "$source".toInt(16)
                return if (dest.length < 6) {
                    null
                } else {
                    ""
                }
            } catch (_: NumberFormatException) { }
            return ""
        }
    }

    init {
        this.inputType = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        this.filters = arrayOf(HexFilter())
        this.setSelectAllOnFocus(true)
        this.typeface = Typeface.MONOSPACE
        this.textAlignment = TEXT_ALIGNMENT_TEXT_END
        this.setEms(4)
    }
}