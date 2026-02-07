/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText

open class HexEditText(context: Context, attrs: AttributeSet? = null): TextInputEditText(context, attrs) {
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
                val new_pre = dest.substring(0,dstart)
                val new_post = dest.substring(dend)
                val new_val = "$new_pre$source$new_post"

                new_val.toInt(16)
                return if (new_val.length <= 6) {
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