package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isEmpty
import com.qfs.pagan.opusmanager.ActiveControlSet
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.Transition

class ContextMenuControlLeaf(context: Context, attrs: AttributeSet? = null): ContextMenuView(R.layout.contextmenu_line_ctl_leaf, context, attrs) {
    lateinit var button_duration: ButtonStd
    lateinit var widget_wrapper: LinearLayout
    lateinit var widget: ControlWidget
    // --------------------------------
    lateinit var button_split: ButtonIcon
    lateinit var button_insert: ButtonIcon
    lateinit var button_remove: ButtonIcon
    lateinit var button_unset: ButtonIcon

    private var _current_type: ControlEventType? = null

    override fun init_properties() {
        this.button_duration = this.findViewById(R.id.btnDuration)
        this.widget_wrapper = this.findViewById(R.id.llCtlTarget)
        this.button_split = this.findViewById(R.id.btnSplit)
        this.button_insert = this.findViewById(R.id.btnInsert)
        this.button_remove = this.findViewById(R.id.btnRemove)
        this.button_unset = this.findViewById(R.id.btnUnset)
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

    private fun _widget_callback(value: Float) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val current_tree = when (cursor.ctl_level!!) {
            CtlLineLevel.Line -> opus_manager.get_line_ctl_tree(cursor.ctl_type!!, cursor.get_beatkey(), cursor.get_position())
            CtlLineLevel.Channel -> opus_manager.get_channel_ctl_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.get_position())
            CtlLineLevel.Global -> opus_manager.get_global_ctl_tree(cursor.ctl_type!!, cursor.beat, cursor.get_position())
        }

        val current_transition = current_tree.get_event()?.transition ?: Transition.Linear
        val current_duration = current_tree.get_event()?.duration ?: 0
        val new_event = OpusControlEvent(
            value,
            transition = current_transition,
            duration = current_duration
        )

