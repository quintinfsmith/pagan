package com.qfs.pagan.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.qfs.apres.soundfont2.SoundFont
import com.qfs.pagan.Activity.ActivityEditor.PlaybackState
import com.qfs.pagan.EditorTable
import com.qfs.pagan.RelativeInputMode
import com.qfs.pagan.enumerate
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import kotlin.math.max
import kotlin.math.min

class ViewModelEditorState: ViewModel() {
    companion object {
        fun get_next_playback_state(input_state: PlaybackState, next_state: PlaybackState): PlaybackState? {
            return when (input_state) {
                PlaybackState.NotReady -> {
                    when (next_state) {
                        PlaybackState.NotReady,
                        PlaybackState.Ready -> next_state
                        else -> null
                    }
                }
                PlaybackState.Ready -> {
                    when (next_state) {
                        PlaybackState.NotReady,
                        PlaybackState.Ready,
                        PlaybackState.Queued -> next_state
                        else -> null
                    }
                }
                PlaybackState.Playing -> {
                    when (next_state) {
                        PlaybackState.Ready,
                        PlaybackState.Stopping -> next_state
                        else -> null
                    }
                }
                PlaybackState.Queued -> {
                    when (next_state) {
                        PlaybackState.Ready,
                        PlaybackState.Playing -> next_state
                        else -> null
                    }
                }
                PlaybackState.Stopping -> {
                    when (next_state) {
                        PlaybackState.Ready -> next_state
                        else -> null
                    }
                }
            }
        }
    }

    var available_preset_names: HashMap<Pair<Int, Int>, String>? = null

    ///////////////////////////////////////////////////////

    private val working_path = mutableListOf<Int>()

    enum class SelectionLevel {
        Unselected,
        Primary,
        Secondary
    }

    data class LineData(var channel: Int?, var line_offset: Int?, var ctl_type: EffectType?, var assigned_offset: Int? = null, var is_mute: Boolean, var is_selected: Boolean = false)
    data class ColumnData(var is_tagged: Boolean, var is_selected: Boolean = false)
    data class LeafData(var is_selected: Boolean = false, var is_secondary: Boolean = false, var is_valid: Boolean = true, var is_spillover: Boolean = false)
    data class ChannelData(var percussion: Boolean, var instrument: Pair<Int, Int>, var is_mute: Boolean, var is_selected: Boolean = false)
    class CacheCursor(var type: CursorMode, vararg ints: Int) {
        var ints = ints.toList()
    }

    var project_name: MutableState<String?> = mutableStateOf(null)
    var beat_count: MutableState<Int> = mutableIntStateOf(0)
    var line_count: MutableState<Int> = mutableIntStateOf(0)
    var channel_count: MutableState<Int> = mutableIntStateOf(0)
    val line_data: MutableList<LineData> = mutableListOf()
    val column_data: MutableList<MutableState<ColumnData>> = mutableListOf()
    val cell_map = mutableListOf<MutableList<MutableState<ReducibleTree<Pair<LeafData, OpusEvent?>>>>>()
    val channel_data: MutableList<ChannelData> = mutableListOf()
    var radix: MutableState<Int> = mutableIntStateOf(12)

    var active_event: MutableState<OpusEvent?> = mutableStateOf(null)
    var active_cursor: MutableState<CacheCursor?> = mutableStateOf(null)
    // selected_* are used to quickly unset is_selected when cursor is changed
    var selected_columns: MutableList<ColumnData> = mutableListOf()
    var selected_lines: MutableList<LineData> = mutableListOf()
    var selected_leafs: MutableList<LeafData> = mutableListOf()

    var project_exists: MutableState<Boolean> = mutableStateOf(false)
    var instrument_names = HashMap<Int, HashMap<Int, String>?>() // NOTE: "instrument". Not "preset".
    var blocker_leaf: List<Int>? = null
    val playback_state_soundfont: MutableState<PlaybackState> = mutableStateOf(PlaybackState.NotReady)
    val playback_state_midi: MutableState<PlaybackState> = mutableStateOf(PlaybackState.NotReady)
    val relative_input_mode: MutableState<RelativeInputMode> = mutableStateOf(RelativeInputMode.Absolute)

