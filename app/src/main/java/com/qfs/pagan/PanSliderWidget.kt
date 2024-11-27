package com.qfs.pagan

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import android.widget.LinearLayout
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ClickableViewAccessibility")
class PanSliderWidget(context: Context, attrs: AttributeSet? = null): LinearLayout(context, attrs) {
    abstract class OnSeekBarChangeListener {
        abstract fun on_touch_start(slider: PanSliderWidget)
        abstract fun on_touch_stop(slider: PanSliderWidget)
        abstract fun on_progress_change(slider: PanSliderWidget, value: Int)

    }
    var in_transition = false
    var max = 1
    var min = -1
    var progress: Int = ((this.max - this.min) / 2) + this.min
    var on_change_listener: OnSeekBarChangeListener? = null
    val image_view: ImageView = object: androidx.appcompat.widget.AppCompatImageView(context, attrs) {
        val paint = Paint()
        val path = Path()

        fun get_main_activity(): MainActivity {
            var context = this@PanSliderWidget.context
            while (context !is MainActivity) {
                context = (context as ContextThemeWrapper).baseContext
            }
            return context
        }

        override fun onDraw(canvas: Canvas) {
            val that = this@PanSliderWidget
            val div_size = 1F / ((that.max + 1) - that.min).toFloat()
            val relative_n = (that.progress - that.min).toFloat() * div_size
            val width = this.width.toFloat()
            val height = this.height.toFloat()

            val color_map = this.get_main_activity().view_model.color_map
            val offset = width * div_size
            val offset_half = offset / 2f
            val handle_center = relative_n + (.5F / ((that.max + 1) - that.min).toFloat())
            val purple = color_map[ColorMap.Palette.Leaf]

            this.paint.strokeWidth = 1f
            this.paint.color = purple
            this.paint.setShadowLayer(5F, 2F, 2F, Color.BLACK)
            val end_clip = if (that.progress == that.min || that.progress == that.max) {
                (div_size / 2F)
            } else {
                0F
            }
            val bar_start = max(0F, handle_center - .5F + end_clip) * width
            val bar_end = min(1F, handle_center + .5f - end_clip) * width

            this.path.reset()
            this.path.moveTo(bar_start, 0f)
            this.path.lineTo(bar_end, 0F)
            this.path.lineTo(bar_end, height)
            this.path.lineTo(bar_start, height)

            canvas.drawPath(this.path, this.paint)

            val handle_point = width * relative_n

            this.paint.strokeWidth = 1F
            this.paint.color = Color.WHITE

            canvas.drawLine(0f, 0F, width, 0F, this.paint)
            canvas.drawLine(0f, height - 1F, width, height - 1F, this.paint)
            canvas.drawLine(0F, 0F, 0F, height, this.paint)
            canvas.drawLine(width - 1F, 0F, width - 1F, height, this.paint)

            this.paint.color = color_map[ColorMap.Palette.LeafSelected]
            this.paint.strokeWidth = 3f
            val nob_width = height * .75F
            val nob_width_half = nob_width / 2F
            this.path.reset()
            when (that.progress) {
                that.max -> {
                    this.path.moveTo(handle_point + offset, height * .9F)
                    this.path.lineTo(handle_point + offset - nob_width_half, height / 2)
                    this.path.lineTo(handle_point + offset, height * .1F)
                }
                that.min -> {
                    this.path.moveTo(handle_point, height * .9F)
                    this.path.lineTo(handle_point + nob_width_half, height / 2)
                    this.path.lineTo(handle_point, height * .1F)
                }
                else -> {
                    this.path.moveTo(handle_point + offset_half, height * .9F)
                    this.path.lineTo(handle_point + (offset_half - nob_width_half), height / 2F)
                    this.path.lineTo(handle_point + offset_half, height * .1F)
                    this.path.lineTo(handle_point + offset_half + nob_width_half, height / 2F)
                    this.path.lineTo(handle_point + offset_half, height * .9F)
                }
            }

            canvas.drawPath(this.path, this.paint)

            super.onDraw(canvas)
        }
    }

    init {
        this.orientation = HORIZONTAL
        this.addView(this.image_view)
        this.image_view.layoutParams.width = MATCH_PARENT
        this.image_view.layoutParams.height = MATCH_PARENT
        this.image_view.setPadding(0,0,0,0)

        this.image_view.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    this.on_change_listener?.on_touch_start(this)
                    this.in_transition = true
                }
            }
            when (motionEvent.action) {
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_UP -> {
                    val measured_max_width = this.image_view.measuredWidth.toFloat()
                    val pos_x = min(
                        measured_max_width,
                        max(0F, motionEvent.x)
                    )

                    val value_x = floor((pos_x * ((this.max + 1) - this.min) / measured_max_width) + this.min)
                    this.set_progress(max(this.min, min(this.max, value_x.toInt())))
                }
            }
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    this.on_change_listener?.on_touch_stop(this)
                    this.in_transition = false
                }
            }

            true
        }
    }

    fun set_progress(n: Int, surpress_callback: Boolean = false) {
        this.progress = n
        if (!surpress_callback) {
            this.on_change_listener?.on_progress_change(this, this.progress)
        }
        this.image_view.invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.set_progress(this.progress, true)
    }

}
