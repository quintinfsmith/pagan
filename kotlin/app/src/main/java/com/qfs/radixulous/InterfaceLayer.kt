package com.qfs.radixulous
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.opusmanager.BeatKey
import com.qfs.radixulous.opusmanager.CursorLayer
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.structure.OpusTree

class InterfaceLayer(var activity: MainActivity): CursorLayer() {
    var interface_lock = 0
    var relative_mode: Int = 0
    fun <T> with_i_lock(callback: (opus_manager: InterfaceLayer) -> T): T {
        //this.interface_lock += 1
        try {
            val output = callback(this)
            //this.interface_lock -= 1
            return output
        } catch (e: Exception) {
            //this.interface_lock -= 1
            throw e
        }
    }

    fun is_interface_unlocked(): Boolean {
        return this.interface_lock == 0
    }

    override fun set_project_name(new_name: String) {
        super.set_project_name(new_name)
        this.activity.update_title_text()
    }

    override fun unset(beatkey: BeatKey, position: List<Int>) {
        super.unset(beatkey, position)
        this.ui_refresh_beat_labels(beatkey)
        //this.reset_context_menu()
    }

    override fun replace_tree(beatkey: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        super.replace_tree(beatkey, position, tree)
        this.ui_refresh_beat_labels(beatkey)
        //this.reset_context_menu()
    }

    override fun set_event(beatkey: BeatKey, position: List<Int>, event: OpusEvent) {
        super.set_event(beatkey, position, event)

        // If the OM is applying history, change the relative mode, otherwise leave it.
        if (this.history_cache.isLocked()) {
            this.set_relative_mode(event)
        }
        //this.setContextMenu(ContextMenu.Leaf)
        this.ui_refresh_beat_labels(beatkey)
        //this.reset_context_menu()
    }
    fun set_relative_mode(event: OpusEvent) {
        this.relative_mode = if (event.relative) {
            if (event.note >= 0) {
                1
            } else {
                2
            }
        } else {
            0
        }
    }

    override fun apply_undo() {
        if (this.has_history()) {
            this.with_i_lock {
                super.apply_undo()
            }
            this.reset_context_menu()
        } else {
            this.activity.feedback_msg(this.activity.getString(R.string.msg_undo_none))
        }
    }

    override fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        super.set_percussion_instrument(line_offset, instrument)

        //this.reset_context_menu()
        this.update_line_labels()
    }

    override fun split_tree(beatkey: BeatKey, position: List<Int>, splits: Int) {
        super.split_tree(beatkey, position, splits)
        this.ui_refresh_beat_labels(beatkey)
        //this.reset_context_menu()
    }

    override fun insert_after(beatkey: BeatKey, position: List<Int>) {
        super.insert_after(beatkey, position)

        this.ui_refresh_beat_labels(beatkey)
        //this.reset_context_menu()
    }

    override fun remove(beatkey: BeatKey, position: List<Int>) {
        super.remove(beatkey, position)
        this.ui_refresh_beat_labels(beatkey)
        //this.reset_context_menu()
    }

    override fun new_line(channel: Int, line_offset: Int?): List<OpusTree<OpusEvent>> {
        var output = super.new_line(channel, line_offset)
        this.ui_add_line_label()
        //this.reset_context_menu()
        return output
    }

    override fun remove_beat(beat: Int) {
        super.remove_beat(beat)
        this.ui_remove_beat(beat)
        //this.reset_context_menu()
    }

    override fun insert_beat(beat: Int, count: Int) {
        super.insert_beat(beat, count)
        for (i in 0 until count) {
            this.ui_add_beat(beat + i)
        }
    }

    override fun new() {
        super.new()
        val new_path = this.activity.project_manager.get_new_path()
        this.path = new_path

        this.ui_add_line_label()

        this.activity.update_menu_options()
        this.activity.setup_config_drawer()

        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        rvBeatTable.scrollToPosition(0)
        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        rvColumnLabels.scrollToPosition(0)
    }

    override fun import_midi(path: String) {
        this.activity.loading_reticle()
        super.import_midi(path)
        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        rvBeatTable.scrollToPosition(0)
        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        rvColumnLabels.scrollToPosition(0)
        this.activity.update_menu_options()
        this.activity.setup_config_drawer()

        this.activity.cancel_reticle()
    }

    override fun load(path: String) {
        this.activity.loading_reticle()

        super.load(path)
        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        rvBeatTable.scrollToPosition(0)
        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        rvColumnLabels.scrollToPosition(0)

        this.activity.update_menu_options()
        this.activity.setup_config_drawer()
        this.activity.cancel_reticle()
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)

        val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)
        val rvActiveChannels_adapter = rvActiveChannels.adapter as ChannelOptionAdapter
        rvActiveChannels_adapter.notifyItemRemoved(channel)
    }

    override fun clear() {
        var channel_counts = this.get_channel_line_counts()
        var beat_count = this.opus_beat_count

        super.clear()

        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        var rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter
        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter

        var channel_offset = 0
        channel_counts.forEachIndexed { _: Int, j: Int ->
            channel_offset += j
        }
        for (i in channel_offset - 1 downTo 0) {
            rvLineLabels_adapter.removeLineLabel(i)
        }

        for (i in 0 until beat_count) {
            rvBeatTable_adapter.removeBeatColumn((beat_count - 1) - i)
        }
    }

    fun ui_remove_beat(beat: Int) {
        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        rvBeatTable_adapter.removeBeatColumn(beat)
    }
    fun ui_add_beat(beat: Int) {
        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        rvBeatTable_adapter.addBeatColumn(beat)
    }

    fun ui_add_line_label() {
        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        var rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter
        rvLineLabels_adapter.addLineLabel()
    }
    fun ui_remove_line_label(channel: Int, line_offset: Int) {
        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        var rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter
        rvLineLabels_adapter.removeLineLabel(this.get_y(channel, line_offset))
    }

    fun ui_refresh_beat_labels(beatkey: BeatKey) {
        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        rvBeatTable_adapter.refresh_leaf_labels(beatkey.beat)
    }

    fun update_line_labels() {
        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter
        val start =
            (rvLineLabels.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val end =
            (rvLineLabels.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

        for (i in start..end) {
            rvLineLabels_adapter.notifyItemChanged(i)
        }
    }

    fun get_fragment(): MainFragment? {
        val output = this.activity.getActiveFragment()
        return if (output is MainFragment) {
            output
        } else {
            null
        }
    }

    fun reset_context_menu() {
        if (this.is_interface_unlocked()) {
            this.get_fragment()?.reset_context_menu()
        }
    }

}