    val highlighted_offset: MutableState<Int?> = mutableStateOf(null)
    val highlighted_octave: MutableState<Int?> = mutableStateOf(null)

    val is_buffering: MutableState<Boolean> = mutableStateOf(false)

    fun clear() {
        this.project_name.value = null
        this.beat_count.value = 0
        this.active_event.value = null
        this.active_cursor.value = null
        this.radix.value = 12
        this.line_count.value = 0
        this.channel_count.value = 0
        this.project_exists.value = false
        this.blocker_leaf = null

        this.highlighted_octave.value = null
        this.highlighted_offset.value = null

        this.line_data.clear()
        this.column_data.clear()
        this.cell_map.clear()
        this.channel_data.clear()
        this.instrument_names.clear()
    }

    /** Only used in full refresh **/
    fun empty_cells() {
        for (line in this.cell_map) {
            line.clear()
        }
    }

    // TODO: This isn't right
    fun update_cell(coordinate: EditorTable.Coordinate, tree: ReducibleTree<out OpusEvent>) {
        this.cell_map[coordinate.y][coordinate.x].value = this.copy_tree_for_cell(tree).value
    }

    fun update_column(column: Int, is_tagged: Boolean) {
        this.column_data[column].value.is_tagged = is_tagged
    }

    fun add_row(y: Int, cells: List<ReducibleTree<out OpusEvent>>, new_line_data: LineData) {
        this.line_data.add(y, new_line_data)
        this.cell_map.add(y, MutableList(cells.size) { i -> this.copy_tree_for_cell(cells[i]) })
        this.line_count.value += 1
    }

    fun set_project_exists(value: Boolean) {
        this.project_exists.value = value
    }

    fun queue_config_drawer_redraw_export_button() {
        TODO()
    }

    fun update_line(y: Int, line_data: LineData) {
        this.line_data[y] = line_data
    }

    fun remove_row(y: Int, count: Int) {
        for (i in 0 until count) {
            this.line_data.removeAt(y)
            this.cell_map.removeAt(y)
        }
        this.line_count.value -= 1
    }

    fun add_channel(channel: Int, percussion: Boolean, instrument: Pair<Int, Int>, is_mute: Boolean) {
        for (ld in this.line_data) {
            ld.channel?.let {
                if (it >= channel) {
                    ld.channel = it + 1
                }
            }
        }
        this.channel_data.add(channel, ChannelData(percussion, instrument, is_mute))
    }

    fun remove_channel(channel: Int) {
        var i = 0
        while (i < this.line_data.size) {
            val ld = this.line_data[i]
            ld.channel?.let { line_channel ->
                if (line_channel == channel) {
                    this.line_data.removeAt(i)
                    this.cell_map.removeAt(i)
                    this.line_count.value -= 1
                    continue
                } else if (line_channel > channel) {
                    ld.channel = line_channel - 1
                }
            }
            i++
        }
        this.channel_data.removeAt(channel)
    }

    fun add_column(column: Int, is_tagged: Boolean, new_cells: List<ReducibleTree<out OpusEvent>>) {
        this.column_data.add(column, mutableStateOf(ColumnData(is_tagged)))
        for ((y, line) in this.cell_map.enumerate()) {
            line.add(column, this.copy_tree_for_cell(new_cells[y]))
        }
        this.beat_count.value += 1
    }

    private fun copy_tree_for_cell(tree: ReducibleTree<out OpusEvent>): MutableState<ReducibleTree<Pair<LeafData, OpusEvent?>>> {
        val new_tree = tree.copy<Pair<LeafData, OpusEvent?>> { event ->
            Pair(LeafData(), event.copy())
        }

        new_tree.traverse { tree, pair ->
            if (pair == null && tree.is_leaf()) {
                tree.event = Pair(LeafData(), null)
            }
        }

        return mutableStateOf(new_tree)
    }

    fun remove_column(column: Int) {
        this.column_data.removeAt(column)
        for (line in this.cell_map) {
            line.removeAt(column)
        }
        this.beat_count.value -= 1
    }

