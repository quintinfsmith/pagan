package com.qfs.pagan

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlin.math.roundToInt

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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val text_view = holder.itemView as TextView
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
        val text_view = TextView(parent.context)
        val padding = this.recycler.resources.getDimension(R.dimen.dropdown_item_padding).roundToInt()
        text_view.setPadding(0, padding, 0, padding)
        text_view.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            this.recycler.resources.getDimension(R.dimen.dropdown_textsize)
        )
        return PopupMenuRecyclerViewHolder(text_view)
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