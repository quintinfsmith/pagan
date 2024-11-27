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
    private val _max = 100
    private var _lockout_ui: Boolean = false

    override fun on_inflated() {
        this._slider = this.inner.findViewById(R.id.volume_slider)
        this._button = this.inner.findViewById(R.id.volume_button)
        this._transition_button = this.inner.findViewById(R.id.volume_transition_button)

        this.set_text((this.working_event.value * 100).toInt())
        this._button.set_icon(R.drawable.volume_widget)
        this._button.label.minEms = 2

        if (this.is_initial_event) {
            this._transition_button.visibility = View.GONE
        } else {
            this._transition_button.setImageResource(
                when (this.working_event.transition) {
                    ControlTransition.Instant -> R.drawable.immediate
                    ControlTransition.Linear -> R.drawable.linear
                }
            )

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
        this._slider.progress = (this.working_event.value * this._max.toFloat()).toInt()

        this._button.setOnClickListener {
            var context = this.context
            while (context !is MainActivity) {
                context = (context as ContextThemeWrapper).baseContext
            }

            val dlg_default = (this.get_event().value * this._max.toFloat()).toInt()
            val dlg_title = context.getString(R.string.dlg_set_volume)
            context.dialog_number_input(dlg_title, this._min, this._max, dlg_default) { new_value: Int ->
                val new_event = OpusVolumeEvent(new_value.toFloat() / this._max.toFloat(), this.get_event().transition, this.working_event.duration)
                this.set_event(new_event)
            }
        }

        this._slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                val that = this@ControlWidgetVolume
                if (that._lockout_ui) {
                    return
                }
                that._lockout_ui = true
                that.set_text(p1)
                that._lockout_ui = false
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                val that = this@ControlWidgetVolume
                val event = that.get_event()
                that.set_event(OpusVolumeEvent(seekbar.progress.toFloat() / that._max.toFloat(), event.transition, event.duration))
            }
        })
    }

    init {
        this.orientation = HORIZONTAL
    }

    fun set_text(value: Int) {
        this._button.set_text("%03d%%".format(value))
    }
    override fun on_set(event: OpusVolumeEvent) {
        this._slider.progress = (event.value * this._max.toFloat()).toInt()
        val value = (event.value * 100).toInt()
        this.set_text(value)
        this._transition_button.setImageResource(when (event.transition) {
            ControlTransition.Instant -> R.drawable.immediate
            ControlTransition.Linear -> R.drawable.linear
        })
    }

}