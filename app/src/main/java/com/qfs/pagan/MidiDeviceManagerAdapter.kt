package com.qfs.pagan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qfs.apres.MidiController

class MidiDeviceManagerAdapter<T>(val midi_controller: MidiController): RecyclerView.Adapter<MidiDeviceManagerAdapter.MidiDeviceViewHolder>() {
    lateinit var recycler_view: RecyclerView
    class MidiDeviceViewHolder(item_view: View) : RecyclerView.ViewHolder(item_view)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recycler_view = recyclerView
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MidiDeviceViewHolder {
        return MidiDeviceViewHolder(
            LayoutInflater.from(this.recycler_view.context)
                .inflate(
                    R.layout.midi_device,
                    this.recycler_view,
                    false
                )
        )
    }

    override fun onBindViewHolder(
        holder: MidiDeviceViewHolder,
        position: Int
    ) {
        val devices = this.midi_controller.poll_output_devices()
        val id_view = holder.itemView.findViewById<TextView>(R.id.midi_device_id)
        id_view.text = "${devices[position].id}"
    }

    override fun getItemCount(): Int {
        val devices = this.midi_controller.poll_output_devices()
        return devices.size
    }
}