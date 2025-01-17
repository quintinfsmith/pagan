package com.qfs.pagan

import android.view.ViewGroup
import android.widget.ImageView
import com.qfs.pagan.opusmanager.OpusLayerBase

class ContextMenuColumn(primary_parent: ViewGroup, secondary_parent: ViewGroup): ContextMenuView(R.layout.contextmenu_column, null, primary_parent, secondary_parent) {
    private lateinit var _button_insert: ImageView
    private lateinit var _button_remove: ImageView

    init {
        this.refresh()
    }

    override fun init_properties() {
        this._button_insert = this.primary!!.findViewById(R.id.btnInsertBeat)
        this._button_remove = this.primary.findViewById(R.id.btnRemoveBeat)
    }

    override fun refresh() {
        val opus_manager = this.get_opus_manager()

        this._button_remove.isEnabled = opus_manager.beat_count > 1
    }

    override fun setup_interactions() {
        this._button_insert.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_insert_beat()
        }

        this._button_insert.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_insert_beat()
        }

        this._button_remove.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_remove_beat()
        }

        this._button_remove.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_remove_beat()
        }
    }

    private fun click_button_remove_beat() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        try {
            opus_manager.remove_beat_at_cursor(1)
        } catch (e: OpusLayerBase.RemovingLastBeatException) {
            main.feedback_msg(this.context.getString(R.string.feedback_rm_lastbeat))
        }
    }

    private fun long_click_button_remove_beat(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        main.dialog_number_input(this.context.getString(R.string.dlg_remove_beats), 1, opus_manager.beat_count - 1) { count: Int ->
            opus_manager.remove_beat_at_cursor(count)
        }
        return true
    }

    private fun click_button_insert_beat() {
        val opus_manager = this.get_opus_manager()
        opus_manager.insert_beat_after_cursor(1)
    }

    private fun long_click_button_insert_beat(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        main.dialog_number_input(this.context.getString(R.string.dlg_insert_beats), 1, 4096) { count: Int ->
            opus_manager.insert_beat_after_cursor(count)
        }
        return true
    }
}
