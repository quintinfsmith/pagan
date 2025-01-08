package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusManagerCursor
import kotlin.math.roundToInt
import com.qfs.pagan.OpusLayerInterface as OpusManager

class EditorTable(context: Context, attrs: AttributeSet): LinearLayout(context, attrs) {
    data class Coordinate(var y: Int, var x: Int)
    data class Rectangle(var x: Int, var y: Int, var width: Int, var height: Int)
    enum class RowType {
        Standard,
        Control
    }

    private val _column_width_map = mutableListOf<MutableList<Int>>()
    private val _column_width_maxes = mutableListOf<Int>()
    val _inv_column_map = HashMap<Int, Int>() // x position by number of leaf-widths:: actual column
    //private val _row_height_map = mutableListOf<Int>()
    val line_label_layout = LineLabelColumnLayout(this)
    internal var table_ui = TableUI(this)
    private val _spacer = CornerView(context)
    private val _first_column = LinearLayout(context, attrs)

    init {
        this._spacer.getChildAt(0).setOnClickListener {
            val fragment = this.get_activity().get_active_fragment()
            if (fragment is FragmentEditor) {
                fragment.shortcut_dialog()
            }
        }

        this._spacer.getChildAt(0).setOnLongClickListener {
            this.get_opus_manager().cursor_select_column(0)
            true
        }

        this.addView(this._first_column)
        this._first_column.orientation = VERTICAL
        this._first_column.addView(this._spacer)
        this._first_column.addView(this.line_label_layout)

        this.addView(this.table_ui)

        this._first_column.layoutParams.width = WRAP_CONTENT
        this._first_column.layoutParams.height = MATCH_PARENT

        this._spacer.layoutParams.width = MATCH_PARENT
        this._spacer.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()

        (this.line_label_layout.layoutParams as LinearLayout.LayoutParams).weight = 1F
        this.line_label_layout.layoutParams.height = 0
        this.line_label_layout.layoutParams.width = WRAP_CONTENT

        (this.table_ui.layoutParams as LinearLayout.LayoutParams).weight = 1F
        this.table_ui.layoutParams.width = 0
        this.table_ui.layoutParams.height = MATCH_PARENT
    }

    fun get_column_from_leaf(x: Int, fallback: Int = 0): Int {
        return this._inv_column_map[x] ?: fallback
    }

    fun get_visible_row_from_pixel(y: Float): Int? {
        val line_height = resources.getDimension(R.dimen.line_height)
        val ctl_line_height = resources.getDimension(R.dimen.ctl_line_height)
        val channel_gap_size = resources.getDimension(R.dimen.channel_gap_size)
        var check_y = line_height // consider column labels
        var output = 0
        val opus_manager = this.get_opus_manager()
        val channels = opus_manager.get_all_channels()

        if (y - this.table_ui.vertical_scroll_view.scrollY < line_height) {
            return -1
        }

        for (i in channels.indices) {
            val channel = channels[i]
            if (!channel.visible) {
                continue
            }
            for (j in channel.lines.indices) {
                val line = channel.lines[j]

                check_y += line_height
                if (check_y >= y) {
                    return output
                } else {
                    output += 1
                }

                for ((type, controller) in line.controllers.get_all()) {
                    if (!controller.visible) {
                        continue
                    }
                    check_y += ctl_line_height
                    if (check_y >= y) {
                        return output
                    } else {
                        output += 1
                    }
                }
            }
            for ((type, controller) in channel.controllers.get_all()) {
                if (!controller.visible) {
                    continue
                }
                check_y += ctl_line_height
                if (check_y >= y) {
                    return output
                } else {
                    output += 1
                }
            }

            check_y += channel_gap_size
            if (check_y >= y) {
                return null
            }
        }

        for ((type, controller) in opus_manager.controllers.get_all()) {
            if (!controller.visible) {
                continue
            }
            check_y += ctl_line_height
            if (check_y >= y) {
                return output
            } else {
                output += 1
            }
        }

        return output
    }

