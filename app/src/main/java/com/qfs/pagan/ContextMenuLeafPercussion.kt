package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.qfs.pagan.opusmanager.BeatKey

class ContextMenuLeafPercussion(context: Context, attrs: AttributeSet? = null): ContextMenuView(context, attrs) {
    val button_split: ButtonIcon
    val button_insert: ButtonIcon
    val button_unset: ButtonIcon
    val button_remove: ButtonIcon
    val button_duration: ButtonStd

    init {
        val view = LayoutInflater.from(this.context)
            .inflate(
                R.layout.contextmenu_cell_percussion,
                this as ViewGroup,
                false
            )

        this.addView(view)

        this.button_split = view.findViewById(R.id.btnSplit)
        this.button_insert = view.findViewById(R.id.btnInsert)
        this.button_unset = view.findViewById(R.id.btnUnset)
        this.button_remove = view.findViewById(R.id.btnRemove)
        this.button_duration = view.findViewById(R.id.btnDuration)

        this.refresh()
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

        if (opus_manager.get_tree().is_event()) {
            opus_manager.unset()
        } else {
            opus_manager.set_percussion_event()
        }
    }

    fun setup_interactions() {
        this.button_duration.setOnClickListener {
            this.click_button_duration()
        }
        this.button_duration.setOnLongClickListener {
            this.long_click_button_duration()
        }
        this.button_remove.setOnClickListener {
            this.click_button_remove()
        }

        this.button_remove.setOnLongClickListener {
            this.long_click_button_remove()
        }

        this.button_unset.setOnClickListener {
            this.click_button_unset()
        }

        this.button_split.setOnClickListener {
            this.click_button_split()
        }

        this.button_split.setOnLongClickListener {
            this.long_click_button_split()
        }

        this.button_insert.setOnClickListener {
            this.click_button_insert()
        }

        this.button_insert.setOnLongClickListener {
            this.long_click_button_insert()
        }
    }

    override fun refresh() {
        this.button_split.visibility = View.VISIBLE
        this.button_insert.visibility = View.VISIBLE

        val opus_manager = this.get_opus_manager()

        val current_tree = opus_manager.get_tree()
        if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            this.button_unset.setImageResource(R.drawable.unset)
            this.button_duration.text = this.context.getString(R.string.label_duration, event.duration)
            this.button_duration.visibility = View.VISIBLE
        } else {
            this.button_duration.visibility = View.GONE
        }


        if (current_tree.is_leaf() && !current_tree.is_event()) {
            this.button_unset.visibility = View.GONE
        } else {
            this.button_unset.visibility = View.VISIBLE
        }

        if (opus_manager.cursor.get_position().isEmpty()) {
            this.button_remove.visibility = View.GONE
        } else {
            this.button_remove.visibility = View.VISIBLE
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