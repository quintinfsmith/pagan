package com.qfs.pagan

import android.view.ViewGroup
import com.qfs.pagan.opusmanager.OpusLayerBase
import kotlin.math.min

class ContextMenuPlayback(primary_parent: ViewGroup, secondary_parent: ViewGroup): ContextMenuView(R.layout.contextmenu_column, null, primary_parent, secondary_parent) {
    lateinit var button_insert: ButtonIcon
    lateinit var button_remove: ButtonIcon

    init {
        this.refresh()
    }

    override fun init_properties() {
        this.button_insert = this.primary!!.findViewById(R.id.btnInsertBeat)
        this.button_remove = this.primary!!.findViewById(R.id.btnRemoveBeat)
    }

    override fun refresh() {
        val opus_manager = this.get_opus_manager()
        val index = opus_manager.cursor.beat
        this.button_remove.isEnabled = opus_manager.beat_count > 1 && try {
            opus_manager.blocked_check_remove_beat(index)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun setup_interactions() {
        this.button_insert.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_insert_beat()
        }

        this.button_insert.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_insert_beat()
        }

        this.button_remove.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_remove_beat()
        }

        this.button_remove.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_remove_beat()
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
            val maximum = opus_manager.beat_count - opus_manager.cursor.beat 
            opus_manager.remove_beat_at_cursor(min(maximum, count))
        }
        return true
    }

    fun click_button_insert_beat() {
        val opus_manager = this.get_opus_manager()
        opus_manager.insert_beat_after_cursor(1)
        opus_manager.cursor_select_column(
            opus_manager.cursor.beat + 1
        )
    }

    fun long_click_button_insert_beat(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        main.dialog_number_input(this.context.getString(R.string.dlg_insert_beats), 1, 4096) { count: Int ->
            opus_manager.insert_beat_after_cursor(count)
                opus_manager.cursor_select_column(
                    opus_manager.cursor.beat + count
                )
        }
        return true
    }
}
