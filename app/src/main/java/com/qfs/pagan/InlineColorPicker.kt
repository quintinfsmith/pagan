package com.qfs.pagan

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import kotlin.math.roundToInt
import com.qfs.pagan.ColorMap.Palette

class InlineColorPicker(private val activity: MainActivity, label: String, key: Palette): LinearLayoutCompat(activity, null) {
    private val color_button = ColorButton(activity, null, activity.view_model.color_map[key])
    private val hex_input = HexEditText(activity)

    init {
        this.orientation = HORIZONTAL
        this.gravity = Gravity.END
        val label_view = PaganTextView(activity)

        this.addView(label_view)
        label_view.text = label
        label_view.setPadding(0,0, this.resources.getDimension(R.dimen.inline_color_picker_label_padding).roundToInt(), 0)
        (label_view.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER_VERTICAL

        this.addView(this.color_button)
        this.addView(this.hex_input)


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
                    this@InlineColorPicker.set_activity_color(key, button.get_color())
                } catch (_: IllegalArgumentException) { }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) { }
        })

        this.color_button.layoutParams.height = this.resources.getDimension(R.dimen.inline_color_picker_button_size).roundToInt()
        this.color_button.layoutParams.width = this.resources.getDimension(R.dimen.inline_color_picker_button_size).roundToInt()
        (this.color_button.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER
        this.color_button.set_on_change { new_color: Int ->
            lock_callback = true
            this.hex_input.setText(this.color_button.get_string())
            this.set_activity_color(key, new_color)
            lock_callback = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    fun set_activity_color(key: Palette, new_color: Int) {
        this.activity.view_model.color_map[key] = new_color
        this.activity.save_configuration()
        when (key) {
            Palette.TitleBar,
            Palette.TitleBarText -> {
                this.activity.refresh_toolbar()
            }
            else -> {}
        }
    }
}