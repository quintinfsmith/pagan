package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.SeekBar
import com.qfs.pagan.opusmanager.OpusPanEvent
import kotlin.math.roundToInt

class ControlWidgetPan(default: OpusPanEvent, is_initial_event: Boolean, context: Context, callback: (OpusPanEvent) -> Unit): ControlWidget<OpusPanEvent>(context, default, is_initial_event, callback) {
    private val _slider = PaganSeekBar(context)
    private val _button = ButtonLabelledIcon(ContextThemeWrapper(context, R.style.volume_widget_button))
    private val _min = -100
    private val _max = 100
    private var _lockout_ui: Boolean = false
    init {
        this.orientation = HORIZONTAL

        this.set_text(default.value)
        this._button.set_icon(R.drawable.volume)
        this._button.label.minEms = 2

        this._slider.max = this._max
        this._slider.min = this._min
        this._slider.progress = default.value.roundToInt()

        this._button.setOnClickListener {
            var context = this.context
            while (context !is MainActivity) {
                context = (context as ContextThemeWrapper).baseContext
            }

            val dlg_default = (this.get_event().value * 100F).toInt()
            val dlg_title = context.getString(R.string.dlg_set_reverb)
            context.dialog_number_input(dlg_title, this._min, this._max, dlg_default) { new_value: Int ->
                val new_event = this@ControlWidgetPan.working_event.copy()
                new_event.value = (new_value.toFloat() / 100F)
                this.set_event(new_event)
            }
        }

        this._slider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (this@ControlWidgetPan._lockout_ui) {
                    return
                }
                this@ControlWidgetPan.set_text((p1.toFloat() / 100F))
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                val new_event = this@ControlWidgetPan.working_event.copy()
                new_event.value = (seekbar.progress.toFloat() / 100F)
                this@ControlWidgetPan.set_event(new_event)
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

    fun set_text(value: Float) {
        this@ControlWidgetPan._lockout_ui = true

        val text = "% 3d".format((value * 100).toInt())
        this@ControlWidgetPan._button.set_text(text)

        this@ControlWidgetPan._lockout_ui = false
    }

    override fun on_set(event: OpusPanEvent) {
        this.set_text(event.value)
        val value = (event.value * 100F).toInt()
        this._slider.progress = value
    }
}
