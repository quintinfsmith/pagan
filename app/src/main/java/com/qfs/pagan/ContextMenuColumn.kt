package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.qfs.pagan.opusmanager.OpusLayerBase

class ContextMenuColumn(context: Context, attrs: AttributeSet? = null): ContextMenuView(context, attrs) {
    val button_insert: ButtonIcon
    val button_remove: ButtonIcon
    init {
        val view = LayoutInflater.from(this.context)
            .inflate(
                R.layout.contextmenu_column,
                this as ViewGroup,
                false
            )
        this.addView(view)

        this.button_insert = this.findViewById(R.id.btnInsertBeat)
        this.button_remove = this.findViewById(R.id.btnRemoveBeat)
        this.refresh()
    }

    override fun refresh() {
        val opus_manager = this.get_opus_manager()

        this.button_insert.visibility = View.VISIBLE
        this.button_remove.visibility = if (opus_manager.beat_count == 1) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    fun click_button_remove_beat() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        try {
            opus_manager.remove_beat_at_cursor(1)
        } catch (e: OpusLayerBase.RemovingLastBeatException) {
            main.feedback_msg(this.context.getString(R.string.feedback_rm_lastbeat))
        }
    }

    fun long_click_button_remove_beat(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        main.dialog_number_input(this.context.getString(R.string.dlg_remove_beats), 1, opus_manager.beat_count - 1) { count: Int ->
            opus_manager.remove_beat_at_cursor(count)
        }
        return true
    }

    fun click_button_insert_beat() {
        val opus_manager = this.get_opus_manager()
        opus_manager.insert_beat_after_cursor(1)
    }

    fun long_click_button_insert_beat(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        main.dialog_number_input(this.context.getString(R.string.dlg_insert_beats), 1, 4096) { count: Int ->
            opus_manager.insert_beat_after_cursor(count)
        }
        return true
    }

    fun setup_interactions() {
        this.button_insert.setOnClickListener {
            this.click_button_insert_beat()
        }

        this.button_insert.setOnLongClickListener {
            this.long_click_button_insert_beat()
        }

        this.button_remove.setOnClickListener {
            this.click_button_remove_beat()
        }

        this.button_remove.setOnLongClickListener {
            this.long_click_button_remove_beat()
        }
    }
}