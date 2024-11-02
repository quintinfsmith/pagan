package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity.CENTER
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.SeekBar
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusVolumeEvent

class ControlWidgetVolume(default: OpusVolumeEvent, context: Context, callback: (OpusControlEvent) -> Unit): ControlWidget(context, callback) {
    private val _slider = PaganSeekBar(context)
    private val _button = ButtonLabelledIcon(ContextThemeWrapper(context, R.style.volume_widget_button))
    private val _transition_button = ButtonIcon(context)
    private val _min = 0
    private val _max = 127
    private var _lockout_ui: Boolean = false

    init {
        this.orientation = HORIZONTAL

        this._button.set_text(default.value.toString())
        this._button.set_icon(R.drawable.volume)
        this._button.label.minEms = 2

        this._transition_button.setImageResource(R.drawable.volume) // TODO transition icons

        this._slider.max = this._max
        this._slider.min = this._min
        this._slider.progress = default.value

        this._button.setOnClickListener {
            var context = this.context
            while (context !is MainActivity) {
                context = (context as ContextThemeWrapper).baseContext
            }

            val dlg_default = this.get_event().value
            val dlg_title = context.getString(R.string.dlg_set_volume)
            context.dialog_number_input(dlg_title, this._min, this._max, dlg_default) { new_value: Int ->
                val new_event = OpusVolumeEvent(new_value)
                this.set_event(new_event)
                this.callback(new_event)
            }
        }

        this._slider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (this@ControlWidgetVolume._lockout_ui) {
                    return
                }
                this@ControlWidgetVolume._lockout_ui = true
                this@ControlWidgetVolume._button.set_text(p1.toString())
                this@ControlWidgetVolume._lockout_ui = false
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                this@ControlWidgetVolume.callback(OpusVolumeEvent(seekbar.progress))
            }
        })


        this.addView(this._button)
        this.addView(this._slider)
        this.addView(this._transition_button)


        this._button.layoutParams.width = WRAP_CONTENT
        this._button.layoutParams.height = WRAP_CONTENT

        this._transition_button.layoutParams.width = WRAP_CONTENT
        this._transition_button.layoutParams.height = WRAP_CONTENT

        this._slider.layoutParams.width = 0
        this._slider.layoutParams.height = MATCH_PARENT
        (this._slider.layoutParams as LinearLayout.LayoutParams).weight = 1f
        (this._slider.layoutParams as LinearLayout.LayoutParams).gravity = CENTER
    }

    override fun get_event(): OpusVolumeEvent {
        return OpusVolumeEvent(this._slider.progress)
    }

    override fun set_event(event: OpusControlEvent) {
        val value = (event as OpusVolumeEvent).value
        this._slider.progress = value
        this._button.set_text(value.toString())
    }
}