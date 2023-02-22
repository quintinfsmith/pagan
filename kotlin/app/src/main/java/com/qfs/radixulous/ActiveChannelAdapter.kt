package com.qfs.radixulous

import android.content.Context
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.apres.SoundFont

class ChannelOptionAdapter(
    private val activity: MainActivity,
    private val recycler: RecyclerView,
    private val soundfont: SoundFont
) : RecyclerView.Adapter<ChannelOptionAdapter.ChannelOptionViewHolder>() {
    var supported_instruments: Set<Int>
    class ChannelOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    init {
        this.recycler.adapter = this
        this.recycler.layoutManager = LinearLayoutManager(this.activity)
        var that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    for (i in start until that.recycler.childCount) {
                        that.notifyItemChanged(i)
                    }
                }
                //override fun onChanged() { }
            }
        )

        var supported_instruments: MutableSet<Int> = mutableSetOf()
        for ((pair, preset) in this.soundfont.presets) {
            if (pair.first != 128) {
                supported_instruments.add(pair.second)
            }
        }
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

    fun addChannel() {
        var opus_manager = this.activity.getOpusManager()
        opus_manager.new_channel()
        opus_manager.new_line(opus_manager.channels.size - 1)
        this.call_tick()
        notifyItemInserted(opus_manager.channels.size - 1)
    }
    fun call_tick() {
        var fragment = this.activity.getActiveFragment()
        when (fragment) {
            is MainFragment -> {
                fragment.tick()
            }
            else -> {

            }
        }

    }

    fun set_text(view: View, position: Int) {
        var opus_manager = this.activity.getOpusManager()
        var channels = opus_manager.channels
        val curChannel = channels[position]

        val instrument = curChannel.midi_instrument
        var btnChooseInstrument: TextView = view.findViewById(R.id.btnChooseInstrument)
        var prefix = if (instrument == 0) {
            // TODO: I don't think this is 100% how percussion gets stored in a soundfont.
            //  but it'll work until I support imported soundfonts
            if (this.soundfont.presets[Pair(128,0)] != null) {
                ""
            } else {
                "\uD83D\uDD07"
            }
        } else {
            if (instrument - 1 in this.supported_instruments) {
                ""
            } else {
                "\uD83D\uDD07"
            }
        }
        btnChooseInstrument.text = if (instrument == 0) {
            "$position: $prefix Percussion"
        } else {
            "$position: $prefix ${view.resources.getStringArray(R.array.midi_instruments)[instrument - 1]}"
        }
    }

    override fun onBindViewHolder(holder: ChannelOptionViewHolder, position: Int) {
        this.set_text(holder.itemView as ViewGroup, position)
        var btnChooseInstrument: TextView = holder.itemView.findViewById(R.id.btnChooseInstrument)
        btnChooseInstrument.setOnClickListener {
            this.interact_btnChooseInstrument(holder.itemView.context, it, position)
        }

        var btnRemoveChannel: TextView = holder.itemView.findViewById(R.id.btnRemoveChannel)
        btnRemoveChannel.setOnClickListener {
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
        var opus_manager = this.activity.getOpusManager()
        var x = this.get_view_channel(view)
        opus_manager.remove_channel(x)
        this.call_tick()

        this.notifyItemRemoved(x)
    }


    private fun interact_btnChooseInstrument(context: Context, view: View, index: Int) {
        var opus_manager = this.activity.getOpusManager()
        var wrapper = ContextThemeWrapper(context, R.style.PopupMenu)
        val popupMenu = PopupMenu(wrapper, view)
        var channel = this.get_view_channel(view)

        val instruments = view.resources.getStringArray(R.array.midi_instruments)
        var x = 0
        if (opus_manager.percussion_channel == null) {
            popupMenu.menu.add(0, 0, 0, "0: Percussion")
            x += 1
        }

        instruments.forEachIndexed { i, string ->
            var display_name = if (i in this.supported_instruments) {
                "$string"
            } else {
                "\uD83D\uDD07 $string"
            }
            popupMenu.menu.add(0, i + 1, x, "${i + 1}: $display_name")
            x += 1
        }

        popupMenu.setOnMenuItemClickListener {
            if (it.itemId == 0) {
                this.set_percussion_channel(channel)
            } else {
                this.set_channel_instrument(channel, it.itemId)
            }
            this.notifyItemChanged(index)

            true
        }

        popupMenu.show()
    }

    private fun set_channel_instrument(channel: Int, instrument: Int) {
        if (instrument == 0) {
            this.set_percussion_channel(channel)
            return
        }

        var opus_manager = this.activity.getOpusManager()
        opus_manager.set_channel_instrument(channel, instrument)
    }

    private fun set_percussion_channel(channel: Int) {
        var opus_manager = this.activity.getOpusManager()
        if (opus_manager.percussion_channel != null) {
            this.set_channel_instrument(opus_manager.percussion_channel!!, 1)
        }
        opus_manager.set_percussion_channel(channel)
    }

    override fun getItemCount(): Int {
        return this.activity.getOpusManager().channels.size
    }
}


















