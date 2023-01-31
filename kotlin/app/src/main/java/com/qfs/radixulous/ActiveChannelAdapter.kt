package com.qfs.radixulous

import android.content.Context
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.apres.ProgramChange
import com.qfs.radixulous.opusmanager.OpusChannel
import com.qfs.radixulous.opusmanager.CursorLayer as OpusManager
import kotlinx.android.synthetic.main.config_active_channel.view.*
import kotlinx.android.synthetic.main.table_line_label.view.*

class ChannelOptionAdapter(
    private val opus_manager: OpusManager
) : RecyclerView.Adapter<ChannelOptionAdapter.ChannelOptionViewHolder>() {

    class ChannelOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    var recycler: RecyclerView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelOptionViewHolder {
        return ChannelOptionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.config_active_channel,
                parent,
                false
            )
        )
    }

    fun addChannel() {
        this.opus_manager.new_channel()
        this.opus_manager.new_line(this.opus_manager.channels.size - 1)

        notifyItemInserted(this.opus_manager.channels.size - 1)
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
        var channels = this.opus_manager.channels

        val curChannel = channels[position]

        val instrument = curChannel.midi_instrument
        holder.itemView.btnChooseInstrument.text = if (instrument == 0) {
            "$position: Percussion"
        } else {
            "$position: ${holder.itemView.resources.getStringArray(R.array.midi_instruments)[instrument - 1]}"
        }

        holder.itemView.btnChooseInstrument.setOnClickListener {
            this.interact_btnChooseInstrument(holder.itemView.context, it)
        }

        holder.itemView.btnRemoveChannel.setOnClickListener {
            this.interact_btnRemoveChannel(it)
        }
    }

    private fun get_view_channel(view: View): Int {
        var parent = view.parent
        var check = view
        while (parent !is RecyclerView) {
            check = parent as View
            parent = (parent as View).parent
        }

        var x: Int? = null
        for (i in 0 until parent.childCount) {
            if (parent.getChildAt(i) == check) {
                x = i
                break
            }
        }

        if (x == null) {
            throw Exception("View position in parent is higher than channel count")
        }

        return x
    }

    private fun interact_btnRemoveChannel(view: View) {
        var x = this.get_view_channel(view)
        this.opus_manager.remove_channel(x)
        this.notifyItemRemoved(x)
    }

    private fun interact_btnChooseInstrument(context: Context, view: View) {
        val popupMenu = PopupMenu(context, view)
        var channel = this.get_view_channel(view)

        //if (opus_manager.is_percussion(channel)) {
        //    val drums = view.resources.getStringArray(R.array.midi_drums)
        //    drums.forEachIndexed { i, string ->
        //        popupMenu.menu.add(0, i, i, string)
        //    }

        //    popupMenu.setOnMenuItemClickListener {
        //        opus_manager.set_percussion_instrument(
        //            cursor.get_beatkey().line_offset,
        //            it.itemId
        //        )
        //        true
        //    }
        //}


        val instruments = view.resources.getStringArray(R.array.midi_instruments)
        var x = 0
        if (this.opus_manager.percussion_channel == null) {
            popupMenu.menu.add(0, x, 0, "0: Percussion")
            x += 1
        }

        instruments.forEachIndexed { i, string ->
            popupMenu.menu.add(0, x, i + 1, "${i + 1}: $string")
            x += 1
        }

        popupMenu.setOnMenuItemClickListener {
            if (it.itemId == 0) {
                this.set_percussion_channel(channel)
            } else {
                this.set_channel_instrument(channel, it.itemId)
            }

            // TODO: This feels sloppy
            //this.getMain().midi_input_device.sendEvent(ProgramChange(cursor.get_beatkey().channel, it.itemId))
            true
        }

        popupMenu.show()
    }

    private fun set_channel_instrument(channel: Int, instrument: Int) {
        if (instrument == 0) {
            this.set_percussion_channel(channel)
            return
        }

        var view = this.recycler?.getChildAt(channel)
        if (view != null) {
            val instruments = view!!.resources.getStringArray(R.array.midi_instruments)
            var index = if (this.opus_manager.percussion_channel == null) {
                instrument - 1
            } else {
                instrument
            }
            (view.btnChooseInstrument as TextView).text = "$channel: ${instruments[index]}"
        }

        this.opus_manager.set_channel_instrument(channel, instrument)
    }

    private fun set_percussion_channel(channel: Int) {
        if (this.opus_manager.percussion_channel != null) {
            this.set_channel_instrument(this.opus_manager.percussion_channel!!, 1)
        }

        var view = this.recycler?.getChildAt(channel)
        if (view != null) {
            (view.btnChooseInstrument as TextView).text = "$channel: Percussion"
        }
        this.opus_manager.set_percussion_channel(channel)
    }

    override fun getItemCount(): Int {
        return this.opus_manager.channels.size
    }
}


















