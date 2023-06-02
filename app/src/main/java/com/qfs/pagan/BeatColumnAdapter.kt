package com.qfs.pagan

import android.content.Context
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.LinksLayer
import java.lang.Integer.max
import kotlin.concurrent.thread
import com.qfs.pagan.InterfaceLayer as OpusManager

class BeatColumnAdapter(private var parent_fragment: EditorFragment, var recycler: RecyclerView, var column_layout: ColumnLabelAdapter) : RecyclerView.Adapter<BeatColumnAdapter.BeatViewHolder>() {
    // BackLink so I can get the x offset from a view in the view holder
    class BackLinkView(context: Context): LinearLayout(context) {
        var viewHolder: BeatViewHolder? = null
        /*
         * update_queued exists to handle the liminal state between being detached and being destroyed
         * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
         */
        var update_queued = false
        // Used to keep track of columns in limbo
        init {
            this.orientation = VERTICAL
        }
        override fun onDetachedFromWindow() {
            this.update_queued = true
            super.onDetachedFromWindow()
        }
    }
    class OpusManagerLayoutManager(context: Context): LinearLayoutManager(context, HORIZONTAL, false)

    class TableOnScrollListener: RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, x: Int, y: Int) {
            super.onScrolled(recyclerView, x, y)
            val adapter = recyclerView.adapter as BeatColumnAdapter
            if (!adapter._scroll_lock_this) {
                adapter._scroll_lock_columns = true
                adapter.column_layout.scroll(x)
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

    class BeatViewHolder(itemView: BackLinkView) : RecyclerView.ViewHolder(itemView) {
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
        this.recycler.layoutManager = OpusManagerLayoutManager(this.get_main_activity())
        this.recycler.itemAnimator = null

        val that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    for (i in 0 until count) {
                        that.column_layout.removeColumnLabel(start + i)
                    }
                    val last_position = (that.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    val beats_to_refresh: Set<Int> = (start + count .. last_position).toSet()
                    that.refresh_leaf_labels(beats_to_refresh)
                }
                override fun onItemRangeChanged(start: Int, count: Int) {
                    for (i in start until that.itemCount) {
                        val viewHolder = that.recycler.findViewHolderForAdapterPosition(i) ?: continue
                        that.updateItem(viewHolder as BeatViewHolder, i)
                        that.column_layout.notifyItemChanged(i)
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
    override fun onViewDetachedFromWindow(holder: BeatViewHolder) {
        val item_view = holder.itemView as BackLinkView
        val beat_index = holder.bindingAdapterPosition
    }

    override fun onViewAttachedToWindow(holder: BeatViewHolder) {
        val item_view = holder.itemView as BackLinkView
        val beat_index = holder.bindingAdapterPosition

        // Redraw Items that were detached but not destroyed
        if (item_view.update_queued) {
            this.updateItem(holder, beat_index)
            item_view.update_queued = false
        }
    }

    override fun onBindViewHolder(holder: BeatViewHolder, position: Int) {
        this.updateItem(holder, position)
    }


    private fun get_main_activity(): MainActivity {
        return this.parent_fragment.get_main()
    }

    private fun buildTreeView(parent: ViewGroup, beatkey: BeatKey): View {
        return this.buildTreeView(parent, beatkey, listOf())
    }

    private fun buildTreeView(parent: ViewGroup, beatkey: BeatKey, position: List<Int>, offset: Int? = null): View {
        val opus_manager = this.get_opus_manager()
        val tree = opus_manager.get_tree(beatkey, position)

        if (tree.is_leaf()) {
            val tvLeaf = LeafButton(
                parent.context,
                this.get_main_activity(),
                tree.get_event(),
                opus_manager.is_percussion(beatkey.channel)
            )

            tvLeaf.setOnClickListener {
                this.interact_leafView_click(tvLeaf)
            }

            tvLeaf.setOnFocusChangeListener { view, is_focused: Boolean ->
                if (is_focused) {
                    this.interact_leafView_click(view)
                }
            }

            tvLeaf.setOnLongClickListener {
                this.interact_leafView_doubletap(tvLeaf)
                true
            }

            if (offset == null) {
                parent.addView(tvLeaf)
            } else {
                parent.addView(tvLeaf, offset)
            }

            (tvLeaf.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, 0, 10, 0)
            (tvLeaf.layoutParams as LinearLayout.LayoutParams).apply {
                gravity = Gravity.CENTER
                height = ViewGroup.LayoutParams.MATCH_PARENT
                width = 128
                weight = 1F
            }

            if (tree.is_event()) {
                val abs_value = opus_manager.get_absolute_value(beatkey, position)
                tvLeaf.set_invalid(abs_value == null || abs_value < 0)
            }
            if (opus_manager.is_networked(beatkey)) {
                tvLeaf.set_linked(true)
            }

            this.apply_cursor_focus(tvLeaf, beatkey, position)

            return tvLeaf
        } else {
            val cellLayout: LinearLayout = LayoutInflater.from(parent.context).inflate(
                R.layout.tree_node,
                parent,
                false
            ) as LinearLayout

            (cellLayout.layoutParams as LinearLayout.LayoutParams).apply {
                gravity = Gravity.CENTER
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                width = 0
                weight = 1F
            }

            for (i in 0 until tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.buildTreeView(cellLayout as ViewGroup, beatkey, new_position)
            }

            if (offset == null) {
                parent.addView(cellLayout)
            } else {
                parent.addView(cellLayout, offset)
            }

            return cellLayout
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeatViewHolder {
        val layoutstack = BackLinkView(parent.context)
        return BeatViewHolder(layoutstack)
    }

    fun updateItem(holder: BeatViewHolder, index: Int) {
        (holder.itemView as ViewGroup).removeAllViews()
        val opus_manager = this.get_opus_manager()

        for (channel in 0 until opus_manager.channels.size) {
            for (line_offset in 0 until opus_manager.channels[channel].size) {
                val beat_wrapper = LayoutInflater.from(holder.itemView.context).inflate(
                    R.layout.beat_node,
                    holder.itemView as ViewGroup,
                    false
                )

                this.buildTreeView(beat_wrapper as ViewGroup, BeatKey(channel, line_offset, index))

                (holder.itemView as ViewGroup).addView(beat_wrapper)
            }
        }
        this.adjust_beat_width(holder, index)
    }

    // TODO: Needs checks
    private fun get_view_position_abs(view: View): Triple<Int, Int, List<Int>> {
        val position = mutableListOf<Int>()
        var working_view = view
        while ((working_view.parent as View).id != R.id.beat_node) {
            position.add(0, (working_view.parent as ViewGroup).indexOfChild(working_view))
            working_view = working_view.parent as View
        }

        working_view = working_view.parent as View

        val y = (working_view.parent as ViewGroup).indexOfChild(working_view)

        val viewholder = (working_view.parent as BackLinkView).viewHolder!!
        val beat = viewholder.bindingAdapterPosition

        return Triple(y, beat, position)
    }

    // TODO: Needs checks
    private fun get_view_position(view: View): Pair<BeatKey, List<Int>> {
        val (y, x, position) = this.get_view_position_abs(view)
        val (channel, line_offset) = this.get_opus_manager().get_std_offset(y)
        return Pair(BeatKey(channel, line_offset, x), position)
    }

    override fun getItemCount(): Int {
        return this.beat_count
    }

    fun addBeatColumn(index: Int) {
        this.beat_count += 1
        this.column_layout.addColumnLabel(index)
        this.notifyItemInserted(index)
    }

    fun removeBeatColumn(index: Int) {
        if (this.beat_count > 0) {
            this.beat_count -= 1
        }
        this.notifyItemRemoved(index)
    }

    private fun get_opus_manager(): OpusManager {
        return this.get_main_activity().get_opus_manager()
    }

    private fun interact_leafView_click(view: View) {
        val main = this.get_main_activity()
        val (beatkey, position) = this.get_view_position(view)

        val opus_manager = this.get_opus_manager()
        if (this.linking_beat != null) {
            // If a second link point hasn't been selected, assume just one beat is being linked
            if (opus_manager.cursor.mode != Cursor.CursorMode.Range) {
                try {
                    opus_manager.link_beats(beatkey, this.linking_beat!!)
                    opus_manager.cursor_select(
                        beatkey,
                        opus_manager.get_first_position(beatkey)
                    )
                } catch (e: Exception) {
                    when (e) {
                        is LinksLayer.SelfLinkError -> { }
                        is LinksLayer.MixedLinkException -> {
                            this.get_main_activity().feedback_msg("Can't link percussion to non-percussion")
                        }
                        else -> {
                            throw e
                        }
                    }
                }
            } else {
                try {
                    opus_manager.link_beat_range(
                        beatkey,
                        this.linking_beat!!,
                        this.linking_beat_b!!
                    )
                    opus_manager.cursor_select(beatkey, position)
                } catch (e: Exception) {
                    when (e) {
                        is LinksLayer.SelfLinkError -> { }
                        is LinksLayer.LinkRangeOverlap -> { }
                        is LinksLayer.LinkRangeOverflow -> { }
                        is LinksLayer.MixedLinkException -> {
                            this.get_main_activity().feedback_msg(
                                "Can't link percussion to non-percussion"
                            )
                        }
                        else -> {
                            throw e
                        }
                    }

                }
            }
            this.cancel_linking()
        } else {
            opus_manager.cursor_select(beatkey, position)
            val tree = opus_manager.get_tree()
            thread {
                if (tree.is_event()) {
                    val abs_value = opus_manager.get_absolute_value(beatkey, position)
                    if ((abs_value != null) && abs_value in (0 .. 90)) {
                        main.play_event(
                            beatkey.channel,
                            if (opus_manager.is_percussion(beatkey.channel)) {
                                opus_manager.get_percussion_instrument(beatkey.line_offset)
                            } else {
                                opus_manager.get_absolute_value(beatkey, position) ?: return@thread
                            },
                            opus_manager.get_line_volume(beatkey.channel, beatkey.line_offset)
                        )
                    }
                }
            }
        }
    }

    private fun interact_leafView_doubletap(view: View) {
        val opus_manager = this.get_opus_manager()
        val (beatkey, position) = this.get_view_position(view)
        if (this.linking_beat == null) {
            opus_manager.cursor_select(beatkey, position)
            this.linking_beat = beatkey
        } else {
            opus_manager.cursor_select_range(this.linking_beat!!, beatkey)
            this.linking_beat_b = beatkey
        }

        this.parent_fragment.setContextMenu_linking()
    }

    // Called only from the buildTreeView()
    private fun apply_cursor_focus(leaf: LeafButton, beatkey: BeatKey, position: List<Int>) {
        val opus_manager = this.get_opus_manager()
        when (opus_manager.cursor.mode) {
            Cursor.CursorMode.Row -> {
                if (beatkey.channel == opus_manager.cursor.channel && beatkey.line_offset == opus_manager.cursor.line_offset) {
                    leaf.set_focused(true)
                }
            }
            Cursor.CursorMode.Column -> {
                if (beatkey.beat == opus_manager.cursor.beat) {
                    leaf.set_focused(true)
                }
            }
            Cursor.CursorMode.Single -> {
                val linked_beats = opus_manager.get_all_linked(beatkey)
                if (linked_beats.contains(opus_manager.cursor.get_beatkey()) && position == opus_manager.cursor.get_position()) {
                    leaf.set_focused(true)
                }
            }
            Cursor.CursorMode.Range -> {
                val (from_key, to_key) = opus_manager.cursor.range!!
                val vert_ok = if (beatkey.channel > from_key.channel && beatkey.channel < to_key.channel) {
                    true
                } else if (from_key.channel == to_key.channel && beatkey.channel == from_key.channel) {
                    beatkey.line_offset >= from_key.line_offset && beatkey.line_offset <= to_key.line_offset
                } else if (beatkey.channel == from_key.channel) {
                    beatkey.line_offset >= from_key.line_offset
                } else if (beatkey.channel == to_key.channel) {
                    beatkey.line_offset <= to_key.line_offset
                } else {
                    false
                }

                leaf.set_focused(vert_ok && beatkey.beat in (from_key.beat .. to_key.beat))
            }
            else -> { }
        }
    }

    fun get_leaf_view(beatkey: BeatKey, position: List<Int>): View? {
        val opus_manager = this.get_opus_manager()

        // Get the full-beat view
        val column_view_holder = this.recycler.findViewHolderForAdapterPosition(beatkey.beat) ?: return null
        var working_view = column_view_holder.itemView

        // Get the beat-cell view
        val y = opus_manager.get_abs_offset(beatkey.channel, beatkey.line_offset)
        working_view = (working_view as ViewGroup).getChildAt(y)

        // dive past the beat_node wrapper
        working_view = (working_view as ViewGroup).getChildAt(0)

        for (i in position) {
            working_view = (working_view as ViewGroup).getChildAt(i)
        }

        return working_view
    }

    private fun get_all_leaf_views(beatkey: BeatKey, position: List<Int>? = null): List<LeafButton>? {
        val opus_manager = this.get_opus_manager()
        return try {
            val y = opus_manager.get_abs_offset(beatkey.channel, beatkey.line_offset)
            this.get_all_leaf_views(y, beatkey.beat, position)
        } catch (e: IndexOutOfBoundsException) {
            listOf()
        }
    }

    private fun get_all_leaf_views(y: Int, x: Int, position: List<Int>? = null): List<LeafButton>? {
        // NOTE: A Bad position MAY be passed. eg, if the cursor doesn't reflect that a node was removed
        // Those should be treated as if they just don't exist here

        // Get the full-beat view
        val column_view_holder = this.recycler.findViewHolderForAdapterPosition(x) ?: return null
        var working_view = column_view_holder.itemView

        // Get the beat-cell view
        working_view = (working_view as ViewGroup).getChildAt(y)

        // dive past the beat_node wrapper
        working_view = (working_view as ViewGroup).getChildAt(0)

        val output = mutableListOf<LeafButton>()

        for (i in position ?: listOf()) {
            // Attempting to get leaf that doesn't exist
            if (working_view is LeafButton) {
                break
            }
            working_view = (working_view as ViewGroup).getChildAt(i)
        }

        val stack = mutableListOf(working_view)
        while (stack.isNotEmpty()) {
            working_view = stack.removeFirst()
            if (working_view is LeafButton) {
                output.add(working_view)
                continue
            }

            for (i in 0 until (working_view as ViewGroup).childCount) {
                stack.add(working_view.getChildAt(i))
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

    private fun adjust_beat_width(holder: BeatViewHolder, beat: Int) {
        val opus_manager = this.get_opus_manager()
        // resize Columns
        var max_width = 0
        for (channel in 0 until opus_manager.channels.size) {
            for (line_offset in 0 until opus_manager.channels[channel].size) {
                val tree = opus_manager.get_beat_tree(BeatKey(channel, line_offset, beat))
                val size = max(1, tree.size) * tree.get_max_child_weight()
                max_width = max(max_width, size)
            }
        }

        for (channel in 0 until opus_manager.channels.size) {
            for (line_offset in 0 until opus_manager.channels[channel].size) {
                this.__tick_resize_beat_cell(holder, channel, line_offset, beat, max_width)
            }
        }

        this.column_layout.set_label_width(beat, max_width)
    }

    // TODO: Rename. 'tick' no longer makes sense
    private fun __tick_resize_beat_cell(holder: BeatViewHolder, channel: Int, line_offset: Int, beat: Int, new_width: Int) {
        val opus_manager = this.get_opus_manager()

        var working_view = holder.itemView
        val y = opus_manager.get_abs_offset(channel, line_offset)
        working_view = (working_view as ViewGroup).getChildAt(y)
        working_view = (working_view as ViewGroup).getChildAt(0)

        val stack = mutableListOf<Triple<Float, List<Int>, View>>(
            Triple(
                new_width.toFloat(),
                listOf(),
                working_view
            )
        )

        val key = BeatKey(channel, line_offset, beat)
        while (stack.isNotEmpty()) {
            val (new_size, current_position, current_view) = stack.removeFirst()
            val current_tree = opus_manager.get_tree(key, current_position)

            val param = current_view.layoutParams as ViewGroup.MarginLayoutParams

            val resources = this.get_main_activity().resources
            if (!current_tree.is_leaf()) {
                for (i in 0 until current_tree.size) {
                    val next_pos = current_position.toMutableList()
                    next_pos.add(i)
                    stack.add(
                        Triple(
                            new_size / current_tree.size.toFloat(),
                            next_pos,
                            (current_view as ViewGroup).getChildAt(i)
                        )
                    )
                }

                param.width = (new_size * resources.getDimension(R.dimen.base_leaf_width)).toInt()
            } else {
                val margin = resources.getDimension(R.dimen.normal_padding)
                param.marginStart = margin.toInt()
                param.marginEnd = 0
                param.width = (new_size * resources.getDimension(R.dimen.base_leaf_width)).toInt() - param.marginStart - param.marginEnd
            }

            current_view.layoutParams = param
        }
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
                val start = (this.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                val end = (this.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
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
        this.column_layout.recycler.addOnScrollListener( this.column_scroll_listener )
    }

    private fun disableScrollSync() {
        this.recycler.removeOnScrollListener( this.table_scroll_listener )
        this.column_layout.recycler.removeOnScrollListener( this.column_scroll_listener )
    }

    fun scrollToPosition(position: Int) {
        this.disableScrollSync()
        val resources = this.get_main_activity().resources
        val item_width = (this.column_layout.column_widths[position].toFloat() * resources.getDimension(R.dimen.base_leaf_width)).toInt()
        val center = (this.recycler.width - item_width) / 2
        (this.recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, center)
        (this.column_layout.recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, center)

        this.enableScrollSync()
    }

    fun scrollToPosition(beatkey: BeatKey, position: List<Int>) {
        this.disableScrollSync()
        val resources = this.get_main_activity().resources
        val item_width = (this.column_layout.column_widths[beatkey.beat].toFloat() * resources.getDimension(R.dimen.base_leaf_width)).toInt()
        val tree = this.get_opus_manager().get_tree(beatkey, position)
        val (tree_position, tree_size) = tree.get_flat_ratios()
        val first_visible = (this.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val buffer = this.recycler.width / 4
        val offset = if (first_visible > beatkey.beat) {
            0 - (item_width.toFloat() * tree_position).toInt() + buffer
        } else {
            (this.recycler.width - item_width + (item_width.toFloat() * (1F - tree_position - tree_size)).toInt()) - buffer
        }
        (this.recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(beatkey.beat, offset)
        (this.column_layout.recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(beatkey.beat, offset)


        this.enableScrollSync()
    }
}

