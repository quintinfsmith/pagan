package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.LinearLayoutCompat
import com.qfs.pagan.OpusLayerInterface as OpusManager

open class ContextMenuView(context: Context, attrs: AttributeSet? = null): LinearLayoutCompat(context, attrs) {
    fun get_main(): MainActivity {
        return this.context as MainActivity
    }
    fun get_opus_manager(): OpusManager {
        return this.get_main().get_opus_manager()
    }
    open fun refresh() { }
}