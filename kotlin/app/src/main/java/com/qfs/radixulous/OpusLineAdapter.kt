package com.qfs.radixulous

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.qfs.radixulous.structure.OpusTree
import com.qfs.radixulous.opusmanager.OpusEvent
import kotlinx.android.synthetic.main.item_opusline.view.*
import kotlinx.android.synthetic.main.item_opustree.view.*

class OpusLineAdapter(
    private var opus_lines: MutableList<MutableList<OpusTree<OpusEvent>>>
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
    fun inflateOpusTreeView(parent: ViewGroup, tree: OpusTree<OpusEvent>) {
        var view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_opustree,
            parent,
            false
        )

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
            view.tvValue.text = numberstr
        } else if (tree.is_leaf()) {
            view.tvOpenBrace.setVisibility(View.GONE)
            view.tvCloseBrace.setVisibility(View.GONE)
            view.tvValue.text = ".."
        } else {
            view.tvValue.setVisibility(View.GONE)
            for (i in tree.divisions.keys) {
                inflateOpusTreeView(view.llSubTree, tree.divisions[i]!!)
            }
        }
        parent.addView(view)
    }

    override fun onBindViewHolder(holder: OpusLineViewHolder, position: Int) {
        var working_line = this.opus_lines[position]
        for (tree in working_line) {
            this.inflateOpusTreeView(holder.itemView.tvOpusTree, tree)
        }
    }

    override fun getItemCount(): Int {
        return this.opus_lines.size
    }
}
