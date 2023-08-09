package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TableLayout
import android.widget.TableRow
import androidx.core.view.children
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusChannel
import kotlin.math.max
import com.qfs.pagan.InterfaceLayer as OpusManager

class EditorTable(context: Context, attrs: AttributeSet): TableLayout(context, attrs) {
    val main_recycler = ColumnRecycler(this, context, attrs)
    val line_label_recycler = LineLabelRecyclerView(context, attrs)
    val column_label_recycler = ColumnLabelRecycler(this, context, attrs)
    val top_row = TableRow(context, attrs)
    val bottom_row = TableRow(context, attrs)
    val spacer = CornerView(context, attrs)
    val vertical_scroll_listener = VerticalScrollListener(this)

    var initializing_column_width_map = false
    val column_width_map = mutableListOf<MutableList<Int>>()

    var active_cursor: Cursor = Cursor(Cursor.CursorMode.Unset)

    init {
        this.top_row.addView(this.spacer)
        this.top_row.addView(this.column_label_recycler)

        this.bottom_row.addView(this.line_label_recycler)
        this.bottom_row.addView(this.main_recycler)

        this.addView(this.top_row)
        this.addView(this.bottom_row)

        ColumnLabelAdapter(this)
        ColumnRecyclerAdapter(this)
        LineLabelRecyclerAdapter(this)
        this.main_recycler.addOnScrollListener(HorizontalScrollListener(this.column_label_recycler))
        this.column_label_recycler.addOnScrollListener(HorizontalScrollListener(this.main_recycler))
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.column_width_map.clear()
        (this.top_row.layoutParams as LayoutParams).apply {
            width = MATCH_PARENT
            height = WRAP_CONTENT
        }

        (this.bottom_row.layoutParams as LayoutParams).apply {
            width = MATCH_PARENT
            height = WRAP_CONTENT
        }

        this.spacer.layoutParams.apply {
            width = MATCH_PARENT
            height = MATCH_PARENT
        }

        this.line_label_recycler.layoutParams.apply {
            width = 100
            height = MATCH_PARENT
        }

        (this.main_recycler.layoutParams as TableRow.LayoutParams).apply {
            width = 0
            weight = 1F
            height = MATCH_PARENT
        }

        (this.column_label_recycler.layoutParams as TableRow.LayoutParams).apply {
            width = 0
            weight = 1F
        }
    }

    fun clear() {
        this.column_width_map.clear()
        (this.main_recycler.adapter!! as ColumnRecyclerAdapter).clear()
        (this.column_label_recycler.adapter!! as ColumnLabelAdapter).clear()
        (this.line_label_recycler.adapter!! as LineLabelRecyclerAdapter).clear()
    }

    fun setup() {
        this.init_column_width_map()
        val opus_manager = this.get_opus_manager()
        val main_adapter = (this.main_recycler.adapter as ColumnRecyclerAdapter)
        val column_label_adapter = (this.column_label_recycler.adapter as ColumnLabelAdapter)
        for (beat in 0 until opus_manager.opus_beat_count) {
            main_adapter.add_column(beat)
            column_label_adapter.add_column(beat)
        }

        val line_label_adapter = (this.line_label_recycler.adapter as LineLabelRecyclerAdapter)
        for (y in 0 until opus_manager.get_total_line_count()) {
            line_label_adapter.add_label(y)
        }

        this.main_recycler.minimumHeight = this.get_opus_manager().get_total_line_count() * (resources.getDimension(R.dimen.line_height).toInt())
    }

