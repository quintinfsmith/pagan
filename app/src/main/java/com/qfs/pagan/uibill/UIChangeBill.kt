package com.qfs.pagan.uibill

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
class UIChangeBill {
    val ui_lock = UILock()
    private val working_path = mutableListOf<Int>()

    enum class SelectionLevel {
        Unselected,
        Primary,
        Secondary
    }

    data class LineData(var channel: Int?, var offset: Int?, var ctl_type: EffectType?, var selected: SelectionLevel)
    data class ColumnData(var is_tagged: Boolean, var selected: SelectionLevel)
    data class ChannelData(var percussion: Boolean, var instrument: Pair<Int, Int>)
    class CacheCursor(var type: CursorMode, vararg ints: Int)

    var project_name: String? = null
    var beat_count: Int = 0
    val line_data: MutableList<LineData> = mutableListOf()
    val column_data: MutableList<ColumnData> = mutableListOf()
    val cell_map: MutableList<MutableList<ReducibleTree<out OpusEvent>>> = mutableListOf()
    val channel_data: MutableList<ChannelData> = mutableListOf()

    var active_event: OpusEvent? = null
    var active_cursor: CacheCursor? = null
    var project_exists: Boolean = false
    var active_percussion_names = HashMap<Int, HashMap<Int, String>>()
    var blocker_leaf: List<Int>? = null

    fun clear() {
        this.project_name = null
        this.beat_count = 0
        this.active_event = null
        this.active_cursor = null
        this.project_exists = false
        this.blocker_leaf = null

        this.line_data.clear()
        this.column_data.clear()
        this.cell_map.clear()
        this.channel_data.clear()
        this.active_percussion_names.clear()
    }

    fun queue_cell_state_changes(coordinates: List<EditorTable.Coordinate>) {
        for (coordinate in coordinates) {
            //this.queue_cell_change(coordinate)
            TODO()
        }
    }

    fun queue_cell_change(coordinate: EditorTable.Coordinate, tree: ReducibleTree<out OpusEvent>) {
        this.cell_map[coordinate.y][coordinate.x] = tree
    }

    fun queue_column_change(column: Int, is_tagged: Boolean) {
        this.column_data[column].is_tagged = is_tagged
    }

    fun queue_new_row(y: Int, cells: MutableList<ReducibleTree<out OpusEvent>>, channel: Int?, line_offset: Int?, ctl_type: EffectType?) {
        this.line_data.add(y, LineData(channel, line_offset, ctl_type, SelectionLevel.Unselected))
        this.cell_map.add(y, cells)
    }

    fun queue_enable_delete_and_copy_buttons() {
        this.project_exists = true
        // activity.findViewById<View>(R.id.btnDeleteProject).isEnabled = true
        // activity.findViewById<View>(R.id.btnCopyProject).isEnabled = true
    }

    fun queue_config_drawer_redraw_export_button() {
        TODO()
    }

    fun queue_line_label_refresh(y: Int, is_percussion: Boolean?, channel: Int?, offset: Int?, control_type: EffectType? = null) {
        this.line_data[y].let { line_data ->
            line_data.channel = channel
            line_data.offset = offset
            line_data.ctl_type = control_type
        }
    }

