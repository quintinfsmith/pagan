package com.qfs.pagan.uibill

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.qfs.pagan.EditorTable
import com.qfs.pagan.enumerate
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import kotlin.math.max
import kotlin.math.min

// IN PROGRESS: converting this into  a UI State representation of the Editor....

/**
* A queue of UI update commands to be executed once it is safe to do so.
*/
class UIFacade {
    val ui_lock = UILock()
    private val working_path = mutableListOf<Int>()

    enum class SelectionLevel {
        Unselected,
        Primary,
        Secondary
    }

    data class LineData(var channel: Int?, var line_offset: Int?, var ctl_type: EffectType?, var assigned_offset: Int? = null)
    data class ColumnData(var is_tagged: Boolean)
    data class ChannelData(var percussion: Boolean, var instrument: Pair<Int, Int>)
    class CacheCursor(var type: CursorMode, vararg ints: Int) {
        var ints = ints.toList()
    }

    var project_name: String? = null
    var beat_count: MutableState<Int> = mutableIntStateOf(0)
    val line_data: MutableList<LineData> = mutableListOf()
    val column_data: MutableList<ColumnData> = mutableListOf()
    val cell_map = mutableListOf<MutableList<MutableState<ReducibleTree<out OpusEvent>>>>()
    val channel_data: MutableList<ChannelData> = mutableListOf()

    var active_event: MutableState<OpusEvent?> = mutableStateOf(null)
    var active_cursor: MutableState<CacheCursor?> = mutableStateOf(null)
    var project_exists: Boolean = false
    var instrument_names = HashMap<Int, HashMap<Int, String>?>()
    var blocker_leaf: List<Int>? = null

