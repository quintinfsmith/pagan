package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
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
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.structure.OpusTree
import kotlin.math.max
import com.qfs.pagan.InterfaceLayer as OpusManager

class EditorTable(context: Context, attrs: AttributeSet): TableLayout(context, attrs) {
    val main_recycler = ColumnRecycler(this)
    val line_label_layout = LineLabelColumnLayout(this)
    val column_label_recycler = ColumnLabelRecycler(context)
    val top_row = TableRow(context)
    val bottom_row = TableRow(context)
    val spacer = CornerView(context)
    val vertical_scroll_view = ScrollView(this.context)

    var initializing_column_width_map = false
    val column_width_map = mutableListOf<MutableList<Int>>()

    var active_cursor: OpusManagerCursor = OpusManagerCursor(OpusManagerCursor.CursorMode.Unset)

    init {
        this.top_row.addView(this.spacer)
        this.top_row.addView(this.column_label_recycler)

        this.spacer.setOnClickListener {
            val fragment = this.get_activity().get_active_fragment()
            if (fragment is EditorFragment) {
                fragment.shortcut_dialog()
            }
        }

        this.bottom_row.addView(LinearLayout(ContextThemeWrapper(context, R.style.column)))

        (this.bottom_row.getChildAt(0) as ViewGroup).layoutParams.width = MATCH_PARENT
        (this.bottom_row.getChildAt(0) as ViewGroup).layoutParams.height = WRAP_CONTENT
        (this.bottom_row.getChildAt(0) as ViewGroup).addView(this.line_label_layout)

        this.vertical_scroll_view.addView(this.main_recycler)

        this.bottom_row.addView(this.vertical_scroll_view)

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

        (this.vertical_scroll_view.layoutParams as LinearLayout.LayoutParams).weight = 1F
        this.vertical_scroll_view.layoutParams.width = 0
        this.vertical_scroll_view.layoutParams.height = MATCH_PARENT
        this.main_recycler.layoutParams.width = MATCH_PARENT
        this.main_recycler.layoutParams.height = WRAP_CONTENT

        (this.column_label_recycler.layoutParams as LinearLayout.LayoutParams).weight = 1F
        this.column_label_recycler.layoutParams.width = 0

        ColumnLabelAdapter(this)
        this.main_recycler.addOnScrollListener(HorizontalScrollListener(this.column_label_recycler))
        this.column_label_recycler.addOnScrollListener(HorizontalScrollListener(this.main_recycler))
        this.vertical_scroll_view.setOnScrollChangeListener { scroll_view: View, x: Int, y: Int, old_x: Int, old_y: Int -> }
    }

    fun clear() {
        this.column_width_map.clear()
        (this.main_recycler.adapter!! as ColumnRecyclerAdapter).clear()
        (this.column_label_recycler.adapter!! as ColumnLabelAdapter).clear()
        this.line_label_layout.clear()
    }

