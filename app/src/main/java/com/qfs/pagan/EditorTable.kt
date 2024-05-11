package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import com.qfs.pagan.opusmanager.ActiveControlSet
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.opusmanager.OpusLine
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

    private var _initializing_column_width_map = false
    private val _column_width_map = mutableListOf<MutableList<Int>>()
    private val _column_width_maxes = mutableListOf<Int>()

    private var _active_cursor: OpusManagerCursor = OpusManagerCursor(OpusManagerCursor.CursorMode.Unset)

    // Scroll Locks
    var _label_scroll_locked = false
    var _main_scroll_locked = false

    var needs_setup = true

    private val _queued_cell_notifications = mutableListOf<Coordinate>()

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
        this._column_width_map.clear()
        this._column_width_maxes.clear()

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

    private fun _init_column_width_map() {
       this._initializing_column_width_map = true
        val opus_manager = this.get_opus_manager()
        for (beat in 0 until opus_manager.beat_count) {
            this._column_width_map.add(mutableListOf<Int>())
            opus_manager.get_visible_channels().forEachIndexed { i: Int, channel: OpusChannel ->
                for (j in channel.lines.indices) {
                    val tree = opus_manager.get_tree(BeatKey(i, j, beat))
                    if (tree.is_leaf()) {
                        this._column_width_map[beat].add(1)
                    } else {
                        val new_weight = tree.get_max_child_weight() * tree.size
                        this._column_width_map[beat].add(new_weight)
                    }

                    for ((type, controller) in channel.lines[j].controllers.get_all()) {
                        if (! opus_manager.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                            continue
                        }
                        val ctl_tree = controller.get_beat(beat)
                        if (ctl_tree.is_leaf()) {
                            this._column_width_map[beat].add(1)
                        } else {
                            val new_weight = ctl_tree.get_max_child_weight() * ctl_tree.size
                            this._column_width_map[beat].add(new_weight)
                        }
                    }
                }

                for ((type, controller) in channel.controllers.get_all()) {
                    if (! opus_manager.is_ctl_line_visible(CtlLineLevel.Channel, type)) {
                        continue
                    }
                    val ctl_tree = controller.get_beat(beat)
                    if (ctl_tree.is_leaf()) {
                        this._column_width_map[beat].add(1)
                    } else {
                        val new_weight = ctl_tree.get_max_child_weight() * ctl_tree.size
                        this._column_width_map[beat].add(new_weight)
                    }
                }
            }

            for ((type, controller) in opus_manager.controllers.get_all()) {
                if (! opus_manager.is_ctl_line_visible(CtlLineLevel.Global, type)) {
                    continue
                }
                val ctl_tree = controller.get_beat(beat)
                if (ctl_tree.is_leaf()) {
                    this._column_width_map[beat].add(1)
                } else {
                    val new_weight = ctl_tree.get_max_child_weight() * ctl_tree.size
                    this._column_width_map[beat].add(new_weight)
                }
            }

            this._column_width_maxes.add(
                if (this._column_width_map.size > beat && this._column_width_map[beat].isNotEmpty()) {
                    this._column_width_map[beat].max()
                } else {
                   1
                }
            )
        }
        this._initializing_column_width_map = false
    }

    fun new_row(y: Int, controller: ActiveControlSet.ActiveController, ignore_ui: Boolean = false) {
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

    fun new_row(y: Int, opus_line: OpusLine, ignore_ui: Boolean = false) {
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
        opus_manager.get_visible_channels().forEachIndexed { i: Int, channel: OpusChannel ->
            channel.lines.forEachIndexed { j: Int, line: OpusLine ->
                val tree = opus_manager.get_tree(BeatKey(i, j, index))
                if (tree.is_leaf()) {
                    column.add(1)
                } else {
                    column.add(tree.get_max_child_weight() * tree.size)
                }
                for ((type, controller) in channel.lines[j].controllers.get_all()) {
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
        this._column_width_map.removeAt(index)
        this._column_width_maxes.removeAt(index)
        if (! ignore_ui) {
            (this.column_label_recycler.adapter!! as ColumnLabelAdapter).remove_column(index)
            (this.get_column_recycler().adapter as ColumnRecyclerAdapter).remove_column(index)
        }
    }

    fun update_cursor(cursor: OpusManagerCursor, deep_update: Boolean = true) {
        if (cursor != this._active_cursor) {
            try {
                this.update_cursor(this._active_cursor, deep_update)
            } catch (e: OpusTree.InvalidGetCall) {
                // Pass
            }
            this._active_cursor = cursor.copy()
        }

        val opus_manager = this.get_opus_manager()
        val column_label_adapter = (this.column_label_recycler.adapter!! as ColumnLabelAdapter)

        when (cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                when (cursor.ctl_level) {
                    null -> {
                        val beat_key = cursor.get_beatkey()
                        val beat_keys = if (deep_update) {
                            opus_manager.get_all_linked(beat_key)
                        } else {
                            listOf(beat_key)
                        }

                        for (linked_key in beat_keys) {
                            val y = try {
                                opus_manager.get_visible_row_from_ctl_line(
                                    opus_manager.get_ctl_line_index(
                                        opus_manager.get_abs_offset(
                                            linked_key.channel,
                                            linked_key.line_offset
                                        )
                                    )
                                ) ?: continue
                            } catch (e: IndexOutOfBoundsException) {
                                return
                            }

                            this._line_label_layout.notify_item_changed(y)
                            column_label_adapter.notifyItemChanged(linked_key.beat)
                            (this.get_column_recycler().adapter as ColumnRecyclerAdapter).notify_cell_changed(y, linked_key.beat, true)
                        }
                    }

                    CtlLineLevel.Line -> {
                        val y = opus_manager.get_visible_row_from_ctl_line_line(
                            cursor.ctl_type!!,
                            cursor.channel,
                            cursor.line_offset
                        )
                        this._line_label_layout.notify_item_changed(y)
                        column_label_adapter.notifyItemChanged(cursor.beat)
                        (this.get_column_recycler().adapter as ColumnRecyclerAdapter).notify_cell_changed(y, cursor.beat, true)
                    }
                    CtlLineLevel.Channel -> {
                        val y = opus_manager.get_visible_row_from_ctl_line_channel(
                            cursor.ctl_type!!,
                            cursor.channel
                        )
                        this._line_label_layout.notify_item_changed(y)
                        column_label_adapter.notifyItemChanged(cursor.beat)
                        (this.get_column_recycler().adapter as ColumnRecyclerAdapter).notify_cell_changed(y, cursor.beat, true)
                    }

                    CtlLineLevel.Global -> {
                        val y = opus_manager.get_visible_row_from_ctl_line_global(
                            cursor.ctl_type!!
                        )

                        this._line_label_layout.notify_item_changed(y)
                        column_label_adapter.notifyItemChanged(cursor.beat)
                        (this.get_column_recycler().adapter as ColumnRecyclerAdapter).notify_cell_changed(y, cursor.beat, true)
                    }
                }
            }

            OpusManagerCursor.CursorMode.Range -> {
                val coords_to_update = mutableListOf<Pair<Int, Int>>()
                when (cursor.ctl_level) {
                    null -> {
                        val (top_left, bottom_right) = cursor.range!!
                        for (beat_key in opus_manager.get_beatkeys_in_range(top_left, bottom_right)) {
                            val y = try {
                                opus_manager.get_visible_row_from_ctl_line(
                                    opus_manager.get_ctl_line_index(
                                        opus_manager.get_abs_offset(
                                            beat_key.channel, beat_key.line_offset
                                        )
                                    )
                                ) ?: continue
                            } catch (e: IndexOutOfBoundsException) {
                                continue
                            }

                            coords_to_update.add(Pair(y, beat_key.beat))
                        }
                    }
                    else -> {
                        val (top_left, bottom_right) = cursor.range!!
                        val y = when (cursor.ctl_level!!) {
                            // Can assume top_left.channel == bottom_right.channel and top_left.line_offset == bottom_right.line_offset
                            CtlLineLevel.Line -> opus_manager.get_visible_row_from_ctl_line_line(
                                cursor.ctl_type!!,
                                top_left.channel,
                                top_left.line_offset
                            )
                            // Can assume top_left.channel == bottom_right.channel
                            CtlLineLevel.Channel -> opus_manager.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, top_left.channel)
                            CtlLineLevel.Global -> opus_manager.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                        }
                        val first_beat = min(top_left.beat, bottom_right.beat)
                        val last_beat = max(top_left.beat, bottom_right.beat)
                        for (x in first_beat..last_beat) {
                            coords_to_update.add(Pair(y, x))
                        }
                    }
                }

                for ((y, x) in coords_to_update) {
                    this._line_label_layout.notify_item_changed(y)
                    column_label_adapter.notifyItemChanged(x)
                    (this.get_column_recycler().adapter as ColumnRecyclerAdapter).notify_cell_changed(y, x, true)
                }
            }
            OpusManagerCursor.CursorMode.Row -> {
                val y = when (cursor.ctl_level) {
                    null -> {
                        try {
                            opus_manager.get_visible_row_from_ctl_line(
                                opus_manager.get_ctl_line_index(
                                    opus_manager.get_abs_offset(
                                        cursor.channel,
                                        cursor.line_offset
                                    )
                                )
                            ) ?: return
                        } catch (e: IndexOutOfBoundsException) {
                            return
                        }
                    }
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

                this._line_label_layout.notify_item_changed(y)
                (this.get_column_recycler().adapter as ColumnRecyclerAdapter).notify_row_changed(y, true)
            }

            OpusManagerCursor.CursorMode.Column -> {
                (this.get_column_recycler().adapter as ColumnRecyclerAdapter).notify_column_state_changed(cursor.beat)
                column_label_adapter.notifyItemChanged(cursor.beat)
            }
            OpusManagerCursor.CursorMode.Unset -> { }
        }
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

            val (pointer, ctl_level, ctl_type) = opus_manager.get_ctl_line_info(
                opus_manager.get_ctl_line_from_visible_row(coord.y)
            )
            val new_tree: OpusTree<out OpusEvent> = when (ctl_level) {
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
        if (this._queued_cell_notifications.isNotEmpty()) {
            this.apply_queued_cell_changes()
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


    fun scroll_to_position(beat_key: BeatKey, position: List<Int>? = null) {
        if (beat_key.beat == -1) {
            return
        }

        val adj_beat_key = BeatKey(
            max(0, beat_key.channel),
            max(0, beat_key.line_offset),
            beat_key.beat
        )

        val new_position = position?.toMutableList() ?: mutableListOf()
        val opus_manager = this.get_opus_manager()

        var tree = opus_manager.get_tree(adj_beat_key, new_position)
        while (! tree.is_leaf()) {
            tree = tree[0]
            new_position.add(0)
        }

        val y = opus_manager.get_visible_row_from_ctl_line(
            opus_manager.get_ctl_line_index(
                opus_manager.get_abs_offset(beat_key.channel, beat_key.line_offset)
            )
        )
        if (position == null) {
            if (this.get_position_visibility(beat_key.beat) < SECTION_VIEW_PARTIAL_OVERSIZED) {
                this.scroll_to_x(beat_key.beat, 0f)
            }
        } else {
            var leaf_offset = 0
            tree = this.get_opus_manager().get_tree(beat_key)
            var sibling_weight = tree.get_max_child_weight()
            val max_cell_weight = if (tree.is_leaf()) {
                1f
            } else {
                (sibling_weight * tree.size).toFloat()
            }

            position.forEachIndexed { i: Int, x: Int ->
                leaf_offset += sibling_weight * x
                tree = tree[x]
                if (i < position.size - 1) {
                    sibling_weight /= max(tree.size, 1)
                }
            }

            val max_column_weight = this._column_width_maxes[beat_key.beat].toFloat()
            val position_offset = leaf_offset / max_cell_weight

            val position_visibility = this.get_position_visibility(beat_key.beat, Pair(position_offset, sibling_weight.toFloat() / max_cell_weight))
            if (position_visibility < SECTION_VIEW_PARTIAL_OVERSIZED) {
                val leaf_width = resources.getDimension(R.dimen.base_leaf_width)
                val precise_x = 0 - (leaf_width * (leaf_offset * max_column_weight / max_cell_weight)).toInt()
                this.precise_scroll(
                    beat_key.beat,
                    precise_x
                )
            }
        }

        if (y != null && !this.is_y_visible(y)) {
            this.scroll_to_y(y)
        }
    }

    fun get_position_visibility(beat: Int, section: Pair<Float, Float> = Pair(0f,1f)): Int {
        val column_lm = this.column_label_recycler.layoutManager!! as LinearLayoutManager
        val first_visible = column_lm.findFirstVisibleItemPosition()
        val last_visible = column_lm.findLastVisibleItemPosition()

        return if (beat in (first_visible + 1) until last_visible) {
            SECTION_VIEW_COMPLETE
        } else {
            val visible_width = this.column_label_recycler.measuredWidth
            val leaf_width = resources.getDimension(R.dimen.base_leaf_width)
            val beat_leaf_count = this._column_width_maxes[beat]
            val beat_width = beat_leaf_count * leaf_width

            val section_start = (section.first * beat_width).roundToInt()
            val section_end = ((section.first + section.second) * beat_width).roundToInt()

            if (first_visible == last_visible) {
                if (beat != first_visible) {
                    SECTION_OUT_OF_VIEW
                } else {
                    val first_column = column_lm.findViewByPosition(first_visible)
                    val fine_x = (first_column?.x ?: 0f).roundToInt()

                    val beat_start_proceeds_view_start = section_start + fine_x > 0
                    val beat_end_precedes_view_end = section_end + fine_x < visible_width
                    val start_visible =
                        beat_start_proceeds_view_start && (section_start + fine_x < visible_width)
                    val end_visible = (section_end + fine_x > 0) && beat_end_precedes_view_end

                    if (!beat_start_proceeds_view_start && !beat_end_precedes_view_end) {
                        SECTION_VIEW_PARTIAL_OVERSIZED
                    } else if (start_visible && end_visible) {
                        SECTION_VIEW_COMPLETE
                    } else if (start_visible) {
                        SECTION_VIEW_PARTIAL_LEFT
                    } else if (end_visible) {
                        SECTION_VIEW_PARTIAL_RIGHT
                    } else {
                        SECTION_OUT_OF_VIEW
                    }
                }
            } else {
                val column = column_lm.findViewByPosition(beat)
                val fine_x = (column?.x ?: 0f).roundToInt()

                when (beat) {
                    first_visible -> {
                        if (section_start + fine_x > 0) {
                            SECTION_VIEW_COMPLETE
                        } else if (section_end + fine_x > 0) {
                            SECTION_VIEW_PARTIAL_RIGHT
                        } else {
                            SECTION_OUT_OF_VIEW
                        }
                    }
                    last_visible -> {
                        if (section_end + fine_x < visible_width) {
                            SECTION_VIEW_COMPLETE
                        } else if (section_start + fine_x < visible_width) {
                            SECTION_VIEW_PARTIAL_LEFT
                        } else {
                            SECTION_OUT_OF_VIEW
                        }
                    }
                    else -> {
                        SECTION_OUT_OF_VIEW
                    }
                }
            }
        }
    }

    fun is_y_visible(y: Int): Boolean {
        val line_height = (resources.getDimension(R.dimen.line_height)).toInt()
        val scroll_offset = this._line_label_layout.scrollY / line_height
        val height = this._scroll_view.measuredHeight / line_height
        return y >= scroll_offset && y <= (scroll_offset + height)
    }

    fun forced_scroll_to_beat(x: Int) {
        val layout_manager = this.get_column_recycler().layoutManager!! as LinearLayoutManager
        this._main_scroll_locked = true
        layout_manager.scrollToPositionWithOffset(x, 0)
        this._main_scroll_locked = false

        this._label_scroll_locked = true
        (this.column_label_recycler.layoutManager!! as LinearLayoutManager).scrollToPositionWithOffset(x, 0)
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

            // 0b0111 -> { }   // Nothing to be done
            // 0b1001 -> { }   // Valid, but no need to do anything
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
        for (channel in opus_manager.channels) {
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
        Log.d("AAA", "$target_y, ${this._scroll_view.scrollY}")
        if (this._scroll_view.measuredHeight + this._scroll_view.scrollY < target_y + working_line_height) {
            val adj_target_y = target_y - (this._scroll_view.measuredHeight - (working_line_height * 1.5).toInt())
            this._line_label_layout.scrollTo(0, adj_target_y)
            this._scroll_view.scrollTo(0, adj_target_y)
        } else if (target_y < this._scroll_view.scrollY) {
            this._line_label_layout.scrollTo(0, target_y)
            this._scroll_view.scrollTo(0, target_y)
        }

    }


    //  Unused. I don't know if i'll need them at any point
    //fun get_leaf(beat_key: BeatKey, position: List<Int>): LeafButton? {
    //    val y = this.get_opus_manager().get_abs_offset(beat_key.channel, beat_key.line_offset)
    //    return this.get_leaf(beat_key.beat, y, position)
    //}

    //fun get_leaf(x: Int, y: Int, position: List<Int>): LeafButton? {
    //    val column_view_holder = this.get_column_recycler().findViewHolderForAdapterPosition(x) ?: return null
    //    val cell_recycler = (column_view_holder as ColumnRecyclerViewHolder).get_cell_recycler() ?: return null
    //    val cell_view_holder = cell_recycler.findViewHolderForAdapterPosition(y) ?: return null
    //    val cell_layout = (cell_view_holder as CellRecyclerViewHolder).get_cell_layout()
    //    for (child in (cell_layout as ViewGroup).children) {
    //        if ((child as LeafButton).position == position) {
    //            return child
    //        }
    //    }
    //    return null
    //}

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

    fun update_percussion_visibility() {
        val main = this.get_activity()

        val opus_manager = this.get_opus_manager()
        val percussion_channel = opus_manager.channels.last()
        if (main.view_model.show_percussion) {
            if (this._column_width_map.isNotEmpty()) {
                var newly_visible_rows = 0
                for (i in 0 until percussion_channel.size) {
                    val row = opus_manager.get_visible_row_from_ctl_line(
                        opus_manager.get_ctl_line_index(
                            opus_manager.get_abs_offset(
                                opus_manager.channels.size - 1,
                                i
                            )
                        )
                    )!!

                    val controllers = percussion_channel.lines[i].controllers.get_all()
                    this.new_row(row, percussion_channel.lines[i])
                    for (j in controllers.indices) {
                        this.new_row(row + j, controllers[j].second)
                    }
                    newly_visible_rows += 1 + controllers.size
                }

                // Not using channel controls ATM
                // Make visible the channel-specific control lines
                //val row = opus_manager.get_visible_row_from_ctl_line(
                //    opus_manager.get_ctl_line_index(
                //        opus_manager.get_abs_offset(opus_manager.channels.size - 1, 0)
                //    )
                //)!!
                //val controllers = percussion_channel.controllers.get_all()
                //for (i in controllers.indices) {
                //    this.new_row(row + newly_visible_rows + i, controllers[i].second)
                //}

            }
        } else {
            val row = opus_manager.get_visible_row_from_ctl_line(
                opus_manager.get_ctl_line_index(
                    opus_manager.get_abs_offset(opus_manager.channels.size - 1, 0)
                )
            )!!
            var remove_count = 0
            for (line in percussion_channel.lines) {
                remove_count += 1
                for ((type, _) in line.controllers.get_all()) {
                    if (opus_manager.is_ctl_line_visible(CtlLineLevel.Line, type)) {
                        remove_count += 1
                    }
                }
            }

            for ((type, _) in percussion_channel.controllers.get_all()) {
                if (opus_manager.is_ctl_line_visible(CtlLineLevel.Channel, type)) {
                    remove_count += percussion_channel.controllers.size()
                }
            }

            opus_manager.recache_line_maps()
            this.remove_rows(row, remove_count)
        }
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
