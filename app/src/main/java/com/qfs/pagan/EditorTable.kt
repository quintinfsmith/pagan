package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.InterfaceLayer as OpusManager

class EditorTable(context: Context, attrs: AttributeSet): TableLayout(context, attrs) {
    val main_recycler = ColumnRecycler(this, context, attrs)
    val line_label_recycler = LineLabelRecyclerView(context, attrs)
    val column_label_recycler = ColumnLabelRecycler(this, context, attrs)
    val top_row = TableRow(context, attrs)
    val bottom_row = TableRow(context, attrs)
    val spacer = LinearLayout(context, attrs)
    val vertical_scroll_listener = VerticalScrollListener(this)
    val horizontal_scroll_listener = HorizontalScrollListener()

    var initializing_column_width_map = false
    val column_width_map = mutableListOf<MutableList<Int>>()

    init {
        this.top_row.addView(this.spacer)
        //this.top_row.addView(this.column_label_recycler)

        this.bottom_row.addView(this.line_label_recycler)
        this.bottom_row.addView(this.main_recycler)

        this.addView(this.top_row)
        this.addView(this.bottom_row)

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()

        val column_label_adapter = ColumnLabelAdapter(this)
        //column_label_adapter.notifyItemRangeInserted(0, opus_manager.opus_beat_count)

        val main_adapter = ColumnRecyclerAdapter(this)
        this.main_recycler.addOnScrollListener(this.horizontal_scroll_listener)
        //main_adapter.notifyItemRangeInserted(0, opus_manager.opus_beat_count)

        val line_label_adapter = LineLabelRecyclerAdapter(this)
        line_label_adapter.notifyItemRangeInserted(0, opus_manager.get_total_line_count())
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.column_width_map.clear()
        (this.top_row.layoutParams as LayoutParams).apply {
            width = MATCH_PARENT
            //height = resources.getDimension(R.dimen.line_height).toInt()
            height = WRAP_CONTENT
        }

        (this.bottom_row.layoutParams as LayoutParams).apply {
            width = MATCH_PARENT
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
        }
    }

    fun clear() {
        var original_size = this.column_width_map.size
        this.column_width_map.clear()
    }

    fun setup() {
        this.clear()
        this.init_column_width_map()
        val opus_manager = this.get_opus_manager()
        for (beat in 0 until opus_manager.opus_beat_count) {
            (this.main_recycler.adapter as ColumnRecyclerAdapter).add_column(beat)
        }
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
        for (i in 0 until opus_line.beats.size) {
            val tree = opus_line.beats[i]
            this.column_width_map[i].add(
                y,
                if (tree.is_leaf()) {
                    1
                } else {
                    tree.get_max_child_weight() * tree.size
                }
            )
            val cell_recycler = (this.main_recycler.adapter as ColumnRecyclerAdapter).get_cell_recycler(i) ?: continue
            (cell_recycler.adapter!! as CellRecyclerAdapter).insert_cell(y)
        }
    }

    fun remove_row(y: Int) {
        for (i in 0 until this.column_width_map.size) {
            this.column_width_map[i].removeAt(y)
            val cell_recycler = (this.main_recycler.adapter as ColumnRecyclerAdapter).get_cell_recycler(i) ?: continue
            (cell_recycler.adapter!! as CellRecyclerAdapter).remove_cell(y)
        }
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
    }


    fun remove_column(index: Int) {
        this.column_width_map.removeAt(index)
        (this.main_recycler.adapter as ColumnRecyclerAdapter).remove_column(index)
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
}