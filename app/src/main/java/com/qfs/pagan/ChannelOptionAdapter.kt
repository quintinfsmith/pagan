package com.qfs.pagan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.apres.soundfont.SoundFont

class ChannelOptionAdapter(
    private val activity: MainActivity,
    private val opus_manager: InterfaceLayer,
    private val recycler: RecyclerView,
    private var soundfont: SoundFont?
) : RecyclerView.Adapter<ChannelOptionAdapter.ChannelOptionViewHolder>() {
    class OutOfSyncException : Exception("Channel Option list out of sync with OpusManager")
    class ChannelOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    private var supported_instruments = HashMap<Pair<Int, Int>, String>()
    init {
        this.recycler.adapter = this
        this.recycler.layoutManager = LinearLayoutManager(this.activity)
        this.recycler.itemAnimator = null
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

        if (this.soundfont != null) {
            for ((name, program, bank) in this.soundfont!!.get_available_presets()) {
                this.supported_instruments[Pair(bank, program)] = name
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

    private fun set_text(view: View, position: Int) {
        val channels = this.opus_manager.channels
        val curChannel = channels[position]
        val btnChooseInstrument: TextView = view.findViewById(R.id.btnChooseInstrument)
        val defaults = this.activity.resources.getStringArray(R.array.midi_instruments)
        val label = this.supported_instruments[Pair(
            curChannel.midi_bank,
            curChannel.midi_program
        )] ?: if (curChannel.midi_channel == 9) {
            this.activity.resources.getString(R.string.unknown_percussion)
        } else {
            this.activity.resources.getString(R.string.unknown_instrument, defaults[curChannel.midi_program])
        }

        btnChooseInstrument.text = this.activity.getString(R.string.label_choose_instrument, position, label)
    }

    override fun onBindViewHolder(holder: ChannelOptionViewHolder, position: Int) {
        this.set_text(holder.itemView as ViewGroup, position)

        holder.itemView.findViewById<TextView>(R.id.btnChooseInstrument).setOnClickListener {
            this.interact_btnChooseInstrument(it)
        }

        val remove_button = holder.itemView.findViewById<TextView>(R.id.btnRemoveChannel)
        remove_button.visibility = if (this.opus_manager.is_percussion(position)) {
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
        for (i in 0 until this.recycler.childCount) {
            if (this.recycler.getChildAt(i) == check) {
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
        if (this.opus_manager.channels.size > 1) {
            val x = this.get_view_channel(view)
            this.opus_manager.remove_channel(x)
        }
    }

    private fun interact_btnChooseInstrument(view: View) {
        val channel = this.get_view_channel(view)

        val sorted_keys = this.supported_instruments.keys.toList().sortedBy {
            it.first + (it.second * 128)
        }

        val options = mutableListOf<Pair<Pair<Int, Int>, String>>()
        sorted_keys.forEachIndexed { i: Int, key: Pair<Int, Int> ->
            val name = this.supported_instruments[key]
            if ((this.opus_manager.is_percussion(channel) && key.first == 128)) {
                options.add(Pair(key, "[${key.second}] $name"))
            } else if (!(key.first == 128 || this.opus_manager.is_percussion(channel))) {
                val pairstring = "${key.first}/${key.second}"
                options.add(Pair(key, "[$pairstring] $name"))
            }
        }

        var default_position = this.opus_manager.get_channel_instrument(channel)
        this.activity.popup_menu_dialog<Pair<Int, Int>>(this.activity.getString(R.string.dropdown_choose_instrument), options, default = default_position) { index: Int, (bank, program): Pair<Int, Int> ->
            this.set_channel_instrument(channel, bank, program)
        }
    }

    private fun set_channel_instrument(channel: Int, bank: Int, program: Int) {
        this.opus_manager.set_channel_instrument(channel, Pair(bank, program))
    }

    override fun getItemCount(): Int {
        return this.activity.get_opus_manager().channels.size
    }

    fun set_soundfont(soundfont: SoundFont) {
        this.soundfont = soundfont
        this.supported_instruments.clear()
        for ((name, program, bank) in this.soundfont!!.get_available_presets()) {
            this.supported_instruments[Pair(bank, program)] = name
        }
        this.notifyItemRangeChanged(0, this.opus_manager.channels.size)
    }

    fun unset_soundfont() {
        this.supported_instruments.clear()
        this.soundfont = null
        this.notifyItemRangeChanged(0, this.opus_manager.channels.size)
    }
}