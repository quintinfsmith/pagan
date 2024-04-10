package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT

class ControlWidgetTempo(default: Float, context: Context, callback: (Float) -> Unit): ControlWidget(context, callback) {
    private val input = ButtonStd(ContextThemeWrapper(context, R.style.icon_button), null)
    private val min = 1
    private val max = 512
    private var current_value = default

    init {
        this.orientation = HORIZONTAL

        this.input.text = "$default BPM"
        this.input.setOnClickListener {
            this.input.get_main().dialog_number_input(context.getString(R.string.dlg_set_tempo), this.min, this.max, this.get_value().toInt()) { value: Int ->
                this.set_value(value.toFloat())
                this.callback(value.toFloat())
            }
        }

        this.addView(this.input)

        this.input.layoutParams.width = MATCH_PARENT
        this.input.layoutParams.height = MATCH_PARENT
    }

    override fun get_value(): Float {
        return this.current_value
    }

    override fun set_value(new_value: Float) {
        this.current_value = new_value
        this.input.text = "$new_value BPM"
    }
}
