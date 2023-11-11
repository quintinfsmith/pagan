package com.qfs.pagan

import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.InterfaceLayer as OpusManager

class CellAdapter(var recycler: RecyclerView): RecyclerView.Adapter<CellViewHolder>() {
    var item_count = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        return CellViewHolder(parent.context)
    }

    override fun getItemCount(): Int {
        return this.item_count
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        val opus_manager = this.get_opus_manager()
        val line_count = opus_manager.get_visible_line_count()
        val (channel, line_offset) = opus_manager.get_std_offset(position % line_count)
        val beat = position / line_count
        holder.beat_key = BeatKey(channel, line_offset, beat)
        CellLayout(holder)
    }

    fun get_activity(): MainActivity {
        return this.recycler.context as MainActivity
    }

    fun get_editor_table(): EditorTable {
        return this.get_activity().findViewById(R.id.etEditorTable)
    }

    fun get_opus_manager(): OpusManager {
        return (this.recycler as CellRecycler).get_opus_manager()
    }

    fun insert_column(index: Int) {
        this.insert_columns(index, 1)
    }

    fun insert_columns(index: Int, count: Int) {
        var line_count = this.get_opus_manager().get_visible_line_count()
        this.item_count += line_count * count
        this.notifyItemRangeInserted(index * line_count, line_count * count)
    }

    fun remove_column(index: Int) {
        this.remove_columns(index, 1)
    }

    fun remove_columns(index: Int, count: Int) {
        var line_count = this.get_opus_manager().get_visible_line_count()
        this.item_count -= line_count * count
        this.notifyItemRangeRemoved(
            index * line_count,
            line_count * count
        )
    }

    fun insert_row(index: Int) {
        this.insert_rows(index, 1)
    }

    fun insert_rows(index: Int, count: Int) {
        var beat_count = this.get_opus_manager().beat_count
        var line_count = this.get_opus_manager().get_visible_line_count()
        this.item_count += beat_count * count

        for (i in 0 until beat_count) {
            this.notifyItemRangeInserted((i * line_count) + index, count)
        }
        (this.recycler.layoutManager as GridLayoutManager).spanCount = line_count
    }

    fun remove_row(index: Int) {
        this.remove_rows(index, 1)
    }

    fun remove_rows(index: Int, count: Int) {
        val line_count = this.get_opus_manager().get_visible_line_count()
        val beat_count = this.get_opus_manager().beat_count
        this.item_count -= beat_count
        for (i in 0 until beat_count) {
            this.notifyItemRemoved((i * line_count) + index)
        }
        (this.recycler.layoutManager as GridLayoutManager).spanCount = line_count
    }

    fun clear() {
        var item_count = this.item_count
        this.item_count = 0
        this.notifyItemRangeRemoved(0, item_count)
    }

    fun notifyBeatChanged(beat_key: BeatKey, state_only: Boolean = false) {
        val abs_line = this.get_opus_manager().get_abs_offset(beat_key.channel, beat_key.line_offset)
        var span_count = (this.recycler.layoutManager as GridLayoutManager).spanCount

        val offset = (beat_key.beat * span_count) + abs_line
        if (state_only) {
            this.invalidate_items(offset, 1)
        } else {
            this.notifyItemChanged(offset)
        }
    }

    private fun invalidate_items(offset: Int, count: Int) {
        for (i in offset until offset + count) {
            var item_view =
                this.recycler.findViewHolderForAdapterPosition(i)?.itemView ?: return
            ((item_view as ViewGroup).getChildAt(0) as CellLayout).invalidate_all()
        }
    }

    fun notifyColumnChanged(index: Int, state_only: Boolean = false) {
        val span_count = (this.recycler.layoutManager as GridLayoutManager).spanCount
        if (state_only) {
            this.invalidate_items(index * span_count, span_count)
        } else {
            this.notifyItemRangeChanged(index * span_count, span_count)
        }
    }

    fun notifyRowChanged(index: Int, state_only: Boolean = false) {
        val span_count = (this.recycler.layoutManager as GridLayoutManager).spanCount
        val beat_count = this.get_opus_manager().beat_count
        for (i in 0 until beat_count) {
            if (state_only) {
                this.invalidate_items(index + (i * span_count), 1)
            } else {
                this.notifyItemChanged(index + (i * span_count))
            }
        }
    }
}