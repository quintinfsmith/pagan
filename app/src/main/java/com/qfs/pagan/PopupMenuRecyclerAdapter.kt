package com.qfs.pagan

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.qfs.apres.soundfont.SoundFont

class PopupMenuRecyclerAdapter(
    private val recycler: RecyclerView,
    private val options: List<String>,
    private val callback: (Int, String) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {
    class PopupMenuRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private var supported_instruments = HashMap<Pair<Int, Int>, String>()

    init {
        this.recycler.adapter = this
        this.recycler.itemAnimator = null
        val that = this

        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    for (i in start until that.recycler.childCount) {
                        that.notifyItemChanged(i)
                    }
                }
                override fun onItemRangeChanged(start: Int, count: Int) { }
                override fun onItemRangeInserted(start: Int, count: Int) { }
                //override fun onChanged() { }
            }
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val text_view = holder.itemView as TextView
        text_view.text = this.options[position]
        (holder.itemView as ViewGroup).addView(text_view)
        text_view.layoutParams.apply {
            width = MATCH_PARENT
            height = WRAP_CONTENT
        }
        text_view.setOnClickListener {
            this.callback( position, this.options[position] )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopupMenuRecyclerViewHolder {
        val text_view = TextView(parent.context)
        return PopupMenuRecyclerViewHolder(text_view)
    }


    override fun getItemCount(): Int {
        return this.options.size
    }
}