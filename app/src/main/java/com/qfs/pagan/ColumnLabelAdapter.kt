package com.qfs.pagan

import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.InterfaceLayer as OpusManager

class ColumnLabelAdapter(var editor_table: EditorTable) : RecyclerView.Adapter<ColumnLabelViewHolder>() {
    var recycler: ColumnLabelRecycler
    var column_recycler: ColumnRecycler
    var column_count = 0
    init {
        this.column_recycler = editor_table.main_recycler
        this.recycler = editor_table.column_label_recycler
        this.recycler.adapter = this
        this.recycler.layoutManager = LinearLayoutManager(
            this.recycler.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        //(this.recycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        this.recycler.itemAnimator = null
        val that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    that.notifyItemRangeChanged(start + count, that.itemCount)
                }
                override fun onItemRangeInserted(start: Int, count: Int) {
                    that.notifyItemRangeChanged(start + count - 1, that.itemCount)
                }
                //override fun onItemRangeChanged(start: Int, count: Int) {
                //    that.column_label_recycler.adapter?.notifyItemRangeChanged(start, count)
                //}
            }
        )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnLabelViewHolder {
        //label.setOnClickListener {
        //    val holder = (it as LabelView).viewHolder ?: return@setOnClickListener
        //    val beat = holder.bindingAdapterPosition

        //    val rvTable = this.activity.findViewById<RecyclerView>(R.id.rvTable)
        //    val adapter = rvTable.adapter as BeatColumnAdapter
        //    if (adapter.linking_beat != null) {
        //        adapter.cancel_linking()
        //    }

        //    this.opus_manager.cursor_select_column(beat)
        //}

        //label.setOnFocusChangeListener { view, is_focused: Boolean ->
        //    if (is_focused) {
        //        val holder = (view as LabelView).viewHolder ?: return@setOnFocusChangeListener
        //        val beat = holder.bindingAdapterPosition
        //        this.opus_manager.cursor_select_column(beat)
        //    }
        //}

        return ColumnLabelViewHolder(parent.context)
    }

    override fun onViewAttachedToWindow(holder: ColumnLabelViewHolder) { }

    override fun onBindViewHolder(holder: ColumnLabelViewHolder, position: Int) {
        val beat = holder.bindingAdapterPosition
        val label = ColumnLabelView(holder)
        label.set_text("$beat")
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

    fun scroll(x: Int) {
        this.recycler.scrollBy(x, 0)
    }

    fun get_editor_table(): EditorTable {
        return this.editor_table
    }

    fun get_activity(): MainActivity {
        return this.recycler.context as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }

    fun clear() {
        var count = this.column_count
        this.column_count = 0
        this.notifyItemRangeRemoved(0, count)
    }

}
