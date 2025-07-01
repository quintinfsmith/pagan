package com.qfs.pagan

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.qfs.apres.MidiController

class MidiDeviceManagerAdapter<T>(val midi_controller: MidiController): RecyclerView.Adapter<ViewHolder>() {
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }
}