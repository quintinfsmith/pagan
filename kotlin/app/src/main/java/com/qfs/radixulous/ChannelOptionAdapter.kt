package com.qfs.radixulous

import android.content.Context
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
                override fun onItemRangeChanged(start: Int, count: Int) {
                }
                override fun onItemRangeInserted(start: Int, count: Int) {
                }
                //override fun onChanged() { }
            }
        )
        var supported_instruments = this.soundfont.get_available_presets(0)
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
        notifyItemInserted(opus_manager.channels.size - 1)
        this.update_fragment()
    }


    fun set_text(view: View, position: Int) {
        var opus_manager = this.activity.getOpusManager()
        var channels = opus_manager.channels
        val curChannel = channels[position]
        var btnChooseInstrument: TextView = view.findViewById(R.id.btnChooseInstrument)
        var label = this.get_label(
            if (opus_manager.is_percussion(position)) {
                -1
            } else {
                curChannel.midi_instrument
            }
        )
        btnChooseInstrument.text = "$position: $label"
    }

    private fun get_label(instrument: Int): String {
        var instrument_array = this.activity.resources.getStringArray(R.array.midi_instruments)

        var prefix = if (instrument == -1) {
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

        if (opus_manager.channels.size > 1) {
            var x = this.get_view_channel(view)
            opus_manager.remove_channel(x)
            this.notifyItemRemoved(x)
            this.update_fragment()
        }
    }

    private fun interact_btnChooseInstrument(context: Context, view: View, index: Int) {
        var opus_manager = this.activity.getOpusManager()

        var wrapper = ContextThemeWrapper(this.activity, R.style.PopupMenu)
        val popupMenu = PopupMenu(wrapper, view)
        var channel = this.get_view_channel(view)

        for (i in 0 until 128) {
            if (i == 0 && opus_manager.percussion_channel != null) {
                continue
            }
            popupMenu.menu.add(0, i - 1, i, "$i: ${this.get_label(i - 1)}")
        }

        popupMenu.setOnMenuItemClickListener {
            this.set_channel_instrument(channel, it.itemId)
            this.notifyItemChanged(index)
            false
        }

        popupMenu.show()
    }

    private fun set_channel_instrument(channel: Int, instrument: Int) {
        if (instrument == -1) {
            this.set_percussion_channel(channel)
            return
        }

        var opus_manager = this.activity.getOpusManager()
        opus_manager.set_channel_instrument(channel, instrument)
        this.update_fragment()

    }

    private fun set_percussion_channel(channel: Int) {
        var opus_manager = this.activity.getOpusManager()
        opus_manager.set_percussion_channel(channel)
        this.update_fragment()
    }

    private fun update_fragment() {
        var fragment = this.activity.getActiveFragment()
        if (fragment is MainFragment) {
            fragment.tick()
            fragment.update_line_labels()
            fragment.refresh_leaf_labels()
        }

    }

    override fun getItemCount(): Int {
        return this.activity.getOpusManager().channels.size
    }
}