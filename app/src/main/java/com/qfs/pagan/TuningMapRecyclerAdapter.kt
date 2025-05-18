package com.qfs.pagan

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class TuningMapRecyclerAdapter(var tuning_map: Array<Pair<Int, Int>>): RecyclerView.Adapter<TuningMapRecycler.TuningMapViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TuningMapRecycler.TuningMapViewHolder {
        val item_view = LinearLayout(ContextThemeWrapper(parent.context, R.style.tuning_map_item))
        item_view.orientation = LinearLayout.HORIZONTAL
        return TuningMapRecycler.TuningMapViewHolder(item_view)
    }

    override fun onViewAttachedToWindow(holder: TuningMapRecycler.TuningMapViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.layoutParams.width = MATCH_PARENT
        (holder.itemView.layoutParams as MarginLayoutParams).setMargins(0, holder.itemView.context.resources.getDimension(R.dimen.normal_padding).roundToInt(),0,0)

    }
    override fun onBindViewHolder(holder: TuningMapRecycler.TuningMapViewHolder, position: Int) {
        val use_context = (holder.itemView.context as ContextThemeWrapper).baseContext

        val wrapper = holder.itemView as ViewGroup
        wrapper.removeAllViews()

        val pair = this.tuning_map[position]
        val number_label_view = TextView(ContextThemeWrapper(use_context, R.style.tuning_map_item_label))
        number_label_view.text = use_context.getString(R.string.label_tuning_index, position)

        val numerator_view = RangedIntegerInput(use_context)
        numerator_view.set_range(0, 99999)
        numerator_view.set_value(pair.first)
        numerator_view.confirm_required = false
        numerator_view.minEms = 2

        val slash_view = TextView(use_context)
        slash_view.text = "/"

        val denominator_view = RangedIntegerInput(use_context)
        denominator_view.set_range(2, 99999)
        denominator_view.set_value(pair.second)
        denominator_view.confirm_required = false
        denominator_view.minEms = 2

        wrapper.addView(number_label_view)
        wrapper.addView(numerator_view)
        wrapper.addView(slash_view)
        wrapper.addView(denominator_view)

        numerator_view.value_set_callback = { value: Int? ->
            val real_position = holder.bindingAdapterPosition
            if (value != null && real_position > -1) {
                val working_pair = this@TuningMapRecyclerAdapter.tuning_map[real_position]
                this@TuningMapRecyclerAdapter.tuning_map[real_position] = Pair(
                    value,
                    working_pair.second
                )
            }
        }

        denominator_view.value_set_callback = { value: Int? ->
            val real_position = holder.bindingAdapterPosition
            if (value != null && real_position > -1) {
                val working_pair = this@TuningMapRecyclerAdapter.tuning_map[real_position]
                this@TuningMapRecyclerAdapter.tuning_map[real_position] = Pair(
                    working_pair.first,
                    value
                )
            }
        }

    }

    override fun getItemCount(): Int {
        return this.tuning_map.size
    }
}
