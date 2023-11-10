package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout

// LeafText exists to make the text consider the state of the LeafButton
class LeafInnerWrapper(context: Context, attrs: AttributeSet): ConstraintLayout(ContextThemeWrapper(context, R.style.leaf_inner), attrs) {
    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        return (this.parent as LeafButton?)?.drawableState ?: super.onCreateDrawableState(extraSpace + 5)
    }

}