        opus_manager.set_event_at_cursor(new_event)
    }

    fun init_widget() {
        this.widget_wrapper.removeAllViews()

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val controller = this.get_controller()

        this.widget = when (cursor.ctl_type!!) {
            ControlEventType.Tempo -> ControlWidgetTempo(controller.initial_value, this.context, this::_widget_callback)
            ControlEventType.Volume -> ControlWidgetVolume(controller.initial_value, this.context, this::_widget_callback)
            ControlEventType.Reverb -> ControlWidgetReverb(this.context, this::_widget_callback)
        }

        this._current_type = cursor.ctl_type

        this.widget_wrapper.addView(this.widget as View)
        (this.widget as View).layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        (this.widget as View).layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    override fun setup_interactions() {
        this.button_split.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_split()
        }
        this.button_split.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_split()
        }

        this.button_insert.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_insert()
        }
        this.button_insert.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_insert()
        }

        this.button_remove.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_remove()
        }
        this.button_remove.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }

            this.long_click_button_remove()
        }

        this.button_unset.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }

            this.click_button_unset()
        }
    }

    fun click_button_unset() {
        val opus_manager = this.get_opus_manager()
        opus_manager.unset()
    }
    fun click_button_remove() {
        val opus_manager = this.get_opus_manager()
        opus_manager.remove(1)
    }

    fun long_click_button_remove(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val cursor = opus_manager.cursor
        val position = cursor.get_position().toMutableList()
        if (position.isNotEmpty()) {
            position.removeLast()
        }

        when (cursor.ctl_level) {
            CtlLineLevel.Global -> {
                opus_manager.cursor_select_ctl_at_global(cursor.ctl_type!!, cursor.beat, position)
            }
            CtlLineLevel.Channel -> {
                opus_manager.cursor_select_ctl_at_channel(cursor.ctl_type!!, cursor.channel, cursor.beat, position)
            }
            CtlLineLevel.Line -> {
                val beat_key = cursor.get_beatkey()
                opus_manager.cursor_select_ctl_at_line(cursor.ctl_type!!, beat_key, position)
            }
            null -> { }
        }
        opus_manager.unset()
        return false
    }

    fun click_button_insert() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val position = opus_manager.cursor.get_position().toMutableList()
        if (position.isEmpty()) {
            when (cursor.ctl_level) {
                CtlLineLevel.Global -> opus_manager.split_global_ctl_tree(cursor.ctl_type!!, cursor.beat, cursor.position, 2)
                CtlLineLevel.Channel -> opus_manager.split_channel_ctl_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position, 2)
                CtlLineLevel.Line -> opus_manager.split_line_ctl_tree(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position, 2)
                null -> { }
            }
        } else {
            when (cursor.ctl_level) {
                CtlLineLevel.Global -> opus_manager.insert_after_global_ctl(cursor.ctl_type!!, cursor.beat, cursor.position)
                CtlLineLevel.Channel -> opus_manager.insert_after_channel_ctl(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position)
                CtlLineLevel.Line -> opus_manager.insert_after_line_ctl(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position)
                null -> { }
            }
        }
    }

    fun long_click_button_insert(): Boolean {
        val main = this.get_main()
        main.dialog_number_input(main.getString(R.string.dlg_insert), 2, 32, 2) { insert_count: Int ->
            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor
            val position = opus_manager.cursor.get_position().toMutableList()
            if (position.isNotEmpty()) {
                when (cursor.ctl_level) {
                    CtlLineLevel.Global -> opus_manager.insert_after_global_ctl(cursor.ctl_type!!, cursor.beat, cursor.position, insert_count)
                    CtlLineLevel.Channel -> opus_manager.insert_after_channel_ctl(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position, insert_count)
                    CtlLineLevel.Line -> opus_manager.insert_after_line_ctl(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position, insert_count)
                    null -> {}
                }
            } else {
                when (cursor.ctl_level) {
                    CtlLineLevel.Global -> opus_manager.split_global_ctl_tree(cursor.ctl_type!!, cursor.beat, cursor.position, insert_count)
                    CtlLineLevel.Channel -> opus_manager.split_channel_ctl_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position, insert_count)
                    CtlLineLevel.Line -> opus_manager.split_line_ctl_tree(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position, insert_count)
                    null -> {}
                }
            }
        }

        return false
    }

    fun click_button_split() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Global -> opus_manager.split_global_ctl_tree(cursor.ctl_type!!, cursor.beat, cursor.position, 2)
            CtlLineLevel.Channel -> opus_manager.split_channel_ctl_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position, 2)
            CtlLineLevel.Line -> opus_manager.split_line_ctl_tree(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position, 2)
            null -> { }
        }
    }

    fun long_click_button_split(): Boolean {
        val main = this.get_main()
        main.dialog_number_input(main.getString(R.string.dlg_split), 2, 32, 2) { split_count: Int ->
            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor
            when (cursor.ctl_level) {
                CtlLineLevel.Global -> opus_manager.split_global_ctl_tree(cursor.ctl_type!!, cursor.beat, cursor.position, split_count)
                CtlLineLevel.Channel -> opus_manager.split_channel_ctl_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position, split_count)
                CtlLineLevel.Line -> opus_manager.split_line_ctl_tree(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position, split_count)
                null -> { }
            }
        }
        return false
    }

    override fun refresh() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (this.widget_wrapper.isEmpty() || cursor.ctl_type != this._current_type) {
            this.init_widget()
        }

        val ctl_tree = when (cursor.ctl_level!!) {
            CtlLineLevel.Global -> {
                opus_manager.get_global_ctl_tree(
                    cursor.ctl_type!!,
                    cursor.beat,
                    cursor.position
                )
            }
            CtlLineLevel.Channel -> {
                opus_manager.get_channel_ctl_tree(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.beat,
                    cursor.position
                )
            }
            CtlLineLevel.Line -> {
                val beat_key = BeatKey(
                    cursor.channel,
                    cursor.line_offset,
                    cursor.beat
                )

                opus_manager.get_line_ctl_tree(
                    cursor.ctl_type!!,
                    beat_key,
                    cursor.position
                )
            }
        }

        this.button_remove.isEnabled = cursor.position.isNotEmpty()
        this.button_unset.isEnabled = ctl_tree.is_event()
        this.widget.set_value(
            if (!ctl_tree.is_event()) {
                when (cursor.ctl_level!!) {
                    CtlLineLevel.Global -> opus_manager.get_current_global_controller_value(
                        cursor.ctl_type!!,
                        cursor.beat,
                        cursor.position
                    )
                    CtlLineLevel.Channel ->
                        opus_manager.get_current_channel_controller_value(
                            cursor.ctl_type!!,
                            cursor.channel,
                            cursor.beat,
                            cursor.position
                        )
                    CtlLineLevel.Line -> {
                        val beat_key = cursor.get_beatkey()
                        opus_manager.get_current_line_controller_value(
                            cursor.ctl_type!!,
                            beat_key,
                            cursor.position
                        )
                    }
                }
            } else {
                ctl_tree.event!!.value
            }
        )

        this.button_duration.text = this.get_main().getString(
            R.string.label_duration,
            if (!ctl_tree.is_event() || ctl_tree.get_event()!!.duration == 0) {
                0
            } else {
                ctl_tree.get_event()!!.duration
            }
        )
    }

    fun click_button_ctl_value() {
        // TODO: Allow floats in dialog_number_input
        val main = this.get_main()
        main.dialog_number_input("Value", 0, 128, 0) { new_value: Int ->
            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor
            val tree = when (cursor.ctl_level!!) {
                CtlLineLevel.Global -> opus_manager.get_global_ctl_tree(cursor.ctl_type!!, cursor.beat, cursor.position)
                CtlLineLevel.Channel -> opus_manager.get_channel_ctl_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position)
                CtlLineLevel.Line -> opus_manager.get_line_ctl_tree(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position)
            }

            val event = tree.get_event()?.copy() ?: OpusControlEvent(1F)

            event.value = new_value.toFloat()
            opus_manager.set_event_at_cursor(event)
        }
    }

    fun click_button_duration() {
        val main = this.get_main()
        main.dialog_number_input(main.getString(R.string.dlg_duration), 0, 1024, 0) { duration: Int ->
            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor
            when (cursor.ctl_level) {
                CtlLineLevel.Global -> opus_manager.set_global_ctl_duration(cursor.ctl_type!!, cursor.beat, cursor.position, duration)
                CtlLineLevel.Channel -> opus_manager.set_channel_ctl_duration(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position, duration)
                CtlLineLevel.Line -> opus_manager.set_line_ctl_duration(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position, duration)
                null -> { }
            }
        }
    }
    fun long_click_button_duration(): Boolean {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Global -> opus_manager.set_global_ctl_duration(cursor.ctl_type!!, cursor.beat, cursor.position, 0)
            CtlLineLevel.Channel -> opus_manager.set_channel_ctl_duration(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position, 0)
            CtlLineLevel.Line -> opus_manager.set_line_ctl_duration(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position, 0)
            null -> { }
        }
        return false
    }
}