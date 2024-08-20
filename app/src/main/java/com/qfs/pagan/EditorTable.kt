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
import androidx.recyclerview.widget.LinearLayoutManager
import com.qfs.pagan.opusmanager.ActiveController
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusChannelAbstract
import com.qfs.pagan.opusmanager.OpusLineAbstract
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.structure.OpusTree
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.qfs.pagan.OpusLayerInterface as OpusManager

class EditorTable(context: Context, attrs: AttributeSet): TableLayout(context, attrs) {
    data class Coordinate(val y: Int, val x: Int)
    val column_label_recycler = ColumnLabelRecycler(context)
    private val _line_label_layout = LineLabelColumnLayout(this)
    private var _scroll_view = CompoundScrollView(this)
    private val _top_row = TableRow(context)
    private val _bottom_row = TableRow(context)
    private val _spacer = CornerView(context)

    // Scroll Locks
    var _label_scroll_locked = false
    var _main_scroll_locked = false

    var needs_setup = true

    private val _queued_cell_notifications = mutableListOf<Coordinate>()
    private val _queued_column_notifications = mutableListOf<Int>()

    companion object {
        // Intentionally Not Enums, So we can use gt/lt comparisons instead of multiple checks
        const val SECTION_OUT_OF_VIEW = 0
        const val SECTION_VIEW_PARTIAL_LEFT = 1
        const val SECTION_VIEW_PARTIAL_RIGHT = 2
        const val SECTION_VIEW_PARTIAL_OVERSIZED = 3
        const val SECTION_VIEW_COMPLETE = 4
    }