    fun get_scroll_view(): TableUI {
        return this.table_ui
    }
    fun clear() {
        this.get_activity().runOnUiThread {
            this.line_label_layout.clear()
            this.table_ui.clear()
            this.reset_table_size()
        }
    }

    fun setup(height: Int, width: Int) {
        // NOTE: Needs column map initialized first
        this.line_label_layout.insert_labels(0, height)
        this.table_ui.add_columns(0, width)
        this.reset_table_size()
    }

    fun reset_table_size() {
        val (pix_width, pix_height) = this._calculate_table_size()
        this.table_ui.set_size(pix_width, pix_height + resources.getDimension(R.dimen.line_height).toInt())
    }

    fun new_row(y: Int) {
        this.reset_table_size()
        this.line_label_layout.insert_label(y)
        this.table_ui.insert_row(y)
    }

    fun remove_rows(y: Int, count: Int) {
        this.reset_table_size()
        this.line_label_layout.remove_labels(y, count)
        this.table_ui.remove_rows(y, count)
    }

    fun new_column(index: Int) {
        this.reset_table_size()
        this.table_ui.add_column(index)
    }

    fun remove_column(index: Int) {
        this.reset_table_size()
        this.table_ui.remove_column(index)
    }

    fun notify_cell_changes(cell_coords: List<Coordinate>, state_only: Boolean = false) {
        // TODO: This may need optimization
        for (coord in cell_coords) {
            this.table_ui.notify_cell_changed(coord.y, coord.x, state_only)
        }
    }

    fun notify_column_changed(x: Int, state_only: Boolean = false) {
        if (!state_only) {
            this.reset_table_size()
        }
        this.table_ui.notify_column_changed(x, state_only)
    }

    fun notify_row_changed(y: Int, state_only: Boolean = false) {
        if (!state_only) {
            this.reset_table_size()
        }
        this.line_label_layout.notify_item_changed(y)
        this.table_ui.notify_row_change(y, state_only)
    }

    fun get_column_map_size(): Int {
        return this._column_width_map.size
    }

    fun recalculate_column_max(x: Int) {
        this._column_width_maxes[x] = this._column_width_map[x].max()
        this._update_inv_column_map(x)
    }

    fun recalculate_column_maxes(): List<Int> {
        val output = mutableListOf<Int>()
        for (x in 0 until this._column_width_map.size) {
            val new_max = this._column_width_map[x].max()
            if (new_max != this._column_width_maxes[x]) {
                output.add(new_max)
                this._column_width_maxes[x] = new_max
            }
        }
        if (output.isNotEmpty()) {
            this._update_inv_column_map(output.min())
        }
        return output
    }

    fun get_column_width(column: Int): Int {
        return this._column_width_maxes[column]
    }

    fun get_activity(): MainActivity {
        return this.context as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return (this.context as MainActivity).get_opus_manager()
    }

    fun update_line_label(y: Int) {
        this.line_label_layout.notify_item_changed(y)
    }

    fun update_column_label(x: Int) {
        // TODO?
        //this.column_label_container.notify_column_changed(x)
    }

    private fun _align_column_labels() {
        val scroll_container_offset = this.table_ui.scrollX
        val min_leaf_width = resources.getDimension(R.dimen.base_leaf_width).roundToInt()
        val reduced_x = scroll_container_offset / min_leaf_width
        val column_position = this.get_column_from_leaf(reduced_x)
        //val column = this._scroll_view.column_container.get_column(column_position)

        // TODO
        //val label_position = layout_manager_labels.findFirstVisibleItemPosition()
        //val label = layout_manager_labels.findViewByPosition(label_position)
        //if (label?.x != column?.x) {
        //    val new_offset = column?.x?.roundToInt() ?: 0
        //    this._main_scroll_locked = true
        //    layout_manager_columns.scrollToPositionWithOffset(column_position, new_offset)
        //    this._main_scroll_locked = false
        //    this._label_scroll_locked = true
        //    layout_manager_labels.scrollToPositionWithOffset(column_position, new_offset)
        //    this._label_scroll_locked = false
        //}
    }