    fun setup() {
        this.init_column_width_map()
        val opus_manager = this.get_opus_manager()
        val main_adapter = (this.main_recycler.adapter as ColumnRecyclerAdapter)
        val column_label_adapter = (this.column_label_recycler.adapter as ColumnLabelAdapter)

        for (beat in 0 until opus_manager.beat_count) {
            main_adapter.add_column(beat)
            column_label_adapter.add_column(beat)
        }

        var y = 0
        opus_manager.get_visible_channels().forEach { channel: OpusChannel ->
            channel.lines.forEach {
                this.line_label_layout.insert_label(y++)
            }
        }
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
        }
        this.initializing_column_width_map = false
    }

    fun new_row(y: Int, opus_line: OpusChannel.OpusLine) {

        for (i in 0 until opus_line.beats.size) {
            val tree = opus_line.beats[i]
            val original_size = this.column_width_map[i].max()
            this.column_width_map[i].add(
                y,
                if (tree.is_leaf()) {
                    1
                } else {
                    tree.get_max_child_weight() * tree.size
                }
            )

            val new_size = this.column_width_map[i].max()
            if (new_size != original_size) {
                val cell_recycler = (this.main_recycler.adapter as ColumnRecyclerAdapter).get_column_layout(i)
                cell_recycler?.clear()

                this.main_recycler.adapter!!.notifyItemChanged(i)
                //if (cell_recycler != null) {
                //    cell_recycler.adapter!!.notifyItemRangeChanged(y + 1, cell_recycler.adapter!!.itemCount)
                //}
            } else {
                val cell_recycler = (this.main_recycler.adapter as ColumnRecyclerAdapter).get_column_layout(i) ?: continue
                cell_recycler.insert_cell(y)
                //cell_recycler.adapter!!.notifyItemRangeChanged(y + 1, cell_recycler.adapter!!.itemCount)
            }
        }

        this.line_label_layout.insert_label(y)
    }

    fun new_channel_rows(y: Int, opus_lines: List<OpusChannel.OpusLine>) {
        opus_lines.forEachIndexed { i: Int, line: OpusChannel.OpusLine ->
            this.new_row(y + i, line)
        }

        this.line_label_layout.insert_label(y)

        //(this.main_recycler.adapter as ColumnRecyclerAdapter).apply_to_visible_columns {
        //    it.notifyItemRangeChanged(y + opus_lines.size, (last_visible_row + 1) - (y + opus_lines.size))
        //}
    }

    fun remove_row(y: Int) {

        for (i in 0 until this.column_width_map.size) {
            val original_size = this.column_width_map[i].max()
            this.column_width_map[i].removeAt(y)
            val new_size = this.column_width_map[i].max()
            if (new_size != original_size) {

                (main_recycler.adapter as ColumnRecyclerAdapter).get_column_layout(i)?.clear()

                this.main_recycler.adapter!!.notifyItemChanged(i)
                this.column_label_recycler.adapter!!.notifyItemChanged(i)
            } else {
                val cell_recycler = (this.main_recycler.adapter as ColumnRecyclerAdapter).get_column_layout(i) ?: continue
                cell_recycler.remove_cell(y)
            }
        }

        this.line_label_layout.remove_label(y)
    }

    fun remove_channel_rows(y: Int, count: Int) {
        for (i in 0 until this.column_width_map.size) {
            val original_size = this.column_width_map[i].max()
            for (j in 0 until count) {
                this.column_width_map[i].removeAt(y)
            }

            val new_size = this.column_width_map[i].max()
            val cell_recycler = (this.main_recycler.adapter as ColumnRecyclerAdapter).get_column_layout(i)
            if (new_size != original_size) {
                cell_recycler?.clear()

                this.main_recycler.adapter!!.notifyItemChanged(i)
                this.column_label_recycler.adapter!!.notifyItemChanged(i)
            } else if (cell_recycler != null) {
                for (j in 0 until count) {
                    cell_recycler.remove_cell(y)
                }
            } else {
                continue
            }
            //if (cell_recycler != null) {
            //    cell_recycler.adapter!!.notifyItemRangeChanged(
            //        y,
            //        cell_recycler.adapter!!.itemCount
            //    )
            //}
        }
        for (j in 0 until count) {
            this.line_label_layout.remove_label(y)
        }
    }


    fun new_column(index: Int) {
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
        (this.main_recycler.adapter!! as ColumnRecyclerAdapter).add_column(index)
        (this.column_label_recycler.adapter!! as ColumnLabelAdapter).add_column(index)
    }


    fun remove_column(index: Int) {
        this.column_width_map.removeAt(index)
        (this.main_recycler.adapter as ColumnRecyclerAdapter).remove_column(index)
        (this.column_label_recycler.adapter!! as ColumnLabelAdapter).remove_column(index)
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
        val main_recycler_adapter = (this.main_recycler.adapter!! as ColumnRecyclerAdapter)
        val column_label_adapter = (this.column_label_recycler.adapter!! as ColumnLabelAdapter)
        when (opusManagerCursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                val beat_key = opusManagerCursor.get_beatkey()

                val y = try {
                    opus_manager.get_abs_offset(beat_key.channel, beat_key.line_offset)
                } catch (e: IndexOutOfBoundsException) {
                    return
                }

                line_label_adapter.notifyItemChanged(y)
                column_label_adapter.notifyItemChanged(beat_key.beat)

                val cell_recycler = main_recycler_adapter.get_column_layout(beat_key.beat) ?: return
                cell_recycler.notify_state_change(y)
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (top_left, bottom_right) = opusManagerCursor.range!!
                for (beat_key in opus_manager.get_beatkeys_in_range(top_left, bottom_right)) {

                    val y = try {
                        opus_manager.get_abs_offset(beat_key.channel, beat_key.line_offset)
                    } catch (e: IndexOutOfBoundsException) {
                        continue
                    }


                    line_label_adapter.notifyItemChanged(y)
                    column_label_adapter.notifyItemChanged(beat_key.beat)

                    // Can ignore if the cell_recycler isn't visible
                    val cell_recycler = main_recycler_adapter.get_column_layout(beat_key.beat) ?: continue
                    cell_recycler.notify_state_change(y)
                }
            }
            OpusManagerCursor.CursorMode.Row -> {
                val y = try {
                    opus_manager.get_abs_offset(opusManagerCursor.channel, opusManagerCursor.line_offset)
                } catch (e: IndexOutOfBoundsException) {
                    return
                }

                for (beat in 0 until main_recycler_adapter.itemCount) {
                    line_label_adapter.notifyItemChanged(y)
                    column_label_adapter.notifyItemChanged(beat)
                    val cell_recycler = main_recycler_adapter.get_column_layout(beat) ?: continue
                    cell_recycler.notify_state_change(y)
                }
            }
            OpusManagerCursor.CursorMode.Column -> {
                var y = 0
                opus_manager.get_visible_channels().forEachIndexed { i: Int, channel: OpusChannel ->
                    channel.lines.forEachIndexed { j: Int, line: OpusChannel.OpusLine ->
                        val cell_recycler = main_recycler_adapter.get_column_layout(opusManagerCursor.beat)
                        if (cell_recycler != null) {
                            cell_recycler.notify_state_change(y)
                        }
                        y += 1
                    }
                }
                column_label_adapter.notifyItemChanged(opusManagerCursor.beat)
            }
            OpusManagerCursor.CursorMode.Unset -> { }
        }
    }

    fun notify_cell_change(beat_key: BeatKey) {
        val opus_manager = this.get_opus_manager()
        val main_recycler_adapter = (this.main_recycler.adapter!! as ColumnRecyclerAdapter)
        // Only one tree needs to be checked, since links are all the same
        val new_tree = opus_manager.get_beat_tree(beat_key)
        val new_cell_width = if (new_tree.is_leaf()) {
            1
        } else {
            new_tree.get_max_child_weight() * new_tree.size
        }

        for (linked_beat_key in opus_manager.get_all_linked(beat_key)) {
            val y = try {
                opus_manager.get_abs_offset(linked_beat_key.channel, linked_beat_key.line_offset)
            } catch (e: IndexOutOfBoundsException) {
                continue
            }

            val original_width = this.column_width_map[linked_beat_key.beat].max()
            this.column_width_map[linked_beat_key.beat][y] = new_cell_width
            val new_width = this.column_width_map[linked_beat_key.beat].max()

            if (original_width != new_width) {
                main_recycler_adapter.notifyItemChanged(linked_beat_key.beat)
                this.column_label_recycler.adapter!!.notifyItemChanged(linked_beat_key.beat)
            } else {
                // Can ignore if the cell_recycler isn't visble
                val cell_recycler = main_recycler_adapter.get_column_layout(linked_beat_key.beat) ?: continue
                //cell_recycler.adapter?.notifyItemChanged(y)
                cell_recycler.notify_state_change(y)
            }
        }
    }

    fun get_column_width(column: Int): Int {
        return if (this.column_width_map[column].isEmpty()) {
            1
        } else {
            this.column_width_map[column].max()
        }
    }

    fun get_activity(): MainActivity {
        return this.context as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return (this.context as MainActivity).get_opus_manager()
    }

    fun update_line_label(channel: Int, line_offset: Int) {
        val y = this.get_opus_manager().get_abs_offset(channel, line_offset)
        this.line_label_recycler.adapter!!.notifyItemChanged(y)
    }

    fun scroll_to_position(x: Int? = null, y: Int? = null, position: List<Int>? = null, force: Boolean = false) {
        if (x != null && (force || ! this.is_x_visible(x))) {
            this.scroll_to_x(x)
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

        if (! this.is_x_visible(beat_key.beat)) {
            this.scroll_to_x(beat_key.beat)
        }
        val y = opus_manager.get_abs_offset(beat_key.channel, beat_key.line_offset )
        if (! this.is_y_visible(y)) {
            this.scroll_to_y(y)
        }
        // TODO: implement this, but only when *actually* necessary
        // val leaf = this.get_leaf(adj_beat_key, new_position) ?: return
        // this.main_recycler.scrollBy(leaf.x.toInt(), 0)
    }

    fun is_x_visible(x: Int): Boolean {
        return this.column_label_recycler.findViewHolderForAdapterPosition(x) != null
        //val layout = this.column_label_recycler.layoutManager!! as LinearLayoutManager
        //val first = layout.findFirstVisibleItemPosition()
        //val last = layout.findLastVisibleItemPosition()
        //return x in first..last
    }
    fun is_y_visible(y: Int): Boolean {
        return this.line_label_recycler.findViewHolderForAdapterPosition(y) != null
        //val layout = this.line_label_recycler.layoutManager!! as LinearLayoutManager
        //val first = layout.findFirstVisibleItemPosition()
        //val last = layout.findLastVisibleItemPosition()
        //return y in first..last
    }

    fun scroll_to_x(x: Int) {
        this.main_recycler.lock_scroll_propagation()
        this.main_recycler.scrollToPosition(x)
        this.main_recycler.unlock_scroll_propagation()
        this.column_label_recycler.lock_scroll_propagation()
        this.column_label_recycler.scrollToPosition(x)
        this.column_label_recycler.unlock_scroll_propagation()
    }
    fun scroll_to_y(y: Int) {
        this.line_label_recycler.scrollToPosition(y)
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

        val line_lm = this.line_label_recycler.layoutManager!! as LinearLayoutManager
        val coarse_y = line_lm.findFirstVisibleItemPosition()
        val line = line_lm.findViewByPosition(coarse_y)
        val fine_y = line?.y ?: 0
        return Pair(
            Pair(coarse_x, fine_x.toInt()),
            Pair(coarse_y, fine_y.toInt())
        )
    }

    fun precise_scroll(x_coarse: Int = 0, x_fine: Int = 0, y_coarse: Int = 0, y_fine: Int = 0) {
        this.main_recycler.lock_scroll_propagation()
        val main_lm = (this.main_recycler.layoutManager!! as LinearLayoutManager)
        main_lm.scrollToPositionWithOffset(x_coarse, x_fine)

        this.column_label_recycler.lock_scroll_propagation()
        val column_label_lm = (this.column_label_recycler.layoutManager!! as LinearLayoutManager)
        column_label_lm.scrollToPositionWithOffset(x_coarse, x_fine)

        this.main_recycler.unlock_scroll_propagation()
        this.column_label_recycler.unlock_scroll_propagation()

        val line_label_lm = (this.line_label_recycler.layoutManager!! as LinearLayoutManager)
        line_label_lm.scrollToPositionWithOffset(y_coarse, y_fine)
    }

    fun get_first_visible_column_index(): Int {
        return (this.main_recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    }

    fun update_percussion_visibility() {
        val main = this.get_activity()
        val percussion_channel = this.get_opus_manager().channels.last()
        if (main.configuration.show_percussion) {
            for (i in 0 until percussion_channel.size) {
                this.new_row(this.line_label_recycler.adapter!!.itemCount, percussion_channel.lines[i])
            }
        } else {
            for (i in 0 until percussion_channel.size) {
                this.remove_row(this.line_label_recycler.adapter!!.itemCount - 1)
            }
        }
    }
}