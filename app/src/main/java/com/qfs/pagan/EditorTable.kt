package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.view.ContextThemeWrapper
import kotlin.math.roundToInt
import com.qfs.pagan.OpusLayerInterface as OpusManager

class EditorTable(context: Context, attrs: AttributeSet): TableLayout(context, attrs) {
    data class Coordinate(var y: Int, var x: Int)

    // Scroll Locks
    private var _label_scroll_locked = false
    private var _main_scroll_locked = false

    private val _column_width_map = mutableListOf<MutableList<Int>>()
    private val _column_width_maxes = mutableListOf<Int>()
    val _inv_column_map = HashMap<Int, Int>() // x position by number of leaf-widths:: actual column
    //private val _row_height_map = mutableListOf<Int>()
    val column_label_container = ColumnLabelContainer(this)
    private val _line_label_layout = LineLabelColumnLayout(this)
    private var _scroll_view = CompoundScrollView(this)
    private val _top_row = TableRow(context)
    private val _bottom_row = TableRow(context)
    private val _spacer = CornerView(context)

    init {
        this._top_row.addView(this._spacer)
        this._top_row.addView(this.column_label_container)

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

        this._bottom_row.addView(LinearLayout(ContextThemeWrapper(context, R.style.column)))

        (this._bottom_row.getChildAt(0) as ViewGroup).layoutParams.width = MATCH_PARENT
        (this._bottom_row.getChildAt(0) as ViewGroup).layoutParams.height = WRAP_CONTENT
        (this._bottom_row.getChildAt(0) as ViewGroup).addView(this._line_label_layout)

        this._bottom_row.addView(this._scroll_view)

        this.addView(this._top_row)
        this.addView(this._bottom_row)

        this._top_row.layoutParams.width = MATCH_PARENT
        this._top_row.layoutParams.height = WRAP_CONTENT

        this._bottom_row.layoutParams.width = MATCH_PARENT
        this._bottom_row.layoutParams.height = MATCH_PARENT

        this._spacer.layoutParams.width = MATCH_PARENT
        this._spacer.layoutParams.height = MATCH_PARENT

        this._line_label_layout.layoutParams.width = WRAP_CONTENT
        this._line_label_layout.layoutParams.height = MATCH_PARENT

        (this._scroll_view.layoutParams as LinearLayout.LayoutParams).weight = 1F
        this._scroll_view.layoutParams.width = 0
        this._scroll_view.layoutParams.height = MATCH_PARENT

        (this.column_label_container.layoutParams as LinearLayout.LayoutParams).weight = 1F
        this.column_label_container.layoutParams.width = 0
        this.column_label_container.layoutParams.height = WRAP_CONTENT
    }

    fun get_column_from_leaf(x: Int): Int {
        return this._inv_column_map[x] ?: 0
    }

    fun get_scroll_view(): CompoundScrollView {
        return this._scroll_view
    }
    fun clear() {
        this.get_activity().runOnUiThread {
            this._scroll_view.column_container.clear()
            this.column_label_container.clear()
            this._line_label_layout.clear()
        }
    }

    fun setup(height: Int, width: Int) {
        // NOTE: Needs column map initialized first

        for (beat in 0 until width) {
            this.column_label_container.add_column(beat)
        }
        this._line_label_layout.insert_labels(0, height)

        this._scroll_view.column_container.add_columns(0, width)
    }

    fun new_row(y: Int) {
        this._scroll_view.column_container.insert_row(y)

        this._line_label_layout.insert_label(y)
    }

    fun remove_rows(y: Int, count: Int) {
        this._scroll_view.column_container.remove_rows(y, count)
        this._line_label_layout.remove_labels(y, count)
    }

    fun new_column(index: Int) {
        this._scroll_view.column_container.add_column(index)
        this.column_label_container.add_column(index)
    }

    fun remove_column(index: Int) {
        this.column_label_container.remove_column(index)
        this._scroll_view.column_container.remove_column(index)
    }

