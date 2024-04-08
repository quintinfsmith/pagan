package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import kotlin.math.roundToInt

open class ContextMenuControlLine(context: Context, attrs: AttributeSet? = null): ContextMenuView(R.layout.contextmenu_control_line, context, attrs) {
    lateinit var initial_widget_wrapper: LinearLayout
    lateinit var label: PaganTextView
    lateinit var widget: ControlWidget

    open fun init_widget(): ControlWidget {
        TODO()
    }

    override fun init_properties() {
        super.init_properties()
        this.initial_widget_wrapper = this.findViewById(R.id.llTarget)
        this.label = this.findViewById(R.id.tvCtlLineLabel)

        this.widget = this.init_widget()
        this.initial_widget_wrapper.addView(this.widget as View)
        (this.widget as View).layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        (this.widget as View).layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    override fun refresh() {
        super.refresh()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val control_set = when (cursor.ctl_level!!) {
            CtlLineLevel.Line -> opus_manager.channels[cursor.channel].lines[cursor.line_offset].controllers
            CtlLineLevel.Channel -> opus_manager.channels[cursor.channel].controllers
            CtlLineLevel.Global -> opus_manager.controllers
        }

        val controller = control_set.get_controller(cursor.ctl_type!!)

        val (label, value_fmt, unit) = when (cursor.ctl_type!!) {
            ControlEventType.Tempo -> Triple("Tempo", "${controller.initial_value}", "bpm")
            ControlEventType.Volume -> Triple("Volume", "${(controller.initial_value * 1000f / 128f).roundToInt().toFloat() / 1000F}", "")
            ControlEventType.Reverb -> Triple("Reverb", "${controller.initial_value}", "%")
        }

        this.label.text = "Initial $label"
    }

    override fun setup_interactions() {
        super.setup_interactions()
    }
}
