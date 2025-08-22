package com.qfs.pagan.controlwidgets

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.Button
import android.widget.NumberPicker
import android.widget.SeekBar
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.R
import com.qfs.pagan.RangedFloatInput
import com.qfs.pagan.RangedIntegerInput
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent

class ControlWidgetDelay(level: CtlLineLevel, is_initial_event: Boolean, context: Context, callback: (DelayEvent) -> Unit): ControlWidget<DelayEvent>(context, level, is_initial_event, R.layout.control_widget_delay, callback) {
    private lateinit var _numerator: RangedIntegerInput
    private lateinit var _denominator: RangedIntegerInput
    private lateinit var _fade: RangedFloatInput
    private lateinit var _transition_button: Button

    val min = 1
    val max = 9999
    private var _lockout_ui: Boolean = false

    override fun on_inflated() {
        this._numerator = this.inner.findViewById(R.id.numerator)
        this._denominator = this.inner.findViewById(R.id.denominator)
        this._fade = this.inner.findViewById(R.id.fade)
        this._transition_button = this.inner.findViewById(R.id.transition_button)

        this._numerator.set_range(this.min, this.max)
        this._denominator.set_range(this.min, this.max)
        this._fade.set_range(0F, 1F)

        if (this.is_initial_event) {
            this._transition_button.visibility = GONE
        } else {
            this._transition_button.setOnClickListener {
                val main = (this.context as ActivityEditor)
                main.get_action_interface().set_ctl_transition()
            }
        }

        var context = this.context
        while (context !is ActivityEditor) {
            context = (context as ContextThemeWrapper).baseContext
        }

        val main = this.get_activity()
        this._numerator.value_set_callback = { value: Int? ->
            main.get_action_interface().set_delay_at_cursor(
                Rational(value ?: this.min, this.working_event?.frequency?.denominator ?: 1),
                this.working_event?.repeat_decay ?: 1F,
                this.working_event?.repeat ?: 1
            )
        }
        this._denominator.value_set_callback = { value: Int? ->
            main.get_action_interface().set_delay_at_cursor(
                Rational(this.working_event?.frequency?.numerator ?: this.min, value ?: 1),
                this.working_event?.repeat_decay ?: 1F,
                this.working_event?.repeat ?: 1
            )
        }

        this._fade.value_set_callback = { value: Float? ->
            main.get_action_interface().set_delay_at_cursor(
                this.working_event?.frequency ?: Rational(1, 1),
                value ?: 0F,
                this.working_event?.repeat ?: 1
            )
        }
    }

    init {
        this.orientation = HORIZONTAL
    }

    override fun on_set(event: DelayEvent) {
        this._fade.set_value(event.repeat_decay)
        this._denominator.set_value(event.frequency.denominator)
        this._numerator.set_value(event.frequency.numerator)
        (this._transition_button as MaterialButton).setIconResource(
            this.get_activity().get_effect_transition_icon(event.transition)
        )
    }
}