    fun notify_cell_changes(cell_coords: List<Coordinate>, state_only: Boolean = false) {
        // TODO: This may need optimization
        for (coord in cell_coords) {
            this._scroll_view.column_container.notify_cell_changed(coord.y, coord.x, state_only)
        }
    }

    fun notify_column_changed(x: Int, state_only: Boolean = false) {
        this._scroll_view.column_container.notify_column_changed(x, state_only)
        this.column_label_container.notify_column_changed(x, state_only)
    }

    fun notify_row_changed(y: Int, state_only: Boolean = false) {
        this._line_label_layout.notify_item_changed(y)
        this._scroll_view.column_container.notify_row_change(y, state_only)
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
        this._line_label_layout.notify_item_changed(y)
    }

    fun update_column_label(x: Int) {
        this.column_label_container.notify_column_changed(x)
    }

    private fun _align_column_labels() {
        val scroll_container_offset = this._scroll_view.scrollX
        val min_leaf_width = resources.getDimension(R.dimen.base_leaf_width).roundToInt()
        val reduced_x = scroll_container_offset / min_leaf_width
        val column_position = this.get_column_from_leaf(reduced_x)
        val column = this._scroll_view.column_container.get_column(column_position)

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
        val box_width = this._scroll_view.measuredWidth

        val base_width = this.resources.getDimension(R.dimen.base_leaf_width)
        val max_width = (this._column_width_maxes[x] * base_width).toInt()

        val offset = if (max_width >= box_width) {
            (box_width - max_width) / 2
        } else {
            0
        }

        val pixel_x = (this._scroll_view.column_container.get_column(x).x).toInt()
        this._main_scroll_locked = true
        this._scroll_view.scrollTo(pixel_x + offset, 0)
        this._main_scroll_locked = false

        this._label_scroll_locked = true
        this.column_label_container.scrollTo(pixel_x + offset, 0)
        this._label_scroll_locked = false
    }