    fun init_column_width_map() {
        this.initializing_column_width_map = true
        val opus_manager = this.get_opus_manager()
        for (beat in 0 until opus_manager.opus_beat_count) {
            this.column_width_map.add(mutableListOf<Int>())
            opus_manager.channels.forEachIndexed { i: Int, channel: OpusChannel ->
                channel.lines.forEachIndexed { j: Int, line: OpusChannel.OpusLine ->
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
        val label_adapter = this.line_label_recycler.adapter as LineLabelRecyclerAdapter

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
                val cell_recycler = (this.main_recycler.adapter as ColumnRecyclerAdapter).get_cell_recycler(i)
                if (cell_recycler != null) {
                    (cell_recycler.adapter!! as CellRecyclerAdapter).clear()
                }

                this.main_recycler.adapter!!.notifyItemChanged(i)
                label_adapter.notifyItemChanged(i)
            } else {
                val cell_recycler =
                    (this.main_recycler.adapter as ColumnRecyclerAdapter).get_cell_recycler(i)
                        ?: continue
                (cell_recycler.adapter!! as CellRecyclerAdapter).insert_cell(y)
            }
        }

        label_adapter.add_label(y)

        // This needs to be reset here since the main_recycler and cell_recyclers need to have a fixed height.
        this.main_recycler.minimumHeight = this.get_opus_manager().get_total_line_count() * (resources.getDimension(R.dimen.line_height).toInt())
    }

    fun remove_row(y: Int) {
        val label_adapter = this.line_label_recycler.adapter as LineLabelRecyclerAdapter

        for (i in 0 until this.column_width_map.size) {
            val original_size = this.column_width_map[i].max()
            this.column_width_map[i].removeAt(y)
            val new_size = this.column_width_map[i].max()
            if (new_size != original_size) {
                val cell_recycler = (this.main_recycler.adapter as ColumnRecyclerAdapter).get_cell_recycler(i)
                if (cell_recycler != null) {
                    (cell_recycler.adapter!! as CellRecyclerAdapter).clear()
                }

                this.main_recycler.adapter!!.notifyItemChanged(i)
                label_adapter.notifyItemChanged(i)
            } else {
                val cell_recycler =
                    (this.main_recycler.adapter as ColumnRecyclerAdapter).get_cell_recycler(i)
                        ?: continue
                (cell_recycler.adapter!! as CellRecyclerAdapter).remove_cell(y)
            }
        }

        label_adapter.remove_label(y)
        // This needs to be reset here since the main_recycler and cell_recyclers need to have a fixed height.
        this.main_recycler.minimumHeight = this.get_opus_manager().get_total_line_count() * (resources.getDimension(R.dimen.line_height).toInt())
    }

    fun new_column(index: Int) {
        val opus_manager = this.get_opus_manager()
        val column = mutableListOf<Int>()
        opus_manager.channels.forEachIndexed { i: Int, channel: OpusChannel ->
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

    fun update_cursor(cursor: Cursor) {
        if (cursor != this.active_cursor) {
            this.update_cursor(this.active_cursor)
            this.active_cursor = cursor.copy()
        }

        val opus_manager = this.get_opus_manager()
        val main_recycler_adapter = (this.main_recycler.adapter!! as ColumnRecyclerAdapter)
        val line_label_adapter = (this.line_label_recycler.adapter!! as LineLabelRecyclerAdapter)
        val column_label_adapter = (this.column_label_recycler.adapter!! as ColumnLabelAdapter)
        when (cursor.mode) {
            Cursor.CursorMode.Single -> {
                val beat_key = cursor.get_beatkey()

                val y = try {
                    opus_manager.get_abs_offset(beat_key.channel, beat_key.line_offset)
                } catch (e: IndexOutOfBoundsException) {
                    return
                }

                line_label_adapter.notifyItemChanged(y)
                column_label_adapter.notifyItemChanged(beat_key.beat)

                val cell_recycler = main_recycler_adapter.get_cell_recycler(beat_key.beat) ?: return
                (cell_recycler.adapter as CellRecyclerAdapter).notifyItemChanged(y)

            }
            Cursor.CursorMode.Range -> {
                val (top_left, bottom_right) = cursor.range!!
                for (beat_key in opus_manager.get_beatkeys_in_range(top_left, bottom_right)) {

                    val y = try {
                        opus_manager.get_abs_offset(beat_key.channel, beat_key.line_offset)
                    } catch (e: IndexOutOfBoundsException) {
                        continue
                    }

                    line_label_adapter.notifyItemChanged(y)
                    column_label_adapter.notifyItemChanged(beat_key.beat)

                    // Can ignore if the cell_recycler isn't visible
                    val cell_recycler = main_recycler_adapter.get_cell_recycler(beat_key.beat) ?: continue
                    cell_recycler.adapter?.notifyItemChanged(y)
                }
            }
            Cursor.CursorMode.Row -> {
                val y = try {
                    opus_manager.get_abs_offset(cursor.channel, cursor.line_offset)
                } catch (e: IndexOutOfBoundsException) {
                    return
                }

                for (beat in 0 until main_recycler_adapter.itemCount) {
                    line_label_adapter.notifyItemChanged(y)
                    column_label_adapter.notifyItemChanged(beat)
                    val cell_recycler = main_recycler_adapter.get_cell_recycler(beat) ?: continue
                    cell_recycler.adapter?.notifyItemChanged(y)
                }
            }
            Cursor.CursorMode.Column -> {
                var y = 0
                opus_manager.channels.forEachIndexed { i: Int, channel: OpusChannel ->
                    channel.lines.forEachIndexed { j: Int, line: OpusChannel.OpusLine ->
                        val cell_recycler = main_recycler_adapter.get_cell_recycler(cursor.beat)
                        if (cell_recycler != null) {
                            cell_recycler.adapter?.notifyItemChanged(y)
                        }
                        y += 1
                    }
                }
                column_label_adapter.notifyItemChanged(cursor.beat)
            }
            Cursor.CursorMode.Unset -> { }
        }
    }

    fun notify_cell_change(beat_key: BeatKey) {
        val opus_manager = this.get_opus_manager()
        val main_recycler_adapter = (this.main_recycler.adapter!! as ColumnRecyclerAdapter)
        // Only one tree needs to be checked, since links are all the same
        val new_tree = opus_manager.get_beat_tree(beat_key)
        val new_cell_width = new_tree.get_max_child_weight() * new_tree.size
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
            }

            // Can ignore if the cell_recycler isn't visible
            val cell_recycler = main_recycler_adapter.get_cell_recycler(linked_beat_key.beat) ?: continue
            cell_recycler.adapter?.notifyItemChanged(y)
        }
    }

    fun get_column_width(column: Int): Int {
        return this.column_width_map[column].max()
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

    // Aligns all columns with the line_label recycler
    fun fix_scroll_offset() {
        val main_adapter = (this.main_recycler.adapter!! as ColumnRecyclerAdapter)
        for (i in 0 until this.get_opus_manager().opus_beat_count) {
            var cell_recycler = main_adapter.get_cell_recycler(i) ?: continue
            cell_recycler.conform_scroll_position()
        }
    }

    fun scroll_to_position(x: Int? = null, y: Int? = null, position: List<Int>? = null) {
        if (x != null) {
            this.scroll_to_x(x)
        }
        if (y != null) {
            this.scroll_to_y(y)
        }

       // val leaf = this.get_leaf(x, y, new_position) ?: return
       // this.main_recycler.scrollBy(leaf.x.toInt(), 0)
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

        this.scroll_to_x(beat_key.beat)
        this.scroll_to_y( opus_manager.get_abs_offset(beat_key.channel, beat_key.line_offset ))
        val leaf = this.get_leaf(adj_beat_key, new_position) ?: return

        this.main_recycler.scrollBy(leaf.x.toInt(), 0)
    }

    fun scroll_to_x(x: Int) {
        this.main_recycler.scrollToPosition(x)
        this.column_label_recycler.scrollToPosition(x)
    }
    fun scroll_to_y(y: Int) {
        this.line_label_recycler.scrollToPosition(y)
        this.fix_scroll_offset()
    }

    fun get_leaf(beat_key: BeatKey, position: List<Int>): LeafButton? {
        val y = this.get_opus_manager().get_abs_offset(beat_key.channel, beat_key.line_offset)
        return this.get_leaf(beat_key.beat, y, position)
    }

    fun get_leaf(x: Int, y: Int, position: List<Int>): LeafButton? {
        val column_view_holder = this.main_recycler.findViewHolderForAdapterPosition(x) ?: return null
        val cell_recycler = (column_view_holder as ColumnRecyclerViewHolder).get_cell_recycler() ?: return null
        val cell_view_holder = cell_recycler.findViewHolderForAdapterPosition(y) ?: return null
        val cell_layout = (cell_view_holder as CellRecyclerViewHolder).get_cell_layout()
        for (child in (cell_layout as ViewGroup).children) {
            if ((child as LeafButton).position == position) {
                return child
            }
        }
        return null

    }


}