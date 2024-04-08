package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

class ContextMenuControlLine(context: Context, attrs: AttributeSet? = null): ContextMenuView(R.layout.contextmenu_control_line, context, attrs) {
    lateinit var initial_widget_wrapper: LinearLayout
    lateinit var label: PaganTextView

    override fun init_properties() {
        super.init_properties()
        this.initial_widget_wrapper = this.findViewById(R.id.llTarget)
        this.label = this.findViewById(R.id.tvCtlLineLabel)
    }

    override fun refresh() {
        super.refresh()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
       // val control_set = when (cursor.ctl_level!!) {
       //     CtlLineLevel.Line -> opus_manager.channels[cursor.channel].lines[cursor.line_offset].controllers
       //     CtlLineLevel.Channel -> opus_manager.channels[cursor.channel].controllers
       //     CtlLineLevel.Global -> opus_manager.controllers
       // }

        this.label.text = "Initial ${cursor.ctl_type}"
    }

    override fun setup_interactions() {
        super.setup_interactions()
    }
}