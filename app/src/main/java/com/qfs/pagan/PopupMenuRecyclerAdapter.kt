package com.qfs.pagan

import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.qfs.apres.soundfont.SoundFont

class PopupMenuRecyclerAdapter(
    private val recycler: RecyclerView,
    private val options: List<Pair<Int, String>>,
    private val callback: (Int, Int) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {
    class PopupMenuRecyclerViewHolder(itemView: View) : ViewHolder(itemView)
    init {
        this.recycler.adapter = this
        this.recycler.itemAnimator = null
        this.recycler.layoutManager = LinearLayoutManager(this.recycler.context)
        this.notifyItemRangeInserted(0, this.options.size)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val text_view = holder.itemView as TextView
        text_view.text = this.options[position].second
        text_view.minEms = 3

        text_view.setOnClickListener {
            this.callback( position, this.options[position].first )
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