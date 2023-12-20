package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TuningMapRecycler(context: Context, attrs: AttributeSet): RecyclerView(context, attrs) {
    class TuningMapViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
    init {
        this.layoutManager = LinearLayoutManager(this.context)
    }

    fun reset_tuning_map(new_radix: Int? = null) {
        val adapter = this.adapter as TuningMapRecyclerAdapter
        val radix = new_radix ?: adapter.tuning_map.size
        adapter.tuning_map = Array(radix) { i: Int ->
            Pair(i, radix)
        }
        adapter.notifyDataSetChanged()
    }
}