package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEventSTD
import kotlin.math.abs

class ContextMenuLeaf(context: Context, attrs: AttributeSet? = null): ContextMenuView(R.layout.contextmenu_cell, context, attrs) {
    lateinit var button_split: ButtonIcon
    lateinit var button_insert: ButtonIcon
    lateinit var button_unset: ButtonIcon
    lateinit var button_remove: ButtonIcon
    lateinit var button_duration: ButtonStd
    lateinit var ns_octave: NumberSelector
    lateinit var ns_offset: NumberSelector
    lateinit var ros_relative_option: RelativeOptionSelector

    override fun init_properties() {
        this.button_split = this.findViewById(R.id.btnSplit)
        this.button_insert = this.findViewById(R.id.btnInsert)
        this.button_unset = this.findViewById(R.id.btnUnset)
        this.button_remove = this.findViewById(R.id.btnRemove)
        this.button_duration = this.findViewById(R.id.btnDuration)
        this.ns_octave = this.findViewById(R.id.nsOctave)
        this.ns_offset = this.findViewById(R.id.nsOffset)
        this.ros_relative_option = this.findViewById(R.id.rosRelativeOption)
    }

    override fun setup_interactions() {
        this.ns_octave.setOnChange(this::on_octave_change)
        this.ns_offset.setOnChange(this::on_offset_change)

        this.ros_relative_option.setOnChange(this::interact_rosRelativeOption)

        this.button_duration.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }

            this.click_button_duration()
        }
        this.button_duration.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_duration()
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
    }

    override fun refresh() {
        val main = this.get_main()
        val opus_manager = this.get_opus_manager()

        val radix = opus_manager.tuning_map.size
        this.ns_offset.set_max(radix - 1)

        if (main.configuration.relative_mode) {
            this.ros_relative_option.visibility = View.VISIBLE
            this.ros_relative_option.setState(opus_manager.relative_mode, true)
        } else {
            this.ros_relative_option.visibility = View.GONE
        }

        val current_tree = opus_manager.get_tree()
        if (current_tree.is_event()) {
            val event = current_tree.get_event()!!

            val value = if (event.relative && ! main.configuration.relative_mode) {
                opus_manager.get_absolute_value(
                    opus_manager.cursor.get_beatkey(),
                    opus_manager.cursor.get_position()
                )!!
            } else {
                abs(event.note)
            }

            if (value >= 0) {
                this.ns_offset.setState(value % radix, manual = true, surpress_callback = true)
                this.ns_octave.setState(value / radix, manual = true, surpress_callback = true)
            }

            this.button_unset.setImageResource(R.drawable.unset)
            this.button_duration.text = this.context.getString(R.string.label_duration, event.duration)
        } else {
            this.ns_octave.unset_active_button()
            this.ns_offset.unset_active_button()
            this.button_duration.text = ""
        }

        this.button_duration.isEnabled = current_tree.is_event()
        this.button_duration.isClickable = this.button_duration.isEnabled

        this.button_unset.isEnabled = !(current_tree.is_leaf() && !current_tree.is_event())
        this.button_unset.isClickable = this.button_unset.isEnabled

        this.button_remove.isEnabled = opus_manager.cursor.get_position().isNotEmpty()
        this.button_remove.isClickable = this.button_remove.isEnabled
    }

    fun click_button_duration() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val cursor = opus_manager.cursor
        val beat_key = cursor.get_beatkey()
        val position = cursor.get_position()
        val event_duration = opus_manager.get_tree().get_event()?.duration ?: return

        main.dialog_number_input(this.context.getString(R.string.dlg_duration), 1, 99, default=event_duration) { value: Int ->
            val adj_value = Integer.max(value, 1)
            opus_manager.set_duration(beat_key, position, adj_value)
        }
    }

    fun click_button_split() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.split_tree(2)
    }

    fun click_button_insert() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val position = opus_manager.cursor.get_position().toMutableList()
        if (position.isEmpty()) {
            opus_manager.split_tree(2)
        } else {
            opus_manager.insert_after(1)
        }
    }

    fun click_button_remove() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.remove(1)
    }

    fun long_click_button_split(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        main.dialog_number_input(this.context.getString(R.string.dlg_split), 2, 32) { splits: Int ->
            opus_manager.split_tree(splits)
        }
        return true
    }

    fun long_click_button_insert(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        main.dialog_number_input(this.context.getString(R.string.dlg_insert), 1, 29) { count: Int ->
            val position = opus_manager.cursor.get_position().toMutableList()
            if (position.isEmpty()) {
                opus_manager.split_tree(count + 1)
            } else {
                opus_manager.insert_after(count)
            }
        }
        return true
    }

    fun long_click_button_duration(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val cursor = opus_manager.cursor
        val beat_key = cursor.get_beatkey()
        val position = cursor.get_position()

        opus_manager.set_duration(beat_key, position, 1)
        return true
    }

    fun long_click_button_remove(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val position = opus_manager.cursor.get_position().toMutableList()
        if (position.isNotEmpty()) {
            position.removeLast()
        }

        opus_manager.cursor_select(
            opus_manager.cursor.get_beatkey(),
            position
        )

        opus_manager.unset()

        return true
    }

    private fun click_button_unset() {
        val opus_manager = this.get_opus_manager()
        opus_manager.unset()
    }

    private fun on_offset_change(view: NumberSelector) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val progress = view.getState()!!
        val current_tree = opus_manager.get_tree()

        val duration = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            event.duration
        } else {
            1
        }

        val radix = opus_manager.tuning_map.size

        val value = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            var prev_note = if (opus_manager.relative_mode != 0) {
                val nsOctave = this.get_main().findViewById<NumberSelector>(R.id.nsOctave)
                if (nsOctave.getState() == null) {
                    nsOctave.setState(0, manual = true, surpress_callback = true)
                    0
                } else {
                    event.note
                }
            } else {
                event.note
            }

            when (opus_manager.relative_mode) {
                2 -> {
                    if (prev_note > 0) {
                        prev_note *= -1
                    }
                    ((prev_note / radix) * radix) - progress
                }
                else -> {
                    ((prev_note / radix) * radix) + progress
                }
            }
        } else {
            when (opus_manager.relative_mode) {
                2 -> {
                    0 - progress
                }
                1 -> {
                    progress
                }
                else -> {
                    val beat_key = opus_manager.cursor.get_beatkey()
                    val position = opus_manager.cursor.get_position()
                    val preceding_event = opus_manager.get_preceding_event(beat_key, position)
                    if (preceding_event != null && !preceding_event.relative) {
                        ((preceding_event.note / radix) * radix) + progress
                    } else {
                        progress
                    }
                }
            }
        }

        val event = OpusEventSTD(
            value,
            opus_manager.cursor.channel,
            opus_manager.relative_mode != 0,
            duration
        )

        opus_manager.set_event_at_cursor(event)
        this._play_event(opus_manager.cursor.get_beatkey(), opus_manager.cursor.get_position())
    }

    private fun on_octave_change(view: NumberSelector) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val radix = opus_manager.tuning_map.size
        val progress = view.getState() ?: return

        val current_tree = opus_manager.get_tree()
        val duration = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            event.duration
        } else {
            1
        }

        val value = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            val prev_note = if (opus_manager.relative_mode != 0) {
                val nsOffset  = this.get_main().findViewById<NumberSelector>(R.id.nsOffset)
                if (nsOffset.getState() == null) {
                    nsOffset.setState(0, manual = true, surpress_callback = true)
                    0
                } else {
                    event.note
                }
            } else {
                event.note
            }

            when (opus_manager.relative_mode) {
                2 -> {
                    0 - (((0 - prev_note) % radix) + (progress * radix))
                }
                else -> {
                    ((prev_note % radix) + (progress * radix))
                }
            }
        } else {
            when (opus_manager.relative_mode) {
                2 -> {
                    (0 - progress) * radix
                }
                1 -> {
                    progress * radix
                }
                else -> {
                    val beat_key = opus_manager.cursor.get_beatkey()
                    val position = opus_manager.cursor.get_position()
                    val preceding_event = opus_manager.get_preceding_event(beat_key, position)
                    if (preceding_event != null && !preceding_event.relative) {
                        (progress * radix) + (preceding_event.note % radix)
                    } else {
                        (progress * radix)
                    }
                }
            }
        }

        val event = OpusEventSTD(
            value,
            opus_manager.cursor.channel,
            opus_manager.relative_mode != 0,
            duration
        )

        opus_manager.set_event_at_cursor(event)
        this._play_event(opus_manager.cursor.get_beatkey(), opus_manager.cursor.get_position())
    }

    private fun interact_rosRelativeOption(view: RelativeOptionSelector) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val current_tree = opus_manager.get_tree()

        opus_manager.relative_mode = view.getState()!!

        var event = current_tree.get_event() ?: return
        val radix = opus_manager.tuning_map.size

        val nsOctave: NumberSelector = main.findViewById(R.id.nsOctave)
        val nsOffset: NumberSelector = main.findViewById(R.id.nsOffset)

        when (opus_manager.relative_mode) {
            0 -> {
                if (event.relative) {
                    try {
                        opus_manager.convert_event_to_absolute()
                        event = current_tree.get_event()!!
                    } catch (e: Exception) {
                        event.note = 0
                        event.relative = false
                        opus_manager.set_event_at_cursor(event.copy())
                    }
                }
                nsOctave.setState(event.note / radix, manual = true, surpress_callback = true)
                nsOffset.setState(event.note % radix, manual = true, surpress_callback = true)
            }
            1 -> {
                if (!event.relative) {
                    opus_manager.convert_event_to_relative()
                    event = current_tree.get_event()!!
                }

                if (event.note < 0) {
                    nsOctave.unset_active_button()
                    nsOffset.unset_active_button()
                } else {
                    nsOctave.setState(event.note / radix, manual = true, surpress_callback = true)
                    nsOffset.setState(event.note % radix, manual = true, surpress_callback = true)
                }
            }
            2 -> {
                if (!event.relative) {
                    opus_manager.convert_event_to_relative()
                    event = current_tree.get_event()!!
                }

                if (event.note > 0) {
                    nsOctave.unset_active_button()
                    nsOffset.unset_active_button()
                } else {
                    nsOctave.setState(abs(event.note) / radix, manual = true, surpress_callback = true)
                    nsOffset.setState(abs(event.note) % radix, manual = true, surpress_callback = true)
                }
            }
        }
    }

    private fun _play_event(beat_key: BeatKey, position: List<Int>) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val event_note = opus_manager.get_absolute_value(beat_key, position) ?: return
        if (event_note < 0) {
            return
        }

        main.play_event(
            beat_key.channel,
            event_note,
            opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)
        )
    }
}