    private fun _scroll_to_x(x: Int, offset: Float = 0F, offset_width: Float = 1F) {
        val box_width = this._scroll_view.measuredWidth

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

        if (x in visible_range) {
            val target_column = this._scroll_view.column_container.get_column(x)
            if (target_column == null) {
                // Shouldn't be Reachable
                return
            } else if (target_column.x + target_width + target_offset > box_width + this._scroll_view.scrollX) {
                subdiv_state[POSITION_TO_RIGHT] = true
                subdiv_state[POSITION_ON_SCREEN] = target_column.x + target_offset < box_width + this._scroll_view.scrollX
                subdiv_state[POSITION_TO_LEFT] = target_column.x + target_offset < this._scroll_view.scrollX
            } else if (target_column.x + target_width + target_offset > this._scroll_view.scrollX) {
                subdiv_state[POSITION_ON_SCREEN] = true
                subdiv_state[POSITION_TO_LEFT] = target_column.x + target_offset < this._scroll_view.scrollX
            } else {
                subdiv_state[POSITION_TO_LEFT] = true
            }


            if (target_column.x > box_width + this._scroll_view.scrollX) {
                column_state[POSITION_TO_RIGHT] = true
            } else if (target_column.x > this._scroll_view.scrollX) {
                column_state[POSITION_ON_SCREEN] = true
                column_state[POSITION_TO_RIGHT] = target_column.x + max_width > box_width + this._scroll_view.scrollX
            } else {
                column_state[POSITION_TO_LEFT] = true
                column_state[POSITION_ON_SCREEN] = target_column.x + max_width > this._scroll_view.scrollX
                column_state[POSITION_TO_RIGHT] = target_column.x + max_width > box_width + this._scroll_view.scrollX
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
            0b0100 -> (box_width - target_width) / 2

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

        this._main_scroll_locked = true
        val calc_x = (this._scroll_view.column_container.get_column(x).x).toInt()
        // this._scroll_view.scrollTo(calc_x + adj_offset, this._scroll_view.scrollY)
        this._scroll_view.scrollTo(calc_x - adj_offset, 0)
        this._main_scroll_locked = false

        this._label_scroll_locked = true
        this.column_label_container.scrollTo(calc_x - adj_offset, 0)
        this._label_scroll_locked = false
    }

    // TODO: Create row_height_map so OpusManager isn't accessed here
    private fun _scroll_to_y(y: Int) {
        val row_height = (resources.getDimension(R.dimen.line_height)).toInt()
        val control_row_height = resources.getDimension(R.dimen.ctl_line_height).toInt()
        var target_y = 0
        var count = 0
        var working_row_height = row_height
        val opus_manager = this.get_opus_manager()
        val channels = opus_manager.get_all_channels()
        for (i in channels.indices) {
            val channel = channels[i]
            for (j in channel.lines.indices) {
                val line = channel.lines[j]
                if (count >= y) {
                    break
                }

                target_y += row_height
                working_row_height = row_height
                count += 1
                for ((_, controller) in line.controllers.get_all()) {
                    if (!controller.visible) {
                        continue
                    }
                    if (count >= y) {
                        break
                    }

                    target_y += control_row_height
                    working_row_height = control_row_height
                    count += 1
                }
            }
            for ((_, controller) in channel.controllers.get_all()) {
                if (!controller.visible) {
                    continue
                }
                if (count >= y) {
                    break
                }

                target_y += control_row_height
                working_row_height = control_row_height
                count += 1
            }
        }

        for ((_, controller) in this.get_opus_manager().controllers.get_all()) {
            if (!controller.visible) {
                continue
            }
            if (count >= y) {
                break
            }
            target_y += control_row_height
            working_row_height = control_row_height
            count += 1
        }

        if (this._scroll_view.measuredHeight + this._scroll_view.scrollY < target_y + working_row_height) {
            val adj_target_y = target_y - (this._scroll_view.measuredHeight - (working_row_height * 1.5).toInt())
            this._line_label_layout.scrollTo(0, adj_target_y)
            this._scroll_view.scrollTo(0, adj_target_y)
        } else if (target_y < this._scroll_view.scrollY) {
            this._line_label_layout.scrollTo(0, target_y)
            this._scroll_view.scrollTo(0, target_y)
        }
    }

    fun get_scroll_offset(): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        // NOTE: Used to be based on recycler view positions. So now we just set the Pairs to Pair(0, scroll[X/Y])

        return Pair(
            Pair(0, this._scroll_view.scrollX),
            Pair(0, this._scroll_view.vertical_scroll_view.scrollY)
        )
    }

    fun precise_scroll(x_coarse: Int = 0, x_fine: Int = 0, y_coarse: Int? = null, y_fine: Int? = null) {
        // TODO
        // val main_lm = (this.get_column_recycler().layoutManager!! as LinearLayoutManager)
        // main_lm.scrollToPositionWithOffset(x_coarse, x_fine)

        // val column_label_lm = (this.column_label_recycler.layoutManager!! as LinearLayoutManager)
        // column_label_lm.scrollToPositionWithOffset(x_coarse, x_fine)

        // if (y_coarse != null) {
        //     val line_height = (resources.getDimension(R.dimen.line_height)).toInt()
        //     this._scroll_view.scrollTo(0, (line_height * y_coarse) + (y_fine ?: 0))
        // }
    }

    fun get_first_visible_column_index(): Int {
        val scroll_container_offset = this._scroll_view.scrollX
        val min_leaf_width = resources.getDimension(R.dimen.base_leaf_width).roundToInt()
        val reduced_x = scroll_container_offset / min_leaf_width
        val column_position = this.get_column_from_leaf(reduced_x)
        return column_position
    }

    fun get_last_visible_column_index(): Int {
        val scroll_container_offset = this._scroll_view.scrollX + this._scroll_view.width
        val min_leaf_width = resources.getDimension(R.dimen.base_leaf_width).roundToInt()
        val reduced_x = scroll_container_offset / min_leaf_width
        val column_position = this.get_column_from_leaf(reduced_x)
        return column_position
    }

    fun get_line_label_layout(): LineLabelColumnLayout {
        return this._line_label_layout
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
