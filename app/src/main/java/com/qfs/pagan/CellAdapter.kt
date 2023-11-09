package com.qfs.pagan

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CellAdapter: RecyclerView.Adapter<CellViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        return CellViewHolder(parent.context)
    }

    override fun getItemCount(): Int {

    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        holder
    }
}