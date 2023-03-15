package com.qfs.radixulous

import android.content.Context
import android.view.*
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.opusmanager.BeatKey
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager

class OpusManagerAdapter(var parent_fragment: MainFragment, var recycler: RecyclerView, var column_layout: ColumnLabelAdapter) : RecyclerView.Adapter<OpusManagerAdapter.BeatViewHolder>() {
    var _longclicking_leaf: View? = null

    var _dragging_leaf: View? = null
    var linking_beat: BeatKey? = null
    var linking_beat_b: BeatKey? = null
    private var focus_type: FocusType = FocusType.Cell
    private var focused_leafs = mutableSetOf<LeafButton>()
    // BackLink so I can get the x offset from a view in the view holder
    class BackLinkView(context: Context): LinearLayout(context) {
        var viewHolder: BeatViewHolder? = null
        init {
            this.orientation = LinearLayout.VERTICAL
        }
    }

    class BeatViewHolder(itemView: BackLinkView) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.viewHolder = this
        }
    }
    init {
        this.recycler.adapter = this
        this.recycler.layoutManager = LinearLayoutManager(
            this.getMainActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        val that = this

        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    for (i in start until that.itemCount) { }
                }
                override fun onItemRangeChanged(start: Int, count: Int) {
                    for (i in start until that.itemCount) {
                        var viewHolder = that.recycler.findViewHolderForAdapterPosition(i) ?: continue
                        that.updateItem(viewHolder as BeatViewHolder, i)
                        that.column_layout.update_label_width(i)
                    }
                }
                override fun onItemRangeInserted(start: Int, count: Int) {
                    for (i in start until that.recycler.childCount) {
                        //that.notifyItemInserted(i)
                    }
                }
                //override fun onChanged() { }
            }
        )

        //this.recycler.viewTreeObserver.addOnScrollChangedListener {
        //    var x = this.recycler.computeHorizontalScrollOffset()
        //    this.column_layout.scrollTo(x)
        //    //(rvColumnLabels.adapter as ColumnLabelAdapter).scrollToP(p)
        //}

        this.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, x: Int, y: Int) {
                super.onScrolled(recyclerView, x, y)
                that.column_layout.scroll(x)
            }

        })

    }

    private fun getMainActivity(): MainActivity {
        return this.parent_fragment.getMain()
    }

    private fun buildTreeView(parent: ViewGroup, beatkey: BeatKey): View {
        return this.buildTreeView(parent, beatkey, listOf())
    }

    private fun buildTreeView(parent: ViewGroup, beatkey: BeatKey, position: List<Int>, offset: Int? = null): View {
        val opus_manager = this.get_opus_manager()
        val tree = opus_manager.get_tree(beatkey, position)

        if (tree.is_leaf()) {
            val tvLeaf = LeafButton(parent.context, this.getMainActivity(), tree.get_event(), opus_manager.is_percussion(beatkey.channel))

            tvLeaf.setOnClickListener {
                this.interact_leafView_click(it)
            }

            tvLeaf.setOnFocusChangeListener { view, is_focused: Boolean ->
                if (is_focused) {
                    this.interact_leafView_click(view)
                }
            }

            tvLeaf.setOnLongClickListener { view: View ->
                this._longclicking_leaf = view
                false
            }

            tvLeaf.setOnTouchListener { view, touchEvent ->
                if (this._longclicking_leaf != null) {
                    if (touchEvent.action == MotionEvent.ACTION_MOVE) {
                        if (this._dragging_leaf == null) {
                            this._dragging_leaf = view

                            view.startDragAndDrop(
                                null,
                                View.DragShadowBuilder(view),
                                null,
                                0
                            )
                        }
                        this._longclicking_leaf = null
                    } else if (touchEvent.action == MotionEvent.ACTION_UP) {
                        this.interact_leafView_longclick(view)
                        this._longclicking_leaf = null
                        return@setOnTouchListener true
                    }
                }
                false
            }

            tvLeaf.setOnDragListener { view: View, dragEvent: DragEvent ->
                when (dragEvent.action) {
                    //DragEvent.ACTION_DRAG_STARTED -> { }
                    DragEvent.ACTION_DROP -> {
                        if (this._dragging_leaf != view && this._dragging_leaf != null) {
                            val (beatkey_to, position_to) = this.get_view_position(view)
                            val (beatkey_from, position_from) = this.get_view_position(this._dragging_leaf!!)

                            opus_manager.move_leaf(beatkey_from, position_from, beatkey_to, position_to)
                            opus_manager.set_cursor_position(beatkey_to, position_to)

                            this.parent_fragment.tick()
                        }
                        this._dragging_leaf = null
                    }
                    else -> { }
                }
                true
            }

            if (offset == null) {
                parent.addView(tvLeaf)
            } else {
                parent.addView(tvLeaf, offset)
            }

            val param = tvLeaf.layoutParams as LinearLayout.LayoutParams
            param.gravity = Gravity.CENTER
            param.height = ViewGroup.LayoutParams.MATCH_PARENT
            param.width = 128
            param.weight = 1F
            (param as ViewGroup.MarginLayoutParams).setMargins(0,0,10,0)
            return tvLeaf
        } else {
            val cellLayout: LinearLayout = LayoutInflater.from(parent.context).inflate(
                R.layout.tree_node,
                parent,
                false
            ) as LinearLayout
            val param = cellLayout.layoutParams as LinearLayout.LayoutParams
            param.gravity = Gravity.CENTER
            param.height = ViewGroup.LayoutParams.MATCH_PARENT
            param.width = 0
            param.weight = 1F
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

    override fun onBindViewHolder(holder: BeatViewHolder, position: Int) {
        this.updateItem(holder, position)
    }

    fun updateItem(holder: BeatViewHolder, index: Int) {
        (holder.itemView as ViewGroup).removeAllViews()
        val opus_manager = this.get_opus_manager()

        for (channel in 0 until opus_manager.channels.size) {
            for (line_offset in 0 until opus_manager.channels[channel].size) {
                var beat_wrapper = LayoutInflater.from(holder.itemView.context).inflate(
                    R.layout.beat_node,
                    holder.itemView as ViewGroup,
                    false
                )

                (holder.itemView as ViewGroup).addView(beat_wrapper)

                this.buildTreeView(beat_wrapper as ViewGroup, BeatKey(channel, line_offset, index))
            }
        }
        this.adjust_beat_width(holder, index)
    }

    // TODO: Needs checks
    private fun get_view_position(view: View): Pair<BeatKey, List<Int>> {
        val position = mutableListOf<Int>()
        var working_view = view
        while ((working_view.parent as View).id != R.id.beat_node) {
            position.add(0, (working_view.parent as ViewGroup).indexOfChild(working_view))
            working_view = working_view.parent as View
        }

        working_view = working_view.parent as View

        val y = (working_view.parent as ViewGroup).indexOfChild(working_view)
        val (channel, line_offset) = this.get_opus_manager().get_channel_index(y)

        var viewholder = (working_view.parent as BackLinkView).viewHolder!!
        var beat = viewholder.getBindingAdapterPosition()
        return Pair(BeatKey(channel, line_offset, beat), position)
    }

    override fun getItemCount(): Int {
        return this.get_opus_manager().opus_beat_count
    }

    private fun get_opus_manager(): OpusManager {
        return this.getMainActivity().getOpusManager()
    }

    private fun interact_leafView_click(view: View) {
        val main = this.getMainActivity()
        main.stop_playback()

        this.set_focus_type(FocusType.Cell)

        val opus_manager = main.getOpusManager()

        val (beatkey, position) = this.get_view_position(view)

        opus_manager.set_cursor_position(beatkey, position)
        val cursor_beatkey = opus_manager.get_cursor().get_beatkey()

        if (this.linking_beat != null) {
            // If a second link point hasn't been selected, assume just one beat is being linked
            if (this.linking_beat_b == null) {
                opus_manager.link_beats(cursor_beatkey, this.linking_beat!!)
            } else {
                opus_manager.link_beat_range(cursor_beatkey, this.linking_beat!!, this.linking_beat_b!!)
            }

            this.linking_beat = null
            this.linking_beat_b = null
        }

        val tree = opus_manager.get_tree_at_cursor()
        if (tree.is_event()) {
            main.play_event(
                beatkey.channel,
                if (opus_manager.is_percussion(beatkey.channel)) {
                    opus_manager.get_percussion_instrument(beatkey.line_offset)
                } else {
                    opus_manager.get_absolute_value(beatkey, position)!!
                }
            )
        }

        this.parent_fragment.tick()
        this.parent_fragment.setContextMenu(ContextMenu.Leaf)
    }

    private fun interact_leafView_longclick(view: View) {
        val main = this.getMainActivity()
        main.stop_playback()

        val opus_manager = main.getOpusManager()
        val (beatkey, _position) = this.get_view_position(view)
        if (this.linking_beat == null) {
            this.linking_beat = beatkey
        } else {
            this.linking_beat_b = beatkey
        }

        opus_manager.set_cursor_position(beatkey, listOf())

        this.parent_fragment.tick()
        this.parent_fragment.setContextMenu(ContextMenu.Linking)
    }

    fun set_focus_type(focus_type: FocusType) {
        this.focus_type = focus_type
    }

    fun tick_unapply_cursor_focus() {
        this.focused_leafs.removeAll { leafbutton: LeafButton ->
            leafbutton.isFocused = false
            true
        }
    }

    fun tick_apply_cursor_focus() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.get_cursor()
        val focused: MutableSet<Pair<BeatKey, List<Int>>> = mutableSetOf()
        when (this.focus_type) {
            FocusType.Row -> {
                for (x in 0 until opus_manager.opus_beat_count) {
                    val beatkey = cursor.get_beatkey()
                    focused.add(
                        Pair(
                            BeatKey(beatkey.channel, beatkey.line_offset, x),
                            listOf()
                        )
                    )
                }

            }
            FocusType.Column -> {
                for (y in 0 until opus_manager.line_count()) {
                    val (channel, index) = opus_manager.get_channel_index(y)
                    focused.add(
                        Pair(
                            BeatKey(channel, index, cursor.get_beatkey().beat),
                            listOf()
                        )
                    )
                }
            }
            FocusType.Cell -> {
                focused.add(Pair(cursor.get_beatkey(), cursor.get_position()))
            }
        }
        this.apply_focus(focused, opus_manager)
    }

    fun get_leaf_view(beatkey: BeatKey, position: List<Int>): LeafButton? {
        val opus_manager = this.get_opus_manager()

        // Get the full-beat view
        var column_view_holder = this.recycler.findViewHolderForAdapterPosition(beatkey.beat) ?: return null
        var working_view = column_view_holder.itemView

        // Get the beat-cell view
        val y = opus_manager.get_y(beatkey.channel, beatkey.line_offset)
        working_view = (working_view as ViewGroup).getChildAt(y)

        // dive past the beat_node wrapper
        working_view = (working_view as ViewGroup).getChildAt(0)

        for (x in position) {
            working_view = (working_view as ViewGroup).getChildAt(x)
        }

        return working_view as LeafButton
    }

    fun get_all_leaf_views(beatkey: BeatKey): List<LeafButton> {
        val opus_manager = this.get_opus_manager()

        // Get the full-beat view
        var column_view_holder = this.recycler.findViewHolderForAdapterPosition(beatkey.beat)!!
        var working_view = column_view_holder.itemView

        // Get the beat-cell view
        val y = opus_manager.get_y(beatkey.channel, beatkey.line_offset)
        working_view = (working_view as ViewGroup).getChildAt(y)

        // dive past the beat_node wrapper
        working_view = (working_view as ViewGroup).getChildAt(0)

        val output = mutableListOf<LeafButton>()
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

    fun focus_leaf(leaf: LeafButton) {
        leaf.isFocused = true
        this.focused_leafs.add(leaf)
    }

    fun apply_focus(focused: Set<Pair<BeatKey, List<Int>>>, opus_manager: OpusManager) {
        for ((beatkey, position) in focused) {
            val linked_beats = opus_manager.get_all_linked(beatkey)
            for (linked_beat in linked_beats) {
                try {
                    var view = this.get_leaf_view(linked_beat, position) ?: continue
                    this.focus_leaf(view)
                } catch (e: Exception) {

                }
            }
        }

        if (this.linking_beat_b != null) {
            val cursor_diff = opus_manager.get_cursor_difference(this.linking_beat!!, this.linking_beat_b!!)

            for (y in 0 .. cursor_diff.first) {
                for (x in 0 .. cursor_diff.second) {
                    val working_beat = BeatKey(
                        this.linking_beat!!.channel,
                        this.linking_beat!!.line_offset,
                        x + this.linking_beat!!.beat
                    )

                    for (leafbutton in this.get_all_leaf_views(working_beat)) {
                        this.focus_leaf(leafbutton)
                    }
                }
            }
        }
    }

    fun rebuildBeatView(beatkey: BeatKey) {
        val main = this.getMainActivity()
        val opus_manager = main.getOpusManager()
        var column_view_holder = this.recycler.findViewHolderForAdapterPosition(beatkey.beat) ?: return
        var column_view = column_view_holder.itemView
        var y = opus_manager.get_y(beatkey.channel, beatkey.line_offset)
        (column_view as ViewGroup).removeViewAt(y)
        this.buildTreeView(column_view, beatkey, listOf(), y)
    }

    fun validate_leaf(beatkey: BeatKey, position: List<Int>, valid: Boolean) {
        this.get_leaf_view(beatkey, position)?.setInvalid(!valid)
    }

    fun update_leaf_labels() {
        var opus_manager = this.get_opus_manager()
        for (channel in 0 until opus_manager.channels.size) {
            for (y in 0 until opus_manager.channels[channel].size) {
                for (x in 0 until opus_manager.opus_beat_count) {
                    for (leafbutton in this.get_all_leaf_views(BeatKey(channel, y, x))) {
                        leafbutton.set_text(opus_manager.is_percussion(channel))
                    }
                }
            }
        }
    }

    fun adjust_beat_width(holder: BeatViewHolder, beat: Int) {
        val opus_manager = this.get_opus_manager()
        // resize Columns
        var max_width = 0
        for (channel in 0 until opus_manager.channels.size) {
            for (line_offset in 0 until opus_manager.channels[channel].size) {
                val tree = opus_manager.get_beat_tree(BeatKey(channel, line_offset, beat))
                val size = Integer.max(1, tree.size) * tree.get_max_child_weight()
                max_width = Integer.max(max_width, size)
            }
        }

        for (channel in 0 until opus_manager.channels.size) {
            for (line_offset in 0 until opus_manager.channels[channel].size) {
                this.__tick_resize_beat_cell(holder, channel, line_offset, beat, max_width)
            }
        }

        this.column_layout.set_label_width(beat, max_width)
    }

    fun __tick_resize_beat_cell(holder: BeatViewHolder, channel: Int, line_offset: Int, beat: Int, new_width: Int) {
        val opus_manager = this.get_opus_manager()

        var working_view = holder.itemView
        var y = opus_manager.get_y(channel, line_offset)
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

            val param = current_view!!.layoutParams as ViewGroup.MarginLayoutParams

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

                param.width = (new_size * 120.toFloat()).toInt()
            } else {
                param.marginStart = 5
                param.marginEnd = 5
                param.width = (new_size * 120.toFloat()).toInt() - param.marginStart - param.marginEnd
            }

            current_view.layoutParams = param
        }
    }
}

enum class FocusType {
    Cell,
    Row,
    Column
}

