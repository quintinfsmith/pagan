package com.qfs.radixulous

import android.content.Context
import android.graphics.PointF
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.concurrent.thread

open class TimeableLayoutManager(context: Context?, orientation: Int, reverseLayout: Boolean): LinearLayoutManager(context, orientation, reverseLayout) {
    var scroll_velocity = 1f
    //private static final float MILLISECONDS_PER_INCH = 5f; //default is 25f (bigger = slower)

    //fun scrollToPositionWithOffsetAndDelay(position: Int, offset: Int, delay: Float) {
    //
    //    return scrollToPositionWithOffset(position, offset)
    //}

    fun get_current_position_and_offset(): Pair<Int, Int> {
        var position = this.findFirstCompletelyVisibleItemPosition()

        println("CHILD POSITION: $position")
        var child = this.findViewByPosition(position) ?: return Pair(-1,0)
        var offset = child.left
        return Pair(position, offset)
    }

    override fun scrollToPositionWithOffset(position: Int, offset: Int) {
        super.scrollToPositionWithOffset(position, offset)
    }

    fun calc_scroll_velocity(position: Int, delay: Float) {
        var first_visible = this.findFirstVisibleItemPosition()
        var last_visible = this.findLastVisibleItemPosition()
        if (position - 1 in first_visible .. last_visible) {
            var prev_child = this.findViewByPosition(position - 1)!!
            var prev_child_offset = prev_child.left
            var width = prev_child.width
            var d = 5
            var first_fully_visible_index = this.findFirstCompletelyVisibleItemPosition()
            var first_child = this.findViewByPosition(first_fully_visible_index) ?: return
            var second_child = this.findViewByPosition(position) ?: return
            var distance = second_child.left - first_child.left
            println("D: $distance, $delay")
            this.set_scroll_velocity(delay / distance.toFloat())
        } else {
            this.set_scroll_velocity(100F)
        }

    }

    fun set_scroll_velocity(ms_per_px: Float) {
        println("$ms_per_px VELOC")
        this.scroll_velocity = ms_per_px
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int)
    {
        var that = this
        var linearSmoothScroller = object: LinearSmoothScroller(recyclerView.context) {
            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                return super.computeScrollVectorForPosition(targetPosition)
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return that.scroll_velocity
            }
        }

        linearSmoothScroller.targetPosition = position
        startSmoothScroll(linearSmoothScroller)
    }
}
