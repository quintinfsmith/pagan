package com.qfs.pagan

import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qfs.apres.soundfont.SoundFont
import kotlin.math.roundToInt

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
        var top_view = LinearLayout(ContextThemeWrapper(parent.context, R.style.recycler_option))
        val btn_choose_instrument = TextView(ContextThemeWrapper(parent.context, R.style.recycler_option_instrument))
        val btn_kill_channel = TextView(ContextThemeWrapper(parent.context, R.style.recycler_option_x))
        top_view.addView(btn_choose_instrument)
        top_view.addView(btn_kill_channel)

        btn_choose_instrument.layoutParams.width = 0
        (btn_choose_instrument.layoutParams as LinearLayout.LayoutParams).weight = 1F
        (btn_choose_instrument.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START


        return ChannelOptionViewHolder(top_view)
    }

    fun get_activity(): MainActivity {
        return this._recycler.context as MainActivity
    }

    private fun set_text(view: ViewGroup, position: Int) {
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()

        val (channel_index, _) = try {
            opus_manager.get_std_offset(position)
        } catch (e: IndexOutOfBoundsException) {
            // Not attached
            return
        }

        val curChannel = opus_manager.channels[channel_index]

        val defaults = activity.resources.getStringArray(R.array.midi_instruments)
        val key = Pair(curChannel.midi_bank, curChannel.midi_program)

        val label = this._supported_instruments[key] ?: if (curChannel.midi_channel == 9) {
            activity.resources.getString(R.string.unknown_percussion)
        } else {
            activity.resources.getString(R.string.unknown_instrument, defaults[curChannel.midi_program])
        }

        (view.getChildAt(0) as TextView).text = if (curChannel.midi_channel != 9) {
            activity.getString(R.string.label_choose_instrument, position, label)
        } else {
            activity.getString(R.string.label_choose_instrument_percussion, label)
        }
    }

    override fun onViewAttachedToWindow(holder: ChannelOptionViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.layoutParams.width = MATCH_PARENT
        (holder.itemView.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
            0,
            0,
            0,
            holder.itemView.context.resources.getDimension(R.dimen.config_item_padding).roundToInt()
        )
    }

    override fun onBindViewHolder(holder: ChannelOptionViewHolder, position: Int) {
        this.set_text(holder.itemView as ViewGroup, position)

        (holder.itemView as ViewGroup).getChildAt(0).setOnClickListener {
            this.interact_btnChooseInstrument(it)
        }

        val remove_button = (holder.itemView as ViewGroup).getChildAt(1) as TextView
        if (this._opus_manager.is_percussion(position)) {
            remove_button.text = this.get_percussion_visibility_button_text()
            remove_button.setOnClickListener {
                this.interact_btnTogglePercussionVisibility(remove_button)
            }
        } else {
            remove_button.text = holder.itemView.context.resources.getString(R.string.percussion_label)
            remove_button.setOnClickListener {
                this.interact_btnRemoveChannel(it)
            }
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

    private fun get_percussion_visibility_button_text(): String {
        val main = this.get_activity()
        return if (main.configuration.show_percussion) {
            "\u2611"
        } else {
            "\u2610"
        }
    }

    private fun interact_btnTogglePercussionVisibility(view: TextView) {
        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()
        if (main.configuration.show_percussion) {
            if (!opus_manager.has_percussion() && opus_manager.channels.size > 1) {
                main.configuration.show_percussion = false
                opus_manager.cursor_clear()
            } else {
                return
            }
        } else {
            main.configuration.show_percussion = true
        }

        main.save_configuration()
        view.text = this.get_percussion_visibility_button_text()
        val editor_table = main.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.update_percussion_visibility()
    }

    private fun interact_btnRemoveChannel(view: View) {
        if (this._opus_manager.channels.size > 1) {
            if (this._opus_manager.channels.size == 2 && !this.get_activity().configuration.show_percussion) {
                this.get_activity().configuration.show_percussion = true
                this.get_activity().save_configuration()
                val editor_table = this.get_activity().findViewById<EditorTable>(R.id.etEditorTable)

                editor_table.update_percussion_visibility()
            }
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
        this.get_activity().dialog_popup_menu<Pair<Int, Int>>(this.get_activity().getString(R.string.dropdown_choose_instrument), options, default = default_position) { _: Int, (bank, program): Pair<Int, Int> ->
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