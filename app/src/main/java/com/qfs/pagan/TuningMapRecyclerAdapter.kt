package com.qfs.pagan

import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TuningMapRecyclerAdapter(val recycler: TuningMapRecycler, var tuning_map: Array<Pair<Int, Int>>): RecyclerView.Adapter<TuningMapRecycler.TuningMapViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TuningMapRecycler.TuningMapViewHolder {
        val item_view = LinearLayout(parent.context)
        item_view.orientation = LinearLayout.HORIZONTAL
        return TuningMapRecycler.TuningMapViewHolder(item_view)
    }

    override fun onBindViewHolder(holder: TuningMapRecycler.TuningMapViewHolder, position: Int) {
        val wrapper = holder.itemView as ViewGroup
        wrapper.removeAllViews()

        val number_label_view = TextView(holder.itemView.context)
        number_label_view.text = "$position:"
        val numerator_view = EditText(holder.itemView.context)
        val slash_view = TextView(holder.itemView.context)
        slash_view.text = "/"

        val denominator_view = EditText(holder.itemView.context)
        numerator_view.setText(this.tuning_map[position].first.toString())
        denominator_view.setText(this.tuning_map[position].second.toString())
        wrapper.addView(number_label_view)
        wrapper.addView(numerator_view)
        wrapper.addView(slash_view)
        wrapper.addView(denominator_view)

        numerator_view.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                this@TuningMapRecyclerAdapter.tuning_map[position] = Pair(p0.toString().toInt(), denominator_view.text.toString().toInt())
            }
            override fun afterTextChanged(p0: Editable?) { }
        })

        denominator_view.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                this@TuningMapRecyclerAdapter.tuning_map[position] = Pair(numerator_view.text.toString().toInt(), p0.toString().toInt())
            }
            override fun afterTextChanged(p0: Editable?) { }
        })

    }

    override fun getItemCount(): Int {
        return this.tuning_map.size
    }
}
