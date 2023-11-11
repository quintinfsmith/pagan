package com.qfs.pagan

import android.content.Context
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.BeatKey

class CellViewHolder(context: Context): RecyclerView.ViewHolder(LinearLayout(context)) {
    var beat_key: BeatKey? = null
    init {
        this.setIsRecyclable(false)
    }
}