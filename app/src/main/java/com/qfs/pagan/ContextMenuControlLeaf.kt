package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent

class ContextMenuControlLeaf(context: Context, attrs: AttributeSet? = null): ContextMenuView(R.layout.contextmenu_line_ctl_leaf, context, attrs) {
    lateinit var button_duration: ButtonStd
    lateinit var button_value: ButtonStd
    // --------------------------------
    lateinit var button_split: ButtonIcon
    lateinit var button_insert: ButtonIcon
    lateinit var button_remove: ButtonIcon
    lateinit var button_unset: ButtonIcon

    override fun init_properties() {
        this.button_duration = this.findViewById(R.id.btnDuration)
        this.button_value = this.findViewById(R.id.btnCtlAmount)
        this.button_split = this.findViewById(R.id.btnSplit)
        this.button_insert = this.findViewById(R.id.btnInsert)
        this.button_remove = this.findViewById(R.id.btnRemove)
        this.button_unset = this.findViewById(R.id.btnUnset)
    }

    override fun setup_interactions() {
        this.button_value.setOnClickListener {
            this.click_button_ctl_value()
        }

        this.button_split.setOnClickListener {
            this.click_button_split()
        }
        this.button_split.setOnLongClickListener {
            this.long_click_button_split()
            true
        }

        this.button_insert.setOnClickListener {
            this.click_button_insert()
        }
        this.button_insert.setOnLongClickListener {
            this.long_click_button_insert()
            true
        }

        this.button_remove.setOnClickListener {
            this.click_button_remove()
        }
        this.button_remove.setOnLongClickListener {
            this.long_click_button_remove()
            true
        }

        this.button_unset.setOnClickListener {
            this.click_button_unset()
        }
    }

    fun click_button_unset() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Global -> opus_manager.unset_global_ctl(cursor.ctl_type!!, cursor.beat, cursor.position)
            CtlLineLevel.Channel -> opus_manager.unset_channel_ctl(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position)
            CtlLineLevel.Line -> opus_manager.unset_line_ctl(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position)
            null -> { }
        }
    }
    fun click_button_remove() {
        val opus_manager = this.get_opus_manager()
        opus_manager.remove(1)
    }

    fun long_click_button_remove() {
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
                opus_manager.unset_global_ctl(cursor.ctl_type!!, cursor.beat, position)
            }
            CtlLineLevel.Channel -> {
                opus_manager.cursor_select_ctl_at_channel(cursor.ctl_type!!, cursor.channel, cursor.beat, position)
                opus_manager.unset_channel_ctl(cursor.ctl_type!!, cursor.beat, cursor.channel, position)
            }
            CtlLineLevel.Line -> {
                val beat_key = cursor.get_beatkey()
                opus_manager.cursor_select_ctl_at_line(cursor.ctl_type!!, beat_key, position)
                opus_manager.unset_line_ctl(cursor.ctl_type!!, beat_key, position)
            }
            null -> { }
        }
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

    fun long_click_button_insert() {
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

    fun long_click_button_split() {
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
    }

    override fun refresh() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val beat_key = BeatKey(
            cursor.channel,
            cursor.line_offset,
            cursor.beat
        )

        val ctl_tree = opus_manager.get_line_ctl_tree(
            cursor.ctl_type!!,
            beat_key,
            cursor.position
        )

        this.button_remove.visibility = if (cursor.position.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        this.button_unset.visibility = if (ctl_tree.is_event()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        this.button_value.text = if (!ctl_tree.is_event()) {
            opus_manager.get_current_ctl_line_value(
                cursor.ctl_type!!,
                beat_key,
                cursor.position
            ).toString()
        } else {
            // TODO: Formatting
            ctl_tree.event!!.value.toString()
        }

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

            when (cursor.ctl_level) {
                null -> {}
                CtlLineLevel.Global -> {
                    val tree = opus_manager.get_global_ctl_tree(cursor.ctl_type!!, cursor.beat, cursor.position)
                    val event = (tree.get_event() ?: OpusControlEvent(1f)).copy()
                    event.value = new_value.toFloat()
                    opus_manager.set_global_ctl_event(cursor.ctl_type!!, cursor.beat, cursor.position, event)
                }
                CtlLineLevel.Channel -> {
                    val tree = opus_manager.get_channel_ctl_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position)
                    val event = (tree.get_event() ?: OpusControlEvent(1f)).copy()
                    event.value = new_value.toFloat()
                    opus_manager.set_channel_ctl_event(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position, event)
                }
                CtlLineLevel.Line -> {
                    val tree = opus_manager.get_line_ctl_tree(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position)
                    val event = (tree.get_event() ?: OpusControlEvent(1f)).copy()
                    event.value = new_value.toFloat()
                    opus_manager.set_line_ctl_event(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position, event)
                }

            }
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
    fun long_click_button_duration() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Global -> opus_manager.set_global_ctl_duration(cursor.ctl_type!!, cursor.beat, cursor.position, 0)
            CtlLineLevel.Channel -> opus_manager.set_channel_ctl_duration(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.position, 0)
            CtlLineLevel.Line -> opus_manager.set_line_ctl_duration(cursor.ctl_type!!, cursor.get_beatkey(), cursor.position, 0)
            null -> { }
        }
    }
}