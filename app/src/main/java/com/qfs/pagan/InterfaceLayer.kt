package com.qfs.pagan
import android.content.res.Configuration
import android.view.View
import android.widget.TextView
import com.qfs.apres.Midi
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.HistoryLayer
import com.qfs.pagan.opusmanager.HistoryToken
import com.qfs.pagan.opusmanager.LoadedJSONData
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import java.lang.Integer.max
import java.lang.Integer.min

class InterfaceLayer(var activity: MainActivity): HistoryLayer() {
    companion object {
        val UI_LOCK_FULL = 0
        val UI_LOCK_PARTIAL = 1
    }
    private var _ui_lock_stack = mutableListOf<Int>()
    private var _queued_cursor_selection: Pair<HistoryToken, List<Int>>? = null
    var relative_mode: Int = 0
    var cursor = OpusManagerCursor()
    var first_load_done = false

    private fun get_ui_lock_level(): Int? {
        return if (this._ui_lock_stack.isEmpty()) {
            null
        } else {
            this._ui_lock_stack.max()
        }
    }
    private fun runOnUiThread(callback: (MainActivity) -> Unit) {
        var main = this.activity
        main.runOnUiThread {
            callback(main)
        }
    }

    private fun <T> surpress_ui(level: Int = UI_LOCK_FULL, callback:(InterfaceLayer) -> T): T {
        this._ui_lock_stack.add(level)
        try {
            val output = callback(this)
            this._ui_lock_stack.removeLast()
            return output
        } catch (e: Exception) {
            this._ui_lock_stack.removeLast()
            throw e
        }
    }

