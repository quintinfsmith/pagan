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
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusLayerCursor
import com.qfs.pagan.opusmanager.OpusLine
import com.qfs.pagan.opusmanager.OpusLineAbstract
import com.qfs.pagan.opusmanager.OpusLinePercussion
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.OpusReverbEvent
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.OpusVolumeEvent
import com.qfs.pagan.opusmanager.RelativeNoteEvent
import com.qfs.pagan.opusmanager.TunedInstrumentEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class OpusLayerInterface : OpusLayerCursor() {
    class HidingNonEmptyPercussionException: Exception()
    class HidingLastChannelException: Exception()
    class MissingEditorTableException: Exception()

    var relative_mode: Int = 0
    var first_load_done = false
    private var _in_reload = false
    private var _activity: MainActivity? = null

    private val _cached_row_map = HashMap<Int, Int>() // Key: visible line, Value: control_line
    private val _cached_inv_visible_line_map = HashMap<Int, Int>()
    private val _cached_ctl_map_line = HashMap<Triple<Int, Int, ControlEventType>, Int>()
    private val _cached_ctl_map_channel = HashMap<Pair<Int, ControlEventType>, Int>()
    private val _cached_ctl_map_global = HashMap<ControlEventType, Int>()

    private var _cache_cursor: OpusManagerCursor = OpusManagerCursor(OpusManagerCursor.CursorMode.Unset)

    private var _blocked_action_catcher_active = false

    var marked_range: Pair<BeatKey, BeatKey>? = null

    private val ui_change_bill = UIChangeBill()
    var temporary_blocker: OpusManagerCursor? = null

    fun attach_activity(activity: MainActivity) {
        this._activity = activity
    }

    fun get_activity(): MainActivity? {
        return this._activity
    }

    private fun get_editor_table(): EditorTable {
        return this._activity?.findViewById(R.id.etEditorTable) ?: throw MissingEditorTableException()
    }

    // UI BILL Interface functions ---------------------------------
    private fun <T> lock_ui_full(callback: () -> T): T {
        this.ui_change_bill.lock_full()

        val output = try {
            callback()
        } catch (e: Exception) {
            this.ui_change_bill.unlock()
            this.ui_change_bill.cancel_most_recent()
            //if (!this.ui_change_bill.is_locked()) {
            //    this.apply_bill_changes()
            //}
            throw e
        }

        this.ui_change_bill.unlock()
        if (!this.ui_change_bill.is_locked()) {
            this.apply_bill_changes()
        }
        return output
    }

    private fun <T> lock_ui_partial(callback: () -> T): T {
        this.ui_change_bill.lock_partial()

        val output = try {
            callback()
        } catch (e: Exception) {
            this.ui_change_bill.unlock()
            this.ui_change_bill.cancel_most_recent()

            //if (!this.ui_change_bill.is_locked()) {
            //    this.apply_bill_changes()
            //}
            throw e
        }

        this.ui_change_bill.unlock()
        if (!this.ui_change_bill.is_locked()) {
            this.apply_bill_changes()
        }
        return output
    }

    /* Notify the editor table to update cells */
    private fun _queue_cell_changes(beat_keys: List<BeatKey>) {
        if (this.ui_change_bill.is_full_locked()) {
            return
        }

        val coord_list = List(beat_keys.size) { i: Int ->
            EditorTable.Coordinate(
                try {
                    this.get_visible_row_from_ctl_line(
                        this.get_actual_line_index(
                            this.get_instrument_line_index(
                                beat_keys[i].channel,
                                beat_keys[i].line_offset
                            )
                        )
                    )!!
                } catch (e: IndexOutOfBoundsException) { // may reference a channel's line before the channel exists
                    this.get_row_count()
                },
                beat_keys[i].beat
            )
        }

        this.ui_change_bill.queue_cell_changes(coord_list)
    }

    /* Notify the editor table to update a cell */
    private fun _queue_cell_change(beat_key: BeatKey) {
        if (this.ui_change_bill.is_full_locked()) {
            return
        }

        val activity = this.get_activity()
        if (activity != null && !activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
            return
        }

        val tree = this.get_tree(beat_key)
        val new_weight = tree.get_total_child_weight()

        val coord_list = mutableListOf<EditorTable.Coordinate>()
        coord_list.add(
            EditorTable.Coordinate(
                y = this.get_visible_row_from_ctl_line(
                    this.get_actual_line_index(
                        this.get_instrument_line_index(
                            beat_key.channel,
                            beat_key.line_offset
                        )
                    )
                )!!,
                x = beat_key.beat
            )
        )

        val editor_table = this.get_editor_table()
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
        if (!this.is_ctl_line_visible(CtlLineLevel.Global, type)) {
            return
        }
        if (this.ui_change_bill.is_full_locked()) {
            return
        }

        val coord = EditorTable.Coordinate(
            y = this._cached_ctl_map_global[type]!!,
            x = beat
        )

        val tree = this.controllers.get_controller<OpusControlEvent>(type).get_tree(beat)
        val new_weight = tree.get_total_child_weight()

        val editor_table = this.get_editor_table()
        if (editor_table.set_mapped_width(coord.y, coord.x, new_weight)) {
            this.ui_change_bill.queue_column_change(coord.x)
        } else {
            this.ui_change_bill.queue_cell_change(coord)
        }
    }

    private fun _queue_channel_ctl_cell_change(type: ControlEventType, channel: Int, beat: Int) {
        if (!this.is_ctl_line_visible(CtlLineLevel.Channel, type)) {
            return
        }
        if (this.ui_change_bill.is_full_locked()) {
            return
        }

        val coord = EditorTable.Coordinate(
            y = this._cached_ctl_map_channel[Pair(channel, type)]!!,
            x = beat
        )

        val tree = this.get_all_channels()[channel].controllers.get_controller<OpusControlEvent>(type).get_tree(beat)
        val new_weight = tree.get_total_child_weight()

        val editor_table = this.get_editor_table()
        if (editor_table.set_mapped_width(coord.y, coord.x, new_weight)) {
            //editor_table.recalculate_column_max(coord.x)
            this.ui_change_bill.queue_column_change(coord.x)
        } else {
            this.ui_change_bill.queue_cell_change(coord)
        }
    }

    private fun _queue_line_ctl_cell_change(type: ControlEventType, beat_key: BeatKey) {
        if (!this.is_ctl_line_visible(CtlLineLevel.Line, type)) {
           return
        }

        val coord = EditorTable.Coordinate(
            y = this._cached_ctl_map_line[Triple(beat_key.channel, beat_key.line_offset, type)]!!,
            x = beat_key.beat
        )

        val tree = this.get_line_ctl_tree<OpusControlEvent>(type, beat_key)
        val new_weight = tree.get_total_child_weight()

        val editor_table = this.get_editor_table()
        if (editor_table.set_mapped_width(coord.y, coord.x, new_weight)) {
            //editor_table.recalculate_column_max(coord.x)
            this.ui_change_bill.queue_column_change(coord.x)
        } else {
            this.ui_change_bill.queue_cell_change(coord)
        }
    }
    // END UI BILL Interface functions ---------------------------------W

    /*
        Wrap a function such that if it throws a BlockedTreeException,
        it is caught and provides feedback, but doesn't stop the propogation of other
        exceptions OR BlockedTreeExceptions thrown in a higher degree _catched_blocked_action call.
        eg: move_leaf is wrapped but also calls replace_tree, which is wrapped so the exception
        will be handled at the first level so the rest of the function is still cancelled
     */
    private fun <T> _catch_blocked_action(callback: () -> T): T? {
        return if (this._blocked_action_catcher_active) {
            callback()
        } else {
            this._blocked_action_catcher_active = true
            val output = try {
                callback()
            } catch (e: OpusLayerBase.BlockedTreeException) { // Standard leaf
                this.set_temporary_blocker(
                    BeatKey(
                        e.channel,
                        e.e.line_offset,
                        e.e.e.blocker_beat
                    ),
                    e.e.e.blocker_position
                )
                null
            } catch (e: OpusLayerBase.BlockedLineCtlTreeException) { // line control leaf
                this.set_temporary_blocker_line_ctl(
                    e.e.e.type,
                    BeatKey(
                        e.channel,
                        e.e.line_offset,
                        e.e.e.e.blocker_beat,
                    ),
                    e.e.e.e.blocker_position
                )
                null
            } catch (e: OpusLayerBase.BlockedChannelCtlTreeException) { // channel control leaf
                this.set_temporary_blocker_channel_ctl(
                    e.e.e.type,
                    e.channel,
                    e.e.e.e.blocker_beat,
                    e.e.e.e.blocker_position
                )
                null
            } catch (e: OpusLineAbstract.BlockedCtlTreeException) { // Global Control Leaf
                this.set_temporary_blocker_global_ctl(e.type, e.e.blocker_beat, e.e.blocker_position)
                null
            }
            // global is just a OpusLineAbstract.BlockedCtlTree at this layer
            this._blocked_action_catcher_active = false

            output
        }
    }

    override fun remove_at_cursor(count: Int) {
        this._catch_blocked_action {
            this.lock_ui_partial {
                this.queue_cursor_update(this.cursor.copy())
                super.remove_at_cursor(count)
            }
        }
    }

    override fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>) {
        this.lock_ui_partial {
            super.set_channel_instrument(channel, instrument)
            if (!this.ui_change_bill.is_full_locked()) {
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
            if (!this.ui_change_bill.is_full_locked()) {
                this.ui_change_bill.queue_project_name_change()
            }
        }
    }


    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.unset(beat_key, position)

            this._queue_cell_change(beat_key)
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
        this._catch_blocked_action {
            this.lock_ui_partial {
                val activity = this.get_activity()
                if (activity != null && !activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
                    this.make_percussion_visible()
                }
                super.replace_tree(beat_key, position, tree)
                this._queue_cell_change(beat_key)
            }
        }
    }

    override fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        this._catch_blocked_action {
            this.lock_ui_partial {
                super.move_leaf(beatkey_from, position_from, beatkey_to, position_to)
            }
        }
    }

    override fun <T: OpusControlEvent> replace_global_ctl_tree(type: ControlEventType, beat: Int, position: List<Int>?, tree: OpusTree<T>) {
        this.lock_ui_partial {
            super.replace_global_ctl_tree(type, beat, position, tree)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun <T: OpusControlEvent> replace_channel_ctl_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>?, tree: OpusTree<T>) {
        this.lock_ui_partial {
            super.replace_channel_ctl_tree(type, channel, beat, position, tree)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun <T: OpusControlEvent> replace_line_ctl_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>?, tree: OpusTree<T>) {
        this.lock_ui_partial {
            super.replace_line_ctl_tree(type, beat_key, position, tree)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun <T: OpusControlEvent> set_global_ctl_event(type: ControlEventType, beat: Int, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.set_global_ctl_event(type, beat, position, event)
            this._queue_global_ctl_cell_change(type, beat)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: OpusControlEvent> set_channel_ctl_event(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.set_channel_ctl_event(type, channel, beat, position, event)
            this._queue_channel_ctl_cell_change(type, channel, beat)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: OpusControlEvent> set_line_ctl_event(type: ControlEventType, beat_key: BeatKey, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.set_line_ctl_event(type, beat_key, position, event)
            this._queue_line_ctl_cell_change(type, beat_key)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        this._catch_blocked_action {
            this.lock_ui_partial {
                val activity = this.get_activity()

                if (activity != null && !activity.view_model.show_percussion && this.is_percussion(beat_key.channel)) {
                    this.make_percussion_visible()
                }

                super.set_event(beat_key, position, event)

                if (event is TunedInstrumentEvent) {
                    this.set_relative_mode(event)
                }

                if (!this.ui_change_bill.is_full_locked()) {
                    this._queue_cell_change(beat_key)
                    this.ui_change_bill.queue_refresh_context_menu()
                }
            }
        }
    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this._catch_blocked_action {
            this.lock_ui_partial {
                super.set_percussion_event(beat_key, position)
                val activity = this.get_activity()
                if (activity != null && !activity.view_model.show_percussion) {
                    this.make_percussion_visible()
                }

                this._queue_cell_change(beat_key)
            }
        }
    }

    override fun set_percussion_event_at_cursor() {
        this.lock_ui_partial {
            super.set_percussion_event_at_cursor()
            if (!this.ui_change_bill.is_full_locked()) {
                this.ui_change_bill.queue_refresh_context_menu()
            }
        }
    }

    override fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        this.lock_ui_partial {
            super.set_percussion_instrument(line_offset, instrument)
            // Need to call get_drum name to repopulate instrument list if needed
            this.get_activity()?.get_drum_name(instrument)

            if (!this.ui_change_bill.is_full_locked()) {
                this.ui_change_bill.queue_refresh_choose_percussion_button(line_offset)
                this.ui_change_bill.queue_line_label_refresh(
                    this.get_visible_row_from_ctl_line(
                        this.get_actual_line_index(
                            this.get_instrument_line_index(this.channels.size, line_offset)
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
                this._queue_cell_change(beat_key)
            }
        }
    }

    override fun split_global_ctl_tree(type: ControlEventType, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_ui_partial {
            super.split_global_ctl_tree(type, beat, position, splits, move_event_to_end)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun split_channel_ctl_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_ui_partial {
            super.split_channel_ctl_tree(type, channel, beat, position, splits, move_event_to_end)

            if ((this._activity != null && this._activity!!.view_model.show_percussion) || !this.is_percussion(channel)) {
                this._queue_channel_ctl_cell_change(type, channel, beat)
            }
        }
    }

    override fun split_line_ctl_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_ui_partial {
            super.split_line_ctl_tree(type, beat_key, position, splits, move_event_to_end)

            if ((this._activity != null && this._activity!!.view_model.show_percussion) || !this.is_percussion(beat_key.channel)) {
                this._queue_line_ctl_cell_change(type, beat_key)
            }
        }
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.insert_after(beat_key, position)
            this._queue_cell_change(beat_key)
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
            this._queue_cell_change(beat_key)
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

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this._catch_blocked_action {
            super.remove(beat_key, position)
        }
    }

    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.remove_standard(beat_key, position)
            this._queue_cell_change(beat_key)
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
            val output = super.new_line(channel, line_offset)
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
        this.set_overlap_callbacks()
    }


    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        this.lock_ui_partial {
            val vis_line_a = this.get_visible_row_from_ctl_line(
                this.get_actual_line_index(
                    this.get_instrument_line_index(channel_a, line_a)
                )
            )!!

            val vis_line_b = this.get_visible_row_from_ctl_line(
                this.get_actual_line_index(
                    this.get_instrument_line_index(channel_b, line_b)
                )
            )!!

            super.swap_lines(channel_a, line_a, channel_b, line_b)

            this.get_editor_table().swap_mapped_lines(vis_line_a, vis_line_b)
            this.ui_change_bill.queue_row_change(vis_line_a)
            this.ui_change_bill.queue_row_change(vis_line_b)
        }
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusLineAbstract<*> {
        return this.lock_ui_partial {
            val activity = this.get_activity()
            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
                this.make_percussion_visible()
            }

            val abs_line = this.get_visible_row_from_ctl_line(
                this.get_actual_line_index(
                    this.get_instrument_line_index(channel, line_offset)
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
        if (!this.ui_change_bill.is_full_locked()) {
            val column_updates = this.get_editor_table()?.remove_mapped_lines(y, count) ?: listOf()
            this.ui_change_bill.queue_row_removal(y, count)
            for (column in column_updates) {
                this.ui_change_bill.queue_column_change(column)
            }
        }
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        this.lock_ui_partial {
            val notify_index = channel ?: this.channels.size

            super.new_channel(channel, lines, uuid)

            if (!this.ui_change_bill.is_full_locked()) {
                val line_list = mutableListOf<OpusLine>()
                for (i in 0 until lines) {
                    line_list.add(this.channels[notify_index].lines[i])
                }
                val y = this.get_instrument_line_index(notify_index, 0)

                this.ui_change_bill.queue_add_channel(notify_index)

                val activity = this.get_activity()
                if (activity == null || !this.is_percussion(notify_index) || activity.view_model.show_percussion) {
                    var ctl_row = this.get_visible_row_from_ctl_line(
                        this.get_actual_line_index(y)
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

    override fun remove_beat(beat_index: Int, count: Int) {
        this._catch_blocked_action {
            this.lock_ui_partial {
                if (!this.ui_change_bill.is_full_locked()) {
                    this.queue_cursor_update(this.cursor)
                    val x = min(beat_index, this.beat_count - 1 - count)
                    for (i in 0 until count) {
                        this.get_editor_table().remove_mapped_column(x)
                        this.ui_change_bill.queue_remove_column(x)
                    }
                }

                super.remove_beat(beat_index, count)

                if (!this.ui_change_bill.is_full_locked()) {
                    this.queue_cursor_update(this.cursor)
                    this.ui_change_bill.queue_refresh_context_menu()
                }
            }
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        this.lock_ui_partial {
            if (!this.ui_change_bill.is_full_locked()) {
                this.queue_cursor_update(this.cursor)
                this.ui_change_bill.queue_add_column(beat_index)

                // Need to find all notes that overflow into the proceding beats and queue a column refresh on those beats
                if (beat_index > 0 && beat_index < this.beat_count) {
                    val channels = this.get_all_channels()
                    for (i in 0 until channels.size) {
                        for (j in 0 until channels[i].lines.size) {
                            var working_beat = beat_index
                            val line = channels[i].lines[j]
                            var working_position = line.get_first_position(working_beat, listOf())

                            val head_position = line.get_blocking_position(working_beat, working_position) ?: Pair(working_beat, working_position)
                            if (head_position.first < beat_index) {
                                var max_beat_blocked = beat_index
                                for ((blocked_beat, _) in line.get_all_blocked_positions(head_position.first, head_position.second)) {
                                    max_beat_blocked = max(max_beat_blocked, blocked_beat)
                                }

                                if (max_beat_blocked > beat_index) {
                                    for (b in beat_index .. max_beat_blocked) {
                                        this.ui_change_bill.queue_column_change(b + 1, true)
                                    }
                                }
                            }

                        }
                    }
                }
            }



            super.insert_beat(beat_index, beats_in_column)

            if (!this.ui_change_bill.is_full_locked()) {
                this._new_column_in_column_width_map(beat_index)
                this.queue_cursor_update(this.cursor)
            }
        }
    }

    override fun insert_beats(beat_index: Int, count: Int) {
        // TODO: is it necessary to queue ui changes in this function?
        // Not sure but i think the new ui logic will be fine to individually call each insert_beat
        super.insert_beats(beat_index, count)
    }

    override fun remove_channel(channel: Int) {
        this.lock_ui_partial {
            if (!this.ui_change_bill.is_full_locked()) {
                val y = try {
                    this.get_instrument_line_index(channel, 0)
                } catch (e: IndexOutOfBoundsException) {
                    this.get_total_line_count()
                }

                val ctl_row = this.get_visible_row_from_ctl_line(this.get_actual_line_index(y))!!
                var removed_row_count = this.channels[channel].size

                val activity = this.get_activity()

                val force_show_percussion = activity != null
                        && !activity.view_model.show_percussion
                        && !this.is_percussion(channel)
                        && this.channels.size == 1

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


                val changed_columns = this.get_editor_table()?.remove_mapped_lines(ctl_row, removed_row_count) ?: listOf()

                super.remove_channel(channel)

                this.ui_change_bill.queue_remove_channel(channel)
                this.ui_change_bill.queue_row_removal(ctl_row, removed_row_count)
                this.ui_change_bill.queue_column_changes(changed_columns, false)

                if (force_show_percussion) {
                    this.make_percussion_visible()
                }
            } else {
                super.remove_channel(channel)
            }
        }
    }


    override fun on_project_changed() {
        super.on_project_changed()
        this.recache_line_maps()
        this.ui_change_bill.queue_full_refresh()
        this.first_load_done = true
    }

    override fun new() {
        this.lock_ui_full {
            this._ui_clear()
            val activity = this.get_activity()!!
            activity.view_model.show_percussion = true

            super.new()
            this.on_project_changed()

            val new_path = activity.get_new_project_path()
            this.path = new_path
        }
    }

    override fun import_midi(midi: Midi) {
        this.lock_ui_full {
            this._ui_clear()

            super.import_midi(midi)
            this.on_project_changed()

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
            this.on_project_changed()

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


    override fun clear() {
        super.clear()

        this._cached_row_map.clear()
        this._cached_inv_visible_line_map.clear()
        this._cached_ctl_map_line.clear()
        this._cached_ctl_map_channel.clear()
        this._cached_ctl_map_global.clear()

        val editor_table = this.get_editor_table()
        editor_table?.clear()
        this.runOnUiThread {
            editor_table?.precise_scroll(0, 0, 0, 0)
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

    fun set_relative_mode(mode: Int, update_ui: Boolean = true) {
        this.relative_mode = mode
        this.lock_ui_partial {
            if (update_ui) {
                this.ui_change_bill.queue_refresh_context_menu()
            }
        }
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
        this._catch_blocked_action {
            this.lock_ui_partial {
                super.set_duration(beat_key, position, duration)

                // Needs to be set to trigger potentially queued cell changes from on_overlap()
                this._queue_cell_change(beat_key)
                // val btnDuration: TextView = main.findViewById(R.id.btnDuration) ?: return@runOnUiThread
                // btnDuration.text = main.getString(R.string.label_duration, duration)
                this.ui_change_bill.queue_refresh_context_menu()
            }
        }
    }

    // Cursor Functions ////////////////////////////////////////////////////////////////////////////
    override fun cursor_apply(cursor: OpusManagerCursor) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_apply(cursor)

            this.queue_cursor_update(this.cursor)

            when (cursor.mode) {
                OpusManagerCursor.CursorMode.Line -> {
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
                    this.ui_change_bill.queue_set_context_menu_range()
                }
                OpusManagerCursor.CursorMode.Unset -> {
                    this.ui_change_bill.queue_clear_context_menu()
                }
            }
        }
    }

    override fun cursor_clear() {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_clear()
            this.temporary_blocker = null

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_clear_context_menu()
        }
    }

    override fun cursor_select_line(channel: Int, line_offset: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_select_line(channel, line_offset)
            this.temporary_blocker = null

            val activity = this.get_activity()
            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
                this.make_percussion_visible()
            }

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_set_context_menu_line()
        }
    }

    override fun cursor_select_channel_ctl_line(ctl_type: ControlEventType, channel: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_select_channel_ctl_line(ctl_type, channel)
            this.temporary_blocker = null

            val activity = this.get_activity()
            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
                this.make_percussion_visible()
            }

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_set_context_menu_control_line()
        }
    }

    override fun cursor_select_line_ctl_line(ctl_type: ControlEventType, channel: Int, line_offset: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_select_line_ctl_line(ctl_type, channel, line_offset)
            this.temporary_blocker = null

            val activity = this.get_activity()
            if (activity != null && !activity.view_model.show_percussion && this.is_percussion(channel)) {
                this.make_percussion_visible()
            }

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_set_context_menu_control_line()
        }
    }

    override fun cursor_select_global_ctl_line(ctl_type: ControlEventType) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_select_global_ctl_line(ctl_type)
            this.temporary_blocker = null

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_set_context_menu_control_line()
        }
    }

    override fun cursor_select_column(beat: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_select_column(beat)
            this.temporary_blocker = null

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_set_context_menu_column()
        }
    }

    override fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this.temporary_blocker = null
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
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this.unset_temporary_blocker()
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
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this.unset_temporary_blocker()
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
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this.unset_temporary_blocker()
            super.cursor_select_ctl_at_global(ctl_type, beat, position)

            this.queue_cursor_update(this.cursor, false)
            this.ui_change_bill.queue_set_context_menu_line_control_leaf()
        }
    }


    override fun cursor_select_global_ctl_range(type: ControlEventType, first: Int, second: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this.unset_temporary_blocker()
            super.cursor_select_global_ctl_range(type, first, second)

            this.queue_cursor_update(this.cursor, false)
            this.ui_change_bill.queue_set_context_menu_line_control_leaf_b()
        }
    }

    override fun cursor_select_line_ctl_range(type: ControlEventType, beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this.unset_temporary_blocker()
            super.cursor_select_line_ctl_range(type, beat_key_a, beat_key_b)

            this.queue_cursor_update(this.cursor, false)
            this.ui_change_bill.queue_set_context_menu_line_control_leaf_b()
        }
    }

    override fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this.unset_temporary_blocker()
            super.cursor_select_range(beat_key_a, beat_key_b)

            this.queue_cursor_update(this.cursor, false)
            this.ui_change_bill.queue_set_context_menu_range()
        }
    }

    fun set_temporary_blocker(beat_key: BeatKey, position: List<Int>) {
        this.get_activity()?.vibrate()
        this.lock_ui_partial {
            this.cursor_select(beat_key, position)
            this.temporary_blocker = this.cursor.copy()
        }
    }
    fun set_temporary_blocker_line_ctl(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.get_activity()?.vibrate()
        this.lock_ui_partial {
            this.cursor_select_ctl_at_line(type, beat_key, position)
            this.temporary_blocker = this.cursor.copy()
        }
    }
    fun set_temporary_blocker_channel_ctl(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.get_activity()?.vibrate()
        this.lock_ui_partial {
            this.cursor_select_ctl_at_channel(type, channel, beat, position)
            this.temporary_blocker = this.cursor.copy()
        }
    }

    fun set_temporary_blocker_global_ctl(type: ControlEventType, beat: Int, position: List<Int>) {
        this.get_activity()?.vibrate()
        this.lock_ui_partial {
            this.cursor_select_ctl_at_global(type, beat, position)
            this.temporary_blocker = this.cursor.copy()
        }
    }

    fun unset_temporary_blocker() {
        this.lock_ui_partial {
            val blocker = this.temporary_blocker
            if (blocker != null) {
                when (blocker.ctl_level) {
                    CtlLineLevel.Line -> {
                        this._queue_line_ctl_cell_change(blocker.ctl_type!!, blocker.get_beatkey())
                    }
                    CtlLineLevel.Channel -> {
                        this._queue_channel_ctl_cell_change(blocker.ctl_type!!, blocker.channel, blocker.beat)
                    }
                    CtlLineLevel.Global -> {
                        this._queue_global_ctl_cell_change(blocker.ctl_type!!, blocker.beat)
                    }
                    null -> {
                        this._queue_cell_change(blocker.get_beatkey())
                    }
                }
            }
            this.temporary_blocker = null
        }
    }

    // End Cursor Functions ////////////////////////////////////////////////////////////////////////
    override fun save(path: String?) {
        this.lock_ui_partial {
            super.save(path)
            this.ui_change_bill.queue_enable_delete_and_copy_buttons()
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
        Get the number of visible lines, control lines included
     */
    fun get_row_count(): Int {
        return this._cached_row_map.size
    }

    /*
        Given the row, get the line number in the Opus
     */
    fun get_ctl_line_from_row(row: Int): Int {
        return this._cached_row_map[row]!!
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
        editor_table.setup(this.get_row_count(), this.beat_count)
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
        editor_table.setup(this.get_row_count(), this.beat_count)
        this.withFragment {
            it.restore_view_model_position()
        }
    }

    override fun recache_line_maps() {
        super.recache_line_maps()
        this._cached_row_map.clear()
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
                    this._cached_row_map[visible_line] = ctl_line
                    visible_line += 1
                }
                ctl_line += 1

                for ((type, _) in channel.lines[line_offset].controllers.get_all()) {
                    if (this.is_ctl_line_visible(CtlLineLevel.Line, type) && !hide_channel) {
                        this._cached_inv_visible_line_map[ctl_line] = visible_line
                        this._cached_row_map[visible_line] = ctl_line
                        this._cached_ctl_map_line[Triple(channel_index, line_offset, type)] = visible_line
                        visible_line += 1
                    }
                    ctl_line += 1
                }
            }

            for (type in channel.controllers.controllers.keys) {
                if (this.is_ctl_line_visible(CtlLineLevel.Channel, type) && !hide_channel) {
                    this._cached_inv_visible_line_map[ctl_line] = visible_line
                    this._cached_row_map[visible_line] = ctl_line
                    this._cached_ctl_map_channel[Pair(channel_index, type)] = visible_line
                    visible_line += 1
                }
                ctl_line += 1
            }
        }

        for (type in this.controllers.controllers.keys) {
            if (this.is_ctl_line_visible(CtlLineLevel.Global, type)) {
                this._cached_inv_visible_line_map[ctl_line] = visible_line
                this._cached_row_map[visible_line] = ctl_line
                this._cached_ctl_map_global[type] = visible_line
                visible_line += 1
            }
            ctl_line += 1
        }

        this.set_overlap_callbacks()
    }

    fun set_overlap_callbacks() {
        val channels = this.get_all_channels()
        for (channel in 0 until channels.size) {
            for (line_offset in 0 until channels[channel].lines.size) {
                val line = channels[channel].lines[line_offset]
                line.overlap_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                    this._queue_cell_changes(
                        listOf(BeatKey(channel, line_offset, blocker.first), BeatKey(channel, line_offset, blocked.first))
                    )
                }
                line.overlap_removed_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                    this._queue_cell_changes(
                        listOf(BeatKey(channel, line_offset, blocker.first), BeatKey(channel, line_offset, blocked.first))
                    )
                }
                for ((type, controller) in line.controllers.get_all()) {
                    controller.overlap_removed_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                        this._queue_line_ctl_cell_change(type, BeatKey(channel, line_offset, blocker.first))
                        this._queue_line_ctl_cell_change(type, BeatKey(channel, line_offset, blocked.first))
                    }
                }
            }
            for ((type, controller) in channels[channel].controllers.get_all()) {
                controller.overlap_removed_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                    this._queue_channel_ctl_cell_change(type, channel, blocker.first)
                    this._queue_channel_ctl_cell_change(type, channel, blocked.first)
                }
                controller.overlap_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                    this._queue_channel_ctl_cell_change(type, channel, blocker.first)
                    this._queue_channel_ctl_cell_change(type, channel, blocked.first)
                }
            }
        }

        for ((type, controller) in this.controllers.get_all()) {
            controller.overlap_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                this._queue_global_ctl_cell_change(type, blocker.first)
                this._queue_global_ctl_cell_change(type, blocked.first)
            }
            controller.overlap_removed_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                this._queue_global_ctl_cell_change(type, blocker.first)
                this._queue_global_ctl_cell_change(type, blocked.first)
            }
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
                            this._queue_cell_change(beat_key)
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

            this.ui_change_bill.queue_refresh_channel(this.channels.size)

            var ctl_line = this.get_visible_row_from_ctl_line(
                this.get_actual_line_index(
                    this.get_instrument_line_index(this.channels.size, 0)
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

    override fun <T: OpusControlEvent> set_global_controller_initial_event(type: ControlEventType, event: T) {
        this.lock_ui_partial {
            super.set_global_controller_initial_event(type, event)
            this.ui_change_bill.queue_refresh_context_menu() 
        }
    }

    override fun <T: OpusControlEvent> set_channel_controller_initial_event(type: ControlEventType, channel: Int, event: T) {
        this.lock_ui_partial {
            super.set_channel_controller_initial_event(type, channel, event)
            this.ui_change_bill.queue_refresh_context_menu() 
        }
    }

    override fun <T: OpusControlEvent> set_line_controller_initial_event(type: ControlEventType, channel: Int, line_offset: Int, event: T) {
        this.lock_ui_partial {
            super.set_line_controller_initial_event(type, channel, line_offset, event)
            this.ui_change_bill.queue_refresh_context_menu() 
        }
    }

   // override fun on_overlap(overlapper: Pair<BeatKey, List<Int>>,overlappee: Pair<BeatKey, List<Int>>) {
   //     this.lock_ui_partial {
   //         this._queue_cell_change(overlappee.first, true)
   //     }
   // }

   // override fun on_overlap_removed(overlapper: Pair<BeatKey, List<Int>>,overlappee: Pair<BeatKey, List<Int>>) {
   //     this.lock_ui_partial {
   //         this._queue_cell_change(overlappee.first, true)
   //     }
   // }

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
                        // TODO: I think its possible to have oob beats from get_all_blocked_keys, NEED CHECK
                        val y = try {
                            this.get_visible_row_from_ctl_line(
                                this.get_actual_line_index(
                                    this.get_instrument_line_index(
                                        beat_key.channel,
                                        beat_key.line_offset
                                    )
                                )
                            )
                        } catch (e: IndexOutOfBoundsException) {
                            return
                        }

                        if (y != null) {
                            val line = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset]
                            val shadow_beats = mutableSetOf<Int>()
                            val event_head = line.get_blocking_position(beat_key.beat, cursor.position) ?: Pair(beat_key.beat, cursor.position)
                            for ((shadow_beat, _) in line.get_all_blocked_positions(event_head.first, event_head.second)) {
                                shadow_beats.add(shadow_beat)
                            }

                            for (shadow_beat in shadow_beats) {

                                if (beat_key.channel == beat_key.channel && beat_key.line_offset == beat_key.line_offset && shadow_beat == beat_key.beat) {
                                    this.ui_change_bill.queue_line_label_refresh(y)
                                    this.ui_change_bill.queue_column_label_refresh(shadow_beat)
                                }
                                coordinates_to_update.add(EditorTable.Coordinate(y, shadow_beat))
                            }
                        }
                    }

                    else -> {
                        val (y, controller) = when (cursor.ctl_level!!) {
                            CtlLineLevel.Line -> {
                                Pair(
                                    this.get_visible_row_from_ctl_line_line(
                                        cursor.ctl_type!!,
                                        cursor.channel,
                                        cursor.line_offset
                                    ),
                                    this.get_all_channels()[cursor.channel].lines[cursor.line_offset].get_controller<OpusControlEvent>(cursor.ctl_type!!)
                                )
                            }

                            CtlLineLevel.Channel -> {
                                Pair(
                                    this.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, cursor.channel),
                                    this.get_all_channels()[cursor.channel].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)
                                )
                            }

                            CtlLineLevel.Global -> {
                                Pair(
                                    this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!),
                                    this.controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)
                                )
                            }
                        }


                        val shadow_beats = mutableSetOf<Int>()
                        val beat = cursor.beat
                        val event_head = controller.get_blocking_position(beat, cursor.position) ?: Pair(beat, cursor.position)
                        for ((shadow_beat, _) in controller.get_all_blocked_positions(event_head.first, event_head.second)) {
                            shadow_beats.add(shadow_beat)
                        }

                        for (shadow_beat in shadow_beats) {
                            if (shadow_beat == beat) {
                                this.ui_change_bill.queue_column_label_refresh(shadow_beat)
                            }
                            coordinates_to_update.add(EditorTable.Coordinate(y, shadow_beat))
                        }

                        this.ui_change_bill.queue_line_label_refresh(y)
                        this.ui_change_bill.queue_column_label_refresh(beat)
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
                                    this.get_actual_line_index(
                                        this.get_instrument_line_index(
                                            beat_key.channel, beat_key.line_offset
                                        )
                                    )
                                ) ?: continue
                            } catch (e: IndexOutOfBoundsException) {
                                continue
                            }

                            this.ui_change_bill.queue_line_label_refresh(y)
                            this.ui_change_bill.queue_column_label_refresh(beat_key.beat)

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

                        this.ui_change_bill.queue_line_label_refresh(y)
                        for (x in first_beat..last_beat) {
                            this.ui_change_bill.queue_column_label_refresh(x)
                            coordinates_to_update.add(EditorTable.Coordinate(y, x))
                        }
                    }
                }
            }

            OpusManagerCursor.CursorMode.Line -> {
                val y = when (cursor.ctl_level) {
                    null -> {
                        try {
                            this.get_visible_row_from_ctl_line(
                                this.get_actual_line_index(
                                    this.get_instrument_line_index(
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

                this.ui_change_bill.queue_row_change(y, true)
            }

            OpusManagerCursor.CursorMode.Column -> {
                this.ui_change_bill.queue_column_change(cursor.beat, false)
            }
            OpusManagerCursor.CursorMode.Unset -> { }
        }

        this.ui_change_bill.queue_cell_changes(coordinates_to_update.toList(), true)
    }

    fun _init_editor_table_width_map() {
        if (this.ui_change_bill.is_full_locked()) {
            return
        }

        val editor_table = this.get_editor_table() ?: return // TODO: Throw Exception?
        editor_table.clear_column_map()

        for (beat in 0 until this.beat_count) {
            val column = mutableListOf<Int>()
            this.get_visible_channels().forEachIndexed { i: Int, channel: OpusChannelAbstract<*,*> ->
                for (j in channel.lines.indices) {
                    val tree = this.get_tree(BeatKey(i, j, beat))
                    column.add(tree.get_total_child_weight())

                    for ((type, controller) in channel.lines[j].controllers.get_all()) {
                        if (! this.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                            continue
                        }
                        val ctl_tree = controller.get_tree(beat)
                        column.add(ctl_tree.get_total_child_weight())
                    }
                }

                for ((type, controller) in channel.controllers.get_all()) {
                    if (! this.is_ctl_line_visible(CtlLineLevel.Channel, type)) {
                        continue
                    }
                    val ctl_tree = controller.get_tree(beat)
                    column.add(ctl_tree.get_total_child_weight())
                }
            }

            for ((type, controller) in this.controllers.get_all()) {
                if (! this.is_ctl_line_visible(CtlLineLevel.Global, type)) {
                    continue
                }

                val ctl_tree = controller.get_tree(beat)
                column.add(ctl_tree.get_total_child_weight())
            }
            editor_table.add_column_to_map(beat, column)
        }
    }

    private fun _add_line_to_column_width_map(y: Int, line: OpusLineAbstract<*>) {
        if (this.ui_change_bill.is_full_locked()) {
            return
        }

        val column_updates = this.get_editor_table()?.add_line_to_map(
            y,
            List(this.beat_count) { x: Int ->
                val tree = line.beats[x]
                tree.get_total_child_weight()
            }
        ) ?: listOf()

        this.ui_change_bill.queue_new_row(y)
        this.ui_change_bill.queue_column_changes(column_updates, false)
    }

    private fun _add_controller_to_column_width_map(y: Int, line: ActiveController<*>) {
        if (this.ui_change_bill.is_full_locked()) {
            return
        }

        val column_updates = this.get_editor_table()?.add_line_to_map(
            y,
            List(this.beat_count) { x: Int ->
                val tree = line.beats[x]
                tree?.get_total_child_weight() ?: 1
            }
        ) ?: listOf()

        this.ui_change_bill.queue_new_row(y)
        this.ui_change_bill.queue_column_changes(column_updates, false)
    }

    private fun _update_after_new_line(channel: Int, line_offset: Int?) {
        if (this.ui_change_bill.is_full_locked() || this.get_activity() == null) {
            return
        }

        val working_channel = this.get_channel(channel)
        val adj_line_offset = line_offset ?: (working_channel.lines.size - 1)
        val abs_offset = this.get_instrument_line_index(channel, adj_line_offset)
        val row_index = this.get_actual_line_index(abs_offset)
        val visible_row = this.get_visible_row_from_ctl_line(row_index) ?: return

        val new_line = if (line_offset == null) {
            working_channel.lines.last()
        } else {
            working_channel.lines[line_offset]
        }

        this._add_line_to_column_width_map(visible_row, new_line)

        val controllers = working_channel.lines[adj_line_offset].controllers.get_all()
        controllers.forEachIndexed { i: Int, (type, controller): Pair<ControlEventType, ActiveController<*>> ->
            if (this.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                this._add_controller_to_column_width_map(visible_row + i, controller)
            }
        }
    }

    private fun _new_column_in_column_width_map(index: Int) {
        if (this.ui_change_bill.is_full_locked()) {
            return
        }

        val column = mutableListOf<Int>()
        this.get_visible_channels().forEachIndexed { i: Int, channel: OpusChannelAbstract<*,*> ->
            channel.lines.forEachIndexed { j: Int, line: OpusLineAbstract<*> ->
                val tree = this.get_tree(BeatKey(i, j, index))
                column.add(tree.get_total_child_weight())
                for ((type, controller) in channel.lines[j].controllers.get_all()) {
                    if (! this.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                        continue
                    }
                    val ctl_tree = controller.get_tree(index)
                    column.add(ctl_tree.get_total_child_weight())
                }
            }

            for ((type, controller) in channel.controllers.get_all()) {
                if (! this.is_ctl_line_visible(CtlLineLevel.Channel, type)) {
                    continue
                }
                val ctl_tree = controller.get_tree(index)
                column.add(ctl_tree.get_total_child_weight())
            }
        }
        for ((type, controller) in this.controllers.get_all()) {
            if (! this.is_ctl_line_visible(CtlLineLevel.Global, type)) {
                continue
            }
            val ctl_tree = controller.get_tree(index)
            column.add(ctl_tree.get_total_child_weight())
        }

        this.get_editor_table()?.add_column_to_map(index, column)
    }

    // UI FUNCS -----------------------
    private fun apply_bill_changes() {
        val editor_table = try {
            this.get_editor_table()
        } catch (e: MissingEditorTableException) {
            this.ui_change_bill.clear()
            return
        }
        this.runOnUiThread { activity: MainActivity ->
            this.ui_change_bill.consolidate()
            while (true) {
                val entry = this.ui_change_bill.get_next_entry()
                when (entry) {
                    BillableItem.FullRefresh -> {
                        activity.setup_project_config_drawer()
                        activity.validate_percussion_visibility()
                        activity.update_menu_options()

                        this._init_editor_table_width_map()
                        editor_table?.setup(this.get_row_count(), this.beat_count)

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
                        if (column < editor_table.get_column_map_size()) {
                            editor_table.recalculate_column_max(column)
                            editor_table.notify_column_changed(column)
                        }
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
                        val channel_recycler = activity.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                        if (channel_recycler.adapter != null) {
                            val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                            channel_adapter.add_channel()
                        }

                        activity.update_channel_instruments(channel)
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
                            val channels = this.get_all_channels()
                            val controller_set = when (this.cursor.ctl_level) {
                                CtlLineLevel.Line -> {
                                    channels[this.cursor.channel].lines[this.cursor.line_offset].controllers
                                }
                                CtlLineLevel.Channel -> {
                                    val channel = this.cursor.channel
                                    channels[channel].controllers
                                }
                                CtlLineLevel.Global -> {
                                    this.controllers
                                }
                                null -> return@withFragment
                            }

                            when (this.cursor.ctl_type) {
                                ControlEventType.Tempo -> {
                                    val controller = controller_set.get_controller<OpusTempoEvent>(this.cursor.ctl_type!!)
                                    it.set_context_menu_line_control_leaf(
                                        ControlWidgetTempo(controller.initial_event, false, this.get_activity()!!) { event: OpusTempoEvent ->
                                            this.set_event_at_cursor(event)
                                        }
                                    )
                                }
                                ControlEventType.Volume -> {
                                    val controller = controller_set.get_controller<OpusVolumeEvent>(this.cursor.ctl_type!!)
                                    it.set_context_menu_line_control_leaf(
                                        ControlWidgetVolume(controller.initial_event, false, this.get_activity()!!) { event: OpusVolumeEvent ->
                                            this.set_event_at_cursor(event)
                                        }
                                    )
                                }
                                ControlEventType.Reverb -> {
                                    val controller = controller_set.get_controller<OpusReverbEvent>(this.cursor.ctl_type!!)
                                    it.set_context_menu_line_control_leaf(
                                        ControlWidgetReverb(controller.initial_event, false, this.get_activity()!!) { event: OpusReverbEvent ->
                                            this.set_event_at_cursor(event)
                                        }
                                    )
                                }
                                null -> return@withFragment
                            }
                        }
                    }

                    BillableItem.ContextMenuSetControlLeafB -> {
                        this.withFragment {
                            it.set_context_menu_line_control_leaf_b()
                        }
                    }

                    BillableItem.ContextMenuSetRange -> {
                        this.withFragment {
                            it.set_context_menu_range()
                        }
                    }

                    BillableItem.ContextMenuSetColumn -> {
                        this.withFragment {
                            it.set_context_menu_column()
                        }
                    }

                    BillableItem.ContextMenuSetControlLine -> {
                        this.withFragment {
                            val channels = this.get_all_channels()
                            val controller_set = when (this.cursor.ctl_level) {
                                CtlLineLevel.Line -> {
                                    channels[this.cursor.channel].lines[this.cursor.line_offset].controllers
                                }
                                CtlLineLevel.Channel -> {
                                    val channel = this.cursor.channel
                                    channels[channel].controllers
                                }
                                CtlLineLevel.Global -> {
                                    this.controllers
                                }
                                null -> return@withFragment
                            }
                            when (this.cursor.ctl_type) {
                                ControlEventType.Tempo -> {
                                    val controller = controller_set.get_controller<OpusTempoEvent>(this.cursor.ctl_type!!)
                                    it.set_context_menu_control_line(
                                        ControlWidgetTempo(controller.initial_event, true, this.get_activity()!!) { event: OpusTempoEvent ->
                                            this.set_event_at_cursor(event)
                                        }
                                    )
                                }
                                ControlEventType.Volume -> {
                                    val controller = controller_set.get_controller<OpusVolumeEvent>(this.cursor.ctl_type!!)
                                    it.set_context_menu_control_line(
                                        ControlWidgetVolume(controller.initial_event, true, this.get_activity()!!) { event: OpusVolumeEvent ->
                                            this.set_event_at_cursor(event)
                                        }
                                    )
                                }
                                ControlEventType.Reverb -> {
                                    val controller = controller_set.get_controller<OpusReverbEvent>(this.cursor.ctl_type!!)
                                    it.set_context_menu_control_line(
                                        ControlWidgetReverb(controller.initial_event, true, this.get_activity()!!) { event: OpusReverbEvent ->
                                            this.set_event_at_cursor(event)
                                        }
                                    )
                                }
                                null -> return@withFragment
                            }
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
                        val btnChoosePercussion: TextView = activity.findViewById(R.id.btnChoosePercussion) ?: continue
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
                    BillableItem.ColumnLabelRefresh -> {
                        editor_table.update_column_label(this.ui_change_bill.get_next_int())
                    }
                    BillableItem.ColumnStateChange -> {
                        val column = this.ui_change_bill.get_next_int()
                        if (column < editor_table.get_column_map_size()) {
                            editor_table.notify_column_changed(column, true)
                        }
                    }
                    BillableItem.RowStateChange -> {
                        val y = this.ui_change_bill.get_next_int()
                        editor_table.notify_row_changed(y,true)
                    }
                    BillableItem.CellStateChange -> {
                        val cells = List<EditorTable.Coordinate>(this.ui_change_bill.get_next_int()) {
                            EditorTable.Coordinate(
                                y = this.ui_change_bill.get_next_int(),
                                x = this.ui_change_bill.get_next_int()
                            )
                        }
                        editor_table.notify_cell_changes(cells, true)
                    }

                    null -> break
                }
            }
            this.ui_change_bill.clear()
        }
    }

    private fun runOnUiThread(callback: (MainActivity) -> Unit) {
        val main = this._activity ?: return // TODO: Throw Exception?
        main.runOnUiThread {
            callback(main)
        }
    }

    private fun _block_cursor_selection(): Boolean {
        return (this._blocked_action_catcher_active && this.temporary_blocker != null)
    }

    override fun move_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        this._catch_blocked_action {
            super.move_beat_range(beat_key, first_corner, second_corner)
        }
    }

    fun get_visible_channel_count(): Int {
        val activity = this.get_activity()
        return if (activity != null && !activity.view_model.show_percussion) {
            this.channels.size
        } else {
            this.channels.size + 1
        }
    }

    fun get_nth_next_channel_at_cursor(n: Int): Int? {
        return when (cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single -> {
                val start_channel = when (cursor.ctl_level) {
                    CtlLineLevel.Global -> 0
                    null,
                    CtlLineLevel.Line,
                    CtlLineLevel.Channel -> cursor.channel
                }

                max(0, min(start_channel + n, this.get_visible_channel_count() - 1))
            }

            OpusManagerCursor.CursorMode.Column -> {
                max(0, min(n - 1, this.get_visible_channel_count() - 1))
            }

            OpusManagerCursor.CursorMode.Range,
            OpusManagerCursor.CursorMode.Unset -> null
        }
    }

    fun select_first_leaf_in_previous_beat(repeat: Int = 1) {
        when (this.cursor.ctl_level) {
            CtlLineLevel.Line -> {
                var working_beat = this.cursor.beat
                var working_position = this.cursor.position
                val controller = this.channels[this.cursor.channel].lines[this.cursor.line_offset].controllers.get_controller<OpusControlEvent>(this.cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(this.cursor.ctl_type!!, BeatKey(working_beat, this.cursor.channel, this.cursor.line_offset), working_position)
            }
            CtlLineLevel.Channel -> {
                var working_beat = this.cursor.beat
                val channel = this.cursor.channel
                var working_position = this.cursor.position
                val controller = this.channels[channel].controllers.get_controller<OpusControlEvent>(this.cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_channel(this.cursor.ctl_type!!, channel, working_beat, working_position)
            }
            CtlLineLevel.Global -> {
                var working_beat = this.cursor.beat
                var working_position = this.cursor.position
                val controller = this.controllers.get_controller<OpusControlEvent>(this.cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_global(this.cursor.ctl_type!!, working_beat, working_position)

            }
            null -> {
                var working_beat_key = this.cursor.get_beatkey()
                var working_position = this.cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_preceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select(working_beat_key, working_position)
            }
        }
    }

    fun select_first_leaf_in_next_beat(repeat: Int = 1) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_line_ctl_proceding_leaf_position(
                        cursor.ctl_type!!,
                        working_beat_key,
                        working_position
                    ) ?: break

                    working_beat_key.beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(cursor.ctl_type!!, working_beat_key, working_position)
            }
            CtlLineLevel.Channel -> {
                var working_beat = cursor.beat
                val channel = cursor.channel
                var working_position = cursor.position
                val controller = this.channels[channel].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_proceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_channel(cursor.ctl_type!!, channel, working_beat, working_position)
            }
            CtlLineLevel.Global -> {
                var working_beat = cursor.beat
                var working_position = cursor.position
                for (i in 0 until repeat) {
                    val next_pair = this.get_global_ctl_proceding_leaf_position(
                        cursor.ctl_type!!,
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_global(cursor.ctl_type!!, working_beat, working_position)

            }
            null -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_proceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select(working_beat_key, working_position)
            }
        }
    }

    fun move_to_previous_visible_line(repeat: Int = 1) {
        val cursor = this.cursor
        if (cursor.mode != OpusManagerCursor.CursorMode.Line) {
            throw Exception("Incorrect Cursor Mode ${cursor.mode}")
        }

        var visible_row = when (cursor.ctl_level) {
            null -> {
                this.get_visible_row_from_ctl_line(
                    this.get_actual_line_index(
                        this.get_instrument_line_index(
                            cursor.channel,
                            cursor.line_offset
                        )
                    )
                )

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
            CtlLineLevel.Global -> this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
        }!!

        visible_row = max(0, visible_row - repeat)

        val (pointer, control_level, control_type) = this.get_ctl_line_info(
            this.get_ctl_line_from_row(visible_row)
        )

        when (control_level) {
            null -> {
                val (new_channel, new_line_offset) = this.get_channel_and_line_offset(pointer)
                this.cursor_select_line(new_channel, new_line_offset)

            }
            CtlLineLevel.Line -> {
                val (new_channel, new_line_offset) = this.get_channel_and_line_offset(pointer)
                this.cursor_select_line_ctl_line(
                    control_type!!,
                    new_channel,
                    new_line_offset,
                )
            }
            CtlLineLevel.Channel -> {
                this.cursor_select_channel_ctl_line(
                    control_type!!,
                    pointer
                )
            }
            CtlLineLevel.Global -> this.cursor_select_global_ctl_line(control_type!!)
        }
    }

    fun move_to_next_visible_line(repeat: Int = 1) {
        val cursor = this.cursor
        if (cursor.mode != OpusManagerCursor.CursorMode.Line) {
            throw Exception("Incorrect Cursor Mode ${cursor.mode}")
        }

        var visible_row = when (cursor.ctl_level) {
            null -> {
                this.get_visible_row_from_ctl_line(
                    this.get_actual_line_index(
                        this.get_instrument_line_index(
                            cursor.channel,
                            cursor.line_offset
                        )
                    )
                )

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
            CtlLineLevel.Global -> this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
        }!!

        visible_row = max(0, min(this.get_total_line_count() - 1,visible_row + repeat))

        val (pointer, control_level, control_type) = this.get_ctl_line_info(
            this.get_ctl_line_from_row(visible_row)
        )

        when (control_level) {
            null -> {
                val (new_channel, new_line_offset) = this.get_channel_and_line_offset(pointer)
                this.cursor_select_line(new_channel, new_line_offset)

            }
            CtlLineLevel.Line -> {
                val (new_channel, new_line_offset) = this.get_channel_and_line_offset(pointer)
                this.cursor_select_line_ctl_line(
                    control_type!!,
                    new_channel,
                    new_line_offset,
                )
            }
            CtlLineLevel.Channel -> {
                this.cursor_select_channel_ctl_line(
                    control_type!!,
                    pointer
                )
            }
            CtlLineLevel.Global -> this.cursor_select_global_ctl_line(control_type!!)
        }
    }

    fun select_next_leaf(repeat: Int) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_line_ctl_proceding_leaf_position(
                        cursor.ctl_type!!,
                        working_beat_key,
                        working_position
                    ) ?: break

                    working_beat_key.beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(cursor.ctl_type!!, working_beat_key, working_position)
            }

            CtlLineLevel.Channel -> {
                var working_beat = cursor.beat
                val channel = cursor.channel
                var working_position = cursor.position
                val controller = this.channels[channel].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_proceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_channel(cursor.ctl_type!!, channel, working_beat, working_position)
            }

            CtlLineLevel.Global -> {
                var working_beat = cursor.beat
                var working_position = cursor.position
                for (i in 0 until repeat) {
                    val next_pair = this.get_global_ctl_proceding_leaf_position(
                        cursor.ctl_type!!,
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_global(cursor.ctl_type!!, working_beat, working_position)

            }
            null -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_proceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select(working_beat_key, working_position)
            }
        }
    }

    fun select_previous_leaf(repeat: Int) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()
                val controller = this.channels[working_beat_key.channel].lines[working_beat_key.line_offset].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat_key.beat,
                        working_position
                    ) ?: break

                    working_beat_key.beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(cursor.ctl_type!!, working_beat_key, working_position)
            }

            CtlLineLevel.Channel -> {
                var working_beat = cursor.beat
                val channel = cursor.channel
                var working_position = cursor.position
                val controller = this.channels[channel].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_channel(cursor.ctl_type!!, channel, working_beat, working_position)
            }

            CtlLineLevel.Global -> {
                var working_beat = cursor.beat
                var working_position = cursor.position
                val controller = this.controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)
                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_global(cursor.ctl_type!!, working_beat, working_position)

            }
            null -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_preceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select(working_beat_key, working_position)
            }
        }
    }

    fun set_note_octave_at_cursor(octave: Int) {
        if (this.cursor.mode != OpusManagerCursor.CursorMode.Single) {
            throw Exception("Incorrect Cursor Mode: ${this.cursor.mode}")
        }
        val current_tree_position = this.get_actual_position(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
        this.set_note_octave(current_tree_position.first, current_tree_position.second, octave)
    }

    fun set_note_octave(beat_key: BeatKey, position: List<Int>, octave: Int) {
        val current_tree_position = this.get_actual_position(
            beat_key,
            position
        )
        val current_tree = this.get_tree(current_tree_position.first, current_tree_position.second)

        val duration = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            event.duration
        } else {
            1
        }


        val radix = this.tuning_map.size
        val current_event = current_tree.get_event()
        var convert_to_rel_flag = false
        val value = when (this.relative_mode) {
            0 -> {
                when (current_event) {
                    is AbsoluteNoteEvent -> (octave * radix) + (current_event.note % radix)
                    null -> {
                        val cursor = this.cursor
                        val previous_value = this.get_absolute_value(cursor.get_beatkey(), cursor.get_position()) ?: 0
                        (octave * radix) + (previous_value % radix)
                    }
                    else -> {
                        // TODO: Specify (Shouldn't be reachable)
                        throw Exception()
                    }
                }
            }
            1 -> {
                when (current_event) {
                    is RelativeNoteEvent -> {
                        (octave * radix) + (current_event.offset % radix)
                    }
                    is AbsoluteNoteEvent -> {
                        val activity = this.get_activity()
                        if (activity != null) {
                            val nsOffset = activity!!.findViewById<NumberSelector>(R.id.nsOffset)
                            nsOffset.setState(0, manual = true, surpress_callback = true)
                        }
                        convert_to_rel_flag = true
                        octave * radix
                    }
                    null -> {
                        convert_to_rel_flag = true
                        (octave * radix)
                    }
                    else -> {
                        // TODO: Specify (Shouldn't be reachable)
                        throw Exception()
                    }
                }
            }
            2 -> {
                when (current_event) {
                    is RelativeNoteEvent -> {
                        0 - ((octave * radix) + (abs(current_event.offset) % radix))
                    }
                    is AbsoluteNoteEvent -> {
                        val activity = this.get_activity()
                        if (activity != null) {
                            val nsOffset = activity!!.findViewById<NumberSelector>(R.id.nsOffset)
                            nsOffset.setState(0, manual = true, surpress_callback = true)
                        }
                        convert_to_rel_flag = true
                        0 - (octave * radix)
                    }
                    null -> {
                        convert_to_rel_flag = true
                        0 - (octave * radix)
                    }
                    else -> {
                        // TODO: Specify (Shouldn't be reachable)
                        throw Exception()
                    }
                }

            }
            else -> {
                // TODO: Specify (Shouldn't be reachable)
                throw Exception()
            }
        }

        this.set_event(
            beat_key,
            position,
            when (current_event) {
                is RelativeNoteEvent -> {
                    RelativeNoteEvent(value, duration)
                }
                null,
                is AbsoluteNoteEvent -> {
                    if (convert_to_rel_flag) {
                        RelativeNoteEvent(value, duration)
                    } else {
                        AbsoluteNoteEvent(value, duration)
                    }
                }
                else -> {
                    // TODO: Specify (Shouldn't be reachable)
                    throw Exception()
                }
            }
        )
    }


    fun set_note_offset_at_cursor(offset: Int) {
        if (this.cursor.mode != OpusManagerCursor.CursorMode.Single) {
            throw Exception("Incorrect Cursor Mode: ${this.cursor.mode}")
        }
        val current_tree_position = this.get_actual_position(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
        this.set_note_offset(current_tree_position.first, current_tree_position.second, offset)
    }

    fun set_note_offset(beat_key: BeatKey, position: List<Int>, offset: Int) {
        val current_tree = this.get_tree(beat_key, position)

        val duration = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            event.duration
        } else {
            1
        }

        val radix = this.tuning_map.size
        val current_event = current_tree.get_event()
        var convert_to_rel_flag = false

        val value = when (this.relative_mode) {
            0 -> {
                when (current_event) {
                    is AbsoluteNoteEvent -> ((current_event.note / radix) * radix) + offset
                    null -> {
                        val cursor = this.cursor
                        val previous_value = this.get_absolute_value(beat_key, position) ?: 0
                        ((previous_value / radix) * radix) + offset
                    }
                    else -> {
                        // TODO: Specify (Shouldn't be reachable)
                        throw Exception()
                    }
                }
            }
            1 -> {
                when (current_event) {
                    is RelativeNoteEvent -> {
                        ((current_event.offset / radix) * radix) + offset
                    }
                    is AbsoluteNoteEvent -> {
                        val activity = this.get_activity() 
                        if (activity != null) {
                            val nsOctave = activity!!.findViewById<NumberSelector>(R.id.nsOctave)
                            nsOctave.setState(0, manual = true, surpress_callback = true)
                        }
                        convert_to_rel_flag = true
                        offset
                    }
                    null -> {
                        convert_to_rel_flag = true
                        offset
                    }
                    else -> {
                        // TODO: Specify (Shouldn't be reachable)
                        throw Exception()
                    }
                }
            }
            2 -> {
                when (current_event) {
                    is RelativeNoteEvent -> {
                        ((current_event.offset / radix) * radix) - offset
                    }
                    is AbsoluteNoteEvent -> {
                        val activity = this.get_activity() 
                        if (activity != null) {
                            val nsOctave = activity.findViewById<NumberSelector>(R.id.nsOctave)
                            nsOctave.setState(0, manual = true, surpress_callback = true)
                        }
                        convert_to_rel_flag = true
                        0 - offset
                    }
                    null -> {
                        convert_to_rel_flag = true
                        0 - offset
                    }
                    else -> {
                        // TODO: Specify (Shouldn't be reachable)
                        throw Exception()
                    }
                }

            }
            else -> {
                // TODO: Specify (Shouldn't be reachable)
                throw Exception()
            }
        }

        this.set_event(
            beat_key,
            position,
            when (current_event) {
                is RelativeNoteEvent -> {
                    RelativeNoteEvent(value, duration)
                }
                null,
                is AbsoluteNoteEvent -> {
                    if (convert_to_rel_flag) {
                        RelativeNoteEvent(value, duration)
                    } else {
                        AbsoluteNoteEvent(value, duration)
                    }
                }
                else -> {
                    // TODO: Specify (Shouldn't be reachable)
                    throw Exception()
                }
            }
        )
    }

    // END UI FUNCS -----------------------
}
