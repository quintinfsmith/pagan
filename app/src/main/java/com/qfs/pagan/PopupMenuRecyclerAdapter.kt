package com.qfs.pagan

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.qfs.pagan.Activity.PaganActivity

class PopupMenuRecyclerAdapter<T>(
    private val _recycler: RecyclerView,
    private var _options: List<Triple<T, Int?, String>>,
    private val _default: T? = null,
    private val event_handler: MenuDialogEventHandler<T>,
) : RecyclerView.Adapter<ViewHolder>() {
    class PopupMenuRecyclerViewHolder(itemView: View) : ViewHolder(itemView) {
        init {
            this.setIsRecyclable(false)
        }
    }

    init {
        this._recycler.adapter = this
        this._recycler.itemAnimator = null
        this._recycler.layoutManager = LinearLayoutManager(this._recycler.context)
        this.notifyItemRangeInserted(0, this._options.size)
    }

    fun set_items(new_items: List<Triple<T, Int?, String>>) {
        this._options = new_items
        this.notifyDataSetChanged()
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) { }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = (holder.itemView.context as PaganActivity)

        val text_view = holder.itemView.findViewById<TextView>(R.id.tvTextView)
        val icon_view = holder.itemView.findViewById<ImageView>(R.id.menu_item_icon)


        if (this._options[position].first == this._default) {
            val background_color = ContextCompat.getColor(activity, R.color.popup_menu_item_selected_bg)
            holder.itemView.setBackgroundColor(background_color)

            val foreground_color = ContextCompat.getColor(activity, R.color.popup_menu_item_selected_fg)
            text_view.setTextColor(foreground_color)
            icon_view.setColorFilter(foreground_color)
        } else {
            val background_color = ContextCompat.getColor(activity, R.color.surface_container)
            holder.itemView.setBackgroundColor(background_color)

            val foreground_color = ContextCompat.getColor(activity, R.color.on_surface_container)
            text_view.setTextColor(foreground_color)
            icon_view.setColorFilter(foreground_color)
        }


        text_view.text = this._options[position].third.trim()

        if (this._options[position].second == null) {
            icon_view.visibility = View.GONE
        } else {
            icon_view.visibility = View.VISIBLE
            icon_view.setImageResource(this._options[position].second!!)
        }

        holder.itemView.setOnClickListener {
            this.event_handler.do_submit(position, this._options[position].first)
        }
        holder.itemView.setOnLongClickListener {
            this.event_handler.on_long_click_item(position, this._options[position].first)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopupMenuRecyclerViewHolder {
        val viewInflated: View = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.dropdown_menu_item,
                parent,
                false
            )
        return PopupMenuRecyclerViewHolder(viewInflated)
    }

    override fun getItemCount(): Int {
        return this._options.size
    }

    fun get_default_position(): Int? {
        if (this._default == null) {
            return null
        }

        this._options.forEachIndexed { i: Int, (value, _, _): Triple<T, Int?, String> ->
            if (this._default == value) {
                return i
            }
        }

        return null
    }
}