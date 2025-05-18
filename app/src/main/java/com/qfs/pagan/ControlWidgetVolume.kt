package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.opusmanager.ControlTransition
import com.qfs.pagan.opusmanager.OpusVolumeEvent

class ControlWidgetVolume(default: OpusVolumeEvent, is_initial_event: Boolean, context: Context, callback: (OpusVolumeEvent) -> Unit): ControlWidget<OpusVolumeEvent>(context, default, is_initial_event, R.layout.control_widget_volume, callback) {
    private lateinit var _slider: SeekBar
    private lateinit var _button: Button
    private lateinit var _transition_button: Button
    val min = 0
    val max = 100
    private var _lockout_ui: Boolean = false

    override fun on_inflated() {
        this._slider = this.inner.findViewById(R.id.volume_slider)
        this._button = this.inner.findViewById(R.id.volume_button)
        this._transition_button = this.inner.findViewById(R.id.volume_transition_button)

        this.set_text((this.working_event.value * 100).toInt())
        //this._button.set_icon(R.drawable.volume_widget)
        this._button.minEms = 2

        if (this.is_initial_event) {
            this._transition_button.visibility = View.GONE
        } else {
            (this._transition_button as MaterialButton).setIconResource(
                when (this.working_event.transition) {
                    ControlTransition.Instant -> R.drawable.immediate
                    ControlTransition.Linear -> R.drawable.linear
                }
            )

            this._transition_button.setOnClickListener {
                val main = (this.context as MainActivity)
                main.get_action_interface().set_ctl_transition()
            }
        }

        this._slider.max = this.max
        this._slider.min = this.min
        this._slider.progress = (this.working_event.value * this.max.toFloat()).toInt()

        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }
        this._button.setOnClickListener {
            (context as MainActivity).get_action_interface().set_volume()
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
                context.get_action_interface().set_volume(seekbar.progress)
            }
        })
    }

    init {
        this.orientation = HORIZONTAL
    }

    fun set_text(value: Int) {
        this._button.text = "%03d%%".format(value)
    }

    override fun on_set(event: OpusVolumeEvent) {
        this._slider.progress = (event.value * this.max.toFloat()).toInt()
        val value = (event.value * 100).toInt()
        this.set_text(value)
        (this._transition_button as MaterialButton).setIconResource(when (event.transition) {
            ControlTransition.Instant -> R.drawable.immediate
            ControlTransition.Linear -> R.drawable.linear
        })
    }

}