    fun queue_force_scroll(y: Int, x: Int, offset: Rational, offset_width: Rational, force: Boolean) {
        TODO()
    }

    fun move_channel(channel_index: Int, new_channel_index: Int) {
        var from_index = -1
        var to_index = -1

        // First, Get the 2 channel start indices...
        for (y in this.line_data.indices) {
            this.line_data[y].channel?.let { working_channel ->
                if (to_index == -1 && new_channel_index == working_channel) {
                    to_index = y
                }
                if (from_index == -1 && channel_index == working_channel) {
                    from_index = y
                }
            }
            if (to_index > -1 && from_index > -1) break
        }

        // ... Then move the lines ...
        if (from_index > to_index) {
            while (from_index < this.line_data.size && this.line_data[from_index].channel != channel_index) {
                this.cell_map.add(to_index, this.cell_map.removeAt(from_index))
                this.line_data.add(to_index++, this.line_data.removeAt(from_index))
            }
        } else if (to_index > from_index) {
            while (true) {
                this.cell_map.add(to_index - 1, this.cell_map.removeAt(from_index))
                this.line_data.add(to_index++ - 1, this.line_data.removeAt(from_index))
            }
        }

        // ... Finally update the channels
        var working_channel = -1
        var c = -1
        for (line in this.line_data) {
            if (line.channel == null) continue
            if (line.channel != working_channel) {
                working_channel++
                c++
            }
            line.channel = c
        }
    }

    fun set_channel_data(channel_index: Int, percussion: Boolean, instrument: Pair<Int, Int>, is_mute: Boolean) {
        this.channel_data[channel_index] = ChannelData(percussion, instrument, is_mute)
    }

    fun set_project_name(name: String? = null) {
        this.project_name.value = name
    }

    fun <T: OpusEvent> set_active_event(event: T? = null) {
        this.active_event.value = event
    }

    fun set_cursor(cursor: CacheCursor) {
        // Deselect old cursor
        while (this.selected_lines.isNotEmpty()) {
            this.selected_lines.removeAt(0).is_selected = false
        }
        while (this.selected_columns.isNotEmpty()) {
            this.selected_columns.removeAt(0).is_selected = false
        }
        while (this.selected_leafs.isNotEmpty()) {
            val leaf = this.selected_leafs.removeAt(0)
            leaf.is_selected = false
            leaf.is_secondary = false
        }

        this.active_cursor.value = cursor
        this.populate_selected_leafs(cursor)
        this.populate_selected_lines(cursor)
        this.populate_selected_columns(cursor)
    }

    fun shift_up_percussion_names(channel: Int) {
        val keys = this.instrument_names.keys.sorted().reversed()
        for (k in keys) {
            if (k < channel) continue
            this.instrument_names[k + 1] = this.instrument_names.remove(k)!!
        }
    }

    fun shift_down_percussion_names(channel: Int) {
        val keys = this.instrument_names.keys.sorted()
        for (k in keys) {
            if (k > channel) {
                this.instrument_names[k - 1] = this.instrument_names.remove(k)!!
            } else if (k == channel) {
                this.instrument_names.remove(k)
            }
        }
    }

