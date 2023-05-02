package com.qfs.radixulous

import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

class LineLabelRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    // Prevents this from intercepting linelabel touch events (disables manual scrolling)
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return false
    }
}

class LineLabelAdapter(var main_fragment: MainFragment, var recycler: RecyclerView) : RecyclerView.Adapter<LineLabelAdapter.LineLabelViewHolder>() {
    // BackLink so I can get the x offset from a view in the view holder
    private var row_count = 0
    private var _dragging_lineLabel: View? = null

    class LabelView(context: Context): LinearLayout(ContextThemeWrapper(context, R.style.line_label)) {
        var viewHolder: LineLabelViewHolder? = null

        var textView = TextView(this.context)

        init {
            this.addView(textView)
        }

        override fun onAttachedToWindow() {
            val margin = resources.getDimension(R.dimen.normal_padding).toInt()
            (this.layoutParams as MarginLayoutParams).setMargins(0,margin,0,margin)
            this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
        }

        // Prevents the child labels from blocking the parent onTouchListener events
        override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
            return true
        }

        fun set_text(text: String) {
            this.textView.text = text
        }
    }

    class LineLabelViewHolder(itemView: LabelView) : RecyclerView.ViewHolder(itemView) {
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

    fun addLineLabel() {
        this.row_count += 1
        this.notifyItemInserted(this.row_count - 1)
    }

    fun removeLineLabel(i: Int) {
        if (this.row_count > 0) {
            this.row_count -= 1
            this.notifyItemRemoved(i)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineLabelViewHolder {
        val label = LabelView(parent.context)

        label.setOnClickListener {
             this.interact_lineLabel(it)
        }

        label.setOnFocusChangeListener { view, is_focused: Boolean ->
            if (is_focused) {
                this.interact_lineLabel(view)
            }
        }

        label.setOnTouchListener { view: View, touchEvent: MotionEvent ->
            if (touchEvent.action == MotionEvent.ACTION_MOVE) {
                if (this._dragging_lineLabel == null) {
                    this._dragging_lineLabel = view
                    view.startDragAndDrop(
                        null,
                        View.DragShadowBuilder(view),
                        null,
                        0
                    )
                    return@setOnTouchListener true
                }
            }
            false
        }

        label.setOnDragListener { view: View, dragEvent: DragEvent ->
            when (dragEvent.action) {
                DragEvent.ACTION_DROP -> {
                    val from_label =  this._dragging_lineLabel
                    if (from_label != null && from_label != view) {
                        val y_from = (from_label.parent as ViewGroup).indexOfChild(from_label)
                        val y_to = (view.parent as ViewGroup).indexOfChild(view)
                        // TODO: This is a bit shit
                        this.main_fragment.get_main().get_opus_manager().move_line(y_from, y_to)
                    }
                    this._dragging_lineLabel = null
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    this._dragging_lineLabel = null
                }
                else -> { }
            }
            true
        }

        return LineLabelViewHolder(label)
    }

    override fun onBindViewHolder(holder: LineLabelViewHolder, position: Int) {
        val label = this.main_fragment.get_label_text(position)
        (holder.itemView as LabelView).set_text(label)
    }

    override fun getItemCount(): Int {
        return this.row_count
    }

    fun scrollToLine(y: Int) {
        val current_y = this.recycler.computeVerticalScrollOffset()
        this.recycler.scrollBy(0, y - current_y)
    }

    private fun interact_lineLabel(view: View) {
        this.main_fragment.set_active_line(this.get_y(view))
    }

    private fun get_y(view: View): Int {
        var abs_y = 0
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
