package com.qfs.pagan.controlwidgets

import android.content.Context
import android.widget.Button
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.R
import com.qfs.pagan.numberinput.RangedFloatInput
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import kotlin.math.roundToInt

class ControlWidgetTempo(_level: CtlLineLevel, is_initial_event: Boolean, context: Context, callback: (OpusTempoEvent) -> Unit): ControlWidget<OpusTempoEvent>(context, CtlLineLevel.Global, is_initial_event, R.layout.control_widget_tempo, callback) {
    private lateinit var input: RangedFloatInput
    private lateinit var _transition_button: Button

    override fun on_inflated() {
        this.input = this.inner.findViewById(R.id.tempo_value)
        this._transition_button = this.inner.findViewById(R.id.transition_button)

        this.input.value_set_callback = { value: Float? ->
            if (value != null) {
                (this.context as ActivityEditor).get_action_interface().set_tempo_at_cursor(value)
            }
        }

        if (this.is_initial_event) {
            this._transition_button.visibility = GONE
        } else {
            this._transition_button.setOnClickListener {
                val main = (this.context as ActivityEditor)
                main.get_action_interface().set_ctl_transition()
            }
        }

    }

    init {
        this.orientation = HORIZONTAL
    }

    override fun on_set(event: OpusTempoEvent) {
        val value = event.value
        this.input.set_value((value * 1000F).roundToInt().toFloat() / 1000F)
        (this._transition_button as MaterialButton).setIconResource(
            this.get_activity().get_effect_transition_icon(event.transition)
        )
    }
}