    fun queue_row_change(y: Int, state_only: Boolean = false) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(y)
        working_tree.bill.add(
            if (state_only) {
                BillableItem.RowStateChange
            } else {
                BillableItem.RowChange
            }
        )
    }

    fun queue_row_removal(y: Int, count: Int) {
        for (i in 0 until count) {
            this.line_data.removeAt(y)
            this.cell_map.removeAt(y)
        }
    }

    fun queue_add_channel(channel: Int, percussion: Boolean, instrument: Pair<Int, Int>) {
        for (ld in this.line_data) {
            ld.channel?.let {
                if (it >= channel) {
                    ld.channel = it + 1
                }
            }
        }
        this.channel_data.add(channel, ChannelData(percussion, instrument))
    }

    fun queue_remove_channel(channel: Int) {
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

    fun queue_add_column(column: Int, is_tagged: Boolean) {
        this.column_data.add(column, ColumnData(is_tagged, SelectionLevel.Unselected))
    }

    fun queue_remove_column(column: Int) {
        this.column_data.removeAt(column)
    }

    fun queue_refresh_choose_percussion_button(channel: Int, line_offset: Int) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(channel)
        working_tree.int_queue.add(line_offset)
        working_tree.bill.add(BillableItem.PercussionButtonRefresh)
    }

    fun queue_full_refresh(restore_position: Boolean = false) {
        val working_tree = this.get_working_tree(true) ?: return
        working_tree.bill.add(BillableItem.FullRefresh)
    }

    fun queue_force_scroll(y: Int, x: Int, offset: Rational, offset_width: Rational, force: Boolean) {
        // editor_table.scroll_to_position(y = y, x = beat, offset = offset, offset_width = offset_width, force = force)
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(y)
        working_tree.int_queue.add(x)
        working_tree.int_queue.add(offset.numerator)
        working_tree.int_queue.add(offset.denominator)
        working_tree.int_queue.add(offset_width.numerator)
        working_tree.int_queue.add(offset_width.denominator)
        working_tree.int_queue.add(if (force) 1 else 0)
        working_tree.bill.add(BillableItem.ForceScroll)
    }

    fun get_working_tree(force: Boolean = false): Node? {
        // Force is used ONLY to apply FullRefresh
        return if (this.is_full_locked() && ! force) {
            null
        } else {
            this._tree.get(this.working_path)
        }
    }

    fun lock_full() {
        this.ui_lock.lock_full()
        val working_tree = this._tree.get(this.working_path)
        this.working_path.add(working_tree.sub_nodes.size)
        working_tree.new_node()
    }

    fun lock_partial() {
        this.ui_lock.lock_partial()
        val working_tree = this._tree.get(this.working_path)
        this.working_path.add(working_tree.sub_nodes.size)
        working_tree.new_node()
    }

    fun unlock() {
        this.ui_lock.unlock()
        if (this.working_path.isNotEmpty()) {
            this.working_path.removeAt(this.working_path.size - 1)
        }
    }

    fun get_level(): Int {
        return this.ui_lock.get_level()
    }

    fun is_locked(): Boolean {
        return this.ui_lock.is_locked()
    }

    fun is_full_locked(): Boolean {
        return this.ui_lock.is_full_locked()
    }

    fun cancel_most_recent() {
        this._tree.remove_last()
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

    fun set_channel_data(channel_index: Int, percussion: Boolean, instrument: Pair<Int, Int>) {
        this.channel_data[channel_index] = ChannelData(percussion, instrument)
    }

    fun set_project_name(name: String? = null) {
        this.project_name = name
    }

    fun <T: OpusEvent> set_active_event(event: T) {
        this.active_event = event
    }

    fun set_cursor(cursor: CacheCursor) {
        this.active_cursor = cursor
    }

    fun shift_up_percussion_names(channel: Int) {
        val keys = this.active_percussion_names.keys.sorted().reversed()
        for (k in keys) {
            if (k < channel) continue
            this.active_percussion_names[k + 1] = this.active_percussion_names.remove(k)!!
        }
    }

    fun shift_down_percussion_names(channel: Int) {
        val keys = this.active_percussion_names.keys.sorted()
        for (k in keys) {
            if (k > channel) {
                this.active_percussion_names[k - 1] = this.active_percussion_names.remove(k)!!
            } else if (k == channel) {
                this.active_percussion_names.remove(k)
            }
        }
    }

    fun clear_percussion_names() {
        this.active_percussion_names.clear()
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
        while (this.line_data[i].channel == lesser_line_data.channel && this.line_data[i].offset == lesser_line_data.offset) {
            i++
            lesser_line_count++
        }

        i = larger
        while (this.line_data[i].channel == larger_line_data.channel && this.line_data[i].offset == larger_line_data.offset) {
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
}