    fun scroll_to_position(x: Int? = null, y: Int? = null, offset: Float = 0f, offset_width: Float = 1f, force: Boolean = false) {
        if (x != null) {
            if (x >= this.get_opus_manager().beat_count) {
                return
            } else if (! force) {
                this._scroll_to_x(x, offset, offset_width)
            } else {
                this._forced_scroll_to_beat(x)
            }
        }

        if (y != null) {
            this._scroll_to_y(y)
        }
    }

    private fun _forced_scroll_to_beat(x: Int) {
        val box_width = this.table_ui.measuredWidth

        val base_width = this.resources.getDimension(R.dimen.base_leaf_width)
        val max_width = (this._column_width_maxes[x] * base_width).toInt()

        val offset = if (max_width >= box_width) {
            (box_width - max_width) / 2
        } else {
            0
        }

        val pixel_x = this._column_width_maxes.subList(0, x).sum() * base_width.toInt()
        this.table_ui.scroll(pixel_x + offset, null)
    }

    fun get_column_offset(x: Int): Int {
        val base_width = this.resources.getDimension(R.dimen.base_leaf_width)
        return this._column_width_maxes.subList(0, x).sum() * base_width.toInt()
    }

    fun get_column_rect(x: Int): Rectangle? {
        if (this._column_width_maxes.size <= x) {
            return null
        }

        val base_width = this.resources.getDimension(R.dimen.base_leaf_width)
        return Rectangle(
            this.get_column_offset(x),
            0,
            this._column_width_maxes[x] * base_width.toInt(),
            1 // TODO
        )
    }

