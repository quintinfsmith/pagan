package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.SeekBar
import kotlin.math.roundToInt

class ControlWidgetVolume(default: Float, context: Context, callback: (Float) -> Unit): ControlWidget(context, callback) {
    private val slider = PaganSeekBar(context)
    private val input = ButtonStd(ContextThemeWrapper(context, R.style.icon_button), null)
    private val min = 0
    private val max = 128

    private var _lockout_ui: Boolean = false

    init {
        this.orientation = HORIZONTAL

        this.slider.max = this.max
        this.slider.min = this.min
        this.slider.progress = default.toInt()

        this.input.text = default.toInt().toString()
        this.input.setOnClickListener {
            this.input.get_main().dialog_number_input(context.getString(R.string.dlg_set_volume), this.min, this.max, this.get_value().toInt()) { value: Int ->
                val new_value = value.toFloat()
                if (new_value != this.get_value()) {
                    this.set_value(new_value)
                    this.callback(new_value)
                }
            }
        }

        this.slider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (this@ControlWidgetVolume._lockout_ui) {
                    return
                }
                this@ControlWidgetVolume._lockout_ui = true
                this@ControlWidgetVolume.input.text = p1.toString()
                this@ControlWidgetVolume._lockout_ui = false
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                this@ControlWidgetVolume.callback(seekbar.progress.toFloat())
            }
        })


        this.addView(this.input)
        this.addView(this.slider)

        this.input.layoutParams.width = resources.getDimension(R.dimen.volume_button_width).roundToInt()
        this.input.layoutParams.height = WRAP_CONTENT

        this.slider.layoutParams.width = 0
        this.slider.layoutParams.height = MATCH_PARENT
        (this.slider.layoutParams as LinearLayout.LayoutParams).weight = 1f
    }

    override fun get_value(): Float {
        return this.slider.progress.toFloat()
    }

    override fun set_value(new_value: Float) {
        this.slider.progress = new_value.toInt()
    }
}