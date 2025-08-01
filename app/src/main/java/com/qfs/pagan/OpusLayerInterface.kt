package com.qfs.pagan
import android.content.res.Configuration
import android.view.View
import android.widget.TextView
import com.qfs.apres.Midi
import com.qfs.json.JSONHashMap
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.DrawerChannelMenu.ChannelOptionAdapter
import com.qfs.pagan.DrawerChannelMenu.ChannelOptionRecycler
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.BlockedActionException
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.InstrumentEvent
import com.qfs.pagan.structure.opusmanager.base.NoteOutOfRange
import com.qfs.pagan.structure.opusmanager.base.OpusChannelAbstract
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.OpusLineAbstract
import com.qfs.pagan.structure.opusmanager.base.OpusPercussionChannel
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.TunedInstrumentEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.opusmanager.cursor.IncorrectCursorMode
import com.qfs.pagan.structure.opusmanager.cursor.OpusManagerCursor
import com.qfs.pagan.structure.opusmanager.history.OpusLayerHistory
import com.qfs.pagan.structure.rationaltree.InvalidGetCall
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import com.qfs.pagan.uibill.BillableItem
import com.qfs.pagan.uibill.UIChangeBill
import kotlin.Pair
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class OpusLayerInterface : OpusLayerHistory() {
    class HidingNonEmptyPercussionException: Exception()
    class HidingLastChannelException: Exception()
    class MissingEditorTableException: Exception()

    companion object {
        @Suppress("unused")
        val global_controller_domain = listOf(
            Pair(EffectType.Tempo, R.drawable.tempo)
        )

        val channel_controller_domain = listOf(
            Pair(EffectType.Volume, R.drawable.volume),
            Pair(EffectType.Pan, R.drawable.pan_icon)
        )

        val line_controller_domain = listOf(
            Pair(EffectType.Volume, R.drawable.volume),
            Pair(EffectType.Velocity, R.drawable.velocity),
            Pair(EffectType.Pan, R.drawable.pan_icon)
        )
    }

    var initialized = false
    var relative_mode: RelativeInputMode = RelativeInputMode.Absolute
    private var _activity: ActivityEditor? = null

    private var _cache_cursor: OpusManagerCursor = OpusManagerCursor(CursorMode.Unset)

    var marked_range: Pair<BeatKey, BeatKey>? = null

    private val _ui_change_bill = UIChangeBill()
    var temporary_blocker: OpusManagerCursor? = null

    private var _in_reload = false

    fun attach_activity(activity: ActivityEditor) {
        this._activity = activity
    }

    fun get_activity(): ActivityEditor? {
        return this._activity
    }

    private fun get_editor_table(): EditorTable {
        return this._activity?.findViewById(R.id.etEditorTable) ?: throw MissingEditorTableException()
    }

    // UI BILL Interface functions vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    private fun <T> lock_ui_full(callback: () -> T): T? {
        this._ui_change_bill.lock_full()

        val output = try {
            val tmp = callback()
            this._ui_change_bill.unlock()
            tmp
        } catch (e: BlockedActionException) {
            this._ui_change_bill.unlock()
            this._ui_change_bill.cancel_most_recent()
            if (!this._ui_change_bill.is_locked()) {
                if (this.temporary_blocker != null) {
                    this.cursor_apply(this.temporary_blocker!!)
                }
                null
            } else { // Still Locked
                throw e
            }
        } catch (e: Exception) {
            this._ui_change_bill.unlock()
            this._ui_change_bill.cancel_most_recent()
            throw e
        }

        if (!this._ui_change_bill.is_locked()) {
            this._apply_bill_changes()
        }

        return output
    }

    private fun <T> lock_ui_partial(callback: () -> T): T? {
        this._ui_change_bill.lock_partial()
        val output = try {
            val tmp = callback()
            this._ui_change_bill.unlock()
            tmp
        } catch (e: BlockedActionException) {
            this._ui_change_bill.unlock()
            this._ui_change_bill.cancel_most_recent()
            if (!this._ui_change_bill.is_locked()) {
                if (this.temporary_blocker != null) {
                    this.cursor_apply(this.temporary_blocker!!)
                }
                null
            } else { // Still Locked
                throw e
            }
        } catch (e: Exception) {
            this._ui_change_bill.unlock()
            this._ui_change_bill.cancel_most_recent()
            throw e
        }

        if (!this._ui_change_bill.is_locked()) {
            this._apply_bill_changes()
        }

        return output
    }

    /* Notify the editor table to update cells */
    private fun _queue_cell_changes(beat_keys: List<BeatKey>) {
        if (this._ui_change_bill.is_full_locked()) {
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
                } catch (_: IndexOutOfBoundsException) { // may reference a channel's line before the channel exists
                    this.get_row_count()
                },
                beat_keys[i].beat
            )
        }

        this._ui_change_bill.queue_cell_changes(coord_list)
    }

    /* Notify the editor table to update a cell */
    private fun _queue_cell_change(beat_key: BeatKey) {
        if (this.project_changing) {
            return
        }
        if (this._ui_change_bill.is_full_locked()) {
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
            this._ui_change_bill.queue_column_change(coord.x)
        } else {
            this._ui_change_bill.queue_cell_change(coord)
        }
    }

    private fun _queue_global_ctl_cell_change(type: EffectType, beat: Int) {
        val controller = this.get_controller<EffectEvent>(type)
        if (!controller.visible) {
            return
        }

        if (this._ui_change_bill.is_full_locked()) {
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
            this._ui_change_bill.queue_column_change(coord.x)
        } else {
            this._ui_change_bill.queue_cell_change(coord)
        }
    }

    private fun _queue_channel_ctl_cell_change(type: EffectType, channel: Int, beat: Int) {
        val controller = this.get_all_channels()[channel].get_controller<EffectEvent>(type)
        if (!controller.visible) {
            return
        }
        if (this._ui_change_bill.is_full_locked()) {
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
            this._ui_change_bill.queue_column_change(coord.x)
        } else {
            this._ui_change_bill.queue_cell_change(coord)
        }
    }

    private fun _queue_line_ctl_cell_change(type: EffectType, beat_key: BeatKey) {
        val controller = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].get_controller<EffectEvent>(type)
        if (!controller.visible) {
           return
        }

        val coord = EditorTable.Coordinate(
            y = this.get_visible_row_from_ctl_line_line(type, beat_key.channel, beat_key.line_offset),
            x = beat_key.beat
        )

        val tree = this.get_line_ctl_tree<EffectEvent>(type, beat_key)
        val new_weight = tree.get_total_child_weight()

        val editor_table = this.get_editor_table()
        if (editor_table.set_mapped_width(coord.y, coord.x, new_weight)) {
            this._ui_change_bill.queue_column_change(coord.x)
        } else {
            this._ui_change_bill.queue_cell_change(coord)
        }
    }
    // UI BILL Interface functions ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    // BASE FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    override fun offset_range(amount: Int, first_key: BeatKey, second_key: BeatKey) {
        this.lock_ui_partial {
            super.offset_range(amount, first_key, second_key)
        }
    }

    override fun remove_global_controller(type: EffectType) {
        this.lock_ui_partial {
            if (this.is_global_ctl_visible(type)) {
                val abs_line = this.get_visible_row_from_ctl_line_global(type)
                this._queue_remove_rows(abs_line, 1)
            }
            super.remove_global_controller(type)
        }
    }

    override fun remove_line_controller(type: EffectType, channel_index: Int, line_offset: Int) {
        this.lock_ui_partial {
            if (this.is_line_ctl_visible(type, channel_index, line_offset )) {
                val abs_line = this.get_visible_row_from_ctl_line_line(type, channel_index, line_offset)
                this._queue_remove_rows(abs_line, 1)
            }
            super.remove_line_controller(type, channel_index, line_offset)
        }
    }

    override fun remove_channel_controller(type: EffectType, channel_index: Int) {
        this.lock_ui_partial {
            if (this.is_channel_ctl_visible(type, channel_index)) {
                val abs_line = this.get_visible_row_from_ctl_line_channel(type, channel_index)
                this._queue_remove_rows(abs_line, 1)
            }
            super.remove_channel_controller(type, channel_index)
        }
    }

    override fun set_line_controller_visibility(type: EffectType, channel_index: Int, line_offset: Int, visibility: Boolean) {
        this.lock_ui_partial {
            if (visibility) {
                super.set_line_controller_visibility(type, channel_index, line_offset, true)
                val visible_row = this.get_visible_row_from_ctl_line_line(type, channel_index, line_offset)
                val working_channel = this.get_channel(channel_index)
                val controller = working_channel.lines[line_offset].get_controller<EffectEvent>(type)
                this._add_controller_to_column_width_map(visible_row, controller)
            } else {
                val visible_row = this.get_visible_row_from_ctl_line_line(type, channel_index, line_offset)
                super.set_line_controller_visibility(type, channel_index, line_offset, false)
                this._queue_remove_rows(visible_row, 1)
            }
        }
    }
    override fun set_channel_controller_visibility(type: EffectType, channel_index: Int, visibility: Boolean) {
        this.lock_ui_partial {
            if (visibility) {
                super.set_channel_controller_visibility(type, channel_index, true)

                val visible_row = this.get_visible_row_from_ctl_line_channel(type, channel_index)
                val working_channel = this.get_channel(channel_index)
                val controller = working_channel.get_controller<EffectEvent>(type)
                this._add_controller_to_column_width_map(visible_row, controller)
            } else {
                val visible_row = this.get_visible_row_from_ctl_line_channel(type, channel_index)
                super.set_channel_controller_visibility(type, channel_index, false)
                this._queue_remove_rows(visible_row, 1)
            }
        }
    }

    override fun set_global_controller_visibility(type: EffectType, visibility: Boolean) {
        this.lock_ui_partial {
            if (visibility) {
                super.set_global_controller_visibility(type, true)
                val visible_row = this.get_visible_row_from_ctl_line_global(type)
                val controller = this.get_controller<EffectEvent>(type)
                this._add_controller_to_column_width_map(visible_row, controller)
            } else {
                val visible_row = this.get_visible_row_from_ctl_line_global(type)
                super.set_global_controller_visibility(type, false)
                this._queue_remove_rows(visible_row, 1)
            }
        }
    }

    override fun set_project_name(new_name: String?) {
        this.lock_ui_partial {
            super.set_project_name(new_name)
            if (!this._ui_change_bill.is_full_locked()) {
                this._ui_change_bill.queue_project_name_change()
            }
        }
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.unset(beat_key, position)

            this._queue_cell_change(beat_key)
        }
    }

    override fun controller_global_unset(type: EffectType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_global_unset(type, beat, position)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun controller_channel_unset(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_channel_unset(type, channel, beat, position)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun controller_line_unset(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_line_unset(type, beat_key, position)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: ReducibleTree<out InstrumentEvent>) {
        this.lock_ui_partial {
            super.replace_tree(beat_key, position, tree)
            this._queue_cell_change(beat_key)
        }
    }

    override fun _controller_line_copy_range(type: EffectType, beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey, unset_original: Boolean) {
        this.lock_ui_partial {
            super._controller_line_copy_range(type, beat_key, first_corner, second_corner, unset_original)
        }
    }

    override fun _controller_line_to_channel_copy_range(type: EffectType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_channel: Int, target_beat: Int, unset_original: Boolean) {
        this.lock_ui_partial {
            super._controller_line_to_channel_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_channel, target_beat, unset_original)
        }
    }

    override fun _controller_line_to_global_copy_range(type: EffectType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_beat: Int, unset_original: Boolean) {
        this.lock_ui_partial {
            super._controller_line_to_global_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_beat, unset_original)
        }
    }

    override fun _controller_channel_copy_range(type: EffectType, target_channel: Int, target_beat: Int, original_channel: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        this.lock_ui_partial {
            super._controller_channel_copy_range(type, target_channel, target_beat, original_channel, point_a, point_b, unset_original)
        }
    }

    override fun _controller_channel_to_line_copy_range(type: EffectType, channel_from: Int, beat_a: Int, beat_b: Int, target_key: BeatKey, unset_original: Boolean) {
        this.lock_ui_partial {
            super._controller_channel_to_line_copy_range(type, channel_from, beat_a, beat_b, target_key, unset_original)
        }
    }

    override fun _controller_channel_to_global_copy_range(type: EffectType, target_beat: Int, original_channel: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        this.lock_ui_partial {
            super._controller_channel_to_global_copy_range(type, target_beat, original_channel, point_a, point_b, unset_original)
        }
    }

    override fun _controller_global_copy_range(type: EffectType, target: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        this.lock_ui_partial {
            super._controller_global_copy_range(type, target, point_a, point_b, unset_original)
        }
    }

    override fun _controller_global_to_line_copy_range(type: EffectType, beat_a: Int, beat_b: Int, target_key: BeatKey, unset_original: Boolean) {
        this.lock_ui_partial {
            super._controller_global_to_line_copy_range(type, beat_a, beat_b, target_key, unset_original)
        }
    }

    override fun _controller_global_to_channel_copy_range(type: EffectType, target_channel: Int, target_beat: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        this.lock_ui_partial {
            super._controller_global_to_channel_copy_range(type, target_channel, target_beat, point_a, point_b, unset_original)
        }
    }

    override fun controller_channel_overwrite_range_horizontally(type: EffectType, target_channel: Int, from_channel: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_channel_overwrite_range_horizontally(type, target_channel, from_channel, first_beat, second_beat, repeat)
        }
    }

    override fun controller_channel_to_global_overwrite_range_horizontally(type: EffectType, channel: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_channel_to_global_overwrite_range_horizontally(type, channel, first_beat, second_beat, repeat)
        }
    }

    override fun controller_channel_to_line_overwrite_range_horizontally(type: EffectType, target_channel: Int, target_line_offset: Int, from_channel: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_channel_to_line_overwrite_range_horizontally(type, target_channel, target_line_offset, from_channel, first_beat, second_beat, repeat)
        }
    }

    override fun overwrite_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey, repeat: Int?) {
        this.lock_ui_partial {
            super.overwrite_beat_range_horizontally(channel, line_offset, first_key, second_key, repeat)
        }
    }

    override fun controller_global_to_channel_overwrite_range_horizontally(type: EffectType, channel: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_global_to_channel_overwrite_range_horizontally(type, channel, first_beat, second_beat, repeat)
        }
    }

    override fun controller_line_to_channel_overwrite_range_horizontally(type: EffectType, channel: Int, first_key: BeatKey, second_key: BeatKey, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_line_to_channel_overwrite_range_horizontally(type, channel, first_key, second_key, repeat)
        }
    }

    override fun controller_global_to_line_overwrite_range_horizontally(type: EffectType, target_channel: Int, target_line_offset: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_global_to_line_overwrite_range_horizontally(type, target_channel, target_line_offset, first_beat, second_beat, repeat)
        }
    }

    override fun controller_global_overwrite_range_horizontally(type: EffectType, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_global_overwrite_range_horizontally(type, first_beat, second_beat, repeat)
        }
    }

    override fun controller_line_to_global_overwrite_range_horizontally(type: EffectType, channel: Int, line_offset: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_line_to_global_overwrite_range_horizontally(type, channel, line_offset, first_beat, second_beat, repeat)
        }
    }

    override fun controller_line_overwrite_range_horizontally(type: EffectType, channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_line_overwrite_range_horizontally(type, channel, line_offset, first_key, second_key, repeat)
        }
    }

    override fun controller_global_overwrite_line(type: EffectType, beat: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_global_overwrite_line(type, beat, repeat)
        }
    }

    override fun overwrite_line(channel: Int, line_offset: Int, beat_key: BeatKey, repeat: Int?) {
        this.lock_ui_partial {
            super.overwrite_line(channel, line_offset, beat_key, repeat)
        }
    }

    override fun overwrite_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        this.lock_ui_partial {
            super.overwrite_beat_range(beat_key, first_corner, second_corner)
        }
    }

    override fun controller_line_overwrite_line(type: EffectType, channel: Int, line_offset: Int, beat_key: BeatKey, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_line_overwrite_line(type, channel, line_offset, beat_key, repeat)
        }
    }

    override fun controller_channel_overwrite_line(type: EffectType, target_channel: Int, original_channel: Int, original_beat: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_channel_overwrite_line(type, target_channel, original_channel, original_beat, repeat)
        }
    }

    override fun controller_line_to_global_overwrite_line(type: EffectType, beat_key: BeatKey, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_line_to_global_overwrite_line(type, beat_key, repeat)
        }
    }

    override fun controller_channel_to_line_overwrite_line(type: EffectType, target_channel: Int, target_line_offset: Int, original_channel: Int, original_beat: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_channel_to_line_overwrite_line(type, target_channel, target_line_offset, original_channel, original_beat, repeat)
        }
    }

    override fun controller_channel_to_global_overwrite_line(type: EffectType, channel: Int, beat: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_channel_to_global_overwrite_line(type, channel, beat, repeat)
        }
    }

    override fun controller_global_to_channel_overwrite_line(type: EffectType, target_channel: Int, beat: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_global_to_channel_overwrite_line(type, target_channel, beat, repeat)
        }
    }

    override fun controller_global_to_line_overwrite_line(type: EffectType, from_beat: Int, target_channel: Int, target_line_offset: Int, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_global_to_line_overwrite_line(type, from_beat, target_channel, target_line_offset, repeat)
        }
    }

    override fun controller_line_to_channel_overwrite_line(type: EffectType, target_channel: Int, original_key: BeatKey, repeat: Int?) {
        this.lock_ui_partial {
            super.controller_line_to_channel_overwrite_line(type, target_channel, original_key, repeat)
        }
    }


    override fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        this.lock_ui_partial {
            super.move_leaf(beatkey_from, position_from, beatkey_to, position_to)
        }
    }

    override fun controller_line_to_global_move_leaf(type: EffectType, beatkey_from: BeatKey, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        this.lock_ui_partial {
            super.controller_line_to_global_move_leaf(type, beatkey_from, position_from, target_beat, target_position)
        }
    }

    override fun controller_line_to_channel_move_leaf(type: EffectType, beatkey_from: BeatKey, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        this.lock_ui_partial {
            super.controller_line_to_channel_move_leaf(type, beatkey_from, position_from, channel_to, beat_to, position_to)
        }
    }

    override fun controller_line_move_leaf(type: EffectType, beatkey_from: BeatKey, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        this.lock_ui_partial {
            super.controller_line_move_leaf(type, beatkey_from, position_from, beat_key_to, position_to)
        }
    }

    override fun controller_channel_move_leaf(type: EffectType, channel_from: Int, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        this.lock_ui_partial {
            super.controller_channel_move_leaf(type, channel_from, beat_from, position_from, channel_to, beat_to, position_to)
        }
    }

    override fun controller_channel_to_line_move_leaf(type: EffectType, channel_from: Int, beat_from: Int, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        this.lock_ui_partial {
            super.controller_channel_to_line_move_leaf(type, channel_from, beat_from, position_from, beat_key_to, position_to)
        }
    }

    override fun controller_channel_to_global_move_leaf(type: EffectType, channel_from: Int, beat_from: Int, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        this.lock_ui_partial {
            super.controller_channel_to_global_move_leaf(type, channel_from, beat_from, position_from, target_beat, target_position)
        }
    }

    override fun controller_global_move_leaf(type: EffectType, beat_from: Int, position_from: List<Int>, beat_to: Int, position_to: List<Int>) {
        this.lock_ui_partial {
            super.controller_global_move_leaf(type, beat_from, position_from, beat_to, position_to)
        }
    }

    override fun controller_global_to_channel_move_leaf(type: EffectType, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        this.lock_ui_partial {
            super.controller_global_to_channel_move_leaf(type, beat_from, position_from, channel_to, beat_to, position_to)
        }
    }

    override fun controller_global_to_line_move_leaf(type: EffectType, beat: Int, position: List<Int>, target_key: BeatKey, target_position: List<Int>) {
        this.lock_ui_partial {
            super.controller_global_to_line_move_leaf(type, beat, position, target_key, target_position)
        }
    }

    override fun <T: EffectEvent> controller_global_replace_tree(type: EffectType, beat: Int, position: List<Int>?, tree: ReducibleTree<T>) {
        this.lock_ui_partial {
            super.controller_global_replace_tree(type, beat, position, tree)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun <T: EffectEvent> controller_channel_replace_tree(type: EffectType, channel: Int, beat: Int, position: List<Int>?, tree: ReducibleTree<T>) {
        this.lock_ui_partial {
            super.controller_channel_replace_tree(type, channel, beat, position, tree)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun <T: EffectEvent> controller_line_replace_tree(type: EffectType, beat_key: BeatKey, position: List<Int>?, tree: ReducibleTree<T>) {
        this.lock_ui_partial {
            super.controller_line_replace_tree(type, beat_key, position, tree)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun <T: EffectEvent> controller_global_set_event(type: EffectType, beat: Int, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.controller_global_set_event(type, beat, position, event)
            this._queue_global_ctl_cell_change(type, beat)
            this._ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: EffectEvent> controller_channel_set_event(type: EffectType, channel: Int, beat: Int, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.controller_channel_set_event(type, channel, beat, position, event)
            this._queue_channel_ctl_cell_change(type, channel, beat)
            this._ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: EffectEvent> controller_line_set_event(type: EffectType, beat_key: BeatKey, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.controller_line_set_event(type, beat_key, position, event)
            this._queue_line_ctl_cell_change(type, beat_key)
            this._ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.set_event(beat_key, position, event)

            if (event is TunedInstrumentEvent) {
                this.set_relative_mode(event)
            }

            if (!this._ui_change_bill.is_full_locked()) {
                this._queue_cell_change(beat_key)
                this._ui_change_bill.queue_refresh_context_menu()
            }
        }
    }

    override fun percussion_set_event(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.percussion_set_event(beat_key, position)
            this._queue_cell_change(beat_key)
            this._ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun percussion_set_instrument(channel: Int, line_offset: Int, instrument: Int) {
        this.lock_ui_partial {
            super.percussion_set_instrument(channel, line_offset, instrument)
            // Need to call get_drum name to repopulate instrument list if needed
            this.get_activity()?.get_drum_name(channel, instrument)

            if (!this._ui_change_bill.is_full_locked()) {
                this._ui_change_bill.queue_refresh_choose_percussion_button(channel, line_offset)
                this._ui_change_bill.queue_line_label_refresh(
                    this.get_visible_row_from_ctl_line(
                        this.get_actual_line_index(
                            this.get_instrument_line_index(channel, line_offset)
                        )
                    )!!
                )
            }
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_ui_partial {
            super.split_tree(beat_key, position, splits, move_event_to_end)
            this._queue_cell_change(beat_key)
        }
    }

    override fun controller_global_split_tree(type: EffectType, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_ui_partial {
            super.controller_global_split_tree(type, beat, position, splits, move_event_to_end)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun controller_channel_split_tree(type: EffectType, channel: Int, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_ui_partial {
            super.controller_channel_split_tree(type, channel, beat, position, splits, move_event_to_end)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun controller_line_split_tree(type: EffectType, beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_ui_partial {
            super.controller_line_split_tree(type, beat_key, position, splits, move_event_to_end)
            this._queue_line_ctl_cell_change(type, beat_key)
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

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.insert_after(beat_key, position)
            this._queue_cell_change(beat_key)
        }
    }

    override fun controller_global_insert_after(type: EffectType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_global_insert_after(type, beat, position)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun controller_channel_insert_after(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_channel_insert_after(type, channel, beat, position)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun controller_line_insert_after(type: EffectType, beat_key: BeatKey, position: List<Int>) {
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

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.insert(beat_key, position)
            this._queue_cell_change(beat_key)
        }
    }

    override fun controller_global_insert(type: EffectType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_global_insert(type, beat, position)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun controller_channel_insert(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_channel_insert(type, channel, beat, position)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun controller_line_insert(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_line_insert(type, beat_key, position)
            this._queue_line_ctl_cell_change(type, beat_key)
        }
    }

    override fun controller_global_remove_standard(type: EffectType, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_global_remove_standard(type, beat, position)
            this._queue_global_ctl_cell_change(type, beat)
        }
    }

    override fun controller_channel_remove_standard(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.lock_ui_partial {
            super.controller_channel_remove_standard(type, channel, beat, position)
            this._queue_channel_ctl_cell_change(type, channel, beat)
        }
    }

    override fun controller_line_remove_standard(type: EffectType, beat_key: BeatKey, position: List<Int>) {
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
                this.get_activity()?.let { activity: ActivityEditor ->
                    activity.populate_active_percussion_names(channel)
                    val percussion_keys = activity.active_percussion_names[channel]?.keys?.sorted() ?: listOf()
                    if (percussion_keys.isNotEmpty()) {
                        (this.get_channel(channel) as OpusPercussionChannel).let {
                            it.lines[line_offset ?: (it.size - 1)].instrument = percussion_keys.first() - 27
                        }
                    }
                }
            }

            this._update_after_new_line(channel, line_offset)
        }
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusLineAbstract<*>) {
        this.lock_ui_partial {
            super.insert_line(channel, line_offset, line)
            this._update_after_new_line(channel, line_offset)
        }
    }

    private fun _swap_line_ui_update(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        var y = 0
        var first_swapped_line = min(
            this.get_instrument_line_index(channel_a, line_a),
            this.get_instrument_line_index(channel_b, line_b)
        )

        for (channel in this.get_all_channels()) {
            for (line in channel.lines) {
                if (y >= first_swapped_line) {
                    this._ui_change_bill.queue_line_label_refresh(y)
                }

                this._ui_change_bill.queue_row_change(y++)

                for ((_, controller) in line.controllers.get_all()) {
                    if (controller.visible) {
                        if (y >= first_swapped_line) {
                            this._ui_change_bill.queue_line_label_refresh(y)
                        }
                        this._ui_change_bill.queue_row_change(y++)
                    }
                }
            }

            for ((_, controller) in channel.controllers.get_all()) {
                if (controller.visible) {
                    if (y >= first_swapped_line) {
                        this._ui_change_bill.queue_line_label_refresh(y)
                    }
                    this._ui_change_bill.queue_row_change(y++)
                }
            }
        }

        for ((_, controller) in this.controllers.get_all()) {
            if (controller.visible) {
                if (y >= first_swapped_line) {
                    this._ui_change_bill.queue_line_label_refresh(y)
                }
                this._ui_change_bill.queue_row_change(y++)
            }
        }
    }

    override fun swap_lines(channel_index_a: Int, line_offset_a: Int, channel_index_b: Int, line_offset_b: Int) {
        this.lock_ui_partial {
            super.swap_lines(channel_index_a, line_offset_a, channel_index_b, line_offset_b)

            val vis_line_a = this.get_visible_row_from_ctl_line(
                this.get_actual_line_index(
                    this.get_instrument_line_index(channel_index_a, line_offset_a)
                )
            )!!

            val vis_line_b = this.get_visible_row_from_ctl_line(
                this.get_actual_line_index(
                    this.get_instrument_line_index(channel_index_b, line_offset_b)
                )
            )!!

            this.get_editor_table().swap_mapped_lines(vis_line_a, vis_line_b)
            this._swap_line_ui_update(channel_index_a, line_offset_a, channel_index_b, line_offset_b)
        }
    }

    override fun swap_channels(channel_a: Int, channel_b: Int) {
        this.lock_ui_partial {
            val vis_line_a = this.get_visible_row_from_ctl_line(
                this.get_actual_line_index(
                    this.get_instrument_line_index(channel_a, 0)
                )
            )!!
            val vis_line_b = this.get_visible_row_from_ctl_line(
                this.get_actual_line_index(
                    this.get_instrument_line_index(channel_b, 0)
                )
            )!!

            super.swap_channels(channel_a, channel_b)

            var channel_a_size = 0
            for (line in this.get_channel(channel_a).lines) {
                channel_a_size += 1
                for ((_, controller) in line.controllers.get_all()) {
                    if (controller.visible) {
                        channel_a_size += 1
                    }
                }
            }
            for ((_, controller) in this.get_channel(channel_a).controllers.get_all()) {
                if (controller.visible) {
                    channel_a_size += 1
                }
            }

            var channel_b_size = 0
            for (line in this.get_channel(channel_b).lines) {
                channel_b_size += 1
                for ((_, controller) in line.controllers.get_all()) {
                    if (controller.visible) {
                        channel_b_size += 1
                    }
                }
            }
            for ((_, controller) in this.get_channel(channel_b).controllers.get_all()) {
                if (controller.visible) {
                    channel_b_size += 1
                }
            }

            this.get_activity()?.swap_percussion_channels(channel_a, channel_b)
            this.get_editor_table().swap_mapped_channels(vis_line_b, channel_a_size, vis_line_a, channel_b_size)
            this._swap_line_ui_update(channel_a, 0, channel_b, 0)
            this._ui_change_bill.queue_refresh_channel(channel_a)
            this._ui_change_bill.queue_refresh_channel(channel_b)
        }
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusLineAbstract<*> {
        return this.lock_ui_partial {
            val abs_line = this.get_visible_row_from_ctl_line(
                this.get_actual_line_index(
                    this.get_instrument_line_index(channel, line_offset)
                )
            )!!

            val output = super.remove_line(channel, line_offset)

            var row_count = 1
            for ((_, controller) in output.controllers.get_all()) {
                if (controller.visible) {
                    row_count += 1
                }
            }

            this._queue_remove_rows(abs_line, row_count)

            output
        }!! // Only null in blocked action, which can't happend in a remove_line()
    }

    private fun _queue_remove_rows(y: Int, count: Int) {
        if (!this._ui_change_bill.is_full_locked()) {
            val column_updates = this.get_editor_table().remove_mapped_lines(y, count)
            this._ui_change_bill.queue_row_removal(y, count)
            for (column in column_updates) {
                this._ui_change_bill.queue_column_change(column)
            }
        }
    }

    /* Used to update the ui after new_channel and set_channel_visibility(n, true) */
    private fun _post_new_channel(channel: Int, lines: Int) {
        if (!this._ui_change_bill.is_full_locked()) {
            val channels = this.get_all_channels()
            val line_list = mutableListOf<OpusLineAbstract<*>>()
            for (i in 0 until lines) {
                line_list.add(channels[channel].lines[i])
            }
            val y = this.get_instrument_line_index(channel, 0)

            var ctl_row = this.get_visible_row_from_ctl_line(
                this.get_actual_line_index(y)
            )!!

            for (j in 0 until line_list.size) {
                val line = line_list[j]
                this._add_line_to_column_width_map(ctl_row++, line)
                for ((_, controller) in line.controllers.get_all()) {
                    if (controller.visible) {
                        this._add_controller_to_column_width_map(ctl_row++, controller)
                    }
                }
            }

            val controllers = channels[channel].controllers.get_all()
            for ((_, controller) in controllers) {
                if (controller.visible) {
                    this._add_controller_to_column_width_map(ctl_row++, controller)
                }
            }
        }
    }
    override fun new_channel(channel: Int?, lines: Int, uuid: Int?, is_percussion: Boolean) {
        this.lock_ui_partial {
            val notify_index = channel ?: this.channels.size
            super.new_channel(channel, lines, uuid, is_percussion)
            this._ui_change_bill.queue_add_channel(notify_index)
            this._post_new_channel(notify_index, lines)
            this.get_activity()?.shift_up_percussion_names(notify_index)
        }
    }

    override fun remove_beat(beat_index: Int, count: Int) {
        this.lock_ui_partial {
            this._queue_cursor_update(this.cursor.copy())
            val original_beat_count = this.length
            super.remove_beat(beat_index, count)

            val x = min(beat_index + count - 1, original_beat_count - 1) - (count - 1)
            for (i in 0 until count) {
                this.get_editor_table().remove_mapped_column(x)
                this._ui_change_bill.queue_remove_column(x)
            }

            this._queue_cursor_update(this.cursor.copy())
            this._ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<ReducibleTree<OpusEvent>>?) {
        this.lock_ui_partial {
            if (!this._ui_change_bill.is_full_locked()) {
                this._queue_cursor_update(this.cursor)
                this._ui_change_bill.queue_add_column(beat_index)

                // Need to find all notes that overflow into the proceding beats and queue a column refresh on those beats
                if (beat_index > 0 && beat_index < this.length) {
                    val channels = this.get_all_channels()
                    for (i in channels.indices) {
                        for (j in 0 until channels[i].lines.size) {
                            val line = channels[i].lines[j]
                            val working_position = line.get_first_position(beat_index, listOf())

                            val head_position = line.get_blocking_position(beat_index, working_position) ?: Pair(beat_index, working_position)
                            if (head_position.first < beat_index) {
                                var max_beat_blocked = beat_index
                                for ((blocked_beat, _) in line.get_all_blocked_positions(head_position.first, head_position.second)) {
                                    max_beat_blocked = max(max_beat_blocked, blocked_beat)
                                }

                                if (max_beat_blocked > beat_index) {
                                    for (b in beat_index .. max_beat_blocked) {
                                        this._ui_change_bill.queue_column_change(b + 1, true)
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

    private fun _pre_remove_channel(channel: Int): Triple<Int, Int, List<Int>> {
        val y = try {
            this.get_instrument_line_index(channel, 0)
        } catch (_: IndexOutOfBoundsException) {
            this.get_total_line_count()
        }

        val ctl_row = this.get_visible_row_from_ctl_line(this.get_actual_line_index(y))!!
        val channels = this.get_all_channels()
        var removed_row_count = channels[channel].size

        // NOTE: Accessing this.channels instead of this.get_all_channels since it's not possible to remove percussion channel
        for ((_, controller) in channels[channel].controllers.get_all()) {
            if (controller.visible) {
                removed_row_count += 1
            }
        }

        for (j in 0 until channels[channel].lines.size) {
            val line = channels[channel].lines[j]
            for ((_, controller) in line.controllers.get_all()) {
                if (controller.visible) {
                    removed_row_count += 1
                }
            }
        }

        val changed_columns = this.get_editor_table().remove_mapped_lines(ctl_row, removed_row_count)

        return Triple(ctl_row, removed_row_count, changed_columns)
    }

    override fun remove_channel(channel: Int) {

        if (!this._ui_change_bill.is_full_locked()) {
            this.lock_ui_partial {
                val (ctl_row, removed_row_count, changed_columns) = this._pre_remove_channel(channel)
                super.remove_channel(channel)
                this.get_activity()?.shift_down_percussion_names(channel)
                this._ui_change_bill.queue_remove_channel(channel)
                this._ui_change_bill.queue_row_removal(ctl_row, removed_row_count)
                this._ui_change_bill.queue_column_changes(changed_columns, false)
            }
        } else {
            super.remove_channel(channel)
            this.get_activity()?.shift_down_percussion_names(channel)
        }
    }

    override fun on_project_changed() {
        super.on_project_changed()

        this.get_activity()?.let { activity ->
            activity.disconnect_feedback_device()
            activity.update_channel_instruments()
        }

        this.recache_line_maps()
        this._ui_change_bill.queue_full_refresh(this._in_reload)
        this.initialized = true

    }

    override fun project_change_wrapper(callback: () -> Unit)  {
        this.lock_ui_full {
            this.get_activity()?.let { activity ->
                activity.disconnect_feedback_device()
                activity.active_percussion_names.clear()
            }

            this._ui_clear()
            super.project_change_wrapper(callback)
        }
    }
    override fun project_refresh() {
        this.lock_ui_full {
            this.get_editor_table().clear()
            this._ui_clear()
            super.project_refresh()
        }
    }

    // This function is called from the Base Layer within th project_change_wrapper.
    // It's implicitly wrapped in a lock_ui_full call
    override fun _project_change_new() {
        val activity = this.get_activity()!!
        super._project_change_new()


        // set the default instrument to the first available in the soundfont (if applicable)
        for (c in this.channels.indices) {
            if (!this.is_percussion(c)) {
                continue
            }
            // Need to prematurely update the channel instrument to find the lowest possible instrument
            activity.update_channel_instruments(c)
            activity.populate_active_percussion_names(c, true)
            val percussion_keys = activity.active_percussion_names[c]?.keys?.sorted() ?: continue
            for (l in 0 until this.get_channel(c).size) {
                this.percussion_set_instrument(c, l, max(0, percussion_keys.first() - 27))
            }
        }

        activity.active_project = null
    }

    // This function is called from the Base Layer within th project_change_wrapper.
    // It's implicitly wrapped in a lock_ui_full call
    override fun _project_change_midi(midi: Midi) {
        super._project_change_midi(midi)
        val activity = this.get_activity()!!
        activity.active_project = null
    }


    override fun <T: EffectEvent> controller_global_set_initial_event(type: EffectType, event: T) {
        this.lock_ui_partial {
            super.controller_global_set_initial_event(type, event)
            this._ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: EffectEvent> controller_channel_set_initial_event(type: EffectType, channel: Int, event: T) {
        this.lock_ui_partial {
            super.controller_channel_set_initial_event(type, channel, event)
            this._ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun <T: EffectEvent> controller_line_set_initial_event(type: EffectType, channel: Int, line_offset: Int, event: T) {
        this.lock_ui_partial {
            super.controller_line_set_initial_event(type, channel, line_offset, event)
            this._ui_change_bill.queue_refresh_context_menu()
        }
    }

    //override fun toggle_channel_controller_visibility(type: ControlEventType, channel_index: Int) {
    //    this.lock_ui_partial {
    //        super.toggle_channel_controller_visibility(type, channel_index)
    //        this._controller_visibility_toggle_callback()
    //    }
    //}

    override fun recache_line_maps() {
        super.recache_line_maps()
        this._set_overlap_callbacks()
    }

    override fun set_transpose(new_transpose: Pair<Int, Int>) {
        this.lock_ui_partial {
            super.set_transpose(new_transpose)
            this._ui_change_bill.queue_config_drawer_redraw_export_button()
        }
    }

    override fun set_tuning_map(new_map: Array<Pair<Int, Int>>, mod_events: Boolean) {
        this.lock_ui_partial {
            val was_tuning_standard = this.is_tuning_standard()
            val original_map = this.tuning_map

            super.set_tuning_map(new_map, mod_events)

            val is_tuning_standard = this.is_tuning_standard()

            val activity = this.get_activity()
            if (activity != null) {
                if (is_tuning_standard && !was_tuning_standard && activity.configuration.allow_midi_playback) {
                    activity.enable_physical_midi_output()
                } else if (!is_tuning_standard && was_tuning_standard) {
                    activity.block_physical_midi_output()
                }
            }

            this._ui_change_bill.queue_config_drawer_redraw_export_button()

            if (new_map.size != original_map.size && mod_events) {
                for (i in 0 until this.channels.size) {
                    for (j in 0 until this.channels[i].lines.size) {
                        for (k in 0 until this.length) {
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

            this._ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun to_json(): JSONHashMap {
        val output = super.to_json()
        val activity = this.get_activity() ?: return output
        if (activity.configuration.soundfont != null) {
            output.get_hashmap("d")["sf"] = activity.configuration.soundfont
        }
        return output
    }

    override fun _project_change_json(json_data: JSONHashMap) {
        super._project_change_json(json_data)
        if (!this._in_reload) {
            val activity = this.get_activity() ?: return
            if (! activity.configuration.use_preferred_soundfont) {
                return
            }

            val sf_path = json_data.get_hashmap("d").get_stringn("sf") ?: return
            if (sf_path != activity.configuration.soundfont) {
                val original_soundfont = activity.configuration.soundfont
                activity.configuration.soundfont = sf_path
                // Try opening the assigned soundfont, but if it fails for any reason, go back to the
                // Currently active one.
                try {
                    activity.set_soundfont()
                } catch (_: Exception) {
                    activity.configuration.soundfont = original_soundfont
                    activity.set_soundfont()
                }
                activity.save_configuration()
            }
        }
    }

    override fun move_channel(channel_index: Int, new_channel_index: Int) {
        this.lock_ui_partial {
            super.move_channel(channel_index, new_channel_index)
            for (y in min(channel_index, new_channel_index) until this.channels.size) {
                this._ui_change_bill.queue_refresh_channel(y)
            }

            for (y in 0 until this.get_total_line_count()) {
                this._ui_change_bill.queue_line_label_refresh(y)
            }
        }
    }

    override fun clear() {
        super.clear()
        val editor_table = this.get_editor_table()
        editor_table.clear()
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this.lock_ui_partial {
            super.set_duration(beat_key, position, duration)

            // Needs to be set to trigger potentially queued cell changes from on_overlap()
            this._queue_cell_change(beat_key)
            this._ui_change_bill.queue_refresh_context_menu()
        }
    }

    override fun channel_set_instrument(channel: Int, instrument: Pair<Int, Int>) {
        this.lock_ui_partial {
            super.channel_set_instrument(channel, instrument)
            if (!this._ui_change_bill.is_full_locked()) {
                // Updating channel instruments doesn't strictly need to be gated behind the full lock,
                // BUT this way these don't get called multiple times every setup
                val activity = this.get_activity()
                activity?.update_channel_instrument(
                    this.get_all_channels()[channel].get_midi_channel(),
                    instrument
                )

                this._ui_change_bill.queue_refresh_channel(channel)
                this._ui_change_bill.queue_refresh_context_menu()
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
        super.on_action_blocked(blocker_key, blocker_position)
        if (!this.project_changing) {
            this._set_temporary_blocker(blocker_key, blocker_position)
        }
    }

    override fun on_action_blocked_line_ctl(type: EffectType, blocker_key: BeatKey, blocker_position: List<Int>) {
        super.on_action_blocked_line_ctl(type, blocker_key, blocker_position)
        if (!this.project_changing) {
            this._set_temporary_blocker_line_ctl(type, blocker_key, blocker_position)
        }
    }

    override fun on_action_blocked_channel_ctl(type: EffectType, blocker_channel: Int, blocker_beat: Int, blocker_position: List<Int>) {
        super.on_action_blocked_channel_ctl(type, blocker_channel, blocker_beat, blocker_position)
        if (!this.project_changing) {
            this._set_temporary_blocker_channel_ctl(type, blocker_channel, blocker_beat, blocker_position)
        }
    }

    override fun on_action_blocked_global_ctl(type: EffectType, blocker_beat: Int, blocker_position: List<Int>) {
        super.on_action_blocked_global_ctl(type, blocker_beat, blocker_position)
        if (!this.project_changing) {
            this._set_temporary_blocker_global_ctl(type, blocker_beat, blocker_position)
        }
    }

    // BASE FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


    // HISTORY FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    override fun apply_undo(repeat: Int) {
        this.lock_ui_partial {
            super.apply_undo(repeat)
            this.recache_line_maps()
        }
    }

    // HISTORY FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    // CURSOR FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    override fun cursor_apply(cursor: OpusManagerCursor, force: Boolean) {
        if (!force && this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_apply(cursor, force)

            this._queue_cursor_update(this.cursor)

            when (cursor.mode) {
                CursorMode.Line -> {
                    if (cursor.ctl_level == null) {
                        this._ui_change_bill.queue_set_context_menu_line()
                    } else {
                        this._ui_change_bill.queue_set_context_menu_control_line()
                    }
                }
                CursorMode.Column -> {
                    this._ui_change_bill.queue_set_context_menu_column()
                }
                CursorMode.Single -> {
                    if (cursor.ctl_level == null) {
                        if (this.is_percussion(cursor.channel)) {
                            this._ui_change_bill.queue_set_context_menu_leaf_percussion()
                        } else {
                            /*
                                Need to set relative mode here since cursor_apply is called after history is applied
                                and set_relative_mode isn't called in replace_tree
                            */
                            val event = this.get_tree().get_event()
                            if (event is TunedInstrumentEvent) {
                                this.set_relative_mode(event)
                            }

                            this._ui_change_bill.queue_set_context_menu_leaf()
                        }
                    } else {
                        this._ui_change_bill.queue_set_context_menu_line_control_leaf()
                    }
                }

                CursorMode.Range -> {
                    this._ui_change_bill.queue_set_context_menu_range()
                }

                CursorMode.Unset -> {
                    this._ui_change_bill.queue_clear_context_menu()
                }

                CursorMode.Channel -> {
                    this._ui_change_bill.queue_set_context_menu_channel()
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
            this._unset_temporary_blocker()
            this._queue_cursor_update(this.cursor)
            this._ui_change_bill.queue_clear_context_menu()
        }
    }

    override fun cursor_select_line(channel: Int, line_offset: Int) {
        if (this._block_cursor_selection()) {
            return
        }

        this.lock_ui_partial {
            super.cursor_select_line(channel, line_offset)
            this.temporary_blocker = null

            this._queue_cursor_update(this.cursor)
            this._ui_change_bill.queue_set_context_menu_line()
        }
    }

    override fun cursor_select_channel(channel: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_select_channel(channel)
            this.temporary_blocker = null

            this._queue_cursor_update(this.cursor)
            this._ui_change_bill.queue_set_context_menu_channel()
        }
    }

    override fun cursor_select_channel_ctl_line(ctl_type: EffectType, channel: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_select_channel_ctl_line(ctl_type, channel)
            this.temporary_blocker = null

            this._queue_cursor_update(this.cursor)
            this._ui_change_bill.queue_set_context_menu_control_line()
        }
    }

    override fun cursor_select_line_ctl_line(ctl_type: EffectType, channel: Int, line_offset: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_select_line_ctl_line(ctl_type, channel, line_offset)
            this.temporary_blocker = null

            this._queue_cursor_update(this.cursor)
            this._ui_change_bill.queue_set_context_menu_control_line()
        }
    }

    override fun cursor_select_global_ctl_line(ctl_type: EffectType) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            super.cursor_select_global_ctl_line(ctl_type)
            this.temporary_blocker = null

            this._queue_cursor_update(this.cursor)
            this._ui_change_bill.queue_set_context_menu_control_line()
        }
    }

    fun force_cursor_select_column(beat: Int) {
        if (this.cursor.mode != CursorMode.Unset) {
            this.cursor_clear()
        }
        this.cursor_select_column(beat)
    }
    override fun cursor_select_column(beat: Int) {
        if (this._block_cursor_selection()) {
            return
        }

        this.lock_ui_partial {
            super.cursor_select_column(beat)
            this.temporary_blocker = null

            this._queue_cursor_update(this.cursor)
            this._ui_change_bill.queue_set_context_menu_column()
        }
    }

    override fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select(beat_key, position)

            val current_tree = this.get_tree()
            if (!this.is_percussion(beat_key.channel) && current_tree.has_event()) {
                this.set_relative_mode(current_tree.get_event()!! as TunedInstrumentEvent)
            }

            this._queue_cursor_update(this.cursor, false)
            if (this.is_percussion(beat_key.channel)) {
                this._ui_change_bill.queue_set_context_menu_leaf_percussion()
            } else {
                this._ui_change_bill.queue_set_context_menu_leaf()
            }

        }
    }

    override fun cursor_select_ctl_at_line(ctl_type: EffectType, beat_key: BeatKey, position: List<Int>) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_ctl_at_line(ctl_type, beat_key, position)

            this._queue_cursor_update(this.cursor, false)
            this._ui_change_bill.queue_set_context_menu_line_control_leaf()
        }
    }

    override fun cursor_select_ctl_at_channel(ctl_type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_ctl_at_channel(ctl_type, channel, beat, position)

            this._queue_cursor_update(this.cursor, false)
            this._ui_change_bill.queue_set_context_menu_line_control_leaf()
        }
    }

    override fun cursor_select_ctl_at_global(ctl_type: EffectType, beat: Int, position: List<Int>) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_ctl_at_global(ctl_type, beat, position)

            this._queue_cursor_update(this.cursor, false)
            this._ui_change_bill.queue_set_context_menu_line_control_leaf()
        }
    }

    override fun cursor_select_global_ctl_range(type: EffectType, first: Int, second: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_global_ctl_range(type, first, second)

            this._queue_cursor_update(this.cursor, false)
            this._ui_change_bill.queue_set_context_menu_line_control_leaf_b()
        }
    }

    override fun cursor_select_channel_ctl_range(type: EffectType, channel: Int, first: Int, second: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_channel_ctl_range(type, channel, first, second)
            this._queue_cursor_update(this.cursor.copy(), false)
            this._ui_change_bill.queue_set_context_menu_line_control_leaf_b()
        }
    }

    override fun cursor_select_line_ctl_range(type: EffectType, beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_line_ctl_range(type, beat_key_a, beat_key_b)

            this._queue_cursor_update(this.cursor.copy(), false)
            this._ui_change_bill.queue_set_context_menu_line_control_leaf_b()
        }
    }

    override fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) {
            return
        }
        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_range(beat_key_a, beat_key_b)

            this._queue_cursor_update(this.cursor.copy(), false)
            this._ui_change_bill.queue_set_context_menu_range()
        }
    }

    // CURSOR FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //------------------------------------------------------------------------
    private fun _set_temporary_blocker(beat_key: BeatKey, position: List<Int>) {
        this.get_activity()?.vibrate()
        this.lock_ui_partial {
            this.temporary_blocker = OpusManagerCursor(
                mode = CursorMode.Single,
                channel = beat_key.channel,
                line_offset = beat_key.line_offset,
                beat = beat_key.beat,
                position = position
            )
        }
    }

    private fun _set_temporary_blocker_line_ctl(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        this.get_activity()?.vibrate()
        this.lock_ui_partial {
            this.temporary_blocker = OpusManagerCursor(
                mode = CursorMode.Single,
                ctl_type = type,
                ctl_level = CtlLineLevel.Line,
                channel = beat_key.channel,
                line_offset = beat_key.line_offset,
                beat = beat_key.beat,
                position = position
            )
        }
    }

    private fun _set_temporary_blocker_channel_ctl(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.get_activity()?.vibrate()
        this.lock_ui_partial {
            this.temporary_blocker = OpusManagerCursor(
                mode = CursorMode.Single,
                ctl_type = type,
                ctl_level = CtlLineLevel.Channel,
                channel = channel,
                beat = beat,
                position = position
            )
        }
    }

    private fun _set_temporary_blocker_global_ctl(type: EffectType, beat: Int, position: List<Int>) {
        this.get_activity()?.vibrate()
        this.lock_ui_partial {
            this.temporary_blocker = OpusManagerCursor(
                mode = CursorMode.Single,
                ctl_type = type,
                ctl_level = CtlLineLevel.Global,
                beat = beat,
                position = position
            )
        }
    }

    private fun _unset_temporary_blocker() {
        this.lock_ui_partial {
            val blocker = this.temporary_blocker
            if (blocker != null) {
                when (blocker.ctl_level) {
                    CtlLineLevel.Line -> this._queue_line_ctl_cell_change(blocker.ctl_type!!, blocker.get_beatkey())
                    CtlLineLevel.Channel -> this._queue_channel_ctl_cell_change(blocker.ctl_type!!, blocker.channel, blocker.beat)
                    CtlLineLevel.Global -> this._queue_global_ctl_cell_change(blocker.ctl_type!!, blocker.beat)
                    null -> this._queue_cell_change(blocker.get_beatkey())
                }
            }
            this.temporary_blocker = null
        }
    }



    private fun _set_overlap_callbacks() {
        val channels = this.get_all_channels()
        for (channel in channels.indices) {
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

    private fun _queue_scroll_to_cursor(cursor: OpusManagerCursor) {
        val y = when (cursor.mode) {
            CursorMode.Line,
            CursorMode.Single -> {
                when (cursor.ctl_level) {
                    CtlLineLevel.Line -> this.get_visible_row_from_ctl_line_line(cursor.ctl_type!!, cursor.channel, cursor.line_offset)
                    CtlLineLevel.Channel -> this.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, cursor.channel)
                    CtlLineLevel.Global -> this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                    null -> {
                        try {
                            this.get_visible_row_from_ctl_line(
                                this.get_actual_line_index(
                                    this.get_instrument_line_index(
                                        this.cursor.channel,
                                        this.cursor.line_offset
                                    )
                                )
                            )
                        } catch (_: IndexOutOfBoundsException) {
                            return // nothing to select
                        }
                    }
                }
            }
            CursorMode.Range -> {
                when (cursor.ctl_level) {
                    CtlLineLevel.Line -> this.get_visible_row_from_ctl_line_line(cursor.ctl_type!!, cursor.range!!.second.channel, cursor.range!!.second.line_offset)
                    CtlLineLevel.Channel -> this.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, cursor.range!!.second.channel)
                    CtlLineLevel.Global ->  this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                    null -> this.get_visible_row_from_ctl_line(
                        this.get_actual_line_index(
                            this.get_instrument_line_index(
                                cursor.range!!.second.channel,
                                cursor.range!!.second.line_offset
                            )
                        )
                    )
                }

            }
            CursorMode.Column,
            CursorMode.Unset -> null

            CursorMode.Channel -> {
                this.get_visible_row_from_ctl_line(
                    this.get_actual_line_index(
                        this.get_instrument_line_index(
                            this.cursor.channel,
                            0
                        )
                    )
                )
            }
        }

        val (beat, offset, offset_width) = when (cursor.mode) {
            CursorMode.Channel -> Triple(null, Rational(0, 1), Rational(1, 1))
            CursorMode.Line -> Triple(null, Rational(0, 1), Rational(1, 1))
            CursorMode.Column -> Triple(cursor.beat, Rational(0, 1), Rational(1, 1))
            CursorMode.Single -> {
                var tree: ReducibleTree<out OpusEvent> = when (cursor.ctl_level) {
                    CtlLineLevel.Line -> this.get_line_ctl_tree(cursor.ctl_type!!, cursor.get_beatkey())
                    CtlLineLevel.Channel -> this.get_channel_ctl_tree(cursor.ctl_type!!, cursor.channel, cursor.beat)
                    CtlLineLevel.Global -> this.get_global_ctl_tree(cursor.ctl_type!!, cursor.beat)
                    null -> this.get_tree(cursor.get_beatkey())
                }

                val width = Rational(1, 1)
                var offset = Rational(0, 1)
                for (p in cursor.get_position()) {
                    width.denominator *= tree.size
                    offset += Rational(p, width.denominator)
                    tree = tree[p]
                }

                Triple(cursor.beat, offset, width)
            }

            CursorMode.Range -> Triple(cursor.range!!.second.beat, Rational(0, 1), Rational(1, 1))
            CursorMode.Unset -> Triple(null, Rational(0, 1), Rational(1, 1))
        }

        this._ui_change_bill.queue_force_scroll(y ?: -1, beat ?: -1, offset, offset_width, this._activity?.in_playback() == true)
    }

    private fun _queue_cursor_update(cursor: OpusManagerCursor, deep_update: Boolean = true) {
        if (cursor != this._cache_cursor) {
            try {
                this._queue_cursor_update(this._cache_cursor, deep_update)
            } catch (_: InvalidGetCall) {
                // Pass
            }

            this._cache_cursor = cursor.copy()
            this._queue_scroll_to_cursor(cursor)
        }
        val coordinates_to_update = mutableSetOf<EditorTable.Coordinate>()

        when (cursor.mode) {
            CursorMode.Single -> {
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
                        } catch (_: IndexOutOfBoundsException) {
                            return
                        }

                        if (y != null) {
                            val line = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset]
                            val shadow_beats = mutableSetOf<Int>()
                            val event_head = try {
                                line.get_blocking_position(beat_key.beat, cursor.get_position()) ?: Pair(beat_key.beat, cursor.get_position())
                            } catch (_: IndexOutOfBoundsException) {
                                return // dead cursor
                            }

                            for ((shadow_beat, _) in line.get_all_blocked_positions(event_head.first, event_head.second)) {
                                shadow_beats.add(shadow_beat)
                            }

                            for (shadow_beat in shadow_beats) {
                                coordinates_to_update.add(EditorTable.Coordinate(y, shadow_beat))
                            }

                            this._ui_change_bill.queue_line_label_refresh(y)
                        }
                        this._ui_change_bill.queue_column_label_refresh(beat_key.beat)
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
                                    this._ui_change_bill.queue_line_label_refresh(line_y)
                                }

                                val channel = this.get_all_channels()[cursor.channel]
                                try {
                                    Pair(
                                        this.get_visible_row_from_ctl_line_line(cursor.ctl_type!!, cursor.channel, cursor.line_offset),
                                        channel.lines[cursor.line_offset].get_controller(cursor.ctl_type!!)
                                    )
                                } catch (_: NullPointerException) {
                                    // Dead cursor
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
                                    this._ui_change_bill.queue_line_label_refresh(line_y++)
                                    for ((_, controller) in channel.lines[line_offset].controllers.get_all()) {
                                        if (!controller.visible) {
                                            continue
                                        }
                                        this._ui_change_bill.queue_line_label_refresh(line_y++)
                                    }
                                }
                                try {
                                    Pair(
                                        this.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, cursor.channel),
                                        this.get_all_channels()[cursor.channel].get_controller(cursor.ctl_type!!)
                                    )
                                } catch (_: NullPointerException) {
                                    // Dead cursor
                                    return
                                }
                            }

                            CtlLineLevel.Global -> {
                                try {
                                    Pair(
                                        this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!),
                                        this.get_controller<EffectEvent>(cursor.ctl_type!!)
                                    )
                                } catch (_: NullPointerException) {
                                    // Dead cursor
                                    return
                                }
                            }
                        }

                        val shadow_beats = mutableSetOf<Int>()
                        val beat = cursor.beat
                        val event_head = try {
                            controller.get_blocking_position(beat, cursor.get_position()) ?: Pair(beat, cursor.get_position())
                        } catch (_: IndexOutOfBoundsException) {
                            return // Dead Cursor
                        }
                        for ((shadow_beat, _) in controller.get_all_blocked_positions(event_head.first, event_head.second)) {
                            shadow_beats.add(shadow_beat)
                        }

                        for (shadow_beat in shadow_beats) {
                            if (shadow_beat == beat) {
                                this._ui_change_bill.queue_column_label_refresh(shadow_beat)
                            }
                            coordinates_to_update.add(EditorTable.Coordinate(y, shadow_beat))
                        }

                        this._ui_change_bill.queue_line_label_refresh(y)
                        this._ui_change_bill.queue_column_label_refresh(beat)
                    }
                }
            }

            CursorMode.Range -> {
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
                            } catch (_: IndexOutOfBoundsException) {
                                continue
                            }

                            this._ui_change_bill.queue_line_label_refresh(y)

                            var i = 1
                            for ((_, controller) in this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].controllers.get_all()) {
                                if (controller.visible) {
                                    this._ui_change_bill.queue_line_label_refresh(y + i++)
                                }
                            }



                            this._ui_change_bill.queue_column_label_refresh(beat_key.beat)

                            coordinates_to_update.add(EditorTable.Coordinate(y, beat_key.beat))
                        }
                    }
                    else -> {
                        val (top_left, bottom_right) = cursor.get_ordered_range()!!
                        val y = when (cursor.ctl_level!!) {
                            // Can assume top_left.channel == bottom_right.channel and top_left.line_offset == bottom_right.line_offset
                            CtlLineLevel.Line -> {
                                this._ui_change_bill.queue_line_label_refresh(
                                    this.get_visible_row_from_ctl_line(
                                        this.get_actual_line_index(
                                            this.get_instrument_line_index(
                                                top_left.channel,
                                                top_left.line_offset
                                            )
                                        )
                                    )!!
                                )

                                this.get_visible_row_from_ctl_line_line(
                                    cursor.ctl_type!!,
                                    top_left.channel,
                                    top_left.line_offset
                                )
                            }
                            // Can assume top_left.channel == bottom_right.channel
                            CtlLineLevel.Channel -> {
                                for (i in 0 until this.get_channel(top_left.channel).lines.size) {
                                    this._ui_change_bill.queue_line_label_refresh(
                                        this.get_visible_row_from_ctl_line(
                                            this.get_actual_line_index(
                                                this.get_instrument_line_index(
                                                    top_left.channel,
                                                    i
                                                )
                                            )
                                        )!!
                                    )
                                }
                                this.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, top_left.channel)
                            }
                            CtlLineLevel.Global -> this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                        }

                        val first_beat = min(top_left.beat, bottom_right.beat)
                        val last_beat = max(top_left.beat, bottom_right.beat)
                        this._ui_change_bill.queue_line_label_refresh(y)
                        for (x in first_beat..last_beat) {
                            this._ui_change_bill.queue_column_label_refresh(x)
                            coordinates_to_update.add(EditorTable.Coordinate(y, x))
                        }
                    }
                }
            }

            CursorMode.Line -> {
                val y = when (cursor.ctl_level) {
                    null -> {
                        try {
                            val line_y = this.get_visible_row_from_ctl_line(
                                this.get_actual_line_index(
                                    this.get_instrument_line_index(
                                        cursor.channel,
                                        cursor.line_offset
                                    )
                                )
                            ) ?: return

                            val channels = this.get_all_channels()
                            var j = 1
                            for ((_, controller) in channels[cursor.channel].lines[cursor.line_offset].controllers.get_all()) {
                                if  (controller.visible) {
                                    this._ui_change_bill.queue_line_label_refresh(line_y + j++)
                                }
                            }

                            line_y
                        } catch (_: IndexOutOfBoundsException) {
                            return
                        }
                    }
                    CtlLineLevel.Line -> {
                        // Update Standard Line label attached to controller
                        val line_y = this.get_visible_row_from_ctl_line(
                            this.get_actual_line_index(
                                this.get_instrument_line_index(cursor.channel, cursor.line_offset)
                            )
                        )

                        val row_index = try {
                            this.get_visible_row_from_ctl_line_line(
                                cursor.ctl_type!!,
                                cursor.channel,
                                cursor.line_offset
                            )
                        } catch (_: NullPointerException) {
                            return
                        }

                        if (line_y != null) {
                            this._ui_change_bill.queue_line_label_refresh(line_y)
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
                            this._ui_change_bill.queue_line_label_refresh(line_y++)
                            for ((_, controller) in channel.lines[line_offset].controllers.get_all()) {
                                if (!controller.visible) {
                                    continue
                                }
                                this._ui_change_bill.queue_line_label_refresh(line_y++)
                            }
                        }
                        for ((_, controller) in channel.controllers.get_all()) {
                            if (!controller.visible) {
                                continue
                            }
                            this._ui_change_bill.queue_line_label_refresh(line_y++)
                        }

                        try {
                            this.get_visible_row_from_ctl_line_channel(
                                cursor.ctl_type!!,
                                cursor.channel
                            )
                        } catch (_: NullPointerException) {
                            return
                        }
                    }
                    CtlLineLevel.Global -> {
                        this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                    }
                }

                this._ui_change_bill.queue_row_change(y, true)
            }

            CursorMode.Column -> {
                this._ui_change_bill.queue_column_change(cursor.beat, false)
            }
            CursorMode.Unset -> { }
            CursorMode.Channel -> {
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
                        } catch (_: IndexOutOfBoundsException) {
                            return
                        }
                    }
                    else -> return // TODO: Throw Exception?
                }

                val channels = this.get_all_channels()
                var x = 0
                for (line in channels[cursor.channel].lines) {
                    this._ui_change_bill.queue_row_change(y + x++, true)
                    for (j in 0 until line.controllers.get_all().size) {
                        this._ui_change_bill.queue_row_change(y + x++, true)
                    }
                }
                for (j in 0 until channels[cursor.channel].controllers.get_all().size) {
                    this._ui_change_bill.queue_row_change(y + x++, true)
                }
            }
        }

        this._ui_change_bill.queue_cell_changes(coordinates_to_update.toList(), true)
    }

    private fun _init_editor_table_width_map() {
        if (this._ui_change_bill.is_full_locked()) {
            return
        }

        val editor_table = this.get_editor_table()
        editor_table.clear_column_map()

        for (beat in 0 until this.length) {
            val column = mutableListOf<Int>()
            this.get_visible_channels().forEachIndexed { i: Int, channel: OpusChannelAbstract<*,*> ->
                for (j in channel.lines.indices) {
                    val tree = this.get_tree(BeatKey(i, j, beat))
                    column.add(tree.get_total_child_weight())

                    for ((_, controller) in channel.lines[j].controllers.get_all()) {
                        if (!controller.visible) {
                            continue
                        }
                        val ctl_tree = controller.get_tree(beat)
                        column.add(ctl_tree.get_total_child_weight())
                    }
                }

                for ((_, controller) in channel.controllers.get_all()) {
                    if (!controller.visible) {
                        continue
                    }
                    val ctl_tree = controller.get_tree(beat)
                    column.add(ctl_tree.get_total_child_weight())
                }
            }

            for ((_, controller) in this.controllers.get_all()) {
                if (!controller.visible) {
                    continue
                }

                val ctl_tree = controller.get_tree(beat)
                column.add(ctl_tree.get_total_child_weight())
            }

            editor_table.add_column_to_map(beat, column)
        }
    }

    private fun _add_line_to_column_width_map(y: Int, line: OpusLineAbstract<*>) {
        if (this._ui_change_bill.is_full_locked()) {
            return
        }

        val column_updates = this.get_editor_table().add_line_to_map(
            y,
            List(this.length) { x: Int ->
                val tree = line.beats[x]
                tree.get_total_child_weight()
            }
        )

        this._ui_change_bill.queue_new_row(y)
        this._ui_change_bill.queue_column_changes(column_updates, false)
    }

    private fun _add_controller_to_column_width_map(y: Int, line: EffectController<*>) {
        if (this._ui_change_bill.is_full_locked()) {
            return
        }

        val column_updates = this.get_editor_table().add_line_to_map(
            y,
            List(this.length) { x: Int ->
                val tree = line.beats[x]
                tree.get_total_child_weight()
            }
        )

        this._ui_change_bill.queue_new_row(y)
        this._ui_change_bill.queue_column_changes(column_updates, false)
    }

    private fun _update_after_new_line(channel: Int, line_offset: Int?) {
        val working_channel = this.get_channel(channel)
        val adj_line_offset = line_offset ?: (working_channel.lines.size - 1)


        if (this._ui_change_bill.is_full_locked() || this.get_activity() == null) {
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
        controllers.forEachIndexed { i: Int, (_, controller): Pair<EffectType, EffectController<*>> ->
            if (controller.visible) {
                this._add_controller_to_column_width_map(visible_row + i + 1, controller)
            }
        }
    }

    private fun _new_column_in_column_width_map(index: Int) {
        if (this._ui_change_bill.is_full_locked()) {
            return
        }

        val column = mutableListOf<Int>()
        for (channel in this.get_visible_channels()) {
            for (j in channel.lines.indices) {
                column.add(1)
                for ((_, controller) in channel.lines[j].controllers.get_all()) {
                    if (!controller.visible) {
                        continue
                    }
                    column.add(1)
                }
            }

            for ((_, controller) in channel.controllers.get_all()) {
                if (!controller.visible) {
                    continue
                }
                column.add(1)
            }
        }
        for ((_, controller) in this.controllers.get_all()) {
            if (!controller.visible) {
                continue
            }
            column.add(1)
        }

        this.get_editor_table().add_column_to_map(index, column)
    }

    // UI FUNCS -----------------------
    private fun _apply_bill_changes() {
        val editor_table = try {
            this.get_editor_table()
        } catch (_: MissingEditorTableException) {
            this._ui_change_bill.clear()
            return
        }
        this.run_on_ui_thread { activity: ActivityEditor ->
            this._ui_change_bill.consolidate()
            while (true) {
                val entry = this._ui_change_bill.get_next_entry()
                when (entry) {
                    BillableItem.ForceScroll -> {
                        val y = this._ui_change_bill.get_next_int()
                        val x = this._ui_change_bill.get_next_int()
                        val offset = Rational(
                            this._ui_change_bill.get_next_int(),
                            this._ui_change_bill.get_next_int()
                        )

                        val offset_width = Rational(
                            this._ui_change_bill.get_next_int(),
                            this._ui_change_bill.get_next_int()
                        )
                        val force = this._ui_change_bill.get_next_int() != 0
                        editor_table.scroll_to_position(
                            y = if (y == -1) null else y,
                            x = if (x == -1) null else x,
                            offset = offset.numerator.toFloat() / offset.denominator.toFloat(),
                            offset_width = offset_width.numerator.toFloat() / offset_width.denominator.toFloat(),
                            force = force
                        )
                    }

                    BillableItem.FullRefresh -> {
                        activity.setup_project_config_drawer()
                        activity.update_menu_options()

                        this._init_editor_table_width_map()
                        editor_table.setup(this.get_row_count(), this.length)
                        activity.update_title_text()
                        activity.clear_context_menu()
                    }

                    BillableItem.RowAdd -> {
                        editor_table.new_row(
                            this._ui_change_bill.get_next_int()
                        )
                    }

                    BillableItem.RowRemove -> {
                        editor_table.remove_rows(
                            this._ui_change_bill.get_next_int(),
                            this._ui_change_bill.get_next_int()
                        )
                    }

                    BillableItem.RowChange -> {
                        editor_table.notify_row_changed(
                            this._ui_change_bill.get_next_int()
                        )
                    }

                    BillableItem.ColumnAdd -> {
                        editor_table.new_column(
                            this._ui_change_bill.get_next_int()
                        )
                    }

                    BillableItem.ColumnRemove -> {
                        editor_table.remove_column(
                            this._ui_change_bill.get_next_int()
                        )
                    }

                    BillableItem.ColumnChange -> {
                        val column = this._ui_change_bill.get_next_int()
                        if (column < editor_table.get_column_map_size()) {
                            editor_table.recalculate_column_max(column)
                            editor_table.notify_column_changed(column)
                        }
                    }

                    BillableItem.CellChange -> {
                        val cells = List(this._ui_change_bill.get_next_int()) {
                            EditorTable.Coordinate(
                                y = this._ui_change_bill.get_next_int(),
                                x = this._ui_change_bill.get_next_int()
                            )
                        }
                        editor_table.notify_cell_changes(cells)
                    }

                    BillableItem.ChannelChange -> {
                        val channel = this._ui_change_bill.get_next_int()

                        if (this.is_percussion(channel)) {
                            activity.populate_active_percussion_names(channel, true)
                        }

                        val channel_recycler = activity.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                        (channel_recycler.adapter as ChannelOptionAdapter?)?.let { adapter ->
                            for (i in adapter.itemCount until this.channels.size) {
                                adapter.add_channel()
                            }
                            (channel_recycler.adapter as ChannelOptionAdapter).notifyItemChanged(channel)
                        }
                    }

                    BillableItem.ChannelAdd -> {
                        val channel = this._ui_change_bill.get_next_int()
                        val channel_recycler = activity.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                        (channel_recycler.adapter as ChannelOptionAdapter?)?.let { adapter ->
                            for (i in adapter.itemCount until this.channels.size) {
                                adapter.add_channel()
                            }
                        }

                        activity.update_channel_instruments(channel)
                    }

                    BillableItem.ChannelRemove -> {
                        val channel = this._ui_change_bill.get_next_int()
                        val channel_recycler = activity.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                        (channel_recycler.adapter as ChannelOptionAdapter?)?.let { adapter ->
                            for (i in adapter.itemCount until this.channels.size + 1) {
                                adapter.add_channel()
                            }
                            adapter.remove_channel(channel)
                        }
                    }

                    BillableItem.ProjectNameChange -> {
                        activity.update_title_text()
                    }

                    BillableItem.ContextMenuRefresh -> {
                        activity.refresh_context_menu()
                    }

                    BillableItem.ContextMenuSetLine -> {
                        activity.set_context_menu_line()
                    }

                    BillableItem.ContextMenuSetLeaf -> {
                        activity.set_context_menu_leaf()
                    }

                    BillableItem.ContextMenuSetLeafPercussion -> {
                        activity.set_context_menu_leaf_percussion()
                    }

                    BillableItem.ContextMenuSetControlLeaf -> {
                        activity.set_context_menu_line_control_leaf()
                    }

                    BillableItem.ContextMenuSetControlLeafB -> {
                        activity.set_context_menu_line_control_leaf_b()
                    }

                    BillableItem.ContextMenuSetRange -> {
                        activity.set_context_menu_range()
                    }

                    BillableItem.ContextMenuSetColumn -> {
                        activity.set_context_menu_column()
                    }

                    BillableItem.ContextMenuSetControlLine -> {
                        activity.set_context_menu_control_line()
                    }

                    BillableItem.ContextMenuSetChannel -> {
                        activity.set_context_menu_channel()
                    }

                    BillableItem.ContextMenuClear -> {
                        activity.clear_context_menu()
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
                        val channel = this._ui_change_bill.get_next_int()
                        val line_offset = this._ui_change_bill.get_next_int()
                        val btn_choose_percussion: TextView = activity.findViewById(R.id.btnChoosePercussion) ?: continue
                        val instrument = this.get_percussion_instrument(channel, line_offset)

                        if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            btn_choose_percussion.text = activity.getString(R.string.label_short_percussion, instrument)
                        } else {
                            btn_choose_percussion.text = activity.getString(
                                R.string.label_choose_percussion,
                                instrument,
                                activity.get_drum_name(channel, instrument)
                            )
                        }
                    }

                    BillableItem.LineLabelRefresh -> {
                        editor_table.update_line_label(this._ui_change_bill.get_next_int())
                    }
                    BillableItem.ColumnLabelRefresh -> {
                        editor_table.update_column_label(this._ui_change_bill.get_next_int())
                    }
                    BillableItem.ColumnStateChange -> {
                        val column = this._ui_change_bill.get_next_int()
                        if (column < editor_table.get_column_map_size()) {
                            editor_table.notify_column_changed(column, true)
                        }
                    }
                    BillableItem.RowStateChange -> {
                        val y = this._ui_change_bill.get_next_int()
                        editor_table.notify_row_changed(y,true)
                    }
                    BillableItem.CellStateChange -> {
                        val cells = List(this._ui_change_bill.get_next_int()) {
                            EditorTable.Coordinate(
                                y = this._ui_change_bill.get_next_int(),
                                x = this._ui_change_bill.get_next_int()
                            )
                        }
                        editor_table.notify_cell_changes(cells, true)
                    }

                    null -> break
                }
            }
            // Temporary function call while I work on a spot-update solution
            editor_table.table_ui.finalize_update()

            this._ui_change_bill.clear()
        }
    }

    private fun run_on_ui_thread(callback: (ActivityEditor) -> Unit) {
        val main = this._activity ?: return // TODO: Throw Exception?
        val runnable = Runnable {
            callback(main)
        }
        synchronized(runnable) {
            main.runOnUiThread(runnable)
        }
    }

    // END UI FUNCS -----------------------

    private fun _ui_clear() {
        this.run_on_ui_thread { main ->
            val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
            if (channel_recycler.adapter != null) {
                val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
                channel_adapter.clear()
            }
        }
    }

    fun set_relative_mode(mode: RelativeInputMode, update_ui: Boolean = true) {
        this.relative_mode = mode
        this.lock_ui_partial {
            if (update_ui) {
                this._ui_change_bill.queue_refresh_context_menu()
            }
        }
    }

    fun set_relative_mode(event: TunedInstrumentEvent) {
        if (this._activity != null && this._activity!!.configuration.relative_mode) {
            this.relative_mode = when (event) {
                is RelativeNoteEvent -> {
                    if (event.offset >= 0) {
                        RelativeInputMode.Positive
                    } else {
                        RelativeInputMode.Negative
                    }
                }
                else -> {
                    RelativeInputMode.Absolute
                }
            }
        } else {
            this.relative_mode = RelativeInputMode.Absolute
        }
    }

    // Note: set_note_octave/offset functions need to be in interface layer since they require access to 'relative_mode' property
    private fun _set_note_octave(beat_key: BeatKey, position: List<Int>, octave: Int) {
        val current_tree_position = this.get_actual_position(
            beat_key,
            position
        )
        val current_tree = this.get_tree(current_tree_position.first, current_tree_position.second)
        val current_event = current_tree.get_event()
        val duration = current_event?.duration ?: 1
        val radix = this.tuning_map.size

        val new_event = when (this.relative_mode) {
            RelativeInputMode.Absolute -> {
                AbsoluteNoteEvent(
                    when (current_event) {
                        is AbsoluteNoteEvent -> (octave * radix) + (current_event.note % radix)
                        is RelativeNoteEvent -> {
                            this.convert_event_to_absolute(beat_key, position)
                            return this._set_note_octave(beat_key, position, octave)
                        }
                        else -> {
                            val cursor = this.cursor
                            val previous_value = this.get_absolute_value(cursor.get_beatkey(), cursor.get_position()) ?: 0
                            (octave * radix) + (previous_value % radix)
                        }
                    },
                    duration
                )
            }
            RelativeInputMode.Positive -> {
                RelativeNoteEvent(
                    when (current_event) {
                        is RelativeNoteEvent -> (octave * radix) + (current_event.offset % radix)
                        is AbsoluteNoteEvent -> {
                            this.convert_event_to_relative(beat_key, position)
                            return this._set_note_octave(beat_key, position, octave)
                        }
                        else -> octave * radix
                    },
                    duration
                )
            }
            RelativeInputMode.Negative -> {
                RelativeNoteEvent(
                    when (current_event) {
                        is RelativeNoteEvent -> 0 - ((octave * radix) + (abs(current_event.offset) % radix))
                        is AbsoluteNoteEvent -> {
                            this.convert_event_to_relative(beat_key, position)
                            return this._set_note_octave(beat_key, position, octave)
                        }
                        else -> 0 - (octave * radix)
                    },
                    duration
                )
            }
        }

        this.set_event(beat_key, position, new_event)
    }

    private fun _set_note_offset(beat_key: BeatKey, position: List<Int>, offset: Int) {
        val current_tree = this.get_tree(beat_key, position)

        val current_event = current_tree.get_event()
        val duration = current_event?.duration ?: 1

        val radix = this.tuning_map.size

        val new_event = when (this.relative_mode) {
            RelativeInputMode.Absolute -> {
                AbsoluteNoteEvent(
                    when (current_event) {
                        is AbsoluteNoteEvent -> ((current_event.note / radix) * radix) + offset
                        is RelativeNoteEvent -> {
                            this.convert_event_to_absolute(beat_key, position)
                            return this._set_note_offset(beat_key, position, offset)
                        }
                        else -> {
                            val previous_value = this.get_absolute_value(beat_key, position) ?: 0
                            ((previous_value / radix) * radix) + offset
                        }
                    },
                    duration
                )
            }
            RelativeInputMode.Positive -> {
                RelativeNoteEvent(
                    when (current_event) {
                        is RelativeNoteEvent -> ((current_event.offset / radix) * radix) + offset
                        is AbsoluteNoteEvent -> {
                            this.convert_event_to_relative(beat_key, position)
                            return this._set_note_offset(beat_key, position, offset)
                        }
                        else -> offset
                    },
                    duration
                )
            }
            RelativeInputMode.Negative -> {
                RelativeNoteEvent(
                    when (current_event) {
                        is RelativeNoteEvent -> ((current_event.offset / radix) * radix) - offset
                        is AbsoluteNoteEvent -> {
                            this.convert_event_to_relative(beat_key, position)
                            return this._set_note_offset(beat_key, position, offset)
                        }
                        else -> 0 - offset
                    },
                    duration
                )
            }
        }

        this.set_event(beat_key, position, new_event)
    }

    fun set_note_octave_at_cursor(octave: Int) {
        if (this.cursor.mode != CursorMode.Single) {
            throw IncorrectCursorMode(this.cursor.mode, CursorMode.Single)
        }
        val current_tree_position = this.get_actual_position(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
        this._set_note_octave(current_tree_position.first, current_tree_position.second, octave)
    }

    fun set_note_offset_at_cursor(offset: Int) {
        if (this.cursor.mode != CursorMode.Single) {
            throw IncorrectCursorMode(this.cursor.mode, CursorMode.Single)
        }
        val current_tree_position = this.get_actual_position(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
        this._set_note_offset(current_tree_position.first, current_tree_position.second, offset)
    }

    override fun move_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        this.lock_ui_partial {
            super.move_beat_range(beat_key, first_corner, second_corner)
        }
    }

    fun is_initialized(): Boolean {
        return this.initialized
    }
}