    private fun _scroll_to_x(x: Int, offset: Float = 0F, offset_width: Float = 1F) {
        val box_width = this.table_ui.measuredWidth

        val base_width = this.resources.getDimension(R.dimen.base_leaf_width)
        val max_width = (this._column_width_maxes[x] * base_width).toInt()
        val target_width = (this._column_width_maxes[x] * this.resources.getDimension(R.dimen.base_leaf_width) * offset_width).toInt()
        val visible_range = this.get_first_visible_column_index() .. this.get_last_visible_column_index()
        val target_offset = (max_width * offset).toInt()
        val POSITION_ON_SCREEN: Int = 0
        val POSITION_TO_RIGHT: Int = 1
        val POSITION_TO_LEFT: Int = 2
        val FITS_ON_SCREEN: Int = 3

        val column_state = Array(4) { false }
        val subdiv_state = Array(4) { false }

        subdiv_state[FITS_ON_SCREEN] = target_width <= box_width
        column_state[FITS_ON_SCREEN] = max_width <= box_width

        val target_rect = this.get_column_rect(x) ?: return
        if (x in visible_range) {
            if (target_rect == null) {
                // Shouldn't be Reachable
                return
            } else if (target_rect.x + target_width + target_offset > box_width + this.table_ui.scrollX) {
                subdiv_state[POSITION_TO_RIGHT] = true
                subdiv_state[POSITION_ON_SCREEN] = target_rect.x + target_offset < box_width + this.table_ui.scrollX
                subdiv_state[POSITION_TO_LEFT] = target_rect.x + target_offset < this.table_ui.scrollX
            } else if (target_rect.x + target_width + target_offset > this.table_ui.scrollX) {
                subdiv_state[POSITION_ON_SCREEN] = true
                subdiv_state[POSITION_TO_LEFT] = target_rect.x + target_offset < this.table_ui.scrollX
            } else {
                subdiv_state[POSITION_TO_LEFT] = true
            }


            if (target_rect.x > box_width + this.table_ui.scrollX) {
                column_state[POSITION_TO_RIGHT] = true
            } else if (target_rect.x > this.table_ui.scrollX) {
                column_state[POSITION_ON_SCREEN] = true
                column_state[POSITION_TO_RIGHT] = target_rect.x + max_width > box_width + this.table_ui.scrollX
            } else {
                column_state[POSITION_TO_LEFT] = true
                column_state[POSITION_ON_SCREEN] = target_rect.x + max_width > this.table_ui.scrollX
                column_state[POSITION_TO_RIGHT] = target_rect.x + max_width > box_width + this.table_ui.scrollX
            }
        } else if (x > visible_range.last) {
            column_state[POSITION_TO_RIGHT] = true
            subdiv_state[POSITION_TO_RIGHT] = true
        } else {
            column_state[POSITION_TO_LEFT] = true
            subdiv_state[POSITION_TO_LEFT] = true
        }

        var subdiv_int = 0
        var column_int = 0
        var working_offset = 1
        for (i in 0 until 4) {
            subdiv_int += working_offset * if (subdiv_state[i]) { 1 } else { 0 }
            column_int += working_offset * if (column_state[i]) { 1 } else { 0 }
            working_offset *= 2
        }

        // FITS, LEFT, RIGHT, ON SCREEN
        val adj_offset = when (subdiv_int) {
            // Center the section
            0b0011,
            0b0101,
            0b0010,
            0b0100 -> {
                (box_width - target_width) / 2
            }

            // Try to scroll the column onto screen, then the section
            0b1010 -> {
                if (column_state[FITS_ON_SCREEN]) {
                    box_width - max_width
                } else {
                    (0 - target_offset) + ((box_width - target_width) / 2)
                }
            }

            // Align the end of the section with the end of the screen
            0b1011 -> {
                box_width - target_offset - target_width
            }


            // Try to scroll the column onto screen, then the section
            0b1100 -> {
                if (column_state[FITS_ON_SCREEN]) {
                    0
                } else {
                    box_width - target_offset - target_width - ((box_width - target_width) / 2)
                }
            }

            // Align the start of the section with the start of the screen
            0b1101 -> 0 - target_offset

            0b0111,   // Overflowing,
            0b1001 -> { // No need to scroll
                this._align_column_labels()
                return
            }
            // 0b0000 -> { }   // Invalid
            // 0b0001 -> { }   // Invalid
            // 0b0110 -> { }   // Invalid
            // 0b1000 -> { }   // Invalid
            // 0b1110 -> { }   // Invalid
            // 0b1111 -> { }   // Invalid
            else -> { return }     // Unreachable
        }

        this.table_ui.scroll(target_rect.x - adj_offset, null)
    }

    fun get_row_y_position_and_height(row: Int): Pair<Int, Int> {
        val channel_gap_size = this.resources.getDimension(R.dimen.channel_gap_size).toInt()
        val controller_height = this.resources.getDimension(R.dimen.ctl_line_height).toInt()
        val line_height = this.resources.getDimension(R.dimen.line_height).toInt()

        val opus_manager = this.get_opus_manager()
        val channels = opus_manager.get_all_channels()
        var y = 0
        var working_y_offset = line_height // consider column label offset
        for (i in channels.indices) {
            val channel = channels[i]
            if (!channel.visible) {
                continue
            }
            for (j in channel.lines.indices) {
                val line = channel.lines[j]
                if (y == row) {
                    return Pair(working_y_offset, line_height)
                } else {
                    working_y_offset += line_height
                    y += 1
                }

                for ((type, controller) in line.controllers.get_all()) {
                    if (!controller.visible) {
                        continue
                    }
                    if (y == row) {
                        return Pair(working_y_offset, controller_height)
                    } else {
                        working_y_offset += controller_height
                        y += 1
                    }
                }
            }
            for ((type, controller) in channel.controllers.get_all()) {
                if (!controller.visible) {
                    continue
                }
                if (y == row) {
                    return Pair(working_y_offset, controller_height)
                } else {
                    working_y_offset += controller_height
                    y += 1
                }
            }
            working_y_offset += channel_gap_size
        }

        for ((type, controller) in opus_manager.controllers.get_all()) {
            if (!controller.visible) {
                continue
            }
            if (y == row) {
                return Pair(working_y_offset, controller_height)
            } else {
                working_y_offset += controller_height
                y += 1
            }
        }
        return Pair(working_y_offset, 0)
    }

