package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.RelativeNoteEvent
import com.qfs.pagan.opusmanager.TunedInstrumentEvent
import kotlin.math.abs

class ContextMenuLeaf(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(R.layout.contextmenu_cell, null, primary_container, secondary_container) {
    lateinit var button_split: ImageView
    lateinit var button_insert: ImageView
    lateinit var button_unset: ImageView
    lateinit var button_remove: ImageView
    lateinit var button_duration: TextView
    lateinit var ns_octave: NumberSelector
    lateinit var ns_offset: NumberSelector
    lateinit var ros_relative_option: RelativeOptionSelector

    init {
        this.refresh()
    }

    override fun init_properties() {
        val primary = this.primary!!
        this.button_split = primary.findViewById(R.id.btnSplit)
        this.button_insert = primary.findViewById(R.id.btnInsert)
        this.button_unset = primary.findViewById(R.id.btnUnset)
        this.button_remove = primary.findViewById(R.id.btnRemove)
        this.button_duration = primary.findViewById(R.id.btnDuration)
        this.ns_octave = primary.findViewById(R.id.nsOctave)
        this.ns_offset = primary.findViewById(R.id.nsOffset)
        this.ros_relative_option = primary.findViewById(R.id.rosRelativeOption)
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

            this.get_main().get_action_interface().insert_leaf(1)
        }

        this.button_insert.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }

            this.get_main().get_action_interface().insert_leaf()
            true
        }
    }

    override fun refresh() {
        val main = this.get_main()
        val opus_manager = this.get_opus_manager()

        val radix = opus_manager.tuning_map.size
        this.ns_offset.set_max(radix - 1)

        if (main.configuration.relative_mode) {
            this.ros_relative_option.visibility = View.VISIBLE
            this.ros_relative_option.setState(opus_manager.relative_mode, true, true)
        } else {
            this.ros_relative_option.visibility = View.GONE
        }
        val beat_key = opus_manager.cursor.get_beatkey()
        val position = opus_manager.cursor.get_position()

        val current_tree_position = opus_manager.get_actual_position(beat_key, position)
        val current_event_tree = opus_manager.get_tree(current_tree_position.first, current_tree_position.second)

        when (val event = current_event_tree.get_event()) {
            is TunedInstrumentEvent -> {
                val value = if (event is RelativeNoteEvent) {
                    if (main.configuration.relative_mode) {
                        abs(event.offset)
                    } else {
                        opus_manager.get_absolute_value(beat_key, position)!!
                    }
                } else if (event is AbsoluteNoteEvent) {
                    event.note
                } else {
                    // Should Be Unreachable
                    throw Exception()
                }

                if (value >= 0) {
                    this.ns_offset.setState(
                        value % radix,
                        manual = true,
                        surpress_callback = true
                    )
                    this.ns_octave.setState(
                        value / radix,
                        manual = true,
                        surpress_callback = true
                    )
                }

                this.button_unset.setImageResource(R.drawable.unset)
                this.button_duration.text = this.context.getString(R.string.label_duration, event.duration)
            }
            null -> {
                this.ns_octave.unset_active_button()
                this.ns_offset.unset_active_button()
                this.button_duration.text = ""
            }
        }

        this.ros_relative_option.setState(
            opus_manager.relative_mode,
            true,
            true
        )

        this.button_split.isEnabled = true
        this.button_split.isClickable = this.button_split.isEnabled

        this.button_duration.isEnabled = current_event_tree.is_event()
        this.button_duration.isClickable = this.button_duration.isEnabled

        this.button_unset.isEnabled = !(current_event_tree.is_leaf() && !current_event_tree.is_event())
        this.button_unset.isClickable = this.button_unset.isEnabled

        this.button_remove.isEnabled = opus_manager.cursor.get_position().isNotEmpty()
        this.button_remove.isClickable = this.button_remove.isEnabled
    }

    private fun click_button_duration() {
        this.get_main().get_action_interface().set_duration()
    }

    private fun click_button_split() {
        this.get_main().get_action_interface().split(2)
    }

    private fun click_button_remove() {
        val main = this.get_main()
        main.get_action_interface().remove_at_cursor()
    }

    private fun long_click_button_split(): Boolean {
        val main = this.get_main()
        main.get_action_interface().split()
        return true
    }

    private fun long_click_button_duration(): Boolean {
        val main = this.get_main()
        main.get_action_interface().set_duration(1)
        return true
    }

    private fun long_click_button_remove(): Boolean {
        val main = this.get_main()
        main.get_action_interface().unset_root()
        return true
    }

    private fun click_button_unset() {
        this.get_main().get_action_interface().unset()
    }

    private fun on_offset_change(view: NumberSelector) {
        val main = this.get_main()
        val progress = view.getState()!!
        main.get_action_interface().set_offset(progress)
    }

    private fun on_octave_change(view: NumberSelector) {
        val main = this.get_main()
        val progress = view.getState()!!
        main.get_action_interface().set_octave(progress)
    }

    private fun interact_rosRelativeOption(view: RelativeOptionSelector) {
        // TODO: Add to ActionTracker
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val current_tree_position = opus_manager.get_actual_position(
            opus_manager.cursor.get_beatkey(),
            opus_manager.cursor.get_position()
        )
        val current_tree = opus_manager.get_tree(current_tree_position.first, current_tree_position.second)

        val event = current_tree.get_event()
        if (event == null) {
            val state = view.getState() ?: return
            opus_manager.set_relative_mode(state, false)
            return
        }
        val radix = opus_manager.tuning_map.size

        val new_value = when (event) {
            is RelativeNoteEvent -> {
                when (view.getState()) {
                    /* Abs */
                    0 -> {
                        try {
                            opus_manager.convert_event_to_absolute()
                            //val current_tree = opus_manager.get_tree()
                            val new_event = current_tree.get_event()!!
                            (new_event as AbsoluteNoteEvent).note
                        } catch (e: OpusLayerBase.NoteOutOfRange) {
                            opus_manager.set_event_at_cursor(
                                AbsoluteNoteEvent(0, event.duration)
                            )
                            0
                        }
                    }
                    /* + */
                    1 -> {
                        if (event.offset < 0) {
                            val new_event = RelativeNoteEvent(0 - event.offset, event.duration)
                            opus_manager.set_event_at_cursor(new_event)
                        }
                        abs(event.offset)
                    }
                    /* - */
                    2 -> {
                        if (event.offset > 0) {
                            val new_event = RelativeNoteEvent(0 - event.offset, event.duration)
                            opus_manager.set_event_at_cursor(new_event)
                        }
                        abs(event.offset)
                    }

                    else -> null
                }
            }
            is AbsoluteNoteEvent -> {
                when (view.getState()) {
                    /* Abs */
                    0 -> {
                        event.note
                    }
                    /* + */
                    1 -> {
                        val cursor = opus_manager.cursor
                        val value = opus_manager.get_relative_value(cursor.get_beatkey(), cursor.get_position())
                        if (value >= 0) {
                            opus_manager.convert_event_to_relative()

                            //val current_tree = opus_manager.get_tree()
                            val new_event = current_tree.get_event()!!
                            abs((new_event as RelativeNoteEvent).offset)
                        } else {
                            opus_manager.relative_mode = 1
                            null
                        }

                    }
                    /* - */
                    2 -> {
                        val cursor = opus_manager.cursor
                        val value = opus_manager.get_relative_value(cursor.get_beatkey(), cursor.get_position())
                        if (value <= 0) {
                            opus_manager.convert_event_to_relative()

                            //val current_tree = opus_manager.get_tree()
                            val new_event = current_tree.get_event()!!
                            abs((new_event as RelativeNoteEvent).offset)
                        } else {
                            opus_manager.relative_mode = 2
                            null
                        }
                    }

                    else -> null
                }
            }

            else -> null
        }

        val nsOctave: NumberSelector = main.findViewById(R.id.nsOctave)
        val nsOffset: NumberSelector = main.findViewById(R.id.nsOffset)
        if (new_value == null) {
            nsOctave.unset_active_button()
            nsOffset.unset_active_button()
        } else {
            nsOctave.setState(new_value / radix, manual = true, surpress_callback = true)
            nsOffset.setState(new_value % radix, manual = true, surpress_callback = true)
        }
        opus_manager.relative_mode = view.getState() ?: 0
    }
}
