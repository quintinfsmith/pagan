package com.qfs.radixulous

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.opusmanager.BeatKey

import com.qfs.radixulous.structure.OpusTree
import com.qfs.radixulous.opusmanager.OpusEvent
import kotlinx.android.synthetic.main.item_opusline.view.*
import kotlinx.android.synthetic.main.item_opustree.view.*
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager

class OpusLineAdapter(
    private var opus_manager: OpusManager
): RecyclerView.Adapter<OpusLineAdapter.OpusLineViewHolder>() {
    class OpusLineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpusLineViewHolder {
        return OpusLineViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_opusline,
                parent,
                false
            )
        )
    }
    fun inflateOpusTreeView(parent: ViewGroup, beat_key: BeatKey, position: List<Int>) {
        var view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_opustree,
            parent,
            false
        )
        val tree = this.opus_manager.get_tree(beat_key, position)
        if (tree.is_event()) {
            view.tvOpenBrace.setVisibility(View.GONE)
            view.tvCloseBrace.setVisibility(View.GONE)
            var event = tree.get_event()!!
            var numberstr: String
            if (event.relative) {
                numberstr = "T"
            } else {
                numberstr = get_number_string(event.note, event.radix, 2)
            }
            view.btnValue.text = numberstr

            view.btnValue.setOnClickListener {
                var cursor = this.opus_manager.cursor
                cursor.set_by_beatkey_position(beat_key, position)
                it.btnValue.setBackgroundColor(Color.parseColor("#ff0000"))
            }
        } else if (tree.is_leaf()) {
            view.tvOpenBrace.setVisibility(View.GONE)
            view.tvCloseBrace.setVisibility(View.GONE)
            view.btnValue.text = ".."
            view.btnValue.setOnClickListener {
                var cursor = this.opus_manager.cursor
                cursor.set_by_beatkey_position(beat_key, position)
                it.btnValue.setBackgroundColor(Color.parseColor("#ffff00"))
            }
        } else {
            view.btnValue.setVisibility(View.GONE)
            for (i in tree.divisions.keys) {
                var new_position = position.toMutableList()
                new_position.add(i)
                inflateOpusTreeView(view.llSubTree, beat_key, new_position)
            }
        }
        parent.addView(view)
    }

    override fun onBindViewHolder(holder: OpusLineViewHolder, position: Int) {
        var channel_index = this.opus_manager.get_channel_index(position)
        var channel = channel_index.first
        var line_offset = channel_index.second
        var line = this.opus_manager.channel_lines[channel][line_offset]
        for (i in 0 until line.size) {
            var beat_key = BeatKey(channel, line_offset, i)
            this.inflateOpusTreeView(holder.itemView.tvOpusTree, beat_key, listOf())
        }
    }

    override fun getItemCount(): Int {
        return this.opus_manager.line_count()
    }
}
