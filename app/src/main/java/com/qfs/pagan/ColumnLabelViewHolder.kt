package com.qfs.pagan

import android.content.Context
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.OpusLayerInterface as OpusManager

class ColumnLabelViewHolder(var context: Context) : RecyclerView.ViewHolder(LinearLayout(context)) {
    fun get_opus_manager(): OpusManager {
        return (context as MainActivity).get_opus_manager()
    }
}

