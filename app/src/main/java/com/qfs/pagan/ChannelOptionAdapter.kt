package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class ChannelOptionAdapter(
    private val _opus_manager: OpusLayerInterface,
    private val _recycler: RecyclerView
) : RecyclerView.Adapter<ChannelOptionAdapter.ChannelOptionViewHolder>() {
    class ChannelOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class BackLinkView(context: Context): LinearLayout(ContextThemeWrapper(context, R.style.song_config_button)) {
        var view_holder: ChannelOptionViewHolder? = null
        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            this.layoutParams.width = MATCH_PARENT
            (this.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
                0,
                0,
                0,
                this.context.resources.getDimension(R.dimen.config_item_padding).roundToInt()
            )
        }
    }

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
        var context = parent.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }

        val top_view = BackLinkView(ContextThemeWrapper(parent.context, R.style.recycler_option))
        val btn_choose_instrument = TextView(ContextThemeWrapper(parent.context, R.style.recycler_option_instrument))
        val btn_kill_channel = ImageView(ContextThemeWrapper(parent.context, R.style.recycler_option_x))
        top_view.addView(btn_choose_instrument)
        top_view.addView(btn_kill_channel)

        btn_choose_instrument.layoutParams.width = 0
        (btn_choose_instrument.layoutParams as LinearLayout.LayoutParams).weight = 1F
        (btn_choose_instrument.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START

        // Kludge. A Recent round of linting exposed an odd bug causing buttons to not actually
        // match the parent width when at least one of the buttons has a long label
        btn_choose_instrument.setMaxWidth(top_view.measuredWidth - top_view.paddingEnd - top_view.paddingStart - btn_kill_channel.measuredWidth)

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
        val supported_instruments = activity.get_supported_instrument_names()
        val label = supported_instruments[key] ?: if (this._opus_manager.is_percussion(position)) {
            "${curChannel.midi_program}"
        } else {
            activity.resources.getString(R.string.unknown_instrument, defaults[curChannel.midi_program])
        }
        val label_view = (view.getChildAt(0) as TextView)
        label_view.text = if (!this._opus_manager.is_percussion(position)) {
            activity.getString(R.string.label_choose_instrument, position, label)
        } else {
            activity.getString(R.string.label_choose_instrument_percussion, label)
        }.trim()
    }

    override fun onBindViewHolder(holder: ChannelOptionViewHolder, position: Int) {
        (holder.itemView as BackLinkView).view_holder = holder
        this.set_text(holder.itemView as BackLinkView, position)

        (holder.itemView as ViewGroup).getChildAt(0).setOnClickListener {
            this.interact_btnChooseInstrument(holder.itemView as BackLinkView)
        }

        val remove_button = (holder.itemView as ViewGroup).getChildAt(1) as ImageView
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()

        if (this._opus_manager.is_percussion(position)) {
            remove_button.setImageResource(
                if (opus_manager.percussion_channel.visible) {
                    R.drawable.show_percussion
                } else {
                    R.drawable.hide_percussion
                }
            )
            remove_button.setOnClickListener {
                this.interact_btnTogglePercussionVisibility(holder.itemView as BackLinkView)
            }
        } else {
            remove_button.setImageResource(R.drawable.delete_channel)
            remove_button.setOnClickListener {
                this.interact_btnRemoveChannel(holder.itemView as BackLinkView)
            }
        }
    }

    private fun interact_btnTogglePercussionVisibility(view: BackLinkView) {
        this.get_activity().get_action_interface().toggle_percussion_visibility()
    }

    private fun interact_btnRemoveChannel(view: BackLinkView) {
        val x = view.view_holder?.bindingAdapterPosition ?: return
        this.get_activity().get_action_interface().remove_channel(x)
    }

    private fun interact_btnChooseInstrument(view: BackLinkView) {
        val channel = view.view_holder?.bindingAdapterPosition ?: return
        this.get_activity().get_action_interface().set_channel_instrument(channel)
    }

    override fun getItemCount(): Int {
        return this._channel_count
    }

    fun notify_soundfont_changed() {
        this.notifyItemRangeChanged(0, this._opus_manager.channels.size + 1)
    }

    fun add_channel() {
        this._channel_count += 1
        this.notifyItemRangeChanged(0, this._channel_count)
    }

    fun remove_channel(channel: Int) {
        this._channel_count -= 1
        this.notifyItemRemoved(channel)
    }

    fun clear() {
        this._channel_count = 0
        this.notifyItemRangeChanged(0, this._channel_count)
    }

    fun setup() {
        this._channel_count = this._opus_manager.channels.size + 1
        this.notifyItemRangeChanged(0, this._channel_count)
    }
}
