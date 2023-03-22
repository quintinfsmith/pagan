package com.qfs.radixulous

import android.content.Context
import android.view.*
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.qfs.radixulous.opusmanager.OpusChannel
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.structure.OpusTree

class RowLabelAdapter(var main_fragment: MainFragment, var recycler: RecyclerView) : RecyclerView.Adapter<RowLabelAdapter.RowLabelViewHolder>() {
    // BackLink so I can get the x offset from a view in the view holder
    private var row_count = 0
    private var _dragging_rowLabel: View? = null

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
        (this.recycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        val that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    that.refresh()
                }
                override fun onItemRangeInserted(start: Int, count: Int) {
                    that.refresh()
                }
            }
        )
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

        label.setOnTouchListener { view: View, touchEvent: MotionEvent ->
            if (this._dragging_rowLabel == null && touchEvent.action == MotionEvent.ACTION_MOVE) {
                this._dragging_rowLabel = view
                view.startDragAndDrop(
                    null,
                    View.DragShadowBuilder(view),
                    null,
                    0
                )
                return@setOnTouchListener true
            }
            false
        }

        label.setOnDragListener { view: View, dragEvent: DragEvent ->
            when (dragEvent.action) {
                //DragEvent.ACTION_DRAG_STARTED -> { }
                DragEvent.ACTION_DROP -> {
                    var from_label =  this._dragging_rowLabel
                    if (from_label != null && from_label != view) {
                        val y_from = (from_label.parent as ViewGroup).indexOfChild(from_label)
                        val y_to = (view.parent as ViewGroup).indexOfChild(view)
                        this.main_fragment.move_line(y_from, y_to)
                    }
                    this._dragging_rowLabel = null
                }
                else -> { }
            }
            true
        }

        return RowLabelViewHolder(label)
    }

    override fun onBindViewHolder(holder: RowLabelViewHolder, position: Int) {
        var label = this.main_fragment.get_label_text(position)
        (holder.itemView as LabelView).set_text(label)
    }

    override fun getItemCount(): Int {
        return this.row_count
    }

    fun scrollToY(y: Int) {
        var current_y = this.recycler.computeVerticalScrollOffset()
        this.recycler.scrollBy(0, y - current_y)
    }

    private fun interact_rowLabel(view: View) {
        this.main_fragment.set_active_row(this.get_y(view))
    }

    private fun get_y(view: View): Int {
        var abs_y: Int = 0
        val label_column = view.parent!! as ViewGroup
        for (i in 0 until label_column.childCount) {
            if (label_column.getChildAt(i) == view) {
                abs_y = i
                break
            }
        }
        return abs_y
    }

    fun refresh() {
        val start = (this.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val end = (this.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

        // NOTE: padding the start/end since an item may be bound but not visible
        for (i in Integer.max(0, start - 1)..Integer.min(this.itemCount, end + 1)) {
            this.notifyItemChanged(i)
        }
    }
}
