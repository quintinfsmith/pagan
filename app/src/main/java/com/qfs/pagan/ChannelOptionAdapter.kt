package com.qfs.pagan

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qfs.apres.soundfont.SoundFont
import com.qfs.pagan.ColorMap.Palette
import kotlin.math.roundToInt

class ChannelOptionAdapter(
    private val _opus_manager: OpusLayerInterface,
    private val _recycler: RecyclerView
) : RecyclerView.Adapter<ChannelOptionAdapter.ChannelOptionViewHolder>() {
    class ChannelOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class BackLinkView(context: Context): LinearLayout(ContextThemeWrapper(context, R.style.song_config_button)) {
        var view_holder: ChannelOptionViewHolder? = null
        init {
            var working_context = this.context
            while (working_context !is MainActivity) {
                working_context = (working_context as ContextThemeWrapper).baseContext
            }

            val color_map = working_context.view_model.color_map
            for (i in 0 until (this.background as StateListDrawable).stateCount) {
                val background = ((this.background as StateListDrawable).getStateDrawable(i) as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
                background?.setTint(color_map[Palette.Button])
            }
        }
    }

    private var _channel_count = 0
    private var _supported_instruments = HashMap<Pair<Int, Int>, String>()
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
        if (this.get_activity().is_connected_to_physical_device()) {
            this.set_soundfont(null)
        } else {
            val soundfont = this.get_activity().get_soundfont()
            this.set_soundfont(soundfont)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelOptionViewHolder {
        var context = parent.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }
        val color_map = context.view_model.color_map

        val top_view = BackLinkView(ContextThemeWrapper(parent.context, R.style.recycler_option))
        val btn_choose_instrument = TextView(ContextThemeWrapper(parent.context, R.style.recycler_option_instrument))
        val btn_kill_channel = TextView(ContextThemeWrapper(parent.context, R.style.recycler_option_x))
        top_view.addView(btn_choose_instrument)
        top_view.addView(btn_kill_channel)
        btn_choose_instrument.setTextColor(color_map[Palette.ButtonText])
        btn_kill_channel.setTextColor(color_map[Palette.ButtonText])

        btn_choose_instrument.layoutParams.width = 0
        (btn_choose_instrument.layoutParams as LinearLayout.LayoutParams).weight = 1F
        (btn_choose_instrument.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START

        return ChannelOptionViewHolder(top_view)
    }

    fun get_activity(): MainActivity {
        return this._recycler.context as MainActivity
    }

    private fun set_text(view: BackLinkView, position: Int) {
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()

        val curChannel = opus_manager.get_channel(position)

        val defaults = activity.resources.getStringArray(R.array.midi_instruments)
        val key = Pair(curChannel.get_midi_bank(), curChannel.midi_program)
        val label = this._supported_instruments[key] ?: if (this._opus_manager.is_percussion(position)) {
            "${curChannel.midi_program}"
        } else {
            activity.resources.getString(R.string.unknown_instrument, defaults[curChannel.midi_program])
        }

        (view.getChildAt(0) as TextView).text = if (!this._opus_manager.is_percussion(position)) {
            activity.getString(R.string.label_choose_instrument, position, label)
        } else {
            activity.getString(R.string.label_choose_instrument_percussion, label)
        }.trim()
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
        (holder.itemView as BackLinkView).view_holder = holder
        this.set_text(holder.itemView as BackLinkView, position)

        (holder.itemView as ViewGroup).getChildAt(0).setOnClickListener {
            this.interact_btnChooseInstrument(holder.itemView as BackLinkView)
        }

        val remove_button = (holder.itemView as ViewGroup).getChildAt(1) as TextView

        if (this._opus_manager.is_percussion(position)) {
            remove_button.text = this.get_percussion_visibility_button_text()
            remove_button.setOnClickListener {
                this.interact_btnTogglePercussionVisibility(holder.itemView as BackLinkView)
            }
        } else {
            remove_button.text = holder.itemView.context.resources.getString(R.string.remove_channel)
            remove_button.setOnClickListener {
                this.interact_btnRemoveChannel(holder.itemView as BackLinkView)
            }
        }
    }

    private fun get_percussion_visibility_button_text(): String {
        val main = this.get_activity()
        return if (main.view_model.show_percussion) {
            main.getString(R.string.btn_percussion_visible)
        } else {
            main.getString(R.string.btn_percussion_hidden)
        }
    }

    private fun interact_btnTogglePercussionVisibility(view: BackLinkView) {
        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()
        try {
            opus_manager.toggle_percussion_visibility()
        } catch (e: OpusLayerInterface.HidingNonEmptyPercussionException) {
            return
        } catch (e: OpusLayerInterface.HidingLastChannelException) {
            return
        }

        val remove_button = (view as ViewGroup).getChildAt(1) as TextView
        remove_button.text = this.get_percussion_visibility_button_text()
    }

    private fun interact_btnRemoveChannel(view: BackLinkView) {
        val x = view.view_holder?.bindingAdapterPosition ?: return
        this._opus_manager.remove_channel(x)
    }

    private fun interact_btnChooseInstrument(view: BackLinkView) {
        val channel = view.view_holder?.bindingAdapterPosition ?: return

        val sorted_keys = this._supported_instruments.keys.toList().sortedBy {
            it.first + (it.second * 128)
        }

        val opus_manager = this._opus_manager
        val is_percussion = opus_manager.is_percussion(channel)
        val default_position = opus_manager.get_channel_instrument(channel)

        val options = mutableListOf<Pair<Pair<Int, Int>, String>>()
        var current_instrument_supported = sorted_keys.contains(default_position)

        for (key in sorted_keys) {
            val name = this._supported_instruments[key]
            if (is_percussion && key.first == 128) {
                options.add(Pair(key, "[${key.second}] $name"))
            } else if (key.first != 128 && !is_percussion) {
                val pairstring = "${key.first}/${key.second}"
                options.add(Pair(key, "[$pairstring] $name"))
            }
        }

        val activity = this.get_activity()
        if (is_percussion) {
            val use_menu_dialog = options.isNotEmpty() && (!current_instrument_supported || options.size > 1)

            if (use_menu_dialog) {
                activity.dialog_popup_menu(activity.getString(R.string.dropdown_choose_instrument), options, default = default_position) { _: Int, (bank, program): Pair<Int, Int> ->
                    this.set_channel_instrument(channel, bank, program)
                }
            } else {
                activity.dialog_number_input(activity.getString(R.string.dropdown_choose_instrument), 0, 127, default_position.second) { program: Int ->
                    this.set_channel_instrument(channel, 1, program)
                }
            }
        } else if (options.size > 1 || !current_instrument_supported) {
            activity.dialog_popup_menu(activity.getString(R.string.dropdown_choose_instrument), options, default = default_position) { _: Int, (bank, program): Pair<Int, Int> ->
                this.set_channel_instrument(channel, bank, program)
            }
        }
    }

    private fun set_channel_instrument(channel: Int, bank: Int, program: Int) {
        this._opus_manager.set_channel_instrument(channel, Pair(bank, program))
    }

    override fun getItemCount(): Int {
        return this._channel_count
    }

    fun set_soundfont(soundfont: SoundFont?) {
        this._supported_instruments.clear()

        if (soundfont != null) {
            for ((name, program, bank) in soundfont.get_available_presets()) {
                this._supported_instruments[Pair(bank, program)] = name
            }
        } else {
            var program = 0
            for (name in this.get_activity().resources.getStringArray(R.array.midi_instruments)) {
                this._supported_instruments[Pair(0, program++)] = name
            }
        }

        this.notifyItemRangeChanged(0, this._opus_manager.channels.size + 1)
    }

    fun add_channel() {
        this._channel_count += 1
        this.notifyDataSetChanged()
    }

    fun remove_channel(channel: Int) {
        this._channel_count -= 1
        this.notifyItemRemoved(channel)
    }

    fun clear() {
        this._channel_count = 0
        this.notifyDataSetChanged()
    }

    fun setup() {
        this._channel_count = this._opus_manager.channels.size + 1
        this.notifyDataSetChanged()
    }
}
