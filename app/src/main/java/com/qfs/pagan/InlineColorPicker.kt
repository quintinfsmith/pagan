package com.qfs.pagan

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.Space
import androidx.appcompat.widget.LinearLayoutCompat

class InlineColorPicker(private val activity: MainActivity, label: String, key: Palette): LinearLayoutCompat(activity, null) {
    private val color_button = ColorButton(activity, null, activity.view_model.color_map[key])
    private val hex_input = HexEditText(activity)

    init {
        this.orientation = HORIZONTAL
        val label_view = PaganTextView(activity)
        val space = Space(activity)

        this.addView(label_view)
        label_view.text = label
        (label_view.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER_VERTICAL
        this.addView(space)
        space.layoutParams.width = 0
        (space.layoutParams as LinearLayout.LayoutParams).weight = 1f

        this.addView(this.hex_input)
        this.addView(this.color_button)

        this.hex_input.setText(this.color_button.get_string())

        this.hex_input.layoutParams.height = WRAP_CONTENT
        this.hex_input.layoutParams.width = WRAP_CONTENT

        (this.hex_input.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER
        var lock_callback = false
        this.hex_input.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (lock_callback) {
                    return
                }
                try {
                    val button = this@InlineColorPicker.color_button
                    button.set_color("#${p0.toString()}", true)
                    this@InlineColorPicker.activity.view_model.color_map[key] = button.get_color()
                } catch (_: IllegalArgumentException) { }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) { }

        })

        this.color_button.layoutParams.height = MATCH_PARENT
        this.color_button.layoutParams.width = 124
        (this.color_button.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER
        this.color_button.set_on_change { new_color: Int ->
            lock_callback = true
            this.hex_input.setText(this.color_button.get_string())
            this@InlineColorPicker.activity.view_model.color_map[key] = new_color
            lock_callback = false
        }
    }
}