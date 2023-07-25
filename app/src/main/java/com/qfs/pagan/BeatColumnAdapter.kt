package com.qfs.pagan

import android.content.Context
import android.view.*
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.BeatKey
import java.lang.Integer.max
import java.lang.Integer.min
import com.qfs.pagan.InterfaceLayer as OpusManager

class BeatColumnAdapter(var parent_fragment: EditorFragment, var recycler: RecyclerView, var column_label_layout: ColumnLabelAdapter) : RecyclerView.Adapter<BeatColumnAdapter.ColumnViewHolder>() {
    class BeatCellRecycler(beat_column_adapter: BeatColumnAdapter): RecyclerView(beat_column_adapter.recycler.context) {
        class BCLayoutManager(context: Context): LinearLayoutManager(context, VERTICAL, false)
        var viewHolder: ColumnViewHolder? = null
        /*
         * update_queued exists to handle the liminal state between being detached and being destroyed
         * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
         */
        var update_queued = false
        init {
            this.adapter = BeatCellAdapter(this)
            this.layoutManager = BCLayoutManager(this.context)
        }

        override fun onDetachedFromWindow() {
            this.update_queued = true
            super.onDetachedFromWindow()
        }

    }

    class TableOnScrollListener: RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, x: Int, y: Int) {
            super.onScrolled(recyclerView, x, y)
            val adapter = recyclerView.adapter as BeatColumnAdapter
            if (!adapter._scroll_lock_this) {
                adapter._scroll_lock_columns = true
                adapter.column_label_layout.scroll(x)
                adapter._scroll_lock_columns = false
            }
        }
    }

    class ColumnLabelOnScrollListener(private var omAdapter: BeatColumnAdapter): RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, x: Int, y: Int) {
            super.onScrolled(recyclerView, x, y)
            if (!omAdapter._scroll_lock_columns) {
                omAdapter._scroll_lock_this = true
                omAdapter.recycler.scrollBy(x, y)
                omAdapter._scroll_lock_this= false
            }
        }
    }

    class OpusManagerLayoutManager(context: Context): LinearLayoutManager(context, HORIZONTAL, false)
    class ColumnViewHolder(var parent_adapter: BeatColumnAdapter, itemView: BeatCellRecycler) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.viewHolder = this
        }
    }

    private var beat_count = 0
    var linking_beat: BeatKey? = null
    var linking_beat_b: BeatKey? = null
    var _scroll_lock_this: Boolean = false
    var _scroll_lock_columns: Boolean = false
    private var table_scroll_listener: TableOnScrollListener
    private var column_scroll_listener: ColumnLabelOnScrollListener

    init {
        this.recycler.adapter = this
        this.recycler.layoutManager = OpusManagerLayoutManager(this.recycler.context)
        this.recycler.itemAnimator = null

        val that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    for (i in 0 until count) {
                        that.column_label_layout.removeColumnLabel(start + i)
                    }
                    val last_position = (that.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    val beats_to_refresh: Set<Int> = (start + count .. last_position).toSet()
                    that.refresh_leaf_labels(beats_to_refresh)
                }
                override fun onItemRangeChanged(start: Int, count: Int) {
                    for (i in start until that.itemCount) {
                        val viewHolder = that.recycler.findViewHolderForAdapterPosition(i) ?: continue

                        // TODO: Limit to visible and needing change
                        val adapter = (viewHolder.itemView as BeatCellRecycler)!!.adapter!!
                        adapter.notifyItemRangeChanged(0, adapter.itemCount)

                        that.column_label_layout.notifyItemChanged(i)
                    }
                }
                override fun onItemRangeInserted(start: Int, count: Int) {
                    that.set_cursor_focus(false)
                    val visible_start = (that.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    if (visible_start >= start) {
                        that.recycler.scrollToPosition(start)
                    }
                }
            }
        )

        this.table_scroll_listener = TableOnScrollListener()
        this.column_scroll_listener = ColumnLabelOnScrollListener(this)

        this.enableScrollSync()
    }

    //private fun adjust_beat_width(holder: BCViewHolder) {
    //    val opus_manager = this.get_opus_manager()
    //    // resize Columns
    //    var max_width = 0
    //    val beat = this.get_beat()
    //    for (channel in 0 until opus_manager.channels.size) {
    //        for (line_offset in 0 until opus_manager.channels[channel].size) {
    //            val tree = opus_manager.get_beat_tree(BeatKey(channel, line_offset, beat))
    //            val size = max(1, tree.size) * tree.get_max_child_weight()
    //            max_width = max(max_width, size)
    //        }
    //    }

    //    for (channel in 0 until opus_manager.channels.size) {
    //        for (line_offset in 0 until opus_manager.channels[channel].size) {
    //            this.__tick_resize_beat_cell(holder, channel, line_offset, beat, max_width)
    //        }
    //    }
    //}

    override fun onViewAttachedToWindow(holder: ColumnViewHolder) {
        val item_view = holder.itemView as BeatCellRecycler
        val beat_index = holder.bindingAdapterPosition

        // Redraw Items that were detached but not destroyed
        if (item_view.update_queued) {
            //this.updateItem(holder, beat_index)
            item_view.update_queued = false
        }
    }

    override fun onBindViewHolder(holder: ColumnViewHolder, position: Int) {
        // TODO: Limit to visible
        (holder.itemView as BeatCellRecycler).adapter!!.notifyItemRangeChanged(0, this.get_opus_manager().get_channel_line_counts().sum())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnViewHolder {
        return ColumnViewHolder(this, BeatCellRecycler(this))
    }

    override fun getItemCount(): Int {
        return this.beat_count
    }

    fun addBeatColumn(index: Int) {
        this.beat_count += 1
        this.column_label_layout.addColumnLabel(index)
        this.notifyItemInserted(index)
    }

    fun removeBeatColumn(index: Int) {
        if (this.beat_count > 0) {
            this.beat_count -= 1
        }
        this.notifyItemRemoved(index)
    }

    private fun apply_to_visible_columns(callback: (adapter: BeatCellAdapter) -> Unit) {
        val first = (this.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val last = (this.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        for (i in max(0, first - 5) .. min(last + 5, this.get_opus_manager().opus_beat_count - 1)) {
            val viewHolder = this.recycler.findViewHolderForAdapterPosition(i) ?: continue
            var column_recycler = (viewHolder.itemView as BeatCellRecycler)
            var adapter = column_recycler.adapter!! as BeatCellAdapter
            callback(adapter)
        }
    }

    fun remove_line(y: Int) {
        this.apply_to_visible_columns {
            it.remove_line(y)
        }
    }

    fun insert_line(y: Int) {
        this.apply_to_visible_columns {
            it.insert_line(y)
        }
    }

    fun get_main_activity(): MainActivity {
        return this.parent_fragment.get_main()
    }

    fun get_opus_manager(): OpusManager {
        return this.get_main_activity().get_opus_manager()
    }

    fun get_leaf_view(beat_key: BeatKey, position: List<Int>): LeafButton? {
        // Get the full-beat view
        val column_view_holder = this.recycler.findViewHolderForAdapterPosition(beat_key.beat) ?: return null
        var working_view = column_view_holder.itemView as ViewGroup
        working_view = working_view.getChildAt(0) as ViewGroup
        for (i in 0 until working_view.childCount) {
            val leaf_button = working_view.getChildAt(i) as LeafButton
            if (leaf_button.position_node.to_list() == position) {
                return leaf_button
            }
        }
        return null
    }

    private fun get_all_leaf_views(beat_key: BeatKey, position: List<Int>? = null): List<LeafButton>? {
        val opus_manager = this.get_opus_manager()
        return try {
            val y = opus_manager.get_abs_offset(beat_key.channel, beat_key.line_offset)
            this.get_all_leaf_views(y, beat_key.beat, position)
        } catch (e: IndexOutOfBoundsException) {
            listOf()
        }
    }

    private fun get_all_leaf_views(y: Int, x: Int, position: List<Int>? = null): List<LeafButton>? {
        // NOTE: A Bad position MAY be passed. eg, if the cursor doesn't reflect that a node was removed
        // Those should be treated as if they just don't exist here

        // Get the full-beat view
        val column_view_holder = this.recycler.findViewHolderForAdapterPosition(x) ?: return null
        val target_position = position ?: listOf()
        var column_recycler = (column_view_holder.itemView as BeatCellRecycler)
        var beat_cell_adapter = column_recycler.adapter

        // Get the beat-cell view
        var beat_cell_holder = column_recycler.findViewHolderForAdapterPosition(y) ?: return null
        val output = mutableListOf<LeafButton>()
        for (leaf_wrapper in (beat_cell_holder.itemView as ViewGroup).children) {
            val leaf_button = (leaf_wrapper as ViewGroup).getChildAt(0) as LeafButton
            val test_position = leaf_button.position_node.to_list()

            if (target_position.size <= test_position.size && test_position.subList(0, target_position.size) == target_position) {
                output.add(leaf_button)
            }
        }
        return output
    }

    fun cancel_linking() {
        val opus_manager = this.get_opus_manager()
        if (opus_manager.cursor.mode != Cursor.CursorMode.Single) {
            opus_manager.cursor_clear()
        } else {
            opus_manager.cursor_select(
                opus_manager.cursor.get_beatkey(),
                opus_manager.cursor.get_position()
            )
        }

        this.linking_beat = null
        this.linking_beat_b = null
    }

    fun link_beats(beat_key: BeatKey) {
        val opus_manager = this.get_opus_manager()
        opus_manager.link_beats(beat_key, this.linking_beat!!)
        opus_manager.cursor_select(
            beat_key,
            opus_manager.get_first_position(beat_key)
        )
    }

    fun link_beat_range(beat_key: BeatKey) {
        val opus_manager = this.get_opus_manager()
        opus_manager.link_beat_range(
            beat_key,
            this.linking_beat!!,
            this.linking_beat_b!!
        )
    }

    fun refresh_leaf_labels(beat: Int) {
        this.refresh_leaf_labels(setOf(beat))
    }

    fun refresh_leaf_labels(beats: Set<Int>? = null) {
        // NOTE: padding the start/end since an item may be bound but not visible
        val start = (this.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val end = (this.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        for (i in start .. end) {
            if (beats == null || i in beats) {
                this.notifyItemChanged(i)
            }
        }
    }

    private fun get_visible_highlighted_leafs(): List<LeafButton> {
        val opus_manager = this.get_opus_manager()
        val output = mutableListOf<LeafButton>()
        when (opus_manager.cursor.mode) {
            Cursor.CursorMode.Row -> {
                // NOTE: padding the start/end since an item may be bound but not visible
                val start = (this.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                val end = (this.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                for (i in start .. end) {
                    val leafs = this.get_all_leaf_views(
                        BeatKey(
                            opus_manager.cursor.channel,
                            opus_manager.cursor.line_offset,
                            i
                        )
                    ) ?: continue

                    for (leaf in leafs) {
                        output.add(leaf)
                    }
                }
            }
            Cursor.CursorMode.Column -> {
                for (i in 0 until opus_manager.channels.size) {
                    for (j in 0 until opus_manager.channels[i].size) {
                        val leafs = this.get_all_leaf_views(BeatKey(i, j, opus_manager.cursor.beat)) ?: continue
                        for (leaf in leafs) {
                            output.add(leaf)
                        }
                    }
                }
            }
            Cursor.CursorMode.Single -> {
                val start = (this.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                val end = (this.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                val beatkey = opus_manager.cursor.get_beatkey()
                for (linkedkey in opus_manager.get_all_linked(beatkey)) {
                    if (!(start .. end).contains(linkedkey.beat)) {
                        continue
                    }
                    for (leaf in this.get_all_leaf_views(linkedkey, opus_manager.cursor.get_position()) ?: continue) {
                        output.add(leaf)
                    }
                }
            }

            Cursor.CursorMode.Range -> {
                val start = (this.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                val end = (this.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                val from_key = opus_manager.cursor.range!!.first
                val to_key = opus_manager.cursor.range!!.second
                for (beatkey in opus_manager.get_beatkeys_in_range(from_key, to_key)) {
                    if (!(start .. end).contains(beatkey.beat)) {
                        continue
                    }
                    for (leaf in this.get_all_leaf_views(beatkey, listOf()) ?: continue) {
                        output.add(leaf)
                    }
                }
            }
            else -> {}
        }
        return output
    }

    fun set_cursor_focus(show: Boolean = true) {
        for (leaf in this.get_visible_highlighted_leafs()) {
            leaf.set_focused(show)
            leaf.invalidate()
        }
    }

    private fun enableScrollSync() {
        this.recycler.addOnScrollListener( this.table_scroll_listener )
        this.column_label_layout.recycler.addOnScrollListener( this.column_scroll_listener )
    }

    private fun disableScrollSync() {
        this.recycler.removeOnScrollListener( this.table_scroll_listener )
        this.column_label_layout.recycler.removeOnScrollListener( this.column_scroll_listener )
    }

    fun scrollToPosition(position: Int) {
        this.disableScrollSync()
        val resources = this.get_main_activity().resources
        val item_width = (this.column_label_layout.column_widths[position].toFloat() * resources.getDimension(R.dimen.base_leaf_width)).toInt()
        val center = (this.recycler.width - item_width) / 2
        (this.recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, center)
        (this.column_label_layout.recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, center)

        this.enableScrollSync()
    }

    fun scrollToPosition(beat_key: BeatKey, position: List<Int>) {
        this.disableScrollSync()
        val resources = this.get_main_activity().resources
        val item_width = (this.column_label_layout.column_widths[beat_key.beat].toFloat() * resources.getDimension(R.dimen.base_leaf_width)).toInt()
        val tree = this.get_opus_manager().get_tree(beat_key, position)
        val (tree_position, tree_size) = tree.get_flat_ratios()
        val first_visible = (this.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val buffer = this.recycler.width / 4
        val offset = if (first_visible > beat_key.beat) {
            0 - (item_width.toFloat() * tree_position).toInt() + buffer
        } else {
            (this.recycler.width - item_width + (item_width.toFloat() * (1F - tree_position - tree_size)).toInt()) - buffer
        }
        (this.recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(beat_key.beat, offset)
        (this.column_label_layout.recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(beat_key.beat, offset)

        this.enableScrollSync()
    }

    fun is_linking(): Boolean {
        return this.linking_beat != null
    }

    fun is_linking_range(): Boolean {
        return this.linking_beat_b != null
    }
}

