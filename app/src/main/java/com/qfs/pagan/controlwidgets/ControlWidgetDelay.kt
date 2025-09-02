package com.qfs.pagan.controlwidgets

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.Button
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.R
import com.qfs.pagan.RangedFloatInput
import com.qfs.pagan.RangedIntegerInput
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent

class ControlWidgetDelay(level: CtlLineLevel, is_initial_event: Boolean, context: Context, callback: (DelayEvent) -> Unit): ControlWidget<DelayEvent>(context, level, is_initial_event, R.layout.control_widget_delay, callback) {
    private lateinit var _echo: RangedIntegerInput
    private lateinit var _numerator: RangedIntegerInput
    private lateinit var _denominator: RangedIntegerInput
    private lateinit var _fade: SeekBar
    private lateinit var _label: TextView
    companion object {
        val DEFAULT_NUMERATOR = 1
        val DEFAULT_DENOMINATOR = 1
        val DEFAULT_FADE = 1F
        val DEFAULT_REPEAT = 0
    }

    val min = 1
    val max = 9999
    private var _lockout_ui: Boolean = false

    override fun on_inflated() {
        this._echo = this.inner.findViewById(R.id.echo)
        this._numerator = this.inner.findViewById(R.id.numerator)
        this._denominator = this.inner.findViewById(R.id.denominator)
        this._fade = this.inner.findViewById(R.id.fade)
        this._label = this.inner.findViewById(R.id.fade_label)

        this._echo.set_auto_resize(true)
        this._numerator.set_auto_resize(true)
        this._denominator.set_auto_resize(true)

        this._echo.confirm_required = false
        this._numerator.confirm_required = false
        this._denominator.confirm_required = false

        this._echo.textAlignment = TEXT_ALIGNMENT_CENTER
        this._numerator.textAlignment = TEXT_ALIGNMENT_CENTER
        this._denominator.textAlignment = TEXT_ALIGNMENT_CENTER

        this._echo.set_range(0, 99)
        this._numerator.set_range(this.min, this.max)
        this._denominator.set_range(this.min, this.max)

        var context = this.context
        while (context !is ActivityEditor) {
            context = (context as ContextThemeWrapper).baseContext
        }

        val main = this.get_activity()
        this._numerator.value_set_callback = { value: Int? ->
            main.get_action_interface().set_delay_at_cursor(
                value ?: DEFAULT_NUMERATOR,
                this.working_event?.denominator ?: DEFAULT_DENOMINATOR,
                this.working_event?.fade ?: DEFAULT_FADE,
                this.working_event?.echo ?: DEFAULT_REPEAT
            )
        }

        this._denominator.value_set_callback = { value: Int? ->
            main.get_action_interface().set_delay_at_cursor(
                this.working_event?.numerator ?: DEFAULT_NUMERATOR,
                value ?: DEFAULT_DENOMINATOR,
                this.working_event?.fade ?: DEFAULT_FADE,
                this.working_event?.echo ?: DEFAULT_REPEAT
            )
        }

        this._fade.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) { }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                val that = this@ControlWidgetDelay
                context.get_action_interface().set_delay_at_cursor(
                    that.working_event?.numerator ?: DEFAULT_NUMERATOR,
                    that.working_event?.denominator ?: DEFAULT_DENOMINATOR,
                    1F - (seekbar.progress.toFloat() / seekbar.max.toFloat()),
                    that.working_event?.echo ?: DEFAULT_REPEAT
                )
            }
        })

        this._echo.value_set_callback = { value: Int? ->
            main.get_action_interface().set_delay_at_cursor(
                this.working_event?.numerator ?: DEFAULT_NUMERATOR,
                this.working_event?.denominator ?: DEFAULT_DENOMINATOR,
                this.working_event?.fade ?: DEFAULT_FADE,
                value ?: DEFAULT_REPEAT
            )
        }
    }

    init {
        this.orientation = HORIZONTAL
    }

    override fun on_set(event: DelayEvent) {
        this._fade.progress = this._fade.max - (this._fade.max * event.fade).toInt()
        this._denominator.set_value(event.denominator)
        this._numerator.set_value(event.numerator)
        this._echo.set_value(event.echo)
        this._label.text = this.context.getString(R.string.contextmenu_delay_attenuation, ((1F - event.fade) * 100).toInt())
    }
}
