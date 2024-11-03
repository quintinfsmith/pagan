package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isEmpty
import com.qfs.pagan.opusmanager.ActiveController
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusReverbEvent
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.OpusVolumeEvent

class ContextMenuControlLine(primary_parent: ViewGroup, secondary_parent: ViewGroup): ContextMenuView(R.layout.contextmenu_control_line, R.layout.contextmenu_control_line_secondary, primary_parent, secondary_parent) {
    lateinit var widget: ControlWidget
    lateinit var button_toggle_line_control: ButtonIcon

    private var _current_type: ControlEventType? = null

    private fun _callback(value: OpusControlEvent) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                opus_manager.set_line_controller_initial_event(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.line_offset,
                    value
                )
            }

            CtlLineLevel.Channel -> {
                opus_manager.set_channel_controller_initial_event(
                    cursor.ctl_type!!,
                    cursor.channel,
                    value
                )
            }

            CtlLineLevel.Global -> {
                opus_manager.set_global_controller_initial_event(
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

        val controller = this.get_controller()

        this.widget = when (cursor.ctl_type!!) {
            ControlEventType.Tempo -> ControlWidgetTempo(controller.initial_event as OpusTempoEvent, this.context, this::_callback)
            ControlEventType.Volume -> ControlWidgetVolume(controller.initial_event as OpusVolumeEvent, this.context, this::_callback)
            ControlEventType.Reverb -> ControlWidgetReverb(controller.initial_event as OpusReverbEvent, this.context, this::_callback)
        }

        this._current_type = cursor.ctl_type

        this.button_toggle_line_control = this.primary!!.findViewById(R.id.btnToggleCtl)
        this.button_toggle_line_control.setImageResource(R.drawable.volume_minus)
        this.button_toggle_line_control.setOnClickListener {
            val opus_manager = this.get_opus_manager()
            opus_manager.toggle_control_line_visibility(cursor.ctl_level!!, cursor.ctl_type!!)
        }

        this.secondary!!.addView(this.widget as View)
        (this.widget as View).layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        (this.widget as View).layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    private fun get_controller(): ActiveController {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val channels = opus_manager.get_all_channels()

        val control_set = when (cursor.ctl_level!!) {
            CtlLineLevel.Line -> channels[cursor.channel].lines[cursor.line_offset].controllers
            CtlLineLevel.Channel -> channels[cursor.channel].controllers
            CtlLineLevel.Global -> opus_manager.controllers
        }

        return control_set.get_controller(cursor.ctl_type!!)
    }

    override fun init_properties() {
        this.init_widget()
    }

    override fun setup_interactions() { }

    override fun refresh() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (this.secondary!!.isEmpty() || cursor.ctl_type != this._current_type) {
            this.init_widget()
        } else {
            val controller = this.get_controller()
            this.widget.set_event(
                when(this._current_type!!) {
                    ControlEventType.Tempo -> controller.initial_event as OpusTempoEvent
                    ControlEventType.Volume -> controller.initial_event as OpusVolumeEvent
                    ControlEventType.Reverb -> controller.initial_event as OpusReverbEvent
                }
            )
        }
    }
}
