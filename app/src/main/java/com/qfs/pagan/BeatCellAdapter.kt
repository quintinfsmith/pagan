package com.qfs.pagan

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.LinksLayer
import kotlin.concurrent.thread
import kotlin.math.max
import com.qfs.pagan.InterfaceLayer as OpusManager

class BeatCellAdapter(var recycler: BeatColumnAdapter.BeatCellRecycler): RecyclerView.Adapter<BeatCellAdapter.BCViewHolder>() {
    class BackLinkView(context: Context): LinearLayout(context) {
        var viewHolder: BCViewHolder? = null
        /*
         * update_queued exists to handle the liminal state between being detached and being destroyed
         * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
         */
        var update_queued = false
        override fun onDetachedFromWindow() {
            this.update_queued = true
            super.onDetachedFromWindow()
        }
    }

    class BCViewHolder(itemView: BackLinkView) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.viewHolder = this
        }
    }

    val cell_width_map = mutableListOf<Int>()
    var resize_lock = false
    var channel_size_map = mutableListOf<Int>()

    init {
        val that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                }
                override fun onItemRangeChanged(start: Int, count: Int) {
                    that.get_beat_column_adapter().column_label_layout.notifyItemChanged(that.get_beat())
                }
                override fun onItemRangeInserted(start: Int, count: Int) {
                }
            }
        )
        this.recycler.itemAnimator = null
    }


    fun get_beat_column_adapter(): BeatColumnAdapter {
        return this.recycler.viewHolder!!.parent_adapter
    }

    fun get_opus_manager(): OpusManager {
        return this.get_beat_column_adapter().get_opus_manager()
    }

    fun get_main_activity(): MainActivity {
        return this.get_beat_column_adapter().get_main_activity()
    }

    fun get_beat(): Int {
        return this.recycler.get_position()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BCViewHolder {
        return BCViewHolder(BackLinkView(this.recycler.context))
    }

    override fun getItemCount(): Int {
        return this.get_opus_manager().get_channel_line_counts().sum()
    }

    override fun onViewAttachedToWindow(holder: BCViewHolder) {
        val item_view = holder.itemView as BackLinkView
        val beat_index = holder.bindingAdapterPosition
        item_view.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT

        this.populate_item(holder, beat_index)
        // Redraw Items that were detached but not destroyed
        if (item_view.update_queued) {
            item_view.update_queued = false
        }
    }

    override fun onBindViewHolder(holder: BCViewHolder, position: Int) {
        if (holder.itemView != null) {
            this.populate_item(holder, position)
        }
    }

    private fun populate_item(holder: BCViewHolder, position: Int) {
        val item_view = holder.itemView
        (item_view as ViewGroup).removeAllViews()

        val opus_manager = this.get_opus_manager()
        val (channel, line_offset) = opus_manager.get_std_offset(position)
        val beat_key = BeatKey(channel, line_offset, this.get_beat())
        val beat_wrapper: LinearLayout = LayoutInflater.from(item_view.context).inflate(
            R.layout.beat_node,
            holder.itemView as ViewGroup,
            false
        ) as LinearLayout


        val beat_tree = opus_manager.get_beat_tree(beat_key)
        val weight = beat_tree.size * beat_tree.get_max_child_weight()
        val resources = this.recycler.resources
        beat_wrapper.minimumWidth = ((weight * resources.getDimension(R.dimen.base_leaf_width)) + ((weight - 1) * 2 * resources.getDimension(R.dimen.line_padding))).toInt()
        (item_view as ViewGroup).addView(beat_wrapper)

        this.buildTreeTopView(
            beat_wrapper,
            beat_key
        )

        this.get_beat_column_adapter().column_label_layout.notifyItemChanged(this.get_beat())
    }

    private fun buildTreeTopView(parent: LinearLayout, beat_key: BeatKey) {
        val tree = this.get_opus_manager().get_beat_tree(beat_key)
        val top_node = PositionNode()
        val max_weight = tree.get_max_child_weight()
        if (!tree.is_leaf()) {
            for (i in 0 until tree.size) {
                val next_node = PositionNode(i)
                next_node.previous = top_node
                this.buildTreeView(parent, beat_key, next_node, max_weight)
            }
        } else {
            this.buildTreeView(parent, beat_key, top_node, max_weight)
        }
    }

    // Returns width in nodes
    private fun buildTreeView(parent: ViewGroup, beat_key: BeatKey, position_node: PositionNode, weight: Int) {
        val opus_manager = this.get_opus_manager()
        val position = position_node.to_list()
        val tree = opus_manager.get_tree(beat_key, position)

        if (tree.is_leaf()) {
            val tvLeaf = LeafButton(
                parent.context,
                this.get_main_activity(),
                tree.get_event(),
                position_node,
                opus_manager.is_percussion(beat_key.channel)
            )

            tvLeaf.setOnClickListener {
                this.interact_leafView_click(tvLeaf)
            }

            tvLeaf.setOnFocusChangeListener { _, is_focused: Boolean ->
                if (is_focused) {
                    this.interact_leafView_click(tvLeaf)
                }
            }

            tvLeaf.setOnLongClickListener {
                this.interact_leafView_doubletap(tvLeaf)
                true
            }

            parent.addView(tvLeaf)

            val resources = this.recycler.resources
            tvLeaf.minimumWidth = resources.getDimension(R.dimen.base_leaf_width).toInt()
            (tvLeaf.layoutParams as LinearLayout.LayoutParams).apply {
                gravity = Gravity.CENTER
                height = resources.getDimension(R.dimen.line_height).toInt()
                width = 0
                this.weight = weight.toFloat()
            }

            (tvLeaf.layoutParams as ViewGroup.MarginLayoutParams).apply {
                val margin = resources.getDimension(R.dimen.line_padding)
                //marginEnd = margin.toInt()
                //marginStart = margin.toInt()
            }

            if (tree.is_event()) {
                val abs_value = opus_manager.get_absolute_value(beat_key, position)
                tvLeaf.set_invalid(abs_value == null || abs_value < 0)
            }
            if (opus_manager.is_networked(beat_key)) {
                tvLeaf.set_linked(true)
            }
            this.apply_cursor_focus(tvLeaf, position)
        } else {
            val new_weight = weight / tree.size
            for (i in 0 until tree.size) {
                val new_node = PositionNode(i)
                new_node.previous = position_node
                this.buildTreeView(parent, beat_key, new_node, new_weight)
            }
        }
    }

    // TODO: Needs checks
    private fun get_view_position_abs(leaf_button: LeafButton): Triple<Int, Int, List<Int>> {
        val viewholder = (leaf_button.parent.parent as BackLinkView).viewHolder!!
        val y = viewholder.bindingAdapterPosition

        return Triple(y, this.get_beat(), leaf_button.position_node.to_list())
    }

    // TODO: Needs checks
    private fun get_view_position(leaf_button: LeafButton): Pair<BeatKey, List<Int>> {
        val (y, x, position) = this.get_view_position_abs(leaf_button)
        val (channel, line_offset) = this.get_opus_manager().get_std_offset(y)
        return Pair(BeatKey(channel, line_offset, x), position)
    }

    private fun get_beat_key(leaf_button: LeafButton): BeatKey {
        val viewholder = (leaf_button.parent.parent as BackLinkView).viewHolder!!
        val y = viewholder.bindingAdapterPosition
        val (channel, line_offset) = this.get_opus_manager().get_std_offset(y)

        return BeatKey(
            channel,
            line_offset,
            this.get_beat()
        )

    }

    private fun interact_leafView_click(leaf_button: LeafButton) {
        val (beatkey, position) = this.get_view_position(leaf_button)
        val opus_manager = this.get_opus_manager()
        val beat_column_adapter = this.get_beat_column_adapter()
        if (beat_column_adapter.is_linking()) {
            // If a second link point hasn't been selected, assume just one beat is being linked
            if (opus_manager.cursor.mode != Cursor.CursorMode.Range) {
                try {
                    beat_column_adapter.link_beats(beatkey)
                    opus_manager.cursor_select(beatkey, position)
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
                    beat_column_adapter.link_beat_range(beatkey)
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
            beat_column_adapter.cancel_linking()
        } else {
            opus_manager.cursor_select(beatkey, position)
            val tree = opus_manager.get_tree()
            thread {
                if (tree.is_event()) {
                    val abs_value = opus_manager.get_absolute_value(beatkey, position)
                    if ((abs_value != null) && abs_value in (0 .. 90)) {
                        this.get_main_activity().play_event(
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

    private fun interact_leafView_doubletap(leaf_button: LeafButton) {
        val (beat_key, position) = this.get_view_position(leaf_button)
        val beat_column_adapter = this.get_beat_column_adapter()
        val opus_manager = this.get_opus_manager()
        if (!beat_column_adapter.is_linking()) {
            opus_manager.cursor_select(beat_key, position)
            beat_column_adapter.linking_beat = beat_key
        } else {
            opus_manager.cursor_select_range(beat_column_adapter.linking_beat!!, beat_key)
            beat_column_adapter.linking_beat_b = beat_key
        }

        beat_column_adapter.parent_fragment.setContextMenu_linking()
    }

    // Called only from the buildTreeView()
    private fun apply_cursor_focus(leaf_button: LeafButton, position: List<Int>) {
        val opus_manager = this.get_opus_manager()
        val beat_key = this.get_beat_key(leaf_button)
        when (opus_manager.cursor.mode) {
            Cursor.CursorMode.Row -> {
                if (beat_key.channel == opus_manager.cursor.channel && beat_key.line_offset == opus_manager.cursor.line_offset) {
                    leaf_button.set_focused(true)
                }
            }
            Cursor.CursorMode.Column -> {
                if (beat_key.beat == opus_manager.cursor.beat) {
                    leaf_button.set_focused(true)
                }
            }
            Cursor.CursorMode.Single -> {
                val linked_beats = opus_manager.get_all_linked(beat_key)
                if (linked_beats.contains(opus_manager.cursor.get_beatkey()) && position == opus_manager.cursor.get_position()) {
                    leaf_button.set_focused(true)
                }
            }
            Cursor.CursorMode.Range -> {
                val (from_key, to_key) = opus_manager.cursor.range!!
                val vert_ok = if (beat_key.channel > from_key.channel && beat_key.channel < to_key.channel) {
                    true
                } else if (from_key.channel == to_key.channel && beat_key.channel == from_key.channel) {
                    beat_key.line_offset >= from_key.line_offset && beat_key.line_offset <= to_key.line_offset
                } else if (beat_key.channel == from_key.channel) {
                    beat_key.line_offset >= from_key.line_offset
                } else if (beat_key.channel == to_key.channel) {
                    beat_key.line_offset <= to_key.line_offset
                } else {
                    false
                }

                leaf_button.set_focused(vert_ok && beat_key.beat in (from_key.beat .. to_key.beat))
            }
            else -> { }
        }
    }

    fun apply_to_visible_cells(callback: (BCViewHolder) -> Unit) {
        // TODO: May not apply to visible ONLY
        for (i in 0 until this.itemCount) {
            val viewHolder = this.recycler.findViewHolderForAdapterPosition(i) ?: continue
            callback(viewHolder as BCViewHolder)
        }
    }

    fun resize_all() {
        if (this.resize_lock || this.cell_width_map.isEmpty()) {
            return
        }

        var new_width = 1
        val opus_manager = this.get_opus_manager()
        val x = this.get_beat()
        this.channel_size_map.forEachIndexed {i: Int, line_count: Int ->
            for (j in 0 until line_count) {
                val tree = opus_manager.get_beat_tree(BeatKey(i, j, x))
                new_width = max(tree.get_max_child_weight(), new_width)
            }
        }

        this.apply_to_visible_cells {
            this.resize(it, new_width)
        }

    }

    private fun resize(holder: BCViewHolder, new_width: Int) {
        val beat = this.get_beat()
        val opus_manager = this.get_opus_manager()

        val (channel, line_offset) = this.get_std_offset(holder.bindingAdapterPosition)
        val beat_wrapper = (holder.itemView as ViewGroup).getChildAt(0)

        val beat_key = BeatKey(channel, line_offset, beat)
        val resources = this.get_main_activity().resources
        var param = beat_wrapper.layoutParams as ViewGroup.MarginLayoutParams
        param.width = (new_width.toFloat() * resources.getDimension(R.dimen.base_leaf_width)).toInt()

        val beat_tree = opus_manager.get_beat_tree(beat_key)
        for (i in 0 until (beat_wrapper as ViewGroup).childCount) {
            val leaf_button = beat_wrapper.getChildAt(i) as LeafButton
            val position = leaf_button.position_node.to_list()
            var working_tree = beat_tree
            var leaf_width = new_width

            if (position.isNotEmpty()) {
                for (x in position.subList(0, position.size - 1)) {
                    leaf_width /= working_tree.size
                    working_tree = working_tree[x]
                }
            }

            param = leaf_button.layoutParams as ViewGroup.MarginLayoutParams
            val margin = resources.getDimension(R.dimen.line_padding)

            param.marginEnd = margin.toInt()
            param.marginStart = 0
            param.width = (leaf_width * resources.getDimension(R.dimen.base_leaf_width)).toInt() - param.marginStart - param.marginEnd
        }
    }

    fun remove_line(channel: Int, line_offset: Int) {
        val y = this.get_abs_offset(channel, line_offset)
        this.channel_size_map[channel] -= 1
        if (this.channel_size_map[channel] == 0) {
            this.channel_size_map.remove(channel)
        }
        this.cell_width_map.remove(y)
        this.notifyItemRemoved(y)
    }

    fun insert_line(channel: Int, line_offset: Int) {
        while (this.channel_size_map.size <= channel) {
            this.channel_size_map.add(0)
        }

        while (this.channel_size_map[channel] <= line_offset) {
            this.channel_size_map[channel] += 1
        }

        val opus_manager = this.get_opus_manager()
        val tree = opus_manager.get_beat_tree(BeatKey(channel, line_offset, this.get_beat()))
        val width = tree.get_max_child_weight()

        val y = this.get_abs_offset(channel, line_offset)

        while (this.cell_width_map.size <= y) {
            this.cell_width_map.add(0)
        }
        this.cell_width_map.add(y, width)

        this.notifyItemInserted(y)
    }

    fun get_abs_offset(channel: Int, line_offset: Int): Int {
        var output = 0
        this.channel_size_map.forEachIndexed { c: Int, line_count: Int ->
            if (c < channel) {
                output += line_count
            } else if (c == channel) {
                output += line_offset
                return@forEachIndexed
            } else {
                output += 1
                return@forEachIndexed
            }
        }
        return output
    }

    fun get_std_offset(y: Int): Pair<Int, Int> {
        var counter = 0
        var channel = 0
        while (this.channel_size_map.size > channel && y > counter + this.channel_size_map[channel]) {
            counter += this.channel_size_map[channel]
            channel += 1
        }
        return Pair(channel, y - counter)
    }
}