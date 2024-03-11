package com.qfs.pagan
import android.content.res.Configuration
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.qfs.apres.Midi
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.LoadedJSONData
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.opusmanager.OpusLayerCursor
import com.qfs.pagan.structure.OpusTree
import java.lang.Integer.max
import java.lang.Integer.min

class OpusLayerInterface : OpusLayerCursor() {
    companion object {
        const val UI_LOCK_FULL = 0
        const val UI_LOCK_PARTIAL = 1
    }

    private var _ui_lock_stack = mutableListOf<Int>()
    var relative_mode: Int = 0
    var first_load_done = false
    private var _in_reload = false
    private var _activity: MainActivity? = null

    fun attach_activity(activity: MainActivity) {
        this._activity = activity
    }

    fun get_activity(): MainActivity? {
        return this._activity
    }

    private fun get_editor_table(): EditorTable? {
        return this._activity?.findViewById<EditorTable>(R.id.etEditorTable)
    }

    private fun get_ui_lock_level(): Int? {
        return if (this._link_deviation_count > 0) {
            UI_LOCK_FULL
        } else if (this._ui_lock_stack.isEmpty()) {
            null
        } else {
            this._ui_lock_stack.max()
        }
    }

    private fun runOnUiThread(callback: (MainActivity) -> Unit) {
        val main = this._activity ?: return
        main.runOnUiThread {
            callback(main)
        }
    }

