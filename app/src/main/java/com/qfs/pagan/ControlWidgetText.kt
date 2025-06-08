package com.qfs.pagan

import android.content.Context
import android.widget.Button
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.OpusTextEvent
import kotlin.math.roundToInt

class ControlWidgetText(default: OpusTextEvent, level: CtlLineLevel, is_initial_event: Boolean, context: Context, callback: (OpusTextEvent) -> Unit): ControlWidget<OpusTextEvent>(context, default, level, is_initial_event, R.layout.control_widget_text, callback) {
    private lateinit var input: TextInputEditText
    override fun on_inflated() {
        this.input = this.inner.findViewById(R.id.etText)
        this.input.setText(this.working_event.value)
    }

    init {
        this.orientation = HORIZONTAL
    }

    override fun on_set(event: OpusTextEvent) {
        val value = event.value
        this.input.setText(value)
    }
}
