package com.qfs.pagan.structure

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.MainActivity
import com.qfs.pagan.opusmanager.BaseLayer as OpusManager

class TuningMapRecycler(activity: MainActivity): RecyclerView(activity) {
    class TuningMapRecyclerAdapter(val recycler: TuningMapRecycler): RecyclerView.Adapter<TuningMapViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TuningMapViewHolder {
            val item_view = LinearLayout(parent.context)
            item_view.orientation = LinearLayout.HORIZONTAL
            return TuningMapViewHolder(item_view)
        }

        override fun onBindViewHolder(holder: TuningMapViewHolder, position: Int) {
            val wrapper = holder.itemView as ViewGroup
            wrapper.removeAllViews()
            val tuning_map = this.get_opus_manager().tuning_map

            val number_label_view = TextView(holder.itemView.context)
            number_label_view.text = "$position:"
            val numerator_view = EditText(holder.itemView.context)
            val slash_view = TextView(holder.itemView.context)
            slash_view.text = "/"

            val denominator_view = EditText(holder.itemView.context)
            numerator_view.setText(tuning_map[position].first.toString())
            denominator_view.setText(tuning_map[position].second.toString())
            wrapper.addView(number_label_view)
            wrapper.addView(numerator_view)
            wrapper.addView(slash_view)
            wrapper.addView(denominator_view)
        }

        fun get_opus_manager(): OpusManager {
            return (this.recycler.context as MainActivity).get_opus_manager()
        }

        override fun getItemCount(): Int {
            return this.get_opus_manager().tuning_map.size
        }
    }

    class TuningMapViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    init {
        this.adapter = TuningMapRecyclerAdapter(this)
        this.layoutManager = LinearLayoutManager(this.context)
    }
}