    private fun populate_selected_leafs(cursor: CacheCursor) {
        when (cursor.type) {
            CursorMode.Column -> {
                for (y in 0 until this.line_count.value) {
                    this.cell_map[y][cursor.ints[0]].value.also {
                        it.traverse { tree, pair ->
                            if (tree.is_leaf()) {
                                pair?.first?.let { leaf_data ->
                                    leaf_data.is_selected = false
                                    leaf_data.is_secondary = true
                                    this.selected_leafs.add(leaf_data)
                                }
                            }
                        }
                    }
                }
            }
            CursorMode.Line -> {
                for (x in 0 until this.beat_count.value) {
                    this.cell_map[cursor.ints[0]][x].value.also {
                        it.traverse { tree, pair ->
                            if (tree.is_leaf()) {
                                pair?.first?.let { leaf_data ->
                                    leaf_data.is_selected = false
                                    leaf_data.is_secondary = true
                                    this.selected_leafs.add(leaf_data)
                                }
                            }
                        }
                    }
                }
            }
            CursorMode.Single -> {
                this.cell_map[cursor.ints[0]][cursor.ints[1]].value.also {
                    it.traverse { tree, pair ->
                        if (tree.is_leaf()) {
                            pair?.first?.let { leaf_data ->
                                leaf_data.is_selected = tree.get_path() == cursor.ints.subList(2, cursor.ints.size)
                                leaf_data.is_secondary = false // TODO
                                this.selected_leafs.add(leaf_data)
                            }
                        }
                    }
                }
            }
            CursorMode.Range -> {
                for (y in min(cursor.ints[0], cursor.ints[2]) .. max(cursor.ints[0], cursor.ints[2])) {
                    for (x in min(cursor.ints[1], cursor.ints[3]) .. max(cursor.ints[1], cursor.ints[3])) {
                        this.cell_map[y][x].value.also {
                            it.traverse { tree, pair ->
                                if (tree.is_leaf()) {
                                    val is_selected = x == cursor.ints[0] && y == cursor.ints[1]
                                    pair?.first?.let { leaf_data ->
                                        leaf_data.is_selected = is_selected
                                        leaf_data.is_secondary = !is_selected
                                        this.selected_leafs.add(leaf_data)
                                    }
                                }
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
                this.column_data[cursor.ints[0]].value.also {
                    it.is_selected = true
                    this.selected_columns.add(it)
                }
            }
            CursorMode.Single -> {
                this.column_data[cursor.ints[1]].value.also {
                    it.is_selected = true
                    this.selected_columns.add(it)
                }
            }
            CursorMode.Range -> {
                for (x in min(cursor.ints[1], cursor.ints[3]) .. max(cursor.ints[1], cursor.ints[3])) {
                    this.column_data[x].value.also {
                        it.is_selected = true
                        this.selected_columns.add(it)
                    }
                }
            }
            else -> {}
        }
    }
    private fun populate_selected_lines(cursor: CacheCursor) {
        when (cursor.type) {
            CursorMode.Line -> {
                // TODO: link effect lines
                this.line_data[cursor.ints[0]].also {
                    it.is_selected = true
                    this.selected_lines.add(it)
                }
            }
            CursorMode.Single -> {
                this.line_data[cursor.ints[0]].also {
                    it.is_selected = true
                    this.selected_lines.add(it)
                }
            }
            CursorMode.Range -> {
                for (y in min(cursor.ints[0], cursor.ints[2]) .. max(cursor.ints[0], cursor.ints[2])) {
                    this.line_data[y].also {
                        it.is_selected = true
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
        while (this.line_data[i].channel == lesser_line_data.channel && this.line_data[i].line_offset == lesser_line_data.line_offset) {
            i++
            lesser_line_count++
        }

        i = larger
        while (this.line_data[i].channel == larger_line_data.channel && this.line_data[i].line_offset == larger_line_data.line_offset) {
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
        this.channel_data[channel].is_mute = mute
    }

    fun mute_line(line: Int, mute: Boolean) {
        this.line_data[line].is_mute = mute
    }

    fun populate_preset_names(soundfont: SoundFont) {
        this.available_preset_names = HashMap()
        for ((name, program, bank) in soundfont.get_available_presets()) {
            this.available_preset_names?.set(Pair(bank, program), name)
        }
    }
    fun clear_preset_names() {
        this.available_preset_names = null
    }

    fun clear_instrument_names() {
        this.instrument_names.clear()
    }

    fun set_instrument_names(channel: Int, names: List<Pair<String, Int>>?) {
        this.instrument_names[channel] = if (names == null) {
            null
        } else {
            val hashmap = HashMap<Int, String>()
            for ((name, index) in names) {
                hashmap[index] = name
            }
            hashmap
        }
    }

    fun set_is_buffering(value: Boolean) {
        this.is_buffering.value = value
    }

    fun set_relative_mode(value: RelativeInputMode) {
        this.relative_input_mode.value = value
    }
}