    init {
        this._top_row.addView(this._spacer)
        this._top_row.addView(this.column_label_recycler)

        this._spacer.setOnClickListener {
            val fragment = this.get_activity().get_active_fragment()
            if (fragment is FragmentEditor) {
                fragment.shortcut_dialog()
            }
        }

        this._spacer.setOnLongClickListener {
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

        (this.column_label_recycler.layoutParams as LinearLayout.LayoutParams).weight = 1F
        this.column_label_recycler.layoutParams.width = 0

        ColumnLabelAdapter(this)
    }

    fun clear() {
        this.needs_setup = true

        this.get_activity().runOnUiThread {
            (this.get_column_recycler().adapter!! as ColumnRecyclerAdapter).clear()
            (this.column_label_recycler.adapter!! as ColumnLabelAdapter).clear()
            this._line_label_layout.clear()
        }
    }

    fun setup() {
        this._init_column_width_map()
        val opus_manager = this.get_opus_manager()
        val main_adapter = (this.get_column_recycler().adapter as ColumnRecyclerAdapter)
        val column_label_adapter = (this.column_label_recycler.adapter as ColumnLabelAdapter)

        for (beat in 0 until opus_manager.beat_count) {
            column_label_adapter.add_column(beat)
        }

        this._line_label_layout.insert_labels(0, opus_manager.get_visible_master_line_count())

        main_adapter.add_columns(0, opus_manager.beat_count)
        this.needs_setup = false
    }

    fun new_row(y: Int, controller: ActiveController, ignore_ui: Boolean = false) {
        for (i in 0 until this.get_opus_manager().beat_count) {
            val tree = controller.events[i] ?: OpusTree()
            this._column_width_map[i].add(
                y,
                if (tree.is_leaf()) {
                    1
                } else {
                    tree.get_max_child_weight() * tree.size
                }
            )
            this._column_width_maxes[i] = this._column_width_map[i].max()
        }

        if (!ignore_ui) {
            val adapter = (this.get_column_recycler().adapter as ColumnRecyclerAdapter)
            adapter.insert_row(y)
            this._line_label_layout.insert_label(y)
            (this.column_label_recycler.adapter as ColumnLabelAdapter).notifyDataSetChanged()
        }
    }

    fun new_row(y: Int, opus_line: OpusLineAbstract<*>, ignore_ui: Boolean = false) {
        for (i in 0 until this.get_opus_manager().beat_count) {
            val tree = opus_line.beats[i]
            this._column_width_map[i].add(
                y,
                if (tree.is_leaf()) {
                    1
                } else {
                    tree.get_max_child_weight() * tree.size
                }
            )
            this._column_width_maxes[i] = this._column_width_map[i].max()
        }

        if (!ignore_ui) {
            val adapter = (this.get_column_recycler().adapter as ColumnRecyclerAdapter)
            adapter.insert_row(y)
            this._line_label_layout.insert_label(y)
            (this.column_label_recycler.adapter as ColumnLabelAdapter).notifyDataSetChanged()
        }
    }

    fun remove_rows(y: Int, count: Int, ignore_ui: Boolean = false) {
        for (i in 0 until this._column_width_map.size) {
            for (j in 0 until count) {
                this._column_width_map[i].removeAt(y)
                this._column_width_maxes[i] = this._column_width_map[i].max()
            }
        }
        if (! ignore_ui) {
            (this.get_column_recycler().adapter as ColumnRecyclerAdapter).remove_rows(y, count)
            this._line_label_layout.remove_labels(y, count)
            (this.column_label_recycler.adapter as ColumnLabelAdapter).notifyDataSetChanged()
        }
    }

    fun new_column(index: Int, ignore_ui: Boolean = false) {
        val opus_manager = this.get_opus_manager()
        val column = mutableListOf<Int>()
        opus_manager.get_visible_channels().forEachIndexed { i: Int, channel: OpusChannelAbstract<*,*> ->
            channel.lines.forEachIndexed { j: Int, line: OpusLineAbstract<*> ->
                val tree = opus_manager.get_tree(BeatKey(i, j, index))
                if (tree.is_leaf()) {
                    column.add(1)
                } else {
                    column.add(tree.get_max_child_weight() * tree.size)
                }
                for ((type, controller) in channel.lines[j].controllers.get_all()) {
                    if (! opus_manager.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                        continue
                    }
                    val ctl_tree = controller.get_beat(index)
                    if (ctl_tree.is_leaf()) {
                        column.add(1)
                    } else {
                        val new_weight = ctl_tree.get_max_child_weight() * ctl_tree.size
                        column.add(new_weight)
                    }
                }
            }
            for ((type, controller) in channel.controllers.get_all()) {
                if (! opus_manager.is_ctl_line_visible(CtlLineLevel.Channel, type)) {
                    continue
                }
                val ctl_tree = controller.get_beat(index)
                if (ctl_tree.is_leaf()) {
                    column.add(1)
                } else {
                    val new_weight = ctl_tree.get_max_child_weight() * ctl_tree.size
                    column.add(new_weight)
                }
            }
        }
        for ((type, controller) in opus_manager.controllers.get_all()) {
            if (! opus_manager.is_ctl_line_visible(CtlLineLevel.Global, type)) {
                continue
            }
            val ctl_tree = controller.get_beat(index)
            if (ctl_tree.is_leaf()) {
                column.add(1)
            } else {
                val new_weight = ctl_tree.get_max_child_weight() * ctl_tree.size
                column.add(new_weight)
            }
        }

        this._column_width_map.add(index, column)
        this._column_width_maxes.add(index, if (column.isNotEmpty()) {
            column.max()
        } else {
            1
        })

        if (! ignore_ui) {
            (this.column_label_recycler.adapter!! as ColumnLabelAdapter).add_column(index)
            (this.get_column_recycler().adapter as ColumnRecyclerAdapter).add_column(index)
        }
    }

    fun remove_column(index: Int, ignore_ui: Boolean = false) {
        this._column_width_maxes.removeAt(index)
        if (! ignore_ui) {
            (this.column_label_recycler.adapter!! as ColumnLabelAdapter).remove_column(index)
            (this.get_column_recycler().adapter as ColumnRecyclerAdapter).remove_column(index)
        }
    }

    fun apply_queued_notifications() {
        this.apply_queued_column_changes()
        this.apply_queued_cell_changes()
    }

    fun apply_queued_column_changes() {
        val queued_columns = this._queued_column_notifications.toList()
        val column_label_adapter = (this.column_label_recycler.adapter!! as ColumnLabelAdapter)
        for (x in queued_columns) {
            column_label_adapter.notifyItemChanged(x)
            (this.get_column_recycler().adapter as ColumnRecyclerAdapter).notify_column_state_changed(x)
        }
        this._queued_column_notifications.clear()   
    }

    fun apply_queued_cell_changes() {
        val queued = this._queued_cell_notifications.toList()
        this._queued_cell_notifications.clear()
        this.notify_cell_changes(queued)
    }

    fun notify_cell_changes(cell_coords: List<Coordinate>, ignore_ui: Boolean = false) {
        val column_recycler_adapter = (this.get_column_recycler().adapter!! as ColumnRecyclerAdapter)

        val changed_beats = mutableSetOf<Int>()
        val changed_beat_keys = mutableSetOf<Coordinate>()
        val done_keys = mutableSetOf<Coordinate>()
        val opus_manager = this.get_opus_manager()
        for (coord in cell_coords) {
            if (done_keys.contains(coord)) {
                continue
            }
            done_keys.add(coord)

            val original_width = try {
                this._column_width_maxes[coord.x]
            } catch (e: java.lang.IndexOutOfBoundsException) {
                continue
            }

            val ctl_line_index = opus_manager.get_ctl_line_from_visible_row(coord.y)
            val (pointer, ctl_level, ctl_type) = opus_manager.get_ctl_line_info(
                ctl_line_index
            )
            val new_tree: OpusTree<*> = when (ctl_level) {
                null -> {
                    val (channel, line_offset) = opus_manager.get_std_offset(pointer)
                    opus_manager.get_tree(
                        BeatKey(
                            channel,
                            line_offset,
                            coord.x
                        )
                    )
                }
                CtlLineLevel.Line -> {
                    val (channel, line_offset) = opus_manager.get_std_offset(pointer)
                    opus_manager.get_line_ctl_tree(ctl_type!!, BeatKey(channel, line_offset, coord.x))
                }
                CtlLineLevel.Channel -> {
                    opus_manager.get_channel_ctl_tree(ctl_type!!, pointer, coord.x)
                }
                CtlLineLevel.Global -> {
                    opus_manager.get_global_ctl_tree(ctl_type!!, coord.x)
                }
            }

            val new_cell_width = if (new_tree.is_leaf()) {
                1
            } else {
                new_tree.get_max_child_weight() * new_tree.size
            }
            this._column_width_map[coord.x][coord.y] = new_cell_width
            this._column_width_maxes[coord.x] = this._column_width_map[coord.x].max()


            if (original_width != this._column_width_maxes[coord.x]) {
                changed_beats.add(coord.x)
            } else {
                changed_beat_keys.add(coord)
            }
        }

        if (! ignore_ui) {
            // In set so as to not notify the same column multiple times
            for (beat in changed_beats) {
                this.column_label_recycler.adapter!!.notifyItemChanged(beat)
                column_recycler_adapter.notifyItemChanged(beat)
            }
            for (coord in changed_beat_keys) {
                // Don't bother notifying beat changed, was handled in column notification
                if (coord.x in changed_beats) {
                    continue
                }
                column_recycler_adapter.notify_cell_changed(coord.y, coord.x)
            }
        }
        if (this._queued_column_notifications.isNotEmpty()) {
            this.apply_queued_column_changes()
        }
        if (this._queued_cell_notifications.isNotEmpty()) {
            this.apply_queued_cell_changes()
        }
    }

    fun notify_column_changed(x: Int, queue: Boolean = false) {
        if (queue) {
            this._queued_column_notifications.add(x)
        } else {
            (this.get_column_recycler().adapter as ColumnRecyclerAdapter).notify_column_state_changed(x)
            val column_label_adapter = (this.column_label_recycler.adapter as ColumnLabelAdapter)
            column_label_adapter.notifyItemChanged(x)
        }
    }

    fun notify_row_changed(y: Int, queue: Boolean = false) {
        if (queue) {
            this.queue_cell_changes(List(this.get_opus_manager().beat_count) { i: Int ->
                Coordinate(y, i)
            })
        } else {
            this._line_label_layout.notify_item_changed(y)
            (this.get_column_recycler().adapter as ColumnRecyclerAdapter).notify_row_changed(y, true)
        }
    }

    fun queue_cell_changes(cell_coords: List<Coordinate>) {
        this._queued_cell_notifications.addAll(cell_coords)
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

    fun update_line_label(channel: Int, line_offset: Int) {
        val opus_manager = this.get_opus_manager()
        val y = opus_manager.get_visible_row_from_ctl_line(
            opus_manager.get_ctl_line_index(
                opus_manager.get_abs_offset(channel, line_offset)
            )
        )?: return
        this._line_label_layout.notify_item_changed(y)
    }

    fun align_column_labels() {
        val layout_manager_columns = this.get_column_recycler().layoutManager!! as LinearLayoutManager
        val layout_manager_labels = this.column_label_recycler.layoutManager!! as LinearLayoutManager

        val column_position = layout_manager_columns.findFirstVisibleItemPosition()
        val column = layout_manager_columns.findViewByPosition(column_position)

        val label_position = layout_manager_labels.findFirstVisibleItemPosition()
        val label = layout_manager_labels.findViewByPosition(label_position)
        if (label?.x != column?.x) {
            val new_offset = column?.x?.roundToInt() ?: 0
            this._main_scroll_locked = true
            layout_manager_columns.scrollToPositionWithOffset(column_position, new_offset)
            this._main_scroll_locked = false
            this._label_scroll_locked = true
            layout_manager_labels.scrollToPositionWithOffset(column_position, new_offset)
            this._label_scroll_locked = false
        }
    }

    fun scroll_to_position(x: Int? = null, y: Int? = null, offset: Float = 0f, offset_width: Float = 1f, force: Boolean = false) {
        if (x != null) {
            if (x >= this.get_opus_manager().beat_count) {
                return
            } else if (! force) {
                this.scroll_to_x(x, offset, offset_width)
            } else {
                this.forced_scroll_to_beat(x)
            }
        }

        if (y != null) {
            this.scroll_to_y(y)
        }
    }

    fun forced_scroll_to_beat(x: Int) {
        val box_width = this.get_column_recycler().measuredWidth

        val base_width = this.resources.getDimension(R.dimen.base_leaf_width)
        val max_width = (this._column_width_maxes[x] * base_width).toInt()

        val layout_manager = this.get_column_recycler().layoutManager!! as LinearLayoutManager
        val offset = if (max_width >= box_width) {
            (box_width - max_width) / 2
        } else {
            0
        }

        this._main_scroll_locked = true
        layout_manager.scrollToPositionWithOffset(x, offset)
        this._main_scroll_locked = false

        this._label_scroll_locked = true
        (this.column_label_recycler.layoutManager!! as LinearLayoutManager).scrollToPositionWithOffset(x, offset)
        this._label_scroll_locked = false
    }

    fun scroll_to_x(x: Int, offset: Float = 0F, offset_width: Float = 1F) {
        val layout_manager = this.get_column_recycler().layoutManager!! as LinearLayoutManager

        val box_width = this.get_column_recycler().measuredWidth

        val base_width = this.resources.getDimension(R.dimen.base_leaf_width)
        val max_width = (this._column_width_maxes[x] * base_width).toInt()
        val target_width = (this._column_width_maxes[x] * this.resources.getDimension(R.dimen.base_leaf_width) * offset_width).toInt()
        val visible_range = layout_manager.findFirstVisibleItemPosition() .. layout_manager.findLastVisibleItemPosition()
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
            val target_column = layout_manager.findViewByPosition(x)
            if (target_column == null) {
                // Shouldn't be Reachable
                return
            } else if (target_column.x + target_width + target_offset > box_width) {
                subdiv_state[POSITION_TO_RIGHT] = true
                subdiv_state[POSITION_ON_SCREEN] = target_column.x + target_offset < box_width
                subdiv_state[POSITION_TO_LEFT] = target_column.x + target_offset < 0
            } else if (target_column.x + target_width + target_offset > 0) {
                subdiv_state[POSITION_ON_SCREEN] = true
                subdiv_state[POSITION_TO_LEFT] = target_column.x + target_offset < 0
            } else {
                subdiv_state[POSITION_TO_LEFT] = true
            }


            if (target_column.x > box_width) {
                column_state[POSITION_TO_RIGHT] = true
            } else if (target_column.x > 0) {
                column_state[POSITION_ON_SCREEN] = true
                column_state[POSITION_TO_RIGHT] = target_column.x + max_width > box_width
            } else {
                column_state[POSITION_TO_LEFT] = true
                column_state[POSITION_ON_SCREEN] = target_column.x + max_width > 0
                column_state[POSITION_TO_RIGHT] = target_column.x + max_width > box_width
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
        var offset = 1
        for (i in 0 until 4) {
            subdiv_int += offset * if (subdiv_state[i]) { 1 } else { 0 }
            column_int += offset * if (column_state[i]) { 1 } else { 0 }
            offset *= 2
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
            0b1011 -> box_width - target_offset - target_width

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
                this.align_column_labels()
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
        layout_manager.scrollToPositionWithOffset(x, adj_offset)
        this._main_scroll_locked = false

        this._label_scroll_locked = true
        (this.column_label_recycler.layoutManager!! as LinearLayoutManager).scrollToPositionWithOffset(x, adj_offset)
        this._label_scroll_locked = false
    }

    fun scroll_to_y(y: Int) {
        val line_height = (resources.getDimension(R.dimen.line_height)).toInt()
        val control_line_height = resources.getDimension(R.dimen.ctl_line_height).toInt()
        var target_y = 0
        var count = 0
        var working_line_height = line_height
        val opus_manager = this.get_opus_manager()
        for (channel in opus_manager.get_visible_channels()) {
            for (line in channel.lines) {
                if (count >= y) {
                    break
                }

                target_y += line_height
                working_line_height = line_height
                count += 1
                for ((type, controller) in line.controllers.get_all()) {
                    if (!opus_manager.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                        continue
                    }
                    if (count >= y) {
                        break
                    }

                    target_y += control_line_height
                    working_line_height = control_line_height
                    count += 1
                }
            }
            for ((type, controller) in channel.controllers.get_all()) {
                if (!opus_manager.is_ctl_line_visible(CtlLineLevel.Channel, type)) {
                    continue
                }
                if (count >= y) {
                    break
                }

                target_y += control_line_height
                working_line_height = control_line_height
                count += 1
            }
        }

        for ((type, controller) in this.get_opus_manager().controllers.get_all()) {
            if (!opus_manager.is_ctl_line_visible(CtlLineLevel.Global, type)) {
                continue
            }
            if (count >= y) {
                break
            }
            target_y += control_line_height
            working_line_height = control_line_height
            count += 1
        }
        if (this._scroll_view.measuredHeight + this._scroll_view.scrollY < target_y + working_line_height) {
            val adj_target_y = target_y - (this._scroll_view.measuredHeight - (working_line_height * 1.5).toInt())
            this._line_label_layout.scrollTo(0, adj_target_y)
            this._scroll_view.scrollTo(0, adj_target_y)
        } else if (target_y < this._scroll_view.scrollY) {
            this._line_label_layout.scrollTo(0, target_y)
            this._scroll_view.scrollTo(0, target_y)
        }
    }

    fun get_scroll_offset(): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        val column_lm = this.column_label_recycler.layoutManager!! as LinearLayoutManager
        val coarse_x = column_lm.findFirstVisibleItemPosition()
        val column = column_lm.findViewByPosition(coarse_x)
        val fine_x = column?.x ?: 0

        val line_lm = this._line_label_layout
        val line_height = (resources.getDimension(R.dimen.line_height)).toInt()
        val coarse_y = line_lm.scrollY / line_height
        val fine_y = line_lm.scrollY % line_height
        return Pair(
            Pair(coarse_x, fine_x.toInt()),
            Pair(coarse_y, fine_y)
        )
    }

    fun precise_scroll(x_coarse: Int = 0, x_fine: Int = 0, y_coarse: Int? = null, y_fine: Int? = null) {
        val main_lm = (this.get_column_recycler().layoutManager!! as LinearLayoutManager)
        main_lm.scrollToPositionWithOffset(x_coarse, x_fine)

        val column_label_lm = (this.column_label_recycler.layoutManager!! as LinearLayoutManager)
        column_label_lm.scrollToPositionWithOffset(x_coarse, x_fine)

        if (y_coarse != null) {
            val line_height = (resources.getDimension(R.dimen.line_height)).toInt()
            this._scroll_view.scrollTo(0, (line_height * y_coarse) + (y_fine ?: 0))
        }
    }

    fun get_first_visible_column_index(): Int {
        return (this.get_column_recycler().layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    }
    fun get_column_recycler(): ColumnRecycler {
        return this._scroll_view.column_recycler
    }

    fun swap_lines(line_a: Int, line_b: Int) {
        for (i in 0 until this._column_width_map.size) {
            val tmp = this._column_width_map[i][line_a]
            this._column_width_map[i][line_a] = this._column_width_map[i][line_b]
            this._column_width_map[i][line_b] = tmp
        }
    }

    fun get_line_label_layout(): LineLabelColumnLayout {
        return this._line_label_layout
    }
}
