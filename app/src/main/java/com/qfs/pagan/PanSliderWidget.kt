package com.qfs.pagan

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@SuppressLint("ClickableViewAccessibility")
class PanSliderWidget(context: Context, attrs: AttributeSet? = null): ConstraintLayout(context, attrs) {
    abstract class OnSeekBarChangeListener {
        abstract fun on_touch_start(slider: PanSliderWidget)
        abstract fun on_touch_stop(slider: PanSliderWidget)
        abstract fun on_progress_change(slider: PanSliderWidget, value: Int)

    }
    val progress_bar = LinearLayout(context, attrs)
    val node = LinearLayout(context, attrs)
    val non_progress_bar = LinearLayout(context, attrs)

    var max = 100
    var min = -100
    var progress: Int = ((this.max - this.min) / 2) + this.min
    var on_change_listener: OnSeekBarChangeListener? = null
    init {
        this.addView(this.non_progress_bar)
        this.non_progress_bar.layoutParams.width = MATCH_PARENT
        this.non_progress_bar.layoutParams.height = 50
        this.non_progress_bar.setBackgroundColor(Color.RED)
        this.non_progress_bar.setPadding(0,0,0,0)

        this.non_progress_bar.addView(this.progress_bar)
        this.non_progress_bar.addView(this.node)

        this.node.layoutParams.width = 50
        this.node.layoutParams.height = 50
        this.node.setPadding(0,0,0,0)
        this.node.setBackgroundResource(R.drawable.button)


        this.progress_bar.orientation = HORIZONTAL
        this.progress_bar.layoutParams.width = MATCH_PARENT
        this.progress_bar.layoutParams.height = MATCH_PARENT
        this.progress_bar.setPadding(0,0,0,0)
        this.progress_bar.setBackgroundColor(Color.BLUE)

        this.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    this.on_change_listener?.on_touch_start(this)
                }
            }
            when (motionEvent.action) {
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_UP -> {
                    val measured_max_width = this.non_progress_bar.measuredWidth.toFloat()
                    val progress_bar_half_width = this.progress_bar.measuredWidth.toFloat() / 2F
                    val pos_x = min(
                        measured_max_width - progress_bar_half_width,
                        max(
                            0F - progress_bar_half_width,
                            (motionEvent.x - this.paddingStart - progress_bar_half_width)
                        )
                    )
                    val value_x = pos_x * (this.max - this.min) / measured_max_width
                    this.set_progress(value_x.roundToInt())
                }
            }
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    this.on_change_listener?.on_touch_stop(this)
                }
            }

            true
        }
    }

    fun set_progress(n: Int) {
        this.progress = n
        val center_point = ((n - this.min) * (this.non_progress_bar.measuredWidth - (this.non_progress_bar.paddingStart + this.non_progress_bar.paddingEnd)) / (this.max - this.min)).toFloat()
        this.progress_bar.x = center_point - (this.progress_bar.measuredWidth / 2F)
        this.node.x = center_point - (this.node.measuredWidth / 2F)
        this.node.y = (this.non_progress_bar.measuredHeight - this.node.measuredHeight) / 2F
        this.on_change_listener?.on_progress_change(this, this.progress)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.setPadding(20, 20, 20, 20)
        // this.node.y = (this.measuredHeight - this.node.measuredHeight).toFloat() / 2F
        // this.node.x = (this.measuredWidth - this.node.measuredWidth).toFloat() / 2F
        // println("NOY: ${this.node.y}")
        // println("NOX: ${this.node.x}")
        this.progress_bar.x = 0F
    }

}