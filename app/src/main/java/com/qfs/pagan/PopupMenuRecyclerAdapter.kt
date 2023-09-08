package com.qfs.pagan

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class PopupMenuRecyclerAdapter<T>(
    private val recycler: RecyclerView,
    private val options: List<Pair<T, String>>,
    private val default: T? = null,
    private val callback: (Int, T) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {
    class PopupMenuRecyclerViewHolder(itemView: View) : ViewHolder(itemView) {
        init {
            this.setIsRecyclable(false)
        }
    }
    init {
        this.recycler.adapter = this
        this.recycler.itemAnimator = null
        this.recycler.layoutManager = LinearLayoutManager(this.recycler.context)
        this.notifyItemRangeInserted(0, this.options.size)
    }
    override fun onViewAttachedToWindow(holder: ViewHolder) { }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val text_view = holder.itemView.findViewById<TextView>(R.id.tvTextView)
        text_view.text = this.options[position].second

        if (this.options[position].first == this.default) {
            text_view.setTypeface(text_view.typeface, Typeface.BOLD)
        } else {
            text_view.setTypeface(text_view.typeface, Typeface.NORMAL)
        }

        text_view.setOnClickListener {
            this.callback( position, this.options[position].first )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopupMenuRecyclerViewHolder {
        val viewInflated: View = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.dropdown_menu_item,
                parent,
                false
            )
        return PopupMenuRecyclerViewHolder(viewInflated)
    }


    override fun getItemCount(): Int {
        return this.options.size
    }

    fun get_default_position(): Int? {
        if (this.default == null) {
            return null
        }

        this.options.forEachIndexed { i: Int, (value, _): Pair<T, String> ->
            if (this.default == value) {
                return i
            }
        }

        return null
    }
}