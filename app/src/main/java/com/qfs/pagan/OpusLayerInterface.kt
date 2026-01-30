package com.qfs.pagan
import com.qfs.json.JSONHashMap
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
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.opusmanager.cursor.IncorrectCursorMode
import com.qfs.pagan.structure.opusmanager.cursor.OpusManagerCursor
import com.qfs.pagan.structure.opusmanager.history.OpusLayerHistory
import com.qfs.pagan.structure.rationaltree.InvalidGetCall
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import com.qfs.pagan.uibill.UILock
import com.qfs.pagan.viewmodel.ViewModelEditorController
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.max
import kotlin.math.min

class OpusLayerInterface(val vm_controller: ViewModelEditorController) : OpusLayerHistory() {
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

        fun get_available_transitions(type: EffectType): Set<EffectTransition> {
            return when (type) {
                EffectType.Delay,
                EffectType.Tempo -> {
                    setOf(
                        EffectTransition.Instant,
                        EffectTransition.RInstant
                    )
                }

                EffectType.Pan,
                EffectType.Velocity,
                EffectType.Volume -> {
                    setOf(
                        EffectTransition.Instant,
                        EffectTransition.RInstant,
                        EffectTransition.Linear,
                        EffectTransition.RLinear,
                    )
                }

                EffectType.LowPass,
                EffectType.Reverb -> setOf(EffectTransition.Instant)
            }

        }
    }

    val ui_lock = UILock()

    // Refactored properties  //////////////////////
    var minimum_percussions = HashMap<Int, Int>()
    /////////////////////////////////////////////

    var temporary_blocker: OpusManagerCursor? = null

    var latest_set_octave: Int? = null
    var latest_set_offset: Int? = null
    lateinit var vm_state: ViewModelEditorState
    var note_memory: Boolean = true

    fun attach_state_model(model: ViewModelEditorState) {
        this.vm_state = model
    }
    // UI BILL Interface functions vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    private fun <T> lock_ui(callback: () -> T): T? {
        this.ui_lock.lock()

        val output = try {
            val tmp = callback()
            this.ui_lock.unlock()
            tmp
        } catch (e: BlockedActionException) {
            this.ui_lock.unlock()
            if (!this.ui_lock.is_locked()) {
                if (this.temporary_blocker != null) {
                    this.cursor_apply(this.temporary_blocker!!)
                }
                null
            } else { // Still Locked
                throw e
            }
        } catch (e: Exception) {
            this.ui_lock.unlock()
            throw e
        }

        return output
    }

    private fun _clear_facade_blocker_leaf() {
        if (this.ui_lock.is_locked()) return
        this.vm_state.blocker_leaf = null
    }

    private fun _update_facade_blocker_leaf_line_ctl(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        if (this.ui_lock.is_locked()) return
        this.vm_state.blocker_leaf = listOf<Int>(
            try {
                this.get_visible_row_from_ctl_line_line(type, beat_key.channel, beat_key.line_offset)
            } catch (_: IndexOutOfBoundsException) { // may reference a channel's line before the channel exists
                this.get_row_count()
            },
            beat_key.beat,
        ) + position
    }
    private fun _update_facade_blocker_leaf_channel_ctl(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        if (this.ui_lock.is_locked()) return
        this.vm_state.blocker_leaf = listOf<Int>(
            try {
                this.get_visible_row_from_ctl_line_channel(type, channel)
            } catch (_: IndexOutOfBoundsException) { // may reference a channel's line before the channel exists
                this.get_row_count()
            },
            beat,
        ) + position
    }
    private fun _update_facade_blocker_leaf_global_ctl(type: EffectType, beat: Int, position: List<Int>) {
        if (this.ui_lock.is_locked()) return
        this.vm_state.blocker_leaf = listOf<Int>(
            try {
                this.get_visible_row_from_ctl_line_global(type)
            } catch (_: IndexOutOfBoundsException) { // may reference a channel's line before the channel exists
                this.get_row_count()
            },
            beat,
        ) + position
    }

    private fun _update_facade_blocker_leaf(beat_key: BeatKey, position: List<Int>) {
        if (this.ui_lock.is_locked()) return
        this.vm_state.blocker_leaf = listOf<Int>(
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

    private fun _vm_state_remove_branch(beat_key: BeatKey, position: List<Int>) {
        if (this.ui_lock.is_locked()) return
        this.vm_state.remove_branch(
            Coordinate(
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
            position
        )
    }

    private fun _update_tree(beat_key: BeatKey, position: List<Int>) {
        if (this.ui_lock.is_locked()) return
        this.vm_state.update_tree(
            Coordinate(
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
            position,
            this.get_tree(beat_key, position)
        )
        this.vm_state.refresh_cursor()
    }

    // Global
    private fun _update_tree(type: EffectType, beat: Int, position: List<Int>,) {
        if (this.ui_lock.is_locked()) return

        val controller = this.get_controller<EffectEvent>(type)
        if (!controller.visible) return

        this.vm_state.update_tree(
            Coordinate(
                y = this.get_visible_row_from_ctl_line_global(type),
                x = beat
            ),
            position,
            controller.get_tree(beat, position)
        )
        this.vm_state.refresh_cursor()
    }

    // Channel
    private fun _update_tree(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        if (this.ui_lock.is_locked()) return

        val controller = this.get_all_channels()[channel].get_controller<EffectEvent>(type)
        if (!controller.visible) return

        this.vm_state.update_tree(
            Coordinate(
                y = this.get_visible_row_from_ctl_line_channel(type, channel),
                x = beat
            ),
            position,
            controller.get_tree(beat, position)
        )
        this.vm_state.refresh_cursor()
    }

    // Line
    private fun _update_tree(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        if (this.ui_lock.is_locked()) return
        val controller = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].get_controller<EffectEvent>(type)
        if (!controller.visible) return

        this.vm_state.update_tree(
            Coordinate(
                y = this.get_visible_row_from_ctl_line_line(type, beat_key.channel, beat_key.line_offset),
                x = beat_key.beat
            ),
            position,
            controller.get_tree(beat_key.beat, position)
        )
        this.vm_state.refresh_cursor()
    }
    // UI BILL Interface functions ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    var exception_catcher_depth = 0
    private fun <T> exception_catcher(callback: () -> T): T? {
        try {
            this.exception_catcher_depth += 1
            val output = callback()
            this.exception_catcher_depth -= 1
            return output
        } catch (e: Exception) {
            this.exception_catcher_depth -= 1
            if (this.exception_catcher_depth == 0) {
                this.exception_handler(e)
            } else {
                throw e
            }
            return null
        }
    }

    fun exception_handler(exception: Exception) {
        when (exception) {
            is BlockedActionException -> {
                this.temporary_blocker?.let {
                    val beat_key = it.get_beatkey()
                    val position = it.get_position()
                    val y = this.get_visible_row_from_pair(beat_key.channel, beat_key.line_offset)
                    try {
                        this.vm_state.cell_map[y][beat_key.beat].value.get_leaf(position).value.is_valid.value = false
                    } catch (e: ViewModelEditorState.TreeData.LeafNotFound) {
                        // pass
                    }
                    this.cursor_apply(it)
                }
            }
            else -> throw exception
        }
    }


    // BASE FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    override fun offset_range(amount: Int, first_key: BeatKey, second_key: BeatKey) {
        super.offset_range(amount, first_key, second_key)
    }

    override fun remove_global_controller(type: EffectType) {
        if (this.is_global_ctl_visible(type)) {
            val abs_line = this.get_visible_row_from_ctl_line_global(type)
            this.vm_state.remove_row(abs_line, 1)
        }
        super.remove_global_controller(type)
    }

    override fun remove_line_repeat(channel: Int, line_offset: Int, count: Int) {
        super.remove_line_repeat(channel, line_offset, count)
    }

    override fun remove_line_controller(type: EffectType, channel_index: Int, line_offset: Int) {
        if (this.is_line_ctl_visible(type, channel_index, line_offset )) {
            val abs_line = this.get_visible_row_from_ctl_line_line(type, channel_index, line_offset)
            this.vm_state.remove_row(abs_line, 1)
        }
        super.remove_line_controller(type, channel_index, line_offset)
    }

    override fun remove_channel_controller(type: EffectType, channel_index: Int) {
        if (this.is_channel_ctl_visible(type, channel_index)) {
            val abs_line = this.get_visible_row_from_ctl_line_channel(type, channel_index)
            this.vm_state.remove_row(abs_line, 1)
        }
        super.remove_channel_controller(type, channel_index)
    }

    override fun set_line_controller_visibility(type: EffectType, channel_index: Int, line_offset: Int, visibility: Boolean) {
        if (visibility) {
            super.set_line_controller_visibility(type, channel_index, line_offset, true)
            val visible_row = this.get_visible_row_from_ctl_line_line(type, channel_index, line_offset)
            val working_channel = this.get_channel(channel_index)
            val controller = working_channel.lines[line_offset].get_controller<EffectEvent>(type)
            this._add_controller_to_column_width_map(visible_row, controller, channel_index, line_offset, type)
        } else {
            val visible_row = this.get_visible_row_from_ctl_line_line(type, channel_index, line_offset)
            super.set_line_controller_visibility(type, channel_index, line_offset, false)
            this.vm_state.remove_row(visible_row, 1)
        }
        this.vm_state.refresh_cursor()
    }

    override fun set_channel_controller_visibility(type: EffectType, channel_index: Int, visibility: Boolean) {
        if (visibility) {
            super.set_channel_controller_visibility(type, channel_index, true)
            val visible_row = this.get_visible_row_from_ctl_line_channel(type, channel_index)
            val working_channel = this.get_channel(channel_index)
            val controller = working_channel.get_controller<EffectEvent>(type)
            this._add_controller_to_column_width_map(visible_row, controller, channel_index, null, type)
        } else {
            val visible_row = this.get_visible_row_from_ctl_line_channel(type, channel_index)
            super.set_channel_controller_visibility(type, channel_index, false)
            this.vm_state.remove_row(visible_row, 1)
        }
        this.vm_state.refresh_cursor()
    }

    override fun set_global_controller_visibility(type: EffectType, visibility: Boolean) {
        if (visibility) {
            super.set_global_controller_visibility(type, true)
            if (!this.ui_lock.is_locked()) {
                val visible_row = this.get_visible_row_from_ctl_line_global(type)
                val controller = this.get_controller<EffectEvent>(type)
                this._add_controller_to_column_width_map(visible_row, controller, null, null, type)
                val available_controllers = OpusLayerInterface.global_controller_domain.toMutableList()
                for ((type, controller) in this.controllers.get_all()) {
                    if (!controller.visible) continue
                    for ((i, value) in available_controllers.enumerate()) {
                        if (type == value.first) {
                            available_controllers.removeAt(i)
                            break
                        }
                    }
                }

                this.vm_state.has_global_effects_hidden.value = available_controllers.isNotEmpty()
                this.vm_state.refresh_cursor()
            }
        } else {
            val visible_row = this.get_visible_row_from_ctl_line_global(type)
            super.set_global_controller_visibility(type, false)

            if (!this.ui_lock.is_locked()) {
                this.vm_state.remove_row(visible_row, 1)
                this.vm_state.has_global_effects_hidden.value = true
                this.vm_state.refresh_cursor()
            }
        }
    }

    override fun set_project_name(new_name: String?) {
        super.set_project_name(new_name)
        this.vm_state.set_project_name(new_name)
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.track_blocked_leafs(beat_key, position) {
            super.unset(beat_key, position)
            this._update_tree(beat_key, position)
            this.check_update_active_event(beat_key, position)
        }
    }

    override fun controller_global_unset(type: EffectType, beat: Int, position: List<Int>) {
        this.track_blocked_leafs(type, beat, position) {
            super.controller_global_unset(type, beat, position)
            this._update_tree(type, beat, position)
            this.check_update_active_event(type, beat, position)
        }
    }

    override fun controller_channel_unset(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.track_blocked_leafs(type, channel, beat, position) {
            super.controller_channel_unset(type, channel, beat, position)
            this._update_tree(type, channel, beat, position)
            this.check_update_active_event(type, channel, beat, position)
        }
    }

    override fun controller_line_unset(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        this.track_blocked_leafs(type, beat_key, position) {
            super.controller_line_unset(type, beat_key, position)
            this._update_tree(type, beat_key, position)
            this.check_update_active_event(type, beat_key, position)
        }
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: ReducibleTree<out InstrumentEvent>) {
        this.exception_catcher {
            this.track_blocked_leafs(beat_key, position ?: listOf()) {
                super.replace_tree(beat_key, position, tree)
                this._update_tree(beat_key, position ?: listOf())
                this.check_update_active_event(beat_key, position ?: listOf())
            }
        }
    }

    override fun <T: EffectEvent> controller_global_replace_tree(type: EffectType, beat: Int, position: List<Int>?, tree: ReducibleTree<T>) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat, position ?: listOf()) {
                super.controller_global_replace_tree(type, beat, position, tree)
                this._update_tree(type, beat, position ?: listOf())
                this.check_update_active_event(type, beat, position ?: listOf())
            }
        }
    }

    override fun <T: EffectEvent> controller_channel_replace_tree(type: EffectType, channel: Int, beat: Int, position: List<Int>?, tree: ReducibleTree<T>) {
        this.exception_catcher {
            this.track_blocked_leafs(type, channel, beat, position ?: listOf()) {
                super.controller_channel_replace_tree(type, channel, beat, position, tree)
                this._update_tree(type, channel, beat, position ?: listOf())
                this.check_update_active_event(type, channel, beat, position ?: listOf())
            }
        }
    }

    override fun <T: EffectEvent> controller_line_replace_tree(type: EffectType, beat_key: BeatKey, position: List<Int>?, tree: ReducibleTree<T>) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat_key, position ?: listOf()) {
                super.controller_line_replace_tree(type, beat_key, position, tree)
                this._update_tree(type, beat_key, position ?: listOf())
                this.check_update_active_event(type, beat_key, position ?: listOf())
            }
        }
    }

    override fun <T: EffectEvent> controller_global_set_event(type: EffectType, beat: Int, position: List<Int>, event: T) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat, position) {
                super.controller_global_set_event(type, beat, position, event)
                this._update_tree(type, beat, position)

                this.check_update_active_event(type, beat, position)
            }
        }
    }

    override fun <T: EffectEvent> controller_channel_set_event(type: EffectType, channel: Int, beat: Int, position: List<Int>, event: T) {
        this.exception_catcher {
            this.track_blocked_leafs(type, channel, beat, position) {
                super.controller_channel_set_event(type, channel, beat, position, event)
                this._update_tree(type, channel, beat, position)
                this.check_update_active_event(type, channel, beat, position)
            }
        }
    }

    override fun <T: EffectEvent> controller_line_set_event(type: EffectType, beat_key: BeatKey, position: List<Int>, event: T) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat_key, position) {
                super.controller_line_set_event(type, beat_key, position, event)
                this._update_tree(type, beat_key, position)
                this.check_update_active_event(type, beat_key, position)
            }
        }
    }

    override fun <T: InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        this.exception_catcher {
            this.track_blocked_leafs(beat_key, position) {
                super.set_event(beat_key, position, event)

                if (this.ui_lock.is_locked()) return@track_blocked_leafs
                this._update_tree(beat_key, position)
                this.check_update_active_event(beat_key, position)
            }
        }
    }

    fun track_blocked_leafs(beat: Int, callback: () -> Unit) {
        val originals = mutableListOf<List<Pair<Pair<Int, List<Int>>, List<Pair<Int, List<Int>>>>>>()

        for (channel in this.channels) {
            for (line in channel.lines) {
                originals.add(line.get_blocked_leafs(beat, listOf()))
                for ((_, controller) in line.controllers.get_all()) {
                    if (!controller.visible) continue
                    originals.add(controller.get_blocked_leafs(beat, listOf()))
                }
            }

            for ((_, controller) in channel.controllers.get_all()) {
                if (!controller.visible) continue
                originals.add(controller.get_blocked_leafs(beat, listOf()))
            }
        }

        for ((_, controller) in this.controllers.get_all()) {
            if (!controller.visible) continue
            originals.add(controller.get_blocked_leafs(beat, listOf()))
        }

        val original_length = this.length
        callback()

        val newly_blocked_leafs = mutableListOf<List<Pair<Pair<Int, List<Int>>, List<Pair<Int, List<Int>>>>>>()
        for (channel in this.channels) {
            for (line in channel.lines) {
                newly_blocked_leafs.add(line.get_blocked_leafs(beat, listOf()))
                for ((_, controller) in line.controllers.get_all()) {
                    if (!controller.visible) continue
                    newly_blocked_leafs.add(controller.get_blocked_leafs(beat, listOf()))
                }
            }

            for ((_, controller) in channel.controllers.get_all()) {
                if (!controller.visible) continue
                newly_blocked_leafs.add(controller.get_blocked_leafs(beat, listOf()))
            }
        }

        for ((_, controller) in this.controllers.get_all()) {
            if (!controller.visible) continue
            newly_blocked_leafs.add(controller.get_blocked_leafs(beat, listOf()))
        }

        // adjust originals
        val diff = this.length - original_length
        for ((i, entries) in originals.enumerate()) {
            originals[i] = List(entries.size) { j ->
                val (head, tail) = entries[j]
                Pair(
                    head,
                    List(tail.size) { k ->
                        val section = tail[k]
                        if (section.first < beat) {
                            section
                        } else {
                            Pair(section.first + diff, section.second)
                        }
                    }
                )
            }
        }

        for ((i, original_blocked_leafs) in originals.enumerate()) {
            this.remap_blocked_leafs(
                i,
                original_blocked_leafs,
                newly_blocked_leafs[i]
            )
        }

    }

    private fun remap_blocked_leafs(y: Int, original_blocked_leafs: List<Pair<Pair<Int, List<Int>>, List<Pair<Int, List<Int>>>>>, newly_blocked_leafs: List<Pair<Pair<Int, List<Int>>, List<Pair<Int, List<Int>>>>>) {
        if (this.ui_lock.is_locked()) return
        for ((head, overlaps) in original_blocked_leafs) {
            for ((blocked_beat, blocked_position) in overlaps) {
                if (blocked_beat >= this.vm_state.cell_map[y].size) continue
                try {
                    this.vm_state.cell_map[y][blocked_beat].value.get_leaf(blocked_position).value.is_spillover.value = false
                } catch (_: ViewModelEditorState.TreeData.LeafNotFound) {
                    // Pass. It's ok if the cell no longer exists here
                } catch (_: InvalidGetCall) {
                    // Pass. It's ok if the cell no longer exists here
                }
            }
        }

        for ((head, overlaps) in newly_blocked_leafs) {
            for ((blocked_beat, blocked_position) in overlaps) {
                if (blocked_beat >= this.length) continue
                this.vm_state.cell_map[y][blocked_beat].value.get_leaf(blocked_position).value.is_spillover.value = true
            }
        }
    }

    private fun track_blocked_leafs(beat_key: BeatKey, position: List<Int>, callback: () -> Unit) {
        val line = this.channels[beat_key.channel].lines[beat_key.line_offset]
        val original_blocked_leafs = line.get_blocked_leafs(beat_key.beat, position)

        callback()

        this.remap_blocked_leafs(
            this.get_visible_row_from_pair(beat_key.channel, beat_key.line_offset),
            original_blocked_leafs,
            line.get_blocked_leafs(beat_key.beat, position)
        )
    }

    private fun track_blocked_leafs(type: EffectType, beat_key: BeatKey, position: List<Int>, callback: () -> Unit) {
        val line = this.channels[beat_key.channel].lines[beat_key.line_offset].controllers.get<EffectEvent>(type)
        val original_blocked_leafs = line.get_blocked_leafs(beat_key.beat, position)

        callback()

        if (!line.visible) return

        this.remap_blocked_leafs(
            this.get_visible_row_from_ctl_line_line(type, beat_key.channel, beat_key.line_offset),
            original_blocked_leafs,
            line.get_blocked_leafs(beat_key.beat, position)
        )
    }

    private fun track_blocked_leafs(type: EffectType, channel: Int, beat: Int, position: List<Int>, callback: () -> Unit) {
        val line = this.channels[channel].controllers.get<EffectEvent>(type)
        val original_blocked_leafs = line.get_blocked_leafs(beat, position)

        callback()

        if (!line.visible) return

        this.remap_blocked_leafs(
            this.get_visible_row_from_ctl_line_channel(type, channel),
            original_blocked_leafs,
            line.get_blocked_leafs(beat, position)
        )
    }

    private fun track_blocked_leafs(type: EffectType, beat: Int, position: List<Int>, callback: () -> Unit) {
        val line = this.controllers.get<EffectEvent>(type)
        val original_blocked_leafs = line.get_blocked_leafs(beat, position)

        callback()

        if (!line.visible) return

        this.remap_blocked_leafs(
            this.get_visible_row_from_ctl_line_global(type),
            original_blocked_leafs,
            line.get_blocked_leafs(beat, position)
        )
    }

    override fun percussion_set_event(beat_key: BeatKey, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(beat_key, position) {
                super.percussion_set_event(beat_key, position)
                this._update_tree(beat_key, position)
                this.check_update_active_event(beat_key, position)
            }
        }
    }

    override fun percussion_set_instrument(channel: Int, line_offset: Int, instrument: Int) {
        super.percussion_set_instrument(channel, line_offset, instrument)
        // Need to call get_drum name to repopulate instrument list if needed
        // this.get_activity()?.get_drum_name(channel, instrument)

        if (this.ui_lock.is_locked()) return
        val y = this.get_visible_row_from_ctl_line(
            this.get_actual_line_index(
                this.get_instrument_line_index(channel, line_offset)
            )
        )!!
        this.vm_state.line_data[y].assigned_offset.value = instrument
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.exception_catcher {
            this.track_blocked_leafs(beat_key, position) {
                super.split_tree(beat_key, position, splits, move_event_to_end)
                this._update_tree(beat_key, position)
            }
        }
    }

    override fun controller_global_split_tree(type: EffectType, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat, position) {
                super.controller_global_split_tree(type, beat, position, splits, move_event_to_end)
                this._update_tree(type, beat, position)
            }
        }
    }

    override fun controller_channel_split_tree(type: EffectType, channel: Int, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.exception_catcher {
            this.track_blocked_leafs(type, channel, beat, position) {
                super.controller_channel_split_tree(type, channel, beat, position, splits, move_event_to_end)
                this._update_tree(type, channel, beat, position)
            }
        }
    }

    override fun controller_line_split_tree(type: EffectType, beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat_key, position) {
                super.controller_line_split_tree(type, beat_key, position, splits, move_event_to_end)
                this._update_tree(type, beat_key, position)
            }
        }
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(beat_key, position) {
                super.insert_after(beat_key, position)
                this._update_tree(beat_key, position.subList(0, position.size - 1))
            }
        }
    }

    override fun controller_global_insert_after(type: EffectType, beat: Int, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat, position) {
                super.controller_global_insert_after(type, beat, position)
                this._update_tree(type, beat, position.subList(0, position.size - 1))
            }
        }
    }

    override fun controller_channel_insert_after(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat, channel, position) {
                super.controller_channel_insert_after(type, channel, beat, position)
                this._update_tree(type, channel, beat, position.subList(0, position.size - 1))
            }
        }
    }

    override fun controller_line_insert_after(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat_key, position) {
                super.controller_line_insert_after(type, beat_key, position)
                this._update_tree(type, beat_key, position.subList(0, position.size - 1))
            }
        }
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(beat_key, position) {
                super.insert(beat_key, position)
                this._update_tree(beat_key, position.subList(0, position.size - 1))
            }
        }
    }

    override fun controller_global_insert(type: EffectType, beat: Int, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat, position) {
                super.controller_global_insert(type, beat, position)
                this._update_tree(type, beat, position.subList(0, position.size - 1))
            }
        }
    }

    override fun controller_channel_insert(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat, channel, position) {
                super.controller_channel_insert(type, channel, beat, position)
                this._update_tree(type, channel, beat, position.subList(0, position.size - 1))
            }
        }
    }

    override fun controller_line_insert(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat_key, position) {
                super.controller_line_insert(type, beat_key, position)
                this._update_tree(type, beat_key, position.subList(0, position.size - 1))
            }
        }
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(beat_key, position) {
                super.remove(beat_key, position)
                this._update_tree(beat_key, position.subList(0, position.size - 1))
            }
        }
    }

    override fun controller_global_remove(type: EffectType, beat: Int, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat, position) {
                super.controller_global_remove(type, beat, position)
                this._update_tree(type, beat, position.subList(0, position.size - 1))
            }
        }
    }

    override fun controller_channel_remove(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(type, channel, beat, position) {
                super.controller_channel_remove(type, channel, beat, position)
                this._update_tree(type, channel, beat, position.subList(0, position.size - 1))
            }
        }
    }

    override fun controller_line_remove(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        this.exception_catcher {
            this.track_blocked_leafs(type, beat_key, position) {
                super.controller_line_remove(type, beat_key, position)
                this._update_tree(type, beat_key, position.subList(0, position.size - 1))
            }
        }
    }

    private fun get_minimum_percussion_instrument(channel: Int): Int {
        return this.minimum_percussions[channel] ?: 0
    }

    override fun new_line(channel: Int, line_offset: Int?) {
        super.new_line(channel, line_offset)

        // set the default instrument to the first available in the soundfont (if applicable)
        if (this.is_percussion(channel)) {
            (this.get_channel(channel) as OpusPercussionChannel).let {
                it.lines[line_offset ?: (it.size - 1)].instrument = max(0, this.get_minimum_percussion_instrument(channel) - 27)
            }
        }

        this._update_after_new_line(channel, line_offset)
        this.vm_state.refresh_cursor()
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusLineAbstract<*>) {
        super.insert_line(channel, line_offset, line)
        this._update_after_new_line(channel, line_offset)
        this.vm_state.refresh_cursor()
    }


    override fun swap_lines(channel_index_a: Int, line_offset_a: Int, channel_index_b: Int, line_offset_b: Int) {
        super.swap_lines(channel_index_a, line_offset_a, channel_index_b, line_offset_b)
        if (this.ui_lock.is_locked()) return

        this._swap_line_ui_update(channel_index_a, line_offset_a, channel_index_b, line_offset_b)
    }

    private fun get_vm_state_line_and_size(channel: Int, line_offset: Int): Pair<Int, Int> {
        var output_index = -1
        var output_size = 0

        for (y in 0 until this.vm_state.line_data.size) {
            val line_data = this.vm_state.line_data[y]
            if (line_data.channel.value != channel) continue
            if (line_data.line_offset.value != line_offset) continue

            if (output_index == -1) {
                output_index = y
            }

            output_size += 1
        }
        return Pair(output_index, output_size)
    }

    private fun _swap_line_ui_update(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        this.vm_state.channel_data.clear()
        this.vm_state.cell_map.clear()
        this.vm_state.line_data.clear()
        this.vm_state.line_count.value = 0
        this.vm_state.channel_count.value = 0
        var i = 0
        for ((c, channel) in this.channels.enumerate()) {
            this.vm_state.add_channel(c, this.is_percussion(c), channel.get_preset(), channel.muted)
            for ((l, line) in channel.lines.enumerate()) {
                val instrument = if (this.is_percussion(c)) {
                    (line as OpusLinePercussion).instrument
                } else {
                    null
                }
                this.vm_state.add_row(
                    i++,
                    line,
                    ViewModelEditorState.LineData(c, l, null, instrument, line.muted)
                )
                for ((type, controller) in line.controllers.get_all()) {
                    if (!controller.visible) continue
                    this.vm_state.add_row(
                        i++,
                        controller,
                        ViewModelEditorState.LineData(c, l, type, null, line.muted)
                    )
                }
            }
            for ((type, controller) in channel.controllers.get_all()) {
                if (!controller.visible) continue
                this.vm_state.add_row(
                    i++,
                    controller,
                    ViewModelEditorState.LineData(c, null, type, null, channel.muted)
                )
            }
        }
        for ((type, controller) in this.controllers.get_all()) {
            if (!controller.visible) continue
            this.vm_state.add_row(
                i++,
                controller,
                ViewModelEditorState.LineData(null, null, type, null, false)
            )
        }
        this.vm_state.refresh_cursor()
    }


    override fun remove_line(channel: Int, line_offset: Int): OpusLineAbstract<*> {
        val abs_line = this.get_visible_row_from_ctl_line(
            this.get_actual_line_index(
                this.get_instrument_line_index(channel, line_offset)
            )
        )!!

        val output = super.remove_line(channel, line_offset)

        if (!this.ui_lock.is_locked()) {
            var row_count = 1
            for ((_, controller) in output.controllers.get_all()) {
                if (!controller.visible) continue
                row_count += 1
            }

            this.vm_state.remove_row(abs_line, row_count)
            this.vm_state.shift_line_offsets_down(channel, line_offset)
            this.vm_state.refresh_cursor()
        }

        return output
    }


    /* Used to update the ui after new_channel and set_channel_visibility(n, true) */
    private fun _post_new_channel(channel: Int, lines: Int) {
        if (this.ui_lock.is_locked()) return

        val y = this.get_instrument_line_index(channel, 0)
        var ctl_row = this.get_visible_row_from_ctl_line(this.get_actual_line_index(y))!!


        val channels = this.get_all_channels()
        for (j in 0 until lines) {
            val line = channels[channel].lines[j]

            this.vm_state.add_row(
                ctl_row++,
                line,
                ViewModelEditorState.LineData(
                    channel,
                    j,
                    null,
                    if (this.is_percussion(channel)) {
                        (line as OpusLinePercussion).instrument
                    } else {
                        null
                    },
                    line.muted
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
        val notify_index = channel ?: this.channels.size
        super.new_channel(channel, lines, uuid, is_percussion)

        val preset = this.get_channel(notify_index).get_preset()

        this.vm_controller.update_channel_preset(
            this.get_midi_channel(notify_index),
            preset.first,
            preset.second
        )

        if (this.ui_lock.is_locked()) return

        this.vm_state.add_channel(notify_index, is_percussion, this.channels[notify_index].get_preset(), this.channels[notify_index].muted)
        this._post_new_channel(notify_index, lines)
        this.vm_state.refresh_cursor()
    }

    override fun remove_beat(beat_index: Int, count: Int) {
        this.exception_catcher {
            this.track_blocked_leafs(beat_index) {
                val original_beat_count = this.length
                super.remove_beat(beat_index, count)

                if (!this.ui_lock.is_locked()) {
                    val x = min(beat_index + count - 1, original_beat_count - 1) - (count - 1)
                    for (i in 0 until count) {
                        this.vm_state.remove_column(x)
                    }
                    this.vm_state.refresh_cursor()
                }
            }
        }
    }

    override fun insert_beats(beat_index: Int, count: Int) {
        super.insert_beats(beat_index, count)

        if (this.ui_lock.is_locked()) return
        this.vm_state.refresh_cursor()
    }

    override fun insert_beat(beat_index: Int) {
        this.track_blocked_leafs(beat_index) {
            super.insert_beat(beat_index)
            this.ui_add_column(beat_index)

            if (this.ui_lock.is_locked()) return@track_blocked_leafs
            this.vm_state.refresh_cursor()
        }
    }

    fun all_global_controllers_visible(): Boolean {
        for ((type, _) in OpusLayerInterface.global_controller_domain) {
            if (!this.has_global_controller(type) || !this.get_controller<EffectEvent>(type).visible) return false
        }
        return true
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
        if (!this.ui_lock.is_locked()) {
            this.vm_state.remove_channel(channel)
            this.vm_state.refresh_cursor()
        }
    }

    override fun on_project_changed() {
        super.on_project_changed()
        this.set_latest_octave()
        this.set_latest_offset()

        for ((i, channel) in this.channels.enumerate()) {
            val instrument = channel.get_preset()
            this.vm_controller.update_channel_preset(
                this.get_midi_channel(i),
                instrument.first,
                instrument.second
            )
        }
    }

    fun set_latest_octave(octave: Int? = null) {
        this.latest_set_octave = octave
        if (!this.ui_lock.is_locked()) {
            this.vm_state.highlighted_octave.value = octave
        }
    }

    fun set_latest_offset(offset: Int? = null) {
        this.latest_set_offset = offset
        if (this.ui_lock.is_locked()) return
        this.vm_state.highlighted_offset.value = offset
    }

    fun ui_full_refresh() {
        this.vm_state.clear()
        this.vm_state.set_project_name(this.project_name)
        this.vm_state.set_radix(this.get_radix())

        for (x in 0 until this.length) {
            this.vm_state.add_column(x, this.is_beat_tagged(x))
        }

        var i = 0
        for ((c, channel) in this.channels.enumerate()) {
            this.vm_state.add_channel(c, this.is_percussion(c), channel.get_preset(), channel.muted)
            for ((l, line) in channel.lines.enumerate()) {
                val instrument = if (this.is_percussion(c)) {
                    (line as OpusLinePercussion).instrument
                } else {
                    null
                }
                this.vm_state.add_row(
                    i++,
                    line,
                    ViewModelEditorState.LineData(c, l, null, instrument, line.muted)
                )
                for ((type, controller) in line.controllers.get_all()) {
                    if (!controller.visible) continue
                    this.vm_state.add_row(
                        i++,
                        controller,
                        ViewModelEditorState.LineData(c, l, type, null, line.muted)
                    )
                }
            }
            for ((type, controller) in channel.controllers.get_all()) {
                if (!controller.visible) continue
                this.vm_state.add_row(
                    i++,
                    controller,
                    ViewModelEditorState.LineData(c, null, type, null, channel.muted)
                )
            }
        }
        for ((type, controller) in this.controllers.get_all()) {
            if (!controller.visible) continue
            this.vm_state.add_row(
                i++,
                controller,
                ViewModelEditorState.LineData(null, null, type, null, false)
            )
        }
        this.vm_state.ready.value = true
    }

    private fun ui_add_column(beat_index: Int) {
        if (this.ui_lock.is_locked()) return
        val new_cells = mutableListOf<ReducibleTree<out OpusEvent>>()
        for ((c, channel) in this.channels.enumerate()) {
            for ((l, line) in channel.lines.enumerate()) {
                new_cells.add(this.get_tree(BeatKey(c, l, beat_index)))
                for ((type, controller) in line.controllers.get_all()) {
                    if (!controller.visible) continue
                    new_cells.add(this.get_line_ctl_tree(type, BeatKey(c, l, beat_index)))
                }
            }
            for ((type, controller) in channel.controllers.get_all()) {
                if (!controller.visible) continue
                new_cells.add(this.get_channel_ctl_tree(type, c, beat_index))
            }
        }
        for ((type, controller) in this.controllers.get_all()) {
            if (!controller.visible) continue
            new_cells.add(this.get_global_ctl_tree(type, beat_index))
        }

        this.vm_state.add_column(beat_index, this.is_beat_tagged(beat_index), new_cells)
    }

    override fun project_change_wrapper(callback: () -> Unit)  {
        this.lock_ui {
            // this.get_activity()?.active_percussion_names?.clear()
            this._ui_clear()
            super.project_change_wrapper(callback)
        }
        this.ui_full_refresh()
    }

    override fun project_refresh() {
        this.lock_ui {
            this._ui_clear()
            super.project_refresh()
        }
    }

    // This function is called from the Base Layer within th project_change_wrapper.
    // It's implicitly wrapped in a lock_ui call
    override fun _project_change_new() {
        super._project_change_new()
        this.vm_controller.active_project = null
        this.vm_controller.project_exists.value = false
    }

    override fun recache_line_maps() {
        super.recache_line_maps()
        this._set_overlap_callbacks()
    }

    override fun set_tuning_map(new_map: Array<Pair<Int, Int>>, mod_events: Boolean) {
        val was_tuning_standard = this.is_tuning_standard()
        super.set_tuning_map(new_map, mod_events)

        val is_tuning_standard = this.is_tuning_standard()
        if (was_tuning_standard && !is_tuning_standard) {
            this.vm_controller.set_active_midi_device(null)
        }

        this.set_latest_offset()
        if (this.ui_lock.is_locked()) return
        this.vm_state.set_radix(this.get_radix())
    }

    override fun to_json(): JSONHashMap {
        val output = super.to_json()

        this.vm_controller.active_soundfont_relative_path?.let {
            output.get_hashmap("d")["sf"] = it
        }

        return output
    }

    override fun _project_change_json(json_data: JSONHashMap) {
        super._project_change_json(json_data)

    }

    override fun move_channel(channel_index: Int, new_channel_index: Int) {
        super.move_channel(channel_index, new_channel_index)
        if (this.ui_lock.is_locked()) return
        this.vm_state.move_channel(channel_index, new_channel_index)
    }

    override fun move_line(channel_index_from: Int, line_offset_from: Int, channel_index_to: Int, line_offset_to: Int) {
        super.move_line(channel_index_from, line_offset_from, channel_index_to, line_offset_to)
        if (this.ui_lock.is_locked()) return
    }

    override fun clear() {
        super.clear()
        this.set_latest_offset()
        this.set_latest_octave()
        this.vm_state.clear()
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this.track_blocked_leafs(beat_key, position) {
            super.set_duration(beat_key, position, duration)
            if (this.ui_lock.is_locked()) return@track_blocked_leafs
            // Needs to be set to trigger potentially queued cell changes from on_overlap()
            //this._update_tree(beat_key, position.subList(0, position.size))
        }
    }

    override fun channel_set_preset(channel: Int, instrument: Pair<Int, Int>) {
        super.channel_set_preset(channel, instrument)

        this.vm_controller.update_channel_preset(
            this.get_midi_channel(channel),
            instrument.first,
            instrument.second
        )

        if (this.ui_lock.is_locked()) return
        this.vm_state.set_channel_data(channel, this.is_percussion(channel), instrument, this.channels[channel].muted, size = this.channels[channel].lines.size, )

    }

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
        super.apply_undo(repeat)
        this.recache_line_maps()
    }

    // HISTORY FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    // CURSOR FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    override fun cursor_apply(cursor: OpusManagerCursor, force: Boolean) {
        if (!force && this._block_cursor_selection()) return

        super.cursor_apply(cursor, force)
        this._queue_cursor_update(this.cursor)

        if (cursor.mode == CursorMode.Single && cursor.ctl_level == null && !this.is_percussion(cursor.channel)) {
            val event = this.get_tree()?.get_event()
            this.vm_state.relative_input_mode.value = when (event) {
                is RelativeNoteEvent -> {
                    if (event.offset < 0) {
                        RelativeInputMode.Negative
                    } else if (event.offset > 0) {
                        RelativeInputMode.Positive
                    } else {
                        return
                    }
                }
                is AbsoluteNoteEvent -> RelativeInputMode.Absolute
                else -> return
            }
        }
    }

    override fun cursor_clear() {
        if (this._block_cursor_selection()) return

        super.cursor_clear()
        this.clear_temporary_blocker()
        this._queue_cursor_update(this.cursor)

        this.vm_state.set_active_event(null)
    }

    override fun cursor_select_line(channel: Int, line_offset: Int) {
        if (this._block_cursor_selection()) return

        super.cursor_select_line(channel, line_offset)
        this.clear_temporary_blocker()
        this._queue_cursor_update(this.cursor)
        this.vm_state.set_active_event(
            this.get_line_controller_initial_event(
                EffectType.Volume,
                channel,
                line_offset,
                copy = true
            ),
            ViewModelEditorState.EventDescriptor.Selected
        )
    }

    override fun cursor_select_channel(channel: Int) {
        if (this._block_cursor_selection()) return

        super.cursor_select_channel(channel)
        this.clear_temporary_blocker()
        this._queue_cursor_update(this.cursor)
        this.vm_state.set_active_event(null)
    }

    override fun cursor_select_channel_ctl_line(ctl_type: EffectType, channel: Int) {
        if (this._block_cursor_selection()) return

        super.cursor_select_channel_ctl_line(ctl_type, channel)
        this.clear_temporary_blocker()

        this._queue_cursor_update(this.cursor)
        this.vm_state.set_active_event(
            this.get_channel_controller_initial_event(ctl_type, channel, copy = true),
            ViewModelEditorState.EventDescriptor.Selected
        )
    }

    override fun cursor_select_line_ctl_line(ctl_type: EffectType, channel: Int, line_offset: Int) {
        if (this._block_cursor_selection()) return

        super.cursor_select_line_ctl_line(ctl_type, channel, line_offset)
        this.clear_temporary_blocker()

        this._queue_cursor_update(this.cursor)
        this.vm_state.set_active_event(
            this.get_line_controller_initial_event(
                ctl_type,
                channel,
                line_offset,
                copy = true
            ),
            ViewModelEditorState.EventDescriptor.Selected
        )
    }

    override fun cursor_select_global_ctl_line(ctl_type: EffectType) {
        if (this._block_cursor_selection()) return

        super.cursor_select_global_ctl_line(ctl_type)
        this.clear_temporary_blocker()

        this._queue_cursor_update(this.cursor)
        this.vm_state.set_active_event(
            this.get_global_controller_initial_event(ctl_type, copy = true),
            ViewModelEditorState.EventDescriptor.Selected
        )
    }

    fun force_cursor_select_column(beat: Int) {
        if (this.cursor.mode != CursorMode.Unset) {
            this.cursor_clear()
        }
        this.cursor_select_column(beat)
    }

    override fun cursor_select_column(beat: Int) {
        if (this._block_cursor_selection()) return

        super.cursor_select_column(beat)
        this.clear_temporary_blocker()
        this._queue_cursor_update(this.cursor)
        this.vm_state.set_active_event(null)

        this.vm_state.scroll_to_beat(beat)
    }

    override fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        if (this._block_cursor_selection()) return

        this.clear_temporary_blocker()
        super.cursor_select(beat_key, position)


        val current_tree = this.get_tree() ?: return

        if (this.ui_lock.is_locked()) return

        this._queue_cursor_update(this.cursor)
        this.check_update_active_event(beat_key, position)

        // UI May still nee to be updated.
        if (this.vm_state.beat_count.value > beat_key.beat) {
            //
            var tree = this.get_tree(beat_key)
            val width = Rational(1, 1)
            var offset = Rational(0, 1)
            for (p in position) {
                width.denominator *= tree.size
                offset += Rational(p, width.denominator)
                tree = tree[p]
            }
            //

            this.vm_state.scroll_to_leaf(beat_key.beat, offset, width)

            current_tree.event?.let { event ->
                this.vm_state.relative_input_mode.value = when (event) {
                    is RelativeNoteEvent -> {
                        if (event.offset < 0) {
                            RelativeInputMode.Negative
                        } else if (event.offset > 0) {
                            RelativeInputMode.Positive
                        } else {
                            return
                        }
                    }

                    is AbsoluteNoteEvent -> RelativeInputMode.Absolute
                    else -> return
                }
            }

        }
    }

    override fun cursor_select_ctl_at_line(ctl_type: EffectType, beat_key: BeatKey, position: List<Int>) {
        if (this._block_cursor_selection()) return

        this.clear_temporary_blocker()
        super.cursor_select_ctl_at_line(ctl_type, beat_key, position)

        if (this.ui_lock.is_locked()) return
        this._queue_cursor_update(this.cursor)
        this.check_update_active_event(ctl_type, beat_key, position)
    }

    override fun cursor_select_ctl_at_channel(ctl_type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        if (this._block_cursor_selection()) return

        this.clear_temporary_blocker()
        super.cursor_select_ctl_at_channel(ctl_type, channel, beat, position)

        if (this.ui_lock.is_locked()) return
        this._queue_cursor_update(this.cursor)
        this.check_update_active_event(ctl_type, channel, beat, position)
    }

    override fun cursor_select_ctl_at_global(ctl_type: EffectType, beat: Int, position: List<Int>) {
        if (this._block_cursor_selection()) return

        this.clear_temporary_blocker()
        super.cursor_select_ctl_at_global(ctl_type, beat, position)

        if (this.ui_lock.is_locked()) return
        this._queue_cursor_update(this.cursor)
        this.check_update_active_event(ctl_type, beat, position)
    }

    fun cursor_select_global_ctl_range_next(type: EffectType, beat: Int) {
        if (this.cursor.mode == CursorMode.Range) {
            this.cursor_select_global_ctl_range(type, this.cursor.range!!.first.beat, beat)
        } else {
            this.cursor_select_global_ctl_range(type, beat, beat)
        }
        this.vm_state.set_active_event(null)
    }

    override fun cursor_select_global_ctl_range(type: EffectType, first: Int, second: Int) {
        if (this._block_cursor_selection()) return

        this.clear_temporary_blocker()
        super.cursor_select_global_ctl_range(type, first, second)

        this._queue_cursor_update(this.cursor)
        this.vm_state.set_active_event(null)
    }

    fun cursor_select_channel_ctl_range_next(type: EffectType, channel: Int, beat: Int) {
        if (this.cursor.mode == CursorMode.Range) {
            this.cursor_select_channel_ctl_range(type, channel, this.cursor.range!!.first.beat, beat)
        } else {
            this.cursor_select_channel_ctl_range(type, channel, beat, beat)
        }
        this.vm_state.set_active_event(null)
    }

    override fun cursor_select_channel_ctl_range(type: EffectType, channel: Int, first: Int, second: Int) {
        if (this._block_cursor_selection()) return

        this.clear_temporary_blocker()
        super.cursor_select_channel_ctl_range(type, channel, first, second)

        if (!this.ui_lock.is_locked()) {
            this._queue_cursor_update(this.cursor)
            this.vm_state.set_active_event(null)
        }
    }

    fun cursor_select_line_ctl_range_next(type: EffectType, beat_key: BeatKey) {
        if (this.cursor.mode == CursorMode.Range) {
            this.cursor_select_line_ctl_range(type, this.cursor.range!!.first, beat_key)
        } else {
            this.cursor_select_line_ctl_range(type, beat_key, beat_key)
        }
        if (!this.ui_lock.is_locked()) {
            this.vm_state.set_active_event(null)
        }
    }

    override fun cursor_select_line_ctl_range(type: EffectType, beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) return

        this.clear_temporary_blocker()
        super.cursor_select_line_ctl_range(type, beat_key_a, beat_key_b)

        if (!this.ui_lock.is_locked()) {
            this._queue_cursor_update(this.cursor)
            this.vm_state.set_active_event(null)
        }
    }

    fun cursor_select_range_next(beat_key: BeatKey) {
        if (this.cursor.mode == CursorMode.Range) {
            this.cursor_select_range(this.cursor.range!!.first, beat_key)
        } else {
            this.cursor_select_range(beat_key, beat_key)
        }

        if (!this.ui_lock.is_locked()) {
            this.vm_state.set_active_event(null)
        }
    }

    override fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) return

        this.clear_temporary_blocker()
        super.cursor_select_range(beat_key_a, beat_key_b)

        if (!this.ui_lock.is_locked()) {
            this._queue_cursor_update(this.cursor)
            this.vm_state.set_active_event(null)
        }
    }

    // CURSOR FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //------------------------------------------------------------------------
    private fun _set_temporary_blocker(beat_key: BeatKey, position: List<Int>) {
        //this.get_activity()?.vibrate()
        this.temporary_blocker = OpusManagerCursor(
            mode = CursorMode.Single,
            channel = beat_key.channel,
            line_offset = beat_key.line_offset,
            beat = beat_key.beat,
            position = position
        )
    }

    private fun _set_temporary_blocker_line_ctl(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        //this.get_activity()?.vibrate()
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

    private fun _set_temporary_blocker_channel_ctl(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        // this.get_activity()?.vibrate()
        this.temporary_blocker = OpusManagerCursor(
            mode = CursorMode.Single,
            ctl_type = type,
            ctl_level = CtlLineLevel.Channel,
            channel = channel,
            beat = beat,
            position = position
        )
    }

    private fun _set_temporary_blocker_global_ctl(type: EffectType, beat: Int, position: List<Int>) {
         // this.get_activity()?.vibrate()
        this.temporary_blocker = OpusManagerCursor(
            mode = CursorMode.Single,
            ctl_type = type,
            ctl_level = CtlLineLevel.Global,
            beat = beat,
            position = position
        )
    }

    private fun clear_temporary_blocker() {
        try {
            this.temporary_blocker?.let {
                val beat_key = it.get_beatkey()
                val position = it.get_position()
                val y = this.get_visible_row_from_pair(beat_key.channel, beat_key.line_offset)
                this.vm_state.cell_map[y][beat_key.beat].value.get_leaf(position).value.is_valid.value = true
            }
        } catch (e: Exception) {
            // Pass
        }

        this.temporary_blocker = null
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

    private fun _queue_cursor_update(cursor: OpusManagerCursor) {
        if (this.ui_lock.is_locked()) return

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

                this.vm_state.set_cursor(ViewModelEditorState.CacheCursor(cursor.mode, y, cursor.beat, *(cursor.get_position().toIntArray())))
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

                this.vm_state.set_cursor(ViewModelEditorState.CacheCursor(cursor.mode, top_left.first, top_left.second, bottom_right.first, bottom_right.second))
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

                this.vm_state.set_cursor(ViewModelEditorState.CacheCursor(cursor.mode, y))
            }

            CursorMode.Column -> {
                this.vm_state.set_cursor(ViewModelEditorState.CacheCursor(cursor.mode, cursor.beat))
            }
            CursorMode.Channel -> {
                this.vm_state.set_cursor(ViewModelEditorState.CacheCursor(cursor.mode, cursor.channel))
            }
            CursorMode.Unset -> {
                this.vm_state.set_cursor(null)
            }
        }
    }

    private fun _add_controller_to_column_width_map(y: Int, controller_line: EffectController<*>, channel_index: Int?, line_offset: Int?, ctl_type: EffectType) {
        if (this.ui_lock.is_locked()) return
        val channel = this.channels[channel_index ?: (this.channels.size - 1)]
        val line = channel.lines[line_offset ?: (channel.lines.size - 1)]
        this.vm_state.add_row(
            y,
            controller_line,
            ViewModelEditorState.LineData(channel_index, line_offset, ctl_type, null, line.muted)
        )
    }

    private fun _update_after_new_line(channel: Int, line_offset: Int?) {
        if (this.ui_lock.is_locked()) return

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

        this.vm_state.add_row(
            visible_row,
            new_line,
            ViewModelEditorState.LineData(
                channel,
                line_offset,
                null,
                if (this.is_percussion(channel)) {
                    (working_channel.lines[adj_line_offset] as OpusLinePercussion).instrument
                } else {
                    null
                },
                this.channels[channel].lines[adj_line_offset].muted
            )
        )

        val controllers = working_channel.lines[adj_line_offset].controllers.get_all()

        var i = 1
        for ((type, controller) in controllers) {
            if (!controller.visible) continue
            this._add_controller_to_column_width_map(visible_row + i++, controller, channel, adj_line_offset, type)
        }

        val y = this.get_visible_row_from_ctl_line(this.get_actual_line_index(this.get_instrument_line_index(channel, adj_line_offset))) ?: return
        this.vm_state.shift_line_offsets_up(channel, adj_line_offset, y)
    }

    private fun _new_column_in_column_width_map(index: Int) {
        if (this.ui_lock.is_locked()) return

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

    }


    // END UI FUNCS -----------------------

    private fun _ui_clear() {
        // this.run_on_ui_thread { main ->
        //     val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
        //     if (channel_recycler.adapter != null) {
        //         val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
        //         channel_adapter.clear()
        //     }
        // }
    }



    override fun _set_note_octave(beat_key: BeatKey, position: List<Int>, octave: Int, mode: RelativeInputMode, fallback_offset: Int) {
        super._set_note_octave(beat_key, position, octave, mode, fallback_offset)
        when (mode) {
            RelativeInputMode.Absolute -> this.set_latest_octave(octave)
            RelativeInputMode.Positive -> this.set_latest_octave()
            RelativeInputMode.Negative -> this.set_latest_octave()
        }
    }
    override fun _set_note_offset(beat_key: BeatKey, position: List<Int>, offset: Int, mode: RelativeInputMode, fallback_octave: Int) {
        super._set_note_offset(beat_key, position, offset, mode, fallback_octave)
        when (mode) {
            RelativeInputMode.Absolute -> this.set_latest_offset(offset)
            RelativeInputMode.Positive -> this.set_latest_offset()
            RelativeInputMode.Negative -> this.set_latest_offset()
        }
    }

    fun set_note_octave_at_cursor(octave: Int, mode: RelativeInputMode) {
        if (this.cursor.mode != CursorMode.Single) throw IncorrectCursorMode(this.cursor.mode, CursorMode.Single)

        val current_tree_position = this.get_actual_position(this.cursor.get_beatkey(), this.cursor.get_position())
        this._set_note_octave(current_tree_position.first, current_tree_position.second, octave, mode, this.latest_set_offset ?: 0)
    }

    fun set_note_offset_at_cursor(offset: Int, mode: RelativeInputMode) {
        if (this.cursor.mode != CursorMode.Single) throw IncorrectCursorMode(this.cursor.mode, CursorMode.Single)

        val current_tree_position = this.get_actual_position(this.cursor.get_beatkey(), this.cursor.get_position())
        this._set_note_offset(current_tree_position.first, current_tree_position.second, offset, mode, this.latest_set_octave ?: 0)
    }

    override fun move_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        super.move_beat_range(beat_key, first_corner, second_corner)
    }

    override fun tag_section(beat: Int, title: String?) {
        super.tag_section(beat, title)
        if (this.ui_lock.is_locked()) return
        this.vm_state.update_column(beat, true)
    }

    override fun remove_tagged_section(beat: Int) {
        super.remove_tagged_section(beat)
        if (this.ui_lock.is_locked()) return
        this.vm_state.update_column(beat, false)
    }


    override fun mute_channel(channel: Int) {
        super.mute_channel(channel)
        if (this.ui_lock.is_locked()) return
        this.vm_state.mute_channel(channel, true)
    }

    override fun unmute_channel(channel: Int) {
        super.unmute_channel(channel)
        if (this.ui_lock.is_locked()) return
        this.vm_state.mute_channel(channel, false)
    }

    override fun mute_line(channel: Int, line_offset: Int) {
        super.mute_line(channel, line_offset)

        if (this.ui_lock.is_locked()) return

        val y = this.get_visible_row_from_ctl_line(
            this.get_actual_line_index(
                this.get_instrument_line_index(channel, line_offset)
            )
        ) ?: return // TODO: Throw Exception
        this.vm_state.mute_line(y, true)
    }

    override fun unmute_line(channel: Int, line_offset: Int) {
        super.unmute_line(channel, line_offset)

        if (this.ui_lock.is_locked()) return

        val y = this.get_visible_row_from_ctl_line(
            this.get_actual_line_index(
                this.get_instrument_line_index(channel, line_offset)
            )
        ) ?: return // TODO: Throw Exception
        this.vm_state.mute_line(y, false)
    }

    fun get_safe_name(): String? {
        val reserved_chars = "|\\?*<\":>+[]/'"
        var base_name: String = this.project_name ?: return null
        for (c in reserved_chars) {
            base_name = base_name.replace("$c", "_")
        }
        return base_name
    }

    fun check_update_active_event(beat_key: BeatKey, position: List<Int>) {
        if (this.ui_lock.is_locked()) return
        if (this.cursor.mode != CursorMode.Single) return
        if (this.cursor.get_beatkey() != beat_key) return

        val cursor_position = this.cursor.get_position()
        val min_position_length = min(cursor_position.size, position.size)
        if (cursor_position.subList(0, min_position_length) != position.subList(0, min_position_length)) return

        val line = this.channels[beat_key.channel].lines[beat_key.line_offset]
        val this_event = this.get_tree(beat_key, position).get_event()

        var (working_event, descriptor) = if (this_event != null) {
            Pair(this_event, ViewModelEditorState.EventDescriptor.Selected)
        } else {
            val original = this.get_actual_position(beat_key, position)
            val head_tree = this.get_tree(original.first, original.second)
            if (original != Pair(beat_key, position) && head_tree.event != null) {
                Pair(head_tree.event!!, ViewModelEditorState.EventDescriptor.Tail)
            } else {
                var (working_beat, working_position) = line.get_preceding_event_position(beat_key.beat, position) ?: Pair(beat_key.beat, position)
                var working_event = line.get_tree(working_beat, working_position).event
                Pair(
                    working_event,
                    ViewModelEditorState.EventDescriptor.Backup
                )
            }
        }

        this.vm_state.set_active_event(working_event, descriptor)
    }

    fun check_update_active_event(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        if (this.ui_lock.is_locked()) return
        if (this.cursor.mode != CursorMode.Single) return
        if (this.cursor.ctl_type != type) return
        if (this.cursor.ctl_level != CtlLineLevel.Line) return
        if (this.cursor.get_beatkey() != beat_key) return

        val cursor_position = this.cursor.get_position()
        val min_position_length = min(cursor_position.size, position.size)
        if (cursor_position.subList(0, min_position_length) != position.subList(0, min_position_length)) return

        val controller = this.get_line_controller<EffectEvent>(type, beat_key.channel, beat_key.line_offset)
        val this_event = this.get_line_ctl_tree<EffectEvent>(type, beat_key, position).get_event()

        var (working_event, descriptor) = if (this_event != null) {
            Pair(this_event, ViewModelEditorState.EventDescriptor.Selected)
        } else {
            val original = this.controller_line_get_actual_position(type, beat_key, position)
            val head_tree = this.get_line_ctl_tree<EffectEvent>(type, original.first, original.second)
            if (original != Pair(beat_key, position) && head_tree.event != null) {
                Pair(head_tree.event!!, ViewModelEditorState.EventDescriptor.Tail)
            } else {
                var (working_beat, working_position) = controller.get_preceding_event_position(beat_key.beat, position) ?: Pair(beat_key.beat, position)
                var working_event = controller.get_tree(beat_key.beat, position).event
                while (working_event == null || !working_event.is_persistent()) {
                    controller.get_preceding_event_position(working_beat, working_position)?.let {
                        working_beat = it.first
                        working_position = it.second
                        working_event = controller.get_tree(it.first, it.second).get_event()
                    } ?: break
                }

                Pair(
                    working_event ?: controller.initial_event,
                    ViewModelEditorState.EventDescriptor.Backup
                )
            }
        }

        this.vm_state.set_active_event(working_event, descriptor)
    }

    fun check_update_active_event(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        if (this.cursor.mode != CursorMode.Single) return
        if (this.ui_lock.is_locked()) return
        if (this.cursor.ctl_type != type) return
        if (this.cursor.ctl_level != CtlLineLevel.Channel) return
        if (this.cursor.channel != channel) return
        if (this.cursor.beat != beat) return

        val cursor_position = this.cursor.get_position()
        val min_position_length = min(cursor_position.size, position.size)
        if (cursor_position.subList(0, min_position_length) != position.subList(0, min_position_length)) return

        val controller = this.get_channel_controller<EffectEvent>(type, channel)
        val this_event = this.get_channel_ctl_tree<EffectEvent>(type, channel, beat, position).get_event()

        val (working_event, descriptor) = if (this_event != null) {
            Pair(this_event, ViewModelEditorState.EventDescriptor.Selected)
        } else {
            val original = this.controller_channel_get_actual_position(type, channel, beat, position)
            val head_tree = this.get_channel_ctl_tree<EffectEvent>(type, channel, original.first, original.second)
            if (original != Pair(beat, position) && head_tree.event != null) {
                Pair(head_tree.event!!, ViewModelEditorState.EventDescriptor.Tail)
            } else {
                var (working_beat, working_position) = controller.get_preceding_event_position(beat, position) ?: Pair(beat, position)
                var working_event = controller.get_tree(beat, position).event
                while (working_event == null || !working_event.is_persistent()) {
                    controller.get_preceding_event_position(working_beat, working_position)?.let {
                        working_beat = it.first
                        working_position = it.second
                        working_event = controller.get_tree(it.first, it.second).get_event()
                    } ?: break
                }
                Pair(
                    working_event ?: controller.initial_event,
                    ViewModelEditorState.EventDescriptor.Backup
                )
            }
        }

        this.vm_state.set_active_event(working_event, descriptor)
    }

    fun check_update_active_event(type: EffectType, beat: Int, position: List<Int>) {
        if (this.cursor.mode != CursorMode.Single) return
        if (this.ui_lock.is_locked()) return
        if (this.cursor.ctl_type != type) return
        if (this.cursor.ctl_level != CtlLineLevel.Global) return
        if (this.cursor.beat != beat) return

        val cursor_position = this.cursor.get_position()
        val min_position_length = min(cursor_position.size, position.size)
        if (cursor_position.subList(0, min_position_length) != position.subList(0, min_position_length)) return

        val controller = this.get_global_controller<EffectEvent>(type)
        val this_event = this.get_global_ctl_tree<EffectEvent>(type, beat, position).get_event()

        val (working_event, descriptor) = if (this_event != null) {
            Pair(this_event, ViewModelEditorState.EventDescriptor.Selected)
        } else {
            val original = this.controller_global_get_actual_position(type, beat, position)
            val head_tree = this.get_global_ctl_tree<EffectEvent>(type, original.first, original.second)
            if (original != Pair(beat, position) && head_tree.event != null) {
                Pair(head_tree.event!!, ViewModelEditorState.EventDescriptor.Tail)
            } else {
                val original = this.controller_global_get_actual_position(type, beat, position)
                val head_tree = this.get_global_ctl_tree<EffectEvent>(type, original.first, original.second)
                if (original != Pair(beat, position) && head_tree.event != null) {
                    Pair(head_tree.event!!, ViewModelEditorState.EventDescriptor.Tail)
                } else {
                    var (working_beat, working_position) = controller.get_preceding_event_position(beat, position) ?: Pair(beat, position)
                    var working_event = controller.get_tree(beat, position).event
                    while (working_event == null || !working_event.is_persistent()) {
                        controller.get_preceding_event_position(working_beat, working_position)?.let {
                            working_beat = it.first
                            working_position = it.second
                            working_event = controller.get_tree(it.first, it.second).get_event()
                        } ?: break
                    }

                    Pair(
                        working_event ?: controller.initial_event,
                        ViewModelEditorState.EventDescriptor.Backup
                    )
                }
            }
        }

        this.vm_state.set_active_event(working_event, descriptor)
    }
}
