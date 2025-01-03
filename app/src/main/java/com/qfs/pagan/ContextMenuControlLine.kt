package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent

class ContextMenuControlLine<T: OpusControlEvent>(val widget: ControlWidget<T>, primary_parent: ViewGroup, secondary_parent: ViewGroup): ContextMenuView(R.layout.contextmenu_control_line, R.layout.contextmenu_control_line_secondary, primary_parent, secondary_parent) {
    lateinit var button_toggle_line_control: ImageView
    lateinit var button_remove_line_control: ImageView
    init {

        this.init_widget()
        this.refresh()
    }


    private fun _callback(value: OpusControlEvent) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                opus_manager.controller_line_set_initial_event(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.line_offset,
                    value
                )
            }

            CtlLineLevel.Channel -> {
                opus_manager.controller_channel_set_initial_event(
                    cursor.ctl_type!!,
                    cursor.channel,
                    value
                )
            }

            CtlLineLevel.Global -> {
                opus_manager.controller_global_set_initial_event(
                    cursor.ctl_type!!,
                    value
                )
            }

            null -> {}
        }
    }

    fun init_widget() {
        this.secondary!!.removeAllViews()

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        this.button_toggle_line_control = this.primary!!.findViewById(R.id.btnToggleCtl)
        this.button_remove_line_control = this.primary!!.findViewById(R.id.btnRemoveCtl)

        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                this.button_toggle_line_control.visibility = View.VISIBLE
                this.button_toggle_line_control.setOnClickListener {
                    opus_manager.toggle_line_controller_visibility(
                        cursor.ctl_type!!,
                        cursor.channel,
                        cursor.line_offset
                    )
                }
                this.button_remove_line_control.setOnClickListener {
                    opus_manager.remove_line_controller(
                        cursor.ctl_type!!,
                        cursor.channel,
                        cursor.line_offset
                    )
                }
            }
            CtlLineLevel.Channel -> {
                this.button_toggle_line_control.visibility = View.VISIBLE
                this.button_toggle_line_control.setOnClickListener {
                    opus_manager.toggle_channel_controller_visibility(
                        cursor.ctl_type!!,
                        cursor.channel
                    )
                }
                this.button_remove_line_control.setOnClickListener {
                    opus_manager.remove_channel_controller(
                        cursor.ctl_type!!,
                        cursor.channel
                    )
                }
            }
            CtlLineLevel.Global,
            null -> {
                this.button_toggle_line_control.visibility = View.GONE
                this.button_remove_line_control.visibility = View.GONE
            }
        }

        this.secondary!!.addView(this.widget as View)
        (this.widget as View).layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        (this.widget as View).layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    fun <T: OpusControlEvent> get_control_event(): T {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        return when (cursor.ctl_level!!) {
            CtlLineLevel.Global -> opus_manager.get_global_controller_initial_event(cursor.ctl_type!!)
            CtlLineLevel.Channel -> opus_manager.get_channel_controller_initial_event(cursor.ctl_type!!, cursor.channel)
            CtlLineLevel.Line -> opus_manager.get_line_controller_initial_event(cursor.ctl_type!!, cursor.channel, cursor.line_offset)
        }
    }

    override fun init_properties() {
    }

    override fun setup_interactions() { }

    override fun refresh() {
        this.widget.set_event(this.get_control_event(), true)
    }
}
