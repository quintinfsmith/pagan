package com.qfs.pagan
import com.qfs.apres.Midi
import com.qfs.json.JSONHashMap
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.DrawerChannelMenu.ChannelOptionAdapter
import com.qfs.pagan.DrawerChannelMenu.ChannelOptionRecycler
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.BlockedActionException
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.InstrumentEvent
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.OpusLineAbstract
import com.qfs.pagan.structure.opusmanager.base.OpusLinePercussion
import com.qfs.pagan.structure.opusmanager.base.OpusPercussionChannel
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.TunedInstrumentEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.opusmanager.cursor.IncorrectCursorMode
import com.qfs.pagan.structure.opusmanager.cursor.OpusManagerCursor
import com.qfs.pagan.structure.opusmanager.history.OpusLayerHistory
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import com.qfs.pagan.uibill.UIChangeBill
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class OpusLayerInterface : OpusLayerHistory() {
    class HidingNonEmptyPercussionException: Exception()
    class HidingLastChannelException: Exception()
    class MissingEditorTableException: Exception()

    companion object {
        val global_controller_domain = listOf(
            Pair(EffectType.Tempo, R.drawable.icon_tempo),
            Pair(EffectType.Delay, R.drawable.icon_echo)
        )

        val channel_controller_domain = listOf(
            Pair(EffectType.Volume, R.drawable.icon_volume),
            Pair(EffectType.Pan, R.drawable.icon_pan),
            Pair(EffectType.Delay, R.drawable.icon_echo)
        )

        val line_controller_domain = listOf(
            Pair(EffectType.Volume, R.drawable.icon_volume),
            Pair(EffectType.Velocity, R.drawable.icon_velocity),
            Pair(EffectType.Pan, R.drawable.icon_pan),
            Pair(EffectType.Delay, R.drawable.icon_echo)
        )
    }

    // Refactored properties  //////////////////////
    var minimum_percussions = HashMap<Int, Int>()
    /////////////////////////////////////////////

    var initialized = false
    var relative_mode: RelativeInputMode = RelativeInputMode.Absolute
    private var _activity: ActivityEditor? = null
    var marked_range: Pair<BeatKey, BeatKey>? = null

    val ui_facade = UIChangeBill()
    var temporary_blocker: OpusManagerCursor? = null

    var latest_set_octave: Int? = null
    var latest_set_offset: Int? = null

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
        this.ui_facade.lock_full()

        val output = try {
            val tmp = callback()
            this.ui_facade.unlock()
            tmp
        } catch (e: BlockedActionException) {
            this.ui_facade.unlock()
            this.ui_facade.cancel_most_recent()
            if (!this.ui_facade.is_locked()) {
                if (this.temporary_blocker != null) {
                    this.cursor_apply(this.temporary_blocker!!)
                }
                null
            } else { // Still Locked
                throw e
            }
        } catch (e: Exception) {
            this.ui_facade.unlock()
            this.ui_facade.cancel_most_recent()
            throw e
        }

        if (!this.ui_facade.is_locked()) {
            //this._apply_bill_changes()
        }

        return output
    }

    private fun <T> lock_ui_partial(callback: () -> T): T? {
        this.ui_facade.lock_partial()
        val output = try {
            val tmp = callback()
            this.ui_facade.unlock()
            tmp
        } catch (e: BlockedActionException) {
            this.ui_facade.unlock()
            this.ui_facade.cancel_most_recent()
            if (!this.ui_facade.is_locked()) {
                if (this.temporary_blocker != null) {
                    this.cursor_apply(this.temporary_blocker!!)
                }
                null
            } else { // Still Locked
                throw e
            }
        } catch (e: Exception) {
            this.ui_facade.unlock()
            this.ui_facade.cancel_most_recent()
            throw e
        }

        if (!this.ui_facade.is_locked()) {
            //this._apply_bill_changes()
        }

        return output
    }

    private fun _clear_facade_blocker_leaf() {
        this.ui_facade.blocker_leaf = null
    }
    private fun _update_facade_blocker_leaf_line_ctl(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        this.ui_facade.blocker_leaf = listOf<Int>(
            try {
                this.get_visible_row_from_ctl_line_line(type, beat_key.channel, beat_key.line_offset)
            } catch (_: IndexOutOfBoundsException) { // may reference a channel's line before the channel exists
                this.get_row_count()
            },
            beat_key.beat,
        ) + position
    }
    private fun _update_facade_blocker_leaf_channel_ctl(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.ui_facade.blocker_leaf = listOf<Int>(
            try {
                this.get_visible_row_from_ctl_line_channel(type, channel)
            } catch (_: IndexOutOfBoundsException) { // may reference a channel's line before the channel exists
                this.get_row_count()
            },
            beat,
        ) + position
    }
    private fun _update_facade_blocker_leaf_global_ctl(type: EffectType, beat: Int, position: List<Int>) {
        this.ui_facade.blocker_leaf = listOf<Int>(
            try {
                this.get_visible_row_from_ctl_line_global(type)
            } catch (_: IndexOutOfBoundsException) { // may reference a channel's line before the channel exists
                this.get_row_count()
            },
            beat,
        ) + position
    }

    private fun _update_facade_blocker_leaf(beat_key: BeatKey, position: List<Int>) {
        this.ui_facade.blocker_leaf = listOf<Int>(
            try {
                this.get_visible_row_from_ctl_line(
                    this.get_actual_line_index(
                        this.get_instrument_line_index(
                            beat_key.channel,
                            beat_key.line_offset
                        )
                    )
                )!!
            } catch (_: IndexOutOfBoundsException) { // may reference a channel's line before the channel exists
                this.get_row_count()
            },
            beat_key.beat,
        ) + position
    }

    /* Notify the editor table to update a cell */
    private fun _queue_cell_change(beat_key: BeatKey) {
        if (this.project_changing || this.ui_facade.is_full_locked()) return

        this.ui_facade.queue_cell_change(
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
            ),
            this.get_tree_copy(beat_key)
        )
    }

    private fun _queue_global_ctl_cell_change(type: EffectType, beat: Int) {
        val controller = this.get_controller<EffectEvent>(type)
        if (!controller.visible || this.ui_facade.is_full_locked()) return

        this.ui_facade.queue_cell_change(
            EditorTable.Coordinate(
                y = this.get_visible_row_from_ctl_line_global(type),
                x = beat
            ),
            controller.get_tree(beat).copy { it.copy() }
        )
    }

    private fun _queue_channel_ctl_cell_change(type: EffectType, channel: Int, beat: Int) {
        val controller = this.get_all_channels()[channel].get_controller<EffectEvent>(type)
        if (!controller.visible || this.ui_facade.is_full_locked()) return

        this.ui_facade.queue_cell_change(
            EditorTable.Coordinate(
                y = this.get_visible_row_from_ctl_line_channel(type, channel),
                x = beat
            ),
            controller.get_tree(beat).copy { it.copy() }
        )
    }

    private fun _queue_line_ctl_cell_change(type: EffectType, beat_key: BeatKey) {
        val controller = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].get_controller<EffectEvent>(type)
        if (!controller.visible || this.ui_facade.is_full_locked()) return

        this.ui_facade.queue_cell_change(
            EditorTable.Coordinate(
                y = this.get_visible_row_from_ctl_line_line(type, beat_key.channel, beat_key.line_offset),
                x = beat_key.beat
            ),
            controller.get_tree(beat_key.beat).copy { it.copy() }
        )
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
                this._add_controller_to_column_width_map(visible_row, controller, channel_index, line_offset, type)
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
                this._add_controller_to_column_width_map(visible_row, controller, channel_index, null, type)
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
                this._add_controller_to_column_width_map(visible_row, controller, null, null, type)
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
            if (!this.ui_facade.is_full_locked()) {
                this.ui_facade.queue_project_name_change()
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
            this.ui_facade.set_active_event(event.copy())
        }
    }

    override fun <T: EffectEvent> controller_channel_set_event(type: EffectType, channel: Int, beat: Int, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.controller_channel_set_event(type, channel, beat, position, event)
            this._queue_channel_ctl_cell_change(type, channel, beat)
            this.ui_facade.set_active_event(event.copy())
        }
    }

    override fun <T: EffectEvent> controller_line_set_event(type: EffectType, beat_key: BeatKey, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.controller_line_set_event(type, beat_key, position, event)
            this._queue_line_ctl_cell_change(type, beat_key)
            this.ui_facade.set_active_event(event.copy())
        }
    }

    override fun <T: InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        this.lock_ui_partial {
            super.set_event(beat_key, position, event)

            if (event is TunedInstrumentEvent) {
                this.set_relative_mode(event)
            }

            if (!this.ui_facade.is_full_locked()) {
                this._queue_cell_change(beat_key)
                this.ui_facade.set_active_event(event.copy())
            }
        }
    }

    override fun percussion_set_event(beat_key: BeatKey, position: List<Int>) {
        this.lock_ui_partial {
            super.percussion_set_event(beat_key, position)
            this._queue_cell_change(beat_key)
        }
    }

    override fun percussion_set_instrument(channel: Int, line_offset: Int, instrument: Int) {
        this.lock_ui_partial {
            super.percussion_set_instrument(channel, line_offset, instrument)
            // Need to call get_drum name to repopulate instrument list if needed
            // this.get_activity()?.get_drum_name(channel, instrument)

            this.ui_facade.queue_refresh_choose_percussion_button(channel, line_offset)
            this.ui_facade.queue_line_label_refresh(
                y = this.get_visible_row_from_ctl_line(
                    this.get_actual_line_index(
                        this.get_instrument_line_index(channel, line_offset)
                    )
                )!!,
                is_percussion = true,
                channel = channel,
                offset = instrument,
            )
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

    private fun get_minimum_percussion_instrument(channel: Int): Int {
        return this.minimum_percussions[channel] ?: 0
    }

    override fun new_line(channel: Int, line_offset: Int?) {
        this.lock_ui_partial {
            super.new_line(channel, line_offset)

            // set the default instrument to the first available in the soundfont (if applicable)
            if (this.is_percussion(channel)) {
                (this.get_channel(channel) as OpusPercussionChannel).let {
                    it.lines[line_offset ?: (it.size - 1)].instrument = this.get_minimum_percussion_instrument(channel) - 27
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
        val first_swapped_line = min(
            this.get_instrument_line_index(channel_a, line_a),
            this.get_instrument_line_index(channel_b, line_b)
        )

        for (c in 0 until this.channels.size) {
            val channel = this.channels[c]
            val is_percussion = this.is_percussion(c)
            for (l in 0 until channel.lines.size) {
                val line = channel.lines[l]

                if (y >= first_swapped_line) {
                    this.ui_facade.queue_line_label_refresh(
                        y,
                        is_percussion,
                        channel = c,
                        offset = if (is_percussion) {
                            (line as OpusLinePercussion).instrument
                        } else {
                            l
                        }
                    )
                }

                for ((type, controller) in line.controllers.get_all()) {
                    if (!controller.visible) continue
                    if (y >= first_swapped_line) {
                        this.ui_facade.queue_line_label_refresh(
                            y,
                            is_percussion,
                            channel = c,
                            offset = if (is_percussion) {
                                (line as OpusLinePercussion).instrument
                            } else {
                                l
                            },
                            control_type = type
                        )
                    }
                    this.ui_facade.queue_row_change(y++)
                }
            }

            for ((type, controller) in channel.controllers.get_all()) {
                if (controller.visible) {
                    if (y >= first_swapped_line) {
                        this.ui_facade.queue_line_label_refresh(y, is_percussion, c, null, type)
                    }
                }
            }
        }

        for ((type, controller) in this.controllers.get_all()) {
            if (controller.visible) {
                if (y >= first_swapped_line) {
                    this.ui_facade.queue_line_label_refresh(y, false, null, null, type)
                }
            }
        }
    }

    override fun swap_lines(channel_index_a: Int, line_offset_a: Int, channel_index_b: Int, line_offset_b: Int) {
        this.lock_ui_partial {
            val y_a = this.get_instrument_line_index(channel_index_a, line_offset_a)
            val y_b = this.get_instrument_line_index(channel_index_b, line_offset_b)
            this.ui_facade.swap_line_cells(y_a, y_b)



            super.swap_lines(channel_index_a, line_offset_a, channel_index_b, line_offset_b)
            this._swap_line_ui_update(channel_index_a, line_offset_a, channel_index_b, line_offset_b)
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
        this.ui_facade.queue_row_removal(y, count)
    }

    /* Used to update the ui after new_channel and set_channel_visibility(n, true) */
    private fun _post_new_channel(channel: Int, lines: Int) {
        if (this.ui_facade.is_full_locked()) return

        val y = this.get_instrument_line_index(channel, 0)
        var ctl_row = this.get_visible_row_from_ctl_line(this.get_actual_line_index(y))!!


        val channels = this.get_all_channels()
        for (j in 0 until lines) {
            val line = channels[channel].lines[j]

            this.ui_facade.queue_new_row(
                ctl_row++,
                MutableList(line.beats.size) { line.beats[it].copy() },
                UIChangeBill.LineData(
                    channel,
                    j,
                    null,
                    if (this.is_percussion(channel)) {
                        (line as OpusLinePercussion).instrument
                    } else {
                        null
                    }
                )
            )
            for ((type, controller) in line.controllers.get_all()) {
                if (!controller.visible) continue
                this._add_controller_to_column_width_map(ctl_row++, controller, channel, j, type)
            }
        }

        val controllers = channels[channel].controllers.get_all()
        for ((type, controller) in controllers) {
            if (! controller.visible) continue
            this._add_controller_to_column_width_map(ctl_row++, controller, channel, null, type)
        }
    }
    override fun new_channel(channel: Int?, lines: Int, uuid: Int?, is_percussion: Boolean) {
        this.lock_ui_partial {
            val notify_index = channel ?: this.channels.size
            super.new_channel(channel, lines, uuid, is_percussion)
            this.ui_facade.queue_add_channel(notify_index, is_percussion, this.channels[notify_index].get_instrument())
            this._post_new_channel(notify_index, lines)

            this.ui_facade.shift_up_percussion_names(notify_index)
            this._activity?.update_channel_instruments()
        }
    }

    override fun remove_beat(beat_index: Int, count: Int) {
        this.lock_ui_partial {
            val original_beat_count = this.length
            super.remove_beat(beat_index, count)

            val x = min(beat_index + count - 1, original_beat_count - 1) - (count - 1)
            for (i in 0 until count) {
                // this.get_editor_table().remove_mapped_column(x)
                this.ui_facade.queue_remove_column(x)
            }

        }
    }

    override fun insert_beats(beat_index: Int, count: Int) {
        this.lock_ui_partial {
            super.insert_beats(beat_index, count)
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<ReducibleTree<OpusEvent>>?) {
        this.lock_ui_partial {
            super.insert_beat(beat_index, beats_in_column)
            this.ui_facade.queue_add_column(beat_index, this.is_beat_tagged(beat_index))
        }
    }

    fun all_global_controllers_visible(): Boolean {
        for ((type, _) in OpusLayerInterface.global_controller_domain) {
            if (!this.has_global_controller(type) || !this.get_controller<EffectEvent>(type).visible) return false
        }
        return true
    }

    // private fun _pre_remove_channel(channel: Int): Triple<Int, Int, List<Int>> {
    //     val y = try {
    //         this.get_instrument_line_index(channel, 0)
    //     } catch (_: IndexOutOfBoundsException) {
    //         this.get_total_line_count()
    //     }

    //     val ctl_row = this.get_visible_row_from_ctl_line(this.get_actual_line_index(y))!!
    //     val channels = this.get_all_channels()
    //     var removed_row_count = channels[channel].size

    //     // NOTE: Accessing this.channels instead of this.get_all_channels since it's not possible to remove percussion channel
    //     for ((_, controller) in channels[channel].controllers.get_all()) {
    //         if (controller.visible) {
    //             removed_row_count += 1
    //         }
    //     }

    //     for (j in 0 until channels[channel].lines.size) {
    //         val line = channels[channel].lines[j]
    //         for ((_, controller) in line.controllers.get_all()) {
    //             if (controller.visible) {
    //                 removed_row_count += 1
    //             }
    //         }
    //     }

    //     val changed_columns = this.get_editor_table().remove_mapped_lines(ctl_row, removed_row_count)

    //     return Triple(ctl_row, removed_row_count, changed_columns)
    // }

    override fun remove_channel(channel: Int) {
        if (!this.ui_facade.is_full_locked()) {
            this.lock_ui_partial {
                //val (ctl_row, removed_row_count, changed_columns) = this._pre_remove_channel(channel)
                super.remove_channel(channel)
                // this.get_activity()?.shift_down_percussion_names(channel)
                this.ui_facade.queue_remove_channel(channel)
                this._activity?.update_channel_instruments()
            }
        } else {
            super.remove_channel(channel)

            this.ui_facade.shift_down_percussion_names(channel)
            this._activity?.update_channel_instruments()
        }
    }

    override fun on_project_changed() {
        super.on_project_changed()
        // this.get_activity()?.update_channel_instruments()

        this.recache_line_maps()
        this.ui_full_refresh()
        this.latest_set_octave = null
        this.latest_set_offset = null
        this.initialized = true
    }

    fun ui_full_refresh() {
        this.ui_facade.clear()
        this.ui_facade.set_project_name(this.project_name)
        this.ui_facade.beat_count = this.length
        var i = 0
        for ((c, channel) in this.channels.enumerate()) {
            this.ui_facade.queue_add_channel(c, this.is_percussion(c), channel.get_instrument())
            for ((l, line) in channel.lines.enumerate()) {
                val instrument = if (this.is_percussion(c)) {
                    (line as OpusLinePercussion).instrument
                } else {
                    null
                }
                this.ui_facade.queue_new_row(
                    i++,
                    MutableList(this.length) { line.beats[it].copy() },
                    UIChangeBill.LineData(c, l, null, instrument)
                )
                for ((type, controller) in line.controllers.get_all()) {
                    this.ui_facade.queue_new_row(
                        i++,
                        MutableList(this.length) { controller.beats[it].copy() },
                        UIChangeBill.LineData(c, l, type, null)
                    )
                }
            }
            for ((type, controller) in channel.controllers.get_all()) {
                this.ui_facade.queue_new_row(
                    i++,
                    MutableList(this.length) { controller.beats[it].copy() },
                    UIChangeBill.LineData(c, null, type, null)
                )
            }
        }
        for ((type, controller) in this.controllers.get_all()) {
            this.ui_facade.queue_new_row(
                i++,
                MutableList(this.length) { controller.beats[it].copy() },
                UIChangeBill.LineData(null, null, type, null)
            )
        }
        for (x in 0 until this.length) {
            this.ui_facade.queue_add_column(x, this.is_beat_tagged(x))
        }
    }

    override fun project_change_wrapper(callback: () -> Unit)  {
        this.lock_ui_full {
            // this.get_activity()?.active_percussion_names?.clear()

            this._ui_clear()
            super.project_change_wrapper(callback)
        }
    }
    override fun project_refresh() {
        this.lock_ui_full {
            // this.get_editor_table().clear()
            this._ui_clear()
            super.project_refresh()
        }
    }

    // This function is called from the Base Layer within th project_change_wrapper.
    // It's implicitly wrapped in a lock_ui_full call
    override fun _project_change_new() {
        super._project_change_new()

        //     // Need to prematurely update the channel instrument to find the lowest possible instrument
        //     activity.update_channel_instruments(c)
        //     activity.populate_active_percussion_names(c, true)
        //     val percussion_keys = activity.active_percussion_names[c]?.keys?.sorted() ?: continue
        //     for (l in 0 until this.get_channel(c).size) {
        //         this.percussion_set_instrument(c, l, max(0, percussion_keys.first() - 27))
        //     }
        // }

        // activity.active_project = null
    }

    // This function is called from the Base Layer within th project_change_wrapper.
    // It's implicitly wrapped in a lock_ui_full call
    override fun _project_change_midi(midi: Midi) {
        super._project_change_midi(midi)
        this.get_activity()?.active_project = null
    }


    override fun <T: EffectEvent> controller_global_set_initial_event(type: EffectType, event: T) {
        this.lock_ui_partial {
            super.controller_global_set_initial_event(type, event)
        }
    }

    override fun <T: EffectEvent> controller_channel_set_initial_event(type: EffectType, channel: Int, event: T) {
        this.lock_ui_partial {
            super.controller_channel_set_initial_event(type, channel, event)
        }
    }

    override fun <T: EffectEvent> controller_line_set_initial_event(type: EffectType, channel: Int, line_offset: Int, event: T) {
        this.lock_ui_partial {
            super.controller_line_set_initial_event(type, channel, line_offset, event)
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
            this.ui_facade.queue_config_drawer_redraw_export_button()
        }
    }

    override fun set_tuning_map(new_map: Array<Pair<Int, Int>>, mod_events: Boolean) {
        this.lock_ui_partial {
            val was_tuning_standard = this.is_tuning_standard()
            val original_map = this.tuning_map

            super.set_tuning_map(new_map, mod_events)

            val is_tuning_standard = this.is_tuning_standard()
           //  if (was_tuning_standard != is_tuning_standard) {
           //      this.run_on_ui_thread {
           //          this.get_activity()?.let { activity ->
           //              if (!is_tuning_standard) {
           //                  activity.set_active_midi_device(null)
           //              }
           //              activity.update_menu_options()
           //          }
           //      }
           //  }

            this.ui_facade.queue_config_drawer_redraw_export_button()

            if (new_map.size != original_map.size && mod_events) {
                for (i in 0 until this.channels.size) {
                    for (j in 0 until this.channels[i].lines.size) {
                        for (k in 0 until this.length) {
                            val beat_key = BeatKey(i, j, k)
                            val tree = this.get_tree(beat_key)
                            if (tree.is_eventless()) continue
                            this._queue_cell_change(beat_key)
                        }
                    }

                }
            }

            this.latest_set_offset = null
        }
    }

    override fun to_json(): JSONHashMap {
        val output = super.to_json()
        //val activity = this.get_activity() ?: return output
        //if (activity.configuration.soundfont != null) {
        //    output.get_hashmap("d")["sf"] = activity.configuration.soundfont
        //}
        return output
    }

    override fun _project_change_json(json_data: JSONHashMap) {
        super._project_change_json(json_data)
    }

    override fun move_channel(channel_index: Int, new_channel_index: Int) {
        this.lock_ui_partial {
            super.move_channel(channel_index, new_channel_index)
            this.ui_facade.move_channel(channel_index, new_channel_index)
            this._activity?.update_channel_instruments()
        }
    }

    override fun clear() {
        super.clear()
        this.ui_facade.clear()
        this.latest_set_offset = null
        this.latest_set_octave = null

        val editor_table = this.get_editor_table()
        editor_table.clear()
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this.lock_ui_partial {
            super.set_duration(beat_key, position, duration)

            // Needs to be set to trigger potentially queued cell changes from on_overlap()
            this._queue_cell_change(beat_key)
        }
    }

    override fun channel_set_instrument(channel: Int, instrument: Pair<Int, Int>) {
        this.lock_ui_partial {
            super.channel_set_instrument(channel, instrument)
            if (!this.ui_facade.is_full_locked()) {
                // Updating channel instruments doesn't strictly need to be gated behind the full lock,
                // BUT this way these don't get called multiple times every setup
                val activity = this.get_activity()
                activity?.update_channel_instrument(
                    this.get_midi_channel(channel),
                    instrument
                )

                this.ui_facade.set_channel_data(channel, this.is_percussion(channel), instrument)
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
        if (this.project_changing) return
        this._set_temporary_blocker(blocker_key, blocker_position)
    }

    override fun on_action_blocked_line_ctl(type: EffectType, blocker_key: BeatKey, blocker_position: List<Int>) {
        super.on_action_blocked_line_ctl(type, blocker_key, blocker_position)
        if (this.project_changing) return
        this._set_temporary_blocker_line_ctl(type, blocker_key, blocker_position)
    }

    override fun on_action_blocked_channel_ctl(type: EffectType, blocker_channel: Int, blocker_beat: Int, blocker_position: List<Int>) {
        super.on_action_blocked_channel_ctl(type, blocker_channel, blocker_beat, blocker_position)
        if (this.project_changing) return
        this._set_temporary_blocker_channel_ctl(type, blocker_channel, blocker_beat, blocker_position)
    }

    override fun on_action_blocked_global_ctl(type: EffectType, blocker_beat: Int, blocker_position: List<Int>) {
        super.on_action_blocked_global_ctl(type, blocker_beat, blocker_position)
        if (this.project_changing) return
        this._set_temporary_blocker_global_ctl(type, blocker_beat, blocker_position)
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
        if (!force && this._block_cursor_selection()) return

        this.lock_ui_partial {
            super.cursor_apply(cursor, force)

            this._queue_cursor_update(this.cursor)

            when (cursor.mode) {
                CursorMode.Single -> {
                    if (cursor.ctl_level == null && !this.is_percussion(cursor.channel)) {
                        /*
                            Need to set relative mode here since cursor_apply is called after history is applied
                            and set_relative_mode isn't called in replace_tree
                        */
                        val event = this.get_tree().get_event()
                        if (event is TunedInstrumentEvent) {
                            this.set_relative_mode(event)
                        }
                    }
                }
                CursorMode.Line,
                CursorMode.Column,
                CursorMode.Range,
                CursorMode.Unset,
                CursorMode.Channel -> { }
            }
        }
    }

    override fun cursor_clear() {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            super.cursor_clear()
            this._unset_temporary_blocker()
            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select_line(channel: Int, line_offset: Int) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            super.cursor_select_line(channel, line_offset)
            this.temporary_blocker = null

            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select_channel(channel: Int) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            super.cursor_select_channel(channel)
            this.temporary_blocker = null

            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select_channel_ctl_line(ctl_type: EffectType, channel: Int) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            super.cursor_select_channel_ctl_line(ctl_type, channel)
            this.temporary_blocker = null

            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select_line_ctl_line(ctl_type: EffectType, channel: Int, line_offset: Int) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            super.cursor_select_line_ctl_line(ctl_type, channel, line_offset)
            this.temporary_blocker = null

            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select_global_ctl_line(ctl_type: EffectType) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            super.cursor_select_global_ctl_line(ctl_type)
            this.temporary_blocker = null

            this._queue_cursor_update(this.cursor)
        }
    }

    fun force_cursor_select_column(beat: Int) {
        if (this.cursor.mode != CursorMode.Unset) {
            this.cursor_clear()
        }
        this.cursor_select_column(beat)
    }

    override fun cursor_select_column(beat: Int) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            super.cursor_select_column(beat)
            this.temporary_blocker = null
            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select(beat_key, position)

            val current_tree = this.get_tree()
            if (!this.is_percussion(beat_key.channel) && current_tree.has_event()) {
                this.set_relative_mode(current_tree.get_event()!! as TunedInstrumentEvent)
            }

            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select_ctl_at_line(ctl_type: EffectType, beat_key: BeatKey, position: List<Int>) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_ctl_at_line(ctl_type, beat_key, position)

            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select_ctl_at_channel(ctl_type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_ctl_at_channel(ctl_type, channel, beat, position)
            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select_ctl_at_global(ctl_type: EffectType, beat: Int, position: List<Int>) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_ctl_at_global(ctl_type, beat, position)

            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select_global_ctl_range(type: EffectType, first: Int, second: Int) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_global_ctl_range(type, first, second)

            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select_channel_ctl_range(type: EffectType, channel: Int, first: Int, second: Int) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_channel_ctl_range(type, channel, first, second)
            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select_line_ctl_range(type: EffectType, beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_line_ctl_range(type, beat_key_a, beat_key_b)

            this._queue_cursor_update(this.cursor)
        }
    }

    override fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) return

        this.lock_ui_partial {
            this._unset_temporary_blocker()
            super.cursor_select_range(beat_key_a, beat_key_b)
            this._queue_cursor_update(this.cursor)
        }
    }

    // CURSOR FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //------------------------------------------------------------------------
    private fun _set_temporary_blocker(beat_key: BeatKey, position: List<Int>) {
        //this.get_activity()?.vibrate()
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
        //this.get_activity()?.vibrate()
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
        // this.get_activity()?.vibrate()
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
         // this.get_activity()?.vibrate()
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
                    this._update_facade_blocker_leaf(BeatKey(channel, line_offset, blocker.first), blocker.second)
                }
                line.overlap_removed_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                    this._clear_facade_blocker_leaf()
                }
                for ((type, controller) in line.controllers.get_all()) {
                    controller.overlap_removed_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                        this._update_facade_blocker_leaf_line_ctl(type, BeatKey(channel, line_offset, blocker.first), blocker.second)
                    }
                    controller.overlap_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                        this._clear_facade_blocker_leaf()
                    }
                }
            }
            for ((type, controller) in channels[channel].controllers.get_all()) {
                controller.overlap_removed_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                    this._update_facade_blocker_leaf_channel_ctl(type, channel, blocker.first, blocker.second)
                }
                controller.overlap_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                    this._clear_facade_blocker_leaf()
                }
            }
        }

        for ((type, controller) in this.controllers.get_all()) {
            controller.overlap_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                this._update_facade_blocker_leaf_global_ctl(type, blocker.first, blocker.second)
            }
            controller.overlap_removed_callback = { blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>> ->
                this._clear_facade_blocker_leaf()
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

        this.ui_facade.queue_force_scroll(y ?: -1, beat ?: -1, offset, offset_width, this._activity?.in_playback() == true)
    }

    private fun _queue_cursor_update(cursor: OpusManagerCursor) {
        when (cursor.mode) {
            CursorMode.Single -> {
                val y = try {
                    when (cursor.ctl_level) {
                        null -> this.get_visible_row_from_ctl_line(this.get_actual_line_index(this.get_instrument_line_index(cursor.channel, cursor.line_offset)))
                        CtlLineLevel.Line -> this.get_visible_row_from_ctl_line_line(cursor.ctl_type!!, cursor.channel, cursor.line_offset)
                        CtlLineLevel.Channel -> this.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, cursor.channel)
                        CtlLineLevel.Global -> this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                    }
                } catch (_: IndexOutOfBoundsException) {
                    null
                } ?: return

                this.ui_facade.set_cursor(UIChangeBill.CacheCursor(cursor.mode, y, cursor.beat, *(cursor.get_position().toIntArray())))
            }

            CursorMode.Range -> {
                val (top_left, bottom_right) = try {
                    when (cursor.ctl_level) {
                        null -> {
                            val (top_left, bottom_right) = cursor.get_ordered_range()!!
                            Pair(
                                Pair(
                                    this.get_visible_row_from_ctl_line(
                                        this.get_actual_line_index(
                                            this.get_instrument_line_index(
                                                top_left.channel, top_left.line_offset
                                            )
                                        )
                                    ) ?: return,
                                    top_left.beat
                                ),
                                Pair(
                                    this.get_visible_row_from_ctl_line(
                                        this.get_actual_line_index(
                                            this.get_instrument_line_index(
                                                bottom_right.channel, bottom_right.line_offset
                                            )
                                        )
                                    ) ?: return,
                                    bottom_right.beat
                                )
                            )
                        }

                        else -> {
                            val (top_left, bottom_right) = cursor.get_ordered_range()!!
                            val y = when (cursor.ctl_level!!) {
                                // Can assume top_left.channel == bottom_right.channel and top_left.line_offset == bottom_right.line_offset
                                CtlLineLevel.Line -> this.get_visible_row_from_ctl_line_line(cursor.ctl_type!!, top_left.channel, top_left.line_offset)
                                CtlLineLevel.Channel -> this.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, top_left.channel)
                                CtlLineLevel.Global -> this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                            }

                            Pair(
                                Pair(y, min(top_left.beat, bottom_right.beat)),
                                Pair(y, max(top_left.beat, bottom_right.beat))
                            )
                        }
                    }
                } catch (_: IndexOutOfBoundsException) {
                    return
                }

                this.ui_facade.set_cursor(UIChangeBill.CacheCursor(cursor.mode, top_left.first, top_left.second, bottom_right.first, bottom_right.second))
            }

            CursorMode.Line -> {
                val y = try {
                    when (cursor.ctl_level) {
                        null -> this.get_visible_row_from_ctl_line(
                            this.get_actual_line_index(
                                this.get_instrument_line_index(cursor.channel, cursor.line_offset)
                            )
                        )
                        CtlLineLevel.Line -> this.get_visible_row_from_ctl_line_line(cursor.ctl_type!!, cursor.channel, cursor.line_offset)
                        CtlLineLevel.Channel -> this.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, cursor.channel)
                        CtlLineLevel.Global -> this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                    }
                } catch (_: IndexOutOfBoundsException) {
                    null
                } ?: return

                this.ui_facade.set_cursor(UIChangeBill.CacheCursor(cursor.mode, y))
            }

            CursorMode.Column -> {
                this.ui_facade.set_cursor(UIChangeBill.CacheCursor(cursor.mode, cursor.beat))
            }
            CursorMode.Channel -> {
                val y = when (cursor.ctl_level) {
                    null -> {
                        try {
                            this.get_visible_row_from_ctl_line(
                                this.get_actual_line_index(
                                    this.get_instrument_line_index(cursor.channel, 0)
                                )
                            ) ?: return
                        } catch (_: IndexOutOfBoundsException) {
                            return
                        }
                    }
                    else -> return // TODO: Throw Exception?
                }

                val channels = this.get_all_channels()
                var line_count = 0
                for (line in channels[cursor.channel].lines) {
                    line_count++
                    for ((_, controller) in line.controllers.get_all()) {
                        if (!controller.visible) continue
                        line_count++
                    }
                }

                for ((_, controller) in channels[cursor.channel].controllers.get_all()) {
                    if (!controller.visible) continue
                    line_count++
                }

                this.ui_facade.set_cursor(UIChangeBill.CacheCursor(cursor.mode, y, line_count))
            }
            CursorMode.Unset -> { }
        }
    }

    private fun _add_controller_to_column_width_map(y: Int, line: EffectController<*>, channel: Int?, line_offset: Int?, ctl_type: EffectType) {
        this.ui_facade.queue_new_row(
            y,
            MutableList(line.beats.size) { line.beats[it].copy() },
            UIChangeBill.LineData(channel, line_offset, ctl_type, null)
        )
    }

    private fun _update_after_new_line(channel: Int, line_offset: Int?) {
        if (this.ui_facade.is_full_locked()) return

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

        this.ui_facade.queue_new_row(
            visible_row,
            MutableList(new_line.beats.size) { new_line.beats[it].copy() },
            UIChangeBill.LineData(
                channel,
                line_offset,
                null,
                if (this.is_percussion(channel)) {
                    (working_channel.lines[adj_line_offset] as OpusLinePercussion).instrument
                } else {
                    null
                }
            )
        )

        val controllers = working_channel.lines[adj_line_offset].controllers.get_all()
        for (i in controllers.indices) {
            val (type, controller) = controllers[i]
            if (!controller.visible) continue
            this._add_controller_to_column_width_map(visible_row + i + 1, controller, channel, adj_line_offset, type)
        }
    }

    private fun _new_column_in_column_width_map(index: Int) {
        if (this.ui_facade.is_full_locked()) return

        val column = mutableListOf<Int>()
        for (channel in this.get_visible_channels()) {
            for (j in channel.lines.indices) {
                column.add(1)
                for ((_, controller) in channel.lines[j].controllers.get_all()) {
                    if (!controller.visible) continue
                    column.add(1)
                }
            }

            for ((_, controller) in channel.controllers.get_all()) {
                if (!controller.visible) continue
                column.add(1)
            }
        }
        for ((_, controller) in this.controllers.get_all()) {
            if (!controller.visible) continue
            column.add(1)
        }

       // this.get_editor_table().add_column_to_map(index, column)
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
        val current_tree_position = this.get_actual_position(beat_key, position)
        val current_tree = this.get_tree(current_tree_position.first, current_tree_position.second)
        val current_event = current_tree.get_event()
        val duration = current_event?.duration ?: 1
        val radix = this.tuning_map.size

        val new_event = when (this.relative_mode) {
            RelativeInputMode.Absolute -> {
                this.latest_set_octave = octave
                AbsoluteNoteEvent(
                    when (current_event) {
                        is AbsoluteNoteEvent -> {
                            val offset = current_event.note % radix
                            this.latest_set_offset = offset
                            (octave * radix) + offset
                        }
                        is RelativeNoteEvent -> {
                            this.convert_event_to_absolute(beat_key, position)
                            return this._set_note_octave(beat_key, position, octave)
                        }
                        else -> {
                            val cursor = this.cursor
                            val offset = when (this.get_activity()?.configuration?.note_memory) {
                                PaganConfiguration.NoteMemory.UserInput -> this.latest_set_offset
                                else -> null
                            }
                            this.latest_set_offset = (offset ?: ((this.get_absolute_value(cursor.get_beatkey(), cursor.get_position()) ?: 0) % radix))
                            (octave * radix) + this.latest_set_offset!!
                        }
                    },
                    duration
                )
            }
            RelativeInputMode.Positive -> {
                this.latest_set_offset = null
                this.latest_set_octave = null
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
                this.latest_set_offset = null
                this.latest_set_octave = null
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
                this.latest_set_offset = offset
                AbsoluteNoteEvent(
                    when (current_event) {
                        is AbsoluteNoteEvent -> {
                            val octave = (current_event.note / radix)
                            this.latest_set_octave = octave
                            (octave * radix) + offset
                        }
                        is RelativeNoteEvent -> {
                            this.convert_event_to_absolute(beat_key, position)
                            return this._set_note_offset(beat_key, position, offset)
                        }
                        else -> {
                            val cursor = this.cursor
                            val octave = when (this.get_activity()?.configuration?.note_memory) {
                                PaganConfiguration.NoteMemory.UserInput -> this.latest_set_octave
                                else -> null
                            }

                            this.latest_set_octave = (octave ?: ((this.get_absolute_value(cursor.get_beatkey(), cursor.get_position()) ?: 0) / radix))
                            offset + (this.latest_set_octave!! * radix)
                        }
                    },
                    duration
                )
            }
            RelativeInputMode.Positive -> {
                this.latest_set_offset = null
                this.latest_set_octave = null
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
                this.latest_set_offset = null
                this.latest_set_octave = null
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

    override fun tag_section(beat: Int, title: String?) {
        this.lock_ui_partial {
            super.tag_section(beat, title)
            this.ui_facade.queue_column_change(beat, true)
        }
    }

    override fun remove_tagged_section(beat: Int) {
        this.lock_ui_partial {
            super.remove_tagged_section(beat)
            this.ui_facade.queue_column_change(beat, false)
        }
    }

    fun is_initialized(): Boolean {
        return this.initialized
    }

}
