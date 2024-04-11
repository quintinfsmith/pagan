package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.SeekBar
import kotlin.math.roundToInt

class ControlWidgetVolume(default: Float, context: Context, callback: (Float) -> Unit): ControlWidget(context, callback) {
    private val _slider = PaganSeekBar(context)
    private val _input = ButtonStd(ContextThemeWrapper(context, R.style.icon_button), null)
    private val _min = 0
    private val _max = 128
    private var _lockout_ui: Boolean = false

    init {
        this.orientation = HORIZONTAL

        this._slider.max = this._max
        this._slider.min = this._min
        this._slider.progress = default.toInt()

        this._input.text = default.toString()
        this._input.setOnClickListener {
            val main = this._input.get_main()
            val dlg_default = this.get_value().toInt()
            val dlg_title = context.getString(R.string.dlg_set_volume)
            main.dialog_number_input(dlg_title, this._min, this._max, dlg_default) { new_value: Int ->
                if (new_value != this.get_value().toInt()) {
                    this.set_value(new_value.toFloat())
                    this.callback(new_value.toFloat())
                }
            }
        }

        this._slider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (this@ControlWidgetVolume._lockout_ui) {
                    return
                }
                this@ControlWidgetVolume._lockout_ui = true
                this@ControlWidgetVolume._input.text = p1.toString()
                this@ControlWidgetVolume._lockout_ui = false
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                this@ControlWidgetVolume.callback(seekbar.progress.toFloat())
            }
        })


        this.addView(this._input)
        this.addView(this._slider)

        this._input.layoutParams.width = resources.getDimension(R.dimen.volume_button_width).roundToInt()
        this._input.layoutParams.height = WRAP_CONTENT

        this._slider.layoutParams.width = 0
        this._slider.layoutParams.height = MATCH_PARENT
        (this._slider.layoutParams as LinearLayout.LayoutParams).weight = 1f
    }

    override fun get_value(): Float {
        return this._slider.progress.toFloat()
    }

    override fun set_value(new_value: Float) {
        this._slider.progress = new_value.toInt()
    }
}