    fun force_scroll_to_cursor_vertical() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val row = when (cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Line -> {
                when (cursor.ctl_level) {
                    null -> opus_manager.get_visible_row_from_ctl_line(
                        opus_manager.get_actual_line_index(
                            opus_manager.get_instrument_line_index(
                                cursor.channel,
                                cursor.line_offset
                            )
                        )
                    )

                    CtlLineLevel.Line -> {
                        opus_manager.get_visible_row_from_ctl_line_line(
                            cursor.ctl_type!!,
                            cursor.channel,
                            cursor.line_offset
                        )
                    }
                    CtlLineLevel.Channel -> {
                        opus_manager.get_visible_row_from_ctl_line_channel(
                            cursor.ctl_type!!,
                            cursor.channel
                        )
                    }
                    CtlLineLevel.Global -> {
                        opus_manager.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                    }
                }
            }
            // No need to force_scroll in these modes
            OpusManagerCursor.CursorMode.Range,
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Unset -> null
        } ?: return

        this._scroll_to_y(row)
    }

    // TODO: Create row_height_map so OpusManager isn't accessed here
    private fun _scroll_to_y(row: Int) {
        val (target_y, row_height) = this.get_row_y_position_and_height(row)
        // TODO: This feels sloppy but i'm too tired to think of a sufficiently cleaner answer
        val vertical_scroll_view = this.get_scroll_view().vertical_scroll_view
        if (vertical_scroll_view.measuredHeight + vertical_scroll_view.scrollY < target_y + row_height) {
            val adj_y = (target_y + row_height) - vertical_scroll_view.measuredHeight
            this.line_label_layout.scrollTo(0, adj_y)
            this.table_ui.scroll(null, adj_y)
        } else if (target_y < vertical_scroll_view.scrollY) {
            this.line_label_layout.scrollTo(0, target_y)
            this.table_ui.scroll(null, target_y)
        }
    }

    fun get_scroll_offset(): Pair<Int, Int> {
        // NOTE: Used to be based on recycler view positions. So now we just set the Pairs to Pair(0, scroll[X/Y])

        return Pair(
            this.table_ui.scrollX,
            this.table_ui.vertical_scroll_view.scrollY
        )
    }

    fun precise_scroll(scroll_x: Int = 0, scroll_y: Int? = null) {
        this.table_ui.scroll(scroll_x, scroll_y)
    }

    fun get_first_visible_column_index(): Int {
        val scroll_container_offset = this.table_ui.scrollX
        val min_leaf_width = resources.getDimension(R.dimen.base_leaf_width).roundToInt()
        val reduced_x = scroll_container_offset / min_leaf_width
        val column_position = this.get_column_from_leaf(reduced_x)
        return column_position
    }

    fun get_last_visible_column_index(): Int {
        val scroll_container_offset = this.table_ui.get_scroll_x()
        val min_leaf_width = resources.getDimension(R.dimen.base_leaf_width).toInt()
        val reduced_x = scroll_container_offset / min_leaf_width
        val column_position = this.get_column_from_leaf(reduced_x, this._column_width_map.size - 1)
        return column_position
    }

    fun get_line_label_layout(): LineLabelColumnLayout {
        return this.line_label_layout
    }

    // width map functions ---------------------------------------
    fun clear_column_map() {
        this._column_width_map.clear()
        this._column_width_maxes.clear()
        this._inv_column_map.clear()
    }

    fun swap_mapped_lines(line_a: Int, line_b: Int) {
        for (i in 0 until this._column_width_map.size) {
            val tmp = this._column_width_map[i][line_a]
            this._column_width_map[i][line_a] = this._column_width_map[i][line_b]
            this._column_width_map[i][line_b] = tmp
        }
    }

    fun remove_mapped_lines(y: Int, count: Int): List<Int> {
        val output = mutableListOf<Int>()
        for (j in 0 until this._column_width_map.size) {
            this._column_width_map[j] = this._column_width_map[j].filterIndexed { i: Int, _: Int ->
                !(i >= y && i < y + count)
            }.toMutableList()

            val new_max = this._column_width_map[j].max()
            if (new_max != this._column_width_maxes[j]) {
                output.add(j)
                this._column_width_maxes[j] = this._column_width_map[j].max()
            }
        }
        if (output.isNotEmpty()) {
            this._update_inv_column_map(output.min())
        }
        return output
    }

    fun remove_mapped_column(x: Int) {
        this._column_width_map.removeAt(x)
        this._column_width_maxes.removeAt(x)
        this._update_inv_column_map(x)
    }

    fun add_column_to_map(x: Int, column: List<Int>) {
        this._column_width_map.add(x, column.toMutableList())
        this._column_width_maxes.add(x, if (column.isNotEmpty()) {
            column.max()
        } else {
            1
        })

        this._update_inv_column_map(x)
    }

    private fun _update_inv_column_map(x: Int = 0) {
        var working_leaf_x = this._column_width_maxes.subList(0, x).sum()

        for (k in (this._inv_column_map.keys).toList()) {
            if (k >= working_leaf_x) {
                this._inv_column_map.remove(k)!!
            }
        }

        for (column in x until this._column_width_maxes.size) {
            val w = this._column_width_maxes[column]
            for (i in 0 until w) {
                this._inv_column_map[working_leaf_x++] = column
            }
        }
    }

    fun add_line_to_map(y: Int, line: List<Int>): List<Int> {
        val output = mutableListOf<Int>()
        for (x in line.indices) {
            this._column_width_map[x].add(y, line[x])
            val new_max = this._column_width_map[x].max()
            if (new_max != this._column_width_maxes[x]) {
                this._column_width_maxes[x] = new_max
                output.add(x)
            }
        }
        if (output.isNotEmpty()) {
            this._update_inv_column_map(output.min())
        }
        return output
    }

    fun set_mapped_width(y: Int, x: Int, width: Int): Boolean {
        if (x >= this._column_width_map.size || this._column_width_map[x].size <= y) {
            return false
        }

        val is_trivial = this._column_width_map[x][y] == width
        if (! is_trivial) {
            this._column_width_map[x][y] = width
            this._update_inv_column_map(x)
        }

        return !is_trivial
    }


    fun _calculate_table_size(): Pair<Int, Int> {
        val base_width = this.resources.getDimension(R.dimen.base_leaf_width).toInt()
        val channel_gap_size = this.resources.getDimension(R.dimen.channel_gap_size).toInt()
        val controller_height = this.resources.getDimension(R.dimen.ctl_line_height).toInt()
        val line_height = this.resources.getDimension(R.dimen.line_height).toInt()

        val size = if (this._inv_column_map.keys.isNotEmpty()) {
            this._inv_column_map.keys.max() + 1
        } else {
            0
        }

        val width = size * base_width
        var vis_channel_count = 0
        var controller_count = 0
        var line_count = 0

        val opus_manager = this.get_opus_manager()
        for (channel in opus_manager.get_all_channels()) {
            if (!channel.visible) {
                continue
            }
            for (line in channel.lines) {
                line_count += 1
                for ((_, controller) in line.controllers.get_all()) {
                    if (controller.visible) {
                        controller_count += 1
                    }
                }
            }
            vis_channel_count += 1
            for ((_, controller) in channel.controllers.get_all()) {
                if (controller.visible) {
                    controller_count += 1
                }
            }
        }

        for ((_, controller) in opus_manager.controllers.get_all()) {
            if (controller.visible) {
                controller_count += 1
            }
        }

        val height = (vis_channel_count * channel_gap_size) + (line_count * line_height) + (controller_count * controller_height)
        return Pair(width, height)
    }

    fun get_column_width_map(): List<List<Int>> {
        return this._column_width_map
    }
}
