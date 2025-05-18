package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.opusmanager.ControlTransition
import com.qfs.pagan.opusmanager.OpusPanEvent

class ControlWidgetPan(default: OpusPanEvent, is_initial_event: Boolean, context: Context, callback: (OpusPanEvent) -> Unit): ControlWidget<OpusPanEvent>(ContextThemeWrapper(context, R.style.pan_widget), default, is_initial_event, R.layout.control_widget_pan, callback) {
    private lateinit var _slider: PanSliderWidget
    private lateinit var _transition_button: Button

    val min = -10
    val max = 10

    override fun on_inflated() {
        this._slider = this.inner.findViewById(R.id.pan_slider)
        this._slider.max = this.max
        this._slider.min = this.min
        val progress = this.working_event.value * this.max.toFloat() * -1F
        this._slider.set_progress(progress.toInt(), true)
        this._transition_button = this.inner.findViewById(R.id.pan_transition_type)

        if (this.is_initial_event) {
            this._transition_button.visibility = View.GONE
        } else {
            (this._transition_button as MaterialButton).setIconResource(when (this.working_event.transition) {
                ControlTransition.Instant -> R.drawable.immediate
                ControlTransition.Linear -> R.drawable.linear
               // ControlTransition.Concave -> TODO()
               // ControlTransition.Convex -> TODO()
            })
            this._transition_button.setOnClickListener {
                val main = this.get_activity()
                main.get_action_interface().set_ctl_transition()
            }
        }


        this._slider.on_change_listener = object: PanSliderWidget.OnSeekBarChangeListener() {
            override fun on_touch_start(slider: PanSliderWidget) { }
            override fun on_touch_stop(slider: PanSliderWidget) {
                val main = this@ControlWidgetPan.get_activity()
                main.get_action_interface().set_pan_at_cursor(slider.progress)
            }
            override fun on_progress_change(slider: PanSliderWidget, value: Int) {
            }
        }
    }

    init {
        this.orientation = VERTICAL
    }

    override fun on_set(event: OpusPanEvent) {
        (this._transition_button as MaterialButton).setIconResource(when (event.transition) {
            ControlTransition.Instant -> R.drawable.immediate
            ControlTransition.Linear -> R.drawable.linear
        })
    }
}
