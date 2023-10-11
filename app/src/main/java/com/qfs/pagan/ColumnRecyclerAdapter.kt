package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt
import com.qfs.pagan.InterfaceLayer as OpusManager

class ColumnRecyclerAdapter(val recycler: ColumnRecycler, editor_table: EditorTable): RecyclerView.Adapter<ColumnRecyclerViewHolder>() {
    val column_label_recycler: ColumnLabelRecycler
    var column_count = 0
    init {
        this.column_label_recycler = editor_table.column_label_recycler

        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(start: Int, count: Int) {
                    this@ColumnRecyclerAdapter.notifyItemChanged(start + count - 1)
                }
                override fun onItemRangeChanged(start: Int, count: Int) {
                    //this@ColumnRecyclerAdapter.column_label_recycler.adapter?.notifyItemRangeChanged(start, count)
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
        val editor_table = this.get_editor_table() ?: return
        val weight = editor_table.get_column_width(position)
        val resources = this.recycler.resources
        ColumnPlaceholder(holder, weight * resources.getDimension(R.dimen.base_leaf_width).roundToInt())
    }
    override fun onViewAttachedToWindow(holder: ColumnRecyclerViewHolder) {
        holder.itemView.layoutParams.height = MATCH_PARENT
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
}