package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.OpusLayerInterface as OpusManager

class ColumnLabelAdapter(editor_table: EditorTable) : RecyclerView.Adapter<ColumnLabelViewHolder>() {
    var recycler: ColumnLabelRecycler
    var column_count = 0
    init {
        this.recycler = editor_table.column_label_recycler
        this.recycler.adapter = this

        this.recycler.itemAnimator = null
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    this@ColumnLabelAdapter.notifyItemRangeChanged(start + count, this@ColumnLabelAdapter.itemCount)
                }
                override fun onItemRangeInserted(start: Int, count: Int) {
                    this@ColumnLabelAdapter.notifyItemRangeChanged(start + count - 1, this@ColumnLabelAdapter.itemCount)
                }
                override fun onItemRangeChanged(start: Int, count: Int) {
                    //this@ColumnLabelAdapter.notifyItemRangeChanged(start, count)
                }
            }
        )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnLabelViewHolder {
        return ColumnLabelViewHolder(parent.context)
    }

    override fun onBindViewHolder(holder: ColumnLabelViewHolder, position: Int) {
        ColumnLabelView(holder)
    }

    override fun getItemCount(): Int {
        return this.column_count
    }

    fun add_column(index: Int) {
        this.column_count += 1
        this.notifyItemInserted(index)
    }

    fun remove_column(index: Int) {
        this.column_count -= 1
        this.notifyItemRemoved(index)
    }

    fun get_editor_table(): EditorTable? {
        var view = this.recycler as View
        while (view !is EditorTable && view.parent != null) {
            view = view.parent as View
        }
        return if (view is EditorTable) {
            view
        } else {
            null
        }
    }

    fun get_activity(): MainActivity {
        return this.recycler.context as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }

    fun clear() {
        val count = this.column_count
        this.column_count = 0
        this.notifyItemRangeRemoved(0, count)
    }
}
