package com.qfs.pagan

import android.content.Context
import android.widget.Button
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusTempoEvent
import kotlin.math.roundToInt

class ControlWidgetTempo(default: OpusTempoEvent, _level: CtlLineLevel, is_initial_event: Boolean, context: Context, callback: (OpusTempoEvent) -> Unit): ControlWidget<OpusTempoEvent>(context, default, CtlLineLevel.Global, is_initial_event, R.layout.control_widget_tempo, callback) {
    private lateinit var input: Button
    val min = 0f
    val max = 512f


    override fun on_inflated() {
        this.input = this.inner.findViewById(R.id.tempo_value_button)
        (this.input as MaterialButton).setIconResource(R.drawable.tempo_widget_icon)
        this.input.text = "${this.working_event.value} BPM"
        this.input.setOnClickListener {
            (this.context as MainActivity).get_action_interface().set_tempo_at_cursor()
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
