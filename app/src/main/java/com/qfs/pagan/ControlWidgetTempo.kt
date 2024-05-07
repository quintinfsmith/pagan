package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusTempoEvent
import kotlin.math.roundToInt

class ControlWidgetTempo(default: OpusTempoEvent, context: Context, callback: (OpusControlEvent) -> Unit): ControlWidget(context, callback) {
    private val input = ButtonStd(ContextThemeWrapper(context, R.style.icon_button), null)
    private val min = 0f
    private val max = 512f
    private var current_event: OpusTempoEvent = default

    init {
        this.orientation = HORIZONTAL

        this.input.text = "$default BPM"
        this.input.setOnClickListener {
            this.input.get_main().dialog_float_input(context.getString(R.string.dlg_set_tempo), this.min, this.max, this.get_event().value) { new_value: Float ->
                val new_event = OpusTempoEvent((new_value * 1000F).roundToInt().toFloat() / 1000F)
                this.set_event(new_event)
                this.callback(new_event)
            }
        }

        this.addView(this.input)

        this.input.layoutParams.width = MATCH_PARENT
        this.input.layoutParams.height = MATCH_PARENT
    }

    override fun get_event(): OpusTempoEvent {
        return this.current_event
    }

    override fun set_event(event: OpusControlEvent) {
        this.current_event = event as OpusTempoEvent

        val value = event.value

        this.input.text = if (value.toInt().toFloat() == value) {
            "${value.toInt()} BPM"
        } else {
            "${(value * 1000F).roundToInt().toFloat() / 1000F} BPM"
        }

    }
}
