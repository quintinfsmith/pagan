package com.qfs.radixulous

import android.content.Context
import android.graphics.PointF
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

open class TimeableLayoutManager(context: Context?, orientation: Int, reverseLayout: Boolean): LinearLayoutManager(context, orientation, reverseLayout) {

    //private static final float MILLISECONDS_PER_INCH = 5f; //default is 25f (bigger = slower)


    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int)
    {
        var linearSmoothScroller = object: LinearSmoothScroller(recyclerView.context) {
            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                return super.computeScrollVectorForPosition(targetPosition)
            }
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 1F
            }
        }

        linearSmoothScroller.targetPosition = position
        startSmoothScroll(linearSmoothScroller)
    }
}
