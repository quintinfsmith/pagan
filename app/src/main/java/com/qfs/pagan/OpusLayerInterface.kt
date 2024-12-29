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
import com.qfs.pagan.opusmanager.HistoryCache
import com.qfs.pagan.opusmanager.HistoryToken
import com.qfs.pagan.opusmanager.InstrumentEvent
import com.qfs.pagan.opusmanager.OpusChannelAbstract
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusLayerHistory
import com.qfs.pagan.opusmanager.OpusLineAbstract
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.RelativeNoteEvent
import com.qfs.pagan.opusmanager.TunedInstrumentEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class OpusLayerInterface : OpusLayerHistory() {
    class HidingNonEmptyPercussionException: Exception()
    class HidingLastChannelException: Exception()
    class MissingEditorTableException: Exception()

    var relative_mode: Int = 0
    var first_load_done = false
    private var _activity: MainActivity? = null

    private var _cache_cursor: OpusManagerCursor = OpusManagerCursor(OpusManagerCursor.CursorMode.Unset)

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

    // UI BILL Interface functions vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    private fun <T> lock_ui_full(callback: () -> T): T? {
        this.ui_change_bill.lock_full()
        val output = try {
            callback()
        } catch (e: OpusLayerBase.BlockedActionException) {
            this.ui_change_bill.cancel_most_recent()
            if (this._blocked_action_catcher > 0) {
                this.ui_change_bill.unlock()
                throw e
            }
            null
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

    private fun <T> lock_ui_partial(callback: () -> T): T? {
        this.ui_change_bill.lock_partial()

        val output = try {
            callback()
        } catch (e: OpusLayerBase.BlockedActionException) {
            this.ui_change_bill.cancel_most_recent()
            if (this._blocked_action_catcher > 0) {
                this.ui_change_bill.unlock()
                throw e
            }
            null
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
        if (this.project_changing) {
            return
        }
        if (this.ui_change_bill.is_full_locked()) {
            return
        }

        if (!this.percussion_channel.visible && this.is_percussion(beat_key.channel)) {
            return
        }

        val tree = this.get_tree(beat_key)
        val new_weight = tree.get_total_child_weight()

        val coord = EditorTable.Coordinate(
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

        val editor_table = this.get_editor_table()

        if (editor_table.set_mapped_width(coord.y, coord.x, new_weight)) {
            this.ui_change_bill.queue_column_change(coord.x)
        } else {
            this.ui_change_bill.queue_cell_change(coord)
        }
    }

    private fun _queue_global_ctl_cell_change(type: ControlEventType, beat: Int) {
        val controller = this.controllers.get_controller<OpusControlEvent>(type)
        if (!controller.visible) {
            return
        }

        if (this.ui_change_bill.is_full_locked()) {
            return
        }

        val coord = EditorTable.Coordinate(
            y = this.get_visible_row_from_ctl_line_global(type),
            x = beat
        )

        val tree = controller.get_tree(beat)
        val new_weight = tree.get_total_child_weight()

        val editor_table = this.get_editor_table()
        if (editor_table.set_mapped_width(coord.y, coord.x, new_weight)) {
            this.ui_change_bill.queue_column_change(coord.x)
        } else {
            this.ui_change_bill.queue_cell_change(coord)
        }
    }

    private fun _queue_channel_ctl_cell_change(type: ControlEventType, channel: Int, beat: Int) {
        val controller = this.get_all_channels()[channel].controllers.get_controller<OpusControlEvent>(type)
        if (!controller.visible) {
            return
        }
        if (this.ui_change_bill.is_full_locked()) {
            return
        }

        val coord = EditorTable.Coordinate(
            y = this.get_visible_row_from_ctl_line_channel(type, channel),
            x = beat
        )

        val tree = controller.get_tree(beat)
        val new_weight = tree.get_total_child_weight()

        val editor_table = this.get_editor_table()
        if (editor_table.set_mapped_width(coord.y, coord.x, new_weight)) {
            this.ui_change_bill.queue_column_change(coord.x)
        } else {
            this.ui_change_bill.queue_cell_change(coord)
        }
    }

    private fun _queue_line_ctl_cell_change(type: ControlEventType, beat_key: BeatKey) {
        val controller = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].controllers.get_controller<OpusControlEvent>(type)
        if (!controller.visible) {
           return
        }

        val coord = EditorTable.Coordinate(
            y = this.get_visible_row_from_ctl_line_line(type, beat_key.channel, beat_key.line_offset),
            x = beat_key.beat
        )

        val tree = this.get_line_ctl_tree<OpusControlEvent>(type, beat_key)
        val new_weight = tree.get_total_child_weight()

        val editor_table = this.get_editor_table()
        if (editor_table.set_mapped_width(coord.y, coord.x, new_weight)) {
            this.ui_change_bill.queue_column_change(coord.x)
        } else {
            this.ui_change_bill.queue_cell_change(coord)
        }
    }
    // UI BILL Interface functions ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    // BASE FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    override fun remove_global_controller(type: ControlEventType) {
        this.lock_ui_partial {
            if (this.is_global_ctl_visible(type)) {
                val abs_line = this.get_visible_row_from_ctl_line_global(type)
                this._queue_remove_rows(abs_line, 1)
            }
            super.remove_global_controller(type)
        }
    }

    override fun remove_line_controller(type: ControlEventType, channel_index: Int, line_offset: Int) {
        this.lock_ui_partial {
            if (this.is_line_ctl_visible(type, channel_index, line_offset )) {
                val abs_line = this.get_visible_row_from_ctl_line_line(type, channel_index, line_offset)
                this._queue_remove_rows(abs_line, 1)
            }
            super.remove_line_controller(type, channel_index, line_offset)
        }
    }

    override fun remove_channel_controller(type: ControlEventType, channel_index: Int) {
        this.lock_ui_partial {
            if (this.is_channel_ctl_visible(type, channel_index)) {
                val abs_line = this.get_visible_row_from_ctl_line_channel(type, channel_index)
                this._queue_remove_rows(abs_line, 1)
            }
            super.remove_channel_controller(type, channel_index)
        }
    }

    // This is just a basic  clear so the UI works while I build the new logic for handling active control visibilities
    // TODO: create per-level logic so it's not just a cursor_clear
    private fun _controller_visibility_toggle_callback() {
        val editor_table = this.get_editor_table() ?: return
        this.withFragment {
            it.backup_position()
        }
        editor_table.clear()

        this._init_editor_table_width_map()
        editor_table.setup(this.get_row_count(), this.beat_count)
        this.withFragment {
            it.restore_view_model_position()
            try {
                it.refresh_context_menu()
            } catch (e: Exception) {
                it.clear_context_menu()
            }
        }
    }
    override fun set_line_controller_visibility(type: ControlEventType, channel_index: Int, line_offset: Int, visibility: Boolean) {
        this.lock_ui_partial {
            if (visibility) {
                super.set_line_controller_visibility(type, channel_index, line_offset, visibility)
                val visible_row = this.get_visible_row_from_ctl_line_line(type, channel_index, line_offset)
                val working_channel = this.get_channel(channel_index)
                val controller = working_channel.lines[line_offset].get_controller<OpusControlEvent>(type)
                this._add_controller_to_column_width_map(visible_row, controller)
            } else {
                val visible_row = this.get_visible_row_from_ctl_line_line(type, channel_index, line_offset)
                super.set_line_controller_visibility(type, channel_index, line_offset, visibility)
                this._queue_remove_rows(visible_row, 1)
            }
        }
    }
    override fun set_channel_controller_visibility(type: ControlEventType, channel_index: Int, visibility: Boolean) {
        this.lock_ui_partial {
            if (visibility) {
                super.set_channel_controller_visibility(type, channel_index, visibility)

                val visible_row = this.get_visible_row_from_ctl_line_channel(type, channel_index)
                val working_channel = this.get_channel(channel_index)
                val controller = working_channel.controllers.get_controller<OpusControlEvent>(type)
                this._add_controller_to_column_width_map(visible_row, controller)
            } else {
                val visible_row = this.get_visible_row_from_ctl_line_channel(type, channel_index)
                super.set_channel_controller_visibility(type, channel_index, visibility)
                this._queue_remove_rows(visible_row, 1)
            }
        }
    }

    override fun set_global_controller_visibility(type: ControlEventType, visibility: Boolean) {
        this.lock_ui_partial {
            if (visibility) {
                super.set_global_controller_visibility(type, visibility)
                val visible_row = this.get_visible_row_from_ctl_line_global(type)
                val controller = this.controllers.get_controller<OpusControlEvent>(type)
                this._add_controller_to_column_width_map(visible_row, controller)
            } else {
                val visible_row = this.get_visible_row_from_ctl_line_global(type)
                super.set_global_controller_visibility(type, visibility)
                this._queue_remove_rows(visible_row, 1)
            }
        }
    }

    override fun set_channel_visibility(channel_index: Int, visibility: Boolean) {
        this.lock_ui_partial {
            if (visibility) {
                super.set_channel_visibility(channel_index, visibility)
                this._post_new_channel(channel_index, this.get_all_channels()[channel_index].lines.size)
            } else {
                val (ctl_row, removed_row_count, changed_columns) = this._pre_remove_channel(channel_index)
                super.set_channel_visibility(channel_index, visibility)

                this.ui_change_bill.queue_row_removal(ctl_row, removed_row_count)
                this.ui_change_bill.queue_column_changes(changed_columns, false)
            }
            this.ui_change_bill.queue_refresh_channel(channel_index)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun convert_events_in_line_to_absolute(channel: Int, line_offset: Int) {
        super.convert_events_in_line_to_absolute(channel, line_offset)
    }

    override fun convert_events_in_tree_to_absolute(beat_key: BeatKey, position: List<Int>) {
        super.convert_events_in_tree_to_absolute(beat_key, position)
    }

    override fun convert_events_in_beat_to_absolute(beat: Int) {
        super.convert_events_in_beat_to_absolute(beat)
    }

    override fun convert_events_in_line_to_relative(channel: Int, line_offset: Int) {
        super.convert_events_in_line_to_relative(channel, line_offset)
    }

    override fun convert_events_in_tree_to_relative(beat_key: BeatKey, position: List<Int>) {
        super.convert_events_in_tree_to_relative(beat_key, position)
    }

    override fun convert_events_in_beat_to_relative(beat: Int) {
        super.convert_events_in_beat_to_relative(beat)
    }

    override fun convert_event_to_absolute(beat_key: BeatKey, position: List<Int>) {
        super.convert_event_to_absolute(beat_key, position)
    }

    override fun convert_event_to_relative(beat_key: BeatKey, position: List<Int>) {
        super.convert_event_to_relative(beat_key, position)
    }

    override fun toggle_line_controller_visibility(type: ControlEventType, channel_index: Int, line_offset: Int) {
        super.toggle_line_controller_visibility(type, channel_index, line_offset)
    }

    override fun toggle_global_control_visibility(type: ControlEventType) {
        super.toggle_global_control_visibility(type)
    }

    override fun toggle_channel_controller_visibility(type: ControlEventType, channel_index: Int) {
        super.toggle_channel_controller_visibility(type, channel_index)
    }

    override fun set_project_name(new_name: String?) {
        this.lock_ui_partial {
            super.set_project_name(new_name)
            if (!this.ui_change_bill.is_full_locked()) {
                this.ui_change_bill.queue_project_name_change()
            }
        }
    }

    override fun set_transpose(new_transpose: Int) {
        super.set_transpose(new_transpose)
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.unset(beat_key, position)

            this._queue_cell_change(beat_key)
        }
    }

    override fun controller_global_unset(type: ControlEventType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_global_unset(type, beat, position)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun controller_channel_unset(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_channel_unset(type, channel, beat, position)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun controller_line_unset(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_line_unset(type, beat_key, position)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<out InstrumentEvent>) {
        this.lock_ui_partial {
            if (this.is_percussion(beat_key.channel) && !this.is_channel_visible(this.channels.size)) {
                this.make_percussion_visible()
            }
            super.replace_tree(beat_key, position, tree)
            this._queue_cell_change(beat_key)
        }
    }

    override fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        this.lock_ui_partial {
            super.move_leaf(beatkey_from, position_from, beatkey_to, position_to)
        }
    }

    override fun controller_channel_move_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        super.controller_channel_move_leaf(type, channel_from, beat_from, position_from, channel_to, beat_to, position_to)
    }

    override fun controller_channel_to_line_move_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        super.controller_channel_to_line_move_leaf(type, channel_from, beat_from, position_from, beat_key_to, position_to)
    }

    override fun controller_line_to_global_move_leaf(type: ControlEventType, beatkey_from: BeatKey, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        super.controller_line_to_global_move_leaf(type, beatkey_from, position_from, target_beat, target_position)
    }

    override fun controller_global_to_line_move_leaf(type: ControlEventType, beat: Int, position: List<Int>, target_key: BeatKey, target_position: List<Int>) {
        super.controller_global_to_line_move_leaf(type, beat, position, target_key, target_position)
    }

    override fun controller_channel_to_global_move_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        super.controller_channel_to_global_move_leaf(type, channel_from, beat_from, position_from, target_beat, target_position)
    }

    override fun controller_global_move_leaf(type: ControlEventType, beat_from: Int, position_from: List<Int>, beat_to: Int, position_to: List<Int>) {
        super.controller_global_move_leaf(type, beat_from, position_from, beat_to, position_to)
    }

    override fun controller_global_to_channel_move_leaf(type: ControlEventType, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        super.controller_global_to_channel_move_leaf(type, beat_from, position_from, channel_to, beat_to, position_to)
    }

    override fun controller_line_move_leaf(type: ControlEventType, beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        super.controller_line_move_leaf(type, beatkey_from, position_from, beatkey_to, position_to)
    }

    override fun controller_line_to_channel_move_leaf(type: ControlEventType, beatkey_from: BeatKey, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        super.controller_line_to_channel_move_leaf(type, beatkey_from, position_from, channel_to, beat_to, position_to)
    }

    override fun _controller_global_copy_range(type: ControlEventType, target: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        super._controller_global_copy_range(type, target, point_a, point_b, unset_original)
    }

    override fun _controller_global_to_channel_copy_range(type: ControlEventType, target_channel: Int, target_beat: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        super._controller_global_to_channel_copy_range(type, target_channel, target_beat, point_a, point_b, unset_original)
    }

    override fun _controller_global_to_line_copy_range(type: ControlEventType, beat_a: Int, beat_b: Int, target_key: BeatKey, unset_original: Boolean) {
        super._controller_global_to_line_copy_range(type, beat_a, beat_b, target_key, unset_original)
    }

    override fun _controller_channel_to_global_copy_range(type: ControlEventType, target_beat: Int, original_channel: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        super._controller_channel_to_global_copy_range(type, target_beat, original_channel, point_a, point_b, unset_original)
    }

    override fun _controller_channel_copy_range(type: ControlEventType, target_channel: Int, target_beat: Int, original_channel: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        super._controller_channel_copy_range(type, target_channel, target_beat, original_channel, point_a, point_b, unset_original)
    }

    override fun _controller_channel_to_line_copy_range(type: ControlEventType, channel_from: Int, beat_a: Int, beat_b: Int, target_key: BeatKey, unset_original: Boolean) {
        super._controller_channel_to_line_copy_range(type, channel_from, beat_a, beat_b, target_key, unset_original)
    }

    override fun _controller_line_to_global_copy_range(type: ControlEventType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_beat: Int, unset_original: Boolean) {
        super._controller_line_to_global_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_beat, unset_original)
    }

    override fun _controller_line_to_channel_copy_range(type: ControlEventType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_channel: Int, target_beat: Int, unset_original: Boolean) {
        super._controller_line_to_channel_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_channel, target_beat, unset_original)
    }

    override fun _controller_line_copy_range(type: ControlEventType, beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey, unset_original: Boolean) {
        super._controller_line_copy_range(type, beat_key, first_corner, second_corner, unset_original)
    }

    override fun move_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        super.move_beat_range(beat_key, first_corner, second_corner)
    }

    override fun unset_line(channel: Int, line_offset: Int) {
        super.unset_line(channel, line_offset)
    }

    override fun controller_line_unset_line(type: ControlEventType, channel: Int, line_offset: Int) {
        super.controller_line_unset_line(type, channel, line_offset)
    }

    override fun controller_channel_unset_line(type: ControlEventType, channel: Int) {
        super.controller_channel_unset_line(type, channel)
    }

    override fun controller_global_unset_line(type: ControlEventType) {
        super.controller_global_unset_line(type)
    }

    override fun unset_range(first_corner: BeatKey, second_corner: BeatKey) {
        super.unset_range(first_corner, second_corner)
    }

    override fun controller_channel_unset_range(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int) {
        super.controller_channel_unset_range(type, channel, first_beat, second_beat)
    }

    override fun controller_global_unset_range(type: ControlEventType, first_beat: Int, second_beat: Int) {
        super.controller_global_unset_range(type, first_beat, second_beat)
    }

    override fun controller_line_unset_range(type: ControlEventType, first_corner: BeatKey, second_corner: BeatKey) {
        super.controller_line_unset_range(type, first_corner, second_corner)
    }

    override fun <T: OpusControlEvent> controller_global_replace_tree(type: ControlEventType, beat: Int, position: List<Int>?, tree: OpusTree<T>) {
        this.lock_ui_partial {
            super.controller_global_replace_tree(type, beat, position, tree)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun <T: OpusControlEvent> controller_channel_replace_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>?, tree: OpusTree<T>) {
        this.lock_ui_partial {
            super.controller_channel_replace_tree(type, channel, beat, position, tree)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun <T: OpusControlEvent> controller_line_replace_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>?, tree: OpusTree<T>) {
        this.lock_ui_partial {
            super.controller_line_replace_tree(type, beat_key, position, tree)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun <T: OpusControlEvent> controller_global_set_event(type: ControlEventType, beat: Int, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.controller_global_set_event(type, beat, position, event)
            this._queue_global_ctl_cell_change(type, beat)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: OpusControlEvent> controller_channel_set_event(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.controller_channel_set_event(type, channel, beat, position, event)
            this._queue_channel_ctl_cell_change(type, channel, beat)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: OpusControlEvent> controller_line_set_event(type: ControlEventType, beat_key: BeatKey, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.controller_line_set_event(type, beat_key, position, event)
            this._queue_line_ctl_cell_change(type, beat_key)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        this.lock_ui_partial {
                if (!this.percussion_channel.visible && this.is_percussion(beat_key.channel)) {
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

    override fun percussion_set_event(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.percussion_set_event(beat_key, position)
            if (!this.percussion_channel.visible) {
                this.make_percussion_visible()
            }

            this._queue_cell_change(beat_key)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun unset_beat(beat: Int) {
        super.unset_beat(beat)
    }

    override fun percussion_set_instrument(line_offset: Int, instrument: Int) {
        this.lock_ui_partial {
            super.percussion_set_instrument(line_offset, instrument)
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

    override fun overwrite_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        super.overwrite_beat_range(beat_key, first_corner, second_corner)
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_ui_partial {
            super.split_tree(beat_key, position, splits, move_event_to_end)
            if ((this.percussion_channel.visible || !this.is_percussion(beat_key.channel))) {
                this._queue_cell_change(beat_key)
            }
        }
    }

    override fun controller_global_split_tree(type: ControlEventType, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_ui_partial {
            super.controller_global_split_tree(type, beat, position, splits, move_event_to_end)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun controller_channel_split_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_ui_partial {
            super.controller_channel_split_tree(type, channel, beat, position, splits, move_event_to_end)

            if (this.percussion_channel.visible || !this.is_percussion(channel)) {
                this._queue_channel_ctl_cell_change(type, channel, beat)
            }
        }
    }

    override fun controller_line_split_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_ui_partial {
            super.controller_line_split_tree(type, beat_key, position, splits, move_event_to_end)

            if (this.percussion_channel.visible || !this.is_percussion(beat_key.channel)) {
                this._queue_line_ctl_cell_change(type, beat_key)
            }
        }
    }

    override fun remove_one_of_two(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.remove_one_of_two(beat_key, position)
            this._queue_cell_change(beat_key)
        }
    }

    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.remove_standard(beat_key, position)
            this._queue_cell_change(beat_key)
        }
    }

    override fun controller_global_remove_one_of_two(type: ControlEventType, beat: Int, position: List<Int>) {
        super.controller_global_remove_one_of_two(type, beat, position)
    }

    override fun controller_channel_remove_one_of_two(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_remove_one_of_two(type, channel, beat, position)
    }

    override fun controller_line_remove_one_of_two(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_remove_one_of_two(type, beat_key, position)
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.insert_after(beat_key, position)
            this._queue_cell_change(beat_key)
        }
    }

    override fun new_line_repeat(channel: Int, line_offset: Int, count: Int) {
        super.new_line_repeat(channel, line_offset, count)
    }

    override fun insert_after_repeat(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        super.insert_after_repeat(beat_key, position, repeat)
    }

    override fun insert_repeat(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        super.insert_repeat(beat_key, position, repeat)
    }

    override fun controller_global_insert_after(type: ControlEventType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_global_insert_after(type, beat, position)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun controller_channel_insert_after(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_channel_insert_after(type, channel, beat, position)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun controller_line_insert_after(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_line_insert_after(type, beat_key, position)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun remove_repeat(beat_key: BeatKey, position: List<Int>, count: Int) {
        this.lock_ui_partial {
            super.remove_repeat(beat_key, position, count)
        }
    }

    override fun repeat_controller_channel_remove(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, repeat: Int) {
        super.repeat_controller_channel_remove(type, channel, beat, position, repeat)
    }

    override fun repeat_controller_line_remove(type: ControlEventType, beat_key: BeatKey, position: List<Int>, count: Int) {
        super.repeat_controller_line_remove(type, beat_key, position, count)
    }

    override fun repeat_controller_global_remove(type: ControlEventType, beat: Int, position: List<Int>, count: Int) {
        super.repeat_controller_global_remove(type, beat, position, count)
    }

    override fun controller_line_remove(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_remove(type, beat_key, position)
    }

    override fun push_to_history_stack(token: HistoryToken, args: List<Any>) {
        super.push_to_history_stack(token, args)
    }

    override fun apply_history_node(current_node: HistoryCache.HistoryNode, depth: Int) {
        super.apply_history_node(current_node, depth)
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return super.toString()
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.insert(beat_key, position)
            this._queue_cell_change(beat_key)
        }
    }

    override fun controller_global_insert(type: ControlEventType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_global_insert(type, beat, position)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun controller_channel_insert(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_channel_insert(type, channel, beat, position)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun controller_line_insert(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_line_insert(type, beat_key, position)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun controller_channel_remove(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_remove(type, channel, beat, position)
    }

    override fun controller_global_remove(type: ControlEventType, beat: Int, position: List<Int>) {
        super.controller_global_remove(type, beat, position)
    }

    override fun overwrite_line(channel: Int, line_offset: Int, beat_key: BeatKey) {
        super.overwrite_line(channel, line_offset, beat_key)
    }

    override fun controller_line_overwrite_line(type: ControlEventType, channel: Int, line_offset: Int, beat_key: BeatKey) {
        super.controller_line_overwrite_line(type, channel, line_offset, beat_key)
    }

    override fun controller_channel_overwrite_line(type: ControlEventType, target_channel: Int, original_channel: Int, original_beat: Int) {
        super.controller_channel_overwrite_line(type, target_channel, original_channel, original_beat)
    }

    override fun controller_global_overwrite_line(type: ControlEventType, beat: Int) {
        super.controller_global_overwrite_line(type, beat)
    }

    override fun overwrite_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {

        super.overwrite_beat_range_horizontally(channel, line_offset, first_key, second_key)
    }

    override fun controller_global_overwrite_range_horizontally(type: ControlEventType, first_beat: Int, second_beat: Int) {
        super.controller_global_overwrite_range_horizontally(type, first_beat, second_beat)
    }

    override fun controller_line_overwrite_range_horizontally(type: ControlEventType, channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {
        super.controller_line_overwrite_range_horizontally(type, channel, line_offset, first_key, second_key)
    }

    override fun controller_channel_overwrite_range_horizontally(type: ControlEventType, target_channel: Int, from_channel: Int, first_beat: Int, second_beat: Int) {
        super.controller_channel_overwrite_range_horizontally(type, target_channel, from_channel, first_beat, second_beat)
    }

    override fun controller_global_remove_standard(type: ControlEventType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_global_remove_standard(type, beat, position)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun controller_channel_remove_standard(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_channel_remove_standard(type, channel, beat, position)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun controller_line_remove_standard(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_line_remove_standard(type, beat_key, position)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun new_line(channel: Int, line_offset: Int?) {
        this.lock_ui_partial {
            super.new_line(channel, line_offset)

            // set the default instrument to the first available in the soundfont (if applicable)
            if (this.is_percussion(channel)) {
                val activity = this.get_activity()
                if (activity != null) {
                    val percussion_keys = activity.active_percussion_names.keys.sorted()
                    if (percussion_keys.isNotEmpty()) {
                        this.percussion_channel.lines[line_offset ?: this.percussion_channel.size - 1].instrument = percussion_keys.first() - 27
                    }
                }
            }

            this._update_after_new_line(channel, line_offset)
        }
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusLineAbstract<*>) {
        this.lock_ui_partial {
            if (!this.percussion_channel.visible && this.is_percussion(channel)) {
                this.make_percussion_visible()
            }

            super.insert_line(channel, line_offset, line)
            this._update_after_new_line(channel, line_offset)
        }
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
            if (!this.percussion_channel.visible && this.is_percussion(channel)) {
                this.make_percussion_visible()
            }

            val abs_line = this.get_visible_row_from_ctl_line(
                this.get_actual_line_index(
                    this.get_instrument_line_index(channel, line_offset)
                )
            )!!

            val output = super.remove_line(channel, line_offset)

            var row_count = 1
            for ((type, controller) in output.controllers.get_all()) {
                if (controller.visible) {
                    row_count += 1
                }
            }

            this._queue_remove_rows(abs_line, row_count)

            output
        }!! // Only null in blocked action, which can't happend in a remove_line()
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

    /* Used to update the ui after new_channel and set_channel_visibility(n, true) */
    private fun _post_new_channel(channel: Int, lines: Int) {
        if (!this.ui_change_bill.is_full_locked()) {
            val channels = this.get_all_channels()
            val line_list = mutableListOf<OpusLineAbstract<*>>()
            for (i in 0 until lines) {
                line_list.add(channels[channel].lines[i])
            }
            val y = this.get_instrument_line_index(channel, 0)


            if (!this.is_percussion(channel) || this.percussion_channel.visible) {
                var ctl_row = this.get_visible_row_from_ctl_line(
                    this.get_actual_line_index(y)
                )!!

                for (j in 0 until line_list.size) {
                    val line = line_list[j]
                    this._add_line_to_column_width_map(ctl_row++, line)
                    for ((type, controller) in line.controllers.get_all()) {
                        if (controller.visible) {
                            this._add_controller_to_column_width_map(ctl_row++, controller)
                        }
                    }
                }

                val controllers = channels[channel].controllers.get_all()
                for ((type, controller) in controllers) {
                    if (controller.visible) {
                        this._add_controller_to_column_width_map(ctl_row++, controller)
                    }
                }
            }
        }
    }
    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        this.lock_ui_partial {
            val notify_index = channel ?: this.channels.size
            super.new_channel(channel, lines, uuid)
            this.ui_change_bill.queue_add_channel(notify_index)
            this._post_new_channel(notify_index, lines)
        }
    }

    override fun remove_beat(beat_index: Int, count: Int) {
        this.lock_ui_partial {
            this.queue_cursor_update(this.cursor.copy())
            val original_beat_count = this.beat_count
            super.remove_beat(beat_index, count)

            val x = min(beat_index + count - 1, original_beat_count - 1) - (count - 1)
            for (i in 0 until count) {
                this.get_editor_table()?.remove_mapped_column(x)
                this.ui_change_bill.queue_remove_column(x)
            }

            this.queue_cursor_update(this.cursor.copy())
            this.ui_change_bill.queue_refresh_context_menu()
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
                this._new_column_in_column_width_map(beat_index)
            }

            super.insert_beat(beat_index, beats_in_column)
        }
    }

    override fun insert_beats(beat_index: Int, count: Int) {
        // TODO: is it necessary to queue ui changes in this function?
        // Not sure but i think the new ui logic will be fine to individually call each insert_beat
        super.insert_beats(beat_index, count)
    }

    private fun _pre_remove_channel(channel: Int): Triple<Int, Int, List<Int>> {
        val y = try {
            this.get_instrument_line_index(channel, 0)
        } catch (e: IndexOutOfBoundsException) {
            this.get_total_line_count()
        }

        val ctl_row = this.get_visible_row_from_ctl_line(this.get_actual_line_index(y))!!
        val channels = this.get_all_channels()
        var removed_row_count = channels[channel].size

        // NOTE: Accessing this.channels instead of this.get_all_channels since it's not possible to remove percussion channel
        for ((type, controller) in channels[channel].controllers.get_all()) {
            if (controller.visible) {
                removed_row_count += 1
            }
        }

        for (j in 0 until channels[channel].lines.size) {
            val line = channels[channel].lines[j]
            for ((type, controller) in line.controllers.get_all()) {
                if (controller.visible) {
                    removed_row_count += 1
                }
            }
        }


        val changed_columns = this.get_editor_table()?.remove_mapped_lines(ctl_row, removed_row_count) ?: listOf()

        return Triple(ctl_row, removed_row_count, changed_columns)
    }

    override fun remove_channel(channel: Int) {
        this.lock_ui_partial {
            if (!this.ui_change_bill.is_full_locked()) {

                val force_show_percussion = !this.percussion_channel.visible
                        && !this.is_percussion(channel)
                        && this.channels.size == 1

                if (force_show_percussion) {
                    this.make_percussion_visible()
                }

                val (ctl_row, removed_row_count, changed_columns) = this._pre_remove_channel(channel)

                super.remove_channel(channel)


                this.ui_change_bill.queue_remove_channel(channel)
                this.ui_change_bill.queue_row_removal(ctl_row, removed_row_count)
                this.ui_change_bill.queue_column_changes(changed_columns, false)

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

    override fun load(bytes: ByteArray, new_path: String?) {
        super.load(bytes, new_path)
    }

    override fun project_change_wrapper(callback: () -> Unit)  {
        this.lock_ui_full {
            this._ui_clear()
            super.project_change_wrapper(callback)
        }
    }

    // This function is called from the Base Layer within th project_change_wrapper.
    // It's implicitly wrapped in a lock_ui_full call
    override fun _project_change_new() {
        val activity = this.get_activity()!!
        this.percussion_channel.visible = true

        super._project_change_new()

        // set the default instrument to the first available in the soundfont (if applicable)
        val percussion_keys = activity.active_percussion_names.keys.sorted()
        if (percussion_keys.isNotEmpty()) {
            this.percussion_set_instrument(0, percussion_keys.first() - 27)
        }

        val new_path = activity.get_new_project_path()
        this.path = new_path
    }

    override fun merge_leafs(beat_key_from: BeatKey, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        super.merge_leafs(beat_key_from, position_from, beat_key_to, position_to)
    }

    override fun set_beat_count(new_count: Int) {
        super.set_beat_count(new_count)
    }

    // This function is called from the Base Layer within th project_change_wrapper.
    // It's implicitly wrapped in a lock_ui_full call
    override fun _project_change_midi(midi: Midi) {
        super._project_change_midi(midi)
        this.on_project_changed()
        this.percussion_channel.visible = this.has_percussion()
    }

    override fun save(path: String?) {
        this.lock_ui_partial {
            super.save(path)
            this.ui_change_bill.queue_enable_delete_and_copy_buttons()
        }
    }

    override fun to_json(): JSONHashMap {
        return super.to_json()
    }

    override fun _project_change_json(json_data: JSONHashMap) {
        super._project_change_json(json_data)
    }

    override fun <T: OpusControlEvent> controller_global_set_initial_event(type: ControlEventType, event: T) {
        this.lock_ui_partial {
            super.controller_global_set_initial_event(type, event)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: OpusControlEvent> controller_channel_set_initial_event(type: ControlEventType, channel: Int, event: T) {
        this.lock_ui_partial {
            super.controller_channel_set_initial_event(type, channel, event)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: OpusControlEvent> controller_line_set_initial_event(type: ControlEventType, channel: Int, line_offset: Int, event: T) {
        this.lock_ui_partial {
            super.controller_line_set_initial_event(type, channel, line_offset, event)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T : OpusLayerBase> import_from_other(other: T) {
        super.import_from_other(other)
    }

    //override fun toggle_channel_controller_visibility(type: ControlEventType, channel_index: Int) {
    //    this.lock_ui_partial {
    //        super.toggle_channel_controller_visibility(type, channel_index)
    //        this._controller_visibility_toggle_callback()
    //    }
    //}

    override fun recache_line_maps() {
        super.recache_line_maps()
        this.set_overlap_callbacks()
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

    override fun clear() {
        super.clear()

        val editor_table = this.get_editor_table()
        editor_table?.clear()
        this.runOnUiThread {
            editor_table?.precise_scroll(0, 0, 0, 0)
        }
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this.lock_ui_partial {
            super.set_duration(beat_key, position, duration)

            // Needs to be set to trigger potentially queued cell changes from on_overlap()
            this._queue_cell_change(beat_key)
            // val btnDuration: TextView = main.findViewById(R.id.btnDuration) ?: return@runOnUiThread
            // btnDuration.text = main.getString(R.string.label_duration, duration)
            this.ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun channel_set_instrument(channel: Int, instrument: Pair<Int, Int>) {
        this.lock_ui_partial {
            super.channel_set_instrument(channel, instrument)
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
                this.ui_change_bill.queue_refresh_context_menu()
            }
        }
    }

   // override fun toggle_global_control_visibility(type: ControlEventType) {
   //     this.lock_ui_partial {
   //         super.toggle_global_control_visibility(type)
   //         this._controller_visibility_toggle_callback()
   //     }
   // }

   // override fun toggle_line_controller_visibility(type: ControlEventType, channel_index: Int, line_offset: Int) {
   //     this.lock_ui_partial {
   //         super.toggle_line_controller_visibility(type, channel_index, line_offset)
   //         this._controller_visibility_toggle_callback()
   //     }
   // }

    override fun on_action_blocked(blocker_key: BeatKey, blocker_position: List<Int>) {
        //super.on_action_blocked(blocker_key, blocker_position)
        if (!this.project_changing) {
            this.set_temporary_blocker(blocker_key, blocker_position)
        }
    }

    override fun on_action_blocked_line_ctl(type: ControlEventType, blocker_key: BeatKey, blocker_position: List<Int>) {
        super.on_action_blocked_line_ctl(type, blocker_key, blocker_position)
        if (!this.project_changing) {
            this.set_temporary_blocker_line_ctl(type, blocker_key, blocker_position)
        }
    }

    override fun on_action_blocked_channel_ctl(type: ControlEventType, blocker_channel: Int, blocker_beat: Int, blocker_position: List<Int>) {
        super.on_action_blocked_channel_ctl(type, blocker_channel, blocker_beat, blocker_position)
        if (!this.project_changing) {
            this.set_temporary_blocker_channel_ctl(type, blocker_channel, blocker_beat, blocker_position)
        }
    }

    override fun on_action_blocked_global_ctl(type: ControlEventType, blocker_beat: Int, blocker_position: List<Int>) {
        super.on_action_blocked_global_ctl(type, blocker_beat, blocker_position)
        if (!this.project_changing) {
            this.set_temporary_blocker_global_ctl(type, blocker_beat, blocker_position)
        }
    }

    // BASE FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


    // HISTORY FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    override fun apply_undo(repeat: Int) {
        //TODO: The recache_line_maps may not be needed, or may be needed outside the lock?
        this.lock_ui_partial {
            super.apply_undo(repeat)
            this.recache_line_maps()
        }
    }

    override fun remove_line_repeat(channel: Int, line_offset: Int, count: Int) {
        super.remove_line_repeat(channel, line_offset, count)
    }

    override fun set_tuning_map_and_transpose(tuning_map: Array<Pair<Int, Int>>, transpose: Int) {
        super.set_tuning_map_and_transpose(tuning_map, transpose)
    }

    // HISTORY FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    // CURSOR FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
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

                OpusManagerCursor.CursorMode.Channel -> {
                    this.ui_change_bill.queue_set_context_menu_channel()
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
            this.unset_temporary_blocker()
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

            if (this.is_percussion(channel) && !this.is_channel_visible(channel)) {
                this.make_percussion_visible()
            }

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_set_context_menu_line()
        }
    }

    override fun cursor_select_channel(channel: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_select_channel(channel)
            this.temporary_blocker = null

            if (this.is_percussion(channel) && !this.is_channel_visible(channel)) {
                this.make_percussion_visible()
            }

            this.queue_cursor_update(this.cursor)
            this.ui_change_bill.queue_set_context_menu_channel()
        }
    }

    override fun cursor_select_channel_ctl_line(ctl_type: ControlEventType, channel: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_select_channel_ctl_line(ctl_type, channel)
            this.temporary_blocker = null

            if (!this.percussion_channel.visible && this.is_percussion(channel)) {
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

            if (!this.percussion_channel.visible && this.is_percussion(channel)) {
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
            this.unset_temporary_blocker()
            if (!this.percussion_channel.visible && this.is_percussion(beat_key.channel)) {
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
            if (!this.percussion_channel.visible && this.is_percussion(beat_key.channel)) {
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
            if (!this.percussion_channel.visible && this.is_percussion(channel)) {
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

    override fun cursor_select_channel_ctl_range(type: ControlEventType, channel: Int, first: Int, second: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this.unset_temporary_blocker()
            super.cursor_select_channel_ctl_range(type, channel, first, second)
            this.queue_cursor_update(this.cursor.copy(), false)
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

            this.queue_cursor_update(this.cursor.copy(), false)
            this.ui_change_bill.queue_set_context_menu_line_control_leaf_b()
        }
    }

    override fun remove_at_cursor(count: Int) {
        super.remove_at_cursor(count)
    }

    override fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this.unset_temporary_blocker()
            super.cursor_select_range(beat_key_a, beat_key_b)

            this.queue_cursor_update(this.cursor.copy(), false)
            this.ui_change_bill.queue_set_context_menu_range()
        }
    }

    // CURSOR FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //------------------------------------------------------------------------
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
                    controller.overlap_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
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

    private fun make_percussion_visible() {
        // Don't force visibility if undo is being called
        if (this.history_cache.isLocked()) {
            return
        }
        this.set_channel_visibility(this.channels.size, true)
    }

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
                            val event_head = line.get_blocking_position(beat_key.beat, cursor.get_position()) ?: Pair(beat_key.beat, cursor.get_position())
                            for ((shadow_beat, _) in line.get_all_blocked_positions(event_head.first, event_head.second)) {
                                shadow_beats.add(shadow_beat)
                            }

                            for (shadow_beat in shadow_beats) {
                                coordinates_to_update.add(EditorTable.Coordinate(y, shadow_beat))
                            }

                            this.ui_change_bill.queue_line_label_refresh(y)
                        }
                        this.ui_change_bill.queue_column_label_refresh(beat_key.beat)
                    }

                    else -> {
                        val (y, controller) = when (cursor.ctl_level!!) {
                            CtlLineLevel.Line -> {
                                // Update Standard Line label attached to controller
                                val line_y = this.get_visible_row_from_ctl_line(
                                    this.get_actual_line_index(
                                        this.get_instrument_line_index(
                                            cursor.channel,
                                            cursor.line_offset
                                        )
                                    )
                                )
                                if (line_y != null) {
                                    this.ui_change_bill.queue_line_label_refresh(line_y)
                                }

                                val channel = this.get_all_channels()[cursor.channel]
                                try {
                                    Pair(
                                        this.get_visible_row_from_ctl_line_line(
                                            cursor.ctl_type!!,
                                            cursor.channel,
                                            cursor.line_offset
                                        ),
                                        channel.lines[cursor.line_offset].get_controller<OpusControlEvent>(cursor.ctl_type!!)
                                    )
                                } catch (e: NullPointerException) {
                                    return
                                }
                            }

                            CtlLineLevel.Channel -> {
                                val channel = this.get_all_channels()[cursor.channel]
                                // Update All Standard Line labels attached to controller
                                for (line_offset in channel.lines.indices) {
                                    var line_y = this.get_visible_row_from_ctl_line(
                                        this.get_actual_line_index(
                                            this.get_instrument_line_index(
                                                cursor.channel,
                                                line_offset
                                            )
                                        )
                                    ) ?: continue
                                    this.ui_change_bill.queue_line_label_refresh(line_y++)
                                    for ((_, controller) in channel.lines[line_offset].controllers.get_all()) {
                                        if (!controller.visible) {
                                            continue
                                        }
                                        this.ui_change_bill.queue_line_label_refresh(line_y++)
                                    }
                                }

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
                        val event_head = controller.get_blocking_position(beat, cursor.get_position()) ?: Pair(beat, cursor.get_position())
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
                        // Update Standard Line label attached to controller
                        var line_y = this.get_visible_row_from_ctl_line(
                            this.get_actual_line_index(
                                this.get_instrument_line_index(
                                    cursor.channel,
                                    cursor.line_offset
                                )
                            )
                        )


                        val row_index = try {
                            this.get_visible_row_from_ctl_line_line(
                                cursor.ctl_type!!,
                                cursor.channel,
                                cursor.line_offset
                            )
                        } catch (e: NullPointerException) {
                            return
                        }

                        if (line_y != null) {
                            this.ui_change_bill.queue_line_label_refresh(line_y)
                        }

                        row_index

                    }
                    CtlLineLevel.Channel -> {
                        val channel = this.get_all_channels()[cursor.channel]

                        // Update All Standard Line labels attached to controller
                        var line_y = this.get_visible_row_from_ctl_line(
                            this.get_actual_line_index(
                                this.get_instrument_line_index(
                                    cursor.channel,
                                    0
                                )
                            )
                        ) ?: return

                        for (line_offset in channel.lines.indices) {
                            this.ui_change_bill.queue_line_label_refresh(line_y++)
                            for ((_, controller) in channel.lines[line_offset].controllers.get_all()) {
                                if (!controller.visible) {
                                    continue
                                }
                                this.ui_change_bill.queue_line_label_refresh(line_y++)
                            }
                        }
                        for ((_, controller) in channel.controllers.get_all()) {
                            if (!controller.visible) {
                                continue
                            }
                            this.ui_change_bill.queue_line_label_refresh(line_y++)
                        }

                        try {
                            this.get_visible_row_from_ctl_line_channel(
                                cursor.ctl_type!!,
                                cursor.channel
                            )
                        } catch (e: NullPointerException) {
                            return
                        }
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
            OpusManagerCursor.CursorMode.Channel -> {
                val y = when (cursor.ctl_level) {
                    null -> {
                        try {
                            this.get_visible_row_from_ctl_line(
                                this.get_actual_line_index(
                                    this.get_instrument_line_index(
                                        cursor.channel,
                                        0
                                    )
                                )
                            ) ?: return
                        } catch (e: IndexOutOfBoundsException) {
                            return
                        }
                    }
                    else -> return // TODO: Throw Exception?
                }

                val channels = this.get_all_channels()
                var x = 0
                for (line in channels[cursor.channel].lines) {
                    this.ui_change_bill.queue_row_change(y + x++, true)
                    for (j in 0 until line.controllers.get_all().size) {
                        this.ui_change_bill.queue_row_change(y + x++, true)
                    }
                }
                for (j in 0 until channels[cursor.channel].controllers.get_all().size) {
                    this.ui_change_bill.queue_row_change(y + x++, true)
                }
            }
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
                        if (!controller.visible) {
                            continue
                        }
                        val ctl_tree = controller.get_tree(beat)
                        column.add(ctl_tree.get_total_child_weight())
                    }
                }

                for ((type, controller) in channel.controllers.get_all()) {
                    if (!controller.visible) {
                        continue
                    }
                    val ctl_tree = controller.get_tree(beat)
                    column.add(ctl_tree.get_total_child_weight())
                }
            }

            for ((type, controller) in this.controllers.get_all()) {
                if (!controller.visible) {
                    continue
                }

                val ctl_tree = controller.get_tree(beat)
                column.add(ctl_tree.get_total_child_weight())
            }
            editor_table.add_column_to_map(beat, column)
        }
        println("${editor_table._inv_column_map}")
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
        val working_channel = this.get_channel(channel)
        val adj_line_offset = line_offset ?: (working_channel.lines.size - 1)


        if (this.ui_change_bill.is_full_locked() || this.get_activity() == null) {
            return
        }


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
            if (controller.visible) {
                this._add_controller_to_column_width_map(visible_row + i, controller)
            }
        }
    }

    private fun _new_column_in_column_width_map(index: Int) {
        if (this.ui_change_bill.is_full_locked()) {
            return
        }

        val column = mutableListOf<Int>()
        this.get_visible_channels().forEachIndexed { i: Int, channel: OpusChannelAbstract<*, *> ->
            channel.lines.forEachIndexed { j: Int, line: OpusLineAbstract<*> ->
                column.add(1)
                for ((type, controller) in channel.lines[j].controllers.get_all()) {
                    if (!controller.visible) {
                        continue
                    }
                    column.add(1)
                }
            }

            for ((type, controller) in channel.controllers.get_all()) {
                if (!controller.visible) {
                    continue
                }
                column.add(1)
            }
        }
        for ((type, controller) in this.controllers.get_all()) {
            if (!controller.visible) {
                continue
            }
            column.add(1)
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
                            it.set_context_menu_line_control_leaf()
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
                            it.set_context_menu_control_line()
                        }
                    }

                    BillableItem.ContextMenuSetChannel -> {
                        this.withFragment {
                            it.set_context_menu_channel()
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
            editor_table.get_column_recycler().get_first_column_test()
        }
    }

    private fun runOnUiThread(callback: (MainActivity) -> Unit) {
        val main = this._activity ?: return // TODO: Throw Exception?
        val runnable = Runnable {
            callback(main)
        }
        synchronized(runnable) {
            main.runOnUiThread(runnable)
        }
    }

    override fun _apply_column_trees(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>) {
        super._apply_column_trees(beat_index, beats_in_column)
    }

    override fun new_channel_controller(type: ControlEventType, channel_index: Int) {
        super.new_channel_controller(type, channel_index)
    }

    override fun new_line_controller(type: ControlEventType, channel_index: Int, line_offset: Int) {
        super.new_line_controller(type, channel_index, line_offset)
    }

    override fun new_global_controller(type: ControlEventType) {
        super.new_global_controller(type)
    }
    // END UI FUNCS -----------------------

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

    // Note: set_note_octave/offset functions need to be in interface layer since they require access to 'relative_mode' property
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
}