    fun clear() {
        if (this.ui_lock.is_locked()) return
        this.project_name = null
        this.beat_count.value = 0
        this.active_event.value = null
        this.active_cursor.value = null
        this.project_exists = false
        this.blocker_leaf = null

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

    fun update_cell(coordinate: EditorTable.Coordinate, tree: ReducibleTree<out OpusEvent>) {
        if (this.ui_lock.is_locked()) return
        this.cell_map[coordinate.y][coordinate.x].value = tree
    }

    fun update_column(column: Int, is_tagged: Boolean) {
        if (this.ui_lock.is_locked()) return
        this.column_data[column].is_tagged = is_tagged
    }

    fun add_row(y: Int, cells: MutableList<MutableState<ReducibleTree<out OpusEvent>>>, new_line_data: LineData) {
        if (this.ui_lock.is_locked()) return
        this.line_data.add(y, new_line_data)
        this.cell_map.add(y, cells)
    }

    fun set_project_exists(value: Boolean) {
        if (this.ui_lock.is_locked()) return
        this.project_exists = value
    }

    fun queue_config_drawer_redraw_export_button() {
        if (this.ui_lock.is_locked()) return
        TODO()
    }

    fun update_line(y: Int, line_data: LineData) {
        if (this.ui_lock.is_locked()) return
        this.line_data[y] = line_data
    }

    fun remove_row(y: Int, count: Int) {
        if (this.ui_lock.is_locked()) return
        for (i in 0 until count) {
            this.line_data.removeAt(y)
            this.cell_map.removeAt(y)
        }
    }

    fun add_channel(channel: Int, percussion: Boolean, instrument: Pair<Int, Int>) {
        if (this.ui_lock.is_locked()) return
        for (ld in this.line_data) {
            ld.channel?.let {
                if (it >= channel) {
                    ld.channel = it + 1
                }
            }
        }
        this.channel_data.add(channel, ChannelData(percussion, instrument))
    }

    fun remove_channel(channel: Int) {
        if (this.ui_lock.is_locked()) return
        var i = 0
        while (i < this.line_data.size) {
            val ld = this.line_data[i]
            ld.channel?.let { line_channel ->
                if (line_channel == channel) {
                    this.line_data.removeAt(i)
                    this.cell_map.removeAt(i)
                    continue
                } else if (line_channel > channel) {
                    ld.channel = line_channel - 1
                }
            }
            i++
        }
        this.channel_data.removeAt(channel)
    }

    fun add_column(column: Int, is_tagged: Boolean, new_cells: List<MutableState<ReducibleTree<out OpusEvent>>>) {
        if (this.ui_lock.is_locked()) return
        this.column_data.add(column, ColumnData(is_tagged))
        for ((y, line) in this.cell_map.enumerate()) {
            line.add(column, new_cells[y])
        }
        this.beat_count.value += 1
    }

    fun remove_column(column: Int) {
        if (this.ui_lock.is_locked()) return
        this.column_data.removeAt(column)
        for (line in this.cell_map) {
            line.removeAt(column)
        }
        this.beat_count.value -= 1
    }

    fun queue_refresh_choose_percussion_button(channel: Int, line_offset: Int) {
        if (this.ui_lock.is_locked()) return
        TODO()
    }

    fun queue_force_scroll(y: Int, x: Int, offset: Rational, offset_width: Rational, force: Boolean) {
        if (this.ui_lock.is_locked()) return
        TODO()
    }

    fun lock() {
        this.ui_lock.lock()
    }

    fun unlock() {
        this.ui_lock.unlock()
    }

    fun is_locked(): Boolean {
        return this.ui_lock.is_locked()
    }

    fun move_channel(channel_index: Int, new_channel_index: Int) {
        if (this.ui_lock.is_locked()) return
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

    fun set_channel_data(channel_index: Int, percussion: Boolean, instrument: Pair<Int, Int>) {
        if (this.ui_lock.is_locked()) return
        this.channel_data[channel_index] = ChannelData(percussion, instrument)
    }

    fun set_project_name(name: String? = null) {
        if (this.ui_lock.is_locked()) return
        this.project_name = name
    }

    fun <T: OpusEvent> set_active_event(event: T) {
        if (this.ui_lock.is_locked()) return
        this.active_event.value = event
    }

    fun set_cursor(cursor: CacheCursor) {
        if (this.ui_lock.is_locked()) return
        this.active_cursor.value = cursor
    }

    fun shift_up_percussion_names(channel: Int) {
        if (this.ui_lock.is_locked()) return
        val keys = this.instrument_names.keys.sorted().reversed()
        for (k in keys) {
            if (k < channel) continue
            this.instrument_names[k + 1] = this.instrument_names.remove(k)!!
        }
    }

    fun shift_down_percussion_names(channel: Int) {
        if (this.ui_lock.is_locked()) return
        val keys = this.instrument_names.keys.sorted()
        for (k in keys) {
            if (k > channel) {
                this.instrument_names[k - 1] = this.instrument_names.remove(k)!!
            } else if (k == channel) {
                this.instrument_names.remove(k)
            }
        }
    }

    fun is_column_selected(cursor: CacheCursor, x: Int): Boolean {
        return when (cursor.type) {
            CursorMode.Column -> cursor.ints[0] == x
            CursorMode.Single -> cursor.ints[1] == x
            CursorMode.Range -> {
                (min(cursor.ints[1], cursor.ints[3]) .. max(cursor.ints[1], cursor.ints[3])).contains(x)
            }
            CursorMode.Channel,
            CursorMode.Unset,
            CursorMode.Line -> false
        }
    }

    // TODO: Handle Effects
    fun is_line_selected(cursor: CacheCursor, y: Int): Boolean {
        return when (cursor.type) {
            CursorMode.Line -> {
                val cursor_line = this.line_data[cursor.ints[0]]
                val check_line = this.line_data[y]
                cursor.ints[0] == y ||
                    (check_line.channel == cursor_line.channel && check_line.line_offset == cursor_line.line_offset)
            }
            CursorMode.Single -> cursor.ints[0] == y
            CursorMode.Range -> {
                (min(cursor.ints[0], cursor.ints[2]) .. max(cursor.ints[0], cursor.ints[2])).contains(y)
            }
            CursorMode.Channel,
            CursorMode.Column,
            CursorMode.Unset -> false
        }
    }

    // TODO: Handle Effects
    fun is_cell_selected(cursor: CacheCursor, y: Int, x: Int): Boolean {
        return when (cursor.type) {
            CursorMode.Line -> cursor.ints[0] == y
            CursorMode.Column -> cursor.ints[0] == x
            CursorMode.Channel -> this.line_data[y].channel == cursor.ints[0]
            CursorMode.Range -> {
                val xrange = min(cursor.ints[1], cursor.ints[3]) .. max(cursor.ints[1], cursor.ints[3])
                val yrange = min(cursor.ints[0], cursor.ints[2]) .. max(cursor.ints[0], cursor.ints[2])
                yrange.contains(y) && xrange.contains(x)
            }
            CursorMode.Unset,
            CursorMode.Single -> false
        }
    }

    fun is_leaf_selected(cursor: CacheCursor, y: Int, x: Int, path: List<Int>): Boolean {
        if (cursor.type != CursorMode.Single) return false

        return cursor.ints[0] == y && cursor.ints[1] == x && path == cursor.ints.subList(2, cursor.ints.size)

    }

    fun swap_line_cells(y_a: Int, y_b: Int) {
        if (this.ui_lock.is_locked()) return
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

    fun clear_instrument_names() {
        if (this.ui_lock.is_locked()) return
        this.instrument_names.clear()
    }
    fun set_instrument_names(channel: Int, names: List<Pair<String, Int>>?) {
        if (this.ui_lock.is_locked()) return
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
}
