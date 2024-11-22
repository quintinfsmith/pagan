package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.SeekBar
import com.qfs.pagan.opusmanager.ControlTransition
import com.qfs.pagan.opusmanager.OpusPanEvent
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ControlWidgetPan(default: OpusPanEvent, is_initial_event: Boolean, context: Context, callback: (OpusPanEvent) -> Unit): ControlWidget<OpusPanEvent>(ContextThemeWrapper(context, R.style.pan_widget), default, is_initial_event, R.layout.control_widget_pan, callback) {
    private lateinit var _slider: PaganSeekBar
    private lateinit var label_left: PaganTextView
    private lateinit var label_right: PaganTextView
    private lateinit var _transition_button: ButtonIcon

    private val _min = -10
    private val _max = 10
    private var _lockout_ui: Boolean = false

    override fun on_inflated() {
        this._slider = this.inner.findViewById(R.id.pan_slider)
        this._slider.max = this._max
        this._slider.min = this._min
         this.label_left = this.inner.findViewById(R.id.pan_value_left)
        this.label_right = this.inner.findViewById(R.id.pan_value_right)
        this._transition_button = this.inner.findViewById(R.id.pan_transition_type)

        this.set_text(this.working_event.value)
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

        this._slider.progress = this.working_event.value.roundToInt()

        this._slider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (this@ControlWidgetPan._lockout_ui) {
                    return
                }
                this@ControlWidgetPan.set_text((p1.toFloat() / this@ControlWidgetPan._max.toFloat()))
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                val new_event = this@ControlWidgetPan.working_event.copy()
                new_event.value = (seekbar.progress.toFloat() / this@ControlWidgetPan._max.toFloat())
                this@ControlWidgetPan.set_event(new_event)
            }
        })
    }


    init {
        this.orientation = VERTICAL
    }

    fun set_text(value: Float) {
        this@ControlWidgetPan._lockout_ui = true
        val value_left = ((1F - max(value, 0F)) * 100F).roundToInt()
        val value_right = ((1F + min(value, 0F)) * 100F).roundToInt()
        this@ControlWidgetPan.label_right.text = "$value_right%"
        this@ControlWidgetPan.label_left.text = "$value_left%"
        this@ControlWidgetPan._lockout_ui = false
    }

    override fun on_set(event: OpusPanEvent) {
        this.set_text(event.value)
        val value = (event.value * this._max.toFloat()).toInt()
        this._slider.progress = value
    }
}
