package com.qfs.radixulous

import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.opusmanager.OpusChannel
import kotlinx.android.synthetic.main.config_active_channel.view.*

class ChannelOptionAdapter(
    private val channels: MutableList<OpusChannel>
) : RecyclerView.Adapter<ChannelOptionAdapter.ChannelOptionViewHolder>() {

    class ChannelOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelOptionViewHolder {
        return ChannelOptionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.config_active_channel,
                parent,
                false
            )
        )
    }

    fun addChannel(channel: OpusChannel) {
        channels.add(channel)
        notifyItemInserted(channels.size - 1)
    }

    //fun deleteDoneTodos() {
    //    channels.removeAll { todo ->
    //        todo.isChecked
    //    }
    //    notifyDataSetChanged()
    //}

    //private fun toggleStrikeThrough(tvTodoTitle: TextView, isChecked: Boolean) {
    //    if(isChecked) {
    //        tvTodoTitle.paintFlags = tvTodoTitle.paintFlags or STRIKE_THRU_TEXT_FLAG
    //    } else {
    //        tvTodoTitle.paintFlags = tvTodoTitle.paintFlags and STRIKE_THRU_TEXT_FLAG.inv()
    //    }
    //}

    override fun onBindViewHolder(holder: ChannelOptionViewHolder, position: Int) {
        val curChannel = channels[position]
        holder.itemView.btnChooseInstrument.text = "!"

        holder.itemView.apply {
            btnChooseInstrument.text = "${curChannel.instrument}"
            //toggleStrikeThrough(tvTodoTitle, curTodo.isChecked)

            //cbDone.setOnCheckedChangeListener { _, isChecked ->
            //    toggleStrikeThrough(tvTodoTitle, isChecked)
            //    curTodo.isChecked = !curTodo.isChecked
            //}
        }
    }

    override fun getItemCount(): Int {
        return channels.size
    }
}


















