package com.qfs.pagan
import android.content.res.Configuration
import android.view.View
import android.widget.TextView
import com.qfs.apres.Midi
import com.qfs.json.JSONHashMap
import com.qfs.pagan.UIChangeBill.BillableItem
import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.ActiveController
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.InstrumentEvent
import com.qfs.pagan.opusmanager.OpusChannelAbstract
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusLayerCursor
import com.qfs.pagan.opusmanager.OpusLine
import com.qfs.pagan.opusmanager.OpusLineAbstract
import com.qfs.pagan.opusmanager.OpusLinePercussion
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.RelativeNoteEvent
import com.qfs.pagan.opusmanager.TunedInstrumentEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class OpusLayerInterface : OpusLayerCursor() {
    class HidingNonEmptyPercussionException: Exception()
    class HidingLastChannelException: Exception()

    var relative_mode: Int = 0
    var first_load_done = false
    private var _in_reload = false
    private var _activity: MainActivity? = null

    private val _cached_visible_line_map = HashMap<Int, Int>() // Key: visible line, Value: control_line
    private val _cached_inv_visible_line_map = HashMap<Int, Int>()
    private val _cached_ctl_map_line = HashMap<Triple<Int, Int, ControlEventType>, Int>()
    private val _cached_ctl_map_channel = HashMap<Pair<Int, ControlEventType>, Int>()
    private val _cached_ctl_map_global = HashMap<ControlEventType, Int>()

    private var _cache_cursor: OpusManagerCursor = OpusManagerCursor(OpusManagerCursor.CursorMode.Unset)

    class UILock {
        var full = 0
        var partial = 0
        fun lock_partial() {
            this.partial += 1
        }
        fun unlock_partial() {
            this.partial -= 1
        }
        fun lock_full() {
            this.full += 1
        }
        fun unlock_full() {
            this.full -= 1
        }

        fun is_locked(): Boolean {
            return this.partial != 0 || this.full != 0
        }

        fun is_full_locked(): Boolean {
            return this.full > 0
        }
    }

    private val ui_change_bill = UIChangeBill()
    private val ui_lock = UILock()

    fun attach_activity(activity: MainActivity) {
        this._activity = activity
    }

    fun get_activity(): MainActivity? {
        return this._activity
    }

    private fun get_editor_table(): EditorTable? {
        return this._activity?.findViewById(R.id.etEditorTable)
    }


    // UI BILL Interface functions ---------------------------------
    private fun lock_ui_full(callback: () -> Unit) {
        this.ui_lock.lock_full()
        val output = callback()
        this.ui_lock.unlock_full()

        if (!this.ui_lock.is_locked()) {
            this.apply_bill_changes()
        }

        return output
    }

    private fun <T> lock_ui_partial(callback: () -> T): T {
        this.ui_lock.lock_partial()
        val output = callback()
        this.ui_lock.unlock_partial()
        if (!this.ui_lock.is_locked()) {
            this.apply_bill_changes()
        }
        return output
    }

    /*
        Notify the editor table to update certain cells without following links
     */
    private fun _queue_cell_changes(beat_keys: List<BeatKey>) {
        if (this.ui_lock.is_full_locked()) {
            return
        }

        val coord_list = List(beat_keys.size) { i: Int ->
            EditorTable.Coordinate(
                try {
                    this.get_visible_row_from_ctl_line(
                        this.get_ctl_line_index(
                            this.get_abs_offset(
                                beat_keys[i].channel,
                                beat_keys[i].line_offset
                            )
                        )
                    )!!
                } catch (e: IndexOutOfBoundsException) { // may reference a channel's line before the channel exists
                    this.get_visible_master_line_count()
                },
                beat_keys[i].beat
            )
        }

        this.ui_change_bill.queue_cell_changes(coord_list)
    }

    /*
        Notify the editor table to update a cell and all linked cells
     */
    private fun _queue_cell_change(beat_key: BeatKey, follow_links: Boolean) {
        if (this.ui_lock.is_full_locked()) {
            return
        }

        val tree = this.get_tree(beat_key)
        val new_weight = tree.get_max_child_weight() * tree.size

        val coord_list = mutableListOf<EditorTable.Coordinate>()
        if (follow_links) {
            coord_list.addAll(this._get_all_linked_as_coords(beat_key))
        } else {
            coord_list.add(
                EditorTable.Coordinate(
                    y = this.get_visible_row_from_ctl_line(
                        this.get_ctl_line_index(
                            this.get_abs_offset(beat_key.channel, beat_key.line_offset)
                        )
                    )!!,
                    x = beat_key.beat
                )
            )
        }

        val editor_table = this.get_editor_table() ?: return // TODO: Throw Error
        val notify_columns = mutableSetOf<Int>()
        for (coord in coord_list) {
            if (editor_table.set_mapped_width(coord.y, coord.x, new_weight)) {
                notify_columns.add(coord.x)
            }
        }
        val adj_coord_list = mutableListOf<EditorTable.Coordinate>()
        for (coord in coord_list) {
            if (notify_columns.contains(coord.x)) {
                continue
            }
            adj_coord_list.add(coord)
        }
        this.ui_change_bill.queue_cell_changes(adj_coord_list)
        this.ui_change_bill.queue_column_changes(notify_columns.toList())
    }


    private fun _queue_global_ctl_cell_change(type: ControlEventType, beat: Int) {
        if (this.ui_lock.is_full_locked()) {
            return
        }

        val coord = EditorTable.Coordinate(
            y = this._cached_ctl_map_global[type]!!,
            x = beat
        )

        val tree = this.controllers.get_controller(type).get_tree(beat)
        val new_weight = tree.get_max_child_weight() * tree.size

        val editor_table = this.get_editor_table() ?: return // TODO: Throw Error
        if (editor_table.set_mapped_width(coord.y, coord.x, new_weight)) {
            this.ui_change_bill.queue_column_change(coord.x)
        } else {
            this.ui_change_bill.queue_cell_change(coord)
        }
    }

    private fun _queue_channel_ctl_cell_change(type: ControlEventType, channel: Int, beat: Int) {
        if (this.ui_lock.is_full_locked()) {
            return
        }

        val coord = EditorTable.Coordinate(
            y = this._cached_ctl_map_channel[Pair(channel, type)]!!,
            x = beat
        )

        val tree = this.get_all_channels()[channel].controllers.get_controller(type).get_tree(beat)
        val new_weight = tree.get_max_child_weight() * tree.size

        val editor_table = this.get_editor_table() ?: return // TODO: Throw Error
        editor_table.set_mapped_width(coord.y, coord.x, new_weight)
        if (editor_table.set_mapped_width(coord.y, coord.x, new_weight)) {
            this.ui_change_bill.queue_column_change(coord.x)
        } else {
            this.ui_change_bill.queue_cell_change(coord)
        }
    }

    private fun _queue_line_ctl_cell_change(type: ControlEventType, beat_key: BeatKey) {
        val coord = EditorTable.Coordinate(
            y = this._cached_ctl_map_line[Triple(beat_key.channel, beat_key.line_offset, type)]!!,
            x = beat_key.beat
        )

        val tree = this.get_line_ctl_tree(type, beat_key)
        val new_weight = tree.get_max_child_weight() * tree.size

        val editor_table = this.get_editor_table() ?: return // TODO: Throw Error
        if (editor_table.set_mapped_width(coord.y, coord.x, new_weight)) {
            this.ui_change_bill.queue_column_change(coord.x)
        } else {
            this.ui_change_bill.queue_cell_change(coord)
        }
    }
    // END UI BILL Interface functions ---------------------------------

    override fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>) {
        this.lock_ui_partial {
            super.set_channel_instrument(channel, instrument)
            if (!this.ui_lock.is_full_locked()) {
                // Updating channel instruments doesn't strictly need to be gated behind the full lock,
                // BUT this way these don't get called multiple times every setup
                val activity = this.get_activity()
                activity?.update_channel_instrument(
                    this.get_all_channels()[channel].get_midi_channel(),
                    instrument
                )

                if (this.is_percussion(channel)) {
                    activity?.populate_active_percussion_names()
                }

                this.ui_change_bill.queue_refresh_channel(channel)
            }
        }
    }

    override fun set_project_name(new_name: String?) {
        this.lock_ui_partial {
            super.set_project_name(new_name)
            if (!this.ui_lock.is_full_locked()) {
                this.ui_change_bill.queue_project_name_change()
            }
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

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.unset(beat_key, position)

            this._queue_cell_change(beat_key, false)
        }
    }

    override fun unset_global_ctl(type: ControlEventType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.unset_global_ctl(type, beat, position)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun unset_channel_ctl(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.unset_channel_ctl(type, channel, beat, position)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun unset_line_ctl(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.unset_line_ctl(type, beat_key, position)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<out InstrumentEvent>) {
        this.lock_ui_partial {
            val activity = this.get_activity()
            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
                this.make_percussion_visible()
            }

            super.replace_tree(beat_key, position, tree)
            this._queue_cell_change(beat_key, false)
        }
    }

    override fun replace_global_ctl_tree(type: ControlEventType, beat: Int, position: List<Int>?, tree: OpusTree<OpusControlEvent>) {
        this.lock_ui_partial {
            super.replace_global_ctl_tree(type, beat, position, tree)

            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun replace_channel_ctl_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>?, tree: OpusTree<OpusControlEvent>) {
        this.lock_ui_partial {
            super.replace_channel_ctl_tree(type, channel, beat, position, tree)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun replace_line_ctl_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>?, tree: OpusTree<OpusControlEvent>) {
        this.lock_ui_partial {
            super.replace_line_ctl_tree(type, beat_key, position, tree)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun set_global_ctl_event(type: ControlEventType, beat: Int, position: List<Int>, event: OpusControlEvent) {
        this.lock_ui_partial {
            super.set_global_ctl_event(type, beat, position, event)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun set_channel_ctl_event(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, event: OpusControlEvent) {
        this.lock_ui_partial {
            super.set_channel_ctl_event(type, channel, beat, position, event)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun set_line_ctl_event(type: ControlEventType, beat_key: BeatKey, position: List<Int>, event: OpusControlEvent) {
        this.lock_ui_partial {
            super.set_line_ctl_event(type, beat_key, position, event)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: InstrumentEvent) {
        this.lock_ui_partial {

            val activity = this.get_activity()

            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
                this.make_percussion_visible()
            }

            super.set_event(beat_key, position, event)

            if (event is TunedInstrumentEvent) {
                this.set_relative_mode(event)
            }

            if (!this.ui_lock.is_full_locked()) {
                this._queue_cell_change(beat_key, false)
                this.ui_change_bill.queue_refresh_context_menu()
            }
        }
    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.set_percussion_event(beat_key, position)
            val activity = this.get_activity()
            if (activity != null && !activity!!.view_model.show_percussion) {
                this.make_percussion_visible()
            }

            this._queue_cell_change(beat_key, false)
        }
    }

    override fun set_percussion_event_at_cursor() {
        this.lock_ui_partial {
            super.set_percussion_event_at_cursor()
            if (!this.ui_lock.is_full_locked()) {
                this.ui_change_bill.queue_refresh_context_menu()
            }
        }
    }

    override fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        this.lock_ui_partial {
            super.set_percussion_instrument(line_offset, instrument)

            // Need to call get_drum name to repopulate instrument list if needed
            this.get_activity()?.get_drum_name(instrument)

            if (!this.ui_lock.is_full_locked()) {
                this.ui_change_bill.queue_refresh_choose_percussion_button(line_offset)
                this.ui_change_bill.queue_line_label_refresh(
                    this.get_visible_row_from_ctl_line(
                        this.get_ctl_line_index(
                            this.get_abs_offset(this.channels.size, line_offset)
                        )
                    )!!
                )
            }
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_ui_partial {
            super.split_tree(beat_key, position, splits, move_event_to_end)

            if ((this._activity != null && this._activity!!.view_model.show_percussion) || !this.is_percussion(beat_key.channel)) {
                this._queue_cell_change(beat_key, false)
            }
        }
    }

    override fun split_global_ctl_tree(type: ControlEventType, beat: Int, position: List<Int>, splits: Int) {
        this.lock_ui_partial {
            super.split_global_ctl_tree(type, beat, position, splits)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun split_channel_ctl_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, splits: Int) {
        this.lock_ui_partial {
            super.split_channel_ctl_tree(type, channel, beat, position, splits)

            if ((this._activity != null && this._activity!!.view_model.show_percussion) || !this.is_percussion(channel)) {
                this._queue_channel_ctl_cell_change(type, channel, beat)
            }
        }
    }

    override fun split_line_ctl_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.lock_ui_partial {
            super.split_line_ctl_tree(type, beat_key, position, splits)

            if ((this._activity != null && this._activity!!.view_model.show_percussion) || !this.is_percussion(beat_key.channel)) {
                this._queue_line_ctl_cell_change(type, beat_key)
            }
        }
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.insert_after(beat_key, position)
            this._queue_cell_change(beat_key, false)
        }
    }

    override fun insert_after_global_ctl(type: ControlEventType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.insert_after_global_ctl(type, beat, position)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun insert_after_channel_ctl(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.insert_after_channel_ctl(type, channel, beat, position)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun insert_after_line_ctl(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.insert_after_line_ctl(type, beat_key, position)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.insert(beat_key, position)
            this._queue_cell_change(beat_key, false)
        }
    }

    override fun insert_global_ctl(type: ControlEventType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.insert_global_ctl(type, beat, position)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun insert_channel_ctl(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.insert_channel_ctl(type, channel, beat, position)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun insert_line_ctl(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.insert_line_ctl(type, beat_key, position)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.remove_standard(beat_key, position)
            this._queue_cell_change(beat_key, false)
        }
    }

    override fun remove_global_ctl(type: ControlEventType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.remove_global_ctl(type, beat, position)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun remove_channel_ctl(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.remove_channel_ctl(type, channel, beat, position)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun remove_line_ctl(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.remove_line_ctl(type, beat_key, position)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun new_line(channel: Int, line_offset: Int?): OpusLineAbstract<*> {
        return this.lock_ui_partial {
            var output = super.new_line(channel, line_offset)
            this._update_after_new_line(channel, line_offset)
            output
        }
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusLineAbstract<*>) {
        this.lock_ui_partial {
            val activity = this.get_activity()
            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
                this.make_percussion_visible()
            }

            super.insert_line(channel, line_offset, line)
            this._update_after_new_line(channel, line_offset)
        }
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        this.lock_ui_partial {
            super.swap_lines(channel_a, line_a, channel_b, line_b)

            if (!this.ui_lock.is_full_locked()) {
                val vis_line_a = this.get_visible_row_from_ctl_line(
                    this.get_ctl_line_index(
                        this.get_abs_offset(channel_a, line_a)
                    )
                )!!

                val vis_line_b = this.get_visible_row_from_ctl_line(
                    this.get_ctl_line_index(
                        this.get_abs_offset(channel_b, line_b)
                    )
                )!!

                this.get_editor_table()?.swap_mapped_lines(vis_line_a, vis_line_b)

                this.ui_change_bill.queue_row_change(line_a)
                this.ui_change_bill.queue_row_change(line_b)
            }
        }
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusLineAbstract<*> {
        return this.lock_ui_partial {
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

            var row_count = 1
            for ((type, _) in output.controllers.get_all()) {
                if (this.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                    row_count += 1
                }
            }

            this._queue_remove_rows(abs_line, row_count)

            output
        }
    }

    private fun _queue_remove_rows(y: Int, count: Int) {
        if (!this.ui_lock.is_full_locked()) {
            this.get_editor_table()?.remove_mapped_lines(y, count)
            this.ui_change_bill.queue_row_removal(y, count)
        }
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        this.lock_ui_partial {
            val notify_index = channel ?: this.channels.size

            super.new_channel(channel, lines, uuid)

            if (!this.ui_lock.is_full_locked()) {
                val line_list = mutableListOf<OpusLine>()
                for (i in 0 until lines) {
                    line_list.add(this.channels[notify_index].lines[i])
                }
                val y = this.get_abs_offset(notify_index, 0)

                this.ui_change_bill.queue_add_channel(notify_index)

                val activity = this.get_activity()
                if (activity == null || !this.is_percussion(notify_index) || activity.view_model.show_percussion) {
                    var ctl_row = this.get_visible_row_from_ctl_line(
                        this.get_ctl_line_index(y)
                    )!!

                    for (line in line_list) {
                        this._add_line_to_column_width_map(ctl_row++, line)
                        for ((type, controller) in line.controllers.get_all()) {
                            if (this.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                                this._add_controller_to_column_width_map(ctl_row++, controller)
                            }
                        }
                    }

                    val controllers = this.channels[notify_index].controllers.get_all()
                    for ((type, controller) in controllers) {
                        if (this.is_ctl_line_visible(CtlLineLevel.Channel, type)) {
                            this._add_controller_to_column_width_map(ctl_row++, controller)
                        }
                    }
                }
            }
        }
    }

    override fun set_link_pools(pools: List<Set<BeatKey>>) {
        this.lock_ui_partial {
            super.set_link_pools(pools)
            // TODO: I think the cell changes should be queued via on_linked callback
            val keys_as_single_list = mutableListOf<BeatKey>()
            for (pool in pools) {
                keys_as_single_list.addAll(pool)
            }
            this._queue_cell_changes(keys_as_single_list)
        }
    }

    override fun remove_beat(beat_index: Int) {
        this.lock_ui_partial {
            val link_keys = mutableListOf<BeatKey>()
            this.get_all_channels().forEachIndexed { i: Int, channel: OpusChannelAbstract<*,*> ->
                for (j in channel.lines.indices) {
                    for (key in this.get_all_linked(BeatKey(i, j, beat_index))) {
                        if (key.beat == beat_index) {
                            continue
                        }
                        link_keys.add(
                            BeatKey(
                                key.channel,
                                key.line_offset,
                                if (key.beat > beat_index) {
                                    key.beat - 1
                                } else {
                                    key.beat
                                }
                            )
                        )
                    }
                }
            }

            super.remove_beat(beat_index)


            if (!this.ui_lock.is_full_locked()) {
                this.get_editor_table()?.remove_mapped_column(beat_index)

                this.ui_change_bill.queue_remove_column(beat_index)

                this._queue_cell_changes(link_keys)
            }
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<InstrumentEvent>>?) {
        this.lock_ui_partial {
            val bkp_cursor = this.cursor.copy()

            super.insert_beat(beat_index, beats_in_column)

            if (!this.ui_lock.is_full_locked()) {
                this.queue_cursor_update(bkp_cursor)
                this._new_column_in_column_width_map(beat_index)
                this.queue_cursor_update(this.cursor)
            }

            // TODO: Is this necessary? I'm not seeing why linked cells would need updates
            // val link_keys = mutableListOf<BeatKey>()
            // this.get_all_channels().forEachIndexed { i: Int, channel: OpusChannelAbstract<*,*> ->
            //     for (j in channel.lines.indices) {
            //         link_keys.addAll(this.get_all_linked(BeatKey(i, j, beat_index)))
            //     }
            // }
            // this._notify_cell_changes(link_keys)
        }
    }

    override fun insert_beats(beat_index: Int, count: Int) {
        // TODO: is it necessary to queue ui changes in this function?
        // Not sure but i think the new ui logic will be fine to individually call each insert_beat
        super.insert_beats(beat_index, count)
    }

    override fun remove_channel(channel: Int) {
        this.lock_ui_partial {
            if (!this.ui_lock.is_full_locked()) {
                val y = try {
                    this.get_abs_offset(channel, 0)
                } catch (e: IndexOutOfBoundsException) {
                    this.get_total_line_count()
                }

                val ctl_row = this.get_visible_row_from_ctl_line(this.get_ctl_line_index(y))!!
                var removed_row_count = this.channels[channel].size

                val activity = this.get_activity()

                val force_show_percussion = activity != null
                        && !activity.view_model.show_percussion
                        && !this.is_percussion(channel)
                        && this.channels.size == 2

                if (force_show_percussion) {
                    this.make_percussion_visible()
                }

                for ((type, _) in this.channels[channel].controllers.get_all()) {
                    if (this.is_ctl_line_visible(CtlLineLevel.Channel, type)) {
                        removed_row_count += 1
                    }
                }

                for (line in this.channels[channel].lines) {
                    for ((type, _) in line.controllers.get_all()) {
                        if (this.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                            removed_row_count += 1
                        }
                    }
                }

                super.remove_channel(channel)

                this.ui_change_bill.queue_remove_channel(channel)
                this.ui_change_bill.queue_row_removal(ctl_row, removed_row_count)
            } else {
                super.remove_channel(channel)
            }
        }
    }



    override fun new() {
        this.lock_ui_full {
            this._ui_clear()
            val activity = this.get_activity()
            activity!!.view_model.show_percussion = true
            super.new()

            val new_path = activity!!.get_new_project_path()
            this.path = new_path
        }
    }

    override fun import_midi(midi: Midi) {
        this.lock_ui_full {
            this._ui_clear()
            super.import_midi(midi)
            val activity = this.get_activity()
            activity!!.view_model.show_percussion = this.has_percussion()
            this.recache_line_maps()
        }
    }

    override fun load_json(json_data: JSONHashMap) {
        this.lock_ui_full {
            val activity = this.get_activity()!!
            this._ui_clear()

            super.load_json(json_data)

            if (! this._in_reload) {
                activity.view_model.show_percussion = !(!this.has_percussion() && this.channels.size > 1)
                this.recache_line_maps()
            }
        }
    }

    fun reload(bytes: ByteArray, path: String) {
        this._in_reload = true
        this.load(bytes, path)
        this._in_reload = false
    }

    override fun on_project_changed() {
        super.on_project_changed()
        this.ui_change_bill.queue_full_refresh()
        this.first_load_done = true
    }

    override fun clear() {
        this._cached_visible_line_map.clear()
        this._cached_inv_visible_line_map.clear()
        this._cached_ctl_map_line.clear()
        this._cached_ctl_map_channel.clear()
        this._cached_ctl_map_global.clear()

        val editor_table = this.get_editor_table()
        editor_table?.clear()
        this.runOnUiThread {
            editor_table?.precise_scroll(0, 0, 0, 0)
        }
        super.clear()
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
        this.lock_ui_partial {
            super.create_link_pool(beat_keys)
            this._queue_cell_changes(beat_keys)
        }
    }

    override fun unlink_beat(beat_key: BeatKey) {
        this.lock_ui_partial {
            if (this.ui_lock.is_full_locked()) {
                super.unlink_beat(beat_key)
            } else {
                val update_keys = this.get_all_linked(beat_key).toMutableList()
                update_keys.remove(beat_key)

                super.unlink_beat(beat_key)

                this._queue_cell_change(beat_key, false)
                this._queue_cell_changes(update_keys)
            }
        }
    }

    override fun remap_links(remap_hook: (beat_key: BeatKey) -> BeatKey?) {
        this.lock_ui_partial {
            val originally_mapped = this.link_pool_map.keys
            super.remap_links(remap_hook)
            val unmapped = originally_mapped - this.link_pool_map.keys
            val changed = (this.link_pool_map.keys - originally_mapped) + unmapped

            // Because remap_links isn't an end-function, we use queue_cell_changes instead
            // of notify_cell_changes
            this._queue_cell_changes(changed.toList())
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

    override fun apply_undo(repeat: Int) {
        //TODO: The recache_line_maps may not be needed, or may be needed outside the lock?
        this.lock_ui_partial {
            super.apply_undo(repeat)
            this.recache_line_maps()
        }
    }

    fun set_relative_mode(mode: Int) {
        this.relative_mode = mode
    }
    fun set_relative_mode(event: TunedInstrumentEvent) {
        if (this._activity != null && this._activity!!.configuration.relative_mode) {
            this.relative_mode = if (event is AbsoluteNoteEvent) {
                0
            } else if (event is RelativeNoteEvent) {
                if (event.offset >= 0) {
                    1
                } else {
                    2
                }
            } else {
                // TODO: Specify Exception
                throw Exception()
            }
        } else {
            this.relative_mode = 0
        }
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this.lock_ui_partial {
            super.set_duration(beat_key, position, duration)

            // Needs to be set to trigger potentially queued cell changes from on_overlap()
            this._queue_cell_change(beat_key, false)
            // val btnDuration: TextView = main.findViewById(R.id.btnDuration) ?: return@runOnUiThread
            // btnDuration.text = main.getString(R.string.label_duration, duration)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    // Cursor Functions ////////////////////////////////////////////////////////////////////////////
    override fun cursor_apply(cursor: OpusManagerCursor) {
        this.lock_ui_partial {
            super.cursor_apply(cursor)

            this.queue_cursor_update(this.cursor)

            when (cursor.mode) {
                OpusManagerCursor.CursorMode.Row -> {
                    if (cursor.ctl_level == null) {
                        this.ui_change_bill.queue_set_context_menu_line()
                    } else {
                        this.ui_change_bill.queue_set_context_menu_control_line()
                    }
                }
                OpusManagerCursor.CursorMode.Column -> {
                    this.ui_change_bill.queue_set_context_menu_column()
                }
                OpusManagerCursor.CursorMode.Single -> {
                    if (cursor.ctl_level == null) {
                        if (this.is_percussion(cursor.channel)) {
                            this.ui_change_bill.queue_set_context_menu_leaf_percussion()
                        } else {
                            /*
                                Need to set relative mode here since cursor_apply is called after history is applied
                                and set_relative_mode isn't called in replace_tree
                            */
                            val event = this.get_tree().get_event()
                            if (event is TunedInstrumentEvent) {
                                this.set_relative_mode(event)
                            }

                            this.ui_change_bill.queue_set_context_menu_leaf()
                        }
                    } else {
                        this.ui_change_bill.queue_set_context_menu_line_control_leaf()
                    }
                }
                OpusManagerCursor.CursorMode.Range -> {
                    this.ui_change_bill.queue_set_context_menu_linking()
                }
                OpusManagerCursor.CursorMode.Unset -> {
                    this.ui_change_bill.queue_clear_context_menu()
                }
            }
        }
    }

    override fun cursor_clear() {
        this.lock_ui_partial {
            super.cursor_clear()

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_clear_context_menu()
        }
    }

    override fun cursor_select_row(channel: Int, line_offset: Int) {
        this.lock_ui_partial {
            super.cursor_select_row(channel, line_offset)

            val activity = this.get_activity()
            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
                this.make_percussion_visible()
            }

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_set_context_menu_line()
        }
    }

    override fun cursor_select_ctl_row_at_channel(ctl_type: ControlEventType, channel: Int) {
        this.lock_ui_partial {
            super.cursor_select_ctl_row_at_channel(ctl_type, channel)

            val activity = this.get_activity()
            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
                this.make_percussion_visible()
            }

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_set_context_menu_control_line()
        }
    }

    override fun cursor_select_ctl_row_at_line(ctl_type: ControlEventType, channel: Int, line_offset: Int) {
        this.lock_ui_partial {
            super.cursor_select_ctl_row_at_line(ctl_type, channel, line_offset)

            val activity = this.get_activity()
            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
                this.make_percussion_visible()
            }

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_set_context_menu_control_line()
        }
    }

    override fun cursor_select_ctl_row_at_global(ctl_type: ControlEventType) {
        this.lock_ui_partial {
            super.cursor_select_ctl_row_at_global(ctl_type)

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_set_context_menu_control_line()
        }
    }

    override fun cursor_select_column(beat: Int) {
        this.lock_ui_partial {
            super.cursor_select_column(beat)

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_set_context_menu_column()
        }
    }

    override fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            val activity = this.get_activity()
            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
                this.make_percussion_visible()
            }

            super.cursor_select(beat_key, position)

            val current_tree = this.get_tree()
            if (!this.is_percussion(beat_key.channel) && current_tree.is_event()) {
                this.set_relative_mode(current_tree.get_event()!! as TunedInstrumentEvent)
            }

            this.queue_cursor_update(this.cursor, false)
            if (this.is_percussion(beat_key.channel)) {
                this.ui_change_bill.queue_set_context_menu_leaf_percussion()
            } else {
                this.ui_change_bill.queue_set_context_menu_leaf()
            }

        }
    }

    override fun cursor_select_ctl_at_line(ctl_type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            val activity = this.get_activity()
            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
                this.make_percussion_visible()
            }

            super.cursor_select_ctl_at_line(ctl_type, beat_key, position)

            this.queue_cursor_update(this.cursor, false)
            this.ui_change_bill.queue_set_context_menu_line_control_leaf()
        }
    }

    override fun cursor_select_ctl_at_channel(ctl_type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            val activity = this.get_activity()
            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
                this.make_percussion_visible()
            }

            super.cursor_select_ctl_at_channel(ctl_type, channel, beat, position)

            this.queue_cursor_update(this.cursor, false)
            this.ui_change_bill.queue_set_context_menu_line_control_leaf()
        }
    }

    override fun cursor_select_ctl_at_global(ctl_type: ControlEventType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.cursor_select_ctl_at_global(ctl_type, beat, position)

            this.queue_cursor_update(this.cursor, false)
            this.ui_change_bill.queue_set_context_menu_line_control_leaf()
        }
    }

    override fun cursor_select_first_corner(beat_key: BeatKey) {
        this.lock_ui_partial {
            super.cursor_select_first_corner(beat_key)

            this.queue_cursor_update(this.cursor, false)
            this.ui_change_bill.queue_set_context_menu_linking()
        }
    }

    override fun cursor_select_global_ctl_range(type: ControlEventType, first: Int, second: Int) {
        this.lock_ui_partial {
            super.cursor_select_global_ctl_range(type, first, second)

            this.queue_cursor_update(this.cursor, false)
            this.ui_change_bill.queue_set_context_menu_line_control_leaf_b()
        }
    }

    override fun cursor_select_global_ctl_end_point(type: ControlEventType, beat: Int) {
        this.lock_ui_partial {
            super.cursor_select_global_ctl_end_point(type, beat)

            this.queue_cursor_update(this.cursor, false)
            this.ui_change_bill.queue_set_context_menu_line_control_leaf_b()
        }
    }

    override fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.lock_ui_partial {
            super.cursor_select_range(beat_key_a, beat_key_b)

            this.queue_cursor_update(this.cursor, false)
            this.ui_change_bill.queue_set_context_menu_linking()
        }
    }

   override fun clear_link_pool(beat_key: BeatKey) {
       this.lock_ui_partial {
           val update_keys = this.get_all_linked(beat_key).toList()
           super.clear_link_pool(beat_key)
           this._queue_cell_changes(update_keys)
       }
    }

    // End Cursor Functions ////////////////////////////////////////////////////////////////////////
    override fun save(path: String?) {
        this.lock_ui_partial {
            super.save(path)
            this.ui_change_bill.queue_enable_delete_and_copy_buttons()
        }
    }

    override fun link_beats(beat_key: BeatKey, target: BeatKey) {
        this.lock_ui_partial {
            super.link_beats(beat_key, target)
            this._queue_cell_change(beat_key, false)
        }
    }

    override fun batch_link_beats(beat_key_pairs: List<Pair<BeatKey, BeatKey>>) {
        this.lock_ui_partial {
            super.batch_link_beats(beat_key_pairs)

            val all_keys = mutableListOf<BeatKey>()
            for ((from_key, _) in beat_key_pairs) {
                if (all_keys.contains(from_key)) {
                    continue
                }

                for (linked_key in this.get_all_linked(from_key)) {
                    all_keys.add(linked_key)
                }
            }

            this._queue_cell_changes(all_keys)
        }
    }

    fun get_visible_channels(): List<OpusChannelAbstract<*,*>> {
        val activity = this.get_activity()
        return if (activity == null || activity.view_model.show_percussion) {
            List(this.channels.size + 1) { i: Int ->
                if (i < this.channels.size) {
                    this.channels[i]
                } else {
                    this.percussion_channel
                }
            }
        } else {
            this.channels
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

    fun toggle_control_line_visibility(level: CtlLineLevel, type: ControlEventType) {
        val key = Pair(level, type)
        val activity = this.get_activity() ?: return
        if (this.is_ctl_line_visible(level, type)) {
            activity.configuration.visible_line_controls.remove(key)
        } else {
            activity.configuration.visible_line_controls.add(key)
        }
        activity.save_configuration()

        val editor_table = this.get_editor_table() ?: return
        this.withFragment {
            it.backup_position()
        }
        editor_table.clear()

        val cursor = this.cursor
        if (cursor.ctl_level != null) {
            this.cursor_clear()
        }

        this.recache_line_maps()
        this._init_editor_table_width_map()
        editor_table.setup(this.get_visible_master_line_count(), this.beat_count)
        this.withFragment {
            it.restore_view_model_position()
            it.refresh_context_menu()
        }
    }

    // Deprecated function but i'll use it to build functions to toggle specific control lines
    //fun toggle_control_lines() {
    //    val activity = this.get_activity() ?: return
    //    activity.view_model.show_control_lines = !activity.view_model.show_control_lines

    //    val editor_table = this.get_editor_table() ?: return
    //    this.withFragment {
    //        it.backup_position()
    //    }
    //    editor_table.clear()

    //    if (!activity.view_model.show_control_lines) {
    //        val cursor = this.cursor
    //        if (cursor.ctl_level != null) {
    //            this.cursor_clear()
    //        }
    //    }

    //    this.recache_line_maps()
    //    editor_table.setup()
    //    this.withFragment {
    //        it.restore_view_model_position()
    //    }
    //}

    fun toggle_percussion_visibility() {
        val activity = this.get_activity() ?: return

        if (this.channels.isEmpty()) {
            throw HidingLastChannelException()
        }

        if (activity.view_model.show_percussion && !this.percussion_channel.is_empty()) {
            throw HidingNonEmptyPercussionException()
        }

        activity.view_model.show_percussion = !activity.view_model.show_percussion

        val editor_table = this.get_editor_table() ?: return
        this.withFragment {
            it.backup_position()
        }
        editor_table.clear()

        if (!activity.view_model.show_percussion) {
            this.cursor_clear()
        }

        this.recache_line_maps()
        this._init_editor_table_width_map()
        editor_table.setup(this.get_visible_master_line_count(), this.beat_count)
        this.withFragment {
            it.restore_view_model_position()
        }
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
        val channels = List(this.channels.size + 1) { i: Int ->
            if (i < this.channels.size) {
                this.channels[i]
            } else {
                this.percussion_channel
            }
        }

        channels.forEachIndexed { channel_index: Int, channel: OpusChannelAbstract<*,*> ->
            val hide_channel = this.is_percussion(channel_index) && !percussion_visible
            for (line_offset in channel.lines.indices) {
                if (!hide_channel) {
                    this._cached_inv_visible_line_map[ctl_line] = visible_line
                    this._cached_visible_line_map[visible_line] = ctl_line
                    visible_line += 1
                }
                ctl_line += 1

                for ((type, _) in channel.lines[line_offset].controllers.get_all()) {
                    if (this.is_ctl_line_visible(CtlLineLevel.Line, type) && !hide_channel) {
                        this._cached_inv_visible_line_map[ctl_line] = visible_line
                        this._cached_visible_line_map[visible_line] = ctl_line
                        this._cached_ctl_map_line[Triple(channel_index, line_offset, type)] = visible_line
                        visible_line += 1
                    }
                    ctl_line += 1
                }
            }

            for (type in channel.controllers.controllers.keys) {
                if (this.is_ctl_line_visible(CtlLineLevel.Channel, type) && !hide_channel) {
                    this._cached_inv_visible_line_map[ctl_line] = visible_line
                    this._cached_visible_line_map[visible_line] = ctl_line
                    this._cached_ctl_map_channel[Pair(channel_index, type)] = visible_line
                    visible_line += 1
                }
                ctl_line += 1
            }
        }

        for (type in this.controllers.controllers.keys) {
            if (this.is_ctl_line_visible(CtlLineLevel.Global, type)) {
                this._cached_inv_visible_line_map[ctl_line] = visible_line
                this._cached_visible_line_map[visible_line] = ctl_line
                this._cached_ctl_map_global[type] = visible_line
                visible_line += 1
            }
            ctl_line += 1
        }

    }

    fun is_ctl_line_visible(level: CtlLineLevel, type: ControlEventType): Boolean {
        return this.get_activity()!!.configuration.visible_line_controls.contains(
            Pair(level, type)
        )
    }

    override fun set_tuning_map(new_map: Array<Pair<Int, Int>>, mod_events: Boolean) {
        this.lock_ui_partial {
            val was_tuning_standard = this.is_tuning_standard()
            val original_map = this.tuning_map

            super.set_tuning_map(new_map, mod_events)

            val is_tuning_standard = this.is_tuning_standard()

            val activity = this.get_activity()
            if (activity != null) {
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
            }

            this.ui_change_bill.queue_config_drawer_redraw_export_button()

            if (new_map.size != original_map.size && mod_events) {
                for (i in 0 until this.channels.size) {
                    for (j in 0 until this.channels[i].lines.size) {
                        for (k in 0 until this.beat_count) {
                            val beat_key = BeatKey(i, j, k)
                            val tree = this.get_tree(beat_key)
                            if (tree.is_eventless()) {
                                continue
                            }
                            this._queue_cell_change(beat_key, true)
                        }
                    }

                }
            }

            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    private fun make_percussion_visible() {
        this.lock_ui_partial {
            val main = this._activity
            main?.view_model?.show_percussion = true

            this.recache_line_maps()

            this.ui_change_bill.queue_add_channel(this.channels.size)

            var ctl_line = this.get_visible_row_from_ctl_line(
                this.get_ctl_line_index(
                    this.get_abs_offset(this.channels.size, 0)
                )
            )!!

            this.percussion_channel.lines.forEachIndexed { i: Int, line: OpusLinePercussion ->
                this._add_line_to_column_width_map(ctl_line++, line)
                for ((type, controller) in line.controllers.get_all()) {
                    if (this.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                        this._add_controller_to_column_width_map(ctl_line++, controller)
                    }
                }
            }

            for ((type, controller) in this.percussion_channel.controllers.get_all()) {
                if (this.is_ctl_line_visible(CtlLineLevel.Channel, type)) {
                    this._add_controller_to_column_width_map(ctl_line++, controller)
                }
            }
        }
    }

    override fun set_global_controller_initial_event(type: ControlEventType, event: OpusControlEvent) {
        this.lock_ui_partial {
            super.set_global_controller_initial_event(type, event)
            this.ui_change_bill.queue_refresh_context_menu() 
        }
    }

    override fun set_channel_controller_initial_event(type: ControlEventType, channel: Int, event: OpusControlEvent) {
        this.lock_ui_partial {
            super.set_channel_controller_initial_event(type, channel, event)
            this.ui_change_bill.queue_refresh_context_menu() 
        }
    }

    override fun set_line_controller_initial_event(type: ControlEventType, channel: Int, line_offset: Int, event: OpusControlEvent) {
        this.lock_ui_partial {
            super.set_line_controller_initial_event(type, channel, line_offset, event)
            this.ui_change_bill.queue_refresh_context_menu() 
        }
    }

    override fun on_overlap(overlapper: Pair<BeatKey, List<Int>>,overlappee: Pair<BeatKey, List<Int>>) {
        this.lock_ui_partial {
            this._queue_cell_change(overlappee.first, true)
        }
    }

    override fun on_overlap_removed(overlapper: Pair<BeatKey, List<Int>>,overlappee: Pair<BeatKey, List<Int>>) {
        this.lock_ui_partial {
            this._queue_cell_change(overlappee.first, true)
        }
    }

    /*
        Need to know when setting the FeedBackPlaybackDevice sample rate, since we want it as low as is possible without killing higher notes
    */
    fun get_maximum_frequency(): Float {
        val base_frequency = 27.5F
        val transpose = this.transpose
        var maximum_initial_frequency = 0f
        for ((numerator, denominator) in this.tuning_map) {
            maximum_initial_frequency = numerator.toFloat() / denominator.toFloat()
        }
        val radix = this.tuning_map.size
        val max_octave = 7

        return base_frequency * 2F.pow((transpose.toFloat() / radix.toFloat()) + (maximum_initial_frequency * max_octave.toFloat()))
    }

    //------------------------------------------------------------------------
    fun queue_cursor_update(cursor: OpusManagerCursor, deep_update: Boolean = true) {
        if (cursor != this._cache_cursor) {
            try {
                this.queue_cursor_update(this._cache_cursor, deep_update)
            } catch (e: OpusTree.InvalidGetCall) {
                // Pass
            }
            this._cache_cursor = cursor.copy()
        }

        val coordinates_to_update = mutableSetOf<EditorTable.Coordinate>()

        when (cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                when (cursor.ctl_level) {
                    null -> {
                        val beat_key = cursor.get_beatkey()
                        val beat_keys = if (deep_update) {
                            this.get_all_linked(beat_key)
                        } else {
                            listOf(beat_key)
                        }

                        for (linked_key in beat_keys) {
                            val shadow_beat_keys = mutableSetOf<BeatKey>()
                            val event_head = this.get_original_position(linked_key, cursor.position)
                            for ((shadow_key, _) in this.get_all_blocked_positions(event_head.first, event_head.second)) {
                                shadow_beat_keys.add(shadow_key)
                            }

                            // TODO: I think its possible to have oob beats from get_all_blocked_keys, NEED CHECK
                            for (shadow_key in shadow_beat_keys) {
                                val y = try {
                                    this.get_visible_row_from_ctl_line(
                                        this.get_ctl_line_index(
                                            this.get_abs_offset(
                                                shadow_key.channel,
                                                shadow_key.line_offset
                                            )
                                        )
                                    ) ?: continue
                                } catch (e: IndexOutOfBoundsException) {
                                    return
                                }

                                coordinates_to_update.add(EditorTable.Coordinate(y, shadow_key.beat))
                            }
                        }
                    }

                    CtlLineLevel.Line -> {
                        val y = this.get_visible_row_from_ctl_line_line(
                            cursor.ctl_type!!,
                            cursor.channel,
                            cursor.line_offset
                        )
                        coordinates_to_update.add(EditorTable.Coordinate(y, cursor.beat))
                    }
                    CtlLineLevel.Channel -> {
                        val y = this.get_visible_row_from_ctl_line_channel(
                            cursor.ctl_type!!,
                            cursor.channel
                        )
                        coordinates_to_update.add(EditorTable.Coordinate(y, cursor.beat))
                    }

                    CtlLineLevel.Global -> {
                        val y = this.get_visible_row_from_ctl_line_global(
                            cursor.ctl_type!!
                        )

                        coordinates_to_update.add(EditorTable.Coordinate(y, cursor.beat))
                    }
                }
            }

            OpusManagerCursor.CursorMode.Range -> {
                when (cursor.ctl_level) {
                    null -> {
                        val (top_left, bottom_right) = cursor.get_ordered_range()!!
                        for (beat_key in this.get_beatkeys_in_range(top_left, bottom_right)) {
                            val y = try {
                                this.get_visible_row_from_ctl_line(
                                    this.get_ctl_line_index(
                                        this.get_abs_offset(
                                            beat_key.channel, beat_key.line_offset
                                        )
                                    )
                                ) ?: continue
                            } catch (e: IndexOutOfBoundsException) {
                                continue
                            }

                            coordinates_to_update.add(EditorTable.Coordinate(y, beat_key.beat))
                        }
                    }
                    else -> {
                        val (top_left, bottom_right) = cursor.get_ordered_range()!!
                        val y = when (cursor.ctl_level!!) {
                            // Can assume top_left.channel == bottom_right.channel and top_left.line_offset == bottom_right.line_offset
                            CtlLineLevel.Line -> this.get_visible_row_from_ctl_line_line(
                                cursor.ctl_type!!,
                                top_left.channel,
                                top_left.line_offset
                            )
                            // Can assume top_left.channel == bottom_right.channel
                            CtlLineLevel.Channel -> this.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, top_left.channel)
                            CtlLineLevel.Global -> this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                        }

                        val first_beat = min(top_left.beat, bottom_right.beat)
                        val last_beat = max(top_left.beat, bottom_right.beat)

                        for (x in first_beat..last_beat) {
                            coordinates_to_update.add(EditorTable.Coordinate(y, x))
                        }
                    }
                }
            }

            OpusManagerCursor.CursorMode.Row -> {
                val y = when (cursor.ctl_level) {
                    null -> {
                        try {
                            this.get_visible_row_from_ctl_line(
                                this.get_ctl_line_index(
                                    this.get_abs_offset(
                                        cursor.channel,
                                        cursor.line_offset
                                    )
                                )
                            ) ?: return
                        } catch (e: IndexOutOfBoundsException) {
                            return
                        }
                    }
                    CtlLineLevel.Line -> {
                        this.get_visible_row_from_ctl_line_line(
                            cursor.ctl_type!!,
                            cursor.channel,
                            cursor.line_offset
                        )
                    }
                    CtlLineLevel.Channel -> {
                        this.get_visible_row_from_ctl_line_channel(
                            cursor.ctl_type!!,
                            cursor.channel
                        )
                    }
                    CtlLineLevel.Global -> {
                        this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                    }
                }

                this.ui_change_bill.queue_row_change(y)
            }

            OpusManagerCursor.CursorMode.Column -> {
                this.ui_change_bill.queue_column_change(cursor.beat)
            }
            OpusManagerCursor.CursorMode.Unset -> { }
        }

        this.ui_change_bill.queue_cell_changes(coordinates_to_update.toList())
    }

    private fun _init_editor_table_width_map() {
        if (this.ui_lock.is_full_locked()) {
            return
        }

        val editor_table = this.get_editor_table() ?: return // TODO: Throw Exception?
        editor_table.clear_column_map()

        for (beat in 0 until this.beat_count) {
            val column = mutableListOf<Int>()
            this.get_visible_channels().forEachIndexed { i: Int, channel: OpusChannelAbstract<*,*> ->
                for (j in channel.lines.indices) {
                    val tree = this.get_tree(BeatKey(i, j, beat))
                    if (tree.is_leaf()) {
                        column.add(1)
                    } else {
                        val new_weight = tree.get_max_child_weight() * tree.size
                        column.add(new_weight)
                    }

                    for ((type, controller) in channel.lines[j].controllers.get_all()) {
                        if (! this.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                            continue
                        }
                        val ctl_tree = controller.get_beat(beat)
                        if (ctl_tree.is_leaf()) {
                            column.add(1)
                        } else {
                            val new_weight = ctl_tree.get_max_child_weight() * ctl_tree.size
                            column.add(new_weight)
                        }
                    }
                }

                for ((type, controller) in channel.controllers.get_all()) {
                    if (! this.is_ctl_line_visible(CtlLineLevel.Channel, type)) {
                        continue
                    }
                    val ctl_tree = controller.get_beat(beat)
                    if (ctl_tree.is_leaf()) {
                        column.add(1)
                    } else {
                        val new_weight = ctl_tree.get_max_child_weight() * ctl_tree.size
                        column.add(new_weight)
                    }
                }
            }

            for ((type, controller) in this.controllers.get_all()) {
                if (! this.is_ctl_line_visible(CtlLineLevel.Global, type)) {
                    continue
                }

                val ctl_tree = controller.get_beat(beat)
                if (ctl_tree.is_leaf()) {
                    column.add(1)
                } else {
                    val new_weight = ctl_tree.get_max_child_weight() * ctl_tree.size
                    column.add(new_weight)
                }
            }
            editor_table.add_column_to_map(beat, column)
        }
    }

    private fun _add_line_to_column_width_map(y: Int, line: OpusLineAbstract<*>) {
        if (this.ui_lock.is_full_locked()) {
            return
        }

        this.get_editor_table()?.add_line_to_map(
            y,
            List(this.beat_count) { x: Int ->
                val tree = line.beats[x]
                if (tree.is_leaf()) {
                    1
                } else {
                    tree.get_max_child_weight() * tree.size
                }
            }
        )
        this.ui_change_bill.queue_new_row(y)
    }

    private fun _add_controller_to_column_width_map(y: Int, line: ActiveController) {
        if (this.ui_lock.is_full_locked()) {
            return
        }

        this.get_editor_table()?.add_line_to_map(
            y,
            List(this.beat_count) { x: Int ->
                val tree = line.events[x]
                if (tree == null || tree.is_leaf()) {
                    1
                } else {
                    tree.get_max_child_weight() * tree.size
                }
            }
        )

        this.ui_change_bill.queue_new_row(y)
    }

    private fun _update_after_new_line(channel: Int, line_offset: Int?) {
        if (this.ui_lock.is_full_locked() || this.get_activity() == null) {
            return
        }

        val working_channel = this.get_channel(channel)
        val adj_line_offset = line_offset ?: (working_channel.lines.size - 1)
        val abs_offset = this.get_abs_offset(channel, adj_line_offset)
        val row_index = this.get_ctl_line_index(abs_offset)
        val visible_row = this.get_visible_row_from_ctl_line(row_index) ?: return

        val new_line = if (line_offset == null) {
            working_channel.lines.last()
        } else {
            working_channel.lines[line_offset]
        }

        this._add_line_to_column_width_map(visible_row, new_line)

        val controllers = working_channel.lines[adj_line_offset].controllers.get_all()
        controllers.forEachIndexed { i: Int, (type, controller): Pair<ControlEventType, ActiveController> ->
            if (this.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                this._add_controller_to_column_width_map(visible_row + i, controller)
            }
        }
    }

    private fun _new_column_in_column_width_map(index: Int) {
        if (this.ui_lock.is_full_locked()) {
            return
        }

        val column = mutableListOf<Int>()
        this.get_visible_channels().forEachIndexed { i: Int, channel: OpusChannelAbstract<*,*> ->
            channel.lines.forEachIndexed { j: Int, line: OpusLineAbstract<*> ->
                val tree = this.get_tree(BeatKey(i, j, index))
                if (tree.is_leaf()) {
                    column.add(1)
                } else {
                    column.add(tree.get_max_child_weight() * tree.size)
                }
                for ((type, controller) in channel.lines[j].controllers.get_all()) {
                    if (! this.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                        continue
                    }
                    val ctl_tree = controller.get_beat(index)
                    if (ctl_tree.is_leaf()) {
                        column.add(1)
                    } else {
                        val new_weight = ctl_tree.get_max_child_weight() * ctl_tree.size
                        column.add(new_weight)
                    }
                }
            }
            for ((type, controller) in channel.controllers.get_all()) {
                if (! this.is_ctl_line_visible(CtlLineLevel.Channel, type)) {
                    continue
                }
                val ctl_tree = controller.get_beat(index)
                if (ctl_tree.is_leaf()) {
                    column.add(1)
                } else {
                    val new_weight = ctl_tree.get_max_child_weight() * ctl_tree.size
                    column.add(new_weight)
                }
            }
        }
        for ((type, controller) in this.controllers.get_all()) {
            if (! this.is_ctl_line_visible(CtlLineLevel.Global, type)) {
                continue
            }
            val ctl_tree = controller.get_beat(index)
            if (ctl_tree.is_leaf()) {
                column.add(1)
            } else {
                val new_weight = ctl_tree.get_max_child_weight() * ctl_tree.size
                column.add(new_weight)
            }
        }

        this.get_editor_table()?.add_column_to_map(index, column)
        this.ui_change_bill.queue_add_column(index)
    }

    // UI FUNCS -----------------------
    private fun apply_bill_changes() {
        val editor_table = this.get_editor_table()!!
        this.runOnUiThread { activity: MainActivity ->
            while (true) {
                val entry = this.ui_change_bill.get_next_entry()
                when (entry) {
                    BillableItem.FullRefresh -> {
                        activity.setup_project_config_drawer()
                        activity.validate_percussion_visibility()
                        activity.update_menu_options()

                        this._init_editor_table_width_map()
                        editor_table?.setup(this.get_visible_master_line_count(), this.beat_count)

                        activity.update_channel_instruments()
                        this.withFragment {
                            it.clear_context_menu()
                        }
                    }

                    BillableItem.RowAdd -> {
                        editor_table.new_row(
                            this.ui_change_bill.get_next_int()
                        )
                    }

                    BillableItem.RowRemove -> {
                        editor_table.remove_rows(
                            this.ui_change_bill.get_next_int(),
                            this.ui_change_bill.get_next_int()
                        )
                    }

                    BillableItem.RowChange -> {
                        editor_table.notify_row_changed(
                            this.ui_change_bill.get_next_int()
                        )
                    }

                    BillableItem.ColumnAdd -> {
                        editor_table.new_column(
                            this.ui_change_bill.get_next_int()
                        )

                    }

                    BillableItem.ColumnRemove -> {
                        editor_table.remove_column(
                            this.ui_change_bill.get_next_int()
                        )
                    }

                    BillableItem.ColumnChange -> {
                        val column = this.ui_change_bill.get_next_int()
                        editor_table.recalculate_column_max(column)
                        editor_table.notify_column_changed(column)
                    }

                    BillableItem.CellChange -> {
                        val cells = List<EditorTable.Coordinate>(this.ui_change_bill.get_next_int()) {
                            EditorTable.Coordinate(
                                y = this.ui_change_bill.get_next_int(),
                                x = this.ui_change_bill.get_next_int()
                            )
                        }

                        editor_table.notify_cell_changes(cells)
                    }

                    BillableItem.ChannelChange -> {
                        val channel = this.ui_change_bill.get_next_int()

                        val channel_recycler = activity.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                        if (channel_recycler.adapter != null) {
                            (channel_recycler.adapter as ChannelOptionAdapter).notifyItemChanged(channel)
                        }
                    }

                    BillableItem.ChannelAdd -> {
                        val channel = this.ui_change_bill.get_next_int()

                        activity.update_channel_instruments(channel)

                        val channel_recycler = activity.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                        if (channel_recycler.adapter != null) {
                            val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                            channel_adapter.add_channel()
                        }
                    }

                    BillableItem.ChannelRemove -> {
                        val channel = this.ui_change_bill.get_next_int()
                        val channel_recycler = activity.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                        if (channel_recycler.adapter != null) {
                            val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                            channel_adapter.remove_channel(channel)
                        }
                    }

                    BillableItem.ProjectNameChange -> {
                        activity.update_title_text()
                    }

                    BillableItem.ContextMenuRefresh -> {
                        this.withFragment {
                            it.refresh_context_menu()
                        }
                    }

                    BillableItem.ContextMenuSetLine -> {
                        this.withFragment {
                            it.set_context_menu_line()
                        }
                    }

                    BillableItem.ContextMenuSetLeaf -> {
                        this.withFragment {
                            it.set_context_menu_leaf()
                        }
                    }

                    BillableItem.ContextMenuSetLeafPercussion -> {
                        this.withFragment {
                            it.set_context_menu_leaf_percussion()
                        }
                    }

                    BillableItem.ContextMenuSetControlLeaf -> {
                        this.withFragment {
                            it.set_context_menu_line_control_leaf()
                        }
                    }

                    BillableItem.ContextMenuSetControlLeafB -> {
                        this.withFragment {
                            it.set_context_menu_line_control_leaf_b()
                        }
                    }

                    BillableItem.ContextMenuSetLinking -> {
                        this.withFragment {
                            it.set_context_menu_linking()
                        }
                    }

                    BillableItem.ContextMenuSetColumn -> {
                        this.withFragment {
                            it.set_context_menu_column()
                        }
                    }

                    BillableItem.ContextMenuSetControlLine -> {
                        this.withFragment {
                            it.set_context_menu_control_line()
                        }
                    }

                    BillableItem.ContextMenuClear -> {
                        this.withFragment {
                            it.clear_context_menu()
                        }
                    }

                    BillableItem.ConfigDrawerEnableCopyAndDelete -> {
                        // TODO: Move to MainActivity method
                        activity.findViewById<View>(R.id.btnDeleteProject).isEnabled = true
                        activity.findViewById<View>(R.id.btnCopyProject).isEnabled = true
                    }

                    BillableItem.ConfigDrawerRefreshExportButton -> {
                        activity.setup_project_config_drawer_export_button()
                    }

                    BillableItem.PercussionButtonRefresh -> {
                        val line_offset = this.ui_change_bill.get_next_int()
                        val btnChoosePercussion: TextView = activity.findViewById(R.id.btnChoosePercussion) ?: return@runOnUiThread
                        val instrument = this.get_percussion_instrument(line_offset)

                        if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            btnChoosePercussion.text = activity.getString(R.string.label_short_percussion, instrument)
                        } else {
                            btnChoosePercussion.text = activity.getString(
                                R.string.label_choose_percussion,
                                instrument,
                                activity.get_drum_name(instrument)
                            )
                        }
                    }

                    BillableItem.LineLabelRefresh -> {
                        editor_table.update_line_label(this.ui_change_bill.get_next_int())
                    }

                    null -> break
                }
            }
        }
    }

    private fun runOnUiThread(callback: (MainActivity) -> Unit) {
        val main = this._activity ?: return // TODO: Throw Exception?
        main.runOnUiThread {
            callback(main)
        }
    }

    // END UI FUNCS -----------------------
}
