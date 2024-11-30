package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import com.qfs.pagan.opusmanager.BeatKey

class ContextMenuLeafPercussion(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(R.layout.contextmenu_cell_percussion, null, primary_container, secondary_container) {
    lateinit var button_split: ButtonIcon
    lateinit var button_insert: ButtonIcon
    lateinit var button_unset: ButtonIcon
    lateinit var button_remove: ButtonIcon
    lateinit var button_duration: ButtonStd

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
    }

    override fun refresh() {
        this.button_split.visibility = View.VISIBLE
        this.button_insert.visibility = View.VISIBLE

        val opus_manager = this.get_opus_manager()

        val beat_key = opus_manager.cursor.get_beatkey()
        val position = opus_manager.cursor.get_position()

        val current_tree_position = opus_manager.get_actual_position(beat_key, position)
        val current_event_tree = opus_manager.get_tree(current_tree_position.first, current_tree_position.second)
        val current_tree = opus_manager.get_tree(beat_key, position)

        if (current_event_tree.is_event()) {
            val event = current_event_tree.get_event()!!
            this.button_unset.setImageResource(R.drawable.unset)
            this.button_duration.text = this.context.getString(R.string.label_duration, event.duration)
        } else {
            this.button_unset.setImageResource(R.drawable.set_percussion)
            this.button_duration.text = ""
        }

        val blocked_amount = opus_manager.get_blocking_amount(beat_key, position)

        this.button_split.isEnabled = true
        this.button_split.isClickable = this.button_split.isEnabled

        this.button_duration.isEnabled = current_event_tree.is_event()
        this.button_duration.isClickable = this.button_duration.isEnabled

        this.button_remove.isEnabled = position.isNotEmpty()
        this.button_remove.isClickable = this.button_remove.isEnabled
    }

    override fun setup_interactions() {
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

    fun click_button_duration() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val cursor = opus_manager.cursor
        val beat_key = cursor.get_beatkey()
        val position = cursor.get_position()

        val current_tree_position = opus_manager.get_actual_position(beat_key, position)
        val current_event_tree = opus_manager.get_tree(current_tree_position.first, current_tree_position.second)

        val event_duration = current_event_tree.get_event()?.duration ?: return

        main.dialog_number_input(this.context.getString(R.string.dlg_duration), 1, 99, default=event_duration) { value: Int ->
            val adj_value = Integer.max(value, 1)
            opus_manager.set_duration(current_tree_position.first, current_tree_position.second, adj_value)
        }
    }

    fun click_button_split() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.split_tree_at_cursor(2)
    }

    fun click_button_insert() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val position = opus_manager.cursor.get_position().toMutableList()
        if (position.isEmpty()) {
            opus_manager.split_tree_at_cursor(2)
        } else {
            opus_manager.insert_after_cursor(1)
        }
    }

    fun click_button_remove() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.remove_at_cursor(1)
    }

    fun long_click_button_split(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        main.dialog_number_input(this.context.getString(R.string.dlg_split), 2, 32) { splits: Int ->
            opus_manager.split_tree_at_cursor(splits)
        }
        return true
    }

    fun long_click_button_insert(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        main.dialog_number_input(this.context.getString(R.string.dlg_insert), 1, 29) { count: Int ->
            val position = opus_manager.cursor.get_position().toMutableList()
            if (position.isEmpty()) {
                opus_manager.split_tree_at_cursor(count + 1)
            } else {
                opus_manager.insert_after_cursor(count)
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
        val current_tree_position = opus_manager.get_actual_position(beat_key, position)

        opus_manager.set_duration(current_tree_position.first, current_tree_position.second, 1)
        return true
    }

    fun long_click_button_remove(): Boolean {
        val opus_manager = this.get_opus_manager()

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
        val cursor = opus_manager.cursor
        val beat_key = cursor.get_beatkey()
        val position = cursor.get_position()
        val current_tree_position = opus_manager.get_actual_position(beat_key, position)

        if (opus_manager.get_tree(current_tree_position.first, current_tree_position.second).is_event()) {
            opus_manager.unset()
        } else {
            opus_manager.set_percussion_event_at_cursor()
            this._play_event(opus_manager.cursor.get_beatkey(), opus_manager.cursor.get_position())
        }
    }

    private fun _play_event(beat_key: BeatKey, position: List<Int>) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val event_note = opus_manager.get_percussion_instrument(beat_key.line_offset)

        main.play_event(
            beat_key.channel,
            event_note,
            opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)
        )
    }
}
