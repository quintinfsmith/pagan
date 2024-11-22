package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.OpusTempoEvent
import kotlin.math.roundToInt

class ControlWidgetTempo(default: OpusTempoEvent, is_initial_event: Boolean, context: Context, callback: (OpusTempoEvent) -> Unit): ControlWidget<OpusTempoEvent>(context, default, is_initial_event, R.layout.control_widget_tempo, callback) {
    private lateinit var input: ButtonLabelledIcon
    private val min = 0f
    private val max = 512f


    override fun on_inflated() {
        this.input = this.inner.findViewById(R.id.tempo_value_button)
        this.input.set_icon(R.drawable.tempo_widget_icon)
        this.input.set_text("${this.working_event.value} BPM")
        this.input.setOnClickListener {
            val event = this.get_event()
            (this.context as MainActivity).dialog_float_input(context.getString(R.string.dlg_set_tempo), this.min, this.max, event.value) { new_value: Float ->
                val new_event = OpusTempoEvent((new_value * 1000F).roundToInt().toFloat() / 1000F)
                this.set_event(new_event)
            }
        }
    }
    init {
        this.orientation = HORIZONTAL
    }

    override fun on_set(event: OpusTempoEvent) {
        val value = event.value

        this.input.set_text(
            if (value.toInt().toFloat() == value) {
                "${value.toInt()} BPM"
            } else {
                "${(value * 1000F).roundToInt().toFloat() / 1000F} BPM"
            }
        )
    }
}
