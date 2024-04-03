package com.qfs.pagan
import android.content.res.Configuration
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.qfs.apres.Midi
import com.qfs.pagan.opusmanager.ActiveControlSet
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.LoadedJSONData
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusEventSTD
import com.qfs.pagan.opusmanager.OpusLayerCursor
import com.qfs.pagan.opusmanager.OpusLine
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

    private val _cached_visible_line_map = HashMap<Int, Int>() // Key: visible line, Value: control_line
    private val _cached_inv_visible_line_map = HashMap<Int, Int>()
    private val _cached_ctl_map_line = HashMap<Triple<Int, Int, ControlEventType>, Int>()
    private val _cached_ctl_map_channel = HashMap<Pair<Int, ControlEventType>, Int>()
    private val _cached_ctl_map_global = HashMap<ControlEventType, Int>()


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

    private fun _get_all_linked_as_coords(beat_key: BeatKey): List<EditorTable.Coordinate> {
        val all_keys = this.get_all_linked(beat_key).toList()
        return List(all_keys.size) { i: Int ->
            val key = all_keys[i]

            EditorTable.Coordinate(
                this.get_visible_row_from_ctl_line(
                    this.get_ctl_line_index(
                        this.get_abs_offset(
                            key.channel,
                            key.line_offset
                        )
                    )
                )!!,
                key.beat
            )
        }
    }

    /*
        Notify the editor table to update certain cells without following links
     */
    private fun _notify_cell_changes(beat_keys: List<BeatKey>, force_queue: Boolean = false) {
        if (this.get_activity() == null) {
            return
        }

        val coord_list = List(beat_keys.size) { i: Int ->
            EditorTable.Coordinate(
                this.get_visible_row_from_ctl_line(
                    this.get_ctl_line_index(
                        this.get_abs_offset(
                            beat_keys[i].channel,
                            beat_keys[i].line_offset
                        )
                    )
                )!!,
                beat_keys[i].beat
            )
        }


        val editor_table = this.get_editor_table() ?: return
        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { _: MainActivity ->
                    if (force_queue || this@OpusLayerInterface.history_cache.isLocked()) {
                        editor_table.queue_cell_changes(coord_list)
                    } else {
                        editor_table.notify_cell_changes(coord_list)
                    }
                }
            }

            UI_LOCK_PARTIAL -> {
                if (force_queue || this.history_cache.isLocked()) {
                    editor_table.queue_cell_changes(coord_list)
                } else {
                    editor_table.notify_cell_changes(coord_list, true)
                }
            }

            UI_LOCK_FULL -> {}
        }
    }

    /*
        Notify the editor table to update a cell and all linked cells
     */
    private fun _notify_cell_change(beat_key: BeatKey) {
        if (this.get_activity() == null) {
            return
        }

        val coord_list = this._get_all_linked_as_coords(beat_key)

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { _: MainActivity ->
                    this.get_editor_table()?.notify_cell_changes(coord_list)
                }
            }

            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.notify_cell_changes(coord_list, true)
            }

            UI_LOCK_FULL -> {}
        }
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        super.unset(beat_key, position)
        this._notify_cell_change(beat_key)
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<OpusEventSTD>) {

        val activity = this.get_activity() ?: return super.replace_tree(beat_key, position, tree)
        if (!activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
            this.make_percussion_visible()
        }

        super.replace_tree(beat_key, position, tree)
        this._notify_cell_change(beat_key)
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEventSTD) {
        val activity = this.get_activity() ?: return super.set_event(beat_key, position, event)
        if (!activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
            this.make_percussion_visible()
        }

        super.set_event(beat_key, position, event)

        // If the OM is applying history, change the relative mode, otherwise leave it.
        if (this.history_cache.isLocked()) {
            this.set_relative_mode(event)
        }

        this._notify_cell_change(beat_key)
    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        super.set_percussion_event(beat_key, position)

        if (this.get_activity() == null) {
            return
        }

        this._notify_cell_change(beat_key)
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

        this._notify_cell_change(beat_key)
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        super.insert_after(beat_key, position)

        if (this.get_activity() == null) {
            return
        }

        this._notify_cell_change(beat_key)
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        super.insert(beat_key, position)

        if (this.get_activity() == null) {
            return
        }

        this._notify_cell_change(beat_key)
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)

        if (this.get_activity() == null) {
            return
        }

        this._notify_cell_change(beat_key)
    }

    override fun new_line(channel: Int, line_offset: Int?): OpusLine {
        val output = super.new_line(channel, line_offset)

        if (this.get_activity() != null) {
            val adj_line_offset = line_offset ?: (this.channels[channel].lines.size - 1)
            val abs_offset = this.get_abs_offset(channel, adj_line_offset)
            val row_index = this.get_ctl_line_index(abs_offset)
            val visible_row = this.get_visible_row_from_ctl_line(row_index) ?: return output

            when (this.get_ui_lock_level()) {
                null -> {
                    this.runOnUiThread { _: MainActivity ->
                        this.get_editor_table()?.new_row(visible_row, output)
                        if (this.is_ctl_level_visible(CtlLineLevel.Line)) {
                            val controllers = this.channels[channel].lines[adj_line_offset].controllers.get_all()
                            controllers.forEachIndexed { i: Int, (_, controller): Pair<ControlEventType, ActiveControlSet.ActiveController> ->
                                this.get_editor_table()?.new_row(visible_row + i, controller)
                            }
                        }
                    }
                }

                UI_LOCK_PARTIAL -> {
                    this.get_editor_table()?.new_row(visible_row, output, true)
                    if (this.is_ctl_level_visible(CtlLineLevel.Line)) {
                        val controllers = this.channels[channel].lines[adj_line_offset].controllers.get_all()
                        controllers.forEachIndexed { i: Int, (_, controller): Pair<ControlEventType, ActiveControlSet.ActiveController> ->
                            this.get_editor_table()?.new_row(visible_row + i, controller, true)
                        }
                    }
                }

                UI_LOCK_FULL -> {}
            }
        }

        return output
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusLine) {
        val activity = this.get_activity()
        if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
            this.make_percussion_visible()
        }

        super.insert_line(channel, line_offset, line)

        if (activity == null) {
            return 
        }

        val abs_offset = this.get_abs_offset(channel, line_offset)
        val row_index = this.get_visible_row_from_ctl_line(
            this.get_ctl_line_index(abs_offset)
        ) ?: return

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread { _: MainActivity ->
                    this.get_editor_table()?.new_row(row_index, line)
                    if (this.is_ctl_level_visible(CtlLineLevel.Line)) {
                        val controllers = line.controllers.get_all()
                        controllers.forEachIndexed { i: Int, (_, controller): Pair<ControlEventType, ActiveControlSet.ActiveController> ->
                            this.get_editor_table()?.new_row(row_index + i, controller)
                        }
                    }
                }
            }
            UI_LOCK_PARTIAL -> {
                this.get_editor_table()?.new_row(row_index, line, true)
                if (this.is_ctl_level_visible(CtlLineLevel.Line)) {
                    val controllers = line.controllers.get_all()
                    controllers.forEachIndexed { i: Int, (_, controller): Pair<ControlEventType, ActiveControlSet.ActiveController> ->
                        this.get_editor_table()?.new_row(row_index + i, controller)
                    }
                }
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

    override fun remove_line(channel: Int, line_offset: Int): OpusLine {
        val activity = this.get_activity()
        if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
            this.make_percussion_visible()
        }

        val abs_line = this.get_visible_row_from_ctl_line(
            this.get_ctl_line_index(
                this.get_abs_offset(channel, line_offset)
            )
        )!!

        val output = super.remove_line(channel, line_offset)

        if (activity != null) {
            when (this.get_ui_lock_level()) {
                null -> {
                    this.runOnUiThread { _: MainActivity ->
                        val control_lines = if (this.is_ctl_level_visible(CtlLineLevel.Line)) {
                            output.controllers.get_all().size
                        } else {
                            0
                        }
                        this.get_editor_table()?.remove_rows(abs_line, control_lines + 1)
                    }
                }

                UI_LOCK_PARTIAL -> {
                    val control_lines = if (this.is_ctl_level_visible(CtlLineLevel.Line)) {
                        output.controllers.get_all().size
                    } else {
                        0
                    }
                    this.get_editor_table()?.remove_rows(abs_line, control_lines + 1, true)
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
        val line_list = mutableListOf<OpusLine>()
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

        if (this.is_percussion(notify_index) && !activity.view_model.show_percussion) {
            return
        }

        when (this.get_ui_lock_level()) {
            null -> {
                this.runOnUiThread {
                    if (editor_table == null) {
                        return@runOnUiThread
                    }
                    var ctl_row = this.get_visible_row_from_ctl_line(
                        this.get_ctl_line_index(y)
                    )!!

                    for (line in line_list) {
                        editor_table.new_row(ctl_row++, line)

                        if (this.is_ctl_level_visible(CtlLineLevel.Line)) {
                            for ((type, controller) in line.controllers.get_all()) {
                                editor_table.new_row(ctl_row++, controller)
                            }
                        }
                    }

                    if (this.is_ctl_level_visible(CtlLineLevel.Channel)) {
                        val controllers = this.channels[notify_index].controllers.get_all()
                        for ((type, controller) in controllers) {
                            editor_table.new_row(ctl_row++, controller)
                        }
                    }
                }
            }
            UI_LOCK_PARTIAL -> {
                if (editor_table == null) {
                    return
                }

                var ctl_row = this.get_visible_row_from_ctl_line(this.get_ctl_line_index(y)) ?: return
                for (line in line_list) {
                    editor_table.new_row(ctl_row++, line, true)
                    if (this.is_ctl_level_visible(CtlLineLevel.Line)) {
                        for ((type, controller) in line.controllers.get_all()) {
                            editor_table.new_row(ctl_row++, controller, true)
                        }
                    }
                }
                if (this.is_ctl_level_visible(CtlLineLevel.Channel)) {
                    val controllers = this.channels[notify_index].controllers.get_all()
                    for ((type, controller) in controllers) {
                        editor_table.new_row(ctl_row++, controller, true)
                    }
                }
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

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEventSTD>>?) {
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
        activity.view_model.show_percussion = true
        this.surpress_ui {
            super.new()
        }

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
        this.recache_line_maps()
    }

    override fun load_json(json_data: LoadedJSONData) {
        val activity = this.get_activity() ?: return super.load_json(json_data)

        this._ui_clear()
        this.surpress_ui {
            super.load_json(json_data)
        }

        if (! this._in_reload) {
            activity.view_model.show_percussion = !(!this.has_percussion() && this.channels.size > 1)
            this.recache_line_maps()
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

    override fun clear() {
        this._cached_visible_line_map.clear()
        this._cached_inv_visible_line_map.clear()
        this._cached_ctl_map_line.clear()
        this._cached_ctl_map_channel.clear()
        this._cached_ctl_map_global.clear()

        val editor_table = this.get_editor_table()
        editor_table?.clear()
        this.runOnUiThread { main: MainActivity ->
            editor_table?.precise_scroll(0, 0, 0, 0)
        }
        super.clear()
    }

    override fun remove_channel(channel: Int) {
        val y = try {
            this.get_abs_offset(channel, 0)
        } catch (e: IndexOutOfBoundsException) {
            this.get_total_line_count()
        }

        val ctl_row = this.get_visible_row_from_ctl_line(this.get_ctl_line_index(y))!!
        var removed_row_count = 0

        if (this.is_ctl_level_visible(CtlLineLevel.Channel)) {
            removed_row_count += this.controllers.size()
        }

        removed_row_count += this.channels[channel].size
        if (this.is_ctl_level_visible(CtlLineLevel.Line)) {
            for (line in this.channels[channel].lines) {
                removed_row_count += line.controllers.size()
            }
        }

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
                editor_table.remove_rows(ctl_row, removed_row_count, true)
            }
            null -> {
                this.runOnUiThread {
                    editor_table.remove_rows(ctl_row, removed_row_count)
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
        this._notify_cell_changes(beat_keys)
    }

    override fun unlink_beat(beat_key: BeatKey) {
        val update_keys = this.get_all_linked(beat_key).toMutableList()
        update_keys.remove(beat_key)
        super.unlink_beat(beat_key)

        this._notify_cell_change(beat_key)
        this._notify_cell_changes(update_keys)
    }

    override fun remap_links(remap_hook: (beat_key: BeatKey) -> BeatKey?) {
        val originally_mapped = this.link_pool_map.keys
        super.remap_links(remap_hook)
        val unmapped = originally_mapped - this.link_pool_map.keys
        val changed = (this.link_pool_map.keys - originally_mapped) + unmapped

        // Because remap_links isn't an end-function, we use queue_cell_changes instead
        // of notify_cell_changes
        this._notify_cell_changes(changed.toList(), true)
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

    fun set_relative_mode(event: OpusEventSTD) {
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

    override fun cursor_select_ctl_row_at_channel(ctl_type: ControlEventType, channel: Int) {
        super.cursor_select_ctl_row_at_channel(ctl_type, channel)

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

            val scroll_to_row = this._cached_ctl_map_channel[Pair(channel, ctl_type)] ?: return@runOnUiThread
            editor_table?.scroll_to_position(y = scroll_to_row)
        }
    }

    override fun cursor_select_ctl_row_at_line(ctl_type: ControlEventType, channel: Int, line_offset: Int) {
        super.cursor_select_ctl_row_at_line(ctl_type, channel, line_offset)

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

            val scroll_to_row = this._cached_ctl_map_line[Triple(channel, line_offset, ctl_type)] ?: return@runOnUiThread
            editor_table?.scroll_to_position(y = scroll_to_row)
        }
    }

    override fun cursor_select_ctl_row_at_global(ctl_type: ControlEventType) {
        super.cursor_select_ctl_row_at_global(ctl_type)

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            val editor_table = this.get_editor_table()
            editor_table?.update_cursor(this.cursor)

            this.withFragment { main ->
                main.set_context_menu_line()
            }

            val scroll_to_row = this._cached_ctl_map_global[ctl_type] ?: return@runOnUiThread

            editor_table?.scroll_to_position(y = scroll_to_row)
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

    override fun cursor_select_ctl_at_line(
        ctl_type: ControlEventType,
        beat_key: BeatKey,
        position: List<Int>
    ) {
        val activity = this.get_activity()
        if (activity != null && !activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
            this.make_percussion_visible()
        }

        super.cursor_select_ctl_at_line(ctl_type, beat_key, position)

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            this.withFragment {
                 it.set_context_menu_line_control_leaf()
            }

            val editor_table = this.get_editor_table() ?: return@runOnUiThread
            editor_table.update_cursor(this.cursor, false)

            editor_table.scroll_to_position(beat_key, position)
        }
    }

    override fun cursor_select_ctl_at_channel(
        ctl_type: ControlEventType,
        channel: Int,
        beat: Int,
        position: List<Int>
    ) {
        val activity = this.get_activity()
        if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
            this.make_percussion_visible()
        }

        super.cursor_select_ctl_at_channel(ctl_type, channel, beat, position)

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            this.withFragment {
                // TODO: SET CONTEXT MENU
            }

            val editor_table = this.get_editor_table() ?: return@runOnUiThread
            editor_table.update_cursor(this.cursor, false)

            // TODO
            //editor_table.scroll_to_position(, position)
        }
    }

    override fun cursor_select_ctl_at_global(
        ctl_type: ControlEventType,
        beat: Int,
        position: List<Int>
    ) {
        super.cursor_select_ctl_at_global(ctl_type, beat, position)

        if (this.get_ui_lock_level() != null) {
            return
        }

        this.runOnUiThread {
            this.withFragment {
                // TODO: SET CONTEXT MENU
            }

            val editor_table = this.get_editor_table() ?: return@runOnUiThread
            editor_table.update_cursor(this.cursor, false)

            // TODO
            //editor_table.scroll_to_position(, position)
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
       val update_keys = this.get_all_linked(beat_key).toList()
       super.clear_link_pool(beat_key)
       this._notify_cell_changes(update_keys)
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
        this._notify_cell_change(beat_key)
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

        this._notify_cell_changes(all_keys)
    }

    fun get_visible_channels(): List<OpusChannel> {
        val activity = this.get_activity()
        return if (activity == null || activity.view_model.show_percussion) {
            this.channels
        } else {
            this.channels.subList(0, max(this.channels.size - 1, 0))
        }
    }

    /*
        Get the number of visible lines, not including control lines
     */
    fun get_visible_line_count(): Int {
        var total = 0
        for (channel in this.get_visible_channels()) {
            total += channel.lines.size
        }
        return total
    }

    /*
        Get the number of visible lines, control lines included
     */
    fun get_visible_master_line_count(): Int {
        return this._cached_visible_line_map.size
    }

    /*
        Given the row, get the line number in the Opus
     */
    fun get_ctl_line_from_visible_row(row: Int): Int {
        return this._cached_visible_line_map[row]!!
    }

    fun get_visible_row_from_ctl_line(line: Int): Int? {
        return this._cached_inv_visible_line_map[line]
    }

    fun get_visible_row_from_ctl_line_line(type: ControlEventType, channel: Int, line_offset: Int): Int {
        return this._cached_ctl_map_line[Triple(channel, line_offset, type)]!!
    }

    fun get_visible_row_from_ctl_line_channel(type: ControlEventType, channel: Int): Int {
        return this._cached_ctl_map_channel[Pair(channel, type)]!!
    }

    fun get_visible_row_from_ctl_line_global(type: ControlEventType): Int {
        return this._cached_ctl_map_global[type]!!
    }

    override fun recache_line_maps() {
        super.recache_line_maps()
        this._cached_visible_line_map.clear()
        this._cached_inv_visible_line_map.clear()
        this._cached_ctl_map_line.clear()
        this._cached_ctl_map_channel.clear()
        this._cached_ctl_map_global.clear()

        val percussion_visible = this.get_activity()!!.view_model.show_percussion
        var ctl_line = 0
        var visible_line = 0
        this.channels.forEachIndexed { channel_index: Int, channel: OpusChannel ->
            val hide_channel = this.is_percussion(channel_index) && !percussion_visible
            for (line_offset in channel.lines.indices) {
                if (!hide_channel) {
                    this._cached_inv_visible_line_map[ctl_line] = visible_line
                    this._cached_visible_line_map[visible_line] = ctl_line
                    visible_line += 1
                }
                ctl_line += 1

                for ((type, controller) in channel.lines[line_offset].controllers.controllers) {
                    if (this.is_ctl_level_visible(CtlLineLevel.Line) && !hide_channel) {
                        this._cached_inv_visible_line_map[ctl_line] = visible_line
                        this._cached_visible_line_map[visible_line] = ctl_line
                        this._cached_ctl_map_line[Triple(channel_index, line_offset, type)] = visible_line
                        visible_line += 1
                    }
                    ctl_line += 1
                }
            }

            for (type in channel.controllers.controllers.keys) {
                if (this.is_ctl_level_visible(CtlLineLevel.Channel) && !hide_channel) {
                    this._cached_inv_visible_line_map[ctl_line] = visible_line
                    this._cached_visible_line_map[visible_line] = ctl_line
                    this._cached_ctl_map_channel[Pair(channel_index, type)] = visible_line
                    visible_line += 1
                }
                ctl_line += 1
            }
        }

        for (type in this.controllers.controllers.keys) {
            if (this.is_ctl_level_visible(CtlLineLevel.Global)) {
                this._cached_inv_visible_line_map[ctl_line] = visible_line
                this._cached_visible_line_map[visible_line] = ctl_line
                this._cached_ctl_map_global[type] = visible_line
                visible_line += 1
            }
            ctl_line += 1
        }
    }


    fun is_ctl_level_visible(level: CtlLineLevel): Boolean {
        // For now, we have nothing to control at the channel level
        return level != CtlLineLevel.Channel
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

        this.recache_line_maps()

        val channel_option_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
        if (channel_option_recycler.adapter != null) {
            val adapter = channel_option_recycler.adapter!! as ChannelOptionAdapter
            adapter.notifyItemChanged(adapter.itemCount - 1)
        }

        val editor_table = main.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.update_percussion_visibility()
    }

}