    private fun <T> surpress_ui(level: Int = UI_LOCK_FULL, callback:(OpusLayerInterface) -> T): T {
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
            main.update_channel_instruments(channel)
            main.populate_active_percussion_names()
            val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
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

    override fun set_tempo(new_tempo: Float) {
        super.set_tempo(new_tempo)
        this.runOnUiThread { main ->
            main.findViewById<TextView>(R.id.tvTempo).text = main.getString(R.string.label_bpm, new_tempo.toInt())
        }
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        super.unset(beat_key, position)

        if (this.get_activity() != null) {
            when (this.get_ui_lock_level()) {
                null -> {
                    this.runOnUiThread { _: MainActivity ->
                        this.get_editor_table()?.notify_cell_change(beat_key)
                    }
                }

                UI_LOCK_PARTIAL -> {
                    this.get_editor_table()?.notify_cell_change(beat_key, true)
                }

                UI_LOCK_FULL -> {}
            }
        }
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<OpusEvent>) {

        val activity = this.get_activity() ?: return super.replace_tree(beat_key, position, tree)
        if (!activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
            this.make_percussion_visible()
        }

        super.replace_tree(beat_key, position, tree)

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { _: MainActivity ->
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
        val activity = this.get_activity() ?: return super.set_event(beat_key, position, event)
        if (!activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
            this.make_percussion_visible()
        }

        super.set_event(beat_key, position, event)

        // If the OM is applying history, change the relative mode, otherwise leave it.
        if (this.history_cache.isLocked()) {
            this.set_relative_mode(event)
        }

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { _: MainActivity ->
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

        if (this.get_activity() == null) {
            return
        }

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { _: MainActivity ->
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

        if (this.get_activity() == null) {
            return
        }

        if (this.get_ui_lock_level() == UI_LOCK_FULL) {
            return
        }

        // TODO: Fix Code duplication with FragmentEditor
        this.runOnUiThread { main: MainActivity ->
            val btnChoosePercussion: TextView? = main.findViewById(R.id.btnChoosePercussion)
            if (btnChoosePercussion != null) {
                if (main.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // Need to call get_drum name to repopulate instrument list if needed
                    main.get_drum_name(instrument)
                    btnChoosePercussion.text = main.getString(R.string.label_short_percussion, instrument)
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

        if (this.get_activity() == null) {
            return
        }

        if (this.is_percussion(beat_key.channel) && !this._activity!!.view_model.show_percussion) {
            return
        }

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { _: MainActivity ->
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

        if (this.get_activity() == null) {
            return
        }

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { _: MainActivity ->
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

        if (this.get_activity() == null) {
            return
        }

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { _: MainActivity ->
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

        if (this.get_activity() == null) {
            return
        }

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { _: MainActivity ->
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

        if (this.get_activity() != null) {
            val abs_offset = this.get_abs_offset(
                channel,
                line_offset ?: (this.channels[channel].lines.size - 1)
            )
            when (this.get_ui_lock_level()) {
                null -> {
                    this.runOnUiThread { _: MainActivity ->
                        this.get_editor_table()?.new_row(abs_offset, output)
                    }
                }

                UI_LOCK_PARTIAL -> {
                    this.get_editor_table()?.new_row(abs_offset, output, true)
                }

                UI_LOCK_FULL -> {}
            }
        }

        return output
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusChannel.OpusLine) {
        val activity = this.get_activity()
        if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
            this.make_percussion_visible()
        }

        super.insert_line(channel, line_offset, line)

        if (activity == null) {
            return 
        }

        val abs_offset = this.get_abs_offset( channel, line_offset )
        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { _: MainActivity ->
                    this.get_editor_table()?.new_row(abs_offset, line)
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.new_row(abs_offset, line, true)
            }
            UI_LOCK_FULL -> { }
        }
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        super.swap_lines(channel_a, line_a, channel_b, line_b)

        this.get_editor_table()?.swap_lines(
            this.get_abs_offset(channel_a, line_a),
            this.get_abs_offset(channel_b, line_b),
        )
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusChannel.OpusLine {
        val activity = this.get_activity()
        if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
            this.make_percussion_visible()
        }

        val abs_line = this.get_abs_offset(channel, line_offset)

        val output = super.remove_line(channel, line_offset)

        if (activity != null) {
            when (this.get_ui_lock_level()) {
                null -> {
                    this.runOnUiThread { _: MainActivity ->
                        this.get_editor_table()?.remove_row(abs_line)
                    }
                }

                UI_LOCK_PARTIAL -> {
                    this.get_editor_table()?.remove_row(abs_line, true)
                }

                UI_LOCK_FULL -> {}
            }

            // TODO: SHould be behind ui lock?
            activity.update_channel_instruments()
        }

        return output
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        val activity = this.get_activity() ?: return super.new_channel(channel, lines, uuid)

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
        activity.update_channel_instruments(notify_index)

        when (this.get_ui_lock_level()) {
            null,
            UI_LOCK_PARTIAL -> {
                this.runOnUiThread { main ->
                    val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                    if (channel_recycler.adapter != null) {
                        val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                        channel_adapter.add_channel()
                    }
                }
            }
            UI_LOCK_FULL -> {}
        }

        if (this.is_percussion(notify_index) && activity.view_model.show_percussion) {
            return
        }

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread {
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
        super.remove_beat(beat_index)

        val editor_table = this.get_editor_table() ?: return

        when (this.get_ui_lock_level()) {
            UI_LOCK_FULL -> { }
            UI_LOCK_PARTIAL -> {
                editor_table.remove_column(beat_index, true)
                editor_table.update_cursor(this.cursor)
            }
            else -> {
                this.runOnUiThread { main ->
                    if (this.beat_count == 1) {
                        main.findViewById<ImageView>(R.id.btnRemoveBeat).visibility = View.GONE
                    }
                    editor_table.remove_column(beat_index)
                    editor_table.update_cursor(this.cursor)
                }
            }
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        val bkp_cursor = this.cursor.copy()
        super.insert_beat(beat_index, beats_in_column)
        val editor_table = this.get_editor_table() ?: return

        when (this.get_ui_lock_level()) {
            UI_LOCK_FULL -> { }
            UI_LOCK_PARTIAL -> {
                editor_table.update_cursor(bkp_cursor)
                editor_table.new_column(beat_index, true)
                editor_table.update_cursor(this.cursor)
            }
            else -> {
                this.runOnUiThread {
                    editor_table.update_cursor(bkp_cursor)
                    editor_table.new_column(beat_index)
                    editor_table.update_cursor(this.cursor)
                }
            }
        }
    }

    override fun insert_beats(beat_index: Int, count: Int) {
        val bkp_cursor = this.cursor.copy()
        this.surpress_ui {
            super.insert_beats(beat_index, count)
        }

        val editor_table = this.get_editor_table() ?: return

        when (this.get_ui_lock_level()) {
            UI_LOCK_FULL -> { }
            UI_LOCK_PARTIAL -> {
                editor_table.update_cursor(bkp_cursor)
                for (i in 0 until count) {
                    editor_table.new_column(beat_index + i, true)
                }
                editor_table.update_cursor(this.cursor)
            }
            else -> {
                this.runOnUiThread {
                    editor_table.update_cursor(bkp_cursor)
                    for (i in 0 until count) {
                        editor_table.new_column(beat_index + i)
                    }
                    editor_table.update_cursor(this.cursor)
                }
            }
        }
    }

    override fun new() {
        val activity = this.get_activity() ?: return super.new()

        this._ui_clear()
        this.surpress_ui {
            super.new()
        }

        activity.view_model.show_percussion = true

        val new_path = activity.get_new_project_path()
        this.path = new_path
    }


    override fun import_midi(midi: Midi) {
        val activity = this.get_activity() ?: return super.import_midi(midi)

        this._ui_clear()
        this.surpress_ui {
            super.import_midi(midi)
        }
        activity.view_model.show_percussion = !(!this.has_percussion() && this.channels.size > 1)
    }

    override fun load_json(json_data: LoadedJSONData) {
        val activity = this.get_activity() ?: return super.load_json(json_data)

        this._ui_clear()
        this.surpress_ui {
            super.load_json(json_data)
        }

        if (! this._in_reload) {
            activity.view_model.show_percussion = !(!this.has_percussion() && this.channels.size > 1)
        }

    }

    fun reload(bytes: ByteArray, path: String) {
        this._in_reload = true
        this.load(bytes, path)
        this._in_reload = false
    }

    override fun on_project_changed() {
        super.on_project_changed()
        this.first_load_done = true

        this.runOnUiThread { main: MainActivity ->
            val editor_table = this.get_editor_table()
            editor_table?.clear()
            editor_table?.precise_scroll(0, 0, 0, 0)

            main.setup_project_config_drawer()
            main.validate_percussion_visibility()
            main.update_menu_options()

            editor_table?.setup()

            main.update_channel_instruments()
            this.withFragment {
                it.clearContextMenu()
            }
        }
    }

    override fun remove_channel(channel: Int) {
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
                    val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                    if (channel_recycler.adapter != null) {
                        val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
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
                this.runOnUiThread {
                    editor_table.remove_channel_rows(y, lines)
                }
            }
        }
    }

    private fun _ui_clear() {
        this.get_editor_table()?.clear()
        this.runOnUiThread { main ->
            val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
            if (channel_recycler.adapter != null) {
                val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                channel_adapter.clear()
            }
        }
    }

    override fun create_link_pool(beat_keys: List<BeatKey>) {
        super.create_link_pool(beat_keys)
        val editor_table = this.get_editor_table() ?: return
        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread {
                    // Need to run update on both the beat_key and *any* of its former link pool
                    editor_table.notify_cell_changes(beat_keys)
                }
            }
            UI_LOCK_PARTIAL -> {
                editor_table.notify_cell_changes(beat_keys, true)
            }
            UI_LOCK_FULL -> {}
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
                        editor_table.notify_cell_changes(update_keys)
                    }
                }
            }
            UI_LOCK_PARTIAL -> {
                editor_table.notify_cell_change(beat_key, true)
                if (update_keys.isNotEmpty()) {
                    editor_table.notify_cell_changes(update_keys, true)
                }
            }
            UI_LOCK_FULL -> {}
        }

    }

    override fun remap_links(remap_hook: (beat_key: BeatKey) -> BeatKey?) {
        val originally_mapped = this.link_pool_map.keys
        super.remap_links(remap_hook)
        val unmapped = originally_mapped - this.link_pool_map.keys
        val changed = (this.link_pool_map.keys - originally_mapped) + unmapped

        val editor_table = this.get_editor_table() ?: return

        // Because remap_links isn't an end-function, we use queue_cell_changes instead
        // of notify_cell_changes
        when (this.get_ui_lock_level()) {
            UI_LOCK_PARTIAL,
            null -> {
                    editor_table.queue_cell_changes(changed.toList())
            }
            UI_LOCK_FULL -> {}
        }
    }

    private fun <T> withFragment(callback: (FragmentEditor) -> T): T? {
        val fragment = this._activity?.get_active_fragment()
        return if (fragment is FragmentEditor) {
            callback(fragment)
        } else {
            null
        }
    }

    override fun apply_undo() {
        super.apply_undo()
        this.get_editor_table()?.apply_queued_cell_changes()
    }

    fun set_relative_mode(event: OpusEvent) {
        if (this._activity != null && this._activity!!.configuration.relative_mode) {
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

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        super.set_duration(beat_key, position, duration)

        when (this.get_ui_lock_level()) {
            UI_LOCK_FULL -> { }
            else -> {
                this.runOnUiThread { main ->
                    val btnDuration: TextView = main.findViewById(R.id.btnDuration) ?: return@runOnUiThread
                    btnDuration.text = main.getString(R.string.label_duration, duration)
                }
            }
        }
    }


    // Cursor Functions ////////////////////////////////////////////////////////////////////////////
    override fun cursor_clear() {
        super.cursor_clear()

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

    override fun cursor_select_row(channel: Int, line_offset: Int) {
        super.cursor_select_row(channel, line_offset)

        val activity = this.get_activity() ?: return
        if (!activity.view_model.show_percussion && this.is_percussion(channel)) {
            this.make_percussion_visible()
        }

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            val editor_table = this.get_editor_table()
            editor_table?.update_cursor(this.cursor)

            this.withFragment { main ->
                main.set_context_menu_line()
            }
            editor_table?.scroll_to_position(y = this.get_abs_offset(channel, line_offset))
        }
    }

    override fun cursor_select_column(beat: Int) {
        super.cursor_select_column(beat)
        this.runOnUiThread {
            this.withFragment { main ->
                main.set_context_menu_column()
            }

            val editor_table = this.get_editor_table() ?: return@runOnUiThread
            editor_table.scroll_to_position(x = beat, force = false)
            editor_table.update_cursor(this.cursor)
        }
    }

    override fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        val activity = this.get_activity()
        if (activity != null && !activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
            this.make_percussion_visible()
        }

        super.cursor_select(beat_key, position)

        val current_tree = this.get_tree()
        if (current_tree.is_event()) {
            this.set_relative_mode(current_tree.get_event()!!)
        }

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            this.withFragment {
                if (this.is_percussion(beat_key.channel)) {
                    it.set_context_menu_leaf_percussion()
                } else {
                    it.set_context_menu_leaf()
                }
            }

            val editor_table = this.get_editor_table() ?: return@runOnUiThread
            editor_table.update_cursor(this.cursor, false)
            editor_table.scroll_to_position(beat_key, position)
        }
    }

    override fun cursor_select_to_link(beat_key: BeatKey) {
        super.cursor_select_to_link(beat_key)

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

    override fun cursor_select_range_to_link(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        super.cursor_select_range(beat_key_a, beat_key_b)

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

    override fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        super.cursor_select_range(beat_key_a, beat_key_b)

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            this.get_editor_table()?.update_cursor(this.cursor)
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
                   editor_table.notify_cell_changes(update_keys)
               }
           }
           UI_LOCK_PARTIAL -> {
               editor_table.notify_cell_changes(update_keys, true)
           }
           UI_LOCK_FULL -> {}
       }
    }

    // End Cursor Functions ////////////////////////////////////////////////////////////////////////

    override fun save(path: String?) {
        super.save(path)
        val activity = this.get_activity() ?: return
        activity.findViewById<View>(R.id.btnDeleteProject).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.btnCopyProject).visibility = View.VISIBLE
    }


    override fun link_beats(beat_key: BeatKey, target: BeatKey) {
        super.link_beats(beat_key, target)

        when (this.get_ui_lock_level()) {
            UI_LOCK_FULL -> { }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.notify_cell_change(beat_key)
            }
            null -> {
                this.runOnUiThread {
                    this.get_editor_table()?.notify_cell_change(beat_key)
                }
            }
        }
    }

