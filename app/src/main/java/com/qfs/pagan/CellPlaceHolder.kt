package com.qfs.pagan

import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper

class CellPlaceHolder(context: Context, is_even: Boolean): LinearLayout(ContextThemeWrapper(context, R.style.leaf)) {
    init {

        var inner = if (is_even) {
            LinearLayout(ContextThemeWrapper(context, R.style.leaf_inner_pl_even))
        } else {
            LinearLayout(ContextThemeWrapper(context, R.style.leaf_inner_pl_odd))
        }
        this.addView(inner)
        inner.layoutParams.height = MATCH_PARENT
        inner.layoutParams.width = MATCH_PARENT
    }

    fun get_activity(): MainActivity {
        return ((this.context as ContextThemeWrapper).baseContext as MainActivity)
    }
    //fun replace() {
    //    thread {
    //        this.get_activity().runOnUiThread {
    //            if (this.parent == null) {
    //                return@runOnUiThread
    //            }
    //            var index = (this.parent as ViewGroup).indexOfChild(this)
    //            (this.parent as ViewGroup).addView(CellLayout(this, index), index)
    //            (this.parent as ViewGroup).removeView(this)

    //        }
    //    }
    //}
}