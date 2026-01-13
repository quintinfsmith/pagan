package com.qfs.pagan.viewmodel

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.pagan.EditorTable
import com.qfs.pagan.PlaybackState
import com.qfs.pagan.RelativeInputMode
import com.qfs.pagan.enumerate
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.ReducibleTreeArray
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class ViewModelEditorState: ViewModel() {
    enum class SelectionLevel {
        Unselected,
        Primary,
        Secondary
    }
    enum class EventDescriptor {
        Selected,
        Tail,
        Backup
    }

    class LineData(channel: Int?, line_offset: Int?, ctl_type: EffectType?, assigned_offset: Int? = null, is_mute: Boolean, is_selected: Boolean = false) {
        val channel = mutableStateOf(channel)
        val line_offset = mutableStateOf(line_offset)
        val ctl_type = mutableStateOf(ctl_type)
        val assigned_offset = mutableStateOf(assigned_offset)
        val is_mute = mutableStateOf(is_mute)
        val is_selected = mutableStateOf(is_selected)
        val is_dragging = mutableStateOf(false)
    }

    class ColumnData(is_tagged: Boolean, is_selected: Boolean = false) {
        val is_tagged = mutableStateOf(is_tagged)
        val is_selected = mutableStateOf(is_selected)
    }

    class LeafData(event: OpusEvent? = null, is_selected: Boolean = false, is_secondary: Boolean = false, is_valid: Boolean = true, is_spillover: Boolean = false, weight: Float = 1F) {
        val weight = mutableFloatStateOf(weight)
        val event = mutableStateOf(event?.copy())
        val is_selected = mutableStateOf(is_selected)
        val is_secondary = mutableStateOf(is_secondary)
        val is_valid = mutableStateOf(is_valid)
        val is_spillover = mutableStateOf(is_spillover)
    }

    class ChannelData(percussion: Boolean, instrument: Pair<Int, Int>, is_mute: Boolean, is_selected: Boolean = false, name: String?, size: Int = 0) {
        val percussion = mutableStateOf(percussion)
        val instrument = mutableStateOf(instrument)
        val is_mute = mutableStateOf(is_mute)
        val is_selected = mutableStateOf(is_selected)
        val active_name = mutableStateOf(name)
        val size = mutableIntStateOf(size)
        fun update(percussion: Boolean, instrument: Pair<Int, Int>, is_mute: Boolean, is_selected: Boolean = false, name: String?, size: Int = 0) {
            this.percussion.value = percussion
            this.instrument.value = instrument
            this.is_mute.value = is_mute
            this.is_selected.value = is_selected
            this.active_name.value = name
            this.size.intValue = size
        }
    }

    class TreeData(tree: ReducibleTree<out OpusEvent>) {
        class LeafNotFound(path: List<Int>): Exception("Leaf $path not found")
        var key: MutableState<Long> = mutableStateOf(System.currentTimeMillis())
        val top_weight: MutableState<Int> = mutableIntStateOf(tree.weighted_size)
        val leafs: MutableList<Pair<List<Int>, MutableState<LeafData>>> = mutableListOf()
        init {
            tree.weighted_traverse { subtree, event, path, weight ->
                if (subtree.is_leaf()) {
                    this.set_leaf(path, event, weight)
                }
            }
            this.sort_leafs()
        }

        fun get_leaf(path: List<Int>): MutableState<LeafData> {
            for ((leaf_path, leaf_data) in this.leafs) {
                if (leaf_path == path) return leaf_data
            }
            throw LeafNotFound(path)
        }

        fun set_leaf(path: List<Int>, event: OpusEvent?, weight: Float) {
            for (i in 0 until this.leafs.size) {
                if (this.leafs[i].first == path) {
                    this.leafs[i].second.value.event.value = event
                    this.leafs[i].second.value.weight.floatValue = weight
                    return
                }
            }

            this.leafs.add(Pair(path, mutableStateOf(LeafData(event = event, weight = weight))))
            this.key.value = System.currentTimeMillis()
        }

        fun sort_leafs() {
            this.leafs.sortWith { (path_a, _), (path_b, _) ->
                if (path_a == path_b) return@sortWith 0

                for (i in 0 until min(path_a.size, path_b.size)) {
                    if (path_a[i] < path_b[i]) {
                        return@sortWith -1
                    } else if (path_a[i] > path_b[i]) {
                        return@sortWith 1
                    }
                }

                if (path_a.size < path_b.size) {
                    -1
                } else {
                    1
                }
            }
            this.key.value = System.currentTimeMillis()
            // TODO
        }

        fun remove_branch(path: List<Int>) {
            var i = 0
            while (i < this.leafs.size) {
                if (this.leafs[i].first.size >= path.size && this.leafs[i].first.subList(0, path.size) == path) {
                    this.leafs.removeAt(i)
                } else {
                    i++
                }
            }
            this.key.value = System.currentTimeMillis()
        }

    }

    class CacheCursor(var type: CursorMode, vararg ints: Int) {
        var ints = ints.toList()
    }

    val ready = mutableStateOf(false)
    val project_name: MutableState<String?> = mutableStateOf(null)
    val beat_count: MutableState<Int> = mutableIntStateOf(0)
    val line_count: MutableState<Int> = mutableIntStateOf(0)
    var channel_count: MutableState<Int> = mutableIntStateOf(0)
    val line_data: MutableList<LineData> = mutableListOf()
    val column_data: MutableList<ColumnData> = mutableListOf()
    val cell_map = mutableListOf<MutableList<MutableState<TreeData>>>()
    val channel_data: MutableList<ChannelData> = mutableListOf()
    var radix: MutableState<Int> = mutableIntStateOf(12)

    var active_event: MutableState<OpusEvent?> = mutableStateOf(null)
    var active_event_descriptor: MutableState<EventDescriptor?> = mutableStateOf(null)
    var active_cursor: MutableState<CacheCursor?> = mutableStateOf(null)
    // selected_* are used to quickly unset is_selected when cursor is changed
    var selected_columns: MutableList<ColumnData> = mutableListOf()
    var selected_lines: MutableList<LineData> = mutableListOf()
    var selected_leafs: MutableList<LeafData> = mutableListOf()

    var blocker_leaf: List<Int>? = null
    val use_midi_playback: MutableState<Boolean> = mutableStateOf(false)
    val midi_device_connected: MutableState<Boolean> = mutableStateOf(false)
    val playback_state_soundfont: MutableState<PlaybackState> = mutableStateOf(PlaybackState.NotReady)
    val looping_playback: MutableState<Boolean> = mutableStateOf(false)
    val playback_state_midi: MutableState<PlaybackState> = mutableStateOf(PlaybackState.NotReady)
    val relative_input_mode: MutableState<RelativeInputMode> = mutableStateOf(RelativeInputMode.Absolute)

    val highlighted_offset: MutableState<Int?> = mutableStateOf(null)
    val highlighted_octave: MutableState<Int?> = mutableStateOf(null)

    val is_buffering: MutableState<Boolean> = mutableStateOf(false)

    val scroll_state_x = mutableStateOf(LazyListState())
    val scroll_state_y: MutableState<ScrollState> = mutableStateOf(ScrollState(0))
    val coroutine_scope: MutableState<CoroutineScope> = mutableStateOf(CoroutineScope(Dispatchers.Default))
    val export_progress: MutableState<Float> = mutableStateOf(0F)
    val export_in_progress = mutableStateOf(false)

    private val working_path = mutableListOf<Int>()
    val preset_names = HashMap<Int, HashMap<Int, String>>()
    val available_instruments = HashMap<Pair<Int, Int>, List<Pair<String, Int>>>()
    val base_leaf_width = mutableStateOf(0F)

    val has_global_effects_hidden = mutableStateOf(true)
    val soundfont_active = mutableStateOf(false)
    val table_side_padding = mutableStateOf(0)
    val wide_beat: MutableState<Int?> = mutableStateOf(null)
    val wide_beat_progress: MutableState<Float> = mutableStateOf(0F)

    val dragging_line: MutableState<Int?> = mutableStateOf(null)
    val dragging_abs_offset: MutableState<Float?> = mutableStateOf(null)
    val dragging_initial_offset: MutableState<Float> = mutableStateOf(0F)
    val dragging_first_line: MutableState<Int?> = mutableStateOf(null)
    val dragging_offset: MutableState<Float> = mutableStateOf(0F)
    val dragging_height: Pair<MutableState<Int>, MutableState<Int>> = Pair(mutableStateOf(0), mutableStateOf(0))
    val dragging_line_map = mutableListOf<Triple<ClosedFloatingPointRange<Float>, IntRange, Boolean>>()

    fun is_dragging_channel(): Boolean {
        val main_line_index = this.dragging_line.value ?: return false
        val main_line = this.line_data[main_line_index]
        return this.active_cursor.value?.type == CursorMode.Channel && this.active_cursor.value?.ints[0] == main_line.channel.value
    }

    fun update_line_map(line_map: List<Triple<ClosedFloatingPointRange<Float>, IntRange, Boolean>>) {
        this.dragging_line_map.clear()
        this.dragging_line_map += line_map
    }

    fun start_dragging(y: Int, initial_offset: Float) {
        this.dragging_line.value = y
        this.dragging_initial_offset.value = initial_offset
        this.dragging_height.first.value = 0
        this.dragging_height.second.value = 0
        val main_line_index = this.dragging_line.value ?: return
        val main_line = this.line_data[main_line_index]
        val is_dragging_channel = this.is_dragging_channel()

        for ((i, line) in this.line_data.enumerate()) {
            if (main_line.channel.value != line.channel.value) continue
            if (line.line_offset.value == main_line.line_offset.value || is_dragging_channel) {
                line.is_dragging.value = true
                if (dragging_first_line.value == null) {
                    dragging_first_line.value = i
                }
                if (line.ctl_type.value != null) {
                    dragging_height.second.value += 1
                } else {
                    dragging_height.first.value += 1
                }
            }
        }
    }

    fun stop_dragging() {
        this.dragging_abs_offset.value = null
        this.dragging_height.first.value = 0
        this.dragging_line_map.clear()
        this.dragging_height.second.value = 0
        this.dragging_line.value = null
        this.dragging_first_line.value = null
        this.dragging_offset.value = 0F
        this.dragging_initial_offset.value = 0F
        for (line in this.line_data) {
            line.is_dragging.value = false
        }
    }

    fun calculate_dragged_to_line(): Pair<Int, Boolean>? {
        val y = this.dragging_line.value ?: return null
        val dragged_offset = this.dragging_initial_offset.value + this.dragging_offset.value
        var adjusted_y = -1F
        val sorted_pairs = this.dragging_line_map.toList().sortedBy { it.first.start }

        if (y > sorted_pairs.last().second.last) {
            adjusted_y = sorted_pairs.last().first.start + dragged_offset
        } else {
            for ((range, line_range, _) in sorted_pairs) {
                if (line_range.contains(y)) {
                    adjusted_y = range.start + dragged_offset
                    break
                }
            }
        }

        for ((range, line_range, is_bottom) in sorted_pairs) {
            if (range.contains(adjusted_y)) {
                return Pair(
                    if (is_bottom) {
                        line_range.last
                    } else {
                        line_range.first
                    },
                    is_bottom
                )
            }
        }


        return null
    }

    fun clear() {
        this.ready.value = false
        this.project_name.value = null
        this.beat_count.value = 0
        this.active_event.value = null
        this.active_event_descriptor.value = null
        this.active_cursor.value = null
        this.radix.value = 12
        this.line_count.value = 0
        this.channel_count.value = 0
        this.blocker_leaf = null

        this.highlighted_octave.value = null
        this.highlighted_offset.value = null

        this.line_data.clear()
        this.column_data.clear()
        this.cell_map.clear()
        this.channel_data.clear()

        this.coroutine_scope.value.launch {
            this@ViewModelEditorState.scroll_state_x.value.requestScrollToItem(0)
            this@ViewModelEditorState.scroll_state_y.value.scrollTo(0)
        }

        // this.preset_names.clear()
        // this.available_instruments.clear()
    }

    fun set_use_midi_playback(value: Boolean) {
        this.use_midi_playback.value = value
    }

    fun update_cell(coordinate: EditorTable.Coordinate, tree: ReducibleTree<out OpusEvent>) {
        this.cell_map[coordinate.y][coordinate.x].value = TreeData(tree)
    }

    fun remove_branch(coordinate: EditorTable.Coordinate, position: List<Int>) {
        val cell = this.cell_map[coordinate.y][coordinate.x].value
        cell.remove_branch(position)
    }

    fun update_tree(coordinate: EditorTable.Coordinate, position: List<Int>, tree: ReducibleTree<out OpusEvent>) {
        // NOTE: tree needs to be the ACTUAL tree in order to recalc the weights
        val cell = this.cell_map[coordinate.y][coordinate.x].value
        val no_prune_paths = mutableSetOf<List<Int>>()

        val input_weight = tree.get_weight()
        tree.weighted_traverse(input_weight) { subtree, event, path, weight ->
            if (subtree.is_leaf()) {
                cell.set_leaf(position + path, event, weight)
                no_prune_paths.add(position + path)
            }
        }

        var i = 0
        while (i < cell.leafs.size) {
            val check_path = cell.leafs[i].first
            if (no_prune_paths.contains(cell.leafs[i].first) || check_path.size < position.size || check_path.subList(0, position.size) != position) {
                i++
            } else {
                cell.leafs.removeAt(i)
            }
        }

        cell.sort_leafs()
        cell.top_weight.value = tree.get_root().weighted_size
    }

    fun update_column(column: Int, is_tagged: Boolean) {
        this.column_data[column].is_tagged.value = is_tagged
    }

    fun add_row(y: Int, cells: ReducibleTreeArray<*>, new_line_data: LineData) {
        this.line_data.add(y, new_line_data)
        if (new_line_data.channel.value != null && new_line_data.line_offset.value != null && new_line_data.ctl_type.value == null) {
            this.channel_data[new_line_data.channel.value!!].size.intValue += 1
        }
        this.cell_map.add(y, MutableList(cells.beats.size) { i -> mutableStateOf(TreeData(cells.beats[i])) })
        this.line_count.value += 1

        // Update spillover
        var working_beat = 0
        var working_position = cells.get_first_position(0)
        if (cells.get_tree(working_beat, working_position).has_event()) {
            cells.get_proceding_event_position(working_beat, working_position)?.let {
                working_beat = it.first
                working_position = it.second
            } ?: return
        }

        while (true) {
            for ((blocked_beat, blocked_position) in cells.get_all_blocked_positions(working_beat, working_position)) {
                if (blocked_position == working_position && blocked_beat == working_beat) continue
                if (blocked_beat >= this.cell_map[y].size) continue
                this.cell_map[y][blocked_beat].value.get_leaf(blocked_position).value.is_spillover.value = true
            }

            cells.get_proceding_event_position(working_beat, working_position)?.let {
                working_beat = it.first
                working_position = it.second
            } ?: break
        }
    }

    fun update_line(y: Int, channel: Int?, line_offset: Int?, ctl_type: EffectType?, assigned_offset: Int?, is_mute: Boolean, is_selected: Boolean) {
        this.line_data[y].let {
            it.channel.value = channel
            it.line_offset.value = line_offset
            it.ctl_type.value = ctl_type
            it.assigned_offset.value = assigned_offset
            it.is_mute.value = is_mute
            it.is_selected.value = is_selected
        }
    }

    fun remove_row(y: Int, count: Int) {
        for (i in 0 until count) {
            val line = this.line_data.removeAt(y)
            if (line.channel.value != null && line.line_offset.value != null && line.ctl_type.value == null) {
                this.channel_data[line.channel.value!!].size.intValue -= 1
            }
            this.cell_map.removeAt(y)
        }
        this.line_count.value -= count
    }

    // Call after removing std line's row
    fun shift_line_offsets_down(channel: Int, line_offset: Int, count: Int = 1) {
        for (line_data in this.line_data) {
            if (line_data.channel.value != channel) continue
            line_data.line_offset.value?.let { check_offset ->
                if (check_offset <= line_offset) continue
                line_data.line_offset.value = check_offset - count
            }
        }
    }

    // Call after adding std line's row
    fun shift_line_offsets_up(channel: Int, line_offset: Int, initial_y: Int, count: Int = 1) {
        var offset = 1
        // we don't want to increment the effects of the working line, so skip over them
        for (line_data in this.line_data.subList(initial_y + 1, this.line_data.size)) {
            if (line_data.ctl_type.value == null) break
            offset++
        }

        for (line_data in this.line_data.subList(initial_y + offset, this.line_data.size)) {
            if (line_data.channel.value != channel) continue
            line_data.line_offset.value?.let { check_offset ->
                line_data.line_offset.value = check_offset + count
            }
        }
    }

    fun add_channel(channel: Int, percussion: Boolean, instrument: Pair<Int, Int>, is_mute: Boolean, size: Int = 0) {
        for (ld in this.line_data) {
            ld.channel.value?.let {
                if (it >= channel) {
                    ld.channel.value = it + 1
                }
            }
        }
        this.channel_count.value += 1
        val name = this.get_preset_name(instrument.first, instrument.second)
        this.channel_data.add(channel, ChannelData(percussion, instrument, is_mute, name = name, is_selected = false, size = size))
    }

    fun remove_channel(channel: Int) {
        var i = 0
        while (i < this.line_data.size) {
            val ld = this.line_data[i]
            ld.channel.value?.let { line_channel ->
                if (line_channel == channel) {
                    this.line_data.removeAt(i)
                    this.cell_map.removeAt(i)
                    this.line_count.value -= 1
                    continue
                } else if (line_channel > channel) {
                    ld.channel.value = line_channel - 1
                }
            }
            i++
        }
        this.channel_count.value -= 1
        this.channel_data.removeAt(channel)
    }

    fun add_column(column: Int, is_tagged: Boolean, new_cells: List<ReducibleTree<out OpusEvent>>? = null) {
        this.column_data.add(column, ColumnData(is_tagged))
        new_cells?.let {
            for ((y, line) in this.cell_map.enumerate()) {
                line.add(column, mutableStateOf(TreeData(new_cells[y])))
            }
        }
        this.beat_count.value += 1
    }

    fun remove_column(column: Int) {
        this.column_data.removeAt(column)
        for (line in this.cell_map) {
            line.removeAt(column)
        }
        this.beat_count.value -= 1
    }

    fun move_channel(channel_index: Int, new_channel_index: Int) {
        var from_index = -1
        var to_index = -1

        // First, Get the 2 channel start indices...
        for (y in this.line_data.indices) {
            this.line_data[y].channel.value?.let { working_channel ->
                if (to_index == -1 && new_channel_index == working_channel) {
                    to_index = y
                }
                if (from_index == -1 && channel_index == working_channel) {
                    from_index = y
                }
            }
            if (to_index > -1 && from_index > -1) break
        }

        if (to_index == -1) {
            to_index = this.line_data.size
            for (y in this.line_data.indices.reversed()) {
                if (this.line_data[y].channel.value != null) break
                to_index = y
            }
        }

        // ... Then move the lines ...
        if (from_index > to_index) {
            var i = 0
            while (i + from_index < this.line_data.size && this.line_data[i + from_index].channel.value == channel_index) {
                this.cell_map.add(to_index + i, this.cell_map.removeAt(from_index + i))
                this.line_data.add(to_index + i, this.line_data.removeAt(from_index + i))
                i++
            }
        } else if (to_index > from_index) {
            if (this.line_data[from_index].channel.value != this.line_data[to_index - 1].channel.value) {
                while (this.line_data[from_index].channel.value == channel_index) {
                    this.cell_map.add(to_index - 1, this.cell_map.removeAt(from_index))
                    this.line_data.add(to_index - 1, this.line_data.removeAt(from_index))
                }
            }
        }

        if (new_channel_index < channel_index) {
            this.channel_data.add(new_channel_index, this.channel_data.removeAt(channel_index))
        } else {
            this.channel_data.add(new_channel_index - 1, this.channel_data.removeAt(channel_index))
        }

        // ... Finally update the channels
        val channel_map = mutableListOf<Int>()
        for (line in this.line_data) {
            if (line.channel.value == null) continue
            if (channel_map.isEmpty() || channel_map.last() != line.channel.value!!) {
                channel_map.add(line.channel.value!!)
            }
            line.channel.value = channel_map.size - 1
        }
    }

    fun set_channel_data(channel_index: Int, percussion: Boolean, preset: Pair<Int, Int>, is_mute: Boolean, size: Int = 0) {
        val name = this.get_preset_name(preset.first, preset.second)
        this.channel_data[channel_index].update(percussion, preset, is_mute, is_selected = false, name = name, size)
    }

    fun set_project_name(name: String? = null) {
        this.project_name.value = name
    }

    fun <T: OpusEvent> set_active_event(event: T? = null, descriptor: EventDescriptor? = null) {
        this.active_event.value = event?.copy()
        this.active_event_descriptor.value = if (event == null) null else descriptor
    }

    fun refresh_cursor() {
        this.active_cursor.value?.let {
            this.set_cursor(it)
        }
    }

    fun set_cursor(cursor: CacheCursor?) {
        // Deselect old cursor
        while (this.selected_lines.isNotEmpty()) {
            this.selected_lines.removeAt(0).is_selected.value = false
        }
        while (this.selected_columns.isNotEmpty()) {
            this.selected_columns.removeAt(0).is_selected.value = false
        }
        while (this.selected_leafs.isNotEmpty()) {
            val leaf = this.selected_leafs.removeAt(0)
            leaf.is_selected.value = false
            leaf.is_secondary.value = false
        }

        this.active_cursor.value = cursor
        cursor?.let {
            this.populate_selected_leafs(cursor)
            this.populate_selected_lines(cursor)
            this.populate_selected_columns(cursor)
        }
    }

    private fun populate_selected_leafs(cursor: CacheCursor) {
        when (cursor.type) {
            CursorMode.Column -> {
                for (y in 0 until this.line_count.value) {
                    this.cell_map[y][cursor.ints[0]].value.let {
                        for ((_, leaf_data) in it.leafs) {
                            leaf_data.value.is_selected.value = false
                            leaf_data.value.is_secondary.value = true
                            this.selected_leafs.add(leaf_data.value)
                        }
                    }
                }
            }
            CursorMode.Line -> {
                val y = cursor.ints[0]
                if (y >= this.line_count.value) return // This is ok. It just means the line hasn't been added yet
                val active_line = this.line_data[y]
                for (x in 0 until this.beat_count.value) {
                    for ((y, line) in this.line_data.enumerate()) {
                        if (line.channel.value != active_line.channel.value) continue
                        if (line.line_offset.value != active_line.line_offset.value) continue
                        if (active_line.ctl_type.value != null && active_line.ctl_type.value != line.ctl_type.value) continue

                        this.cell_map[y][x].value.let {
                            for ((_, leaf_data) in it.leafs) {
                                leaf_data.value.is_selected.value = false
                                leaf_data.value.is_secondary.value = true
                                this.selected_leafs.add(leaf_data.value)
                            }
                        }
                    }
                }
            }
            CursorMode.Channel -> {
                for (x in 0 until this.beat_count.value) {
                    for ((y, line) in this.line_data.enumerate()) {
                        if (line.channel.value != cursor.ints[0]) continue

                        this.cell_map[y][x].value.also {
                            for ((_, leaf_data) in it.leafs) {
                                leaf_data.value.is_selected.value = false
                                leaf_data.value.is_secondary.value = true
                                this.selected_leafs.add(leaf_data.value)
                            }
                        }
                    }
                }
            }
            CursorMode.Single -> {
                val y = cursor.ints[0]
                val x = cursor.ints[1]
                if (this.cell_map.size > y && this.cell_map[y].size > x) {
                    this.cell_map[y][x].value.also {
                        for ((leaf_path, leaf_data) in it.leafs) {
                            leaf_data.value.is_selected.value = leaf_path == cursor.ints.subList(2, cursor.ints.size)
                            leaf_data.value.is_secondary.value = false // TODO
                            this.selected_leafs.add(leaf_data.value)
                        }
                    }
                }
            }
            CursorMode.Range -> {
                for (y in min(cursor.ints[0], cursor.ints[2]) .. max(cursor.ints[0], cursor.ints[2])) {
                    for (x in min(cursor.ints[1], cursor.ints[3]) .. max(cursor.ints[1], cursor.ints[3])) {
                        this.cell_map[y][x].value.also {
                            for ((_, leaf_data) in it.leafs) {
                                val is_selected = x == cursor.ints[0] && y == cursor.ints[1]
                                leaf_data.value.is_selected.value = is_selected
                                leaf_data.value.is_secondary.value = !is_selected
                                this.selected_leafs.add(leaf_data.value)
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }

    private fun populate_selected_columns(cursor: CacheCursor) {
        when (cursor.type) {
            CursorMode.Column -> {
                val x = cursor.ints[0]
                if (x < this.column_data.size) {
                    this.column_data[x].also {
                        it.is_selected.value = true
                        this.selected_columns.add(it)
                    }
                }
            }
            CursorMode.Single -> {
                val x = cursor.ints[1]
                if (x < this.column_data.size) {
                    this.column_data[x].also {
                        it.is_selected.value = true
                        this.selected_columns.add(it)
                    }
                }
            }
            CursorMode.Range -> {
                for (x in min(cursor.ints[1], cursor.ints[3]) .. max(cursor.ints[1], cursor.ints[3])) {
                    if (x < this.column_data.size) {
                        this.column_data[x].also {
                            it.is_selected.value = true
                            this.selected_columns.add(it)
                        }
                    }
                }
            }
            else -> {}
        }
    }

    private fun populate_selected_lines(cursor: CacheCursor) {
        when (cursor.type) {
            CursorMode.Channel -> {
                for (line_data in this.line_data) {
                    if (line_data.channel.value != cursor.ints[0]) continue
                    line_data.is_selected.value = true
                    this.selected_lines.add(line_data)
                }
            }
            CursorMode.Line -> {
                val y = cursor.ints[0]
                if (y >= this.line_count.value) return // This is ok. It just means the line hasn't been added yet
                val active_line = this.line_data[y]
                for ((_, line) in this.line_data.enumerate()) {
                    if (line.channel.value != active_line.channel.value) continue
                    if (line.line_offset.value != active_line.line_offset.value) continue
                    if (active_line.ctl_type.value != null && active_line.ctl_type.value != line.ctl_type.value) continue
                    line.is_selected.value = true
                    this.selected_lines.add(line)

                }
            }
            CursorMode.Single -> {
                this.line_data[cursor.ints[0]].also {
                    it.is_selected.value = true
                    this.selected_lines.add(it)
                }
            }
            CursorMode.Range -> {
                for (y in min(cursor.ints[0], cursor.ints[2]) .. max(cursor.ints[0], cursor.ints[2])) {
                    this.line_data[y].also {
                        it.is_selected.value = true
                        this.selected_lines.add(it)
                    }
                }
            }
            else -> {}
        }
    }

    fun swap_line_cells(y_a: Int, y_b: Int) {
        if (y_a == y_b) return

        val lesser = min(y_a, y_b)
        val larger = max(y_a, y_b)

        val lesser_line_data = this.line_data[lesser]
        val larger_line_data = this.line_data[larger]
        var lesser_line_count = 0
        var larger_line_count = 0

        var i = lesser
        while (this.line_data[i].channel.value == lesser_line_data.channel.value && this.line_data[i].line_offset.value == lesser_line_data.line_offset.value) {
            i++
            lesser_line_count++
        }

        i = larger
        while (this.line_data[i].channel.value == larger_line_data.channel.value && this.line_data[i].line_offset.value == larger_line_data.line_offset.value) {
            i++
            larger_line_count++
        }

        val larger_lines = Array(larger_line_count) {
            this.cell_map.removeAt(larger)
        }
        val lesser_lines = Array(lesser_line_count) {
            this.cell_map.removeAt(lesser)
        }

        for ((i, line) in larger_lines.enumerate()) {
            this.cell_map.add(lesser + i, line)
        }
        for ((i, line) in lesser_lines.enumerate()) {
            this.cell_map.add(larger + i + (larger_line_count - lesser_line_count), line)
        }
    }

    fun set_radix(radix: Int) {
        this.radix.value = radix
    }

    fun mute_channel(channel: Int, mute: Boolean) {
        this.channel_data[channel].is_mute.value = mute
    }

    fun mute_line(line: Int, mute: Boolean) {
        this.line_data[line].is_mute.value = mute
    }

    fun clear_instrument_names() {
        this.available_instruments.clear()
        this.preset_names.clear()
    }

    fun set_is_buffering(value: Boolean) {
        this.is_buffering.value = value
    }

    fun set_relative_mode(value: RelativeInputMode) {
        this.relative_input_mode.value = value
    }

    fun populate_presets(soundfont: SoundFont? = null) {
        this.preset_names.clear()
        this.available_instruments.clear()
        if (soundfont == null) return

        for ((name, bank, program) in soundfont.get_available_presets()) {
            if (!this.preset_names.containsKey(bank)) {
                this.preset_names[bank] = HashMap()
            }
            this.preset_names[bank]?.set(program, name)

            if (bank != 128) continue
            val preset = soundfont.get_preset(program, bank)

            val available_keys = mutableSetOf<Pair<String, Int>>()
            for ((_, preset_instrument) in preset.instruments) {
                if (preset_instrument.instrument == null) continue
                val instrument_range = preset_instrument.key_range ?: Pair(0, 127)

                for (sample_directive in preset_instrument.instrument!!.sample_directives.values) {
                    val key_range = sample_directive.key_range ?: Pair(0, 127)
                    val usable_range = max(key_range.first, instrument_range.first)..min(key_range.second, instrument_range.second)

                    var instrument_name = sample_directive.sample!!.first().name
                    if (instrument_name.contains("(")) {
                        instrument_name = instrument_name.substring(0, instrument_name.indexOf("("))
                    }

                    for (key in usable_range) {
                        val use_name = if (usable_range.first != usable_range.last) {
                            "$instrument_name - ${(key - usable_range.first) + 1}"
                        } else {
                            instrument_name
                        }
                        available_keys.add(Pair(use_name, key - 27))
                    }
                }
            }
            this.available_instruments[Pair(bank, program)] = available_keys.sortedBy { it.second }
        }
    }

    fun get_preset_name(bank: Int, program: Int): String? {
        return this.preset_names[bank]?.get(program)
    }

    fun update_channel_names() {
        for (channel in this.channel_data) {
            val (program, bank) = channel.instrument.value
            channel.active_name.value = this.get_preset_name(program, bank)
        }
    }

    fun get_available_instruments(preset_key: Pair<Int, Int>): List<Pair<String, Int>> {
        return this.available_instruments[preset_key] ?: listOf()
    }

    fun get_instrument_name(preset_key: Pair<Int, Int>, offset: Int): String? {
        for ((name, index) in this.get_available_instruments(preset_key)) {
            if (index == offset) return name
        }
        return null
    }

    // TODO: (maybe) cache these values so we don't have to calculate on every scroll
    private fun get_column_from_leaf(leaf: Int): Int {
        var output = 0
        var working_value = leaf
        for (i in 0 until this.beat_count.value) {
            if (working_value == 0) break
            working_value -= Array(this.cell_map.size) { j -> this.cell_map[j][i].value.top_weight.value }.max()
            output = i
            if (working_value < 0) break
        }
        return output
    }

    fun get_first_visible_column_index(): Int {
        return this.scroll_state_x.value.firstVisibleItemIndex
    }

    // fun get_last_visible_column_index(): Int {
    //     val scroll_container_offset = this.scroll_state_x.value.
    //     val min_leaf_width = this.base_leaf_width.value
    //     val reduced_x = scroll_container_offset / min_leaf_width
    //     val column_position = this.get_column_from_leaf(reduced_x.toInt())
    //     return column_position
    // }

    fun scroll_to_beat(beat: Int) {
        if (beat >= this.beat_count.value) return

        val state = this.scroll_state_x.value
        val target = if (this.playback_state_soundfont.value != PlaybackState.Ready) {
            Pair(beat, 0)
        } else if (state.firstVisibleItemIndex >= beat) {
            Pair(beat, 0)
        } else if (state.layoutInfo.visibleItemsInfo.last().index <= beat) {
            val beat_width = Array(this.line_count.value) { this.cell_map[it][beat].value.top_weight.value }.max()
            Pair(beat, (0 - state.layoutInfo.viewportSize.width + (beat_width * this.base_leaf_width.value)).toInt())
        } else {
            return
        }


        // TODO: Animate when not playing
        CoroutineScope(Dispatchers.Default).launch {
            this@ViewModelEditorState.scroll_state_x.value.requestScrollToItem(target.first, target.second)
        }
    }

    private fun get_beat_width(beat: Int): Int {
        return Array(this.line_count.value) { j -> this.cell_map[j][beat].value.top_weight.value }.max()
    }

    fun scroll_to_leaf(beat: Int, offset: Rational, width: Rational) {
        val beat_width = this.get_beat_width(beat)
        val offset_px = (beat_width * offset.toFloat() * this.base_leaf_width.value)

        val state = this.scroll_state_x.value
        val last_visible_beat = state.layoutInfo.visibleItemsInfo.last().index
        val first_visible_beat_width = if (state.firstVisibleItemIndex == beat) {
            beat_width
        } else {
            this.get_beat_width(state.firstVisibleItemIndex)
        } * this.base_leaf_width.value

        val (first_visible_beat, first_visible_offset) = if (state.firstVisibleItemScrollOffset > first_visible_beat_width) {
            Pair(state.firstVisibleItemIndex + 1, (state.firstVisibleItemScrollOffset - first_visible_beat_width).toInt())
        } else {
            Pair(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }

        if (first_visible_beat == beat && last_visible_beat == beat) return

        val end_offset = beat_width * (offset + width).toFloat() * this.base_leaf_width.value

        if (first_visible_beat < beat && last_visible_beat > beat) {
            return
        } else if (first_visible_beat > beat || first_visible_beat == beat && first_visible_offset > offset_px.toInt()) {
            CoroutineScope(Dispatchers.Default).launch {
                state.requestScrollToItem(beat, offset_px.toInt())
            }
        } else if (last_visible_beat < beat || (last_visible_beat == beat && state.layoutInfo.visibleItemsInfo.last().offset + end_offset > state.layoutInfo.viewportSize.width - this.table_side_padding.value)) {
            var working_offset = state.layoutInfo.viewportSize.width - this.table_side_padding.value
            var working_index = beat
            val beat_width_px = (beat_width * this.base_leaf_width.value).toInt()

            while (working_index >= 0) {
                val working_beat_width = Array(this.line_count.value) { j -> this.cell_map[j][working_index].value.top_weight.value }.max()
                val subtracter = (working_beat_width * this.base_leaf_width.value).toInt()
                if (working_offset - subtracter < beat_width_px) break
                working_offset -= subtracter
                working_index -= 1
            }

            CoroutineScope(Dispatchers.Default).launch {
                if (working_index > -1) {
                    state.requestScrollToItem(
                        working_index,
                        0 - working_offset + end_offset.toInt()
                    )
                } else {
                    state.requestScrollToItem(0)
                }
            }
        }
    }

    data class LocationQuad(var channel: Int?, var line_offset: Int?, var beat: Int?, var position: List<Int>?)

    fun get_location_ints(): LocationQuad {
        val cursor = this.active_cursor.value ?: return LocationQuad(null, null, null, null)
        val (channel, line_offset) = if (cursor.type == CursorMode.Line || cursor.type == CursorMode.Single) {
            val line_info = this.line_data[cursor.ints[0]]
            Pair(line_info.channel.value, line_info.line_offset.value)
        } else {
            Pair(null, null)
        }

        val (beat, position) = if (cursor.type == CursorMode.Single) {
            Pair(cursor.ints[1], cursor.ints.subList(2, cursor.ints.size))
        } else {
            Pair(null, null)
        }

        return LocationQuad(channel, line_offset, beat, position)
    }

    fun enable_soundfont() {
        this.soundfont_active.value = true
    }

    fun unset_soundfont() {
        this.soundfont_active.value = false
        this.clear_instrument_names()
    }
}