    override fun batch_link_beats(beat_key_pairs: List<Pair<BeatKey, BeatKey>>) {
        this.surpress_ui {
            super.batch_link_beats(beat_key_pairs)
        }

        val all_keys = mutableListOf<BeatKey>()
        for ((from_key, _) in beat_key_pairs) {
            if (all_keys.contains(from_key)) {
                continue
            }

            for (linked_key in this.get_all_linked(from_key)) {
                all_keys.add(linked_key)
            }
        }

        when (this.get_ui_lock_level()) {
            UI_LOCK_FULL -> { }
            UI_LOCK_PARTIAL -> {
                val editor_table = this.get_editor_table() ?: return
                editor_table.notify_cell_changes(all_keys)
            }
            null -> {
                this.runOnUiThread {
                    val editor_table = this.get_editor_table() ?: return@runOnUiThread
                    editor_table.notify_cell_changes(all_keys)
                }
            }
        }
    }

    fun get_visible_channels(): List<OpusChannel> {
        val activity = this.get_activity()
        return if (activity == null || activity.view_model.show_percussion) {
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

    override fun set_tuning_map(new_map: Array<Pair<Int, Int>>, mod_events: Boolean) {
        val was_tuning_standard = this.is_tuning_standard()
        super.set_tuning_map(new_map, mod_events)
        val is_tuning_standard = this.is_tuning_standard()

        val activity = this.get_activity() ?: return

        if (is_tuning_standard && !was_tuning_standard) {
            activity.enable_physical_midi_output()
            if (activity.is_connected_to_physical_device()) {
                activity.disconnect_feedback_device()
            }
        } else if (!is_tuning_standard && was_tuning_standard) {
            activity.block_physical_midi_output()
            if (activity.is_connected_to_physical_device()) {
                activity.connect_feedback_device()
            }
        }

        activity.setup_project_config_drawer_export_button()

        if (this.get_ui_lock_level() != null || !mod_events) {
            return
        }

        this.withFragment { fragment ->
            fragment.reset_context_menu()
        }
    }

    private fun make_percussion_visible() {
        if (this.get_ui_lock_level() == UI_LOCK_FULL) {
            return
        }

        val main = this._activity ?: return
        main.view_model.show_percussion = true

        val channel_option_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
        if (channel_option_recycler.adapter != null) {
            val adapter = channel_option_recycler.adapter!! as ChannelOptionAdapter
            adapter.notifyItemChanged(adapter.itemCount - 1)
        }

        val editor_table = main.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.update_percussion_visibility()
    }
}
