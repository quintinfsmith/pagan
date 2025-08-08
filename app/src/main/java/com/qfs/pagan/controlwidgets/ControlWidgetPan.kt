package com.qfs.pagan.controlwidgets

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.Button
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.PanSliderWidget
import com.qfs.pagan.R
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent

class ControlWidgetPan(level: CtlLineLevel, is_initial_event: Boolean, context: Context, callback: (OpusPanEvent) -> Unit): ControlWidget<OpusPanEvent>(ContextThemeWrapper(context, R.style.pan_widget), level, is_initial_event, R.layout.control_widget_pan, callback) {
    private lateinit var _slider: PanSliderWidget
    private lateinit var _transition_button: Button

    val min = -10
    val max = 10

    override fun on_inflated() {
        this._slider = this.inner.findViewById(R.id.pan_slider)
        this._slider.max = this.max
        this._slider.min = this.min
        this._transition_button = this.inner.findViewById(R.id.pan_transition_type)

        if (this.is_initial_event) {
            this._transition_button.visibility = GONE
        } else {
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
            override fun on_progress_change(slider: PanSliderWidget, value: Int) { }
        }
    }

    init {
        this.orientation = VERTICAL
    }

    override fun on_set(event: OpusPanEvent) {
        (this._transition_button as MaterialButton).setIconResource(
            this.get_activity().get_effect_transition_icon(event.transition)
        )

        val range = (this.max - this.min) / 2
        val progress: Int = range + this.min + (-1 * range * event.value).toInt()
        this._slider.set_progress(progress, true)
    }
}
