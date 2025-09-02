package com.qfs.pagan.controlwidgets

import android.content.Context
import android.widget.Button
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.R
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import kotlin.math.roundToInt

class ControlWidgetTempo(_level: CtlLineLevel, is_initial_event: Boolean, context: Context, callback: (OpusTempoEvent) -> Unit): ControlWidget<OpusTempoEvent>(context, CtlLineLevel.Global, is_initial_event, R.layout.control_widget_tempo, callback) {
    private lateinit var input: Button
    val min = 0f
    val max = 1024f


    override fun on_inflated() {
        this.input = this.inner.findViewById(R.id.tempo_value_button)
        this.input.setOnClickListener {
            (this.context as ActivityEditor).get_action_interface().set_tempo_at_cursor()
        }
    }

    init {
        this.orientation = HORIZONTAL
    }

    override fun on_set(event: OpusTempoEvent) {
        val value = event.value

        this.input.text = if (value.toInt().toFloat() == value) {
            "${value.toInt()} BPM"
        } else {
            "${(value * 1000F).roundToInt().toFloat() / 1000F} BPM"
        }
    }
}
