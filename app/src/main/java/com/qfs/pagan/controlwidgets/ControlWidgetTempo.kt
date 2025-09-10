package com.qfs.pagan.controlwidgets

import android.content.Context
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.R
import com.qfs.pagan.RangedDoubleInput
import com.qfs.pagan.RangedFloatInput
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import kotlin.math.roundToInt

class ControlWidgetTempo(_level: CtlLineLevel, is_initial_event: Boolean, context: Context, callback: (OpusTempoEvent) -> Unit): ControlWidget<OpusTempoEvent>(context, CtlLineLevel.Global, is_initial_event, R.layout.control_widget_tempo, callback) {
    private lateinit var input: RangedDoubleInput
    val min = 0.0
    val max = 1024.0


    override fun on_inflated() {
        this.input = this.inner.findViewById(R.id.tempo_value)
        this.input.set_range(this.min, this.max)
        this.input.set_precision(3)
        this.input.confirm_required = false
        this.input.auto_resize = true

        this.input.value_set_callback = { value: Double? ->
            if (value != null) {
                (this.context as ActivityEditor).get_action_interface().set_tempo_at_cursor(value)
            }
        }

    }

    init {
        this.orientation = HORIZONTAL
    }

    override fun on_set(event: OpusTempoEvent) {
        val value = event.value
        this.input.set_value((value * 1000F).roundToInt().toFloat() / 1000F)
    }
}
