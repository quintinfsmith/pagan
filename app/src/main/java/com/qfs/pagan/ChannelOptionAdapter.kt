package com.qfs.pagan

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.qfs.apres.soundfont.SoundFont
import kotlin.math.roundToInt

class ChannelOptionAdapter(
    private val _opus_manager: InterfaceLayer,
    private val _recycler: RecyclerView
) : RecyclerView.Adapter<ChannelOptionAdapter.ChannelOptionViewHolder>() {
    class OutOfSyncException : Exception("Channel Option list out of sync with OpusManager")
    class ChannelOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class BackLinkView(context: Context): LinearLayout(context) {
        var view_holder: ChannelOptionViewHolder? = null
        init {
            this.background = AppCompatResources.getDrawable(context, R.drawable.button)
        }

        override fun drawableStateChanged() {
            super.drawableStateChanged()
            var context = this.context
            while (context !is MainActivity) {
                context = (context as ContextThemeWrapper).baseContext
            }

            val palette = context.view_model.palette!!
            val background = (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
            val stroke = (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_stroke)
            background.setTint(palette.button)
            stroke.setTint(palette.button_stroke)
        }
    }

    private var channel_count = 0
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
        val soundfont = this.get_activity().get_soundfont()
        if (soundfont != null) {
            for ((name, program, bank) in soundfont.get_available_presets()) {
                this._supported_instruments[Pair(bank, program)] = name
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelOptionViewHolder {
        val top_view = BackLinkView(ContextThemeWrapper(parent.context, R.style.recycler_option))
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

    private fun set_text(view: BackLinkView, position: Int) {
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()

        val curChannel = opus_manager.channels[position]

        val defaults = activity.resources.getStringArray(R.array.midi_instruments)
        val key = Pair(curChannel.midi_bank, curChannel.midi_program)
        val label = this._supported_instruments[key] ?: if (this._opus_manager.is_percussion(position)) {
            activity.resources.getString(R.string.unknown_percussion)
        } else {
            activity.resources.getString(R.string.unknown_instrument, defaults[curChannel.midi_program])
        }

        (view.getChildAt(0) as TextView).text = if (!this._opus_manager.is_percussion(position)) {
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
            remove_button.text = holder.itemView.context.resources.getString(R.string.percussion_label)
            remove_button.setOnClickListener {
                this.interact_btnRemoveChannel(holder.itemView as BackLinkView)
            }
        }
    }

    private fun get_percussion_visibility_button_text(): String {
        val main = this.get_activity()
        return if (main.configuration.show_percussion) {
            main.getString(R.string.btn_percussion_visible)
        } else {
            main.getString(R.string.btn_percussion_hidden)
        }
    }

    private fun interact_btnTogglePercussionVisibility(view: BackLinkView) {
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
        val remove_button = (view as ViewGroup).getChildAt(1) as TextView
        remove_button.text = this.get_percussion_visibility_button_text()
        val editor_table = main.findViewById<EditorTable>(R.id.etEditorTable)
        editor_table.update_percussion_visibility()
    }

    private fun interact_btnRemoveChannel(view: BackLinkView) {
        if (this._opus_manager.channels.size > 1) {
            if (this._opus_manager.channels.size == 2 && !this.get_activity().configuration.show_percussion) {
                this.get_activity().configuration.show_percussion = true
                this.get_activity().save_configuration()
                val editor_table = this.get_activity().findViewById<EditorTable>(R.id.etEditorTable)

                editor_table.update_percussion_visibility()
            }

            val x = view.view_holder?.bindingAdapterPosition ?: return
            this._opus_manager.remove_channel(x)
        }
    }

    private fun interact_btnChooseInstrument(view: BackLinkView) {
        val channel = view.view_holder?.bindingAdapterPosition ?: return

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
        return this.channel_count
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

    fun add_channel() {
        this.channel_count += 1
        this.notifyDataSetChanged()
    }

    fun remove_channel(channel: Int) {
        this.channel_count -= 1
        this.notifyItemRemoved(channel)
    }

    fun clear() {
        this.channel_count = 0
        this.notifyDataSetChanged()
    }
    fun setup() {
        this.channel_count = this._opus_manager.channels.size
        this.notifyDataSetChanged()
    }
}