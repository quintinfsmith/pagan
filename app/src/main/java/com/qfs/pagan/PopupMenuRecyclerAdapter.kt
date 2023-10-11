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
    private val _recycler: RecyclerView,
    private val _options: List<Pair<T, String>>,
    private val _default: T? = null,
    private val _callback: (Int, T) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {
    class PopupMenuRecyclerViewHolder(itemView: View) : ViewHolder(itemView) {
        init {
            this.setIsRecyclable(false)
        }
    }
    init {
        this._recycler.adapter = this
        this._recycler.itemAnimator = null
        this._recycler.layoutManager = LinearLayoutManager(this._recycler.context)
        this.notifyItemRangeInserted(0, this._options.size)
    }
    override fun onViewAttachedToWindow(holder: ViewHolder) { }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val text_view = holder.itemView.findViewById<TextView>(R.id.tvTextView)
        text_view.text = this._options[position].second

        if (this._options[position].first == this._default) {
            text_view.setTypeface(text_view.typeface, Typeface.BOLD)
        } else {
            text_view.setTypeface(text_view.typeface, Typeface.NORMAL)
        }

        text_view.setOnClickListener {
            this._callback( position, this._options[position].first )
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
        return this._options.size
    }

    fun get_default_position(): Int? {
        if (this._default == null) {
            return null
        }

        this._options.forEachIndexed { i: Int, (value, _): Pair<T, String> ->
            if (this._default == value) {
                return i
            }
        }

        return null
    }
}