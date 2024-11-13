package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.SeekBar
import com.qfs.pagan.opusmanager.ControlTransition
import com.qfs.pagan.opusmanager.OpusPanEvent
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ControlWidgetPan(default: OpusPanEvent, is_initial_event: Boolean, context: Context, callback: (OpusPanEvent) -> Unit): ControlWidget<OpusPanEvent>(ContextThemeWrapper(context, R.style.pan_widget), default, is_initial_event, callback) {
    private val _slider = PaganSeekBar(context)
    private val label_left = PaganTextView(context)
    private val label_right = PaganTextView(context)
    private val _transition_button = ButtonIcon(context)

    private val _min = -100
    private val _max = 100
    private var _lockout_ui: Boolean = false
    init {
        this.orientation = HORIZONTAL

        this.set_text(default.value)

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

        this.label_left.text = "L"
        this.label_right.text = "R"

        this._slider.max = this._max
        this._slider.min = this._min
        this._slider.progress = default.value.roundToInt()

        this._slider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (this@ControlWidgetPan._lockout_ui) {
                    return
                }
                this@ControlWidgetPan.set_text((p1.toFloat() / 100F))
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                val new_event = this@ControlWidgetPan.working_event.copy()
                new_event.value = (seekbar.progress.toFloat() / 100F)
                this@ControlWidgetPan.set_event(new_event)
            }
        })

        this.addView(this.label_left)
        this.addView(this._slider)
        this.addView(this.label_right)
        this.addView(this._transition_button)

        this.label_left.layoutParams.width = WRAP_CONTENT
        this.label_left.layoutParams.height = WRAP_CONTENT
        this.label_right.layoutParams.width = WRAP_CONTENT
        this.label_right.layoutParams.height = WRAP_CONTENT

        this._slider.layoutParams.width = 0
        this._slider.layoutParams.height = MATCH_PARENT
        (this._slider.layoutParams as LinearLayout.LayoutParams).weight = 1f
        (this._slider.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER
    }

    fun set_text(value: Float) {
        this@ControlWidgetPan._lockout_ui = true
        val value_left = ((1F - max(value, 0F)) * 100F).toInt()
        val value_right = ((1F + min(value, 0F)) * 100F).toInt()
        // TODO: Make MonoSpace
        this@ControlWidgetPan.label_right.text = "% 3d%%".format(value_right)
        this@ControlWidgetPan.label_left.text = "% 3d%%".format(value_left)
        this@ControlWidgetPan._lockout_ui = false
    }

    override fun on_set(event: OpusPanEvent) {
        this.set_text(event.value)
        val value = (event.value * 100F).toInt()
        this._slider.progress = value
    }
}
