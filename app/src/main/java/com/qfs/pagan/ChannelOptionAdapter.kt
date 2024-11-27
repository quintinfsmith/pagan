package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
            this.gravity = Gravity.CENTER_VERTICAL
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
        val color_map = context.view_model.color_map

        val top_view = BackLinkView(ContextThemeWrapper(parent.context, R.style.recycler_option))
        val btn_choose_instrument = TextView(ContextThemeWrapper(parent.context, R.style.recycler_option_instrument))
        val btn_kill_channel = ImageView(ContextThemeWrapper(parent.context, R.style.recycler_option_x))
        top_view.addView(btn_choose_instrument)
        top_view.addView(btn_kill_channel)
        btn_choose_instrument.setTextColor(color_map[Palette.ButtonText])
        btn_kill_channel.imageTintList = ColorStateList(
            arrayOf(intArrayOf()),
            intArrayOf(color_map[Palette.ButtonText])
        )

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
        val supported_instruments = activity.get_supported_instrument_names()
        val label = supported_instruments[key] ?: if (this._opus_manager.is_percussion(position)) {
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

        val remove_button = (holder.itemView as ViewGroup).getChildAt(1) as ImageView
        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()
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
        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()
        try {
            if (!opus_manager.percussion_channel.visible || opus_manager.channels.isNotEmpty()) {
                opus_manager.toggle_channel_visibility(opus_manager.channels.size)
            } else {
                return
            }
        } catch (e: OpusLayerInterface.HidingNonEmptyPercussionException) {
            return
        } catch (e: OpusLayerInterface.HidingLastChannelException) {
            return
        }

        val remove_button = (view as ViewGroup).getChildAt(1) as ImageView
        remove_button.setImageResource(
            if (opus_manager.percussion_channel.visible) {
                R.drawable.show_percussion
            } else {
                R.drawable.hide_percussion
            }
        )
    }

    private fun interact_btnRemoveChannel(view: BackLinkView) {
        val x = view.view_holder?.bindingAdapterPosition ?: return
        this._opus_manager.remove_channel(x)
    }

    private fun interact_btnChooseInstrument(view: BackLinkView) {
        val channel = view.view_holder?.bindingAdapterPosition ?: return
        this.get_activity().dialog_set_channel_instrument(channel)
    }

    override fun getItemCount(): Int {
        return this._channel_count
    }

    fun notify_soundfont_changed() {
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
