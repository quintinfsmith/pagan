package com.qfs.pagan

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.RecyclerView

class ColumnRecyclerViewHolder(context: Context): RecyclerView.ViewHolder(LinearLayout(
    ContextThemeWrapper(context, R.style.column)
)) {
    fun get_column_layout(): ColumnLayout? {
        return if ((this.itemView as ViewGroup).childCount > 0) {
            val item = (this.itemView as ViewGroup).getChildAt(0)
            if (item is ColumnLayout) {
                item
            } else {
                null
            }
        } else {
            null
        }
    }
}
