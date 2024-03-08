package com.qfs.pagan

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.graphics.toColorLong
import com.qfs.pagan.ColorMap.Palette
import kotlin.math.roundToInt

class InlineColorPicker(private val _activity: MainActivity, label: String, key: Palette): LinearLayoutCompat(_activity, null) {
    private val _color_button = ButtonColor(_activity, null, _activity.view_model.color_map[key])
    private val _hex_input = HexEditText(_activity)

    init {
        this.orientation = HORIZONTAL
        this.gravity = Gravity.END
        val label_view = PaganTextView(_activity)

        this.addView(label_view)
        label_view.text = label
        label_view.setPadding(0,0, this.resources.getDimension(R.dimen.inline_color_picker_label_padding).roundToInt(), 0)
        (label_view.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER_VERTICAL

        this.addView(this._color_button)
        this.addView(this._hex_input)


        this._hex_input.setText(this._color_button.get_string())

        this._hex_input.layoutParams.height = WRAP_CONTENT
        this._hex_input.layoutParams.width = WRAP_CONTENT

        (this._hex_input.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER
        var lock_callback = false
        this._hex_input.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (lock_callback) {
                    return
                }
                try {
                    val button = this@InlineColorPicker._color_button
                    button.set_color("#${p0.toString()}", true)
                    this@InlineColorPicker.set_activity_color(key, button.get_color())
                } catch (_: IllegalArgumentException) { }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) { }
        })

        this._color_button.layoutParams.height = this.resources.getDimension(R.dimen.inline_color_picker_button_size).roundToInt()
        this._color_button.layoutParams.width = this.resources.getDimension(R.dimen.inline_color_picker_button_size).roundToInt()
        (this._color_button.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER
        this._color_button.set_on_change { new_color: Int ->
            lock_callback = true
            this._hex_input.setText(this._color_button.get_string())
            this.set_activity_color(key, new_color)
            lock_callback = false
        }
        this._color_button.setOnClickListener {
            this._activity.dialog_color_picker(this._color_button.get_color().toColorLong()) { new_color: Int ->
                this._color_button.set_color(new_color)
            }
        }
    }


    fun set_activity_color(key: Palette, new_color: Int) {
        this._activity.view_model.color_map[key] = new_color
        this._activity.save_configuration()
    }
}