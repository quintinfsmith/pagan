package com.qfs.pagan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qfs.apres.soundfont.SoundFont

class ChannelOptionAdapter(
    private val _opus_manager: InterfaceLayer,
    private val _recycler: RecyclerView
) : RecyclerView.Adapter<ChannelOptionAdapter.ChannelOptionViewHolder>() {
    class OutOfSyncException : Exception("Channel Option list out of sync with OpusManager")
    class ChannelOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    private var _supported_instruments = HashMap<Pair<Int, Int>, String>()
    init {
        this._recycler.adapter = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    for (i in start until this@ChannelOptionAdapter._recycler.childCount) {
                        this@ChannelOptionAdapter.notifyItemChanged(i)
                    }
                }
                override fun onItemRangeChanged(start: Int, count: Int) { }
                override fun onItemRangeInserted(start: Int, count: Int) { }
            }
        )
        val soundfont = this.get_activity().get_soundfont()
        if (soundfont != null) {
            for ((name, program, bank) in soundfont.get_available_presets()) {
                this._supported_instruments[Pair(bank, program)] = name
            }
        }
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

    fun get_activity(): MainActivity {
        return this._recycler.context as MainActivity
    }

    private fun set_text(view: View, position: Int) {
        val activity = this.get_activity()
        val channels = this._opus_manager.channels
        val curChannel = channels[position]
        val btnChooseInstrument: TextView = view.findViewById(R.id.btnChooseInstrument)
        val defaults = activity.resources.getStringArray(R.array.midi_instruments)
        val label = this._supported_instruments[Pair(
            curChannel.midi_bank,
            curChannel.midi_program
        )] ?: if (curChannel.midi_channel == 9) {
            activity.resources.getString(R.string.unknown_percussion)
        } else {
            activity.resources.getString(R.string.unknown_instrument, defaults[curChannel.midi_program])
        }

        btnChooseInstrument.text = activity.getString(R.string.label_choose_instrument, position, label)
    }

    override fun onBindViewHolder(holder: ChannelOptionViewHolder, position: Int) {
        this.set_text(holder.itemView as ViewGroup, position)

        holder.itemView.findViewById<TextView>(R.id.btnChooseInstrument).setOnClickListener {
            this.interact_btnChooseInstrument(it)
        }

        val remove_button = holder.itemView.findViewById<TextView>(R.id.btnRemoveChannel)
        remove_button.visibility = if (this._opus_manager.is_percussion(position)) {
             View.GONE
        } else {
            View.VISIBLE
        }

        remove_button.setOnClickListener {
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
        for (i in 0 until this._recycler.childCount) {
            if (this._recycler.getChildAt(i) == check) {
                x = i
                break
            }
        }

        if (x == null) {
            throw OutOfSyncException()
        }

        return x
    }

    private fun interact_btnRemoveChannel(view: View) {
        if (this._opus_manager.channels.size > 1) {
            val x = this.get_view_channel(view)
            this._opus_manager.remove_channel(x)
        }
    }

    private fun interact_btnChooseInstrument(view: View) {
        val channel = this.get_view_channel(view)

        val sorted_keys = this._supported_instruments.keys.toList().sortedBy {
            it.first + (it.second * 128)
        }

        val options = mutableListOf<Pair<Pair<Int, Int>, String>>()
        sorted_keys.forEach { key: Pair<Int, Int> ->
            val name = this._supported_instruments[key]
            if ((this._opus_manager.is_percussion(channel) && key.first == 128)) {
                options.add(Pair(key, "[${key.second}] $name"))
            } else if (!(key.first == 128 || this._opus_manager.is_percussion(channel))) {
                val pairstring = "${key.first}/${key.second}"
                options.add(Pair(key, "[$pairstring] $name"))
            }
        }

        val default_position = this._opus_manager.get_channel_instrument(channel)
        this.get_activity().popup_menu_dialog<Pair<Int, Int>>(this.get_activity().getString(R.string.dropdown_choose_instrument), options, default = default_position) { _: Int, (bank, program): Pair<Int, Int> ->
            this.set_channel_instrument(channel, bank, program)
        }
    }

    private fun set_channel_instrument(channel: Int, bank: Int, program: Int) {
        this._opus_manager.set_channel_instrument(channel, Pair(bank, program))
    }

    override fun getItemCount(): Int {
        return this.get_activity().get_opus_manager().channels.size
    }

    fun set_soundfont(soundfont: SoundFont) {
        this._supported_instruments.clear()
        for ((name, program, bank) in soundfont.get_available_presets()) {
            this._supported_instruments[Pair(bank, program)] = name
        }
        this.notifyItemRangeChanged(0, this._opus_manager.channels.size)
    }

    fun unset_soundfont() {
        this._supported_instruments.clear()
        this.notifyItemRangeChanged(0, this._opus_manager.channels.size)
    }
}