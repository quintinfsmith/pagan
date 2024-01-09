package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.structure.OpusTree
import kotlin.math.max
import kotlin.math.roundToInt
import com.qfs.pagan.InterfaceLayer as OpusManager

class EditorTable(context: Context, attrs: AttributeSet): TableLayout(context, attrs) {
    val main_recycler = ColumnRecycler(this)
    var scroll_view = ScrollView(context)
    val line_label_layout = LineLabelColumnLayout(this)
    val column_label_recycler = ColumnLabelRecycler(context)
    val top_row = TableRow(context)
    val bottom_row = TableRow(context)
    val spacer = CornerView(context)

    var initializing_column_width_map = false
    val column_width_map = mutableListOf<MutableList<Int>>()
    val column_width_maxes = mutableListOf<Int>()

    var active_cursor: OpusManagerCursor = OpusManagerCursor(OpusManagerCursor.CursorMode.Unset)

    // Scroll Locks
    var _label_scroll_locked = false
    var _main_scroll_locked = false

    private val _queued_cell_notifications = mutableListOf<BeatKey>()

    companion object {
        val SECTION_OUT_OF_VIEW = 0
        val SECTION_VIEW_PARTIAL_LEFT = 1
        val SECTION_VIEW_PARTIAL_RIGHT = 2
        val SECTION_VIEW_PARTIAL_OVERSIZED = 3
        val SECTION_VIEW_COMPLETE = 4

    }

