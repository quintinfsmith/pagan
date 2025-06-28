package com.qfs.pagan.DraggableAdapter

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

abstract class DraggableAdapter<T: RecyclerView.ViewHolder> : RecyclerView.Adapter<T>() {
    class ItemMoveCallback<T: RecyclerView.ViewHolder>(val adapter: DraggableAdapter<T>): ItemTouchHelper.Callback() {
        override fun isLongPressDragEnabled(): Boolean {
            return true
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val from_position = viewHolder.bindingAdapterPosition
            val to_position = target.bindingAdapterPosition
            this.adapter.notifyItemMoved(from_position, to_position)
            this.adapter.on_row_moved(from_position, to_position)
            return false
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE && viewHolder != null) {
                this.adapter.on_row_selected(viewHolder)
            }
            super.onSelectedChanged(viewHolder, actionState)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            this.adapter.on_row_clear(viewHolder)
        }
    }

    abstract fun on_row_moved(from_position: Int, to_position: Int)
    abstract fun on_row_selected(view_holder: ViewHolder)
    abstract fun on_row_clear(view_holder: ViewHolder)

    val touch_helper = ItemTouchHelper(ItemMoveCallback(this))

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.touch_helper.attachToRecyclerView(recyclerView)
        super.onAttachedToRecyclerView(recyclerView)
    }
}

