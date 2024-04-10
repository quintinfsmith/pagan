package com.qfs.pagan

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.SeekBar

class ControlWidgetVolume(default: Float, context: Context, callback: (Float) -> Unit): ControlWidget(context, callback) {
    private val slider = PaganSeekBar(context)
    private val input = RangedNumberInput(context)
    private val min = 0
    private val max = 128

    private var _lockout_ui: Boolean = false

    init {
        this.orientation = HORIZONTAL

        this.slider.max = this.max
        this.slider.min = this.min
        this.slider.progress = default.toInt()

        this.input.set_range(this.min, this.max)
        this.input.set_value(default.toInt())

        this.input.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) {
                if (this@ControlWidgetVolume._lockout_ui || p0.toString().isEmpty()) {
                    return
                }

                this@ControlWidgetVolume._lockout_ui = true
                this@ControlWidgetVolume.slider.progress = p0.toString().toInt()
                this@ControlWidgetVolume._lockout_ui = false
                this@ControlWidgetVolume.callback(p0.toString().toFloat())
            }
        })

        this.slider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (this@ControlWidgetVolume._lockout_ui) {
                    return
                }
                this@ControlWidgetVolume._lockout_ui = true
                this@ControlWidgetVolume.input.set_value(p1)
                this@ControlWidgetVolume._lockout_ui = false
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                this@ControlWidgetVolume.callback(seekbar.progress.toFloat())
            }
        })


        this.addView(this.input)
        this.addView(this.slider)

        this.input.layoutParams.width = WRAP_CONTENT
        this.input.layoutParams.height = MATCH_PARENT
        this.input.minEms = 2
        this.input.maxEms = 2

        this.slider.layoutParams.width = 0
        this.slider.layoutParams.height = MATCH_PARENT
        (this.slider.layoutParams as LinearLayout.LayoutParams).weight = 1f
    }

    override fun get_value(): Float {
        return this.slider.progress.toFloat()
    }

    override fun set_value(new_value: Float) {
        this._lockout_ui = true
        this.slider.progress = new_value.toInt()
        this.input.set_value(new_value.toInt())
        this._lockout_ui = false
    }
}