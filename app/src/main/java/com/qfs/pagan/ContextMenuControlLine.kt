package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isEmpty
import com.qfs.pagan.opusmanager.ActiveControlSet
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.OpusVolumeEvent

class ContextMenuControlLine(context: Context, attrs: AttributeSet? = null): ContextMenuView(R.layout.contextmenu_control_line, context, attrs) {
    lateinit var initial_widget_wrapper: LinearLayout
    lateinit var widget: ControlWidget

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
        this.initial_widget_wrapper.removeAllViews()

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val controller = this.get_controller()

        this.widget = when (cursor.ctl_type!!) {
            ControlEventType.Tempo -> ControlWidgetTempo(controller.initial_event as OpusTempoEvent, this.context, this::_callback)
            ControlEventType.Volume -> ControlWidgetVolume(controller.initial_event as OpusVolumeEvent, this.context, this::_callback)
            else -> TODO()
            //ControlEventType.Reverb -> ControlWidgetReverb(this.context, this::_callback)
        }

        this._current_type = cursor.ctl_type

        this.initial_widget_wrapper.addView(this.widget as View)
        (this.widget as View).layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        (this.widget as View).layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    private fun get_controller(): ActiveControlSet.ActiveController {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val control_set = when (cursor.ctl_level!!) {
            CtlLineLevel.Line -> opus_manager.channels[cursor.channel].lines[cursor.line_offset].controllers
            CtlLineLevel.Channel -> opus_manager.channels[cursor.channel].controllers
            CtlLineLevel.Global -> opus_manager.controllers
        }

        return control_set.get_controller(cursor.ctl_type!!)
    }

    override fun init_properties() {
        super.init_properties()
        this.initial_widget_wrapper = this.findViewById(R.id.llTarget)
        this.init_widget()
    }

    override fun refresh() {
        super.refresh()

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (this.initial_widget_wrapper.isEmpty() || cursor.ctl_type != this._current_type) {
            this.init_widget()
        } else {
            val controller = this.get_controller()
            this.widget.set_event(
                when(this._current_type) {
                    ControlEventType.Tempo -> controller.initial_event as OpusTempoEvent
                    ControlEventType.Volume -> TODO()
                    ControlEventType.Reverb -> TODO()
                    null -> TODO()
                }
            )
        }
    }

    override fun setup_interactions() {
        super.setup_interactions()
    }
}