    override fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>) {
        super.set_channel_instrument(channel, instrument)

        this.runOnUiThread { main: MainActivity ->
            main.update_channel_instruments()
            main.populate_active_percussion_names()
            var channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
            if (channel_recycler.adapter != null) {
                (channel_recycler.adapter as ChannelOptionAdapter).notifyItemChanged(channel)
            }
        }
    }

    override fun set_project_name(new_name: String) {
        super.set_project_name(new_name)
        this.runOnUiThread { main: MainActivity ->
            main.update_title_text()
        }
    }

    override fun set_transpose(new_transpose: Int) {
        super.set_transpose(new_transpose)

        this.runOnUiThread { main ->
            main.findViewById<TextView>(R.id.btnTranspose).text = main.getString(R.string.label_transpose, new_transpose)
        }
    }

    override fun set_tempo(new_tempo: Float) {
        super.set_tempo(new_tempo)
        this.runOnUiThread { main ->
            main.findViewById<TextView>(R.id.tvTempo).text = main.getString(R.string.label_bpm, new_tempo.toInt())
        }
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        super.unset(beat_key, position)

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { main: MainActivity ->
                    this.get_editor_table()?.notify_cell_change(beat_key)
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.notify_cell_change(beat_key, true)
            }
            UI_LOCK_FULL -> { }
        }

    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        if (!this.activity.configuration.show_percussion && this.is_percussion(beat_key.channel)) {
            this.make_percussion_visible()
        }

        super.replace_tree(beat_key, position, tree)

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { main: MainActivity ->
                    this.get_editor_table()?.notify_cell_change(beat_key)
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.notify_cell_change(beat_key, true)
            }
            UI_LOCK_FULL -> { }
        }
    }


    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        if (!this.activity.configuration.show_percussion && this.is_percussion(beat_key.channel)) {
            this.make_percussion_visible()
        }

        super.set_event(beat_key, position, event)

        // If the OM is applying history, change the relative mode, otherwise leave it.
        if (this.history_cache.isLocked()) {
            this.set_relative_mode(event)
        }
        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { main: MainActivity ->
                    this.get_editor_table()?.notify_cell_change(beat_key)
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.notify_cell_change(beat_key, true)
            }
            UI_LOCK_FULL -> { }
        }
    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        super.set_percussion_event(beat_key, position)
        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { main: MainActivity ->
                    this.get_editor_table()?.notify_cell_change(beat_key)
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.notify_cell_change(beat_key, true)
            }
            UI_LOCK_FULL -> { }
        }
    }

    override fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        super.set_percussion_instrument(line_offset, instrument)

        if (this.get_ui_lock_level() == UI_LOCK_FULL) {
            return
        }

        this.runOnUiThread { main: MainActivity ->
            val btnChoosePercussion: TextView? = main.findViewById(R.id.btnChoosePercussion)
            if (btnChoosePercussion != null) {
                if (main.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // Need to call get_drum name to repopulate instrument list if needed
                    main.get_drum_name(instrument)

                    if (instrument < 10) {
                        btnChoosePercussion.text = "!0$instrument"
                    } else {
                        btnChoosePercussion.text = "!$instrument"
                    }
                } else {
                    btnChoosePercussion.text = main.getString(
                        R.string.label_choose_percussion,
                        instrument,
                        main.get_drum_name(instrument)
                    )
                }
                this.get_editor_table()?.update_line_label(this.channels.size - 1, line_offset)
            }
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        super.split_tree(beat_key, position, splits)

        if (this.is_percussion(beat_key.channel) && !this.activity.configuration.show_percussion) {
            return
        }

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { main: MainActivity ->
                    this.get_editor_table()?.notify_cell_change(beat_key)
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.notify_cell_change(beat_key, true)
            }
            UI_LOCK_FULL -> { }
        }
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        super.insert_after(beat_key, position)

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { main: MainActivity ->
                    this.get_editor_table()?.notify_cell_change(beat_key)
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.notify_cell_change(beat_key, true)
            }
            UI_LOCK_FULL -> { }
        }
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        super.insert(beat_key, position)
        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { main: MainActivity ->
                    this.get_editor_table()?.notify_cell_change(beat_key)
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.notify_cell_change(beat_key, true)
            }
            UI_LOCK_FULL -> { }
        }
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)
        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { main: MainActivity ->
                    this.get_editor_table()?.notify_cell_change(beat_key)
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.notify_cell_change(beat_key, true)
            }
            UI_LOCK_FULL -> { }
        }
    }

    override fun new_line(channel: Int, line_offset: Int?): OpusChannel.OpusLine {
        val output = super.new_line(channel, line_offset)
        val abs_offset = this.get_abs_offset(
            channel,
            line_offset ?: (this.channels[channel].lines.size - 1)
        )
        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { main: MainActivity ->
                    this.get_editor_table()?.new_row(abs_offset, output)
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.new_row(abs_offset, output, true)
            }
            UI_LOCK_FULL -> { }
        }
        return output
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusChannel.OpusLine) {
        if (!this.activity.configuration.show_percussion && this.is_percussion(channel)) {
            this.make_percussion_visible()
        }
        // Need to clear cursor before change since the way the editor_table updates
        // Cursors doesn't take into account changes to row count
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        super.insert_line(channel, line_offset, line)

        val abs_offset = this.get_abs_offset( channel, line_offset )
        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { main: MainActivity ->
                    this.get_editor_table()?.new_row(abs_offset, line)
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.new_row(abs_offset, line, true)
            }
            UI_LOCK_FULL -> { }
        }

        if (bkp_cursor.mode == OpusManagerCursor.CursorMode.Row) {
            this.cursor_select_row(bkp_cursor.channel, bkp_cursor.line_offset)
        }
    }

    /*
     TODO: move cursor_select_row out of here. it won't break anything right now since move_line
        is built out of multiple other remembered function but i'd like to get some consistency and not
        have any cursor selection inside of context-independent functions()
     */
    override fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        try {
            super.move_line(channel_old, line_old, channel_new, line_new)

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
        if (!this.activity.configuration.show_percussion && this.is_percussion(channel)) {
            this.make_percussion_visible()
        }
        // Need to clear cursor before change since the way the editor_table updates
        // Cursors doesn't take into account changes to row count
        val bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        val abs_line = this.get_abs_offset(channel, line_offset)

        val output = try {
            super.remove_line(channel, line_offset)
        } catch (e: OpusChannel.LastLineException) {
            throw e
        }

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { main: MainActivity ->
                    this.get_editor_table()?.remove_row(abs_line)
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.remove_row(abs_line, true)
            }
            UI_LOCK_FULL -> { }
        }

        // TODO: SHould be behind ui lock?
        this.activity.update_channel_instruments()

        if (bkp_cursor.mode == OpusManagerCursor.CursorMode.Row) {
            if (bkp_cursor.channel < this.channels.size) {
                if (bkp_cursor.line_offset < this.channels[bkp_cursor.channel].size) {
                    this.cursor_select_row(bkp_cursor.channel, bkp_cursor.line_offset)
                } else {
                    this.cursor_select_row(bkp_cursor.channel, this.channels[bkp_cursor.channel].size - 1)
                }
            } else {
                this.cursor_select_row(this.channels.size - 1, this.channels.last().size - 1)
            }
        }


        return output
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        this.cursor_clear()
        val notify_index = if (channel == null) {
            if (this.channels.isEmpty()) {
                0
            } else {
                this.channels.size - 1
            }
        } else {
            min(channel, this.channels.size)
        }

        super.new_channel(channel, lines, uuid)

        val editor_table = this.get_editor_table()
        val line_list = mutableListOf<OpusChannel.OpusLine>()
        for (i in 0 until lines) {
            line_list.add(this.channels[notify_index].lines[i])
        }
        val y = this.get_abs_offset(notify_index, 0)

        // TODO: Should be behind ui lock?
        this.activity.update_channel_instruments(notify_index)

        when (this.get_ui_lock_level()) {
            null,
            UI_LOCK_PARTIAL -> {
                this.runOnUiThread { main ->
                    var channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                    if (channel_recycler.adapter != null) {
                        var channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                        channel_adapter.add_channel()
                    }
                }
            }
            UI_LOCK_FULL -> {}
        }

        if (this.is_percussion(notify_index) && !this.activity.configuration.show_percussion) {
            return
        }

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { main ->
                    editor_table?.new_channel_rows(y, line_list)
                }
            }
            UI_LOCK_PARTIAL -> {
                editor_table?.new_channel_rows(y, line_list, true)
            }
            UI_LOCK_FULL -> {}
        }
    }

    override fun remove_beat(beat_index: Int) {
        // Need to clear cursor before change since the way the editor_table updates
        // Cursors doesn't take into account changes to column count
        var bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        super.remove_beat(beat_index)

        val editor_table = this.get_editor_table() ?: return

        when (this.get_ui_lock_level()) {
            UI_LOCK_FULL -> { }
            UI_LOCK_PARTIAL -> {
                editor_table.remove_column(beat_index, true)
                editor_table.update_cursor(this.cursor)
            }
            else -> {
                this.runOnUiThread {
                    editor_table.remove_column(beat_index)
                    editor_table.update_cursor(this.cursor)
                }
            }
        }

        if (bkp_cursor.mode == OpusManagerCursor.CursorMode.Column) {
            if (bkp_cursor.beat < this.beat_count) {
                this.cursor_select_column(bkp_cursor.beat)
            } else {
                this.cursor_select_column(this.beat_count - 1)
            }
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        // Need to clear cursor before change since the way the editor_table updates
        // Cursors doesn't take into account changes to column count
        var bkp_cursor = this.cursor.copy()
        this.cursor_clear()

        super.insert_beat(beat_index, beats_in_column)

        val editor_table = this.get_editor_table() ?: return

        when (this.get_ui_lock_level()) {
            UI_LOCK_FULL -> { }
            UI_LOCK_PARTIAL -> {
                editor_table.new_column(beat_index, true)
                editor_table.update_cursor(this.cursor)
            }
            else -> {
                this.runOnUiThread {
                    editor_table.new_column(beat_index)
                    editor_table.update_cursor(this.cursor)
                }
            }
        }

        if (bkp_cursor.mode == OpusManagerCursor.CursorMode.Column) {
            this.cursor_select_column(beat_index + 1)
        }
    }

    override fun new() {
        try {
            this.surpress_ui {
                super.new()
            }
        } catch (e: Exception) {
            throw e
        }
        this.first_load_done = true
        val new_path = this.activity.get_new_project_path()
        this.path = new_path

        this.runOnUiThread { main: MainActivity ->
            main.setup_project_config_drawer()
            main.validate_percussion_visibility()
            main.update_menu_options()

            val editor_table = this.get_editor_table()
            editor_table?.clear()
            editor_table?.setup()
            editor_table?.precise_scroll(0, 0, 0, 0)

            main.update_channel_instruments()
            this.withFragment {
                it.clearContextMenu()
            }
        }
    }

    override fun import_midi(midi: Midi) {
        try {
            this.surpress_ui {
                super.import_midi(midi)
            }
        } catch (e: Exception) {
            throw e
        }

        this.first_load_done = true
        this.runOnUiThread { main: MainActivity ->
            main.setup_project_config_drawer()
            main.validate_percussion_visibility()
            main.update_menu_options()

            val editor_table = this.get_editor_table()
            editor_table?.setup()
            editor_table?.precise_scroll(0, 0, 0, 0)

            main.update_channel_instruments()
            this.withFragment {
                it.clearContextMenu()
            }
        }
    }

    override fun load_json(json_data: LoadedJSONData) {
        try {
            this.surpress_ui {
                super.load_json(json_data)
            }
        } catch (e: Exception) {
            throw e
        }

        this.first_load_done = true
        this.runOnUiThread { main: MainActivity ->
            main.setup_project_config_drawer()
            main.validate_percussion_visibility()
            main.update_menu_options()

            val editor_table = this.get_editor_table()
            editor_table?.setup()
            editor_table?.precise_scroll(0, 0, 0, 0)

            main.update_channel_instruments()
            this.withFragment {
                it.clearContextMenu()
            }
        }
    }

    override fun remove_channel(channel: Int) {
        this.cursor_clear()

        val y = try {
            this.get_abs_offset(channel, 0)
        } catch (e: IndexOutOfBoundsException) {
            this.get_total_line_count()
        }

        val lines = this.channels[channel].size

        super.remove_channel(channel)

        when (this.get_ui_lock_level()) {
            null,
            UI_LOCK_PARTIAL -> {
                this.runOnUiThread { main ->
                    var channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                    if (channel_recycler.adapter != null) {
                        var channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                        channel_adapter.remove_channel(channel)
                    }
                }
            }
            UI_LOCK_FULL -> {}
        }

        val editor_table = this.get_editor_table() ?: return
        when (this.get_ui_lock_level()) {
            UI_LOCK_FULL -> {}
            UI_LOCK_PARTIAL -> {
                editor_table.remove_channel_rows(y, lines, true)
            }
            null -> {
                this.runOnUiThread { main ->
                    editor_table.remove_channel_rows(y, lines)
                }
            }
        }
    }

    override fun clear() {
        this.surpress_ui {
            super.clear()
            this.cursor_clear()
        }
        this.get_editor_table()?.clear()
        this.runOnUiThread { main ->
            var channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
            if (channel_recycler.adapter != null) {
                var channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                channel_adapter.clear()
            }
        }
    }

    override fun unlink_beat(beat_key: BeatKey) {
        val update_keys = this.get_all_linked(beat_key).toMutableList()
        update_keys.remove(beat_key)
        super.unlink_beat(beat_key)

        val editor_table = this.get_editor_table() ?: return
        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread {
                    // Need to run update on both the beat_key and *any* of its former link pool
                    editor_table.notify_cell_change(beat_key)
                    if (update_keys.isNotEmpty()) {
                        editor_table.notify_cell_change(update_keys.first())
                    }
                }
            }
            UI_LOCK_PARTIAL -> {
                editor_table.notify_cell_change(beat_key, true)
                if (update_keys.isNotEmpty()) {
                    editor_table.notify_cell_change(update_keys.first(), true)
                }
            }
            UI_LOCK_FULL -> {}
        }

    }

    private fun <T> withFragment(callback: (EditorFragment) -> T): T? {
        val fragment = this.activity.get_active_fragment()
        return if (fragment is EditorFragment) {
            callback(fragment)
        } else {
            null
        }
    }

    // For history when we don't want to worry about the cursor
    private fun queue_cursor_select(token: HistoryToken, args: List<Int>) {
        this._queued_cursor_selection = Pair(token, args)
    }

    private fun apply_queued_cursor_select() {
        if (this._queued_cursor_selection == null) {
            return
        }
        val (token, args) = this._queued_cursor_selection!!
        this._queued_cursor_selection = null
        when (token) {
            HistoryToken.CURSOR_SELECT_ROW -> {
                this.cursor_select_row(args[0], args[1])
            }
            HistoryToken.CURSOR_SELECT_COLUMN -> {
                this.cursor_select_column(args[0])
            }
            HistoryToken.CURSOR_SELECT -> {
                this.cursor_select(
                    BeatKey(
                        args[0],
                        args[1],
                        args[2]
                    ),
                    args.subList(3, args.size)
                )
            }
            else -> {}
        }
    }

    override fun apply_undo() {
        super.apply_undo()
        this.apply_queued_cursor_select()
    }

    override fun apply_history_node(current_node: HistoryCache.HistoryNode, depth: Int)  {
        when (current_node.token) {
            HistoryToken.CURSOR_SELECT_ROW -> {
                this.queue_cursor_select(
                    current_node.token,
                    this.checked_cast<List<Int>>(current_node.args)
                )
            }
            HistoryToken.CURSOR_SELECT -> {
                val beat_key = current_node.args[0] as BeatKey
                val args = mutableListOf<Int>(beat_key.channel, beat_key.line_offset, beat_key.beat)
                val position = this.checked_cast<List<Int>>(current_node.args[1])
                args.addAll(position)

                this.queue_cursor_select(
                    current_node.token,
                    args
                )
            }
            HistoryToken.CURSOR_SELECT_COLUMN -> {
                this.queue_cursor_select(
                    current_node.token,
                    this.checked_cast<List<Int>>(current_node.args)
                )
            }
            else -> { }
        }
        super.apply_history_node(current_node, depth)
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
                    val line_offset = min(
                        args[1] as Int,
                        this.channels[channel].size - 1
                    )
                    this.push_to_history_stack(
                        HistoryToken.CURSOR_SELECT_ROW,
                        listOf(
                            channel,
                            max(0, line_offset - 1)
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
                HistoryToken.SET_EVENT_DURATION -> {
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
                    val x = max(0, min(args[0] as Int, this.beat_count - 2))
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

    fun set_relative_mode(event: OpusEvent) {
        if (this.activity.configuration.relative_mode) {
            this.relative_mode = if (!event.relative) {
                0
            } else if (event.note >= 0) {
                1
            } else {
                2
            }
        } else {
            this.relative_mode = 0
        }
    }
    // Cursor Functions ////////////////////////////////////////////////////////////////////////////
    fun cursor_clear() {
        this.cursor.clear()
        if (this.get_ui_lock_level() != UI_LOCK_FULL) {
            this.runOnUiThread {
                val editor_table = this.get_editor_table()
                editor_table?.update_cursor(this.cursor)
                this.withFragment {
                    it.clearContextMenu()
                }
            }
        }

    }

    fun cursor_select_row(channel: Int, line_offset: Int) {
        if (!this.activity.configuration.show_percussion && this.is_percussion(channel)) {
            this.make_percussion_visible()
        }

        this.cursor.select_row(channel, line_offset)

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            val editor_table = this.get_editor_table()
            editor_table?.update_cursor(this.cursor)

            this.withFragment {
                it.setContextMenu_line()
            }
            editor_table?.scroll_to_position(y = this.get_abs_offset(channel, line_offset))
        }
    }

    fun cursor_select_column(beat: Int, force_scroll: Boolean = false) {
        this.cursor.select_column(beat)

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            this.withFragment {
                it.setContextMenu_column()
            }
            val editor_table = this.get_editor_table() ?: return@runOnUiThread
            editor_table.update_cursor(this.cursor)
            editor_table.scroll_to_position(x = beat, force = force_scroll)
        }
    }

    fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        if (!this.activity.configuration.show_percussion && this.is_percussion(beat_key.channel)) {
            this.make_percussion_visible()
        }

        this.cursor.select(beat_key, position)

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            this.withFragment {
                it.setContextMenu_leaf()
            }

            val editor_table = this.get_editor_table() ?: return@runOnUiThread
            editor_table.update_cursor(this.cursor)
            editor_table.scroll_to_position(beat_key, position)
        }
    }

    fun cursor_select_to_link(beat_key: BeatKey) {
        this.cursor.select_to_link(beat_key)

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            this.withFragment {
                it.setContextMenu_linking()
            }
            val editor_table = this.get_editor_table() ?: return@runOnUiThread
            editor_table.update_cursor(this.cursor)
        }
    }
    fun cursor_select_range_to_link(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.cursor.select_range(beat_key_a, beat_key_b)

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            this.withFragment {
                it.setContextMenu_linking()
            }
            val editor_table = this.get_editor_table() ?: return@runOnUiThread
            editor_table.update_cursor(this.cursor)

        }
    }

    fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.cursor.select_range(beat_key_a, beat_key_b)

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            this.get_editor_table()?.update_cursor(this.cursor)
        }
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
        if (this.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            this.unlink_beat(this.cursor.get_beatkey())
        } else if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            this.unlink_range(cursor.range!!.first, cursor.range!!.second)
        }
    }

    fun clear_link_pool() {
        if (this.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            val beat_key = this.cursor.get_beatkey()
            this.clear_link_pool(beat_key)
        } else if (this.cursor.mode == OpusManagerCursor.CursorMode.Range) {
            val beat_key = this.cursor.range!!.first
            this.clear_link_pools_by_range(
                beat_key,
                this.cursor.range!!.second
            )
        }
    }

   override fun clear_link_pool(beat_key: BeatKey) {
       val update_keys = this.get_all_linked(beat_key).toMutableList()
       super.clear_link_pool(beat_key)

       val editor_table = this.get_editor_table() ?: return
       when (this.get_ui_lock_level()) {
           null -> {
               this.runOnUiThread {
                   // Need to run update on both the beat_key and *any* of its former link pool
                   for (unlinked in update_keys) {
                       editor_table.notify_cell_change(unlinked)
                   }
               }
           }
           UI_LOCK_PARTIAL -> {
               for (unlinked in update_keys) {
                   editor_table.notify_cell_change(unlinked, true)
               }
           }
           UI_LOCK_FULL -> {}
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

        this.remove(beat_key, position, count)

        this.cursor_select(
            beat_key,
            this.get_first_position(beat_key, cursor_position)
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
        this.insert_beat(this.cursor.beat + 1, count)
    }
    // End Cursor Functions ////////////////////////////////////////////////////////////////////////

    override fun save(path: String?) {
        super.save(path)
        val btnDeleteProject = this.activity.findViewById<View>(R.id.btnDeleteProject)
        val btnCopyProject = this.activity.findViewById<View>(R.id.btnCopyProject)
        btnDeleteProject.visibility = View.VISIBLE
        btnCopyProject.visibility = View.VISIBLE
    }

    fun is_selected(beat_key: BeatKey, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat_key.beat
            }
            OpusManagerCursor.CursorMode.Row -> {
                this.cursor.channel == beat_key.channel && this.cursor.line_offset == beat_key.line_offset
            }
            OpusManagerCursor.CursorMode.Range -> {
                beat_key in this.get_beatkeys_in_range(this.cursor.range!!.first, this.cursor.range!!.second)
            }
            OpusManagerCursor.CursorMode.Single -> {
                val cposition = this.cursor.get_position()
                this.cursor.get_beatkey() == beat_key && position.size >= cposition.size && position.subList(0, cposition.size) == cposition
            }
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }
        }
    }

    fun get_editor_table(): EditorTable? {
        return try {
            this.activity.findViewById<EditorTable>(R.id.etEditorTable)
        } catch (e: Exception) {
            null
        }
    }

    fun link_beat(beat_key: BeatKey) {
        if (this.cursor.is_linking_range()) {
            val (first, second) = this.cursor.range!!
            this.link_beat_range(beat_key, first, second)
        } else if (this.cursor.is_linking) {
            this.link_beats(beat_key, this.cursor.get_beatkey())
        } else {
            // TODO: Raise Error
        }
    }

    fun move_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_linking_range()) {
            val (first, second) = this.cursor.range!!
            this.move_beat_range(beat_key, first, second)
        } else if (this.cursor.is_linking) {
            this.move_leaf(this.cursor.get_beatkey(), listOf(), beat_key, listOf())
        } else {
            // TODO: Raise Error
        }
    }

    fun copy_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_linking_range()) {
            val (first, second) = this.cursor.range!!
            this.overwrite_beat_range(beat_key, first, second)
        } else if (this.cursor.is_linking) {
            this.overwrite_beat(beat_key, this.cursor.get_beatkey())
        } else {
            // TODO: Raise Error
        }
    }

    override fun link_beats(beat_key: BeatKey, target: BeatKey) {
        super.link_beats(beat_key, target)
        when (this.get_ui_lock_level()) {
            UI_LOCK_FULL -> { }
            UI_LOCK_PARTIAL -> {
                for (linked_key in this.get_all_linked(beat_key)) {
                    this.get_editor_table()?.notify_cell_change(linked_key)
                }
            }
            null -> {
                this.runOnUiThread {
                    for (linked_key in this.get_all_linked(beat_key)) {
                        this.get_editor_table()?.notify_cell_change(linked_key)
                    }
                }
            }
        }
    }


    fun get_visible_channels(): List<OpusChannel> {
        return if (this.activity.configuration.show_percussion) {
            this.channels
        } else {
            this.channels.subList(0, max(this.channels.size - 1, 0))
        }
    }

    fun get_visible_line_count(): Int {
        var total = 0
        for (channel in this.get_visible_channels()) {
            for (line in channel.lines) {
                total += 1
            }
        }
        return total
    }

    override fun set_radix(radix: Int, mod_events: Boolean) {
        super.set_radix(radix, mod_events)

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread { main: MainActivity ->
            main.findViewById<TextView>(R.id.btnRadix).text = main.getString(R.string.label_radix, radix)
            this.withFragment {
                it.reset_context_menu()
            }
        }
    }

    fun make_percussion_visible() {
        var main = this.activity
        main.configuration.show_percussion = true
        main.save_configuration()

        this.runOnUiThread {
            val channel_option_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
            val adapter = channel_option_recycler.adapter!! as ChannelOptionAdapter
            adapter.notifyItemChanged(adapter.itemCount - 1)

            val editor_table = main.findViewById<EditorTable>(R.id.etEditorTable)
            editor_table.update_percussion_visibility()
        }
    }
}