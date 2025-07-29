package com.qfs.pagan.controlwidgets

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.Button
import android.widget.SeekBar
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.R
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent

class ControlWidgetVelocity(default: OpusVelocityEvent, level: CtlLineLevel, is_initial_event: Boolean, context: Context, callback: (OpusVelocityEvent) -> Unit): ControlWidget<OpusVelocityEvent>(context, default, level, is_initial_event, R.layout.control_widget_volume, callback) {
    private lateinit var _slider: SeekBar
    private lateinit var _button: Button
    private lateinit var _transition_button: Button
    val min = 0
    val max = 127
    private var _lockout_ui: Boolean = false

    override fun on_inflated() {
        this._slider = this.inner.findViewById(R.id.volume_slider)
        this._button = this.inner.findViewById(R.id.volume_button)
        this._transition_button = this.inner.findViewById(R.id.volume_transition_button)

        this.set_text((this.working_event.value * 100).toInt())
        this._button.minEms = 2

        if (this.is_initial_event) {
            this._transition_button.visibility = GONE
        } else {
            (this._transition_button as MaterialButton).setIconResource(
                this.get_activity().get_effect_transition_icon(this.working_event.transition)
            )

            this._transition_button.setOnClickListener {
                val main = (this.context as ActivityEditor)
                main.get_action_interface().set_ctl_transition()
            }
        }

        this._slider.max = this.max
        this._slider.min = this.min
        this._slider.progress = (this.working_event.value * this.max.toFloat()).toInt()

        var context = this.context
        while (context !is ActivityEditor) {
            context = (context as ContextThemeWrapper).baseContext
        }
        this._button.setOnClickListener {
            (context as ActivityEditor).get_action_interface().set_velocity()
        }

        this._slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                val that = this@ControlWidgetVelocity
                if (that._lockout_ui) {
                    return
                }
                that._lockout_ui = true
                that.set_text(p1)
                that._lockout_ui = false
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                context.get_action_interface().set_velocity(seekbar.progress)
            }
        })
    }

    init {
        this.orientation = HORIZONTAL
    }

    fun set_text(value: Int) {
        this._button.text = "%03d%%".format(value)
    }

    override fun on_set(event: OpusVelocityEvent) {
        this._slider.progress = (event.value * 100F).toInt()
        val value = (event.value * 100).toInt()
        this.set_text(value)
        (this._transition_button as MaterialButton).setIconResource(
            this.get_activity().get_effect_transition_icon(event.transition)
        )
    }

}