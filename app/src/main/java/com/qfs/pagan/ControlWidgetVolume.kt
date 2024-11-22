package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.SeekBar
import com.qfs.pagan.opusmanager.ControlTransition
import com.qfs.pagan.opusmanager.OpusVolumeEvent

class ControlWidgetVolume(default: OpusVolumeEvent, is_initial_event: Boolean, context: Context, callback: (OpusVolumeEvent) -> Unit): ControlWidget<OpusVolumeEvent>(context, default, is_initial_event, R.layout.control_widget_volume, callback) {
    private lateinit var _slider: PaganSeekBar
    private lateinit var _button: ButtonLabelledIcon
    private lateinit var _transition_button: ButtonIcon
    private val _min = 0
    private val _max = 127
    private var _lockout_ui: Boolean = false

    override fun on_inflated() {
        this._slider = this.inner.findViewById(R.id.volume_slider)
        this._button = this.inner.findViewById(R.id.volume_button)
        this._transition_button = this.inner.findViewById(R.id.volume_transition_button)

        this._button.set_text(this.working_event.value.toString())
        this._button.set_icon(R.drawable.volume)
        this._button.label.minEms = 2

        if (this.is_initial_event) {
            this._transition_button.visibility = View.GONE
        } else {
            this._transition_button.setImageResource(R.drawable.volume) // TODO transition icons
            this._transition_button.setOnClickListener {
                val main = (this.context as MainActivity)
                val control_transitions = ControlTransition.values()
                val options = List(control_transitions.size) { i: Int ->
                    Pair(control_transitions[i], control_transitions[i].name)
                }

                val event = this.get_event()
                main.dialog_popup_menu("Transition", options, default = event.transition) { i: Int, transition: ControlTransition ->
                    event.transition = transition
                    this.set_event(event)
                }
            }
        }

        this._slider.max = this._max
        this._slider.min = this._min
        this._slider.progress = this.working_event.value

        this._button.setOnClickListener {
            var context = this.context
            while (context !is MainActivity) {
                context = (context as ContextThemeWrapper).baseContext
            }

            val dlg_default = this.get_event().value
            val dlg_title = context.getString(R.string.dlg_set_volume)
            context.dialog_number_input(dlg_title, this._min, this._max, dlg_default) { new_value: Int ->
                val new_event = OpusVolumeEvent(new_value, this.get_event().transition)
                this.set_event(new_event)
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
                this@ControlWidgetVolume.set_event(OpusVolumeEvent(seekbar.progress, this@ControlWidgetVolume.get_event().transition))
            }
        })
    }

    init {
        this.orientation = HORIZONTAL
    }

    override fun on_set(event: OpusVolumeEvent) {
        this._slider.progress = event.value
        this._button.set_text(event.value.toString())
    }

}