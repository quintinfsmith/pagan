package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.InterfaceLayer as OpusManager

class ColumnRecyclerAdapter(editor_table: EditorTable): RecyclerView.Adapter<ColumnRecyclerViewHolder>() {
    val recycler: ColumnRecycler
    val column_label_recycler: ColumnLabelRecycler
    var column_count = 0
    init {
        this.column_label_recycler = editor_table.column_label_recycler
        this.recycler = editor_table.main_recycler
        this.recycler.adapter = this

        val that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(start: Int, count: Int) {
                    that.notifyItemChanged(start + count - 1)
                }
                override fun onItemRangeChanged(start: Int, count: Int) {
                    //that.column_label_recycler.adapter?.notifyItemRangeChanged(start, count)
                }
            }
        )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnRecyclerViewHolder {
        return ColumnRecyclerViewHolder(parent.context)
    }

    override fun getItemCount(): Int {
        return this.column_count
    }

    override fun onBindViewHolder(holder: ColumnRecyclerViewHolder, position: Int) {
        var editor_table = this.get_editor_table()!!
        var weight = editor_table.get_column_width(position)
        val resources = this.recycler.resources
        ColumnPlaceholder(holder, weight * resources.getDimension(R.dimen.base_leaf_width).toInt())
    }
    override fun onViewAttachedToWindow(holder: ColumnRecyclerViewHolder) {
        holder.itemView.layoutParams.height = MATCH_PARENT
    }
    override fun onViewDetachedFromWindow(holder: ColumnRecyclerViewHolder) {
        // Need to remove the view (CellRecycler, see ColumnPlaceholder.replace())
        // so that if something changes between being bound and attached, the
        // recycler doesn't try to reattach LeafButtons

        ColumnPlaceholder(holder, holder.itemView.measuredWidth)
    }


    //-------------------------------------------------------//
    fun apply_to_visible_columns(callback: (CellRecyclerAdapter) -> Unit) {
        for (i in 0 until this.itemCount) {
            val viewHolder = this.recycler.findViewHolderForAdapterPosition(i) ?: continue
            if ((viewHolder.itemView as ViewGroup).childCount == 0) {
                continue
            }

            val item = (viewHolder.itemView as ViewGroup).getChildAt(0)
            if (item is CellRecycler) {
                val adapter = item.adapter!! as CellRecyclerAdapter
                callback(adapter)
            }
        }
    }
    fun add_column(index: Int) {
        this.column_count += 1
        this.notifyItemInserted(index)
    }

    fun remove_column(index: Int) {
        this.column_count -= 1
        this.notifyItemRemoved(index)
    }

    //-------------------------------------------------------//
    fun get_activity(): MainActivity {
        return this.recycler.context as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }

    fun get_editor_table(): EditorTable? {
        var view = this.recycler as View
        while (view !is EditorTable) {
            if (view.parent == null) {
                break
            }

            view = view.parent as View
        }

        return if (view is EditorTable) {
            view
        } else {
            null
        }
    }

    fun get_cell_recycler(beat: Int): CellRecycler? {
        val view_holder = this.recycler.findViewHolderForAdapterPosition(beat) ?: return null
        return (view_holder as ColumnRecyclerViewHolder).get_cell_recycler()
    }

    fun clear() {
        val count = this.column_count
        for (x in 0 until count) {
            val cell_recycler = this.get_cell_recycler(x) ?: continue
            (cell_recycler.adapter!! as CellRecyclerAdapter).clear()
        }
        this.column_count = 0
        this.notifyItemRangeRemoved(0, count)
    }

    //-------------------------------------------------------//
    fun get_leaf_view(beat_key: BeatKey, position: List<Int>): LeafButton? { return null }
    fun scroll_to_position(column: Int) { }
    fun scroll_to_position(beat_key: BeatKey, position: List<Int>) {}
}