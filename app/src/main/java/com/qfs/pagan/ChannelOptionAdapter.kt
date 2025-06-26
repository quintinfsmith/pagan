package com.qfs.pagan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.Activity.ActivityEditor


class ChannelOptionAdapter(
    private val _opus_manager: OpusLayerInterface,
    private val _recycler: RecyclerView
) : RecyclerView.Adapter<ChannelOptionAdapter.ChannelOptionViewHolder>() {
    class ChannelOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private var _channel_count = 0
    init {
        this._recycler.adapter = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    this@ChannelOptionAdapter.notifyItemRangeChanged(start, this@ChannelOptionAdapter.itemCount - start)
                }
                override fun onItemRangeChanged(start: Int, count: Int) { }
                override fun onItemRangeInserted(start: Int, count: Int) { }
            }
        )
        this.notify_soundfont_changed()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelOptionViewHolder {
        val wrapper: View = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.config_channel_item,
                parent,
                false
            )
        return ChannelOptionViewHolder(wrapper)
    }

    fun get_activity(): ActivityEditor {
        return this._recycler.context as ActivityEditor
    }

    private fun set_text(view: MaterialButton, position: Int) {
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        val curChannel = opus_manager.get_channel(position)

        val defaults = activity.resources.getStringArray(R.array.midi_instruments)
        val key = Pair(curChannel.get_midi_bank(), curChannel.midi_program)
        val supported_instruments = activity.get_supported_instrument_names()
        val label = supported_instruments[key] ?: if (this._opus_manager.is_percussion(position)) {
            "${curChannel.midi_program}"
        } else {
            activity.resources.getString(R.string.unknown_instrument, defaults[curChannel.midi_program])
        }

        view.text = if (this._opus_manager.is_percussion(position)) {
            activity.getString(R.string.label_choose_instrument_percussion, position, label)
        } else {
            activity.getString(R.string.label_choose_instrument, position, label)
        }.trim()
    }

    override fun onBindViewHolder(holder: ChannelOptionViewHolder, position: Int) {
        val wrapper = holder.itemView

        val option_button = wrapper.findViewById<MaterialButton>(R.id.btnLabel)
        val remove_button = wrapper.findViewById<MaterialButton>(R.id.btnClose)
        this.set_text(option_button, position)

        option_button.setOnClickListener {
            this.interact_btnChooseInstrument(holder.layoutPosition)
        }

        remove_button.setOnClickListener {
            this.interact_btnRemoveChannel(holder.layoutPosition)
        }
    }

    private fun interact_btnRemoveChannel(c: Int) {
        this.get_activity().get_action_interface().remove_channel(c)
    }

    private fun interact_btnChooseInstrument(c: Int) {
        this.get_activity().get_action_interface().set_channel_instrument(c)
    }

    override fun getItemCount(): Int {
        return this._channel_count
    }

    fun notify_soundfont_changed() {
        this.notifyItemRangeChanged(0, this._opus_manager.channels.size)
    }

    fun add_channel() {
        this._channel_count += 1
        this.notifyItemRangeChanged(0, this._channel_count)
    }

    fun remove_channel(channel: Int) {
        this.notifyItemRangeChanged(channel, this._channel_count)
        this._channel_count -= 1
    }

    fun clear() {
        this.notifyItemRangeChanged(0, this._channel_count)
        this._channel_count = 0
    }

    fun setup() {
        this._channel_count = this._opus_manager.channels.size
        this.notifyItemRangeChanged(0, this._channel_count)
    }
}
