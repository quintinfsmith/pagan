package com.qfs.radixulous
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.apres.MIDI
import com.qfs.radixulous.opusmanager.*
import com.qfs.radixulous.structure.OpusTree
import java.lang.Integer.max
import java.lang.Integer.min

class InterfaceLayer(var activity: MainActivity): HistoryLayer() {
    enum class LabelUpdate {
        AddColumn,
        PopColumn,
        AddLine,
        PopLine
    }
    var simple_ui_lock = 0
    var relative_mode: Int = 0
    var cursor = Cursor()
    var queued_location_stamp: Pair<BeatKey, List<Int>?>? = null

    private fun simple_ui_locked(): Boolean {
        return this.simple_ui_lock != 0
    }

    private fun <T> surpress_ui(callback:(InterfaceLayer) -> T): T {
        this.simple_ui_lock += 1
        try {
            val output = callback(this)
            this.simple_ui_lock -= 1
            return output
        } catch (e: Exception) {
            this.simple_ui_lock -= 1
            throw e
        }
    }

    override fun set_channel_instrument(channel: Int, instrument: Int) {
        super.set_channel_instrument(channel, instrument)

        val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)
        rvActiveChannels.adapter?.notifyItemChanged(channel)

        this.activity.update_channel_instruments(this)
    }

    override fun set_percussion_channel(channel: Int) {
        super.set_percussion_channel(channel)
        val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)
        rvActiveChannels.adapter?.notifyItemChanged(channel)
        if (!this.simple_ui_locked()) {
            this.update_line_labels()
        }
    }

    override fun unset_percussion_channel() {
        val old_channel = this.percussion_channel
        super.unset_percussion_channel()
        if (!this.simple_ui_locked() && old_channel != null) {
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
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        super.replace_tree(beat_key, position, tree)
        this.ui_refresh_beat_labels(beat_key)
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        super.set_event(beat_key, position, event)

        // If the OM is applying history, change the relative mode, otherwise leave it.
        if (this.history_cache.isLocked()) {
            this.set_relative_mode(event)
        }

        this.ui_refresh_beat_labels(beat_key)
    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        super.set_percussion_event(beat_key, position)
        this.ui_refresh_beat_labels(beat_key)
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

    override fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        super.set_percussion_instrument(line_offset, instrument)

        val btnChoosePercussion: TextView = this.activity.findViewById(R.id.btnChoosePercussion)

        val drums = this.activity.resources.getStringArray(R.array.midi_drums)
        val drum_index = this.get_percussion_instrument(line_offset)
        btnChoosePercussion.text = "$instrument: ${drums[drum_index]}"

        this.update_line_labels()
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        super.split_tree(beat_key, position, splits)
        this.ui_refresh_beat_labels(beat_key)
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        super.insert_after(beat_key, position)
        this.ui_refresh_beat_labels(beat_key)
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)
        this.ui_refresh_beat_labels(beat_key)
    }

    override fun new_line(channel: Int, line_offset: Int?): List<OpusTree<OpusEvent>> {
        val output = super.new_line(channel, line_offset)

        this.ui_add_line_label()
        this.ui_notify_visible_changes()
        return output
    }

    override fun insert_line(channel: Int, line_offset: Int, line: MutableList<OpusTree<OpusEvent>>) {
        super.insert_line(channel, line_offset, line)
        this.ui_add_line_label()
        this.ui_notify_visible_changes()
    }

    //override fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
    //    super.move_line(channel_old, line_old, channel_new, line_new)
    //    this.ui_notify_visible_changes()
    //}

    override fun remove_line(channel: Int, line_offset: Int): MutableList<OpusTree<OpusEvent>> {
        this.ui_remove_line_label(channel, line_offset)
        val output = super.remove_line(channel, line_offset)
        this.ui_notify_visible_changes()
        return output
    }

    override fun new_channel(channel: Int?, lines: Int) {
        super.new_channel(channel, lines)
        for (i in 0 until lines) {
            this.ui_add_line_label()
        }
        this.ui_notify_visible_changes()
        val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)
        rvActiveChannels.adapter?.notifyItemInserted(channel ?: (this.channels.size - 1))
    }

    override fun remove_beat(beat_index: Int) {
        super.remove_beat(beat_index)
        this.ui_remove_beat(beat_index)
        //this.ui_refresh_beat_labels(this.get_cursor().get_beatkey())
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        super.insert_beat(beat_index, beats_in_column)
        this.ui_add_beat(beat_index)
        //this.ui_refresh_beat_labels(original_beat)
    }

    override fun new() {
        this.surpress_ui {
            super.new()
        }
        val new_path = this.activity.project_manager.get_new_path()
        this.path = new_path

        //this.init_ui()

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
        //this.init_ui()
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
        //this.init_ui()
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
        this.cursor.clear()

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
        if (this.simple_ui_locked()) {
            return
        }

        this.withFragment {
            it.scrollTo(beat_key, position)
        }
    }

    private fun ui_remove_beat(beat: Int) {
        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        rvBeatTable_adapter.removeBeatColumn(beat)
    }
    private fun ui_add_beat(beat: Int) {
        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        rvBeatTable_adapter.addBeatColumn(beat)
    }

    private fun ui_notify_visible_changes() {
        if (this.simple_ui_locked()) {
            return
        }

        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        val first = (beat_table.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val last = (beat_table.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        for (i in max(0, first - 5) .. min(last + 5, this.opus_beat_count - 1)) {
            rvBeatTable_adapter.notifyItemChanged(i)
        }
    }

    private fun ui_add_line_label() {
        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter
        rvLineLabels_adapter.addLineLabel()
    }

    private fun ui_remove_line_label(channel: Int, line_offset: Int) {
        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter
        rvLineLabels_adapter.removeLineLabel(this.get_abs_offset(channel, line_offset))
    }

    private fun ui_refresh_beat_labels(beat_key: BeatKey) {
        if (this.simple_ui_locked()) {
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
        if (this.simple_ui_locked()) {
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
        if (this.simple_ui_locked()) {
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
        Log.d("AAA", "XXXX -- ${rvBeatTable_adapter.itemCount}")
        //for (i in 0 until this.opus_beat_count) {
        //    rvBeatTable_adapter.addBeatColumn(i)
        //}

        //this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
        //    repeat(channel.lines.size) {
        //        rvLineLabels_adapter.addLineLabel()
        //    }
        //    rvActiveChannels.adapter?.notifyItemInserted(i)
        //}
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

    override fun apply_undo() {
        if (this.has_history()) {
            this.surpress_ui {
                super.apply_undo()
            }
            this.ui_notify_visible_changes()

            if (this.queued_location_stamp != null) {
                Log.d("AAA", "${this.queued_location_stamp}!!!!!!!!")
                val (beat_key, position) = this.queued_location_stamp!!
                this.queued_location_stamp = null

                if (beat_key.beat == -1) { // Row Select
                    this.cursor_select_row(beat_key.channel, beat_key.line_offset)
                } else if (beat_key.channel == -1 || beat_key.line_offset == -1) { // Beat Select
                    this.cursor_select_column(beat_key.beat)
                } else if (position == null) {
                    Log.w("AAA", "No position given")
                    return
                } else {
                    this.cursor_select(beat_key, position)
                }
            } else {
                this.cursor_clear()
            }

            this.update_line_labels()

        } else {
            this.activity.feedback_msg(this.activity.getString(R.string.msg_undo_none))
        }
    }

    override fun apply_history_node(current_node: HistoryNode, depth: Int)  {
        super.apply_history_node(current_node, depth)
        Log.d("AAA", "Setting Stamp: ${current_node.func_name}")
        this.queued_location_stamp = when (current_node.func_name) {
            "cursor_select_row" -> {
                Log.d("AAA", "${BeatKey( current_node.args[0] as Int, current_node.args[1] as Int, -1 )}")
                Pair(
                    BeatKey(
                        current_node.args[0] as Int,
                        current_node.args[1] as Int,
                        -1
                    ),
                    null
                )
            }
            "cursor_select" -> {
                Pair(
                    current_node.args[0] as BeatKey,
                    current_node.args[1] as List<Int>
                )
            }
            "cursor_select_column" -> {
                Pair(
                    BeatKey(
                        -1,
                        -1,
                        current_node.args[0] as Int
                    ),
                    null
                )
            }
            else -> {
                this.queued_location_stamp
            }
        }
    }

    override fun push_to_history_stack(func_name: String, args: List<Any>) {
        if (this.history_cache.isLocked()) {
            return
        }

        super.history_cache.remember {
            when (func_name) {
                "move_line" -> {
                    var to_line = args[3] as Int
                    var from_line = args[1] as Int
                    super.push_to_history_stack(
                        "cursor_select_row",
                        listOf(
                            args[2] as Int,
                            if (args[0] as Int == args[2] as Int && to_line >= from_line) {
                                to_line - 1
                            } else {
                                to_line
                            }
                        )
                    )
                }
                "insert_line" -> {
                    super.push_to_history_stack(
                        "cursor_select_row",
                        listOf(
                            args[0] as Int,
                            args[1] as Int
                        )
                    )
                }
                "remove_line" -> {
                    var channel = args[0] as Int
                    var line_offset = args[1] as Int
                    super.push_to_history_stack(
                        "cursor_select_row",
                        listOf(
                            channel,
                            if (line_offset == this.channels[channel].size - 1) {
                                line_offset - 1
                            } else {
                                line_offset
                            }
                        )
                    )
                }
                "replace_tree" -> {
                    var new_position = (args[1] as List<Int>).toMutableList()
                    var tree = args[2] as OpusTree<OpusEvent>
                    while (! tree.is_leaf()) {
                        new_position.add(0)
                        tree = tree[0]
                    }

                    super.push_to_history_stack(
                        "cursor_select",
                        listOf(
                            args[0] as BeatKey,
                            new_position
                        )
                    )
                }
                "set_percussion_instrument" ->{
                    super.push_to_history_stack(
                        "cursor_select_row",
                        listOf(
                            this.percussion_channel!!,
                            args[0] as Int
                        )
                    )
                }
                "set_percussion_channel" -> {
                    super.push_to_history_stack(
                        "cursor_select_row",
                        listOf(args[0], 0)
                    )
                }
                "unset_percussion_channel" -> {
                    super.push_to_history_stack(
                        "cursor_select_row",
                        listOf(this.percussion_channel!!, 0)
                    )
                }
                "set_channel_instrument" -> {
                    super.push_to_history_stack(
                        "cursor_select_row",
                        listOf(args[0], 0)
                    )
                }
                "unset" -> {
                    super.push_to_history_stack(
                        "cursor_select",
                        listOf(
                            args[0] as BeatKey,
                            args[1] as List<Int>
                        )
                    )
                }
                "set_event" -> {
                    super.push_to_history_stack(
                        "cursor_select",
                        listOf(
                            args[0] as BeatKey,
                            args[1] as List<Int>
                        )
                    )
                }
                "set_percussion_event" -> {
                    super.push_to_history_stack(
                        "cursor_select",
                        listOf(
                            args[0] as BeatKey,
                            args[1] as List<Int>
                        )
                    )
                }
                "insert_beat" -> {
                    super.push_to_history_stack(
                        "cursor_select_column",
                        listOf(args[0] as Int)
                    )
                }
                "remove_beat" -> {
                    var x = min(args[0] as Int, this.opus_beat_count - 1)
                    super.push_to_history_stack(
                        "cursor_select_column",
                        listOf(x)
                    )
                }
                else -> { }
            }
            super.push_to_history_stack(func_name, args)
        }
    }


    private fun ui_unset_cursor_focus() {
        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val adapter = rvBeatTable.adapter as BeatColumnAdapter
        adapter.unset_cursor_focus()
    }

    private fun ui_set_cursor_focus() {
        if (this.simple_ui_locked()) {
            return
        }
        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val adapter = rvBeatTable.adapter as BeatColumnAdapter
        adapter.set_cursor_focus()
    }


    // Cursor Functions ////////////////////////////////////////////////////////////////////////////
    fun cursor_clear() {
        this.ui_unset_cursor_focus()
        this.cursor.clear()
        this.withFragment {
            it.clearContextMenu()
        }
    }
    fun cursor_select_row(channel: Int, line_offset: Int) {
        this.ui_unset_cursor_focus()
        this.cursor.select_row(channel, line_offset)
        this.ui_set_cursor_focus()
        this.withFragment {
            it.setContextMenu_line()
        }
    }

    fun cursor_select_column(beat: Int) {
        this.ui_unset_cursor_focus()
        this.cursor.select_column(beat)
        this.ui_set_cursor_focus()

        this.withFragment {
            it.setContextMenu_column()
        }
    }

    fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        this.ui_unset_cursor_focus()
        this.cursor.select(beat_key, position)
        this.ui_set_cursor_focus()

        this.withFragment {
            it.setContextMenu_leaf()
        }
    }

    fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.ui_unset_cursor_focus()
        this.cursor.select_range(beat_key_a, beat_key_b)
        this.ui_set_cursor_focus()
    }

    fun get_tree(): OpusTree<OpusEvent> {
        return this.get_tree(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }

    fun unset() {
        this.unset(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }

    fun convert_event_to_absolute() {
        this.convert_event_to_absolute(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }

    fun convert_event_to_relative() {
        this.convert_event_to_relative(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }

    fun set_event(event: OpusEvent) {
        this.set_event(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            event
        )
    }

    fun set_percussion_event() {
        this.set_percussion_event(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }

    fun unlink_beat() {
        this.unlink_beat(this.cursor.get_beatkey())
    }

    fun clear_link_pool() {
        this.clear_link_pool(
            this.cursor.get_beatkey()
        )
    }

    fun set_percussion_instrument(instrument: Int) {
        this.set_percussion_instrument(
            this.cursor.line_offset,
            instrument
        )
    }

    fun split_tree(splits: Int) {
        this.split_tree(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            splits
        )
    }

    fun insert_after(count: Int) {
        this.insert_after(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            count
        )
    }

    fun remove(count: Int) {
        this.remove(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            count
        )
    }
    fun insert_line(count: Int) {
        this.new_line(
            this.cursor.channel,
            this.cursor.line_offset + 1,
            count
        )
    }

    fun remove_line(count: Int) {
        this.remove_line(
            this.cursor.channel,
            this.cursor.line_offset,
            count
        )
    }

    fun remove_beat_at_cursor(count: Int) {
        this.remove_beat(this.cursor.beat, count)
    }

    fun insert_beat_at_cursor(count: Int) {
        this.insert_beat(
            this.cursor.beat + 1,
            count
        )
    }
    // End Cursor Functions ////////////////////////////////////////////////////////////////////////

    //fun process_queued_label_updates() {
    //    val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
    //    val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter

    //    val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
    //    val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter

    //    while (ui_label_queue.isNotEmpty()) {
    //        var (action, index) = ui_label_queue.removeFirst()
    //        when (action) {
    //            LabelUpdate.AddColumn -> {
    //                rvBeatTable_adapter.addBeatColumn(index)
    //            }
    //            LabelUpdate.AddLine -> {
    //                rvLineLabels_adapter.addLineLabel()
    //            }
    //            LabelUpdate.PopColumn -> {
    //                rvBeatTable_adapter.removeBeatColumn(index)
    //            }
    //            LabelUpdate.PopLine -> {
    //                rvLineLabels_adapter.removeLineLabel(index)
    //            }
    //        }
    //    }
    //}


}