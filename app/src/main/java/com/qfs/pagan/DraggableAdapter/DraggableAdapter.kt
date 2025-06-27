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
            return ItemTouchHelper.UP or ItemTouchHelper.DOWN;
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            this.adapter.onRowMoved(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
            return true
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE && viewHolder != null) {
                this.adapter.onRowSelected(viewHolder)
            }
            super.onSelectedChanged(viewHolder, actionState)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            this.adapter.onRowClear(viewHolder)
        }
    }

    fun onRowMoved(from_position: Int, to_position: Int) {
        notifyItemMoved(from_position, to_position);
    }
    abstract fun onRowSelected(view_holder: ViewHolder)
    abstract fun onRowClear(view_holder: ViewHolder)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        val touch_helper = ItemTouchHelper(ItemMoveCallback(this))
        touch_helper.attachToRecyclerView(recyclerView)
        super.onAttachedToRecyclerView(recyclerView)
    }
}

