package com.qfs.radixulous

import android.content.Context
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RowLabelAdapter(var main_fragment: MainFragment, var recycler: RecyclerView) : RecyclerView.Adapter<RowLabelAdapter.RowLabelViewHolder>() {
    // BackLink so I can get the x offset from a view in the view holder
    var row_count = 0
    class LabelView(context: Context): LinearLayout(context) {
        var viewHolder: RowLabelViewHolder? = null

        var textView: TextView = LayoutInflater.from(this.context).inflate(
            R.layout.table_line_label,
            this,
            false
        ) as TextView

        init {
            this.addView(textView)
        }

        fun set_text(text: String) {
            this.textView.text = text
        }
    }

    class RowLabelViewHolder(itemView: LabelView) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.viewHolder = this
        }
    }

    init {
        this.recycler.adapter = this
        this.recycler.layoutManager = LinearLayoutManager(this.recycler.context)
    }

    fun addRowLabel() {
        this.row_count += 1
        this.notifyItemInserted(this.row_count - 1)
    }

    fun removeRowLabel(i: Int) {
        if (this.row_count > 0) {
            this.row_count -= 1
            this.notifyItemRemoved(i)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowLabelViewHolder {
        val label = LabelView(parent.context)

        label.setOnClickListener {
             this.interact_rowLabel(it)
        }

        label.setOnFocusChangeListener { view, is_focused: Boolean ->
            if (is_focused) {
                this.interact_rowLabel(view)
            }
        }

        return RowLabelViewHolder(label)
    }

    override fun onBindViewHolder(holder: RowLabelViewHolder, position: Int) {
        var item_view = holder.itemView as LabelView
    }

    fun set_label_text() {
        if (!opus_manager.is_percussion(channel)) {
            if (line_offset == 0) {
                rowLabelText.text = "$channel:$line_offset"
            } else {
                rowLabelText.text = "  :$line_offset"
            }
        } else {
            val instrument = opus_manager.get_percussion_instrument(line_offset)
            rowLabelText.text = "P:$instrument"
        }
    }

    fun set_label_width(beat: Int, width: Int) {
        this.column_widths[beat] = width
        this.notifyItemChanged(beat)
    }

    override fun getItemCount(): Int {
        return this.column_widths.size
    }

    fun scrollToX(x: Int) {
        var current_x = this.recycler.computeHorizontalScrollOffset()
        this.recycler.scrollBy(x - current_x, 0)
    }

    private fun interact_rowLabel(view: View) {
        var abs_y: Int = 0
        val label_column = view.parent!! as ViewGroup
        for (i in 0 until label_column.childCount) {
            if (label_column.getChildAt(i) == view) {
                abs_y = i
                break
            }
        }

        this.main_fragment.set_active_row(abs_y)
    }

    // private fun buildLineView(channel: Int, line_offset: Int): TableRow {
    //     val main = this.getMain()
    //     val opus_manager = main.getOpusManager()

    //     val tlOpusLines: TableLayout = this.activity!!.findViewById(R.id.tlOpusLines)
    //     val llLineLabels: LinearLayout = this.activity!!.findViewById(R.id.llLineLabels)

    //     val rowView: TableRow = LayoutInflater.from(tlOpusLines.context).inflate(
    //         R.layout.table_row,
    //         tlOpusLines,
    //         false
    //     ) as TableRow

    //     var y = opus_manager.get_y(channel, line_offset)
    //     tlOpusLines.addView(rowView, y)
    //     this.cache.cacheLine(rowView, channel, line_offset)

    //     for (i in 0 until opus_manager.opus_beat_count) {
    //         val wrapper = LayoutInflater.from(rowView.context).inflate(
    //             R.layout.beat_node,
    //             rowView,
    //             false
    //         )
    //         rowView.addView(wrapper)
    //     }

    //     /////////////////////////////////

    //     val rowLabel = LayoutInflater.from(llLineLabels.context).inflate(
    //         R.layout.table_line_label,
    //         llLineLabels,
    //         false
    //     ) as LinearLayout

    //     val rowLabelText = rowLabel.getChildAt(0) as TextView



    //     llLineLabels.addView(rowLabel)

    //     return rowView
    // }
}
