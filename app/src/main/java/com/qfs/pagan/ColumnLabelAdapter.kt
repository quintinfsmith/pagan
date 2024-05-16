package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.OpusLayerInterface as OpusManager

class ColumnLabelAdapter(editor_table: EditorTable) : RecyclerView.Adapter<ColumnLabelViewHolder>() {
    private var _recycler: ColumnLabelRecycler
    private var _column_count = 0
    init {
        this._recycler = editor_table.column_label_recycler
        this._recycler.adapter = this

        this._recycler.itemAnimator = null
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
        return this._column_count
    }

    fun add_column(index: Int) {
        this._column_count += 1
        this.notifyItemInserted(index)
    }

    fun remove_column(index: Int) {
        /* KLUDGE ALERT:
            for some reason, the ColumnRecyclerAdapter and the ColumnLabel Adapter don't automatically scroll to the same place when removing an item.
            everything aroun column_count -= 1 and notifyItemRemoved is just to keep the two recyclers aligned
         */
        val layout_manager = this._recycler.layoutManager as LinearLayoutManager
        val item_position = layout_manager.findFirstVisibleItemPosition()
        val item = layout_manager.findViewByPosition(item_position)
        val original_offset = item?.x

        this.notifyItemRemoved(index)
        this._column_count -= 1

        if (original_offset != null) {
            layout_manager.scrollToPositionWithOffset(item_position, original_offset.toInt())
        }
    }

    fun get_editor_table(): EditorTable? {
        var view = this._recycler as View
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
        return this._recycler.context as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }

    fun clear() {
        val count = this._column_count
        this._column_count = 0
        this.notifyItemRangeRemoved(0, count)
    }
}
