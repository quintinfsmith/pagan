package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import com.qfs.pagan.opusmanager.ControlTransition
import com.qfs.pagan.opusmanager.OpusPanEvent

class ControlWidgetPan(default: OpusPanEvent, is_initial_event: Boolean, context: Context, callback: (OpusPanEvent) -> Unit): ControlWidget<OpusPanEvent>(ContextThemeWrapper(context, R.style.pan_widget), default, is_initial_event, R.layout.control_widget_pan, callback) {
    private lateinit var _slider: PanSliderWidget
    private lateinit var _transition_button: ButtonIcon

    private val _min = -10
    private val _max = 10

    override fun on_inflated() {
        this._slider = this.inner.findViewById(R.id.pan_slider)
        this._slider.max = this._max
        this._slider.min = this._min
        val progress = this.working_event.value * this._max.toFloat()
        this._slider.set_progress(progress.toInt(), true)
        this._transition_button = this.inner.findViewById(R.id.pan_transition_type)

        if (this.is_initial_event) {
            this._transition_button.visibility = View.GONE
        } else {
            this._transition_button.setImageResource(R.drawable.volume) // TODO transition icons
            this._transition_button.setOnClickListener {
                val main = (this.context as ContextThemeWrapper).baseContext as MainActivity
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


        this._slider.on_change_listener = object: PanSliderWidget.OnSeekBarChangeListener() {
            override fun on_touch_start(slider: PanSliderWidget) { }
            override fun on_touch_stop(slider: PanSliderWidget) { }
            override fun on_progress_change(slider: PanSliderWidget, value: Int) {
                val new_event = this@ControlWidgetPan.working_event.copy()
                new_event.value = (slider.progress.toFloat() / this@ControlWidgetPan._max.toFloat())
                this@ControlWidgetPan.set_event(new_event)
            }

        }
    }


    init {
        this.orientation = VERTICAL
    }


    override fun on_set(event: OpusPanEvent) {
        val value = (event.value * this._max.toFloat()).toInt()
       // this._slider.progress = value
    }
}
