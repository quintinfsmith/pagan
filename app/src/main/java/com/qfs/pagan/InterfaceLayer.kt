package com.qfs.pagan
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.apres.Midi
import com.qfs.pagan.opusmanager.*
import com.qfs.pagan.structure.OpusTree
import java.lang.Integer.max
import java.lang.Integer.min

class InterfaceLayer(var activity: MainActivity): HistoryLayer() {
    private var simple_ui_lock = 0
    var relative_mode: Int = 0
    var cursor = Cursor()
    private var queued_location_stamp: Pair<BeatKey, List<Int>?>? = null
    var first_load_done = false

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

    override fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>) {
        this.activity.loading_reticle()
        super.set_channel_instrument(channel, instrument)

        val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)
        rvActiveChannels.adapter?.notifyItemChanged(channel)

        if (this.is_percussion(channel)) {
            withFragment {
                it.get_main().populate_active_percussion_names()
            }
        }

        this.activity.update_channel_instruments()
        this.activity.cancel_reticle()
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

        val btnChoosePercussion: TextView? = this.activity.findViewById(R.id.btnChoosePercussion)
        if (btnChoosePercussion != null) {
            btnChoosePercussion.text = this.activity.getString(
                R.string.label_choose_percussion,
                instrument,
                this.activity.get_drum_name(instrument)
            )

            this.update_line_labels()
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        super.split_tree(beat_key, position, splits)
        this.ui_refresh_beat_labels(beat_key)
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        super.insert_after(beat_key, position)
        this.ui_refresh_beat_labels(beat_key)
    }
    override fun insert(beat_key: BeatKey, position: List<Int>) {
        super.insert(beat_key, position)
        this.ui_refresh_beat_labels(beat_key)
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)
        this.ui_refresh_beat_labels(beat_key)
    }

    override fun new_line(channel: Int, line_offset: Int?): OpusChannel.OpusLine {
        val output = super.new_line(channel, line_offset)

        this.ui_add_line_label()
        this.ui_notify_visible_changes()
        return output
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusChannel.OpusLine) {
        this.ui_unset_cursor_focus()
        super.insert_line(channel, line_offset, line)
        this.ui_add_line_label()
        this.ui_notify_visible_changes()
        this.ui_set_cursor_focus()
    }

    override fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        try {
            this.surpress_ui {
                super.move_line(channel_old, line_old, channel_new, line_new)
            }
            this.ui_notify_visible_changes()

            this.cursor_select_row(
                channel_new,
                if (channel_old == channel_new) {
                    if (line_old < line_new) {
                        line_new - 1
                    } else {
                        line_new
                    }
                } else if (this.channels[channel_new].size == 1) {
                    0
                } else {
                    line_new
                }
            )
        } catch (exception: IncompatibleChannelException) {
            // pass
        }
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusChannel.OpusLine {

        this.ui_unset_cursor_focus()
        this.ui_remove_line_label(channel, line_offset)

        val output = try {
            super.remove_line(channel, line_offset)
        } catch (e: OpusChannel.LastLineException) {
            this.ui_add_line_label()
            this.ui_notify_visible_changes()
            this.ui_set_cursor_focus()

            throw e
        }

        val cursor = this.cursor
        if (cursor.line_offset != 0 && cursor.line_offset == this.channels[cursor.channel].size) {
            when (cursor.mode) {
                Cursor.CursorMode.Row -> {
                    cursor.line_offset -= 1
                }
                Cursor.CursorMode.Single -> {
                    cursor.line_offset -= 1
                    cursor.position = this.get_first_position(this.cursor.get_beatkey(), listOf())
                }
                else -> {}
            }
        }

        this.ui_notify_visible_changes()
        this.ui_set_cursor_focus()
        this.activity.update_channel_instruments()

        return output
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        val notify_index = if (channel == null) {
            this.channels.size
        } else {
            min(channel, this.channels.size)
        }

        super.new_channel(channel, lines, uuid)
        for (i in 0 until lines) {
            this.ui_add_line_label()
        }
        this.ui_notify_visible_changes()
        val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)
        this.activity.update_channel_instruments(notify_index)

        rvActiveChannels.adapter?.notifyItemChanged(notify_index - 1)
        rvActiveChannels.adapter?.notifyItemInserted(notify_index)
    }

    override fun remove_beat(beat_index: Int) {
        this.ui_unset_cursor_focus()
        super.remove_beat(beat_index)
        this.ui_remove_beat(beat_index)
        this.ui_set_cursor_focus()
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        this.ui_unset_cursor_focus()
        super.insert_beat(beat_index, beats_in_column)
        this.ui_add_beat(beat_index)
        this.ui_set_cursor_focus()
    }

    override fun new() {
        this.activity.loading_reticle()
        try {
            this.surpress_ui {
                super.new()
            }
        } catch (e: Exception) {
            this.activity.cancel_reticle()
            throw e
        }
        this.first_load_done = true
        val new_path = this.activity.project_manager.get_new_path()
        this.path = new_path

        this.activity.update_menu_options()
        this.activity.setup_config_drawer()
        this.activity.cancel_reticle()

        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        rvBeatTable.scrollToPosition(0)
        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        rvColumnLabels.scrollToPosition(0)

        this.activity.update_channel_instruments()
        this.withFragment {
            it.clearContextMenu()
        }
    }
    override fun import_midi(midi: Midi) {
        this.activity.loading_reticle()
        try {
            this.surpress_ui {
                super.import_midi(midi)
            }
        } catch (e: Exception) {
            this.activity.cancel_reticle()
            throw e
        }

        this.first_load_done = true
        this.activity.update_menu_options()
        this.activity.setup_config_drawer()

        this.activity.cancel_reticle()

        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        rvBeatTable.scrollToPosition(0)
        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        rvColumnLabels.scrollToPosition(0)

        this.activity.update_channel_instruments()
        this.withFragment {
            it.clearContextMenu()
        }
    }

    override fun load(bytes: ByteArray) {
        this.activity.loading_reticle()
        try {
            this.surpress_ui {
                super.load(bytes)
            }
        } catch (e: Exception) {
            this.activity.cancel_reticle()
            throw e
        }
        this.first_load_done = true
        this.activity.update_menu_options()
        this.activity.setup_config_drawer()
        this.activity.cancel_reticle()

        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        rvBeatTable.scrollToPosition(0)
        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        rvColumnLabels.scrollToPosition(0)
        this.activity.update_channel_instruments()

        this.withFragment {
            it.clearContextMenu()
        }
    }

    override fun load(path: String) {
        this.activity.loading_reticle()
        try {
            this.surpress_ui {
                super.load(path)
            }
        } catch (e: Exception) {
            this.activity.cancel_reticle()
            throw e
        }
        this.first_load_done = true
        this.activity.update_menu_options()
        this.activity.setup_config_drawer()
        this.activity.cancel_reticle()

        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        rvBeatTable.scrollToPosition(0)
        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        rvColumnLabels.scrollToPosition(0)

        this.activity.update_channel_instruments()
        this.withFragment {
            it.clearContextMenu()
        }
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
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelRecyclerView.LineLabelAdapter
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
        val update_keys = this.get_all_linked(beat_key).toMutableList()
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

        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        (rvColumnLabels.adapter as ColumnLabelAdapter).refresh()
    }
    private fun ui_add_beat(beat: Int) {
        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        rvBeatTable_adapter.addBeatColumn(beat)

        //val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        //(rvColumnLabels.adapter as ColumnLabelAdapter).refresh()
    }

    private fun ui_notify_visible_changes() {
        if (this.simple_ui_locked()) {
            return
        }

        val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
        var first = (beat_table.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        var last = (beat_table.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        for (i in max(0, first - 5) .. min(last + 5, this.opus_beat_count - 1)) {
            rvBeatTable_adapter.notifyItemChanged(i)
        }

        val line_labels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        val rvLineLabel_adapter = line_labels.adapter as LineLabelRecyclerView.LineLabelAdapter
        first = (line_labels.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        last = (line_labels.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        for (i in max(0, first - 5) .. min(last + 5, this.opus_beat_count - 1)) {
            rvLineLabel_adapter.notifyItemChanged(i)
        }
    }

    private fun ui_add_line_label() {
        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelRecyclerView.LineLabelAdapter
        rvLineLabels_adapter.addLineLabel()
    }

    private fun ui_remove_line_label(channel: Int, line_offset: Int) {
        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelRecyclerView.LineLabelAdapter
        rvLineLabels_adapter.removeLineLabel(
            try {
                this.get_abs_offset(channel, line_offset)
            } catch (e: IndexOutOfBoundsException) {
                return // no need to remove the label, it doesn't exist
            }
        )
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
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelRecyclerView.LineLabelAdapter
        val start =
            (rvLineLabels.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val end =
            (rvLineLabels.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

        for (i in start..end) {
            rvLineLabels_adapter.notifyItemChanged(i)
        }
    }

    private fun <T> withFragment(callback: (EditorFragment) -> T): T? {
        val fragment = this.activity.getActiveFragment()
        return if (fragment is EditorFragment) {
            callback(fragment)
        } else {
            null
        }
    }

    //private fun init_ui() {
    //    val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
    //    val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter
    //    val rvActiveChannels: RecyclerView = this.activity.findViewById(R.id.rvActiveChannels)

    //    val beat_table = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
    //    val rvBeatTable_adapter = beat_table.adapter as BeatColumnAdapter
    //    //for (i in 0 until this.opus_beat_count) {
    //    //    rvBeatTable_adapter.addBeatColumn(i)
    //    //}

    //    //this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
    //    //    repeat(channel.lines.size) {
    //    //        rvLineLabels_adapter.addLineLabel()
    //    //    }
    //    //    rvActiveChannels.adapter?.notifyItemInserted(i)
    //    //}
    //}

    override fun set_transpose(new_transpose: Int)  {
        super.set_transpose(new_transpose)
        val btnTranspose: TextView = this.activity.findViewById(R.id.btnTranspose)
        btnTranspose.text = this.activity.getString(
            R.string.label_transpose,
            get_number_string(new_transpose, this.RADIX, 2)
        )
    }

    override fun set_tempo(new_tempo: Float) {
        super.set_tempo(new_tempo)

        val tvTempo = this.activity.findViewById<TextView>(R.id.tvTempo)
        tvTempo.text = this.activity.getString(R.string.label_bpm, this.tempo.toInt())
    }

    override fun apply_undo() {
        if (this.has_history()) {
            this.surpress_ui {
                super.apply_undo()
            }
            this.ui_notify_visible_changes()

            if (this.queued_location_stamp != null) {
                val (beat_key, position) = this.queued_location_stamp!!
                this.queued_location_stamp = null

                if (beat_key.beat == -1) { // Row Select
                    this.cursor_select_row(beat_key.channel, beat_key.line_offset, scroll=true)
                } else if (beat_key.channel == -1 || beat_key.line_offset == -1) { // Beat Select
                    this.cursor_select_column(beat_key.beat, scroll=true)
                } else if (position == null) {
                    return
                } else {
                    this.cursor_select(beat_key, position, scroll=true)
                }
            } else {
                this.cursor_clear()
            }

            this.update_line_labels()

        } else {
            // It's just irritating
            //this.activity.feedback_msg(this.activity.getString(R.string.msg_undo_none))
        }
    }

    override fun apply_history_node(current_node: HistoryCache.HistoryNode, depth: Int)  {
        super.apply_history_node(current_node, depth)
        this.queued_location_stamp = when (current_node.token) {
            HistoryToken.CURSOR_SELECT_ROW -> {
                Pair(
                    BeatKey(
                        current_node.args[0] as Int,
                        current_node.args[1] as Int,
                        -1
                    ),
                    null
                )
            }
            HistoryToken.CURSOR_SELECT -> {
                Pair(
                    current_node.args[0] as BeatKey,
                    if (current_node.args[1] is List<*>) {
                        this.checked_cast<List<Int>>(current_node.args[1])
                    } else {
                        return
                    }
                )
            }
            HistoryToken.CURSOR_SELECT_COLUMN -> {
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

    override fun push_to_history_stack(token: HistoryToken, args: List<Any>) {
        if (this.history_cache.isLocked()) {
            return
        }
        var has_cursor_action = true
        this.history_cache.remember {
            when (token) {
                HistoryToken.MOVE_LINE -> {
                    val from_channel = args[0] as Int
                    val from_line = args[1] as Int
                    val to_channel = args[2] as Int
                    val to_line = args[3] as Int
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_ROW,
                        listOf(
                            to_channel,
                            if (from_channel == to_channel && to_line >= from_line) {
                                //if ((from_channel == to_channel || to_channel == this.channels.size - 1) && to_line >= from_line) {
                                to_line - 1
                            } else {
                                to_line
                            }
                        )
                    )
                }
                HistoryToken.INSERT_LINE -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_ROW,
                        listOf(
                            args[0] as Int,
                            args[1] as Int
                        )
                    )
                }
                HistoryToken.REMOVE_LINE -> {
                    val channel = args[0] as Int
                    val line_offset = min(args[1] as Int, this.channels[channel].size - 1)

                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_ROW,
                        listOf(
                            channel,
                            line_offset
                        )
                    )
                }
                HistoryToken.REPLACE_TREE -> {
                    val new_position = this.checked_cast<List<Int>>(args[1]).toMutableList()
                    var tree = this.checked_cast<OpusTree<OpusEvent>>(args[2])
                    while (! tree.is_leaf()) {
                        new_position.add(0)
                        tree = tree[0]
                    }

                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(
                            args[0] as BeatKey,
                            new_position
                        )
                    )
                }
                HistoryToken.SET_PERCUSSION_INSTRUMENT ->{
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_ROW,
                        listOf(
                            this.channels.size - 1,
                            args[0] as Int
                        )
                    )
                }
                HistoryToken.SET_CHANNEL_INSTRUMENT -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_ROW,
                        listOf(args[0], 0)
                    )
                }
                HistoryToken.UNSET -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(
                            args[0] as BeatKey,
                            this.checked_cast<List<Int>>(args[1])
                        )
                    )
                }
                HistoryToken.SET_EVENT -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(
                            args[0] as BeatKey,
                            this.checked_cast<List<Int>>(args[1])
                        )
                    )
                }
                HistoryToken.SET_PERCUSSION_EVENT -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(
                            args[0] as BeatKey,
                            this.checked_cast<List<Int>>(args[1])
                        )
                    )
                }
                HistoryToken.INSERT_BEAT -> {
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_COLUMN,
                        listOf(args[0] as Int)
                    )
                }
                HistoryToken.REMOVE_BEAT -> {
                    val x = max(0, min(args[0] as Int, this.opus_beat_count - 2))
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_COLUMN,
                        listOf(x)
                    )
                }
                HistoryToken.LINK_BEATS -> {
                    val beat_key = args[0] as BeatKey
                    val position = mutableListOf<Int>()
                    var tree = this.get_tree(beat_key,position)
                    while (! tree.is_leaf()) {
                        tree = tree[0]
                        position.add(0)
                    }
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf( beat_key, position )
                    )
                }
                HistoryToken.REMOVE -> {
                    val beat_key = args[0] as BeatKey
                    val position = this.checked_cast<List<Int>>(args[1])

                    val tree = this.get_tree(beat_key, position)
                    val cursor_position = position.toMutableList()
                    if (tree.parent!!.size <= 2) { // Will be pruned
                        cursor_position.removeLast()
                    } else if (position.last() == tree.parent!!.size - 1) {
                        cursor_position[cursor_position.size - 1] -= 1
                    }

                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(beat_key, cursor_position)
                    )
                }
                HistoryToken.INSERT_TREE -> {
                    val beat_key = args[0] as BeatKey
                    val position = this.checked_cast<List<Int>>(args[1])
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT,
                        listOf(beat_key, position)
                    )
                }
                else -> {
                    has_cursor_action = false
                }
            }
            if (has_cursor_action) {
                super.push_to_history_stack(token, args)
            }
        }
        if (! has_cursor_action) {
            //this.history_cache.pop()
            super.push_to_history_stack(token, args)
        }
    }


    private fun ui_unset_cursor_focus() {
        if (this.simple_ui_locked()) {
            return
        }
        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val adapter = rvBeatTable.adapter as BeatColumnAdapter
        adapter.set_cursor_focus(false)

        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        (rvLineLabels.adapter as LineLabelRecyclerView.LineLabelAdapter).set_cursor_focus(false)

        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        (rvColumnLabels.adapter as ColumnLabelAdapter).set_cursor_focus(false)

    }

    private fun ui_set_cursor_focus() {
        if (this.simple_ui_locked()) {
            return
        }
        val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
        val adapter = rvBeatTable.adapter as BeatColumnAdapter
        adapter.set_cursor_focus()

        val rvLineLabels = this.activity.findViewById<RecyclerView>(R.id.rvLineLabels)
        (rvLineLabels.adapter as LineLabelRecyclerView.LineLabelAdapter).set_cursor_focus(true)

        val rvColumnLabels = this.activity.findViewById<RecyclerView>(R.id.rvColumnLabels)
        (rvColumnLabels.adapter as ColumnLabelAdapter).set_cursor_focus(true)
    }


    // Cursor Functions ////////////////////////////////////////////////////////////////////////////
    fun cursor_clear() {
        this.ui_unset_cursor_focus()
        this.cursor.clear()
        this.withFragment {
            it.clearContextMenu()
        }
    }
    fun cursor_select_row(channel: Int, line_offset: Int, scroll: Boolean = false) {
        this.ui_unset_cursor_focus()
        this.cursor.select_row(channel, line_offset)
        this.ui_set_cursor_focus()

        this.withFragment {
            it.setContextMenu_line()
        }

        if (scroll) {
            this.ui_scroll_to_position(
                BeatKey(channel, line_offset, -1),
                listOf()
            )
        }
    }

    fun cursor_select_column(beat: Int, scroll: Boolean = false) {
        this.ui_unset_cursor_focus()
        this.cursor.select_column(beat)
        this.ui_set_cursor_focus()

        this.withFragment {
            it.setContextMenu_column()
        }

        if (scroll) {
            this.ui_scroll_to_position(
                BeatKey(-1, -1, beat),
                listOf()
            )
        }
    }

    fun cursor_select(beat_key: BeatKey, position: List<Int>, scroll: Boolean = false) {
        this.ui_unset_cursor_focus()
        this.cursor.select(beat_key, position)
        this.ui_set_cursor_focus()

        this.withFragment {
            it.setContextMenu_leaf()
        }
        if (scroll) {
            this.ui_scroll_to_position(beat_key, position)
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
        if (this.cursor.mode == Cursor.CursorMode.Single) {
            this.unlink_beat(this.cursor.get_beatkey())
        } else if (this.cursor.mode == Cursor.CursorMode.Range) {
            for (beat_key in this.get_beatkeys_in_range(cursor.range!!.first, cursor.range!!.second)) {
                this.unlink_beat(beat_key)
            }
        }
    }

    fun clear_link_pool() {
        if (this.cursor.mode == Cursor.CursorMode.Single) {
            this.clear_link_pool(
                this.cursor.get_beatkey()
            )
        } else if (this.cursor.mode == Cursor.CursorMode.Range) {
            this.clear_link_pools_by_range(
                this.cursor.range!!.first,
                this.cursor.range!!.second
            )
        }
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
        val cursor = this.cursor
        val beat_key = cursor.get_beatkey()
        val position = cursor.get_position().toMutableList()

        val tree = this.get_tree()
        val cursor_position = position.toMutableList()
        if (tree.parent!!.size <= 2) { // Will be pruned
            cursor_position.removeLast()
        } else if (position.last() == tree.parent!!.size - 1) {
            cursor_position[cursor_position.size - 1] -= 1
        }

        this.remove( beat_key, position, count )
        this.cursor_select(beat_key, cursor_position)
    }
    fun insert_line(count: Int) {
        this.surpress_ui {
            this.new_line(
                this.cursor.channel,
                this.cursor.line_offset + 1,
                count
            )
        }
        this.ui_notify_visible_changes()
    }

    fun remove_line(count: Int) {
        this.surpress_ui {
            this.remove_line(
                this.cursor.channel,
                this.cursor.line_offset,
                count
            )
        }
        this.ui_notify_visible_changes()
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

    override fun save(path: String?) {
        super.save(path)
        val btnDeleteProject = this.activity.findViewById<View>(R.id.btnDeleteProject)
        val btnCopyProject = this.activity.findViewById<View>(R.id.btnCopyProject)
        btnDeleteProject.visibility = View.VISIBLE
        btnCopyProject.visibility = View.VISIBLE
    }

    override fun link_beats(beat_key: BeatKey, target: BeatKey) {
        super.link_beats(beat_key, target)
        if (! this.simple_ui_locked()) {
            this.ui_refresh_beat_labels(beat_key)
            this.ui_refresh_beat_labels(target)
        }
    }

    override fun remove_link_pool(index: Int) {
        val link_pool = this.link_pools[index].toList()
        super.remove_link_pool(index)
        for (beat_key in link_pool) {
            this.ui_refresh_beat_labels(beat_key)
        }
    }

    override fun link_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {
        this.surpress_ui {
            super.link_beat_range_horizontally(channel, line_offset, first_key, second_key)
        }
        if (! this.simple_ui_locked()) {
            this.ui_notify_visible_changes()
        }
    }
}
