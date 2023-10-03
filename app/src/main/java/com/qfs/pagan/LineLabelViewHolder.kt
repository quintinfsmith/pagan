package com.qfs.pagan

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView

class LineLabelViewHolder(context: Context) : RecyclerView.ViewHolder(LinearLayout(context)) {
    fun get_label(): LineLabelView {
        return (this.itemView as ViewGroup).children.first() as LineLabelView
    }
}