    init {
        this.top_row.addView(this.spacer)
        this.top_row.addView(this.column_label_recycler)

        this.spacer.setOnClickListener {
            val fragment = this.get_activity().get_active_fragment()
            if (fragment is EditorFragment) {
                fragment.shortcut_dialog()
            }
        }
        this.spacer.setOnLongClickListener {
            this.get_opus_manager().cursor_select_column(0, true)
            true
        }

        this.bottom_row.addView(LinearLayout(ContextThemeWrapper(context, R.style.column)))

        (this.bottom_row.getChildAt(0) as ViewGroup).layoutParams.width = MATCH_PARENT
        (this.bottom_row.getChildAt(0) as ViewGroup).layoutParams.height = WRAP_CONTENT
        (this.bottom_row.getChildAt(0) as ViewGroup).addView(this.line_label_layout)

        this.scroll_view.addView(this.main_recycler)
        this.bottom_row.addView(this.scroll_view)

        this.addView(this.top_row)
        this.addView(this.bottom_row)

        this.top_row.layoutParams.width = MATCH_PARENT
        this.top_row.layoutParams.height = WRAP_CONTENT

        this.bottom_row.layoutParams.width = MATCH_PARENT
        this.bottom_row.layoutParams.height = MATCH_PARENT

        this.spacer.layoutParams.width = MATCH_PARENT
        this.spacer.layoutParams.height = MATCH_PARENT

        this.line_label_layout.layoutParams.width = WRAP_CONTENT
        this.line_label_layout.layoutParams.height = MATCH_PARENT

        this.scroll_view.overScrollMode = OVER_SCROLL_NEVER
        this.scroll_view.isVerticalScrollBarEnabled = false
        (this.scroll_view.layoutParams as LinearLayout.LayoutParams).weight = 1F
        this.scroll_view.layoutParams.width = 0
        this.scroll_view.layoutParams.height = MATCH_PARENT

        this.main_recycler.layoutParams.width = WRAP_CONTENT
        this.main_recycler.layoutParams.height = WRAP_CONTENT

        (this.column_label_recycler.layoutParams as LinearLayout.LayoutParams).weight = 1F
        this.column_label_recycler.layoutParams.width = 0

        ColumnLabelAdapter(this)

        this.main_recycler.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, x: Int, y: Int) {
                if (! this@EditorTable._main_scroll_locked) {
                    this@EditorTable._label_scroll_locked = true
                    this@EditorTable.column_label_recycler.scrollBy(x, 0)
                    this@EditorTable._label_scroll_locked = false
                }
            }
        })

        /* Allow Scrolling on the y axis when scrolling in the main_recycler */
        var last_y_position: Float? = null
        this.main_recycler.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == 1) {
                last_y_position = null
                return@setOnTouchListener false
            }

            if (motionEvent.action != MotionEvent.ACTION_MOVE) {
                return@setOnTouchListener false
            }

            if (last_y_position == null) {
                last_y_position = (motionEvent.y - this.scroll_view.y) - this.scroll_view.scrollY.toFloat()
            }

            val rel_y = (motionEvent.y - this.scroll_view.y)  - this.scroll_view.scrollY
            val delta_y = last_y_position!! - rel_y


            this.scroll_view.scrollBy(0, delta_y.toInt())
            last_y_position = rel_y

            false
        }

        /* Allow Scrolling on the x axis when scrolling in the vertical scroll_view */
        var last_x_position: Float? = null
        this.scroll_view.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == 1) {
                last_x_position = null
                return@setOnTouchListener false
            }

            if (motionEvent.action != MotionEvent.ACTION_MOVE) {
                return@setOnTouchListener false
            }

            if (last_x_position == null) {
                last_x_position = (motionEvent.x - this.main_recycler.x) - this.main_recycler.scrollY.toFloat()
            }

            val rel_x = (motionEvent.x - this.main_recycler.x)  - this.main_recycler.scrollY
            val delta_x = last_x_position!! - rel_x


            this.main_recycler.scrollBy(delta_x.toInt(), 0)
            last_x_position = rel_x

            false
        }

        this.column_label_recycler.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, x: Int, y: Int) {
                if (! this@EditorTable._label_scroll_locked) {
                    this@EditorTable._main_scroll_locked = true
                    this@EditorTable.main_recycler.scrollBy(x, 0)
                    this@EditorTable._main_scroll_locked = false
                }
            }
        })

        this.scroll_view.setOnScrollChangeListener { _: View, x: Int, y: Int, _: Int, _: Int ->
            this.line_label_layout.scrollTo(x, y)
        }
    }

    fun clear() {
        this.column_width_map.clear()
        this.column_width_maxes.clear()

        this.get_activity().runOnUiThread {
            (this.main_recycler.adapter!! as ColumnRecyclerAdapter).clear()
            (this.column_label_recycler.adapter!! as ColumnLabelAdapter).clear()
            this.line_label_layout.clear()
        }
    }

    fun setup() {
        this.init_column_width_map()
        val opus_manager = this.get_opus_manager()
        val main_adapter = (this.main_recycler.adapter as ColumnRecyclerAdapter)
        val column_label_adapter = (this.column_label_recycler.adapter as ColumnLabelAdapter)

        for (beat in 0 until opus_manager.beat_count) {
            column_label_adapter.add_column(beat)
        }

        var y = 0
        opus_manager.get_visible_channels().forEach { channel: OpusChannel ->
            channel.lines.forEach {
                this.line_label_layout.insert_label(y++)
            }
        }
        main_adapter.add_columns(0, opus_manager.beat_count)
    }

    fun init_column_width_map() {
        this.initializing_column_width_map = true
        val opus_manager = this.get_opus_manager()
        for (beat in 0 until opus_manager.beat_count) {
            this.column_width_map.add(mutableListOf<Int>())
            opus_manager.get_visible_channels().forEachIndexed { i: Int, channel: OpusChannel ->
                for (j in channel.lines.indices) {
                    val tree = opus_manager.get_beat_tree(BeatKey(i, j, beat))
                    if (tree.is_leaf()) {
                        this.column_width_map[beat].add(1)
                    } else {
                        val new_weight = tree.get_max_child_weight() * tree.size
                        this.column_width_map[beat].add(new_weight)
                    }
                }
            }
            this.column_width_maxes.add(
                if (this.column_width_map.size > beat && this.column_width_map[beat].isNotEmpty()) {
                    this.column_width_map[beat].max()
                } else {
                   1
                }
            )
        }
        this.initializing_column_width_map = false
    }

    fun new_row(y: Int, opus_line: OpusChannel.OpusLine, ignore_ui: Boolean = false) {
        for (i in 0 until this.get_opus_manager().beat_count) {
            val tree = opus_line.beats[i]
            this.column_width_map[i].add(
                y,
                if (tree.is_leaf()) {
                    1
                } else {
                    tree.get_max_child_weight() * tree.size
                }
            )
            this.column_width_maxes[i] = this.column_width_map[i].max()
        }

        if (!ignore_ui) {
            val adapter = (this.main_recycler.adapter as ColumnRecyclerAdapter)
            adapter.insert_row(y)
            (this.column_label_recycler.adapter as ColumnLabelAdapter).notifyDataSetChanged()

            this.line_label_layout.insert_label(y)
        }
    }

    fun new_channel_rows(y: Int, opus_lines: List<OpusChannel.OpusLine>, ignore_ui: Boolean = false) {
        opus_lines.forEachIndexed { i: Int, opus_line: OpusChannel.OpusLine ->
            for (j in 0 until opus_line.beats.size) {
                val tree = opus_line.beats[j]
                if (this.column_width_map.size <= j) {
                    continue
                }
                this.column_width_map[j].add(
                    y + i,
                    if (tree.is_leaf()) {
                        1
                    } else {
                        tree.get_max_child_weight() * tree.size
                    }
                )
            }
        }

        for (j in 0 until this.get_opus_manager().beat_count) {
            if (this.column_width_map.size <= j) {
                while (this.column_width_maxes.size <= j) {
                    this.column_width_maxes.add(1)
                }
                this.column_width_maxes[j] = 1
            } else {
                this.column_width_maxes[j] = this.column_width_map[j].max()
            }
        }

        if (! ignore_ui) {
            this.line_label_layout.insert_labels(y, opus_lines.size)
            val adapter = (this.main_recycler.adapter as ColumnRecyclerAdapter)
            adapter.insert_rows(y, opus_lines.size)
            (this.column_label_recycler.adapter as ColumnLabelAdapter).notifyDataSetChanged()
        }
    }

    fun remove_row(y: Int, ignore_ui: Boolean = false) {
        for (i in 0 until this.column_width_map.size) {
            this.column_width_map[i].removeAt(y)
            this.column_width_maxes[i] = this.column_width_map[i].max()
        }
        if (! ignore_ui) {
            (this.main_recycler.adapter as ColumnRecyclerAdapter).remove_row(y)
            this.line_label_layout.remove_label(y)
            (this.column_label_recycler.adapter as ColumnLabelAdapter).notifyDataSetChanged()
        }
    }

    fun remove_channel_rows(y: Int, count: Int, ignore_ui: Boolean = false) {
        for (i in 0 until this.column_width_map.size) {
            for (j in 0 until count) {
                this.column_width_map[i].removeAt(y)
                this.column_width_maxes[i] = this.column_width_map[i].max()
            }
        }
        if (! ignore_ui) {
            (this.main_recycler.adapter as ColumnRecyclerAdapter).remove_rows(y, count)
            this.line_label_layout.remove_labels(y, count)
            (this.column_label_recycler.adapter as ColumnLabelAdapter).notifyDataSetChanged()
        }
    }

    fun new_column(index: Int, ignore_ui: Boolean = false) {
        val opus_manager = this.get_opus_manager()
        val column = mutableListOf<Int>()
        opus_manager.get_visible_channels().forEachIndexed { i: Int, channel: OpusChannel ->
            channel.lines.forEachIndexed { j: Int, line: OpusChannel.OpusLine ->
                val tree = opus_manager.get_beat_tree(BeatKey(i, j, index))
                if (tree.is_leaf()) {
                    column.add(1)
                } else {
                    column.add(tree.get_max_child_weight() * tree.size)
                }
            }
        }

        this.column_width_map.add(index, column)
        this.column_width_maxes.add(index, if (column.isNotEmpty()) {
            column.max()
        } else {
            1
        })

        if (! ignore_ui) {
            (this.column_label_recycler.adapter!! as ColumnLabelAdapter).add_column(index)
            (this.main_recycler.adapter as ColumnRecyclerAdapter).add_column(index)
        }
    }

    fun remove_column(index: Int, ignore_ui: Boolean = false) {
        this.column_width_map.removeAt(index)
        this.column_width_maxes.removeAt(index)
        if (! ignore_ui) {
            (this.column_label_recycler.adapter!! as ColumnLabelAdapter).remove_column(index)
            (this.main_recycler.adapter as ColumnRecyclerAdapter).remove_column(index)
        }
    }

    fun update_cursor(opusManagerCursor: OpusManagerCursor) {
        if (opusManagerCursor != this.active_cursor) {
            try {
                this.update_cursor(this.active_cursor)
            } catch (e: OpusTree.InvalidGetCall) {
                // Pass
            }
            this.active_cursor = opusManagerCursor.copy()
        }

        val opus_manager = this.get_opus_manager()
        val column_label_adapter = (this.column_label_recycler.adapter!! as ColumnLabelAdapter)
        when (opusManagerCursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                val beat_key = opusManagerCursor.get_beatkey()
                for (linked_key in opus_manager.get_all_linked(beat_key)) {
                    val y = try {
                        opus_manager.get_abs_offset(linked_key.channel, linked_key.line_offset)
                    } catch (e: IndexOutOfBoundsException) {
                        return
                    }

                    this.line_label_layout.notify_item_changed(y)
                    column_label_adapter.notifyItemChanged(linked_key.beat)
                    (this.main_recycler.adapter as ColumnRecyclerAdapter).notify_cell_changed(y, linked_key.beat, true)
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (top_left, bottom_right) = opusManagerCursor.range!!
                for (beat_key in opus_manager.get_beatkeys_in_range(top_left, bottom_right)) {

                    val y = try {
                        opus_manager.get_abs_offset(beat_key.channel, beat_key.line_offset)
                    } catch (e: IndexOutOfBoundsException) {
                        continue
                    }


                    this.line_label_layout.notify_item_changed(y)
                    column_label_adapter.notifyItemChanged(beat_key.beat)
                    (this.main_recycler.adapter as ColumnRecyclerAdapter).notify_cell_changed(y, beat_key.beat, true)
                }
            }
            OpusManagerCursor.CursorMode.Row -> {
                val y = try {
                    opus_manager.get_abs_offset(opusManagerCursor.channel, opusManagerCursor.line_offset)
                } catch (e: IndexOutOfBoundsException) {
                    return
                }

                (this.main_recycler.adapter as ColumnRecyclerAdapter).notify_row_changed(y, true)
                this.line_label_layout.notify_item_changed(y)
            }
            OpusManagerCursor.CursorMode.Column -> {
                (this.main_recycler.adapter as ColumnRecyclerAdapter).notify_column_state_changed(opusManagerCursor.beat)
                column_label_adapter.notifyItemChanged(opusManagerCursor.beat)
            }
            OpusManagerCursor.CursorMode.Unset -> { }
        }
    }

    fun apply_queued_cell_changes() {
        val done_set = mutableSetOf<BeatKey>()
        while (this._queued_cell_notifications.isNotEmpty()) {
            val beat_key = this._queued_cell_notifications.removeFirst()
            if (beat_key in done_set) {
                continue
            }

            val opus_manager = this.get_opus_manager()
            for (linked_beat_key in opus_manager.get_all_linked(beat_key)) {
                done_set.add(linked_beat_key)
            }

            this.notify_cell_change(beat_key)
        }
    }

    fun notify_cell_change(beat_key: BeatKey, ignore_ui: Boolean = false) {
        val opus_manager = this.get_opus_manager()
        if (opus_manager.history_cache.isLocked()) {
            this._queued_cell_notifications.add(beat_key)
            return
        }

        val main_recycler_adapter = (this.main_recycler.adapter!! as ColumnRecyclerAdapter)
        val percussion_visible = this.get_activity().configuration.show_percussion

        // Only one tree needs to be checked, since links are all the same
        val new_tree = opus_manager.get_beat_tree(beat_key)
        val new_cell_width = if (new_tree.is_leaf()) {
            1
        } else {
            new_tree.get_max_child_weight() * new_tree.size
        }

        val changed_beats = mutableSetOf<Int>()
        val changed_beat_keys = mutableSetOf<BeatKey>()
        for (linked_beat_key in opus_manager.get_all_linked(beat_key)) {
            if (!percussion_visible && opus_manager.is_percussion(linked_beat_key.channel)) {
                continue
            }
            val y = try {
                opus_manager.get_abs_offset(linked_beat_key.channel, linked_beat_key.line_offset)
            } catch (e: IndexOutOfBoundsException) {
                continue
            }

            val original_width = this.column_width_maxes[linked_beat_key.beat]
            this.column_width_map[linked_beat_key.beat][y] = new_cell_width
            this.column_width_maxes[linked_beat_key.beat] = this.column_width_map[linked_beat_key.beat].max()

            if (original_width != this.column_width_maxes[linked_beat_key.beat]) {
                changed_beats.add(linked_beat_key.beat)
            } else {
                changed_beat_keys.add(linked_beat_key)
            }
        }

        if (! ignore_ui) {
            // In set so as to not notify the same column multiple times
            for (beat in changed_beats) {
                this.column_label_recycler.adapter!!.notifyItemChanged(beat)
                main_recycler_adapter.notifyItemChanged(beat)
            }
            for (changed_key in changed_beat_keys) {
                // Don't bother notifying beat changed, was handled in column notification
                if (changed_key.beat in changed_beats) {
                    continue
                }
                main_recycler_adapter.notify_cell_changed(changed_key)
            }
        }
    }

    fun get_column_width(column: Int): Int {
        return this.column_width_maxes[column]
    }

    fun get_activity(): MainActivity {
        return this.context as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return (this.context as MainActivity).get_opus_manager()
    }

    fun update_line_label(channel: Int, line_offset: Int) {
        val y = this.get_opus_manager().get_abs_offset(channel, line_offset)
        this.line_label_layout.notify_item_changed(y)
    }

    fun scroll_to_position(x: Int? = null, y: Int? = null, position: List<Int>? = null, force: Boolean = false) {
        if (x != null) {
            if (x >= this.get_opus_manager().beat_count) {
                return
            }
            var position_visibility = this.get_position_visibility(x)
            if (force || position_visibility < SECTION_VIEW_PARTIAL_OVERSIZED) {
                this.scroll_to_x(x)
            }
        }
        if (y != null && (force || ! this.is_y_visible(y))) {
            this.scroll_to_y(y)
        }

        // TODO: implement this, but only when *actually* necessary
        //if (x != null && y != null && position != null) {
        //    val leaf = this.get_leaf(x, y, position) ?: return
        //    this.main_recycler.scrollBy(leaf.x.toInt(), 0)
        //}
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

        val y = opus_manager.get_abs_offset(beat_key.channel, beat_key.line_offset)
        if (position == null) {
            if (this.get_position_visibility(beat_key.beat) < SECTION_VIEW_PARTIAL_OVERSIZED) {
                this.scroll_to_x(beat_key.beat)
            }
        } else {
            var leaf_offset = 0
            tree = this.get_opus_manager().get_beat_tree(beat_key)
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

            val max_column_weight = this.column_width_maxes[beat_key.beat].toFloat()
            val position_offset = leaf_offset / max_cell_weight

            var position_visibility = this.get_position_visibility(beat_key.beat, Pair(position_offset, sibling_weight.toFloat() / max_cell_weight))
            if (position_visibility < SECTION_VIEW_PARTIAL_OVERSIZED) {
                val leaf_width = resources.getDimension(R.dimen.base_leaf_width)
                this.precise_scroll(
                    beat_key.beat,
                    0 - (leaf_width * (leaf_offset * max_column_weight / max_cell_weight)).toInt()
                )
            }
        }

        if (!this.is_y_visible(y)) {
            this.scroll_to_y(y)
        }

        // TODO: implement this, but only when *actually* necessary
        // val leaf = this.get_leaf(adj_beat_key, new_position) ?: return
        // this.main_recycler.scrollBy(leaf.x.toInt(), 0)
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
            val beat_leaf_count = this.column_width_maxes[beat]
            val beat_width = beat_leaf_count * leaf_width

            val section_start = (section.first * beat_width).roundToInt()
            val section_end = ((section.first + section.second) * beat_width).roundToInt()

            if (first_visible == last_visible) {
                val first_column = column_lm.findViewByPosition(first_visible)
                val fine_x = (first_column?.x ?: 0f).roundToInt()

                val beat_start_proceeds_view_start = section_start + fine_x > 0
                val beat_end_precedes_view_end = section_end + fine_x < visible_width
                val start_visible = beat_start_proceeds_view_start && (section_start + fine_x < visible_width)
                val end_visible = (section_end + fine_x > 0) && beat_end_precedes_view_end

                if (!beat_start_proceeds_view_start && !beat_end_precedes_view_end) {
                    SECTION_VIEW_PARTIAL_OVERSIZED
                } else if (start_visible && end_visible) {
                    SECTION_VIEW_COMPLETE
                } else if (start_visible && ! end_visible) {
                    SECTION_VIEW_PARTIAL_LEFT
                } else if (!start_visible && end_visible) {
                    SECTION_VIEW_PARTIAL_RIGHT
                } else {
                    SECTION_OUT_OF_VIEW
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
        val scroll_offset = this.line_label_layout.scrollY / line_height
        val height = this.scroll_view.measuredHeight / line_height
        return y >= scroll_offset && y <= (scroll_offset + height)
    }

    fun scroll_to_x(x: Int) {
        this._main_scroll_locked = true
        this.main_recycler.scrollToPosition(x)
        this._main_scroll_locked = false
        this._label_scroll_locked = true
        this.column_label_recycler.scrollToPosition(x)
        this._label_scroll_locked = false
    }

    fun scroll_to_y(y: Int) {
        val line_height = (resources.getDimension(R.dimen.line_height)).toInt()
        this.line_label_layout.scrollTo(0, y * line_height)
        this.scroll_view.scrollTo(0, y * line_height)
    }


    //  Unused. I don't know if i'll need them at any point
    //fun get_leaf(beat_key: BeatKey, position: List<Int>): LeafButton? {
    //    val y = this.get_opus_manager().get_abs_offset(beat_key.channel, beat_key.line_offset)
    //    return this.get_leaf(beat_key.beat, y, position)
    //}

    //fun get_leaf(x: Int, y: Int, position: List<Int>): LeafButton? {
    //    val column_view_holder = this.main_recycler.findViewHolderForAdapterPosition(x) ?: return null
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

        val line_lm = this.line_label_layout
        val line_height = (resources.getDimension(R.dimen.line_height)).toInt()
        val coarse_y = line_lm.scrollY / line_height
        val fine_y = line_lm.scrollY % line_height
        return Pair(
            Pair(coarse_x, fine_x.toInt()),
            Pair(coarse_y, fine_y)
        )
    }

    fun precise_scroll(x_coarse: Int = 0, x_fine: Int = 0, y_coarse: Int? = null, y_fine: Int? = null) {
        /*
            KLUDGE ALERT
            There's a wierd bug that returns the main lm to the first position if the x_fine == 0.
            so we force it to be -1 if that's the case.
         */
        val main_lm = (this.main_recycler.layoutManager!! as LinearLayoutManager)
        main_lm.scrollToPositionWithOffset(x_coarse, if (x_fine == 0) { -1 } else { x_fine })

        val column_label_lm = (this.column_label_recycler.layoutManager!! as LinearLayoutManager)
        column_label_lm.scrollToPositionWithOffset(x_coarse, if (x_fine == 0) { -1 } else { x_fine })

        if (y_coarse != null) {
            val line_height = (resources.getDimension(R.dimen.line_height)).toInt()
            this.scroll_view.scrollTo(0, (line_height * y_coarse) + (y_fine ?: 0))
        }
    }

    fun get_first_visible_column_index(): Int {
        return (this.main_recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    }

    fun update_percussion_visibility() {
        val main = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val percussion_channel = opus_manager.channels.last()
        if (main.configuration.show_percussion) {
            if (this.column_width_map.isNotEmpty()) {
                for (i in 0 until percussion_channel.size) {
                    this.new_row(this.line_label_layout.get_count(), percussion_channel.lines[i])
                }
            }
        } else {
            val target_line_count = opus_manager.get_visible_line_count()
            val current_line_count = opus_manager.get_total_line_count()
            for (i in target_line_count until current_line_count) {
                this.remove_row(target_line_count)
            }
        }
    }
}