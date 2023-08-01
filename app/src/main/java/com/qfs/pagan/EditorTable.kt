package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TableLayout
import android.widget.TableRow
import androidx.core.view.children

class EditorTable(context: Context, attrs: AttributeSet): TableLayout(context, attrs) {
    val main_recycler = ColumnRecycler(this, context, attrs)
    val line_label_recycler = LineLabelRecyclerView(context, attrs)
    val column_label_recycler = ColumnLabelRecycler(context, attrs)
    val top_row = TableRow(context, attrs)
    val bottom_row = TableRow(context, attrs)
    val spacer = LinearLayout(context, attrs)
    val vertical_scroll_listener = VerticalScrollListener(this)
    val horizontal_scroll_listener = HorizontalScrollListener()
    init {
        this.top_row.addView(this.spacer)
        this.top_row.addView(this.column_label_recycler)

        this.bottom_row.addView(this.line_label_recycler)
        this.bottom_row.addView(this.main_recycler)

        this.addView(this.top_row)
        this.addView(this.bottom_row)

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()

        val column_label_adapter = ColumnLabelAdapter(this)
        column_label_adapter.notifyItemRangeInserted(0, opus_manager.opus_beat_count)

        val main_adapter = ColumnRecyclerAdapter(this)
        this.main_recycler.addOnScrollListener(this.horizontal_scroll_listener)
        main_adapter.notifyItemRangeInserted(0, opus_manager.opus_beat_count)

        val line_label_adapter = LineLabelRecyclerAdapter(this)
        line_label_adapter.notifyItemRangeInserted(0, opus_manager.get_total_line_count())
    }

    fun get_activity(): MainActivity {
        return this.context as MainActivity
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (this.top_row.layoutParams as LayoutParams).apply {
            width = MATCH_PARENT
            //height = resources.getDimension(R.dimen.line_height).toInt()
            height = WRAP_CONTENT
        }

        (this.bottom_row.layoutParams as LayoutParams).apply {
            width = MATCH_PARENT
        }

        this.spacer.layoutParams.apply {
            width = MATCH_PARENT
            height = MATCH_PARENT
        }

        this.line_label_recycler.layoutParams.apply {
            width = 100
            height = MATCH_PARENT
        }

        (this.main_recycler.layoutParams as TableRow.LayoutParams).apply {
            width = 0
            weight = 1F
        }

    }
}