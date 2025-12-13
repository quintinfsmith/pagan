package com.qfs.pagan.viewmodel

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
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

    class LineData(channel: Int?, line_offset: Int?, ctl_type: EffectType?, assigned_offset: Int? = null, is_mute: Boolean, is_selected: Boolean = false) {
        val channel = mutableStateOf(channel)
        val line_offset = mutableStateOf(line_offset)
        val ctl_type = mutableStateOf(ctl_type)
        val assigned_offset = mutableStateOf(assigned_offset)
        val is_mute = mutableStateOf(is_mute)
        val is_selected = mutableStateOf(is_selected)
    }

    class ColumnData(is_tagged: Boolean, is_selected: Boolean = false) {
        val is_tagged = mutableStateOf(is_tagged)
        val is_selected = mutableStateOf(is_selected)
    }

    class LeafData(is_selected: Boolean = false, is_secondary: Boolean = false, is_valid: Boolean = true, is_spillover: Boolean = false) {
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
            this.size.value = size
        }
    }
    class CacheCursor(var type: CursorMode, vararg ints: Int) {
        var ints = ints.toList()
    }

    var project_name: MutableState<String?> = mutableStateOf(null)
    var beat_count: MutableState<Int> = mutableIntStateOf(0)
    var line_count: MutableState<Int> = mutableIntStateOf(0)
    var channel_count: MutableState<Int> = mutableIntStateOf(0)
    val line_data: MutableList<LineData> = mutableListOf()
    val column_data: MutableList<ColumnData> = mutableListOf()
    val cell_map = mutableListOf<MutableList<MutableState<ReducibleTree<Pair<LeafData, OpusEvent?>>>>>()
    val channel_data: MutableList<ChannelData> = mutableListOf()
    var radix: MutableState<Int> = mutableIntStateOf(12)

    var active_event: MutableState<OpusEvent?> = mutableStateOf(null)
    var active_cursor: MutableState<CacheCursor?> = mutableStateOf(null)
    // selected_* are used to quickly unset is_selected when cursor is changed
    var selected_columns: MutableList<ColumnData> = mutableListOf()
    var selected_lines: MutableList<LineData> = mutableListOf()
    var selected_leafs: MutableList<LeafData> = mutableListOf()

    var blocker_leaf: List<Int>? = null
    val playback_state_soundfont: MutableState<PlaybackState> = mutableStateOf(PlaybackState.NotReady)
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

    fun clear() {
        this.project_name.value = null
        this.beat_count.value = 0
        this.active_event.value = null
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
        this.column_data[column].is_tagged.value = is_tagged
    }

    fun add_row(y: Int, cells: List<ReducibleTree<out OpusEvent>>, new_line_data: LineData) {
        this.line_data.add(y, new_line_data)
        if (new_line_data.channel.value != null && new_line_data.line_offset.value != null && new_line_data.ctl_type.value == null) {
            this.channel_data[new_line_data.channel.value!!].size.value += 1
        }
        this.cell_map.add(y, MutableList(cells.size) { i -> this.copy_tree_for_cell(cells[i]) })
        this.line_count.value += 1
    }

    fun queue_config_drawer_redraw_export_button() {
        TODO()
    }

    fun update_line(y: Int, channel: Int?, line_offset: Int?, ctl_type: EffectType?, assigned_offset: Int?, is_mute: Boolean, is_selected: Boolean) {
        this.line_data[y].channel.value = channel
        this.line_data[y].line_offset.value = line_offset
        this.line_data[y].ctl_type.value = ctl_type
        this.line_data[y].assigned_offset.value = assigned_offset
        this.line_data[y].is_mute.value = is_mute
        this.line_data[y].is_selected.value = is_selected
    }

    fun remove_row(y: Int, count: Int) {
        for (i in 0 until count) {
            val line = this.line_data.removeAt(y)
            if (line.channel.value != null && line.line_offset.value != null && line.ctl_type.value == null) {
                this.channel_data[line.channel.value!!].size.value -= 1
            }
            this.cell_map.removeAt(y)
        }
        this.line_count.value -= 1
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
    fun shift_line_offsets_up(channel: Int, line_offset: Int, count: Int = 1) {
        for (line_data in this.line_data) {
            if (line_data.channel.value != channel) continue
            line_data.line_offset.value?.let { check_offset ->
                if (check_offset <= line_offset) continue
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

    fun add_column(column: Int, is_tagged: Boolean, new_cells: List<ReducibleTree<out OpusEvent>>) {
        this.column_data.add(column, ColumnData(is_tagged))
        for ((y, line) in this.cell_map.enumerate()) {
            line.add(column, this.copy_tree_for_cell(new_cells[y]))
        }
        this.beat_count.value += 1
    }

    private fun copy_tree_for_cell(tree: ReducibleTree<out OpusEvent>): MutableState<ReducibleTree<Pair<LeafData, OpusEvent?>>> {
        val new_tree = tree.copy { event ->
            Pair(LeafData(), event?.copy())
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

        // ... Then move the lines ...
        if (from_index > to_index) {
            while (from_index < this.line_data.size && this.line_data[from_index].channel.value != channel_index) {
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
            if (line.channel.value == null) continue
            if (line.channel.value != working_channel) {
                working_channel++
                c++
            }
            line.channel.value = c
        }
    }

    fun set_channel_data(channel_index: Int, percussion: Boolean, preset: Pair<Int, Int>, is_mute: Boolean, size: Int = 0) {
        val name = this.get_preset_name(preset.first, preset.second)
        this.channel_data[channel_index].update(percussion, preset, is_mute, is_selected = false, name = name, size)
    }

    fun set_project_name(name: String? = null) {
        this.project_name.value = name
    }

    fun <T: OpusEvent> set_active_event(event: T? = null) {
        this.active_event.value = event
    }

    fun refresh_cursor() {
        this.active_cursor.value?.let {
            this.set_cursor(it)
        }
    }
    fun set_cursor(cursor: CacheCursor) {
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
        this.populate_selected_leafs(cursor)
        this.populate_selected_lines(cursor)
        this.populate_selected_columns(cursor)
    }

    private fun populate_selected_leafs(cursor: CacheCursor) {
        when (cursor.type) {
            CursorMode.Column -> {
                for (y in 0 until this.line_count.value) {
                    this.cell_map[y][cursor.ints[0]].value.also {
                        it.traverse { tree, pair ->
                            if (tree.is_leaf()) {
                                pair?.first?.let { leaf_data ->
                                    leaf_data.is_selected.value = false
                                    leaf_data.is_secondary.value = true
                                    this.selected_leafs.add(leaf_data)
                                }
                            }
                        }
                    }
                }
            }
            CursorMode.Line -> {
                val active_line = this.line_data[cursor.ints[0]]
                for (x in 0 until this.beat_count.value) {
                    for ((y, line) in this.line_data.enumerate()) {
                        if (line.channel.value != active_line.channel.value) continue
                        if (line.line_offset.value != active_line.line_offset.value) continue
                        if (active_line.ctl_type.value != null && active_line.ctl_type.value != line.ctl_type.value) continue

                        this.cell_map[y][x].value.also {
                            it.traverse { tree, pair ->
                                if (tree.is_leaf()) {
                                    pair?.first?.let { leaf_data ->
                                        leaf_data.is_selected.value = false
                                        leaf_data.is_secondary.value = true
                                        this.selected_leafs.add(leaf_data)
                                    }
                                }
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
                            it.traverse { tree, pair ->
                                if (tree.is_leaf()) {
                                    pair?.first?.let { leaf_data ->
                                        leaf_data.is_selected.value = false
                                        leaf_data.is_secondary.value = true
                                        this.selected_leafs.add(leaf_data)
                                    }
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
                                leaf_data.is_selected.value = tree.get_path() == cursor.ints.subList(2, cursor.ints.size)
                                leaf_data.is_secondary.value = false // TODO
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
                                        leaf_data.is_selected.value = is_selected
                                        leaf_data.is_secondary.value = !is_selected
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
                this.column_data[cursor.ints[0]].also {
                    it.is_selected.value = true
                    this.selected_columns.add(it)
                }
            }
            CursorMode.Single -> {
                this.column_data[cursor.ints[1]].also {
                    it.is_selected.value = true
                    this.selected_columns.add(it)
                }
            }
            CursorMode.Range -> {
                for (x in min(cursor.ints[1], cursor.ints[3]) .. max(cursor.ints[1], cursor.ints[3])) {
                    this.column_data[x].also {
                        it.is_selected.value = true
                        this.selected_columns.add(it)
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
                val active_line = this.line_data[cursor.ints[0]]
                for ((y, line) in this.line_data.enumerate()) {
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

    fun get_instrument_name(preset_key: Pair<Int, Int>, offset: Int): String {
        val available = this.get_available_instruments(preset_key)
        return if (available.size <= offset) "TODO"
            else available[offset].first
    }

    // TODO: (maybe) cache these values so we don't have to calculate on every scroll
    private fun get_column_from_leaf(leaf: Int): Int {
        var output = 0
        var working_value = leaf
        for (i in 0 until this.beat_count.value) {
            if (working_value == 0) break
            working_value -= Array(this.cell_map.size) { j -> this.cell_map[j][i].value.weighted_size }.max()
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
            val beat_width = Array(this.line_count.value) { this.cell_map[it][beat].value.weighted_size }.max()
            Pair(beat, (0 - state.layoutInfo.viewportSize.width + (beat_width * this.base_leaf_width.value)).toInt())
        } else {
            return
        }

        // TODO: Animate when not playing
        CoroutineScope(Dispatchers.Default).launch {
            this@ViewModelEditorState.scroll_state_x.value.requestScrollToItem(target.first, target.second)
        }
    }
    fun scroll_to_leaf(beat: Int, offset: Rational) {

    }
}