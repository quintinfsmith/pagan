package com.qfs.pagan

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TuningMapRecyclerAdapter(val recycler: TuningMapRecycler, var tuning_map: Array<Pair<Int, Int>>): RecyclerView.Adapter<TuningMapRecycler.TuningMapViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TuningMapRecycler.TuningMapViewHolder {
        val item_view = LinearLayout(parent.context)
        item_view.orientation = LinearLayout.HORIZONTAL
        return TuningMapRecycler.TuningMapViewHolder(item_view)
    }

    override fun onViewAttachedToWindow(holder: TuningMapRecycler.TuningMapViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.layoutParams.width = MATCH_PARENT
    }
    override fun onBindViewHolder(holder: TuningMapRecycler.TuningMapViewHolder, position: Int) {
        val wrapper = holder.itemView as ViewGroup
        wrapper.removeAllViews()
        val pair = this.tuning_map[position]
        val number_label_view = TextView(holder.itemView.context)
        number_label_view.text = "$position:"

        val numerator_view = RangedNumberInput(holder.itemView.context)
        numerator_view.set_range(0, 99999)
        numerator_view.set_value(pair.first)
        numerator_view.confirm_required = false

        val slash_view = TextView(holder.itemView.context)
        slash_view.text = "/"

        val denominator_view = RangedNumberInput(holder.itemView.context)
        denominator_view.set_range(2, 99999)
        denominator_view.set_value(pair.second)
        denominator_view.confirm_required = false

        wrapper.addView(number_label_view)
        number_label_view.layoutParams.width = 0
        (number_label_view.layoutParams as LinearLayout.LayoutParams).weight = 1f

        wrapper.addView(numerator_view)
        wrapper.addView(slash_view)
        wrapper.addView(denominator_view)

        numerator_view.value_set_callback = {
            var real_position = holder.bindingAdapterPosition
            val value = it.get_value()
            if (value != null && real_position > -1) {
                val pair = this@TuningMapRecyclerAdapter.tuning_map[real_position]
                this@TuningMapRecyclerAdapter.tuning_map[real_position] = Pair(
                    value,
                    pair.second
                )
            }
        }

        denominator_view.value_set_callback = {
            var real_position = holder.bindingAdapterPosition
            val value = it.get_value()
            if (value != null && real_position > -1) {
                val pair = this@TuningMapRecyclerAdapter.tuning_map[real_position]
                this@TuningMapRecyclerAdapter.tuning_map[real_position] = Pair(
                    pair.first,
                    value
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return this.tuning_map.size
    }
}
