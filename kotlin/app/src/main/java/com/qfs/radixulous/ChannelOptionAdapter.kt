package com.qfs.radixulous

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.apres.SoundFont

class ChannelOptionAdapter(
    private val activity: MainActivity,
    val opus_manager: InterfaceLayer,
    private val recycler: RecyclerView,
    private val soundfont: SoundFont
) : RecyclerView.Adapter<ChannelOptionAdapter.ChannelOptionViewHolder>() {
    private var supported_instruments: Set<Int>
    class ChannelOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    init {
        this.recycler.adapter = this
        this.recycler.layoutManager = LinearLayoutManager(this.activity)
        val that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    for (i in start until that.recycler.childCount) {
                        that.notifyItemChanged(i)
                    }
                }
                override fun onItemRangeChanged(start: Int, count: Int) { }
                override fun onItemRangeInserted(start: Int, count: Int) { }
                //override fun onChanged() { }
            }
        )
        val supported_instruments = this.soundfont.get_available_presets(0)
        this.supported_instruments = supported_instruments
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelOptionViewHolder {
        return ChannelOptionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.config_active_channel,
                parent,
                false
            )
        )
    }


    private fun set_text(view: View, position: Int) {
        val channels = this.opus_manager.channels
        val curChannel = channels[position]
        val btnChooseInstrument: TextView = view.findViewById(R.id.btnChooseInstrument)
        val label = this.get_label(
            if (this.opus_manager.is_percussion(position)) {
                -1
            } else {
                curChannel.midi_instrument
            }
        )
        btnChooseInstrument.text = "$position: $label"
    }

    private fun get_label(instrument: Int): String {
        val instrument_array = this.activity.resources.getStringArray(R.array.midi_instruments)

        val prefix = if (instrument == -1) {
            if (this.soundfont.get_available_presets(128).isNotEmpty()) {
                ""
            } else {
                "\uD83D\uDD07"
            }
        } else {
            if (instrument in this.supported_instruments) {
                ""
            } else {
                "\uD83D\uDD07"
            }
        }

        return if (instrument == -1) {
            "$prefix Percussion"
        } else {
            "$prefix ${instrument_array[instrument]}"
        }
    }

    override fun onBindViewHolder(holder: ChannelOptionViewHolder, position: Int) {
        this.set_text(holder.itemView as ViewGroup, position)

        holder.itemView.findViewById<TextView>(R.id.btnChooseInstrument).setOnClickListener {
            this.interact_btnChooseInstrument(it, position)
        }

        holder.itemView.findViewById<TextView>(R.id.btnRemoveChannel).setOnClickListener {
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
        for (i in 0 until this.recycler.childCount) {
            if (this.recycler.getChildAt(i) == check) {
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
        if (this.opus_manager.channels.size > 1) {
            val x = this.get_view_channel(view)
            this.opus_manager.remove_channel(x)
        }
    }

    private fun interact_btnChooseInstrument(view: View, index: Int) {
        val wrapper = ContextThemeWrapper(this.activity, R.style.PopupMenu)
        val popupMenu = PopupMenu(wrapper, view)
        val channel = this.get_view_channel(view)

        for (i in 0 until 128) {
            if (i == 0 && this.opus_manager.percussion_channel != null) {
                continue
            }
            popupMenu.menu.add(0, i - 1, i, "$i: ${this.get_label(i - 1)}")
        }

        popupMenu.setOnMenuItemClickListener {
            this.set_channel_instrument(channel, it.itemId)
            false
        }

        popupMenu.show()
    }

    private fun set_channel_instrument(channel: Int, instrument: Int) {
        if (instrument == -1) {
            this.opus_manager.set_percussion_channel(channel)
            return
        }
        this.opus_manager.set_channel_instrument(channel, instrument)
    }

    override fun getItemCount(): Int {
        return this.activity.get_opus_manager().channels.size
    }
}