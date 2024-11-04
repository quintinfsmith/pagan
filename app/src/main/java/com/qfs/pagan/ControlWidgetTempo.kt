package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusTempoEvent
import kotlin.math.roundToInt

class ControlWidgetTempo(default: OpusTempoEvent, is_initial_event: Boolean, context: Context, callback: (OpusTempoEvent) -> Unit): ControlWidget<OpusTempoEvent>(context, default, is_initial_event, callback) {
    private val input = ButtonStd(ContextThemeWrapper(context, R.style.icon_button), null)
    private val min = 0f
    private val max = 512f

    init {
        this.orientation = HORIZONTAL

        this.input.text = "$default BPM"
        this.input.setOnClickListener {
            val event = this.get_event()
            this.input.get_main().dialog_float_input(context.getString(R.string.dlg_set_tempo), this.min, this.max, event.value) { new_value: Float ->
                val new_event = OpusTempoEvent((new_value * 1000F).roundToInt().toFloat() / 1000F)
                this.set_event(new_event)
                this.callback(new_event)
            }
        }

        this.addView(this.input)

        this.input.layoutParams.width = MATCH_PARENT
        this.input.layoutParams.height = MATCH_PARENT
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
