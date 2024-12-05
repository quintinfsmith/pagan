package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.SeekBar
import com.qfs.pagan.opusmanager.OpusReverbEvent
import kotlin.math.roundToInt

class ControlWidgetReverb(default: OpusReverbEvent, is_initial_event: Boolean, context: Context, callback: (OpusReverbEvent) -> Unit): ControlWidget<OpusReverbEvent>(context, default, is_initial_event, R.layout.control_widget_reverb, callback) {
    private val _slider = SeekBar(ContextThemeWrapper(context, R.style.Theme_Pagan_SeekBar))
    private val _button = ButtonLabelledIcon(ContextThemeWrapper(context, R.style.volume_widget_button))
    private val _min = 0f
    private val _max = 100f
    private var _lockout_ui: Boolean = false
    init {
        this.orientation = HORIZONTAL

        this._button.set_text(default.value.toString())
        this._button.set_icon(R.drawable.volume)
        this._button.label.minEms = 2

        this._slider.max = this._max.roundToInt()
        this._slider.min = this._min.roundToInt()
        this._slider.progress = default.value.roundToInt()

        this._button.setOnClickListener {
            var context = this.context
            while (context !is MainActivity) {
                context = (context as ContextThemeWrapper).baseContext
            }

            val dlg_default = this.get_event().value
            val dlg_title = context.getString(R.string.dlg_set_reverb)
            context.dialog_float_input(dlg_title, this._min, this._max, dlg_default) { new_value: Float ->
                val new_event = OpusReverbEvent(new_value)
                this.set_event(new_event)
            }
        }

        this._slider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (this@ControlWidgetReverb._lockout_ui) {
                    return
                }
                this@ControlWidgetReverb._lockout_ui = true
                this@ControlWidgetReverb._button.set_text(p1.toString())
                this@ControlWidgetReverb._lockout_ui = false
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                this@ControlWidgetReverb.set_event(this@ControlWidgetReverb.get_event())
            }
        })


        this.addView(this._button)
        this.addView(this._slider)

        this._button.layoutParams.width = WRAP_CONTENT
        this._button.layoutParams.height = WRAP_CONTENT

        this._slider.layoutParams.width = 0
        this._slider.layoutParams.height = MATCH_PARENT
        (this._slider.layoutParams as LinearLayout.LayoutParams).weight = 1f
        (this._slider.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER
    }


    override fun on_set(event: OpusReverbEvent) {
        val value = event.value
        this._slider.progress = value.roundToInt()
        this._button.set_text(value.toString())
    }

    override fun on_inflated() {
        TODO("Not yet implemented")
    }
}
