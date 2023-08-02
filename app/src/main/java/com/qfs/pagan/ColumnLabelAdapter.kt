package com.qfs.pagan

import android.util.Log
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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
    }

    fun update_label_focus(label_view: ColumnLabelView) {
        val holder  = label_view.viewHolder ?: return
        val beat = holder.bindingAdapterPosition
        //val cursor = this.opus_manager.cursor
        //label_view.set_focused(
        //    when (cursor.mode) {
        //        Cursor.CursorMode.Column -> {
        //            cursor.beat == beat
        //        }
        //        Cursor.CursorMode.Single -> {
        //            cursor.beat == beat
        //        }
        //        Cursor.CursorMode.Range -> {
        //            val from_key = cursor.range!!.first
        //            val to_key = cursor.range!!.second

        //            if (from_key.beat != to_key.beat) {
        //                (from_key.beat .. to_key.beat).contains(beat)
        //            } else {
        //                beat == from_key.beat
        //            }
        //        }
        //        else -> {
        //            false
        //        }
        //    }
        //)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnLabelViewHolder {
        val label = ColumnLabelView(parent.context)

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

        return ColumnLabelViewHolder(label)
    }

    override fun onViewAttachedToWindow(holder: ColumnLabelViewHolder) {
        super.onViewAttachedToWindow(holder)

        val beat = holder.bindingAdapterPosition
        val new_width = this.get_column_width(beat)
        holder.itemView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        holder.itemView.layoutParams.width = (new_width * this.recycler.resources.getDimension(R.dimen.base_leaf_width)).toInt()
    }

    override fun onBindViewHolder(holder: ColumnLabelViewHolder, position: Int) {
        this.set_text(holder, position)
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

    fun get_column_width(beat: Int): Int {
        val editor_table = this.recycler.editor_table
        return editor_table.get_column_width(beat)
    }

    fun set_text(holder: ColumnLabelViewHolder, position: Int) {
        val item_view = holder.itemView as ColumnLabelView
        Log.d("AAA", "setting $position")
        item_view.set_text("$position")
    }

    fun scroll(x: Int) {
        this.recycler.scrollBy(x, 0)
    }

    //fun set_cursor_focus(show: Boolean = true) {
    //    val cursor = this.opus_manager.cursor
    //    when (cursor.mode) {
    //        Cursor.CursorMode.Range -> {
    //            val (from_key, to_key) = cursor.range!!
    //            for (i in from_key.beat .. to_key.beat) {
    //                val viewHolder = this.recycler.findViewHolderForAdapterPosition(i) ?: return
    //                val label = viewHolder.itemView as LabelView
    //                label.set_focused(show)
    //                label.invalidate()
    //            }
    //        }
    //        Cursor.CursorMode.Single,
    //        Cursor.CursorMode.Column -> {
    //            val viewHolder = this.recycler.findViewHolderForAdapterPosition(cursor.beat) ?: return
    //            val label = viewHolder.itemView as LabelView
    //            label.set_focused(show)
    //            label.invalidate()
    //        }
    //        Cursor.CursorMode.Row,
    //        Cursor.CursorMode.Unset -> { }
    //    }
    //}

    fun get_editor_table(): EditorTable {
        return this.editor_table
    }

}
