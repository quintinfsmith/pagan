package com.qfs.radixulous
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.apres.MIDI
import com.qfs.radixulous.opusmanager.BeatKey
import com.qfs.radixulous.opusmanager.CursorLayer
import com.qfs.radixulous.opusmanager.OpusChannel
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.structure.OpusTree

class InterfaceLayer(var activity: MainActivity): CursorLayer() {
    var interface_lock = 0
    var relative_mode: Int = 0

    private fun interface_locked(): Boolean {
        return this.interface_lock != 0
    }

    private fun <T> surpress_ui(callback:(InterfaceLayer) -> T): T {
        this.interface_lock += 1
        try {
            val output = callback(this)
            this.interface_lock -= 1
            return output
        } catch (e: Exception) {
            this.interface_lock -= 1
            throw e
        }
    }

    override fun set_channel_instrument(channel: Int, instrument: Int) {
        super.set_channel_instrument(channel, instrument)

        val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)
        rvActiveChannels.adapter?.notifyItemChanged(channel)
    }

    override fun set_percussion_channel(channel: Int) {
        super.set_percussion_channel(channel)
        val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)
        rvActiveChannels.adapter?.notifyItemChanged(channel)
    }

    override fun unset_percussion_channel() {
        val old_channel = this.percussion_channel
        super.unset_percussion_channel()
        if (old_channel != null) {
            val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)
            rvActiveChannels.adapter?.notifyItemChanged(old_channel)
        }
    }

    override fun set_project_name(new_name: String) {
        super.set_project_name(new_name)
        this.activity.update_title_text()
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        super.unset(beat_key, position)
        this.ui_refresh_beat_labels(beat_key)
        this.ui_scroll_to_position(beat_key, position)
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        super.replace_tree(beat_key, position, tree)
        this.ui_refresh_beat_labels(beat_key)
        this.ui_scroll_to_position(beat_key, position)
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        super.set_event(beat_key, position, event)

        // If the OM is applying history, change the relative mode, otherwise leave it.
        if (this.history_cache.isLocked()) {
            this.set_relative_mode(event)
        }

        this.ui_refresh_beat_labels(beat_key)
        this.ui_scroll_to_position(beat_key, position)
    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        super.set_percussion_event(beat_key, position)

        this.ui_refresh_beat_labels(beat_key)
        this.ui_scroll_to_position(beat_key, position)

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
            super.apply_undo()
            this.reset_context_menu()
        } else {
            this.activity.feedback_msg(this.activity.getString(R.string.msg_undo_none))
        }
    }

    override fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        super.set_percussion_instrument(line_offset, instrument)

        this.update_line_labels()
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        super.split_tree(beat_key, position, splits)
        this.ui_refresh_beat_labels(beat_key)
        this.ui_scroll_to_position(beat_key, position)
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        super.insert_after(beat_key, position)
        this.withFragment {
            if (position.isNotEmpty()) {
                val new_position = position.toMutableList()
                new_position[new_position.size - 1] += 1
            }
            this.ui_scroll_to_position(beat_key, position)
        }

        this.ui_refresh_beat_labels(beat_key)
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.ui_scroll_to_position(beat_key, position)
        super.remove(beat_key, position)
        this.ui_refresh_beat_labels(beat_key)
    }

    override fun new_line(channel: Int, line_offset: Int?): List<OpusTree<OpusEvent>> {
        val output = super.new_line(channel, line_offset)
        this.ui_add_line_label()
        this.ui_notify_visible_changes()
        return output
    }

    override fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        val initial_old_size = this.channels[channel_old].size
        val initial_new_size = this.channels[channel_new].size
        this.surpress_ui {
            super.move_line(channel_old, line_old, channel_new, line_new)
        }

        val current_new_size = this.channels[channel_new].size
        if (current_new_size > initial_new_size) {
            for (i in initial_new_size until current_new_size) {
                this.ui_add_line_label()
            }
        } else {
            for (i in current_new_size until initial_new_size) {
                this.ui_remove_line_label(channel_new, i)
            }
        }

        if (channel_new != channel_old) {
            val current_old_size = this.channels[channel_old].size
            if (current_old_size > initial_old_size) {
                for (i in initial_old_size until current_old_size) {
                    this.ui_add_line_label()
                }
            } else {
                for (i in current_old_size until initial_old_size) {
                    this.ui_remove_line_label(channel_old, i)
                }
            }
        }

        this.ui_notify_visible_changes()
    }

    override fun remove_line(channel: Int, line_offset: Int): MutableList<OpusTree<OpusEvent>> {
        val output = super.remove_line(channel, line_offset)
        this.ui_remove_line_label(channel, line_offset)
        this.ui_notify_visible_changes()
        return output
    }

    override fun new_channel(channel: Int?, lines: Int) {
        super.new_channel(channel, lines)
        for (i in 0 until lines) {
            this.ui_add_line_label()
        }
        this.ui_notify_visible_changes()
        if (! this.interface_locked()) {
            val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)
            rvActiveChannels.adapter?.notifyItemInserted(channel ?: (this.channels.size - 1))
        }
    }

    override fun set_cursor_position(beat_key: BeatKey, position: List<Int>) {
        val previous_beat_key = this.get_cursor().get_beatkey()
        var previous_position = this.get_cursor().get_position()
        super.set_cursor_position(beat_key, position)
        if (beat_key != previous_beat_key && position != previous_position) {
            this.ui_refresh_beat_labels(beat_key)
        }
    }

    override fun remove_beat(beat_index: Int) {
        super.remove_beat(beat_index)
        this.ui_remove_beat(beat_index)
        this.ui_refresh_beat_labels(this.get_cursor().get_beatkey())
    }

    override fun insert_beat(beat_index: Int) {
        val original_beat = this.get_cursor().get_beatkey()
        super.insert_beat(beat_index)
        this.ui_add_beat(beat_index)
        this.ui_refresh_beat_labels(original_beat)
    }

    override fun new() {
        this.surpress_ui {
            super.new()
        }
        val new_path = this.activity.project_manager.get_new_path()
        this.path = new_path

        this.init_ui()

        this.activity.update_menu_options()
        this.activity.setup_config_drawer()

        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        rvBeatTable.scrollToPosition(0)
        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        rvColumnLabels.scrollToPosition(0)
    }

    override fun import_midi(midi: MIDI) {
        this.activity.loading_reticle()
        this.surpress_ui {
            super.import_midi(midi)
        }
        this.init_ui()
        this.activity.update_menu_options()
        this.activity.setup_config_drawer()

        this.activity.cancel_reticle()

        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        rvBeatTable.scrollToPosition(0)
        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        rvColumnLabels.scrollToPosition(0)
    }

    override fun load(path: String) {
        this.activity.loading_reticle()
        this.surpress_ui {
            super.load(path)
        }
        this.init_ui()
        this.activity.update_menu_options()
        this.activity.setup_config_drawer()
        this.activity.cancel_reticle()

        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        rvBeatTable.scrollToPosition(0)
        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        rvColumnLabels.scrollToPosition(0)
    }

    override fun remove_channel(channel: Int) {
        val lines = this.channels[channel].size
        for (i in 0 until lines) {
            this.ui_remove_line_label(channel, 0)
        }

        super.remove_channel(channel)

        val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)
        if (rvActiveChannels.adapter != null) {
            val rvActiveChannels_adapter = rvActiveChannels.adapter as ChannelOptionAdapter
            rvActiveChannels_adapter.notifyItemRemoved(channel)

            this.ui_notify_visible_changes()
        }
    }

    override fun clear() {
        val channel_counts = this.get_channel_line_counts()
        val beat_count = this.opus_beat_count

        val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)
        super.clear()

        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter
        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter

        var channel_offset = 0
        channel_counts.forEachIndexed { _: Int, j: Int ->
            rvActiveChannels.adapter?.notifyItemRemoved(0)
            channel_offset += j
        }
        for (i in channel_offset - 1 downTo 0) {
            rvLineLabels_adapter.removeLineLabel(i)
        }
        for (i in beat_count downTo 0) {
            rvBeatTable_adapter.removeBeatColumn(i)
        }

    }

    override fun unlink_beat(beat_key: BeatKey) {
        var update_keys = this.get_all_linked(beat_key).toMutableList()
        update_keys.remove(beat_key)
        super.unlink_beat(beat_key)
        // Need to run update on both the beat_key and *any* of its former link pool
        this.ui_refresh_beat_labels(beat_key)
        if (update_keys.isNotEmpty()) {
            this.ui_refresh_beat_labels(update_keys.first())
        }
    }

    private fun ui_scroll_to_position(beat_key: BeatKey, position: List<Int>) {
        if (this.interface_locked()) {
            return
        }
        this.withFragment {
            it.scrollTo(beat_key, position)
        }

    }

    private fun ui_remove_beat(beat: Int) {
        if (this.interface_locked()) {
            return
        }
        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        rvBeatTable_adapter.removeBeatColumn(beat)
    }
    private fun ui_add_beat(beat: Int) {
        if (this.interface_locked()) {
            return
        }
        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        rvBeatTable_adapter.addBeatColumn(beat)
    }

    private fun ui_notify_visible_changes() {
        if (this.interface_locked()) {
            return
        }
        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        val first = (beat_table.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val last = (beat_table.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        for (i in first - 5 .. last + 5) {
            rvBeatTable_adapter.notifyItemChanged(i)
        }
    }

    private fun ui_add_line_label() {
        if (this.interface_locked()) {
            return
        }
        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter
        rvLineLabels_adapter.addLineLabel()
    }
    private fun ui_remove_line_label(channel: Int, line_offset: Int) {
        if (this.interface_locked()) {
            return
        }
        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter
        rvLineLabels_adapter.removeLineLabel(this.get_y(channel, line_offset))
    }

    private fun ui_refresh_beat_labels(beat_key: BeatKey) {
        if (this.interface_locked()) {
            return
        }
        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        val linked_beats = mutableSetOf<Int>()
        for (linked_key in this.get_all_linked(beat_key)) {
            linked_beats.add(linked_key.beat)
        }
        for (beat in linked_beats) {
            rvBeatTable_adapter.refresh_leaf_labels(beat)
        }
    }

    private fun update_line_labels() {
        if (this.interface_locked()) {
            return
        }
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

    private fun <T> withFragment(callback: (MainFragment) -> T): T? {
        val fragment = this.activity.getActiveFragment()
        return if (fragment is MainFragment) {
            callback(fragment)
        } else {
            null
        }
    }

    private fun reset_context_menu() {
        if (this.interface_locked()) {
            return
        }
        this.withFragment {
            it.reset_context_menu()
        }
    }

    private fun init_ui() {
        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter
        val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)

        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        for (i in 0 until this.opus_beat_count) {
            rvBeatTable_adapter.addBeatColumn(i)
        }

        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            repeat(channel.lines.size) {
                rvLineLabels_adapter.addLineLabel()
            }
            rvActiveChannels.adapter?.notifyItemInserted(i)
        }
    }

    override fun set_transpose(new_transpose: Int)  {
        super.set_transpose(new_transpose)
        val btnTranspose: TextView = this.activity.findViewById(R.id.btnTranspose)
        btnTranspose.text = "T: ${get_number_string(new_transpose, this.RADIX, 2)}"
    }

    override fun set_tempo(new_tempo: Float) {
        super.set_tempo(new_tempo)

        val tvTempo = this.activity.findViewById<TextView>(R.id.tvTempo)
        tvTempo.text = "${this.tempo.toInt()} BPM"
    }
}