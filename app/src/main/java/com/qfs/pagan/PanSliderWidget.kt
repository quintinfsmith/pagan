package com.qfs.pagan

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import android.widget.LinearLayout
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ClickableViewAccessibility")
class PanSliderWidget(context: Context, attrs: AttributeSet? = null): LinearLayout(context, attrs) {
    abstract class OnSeekBarChangeListener {
        abstract fun on_touch_start(slider: PanSliderWidget)
        abstract fun on_touch_stop(slider: PanSliderWidget)
        abstract fun on_progress_change(slider: PanSliderWidget, value: Int)
    }
    val stroke_width = 10F

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
            val width = this.width.toFloat()
            val height = this.height.toFloat()
            val padding = (height / 2f) + (2F * that.stroke_width)

            val color_map = this.get_main_activity().view_model.color_map
            val purple = color_map[ColorMap.Palette.Leaf]

            val div_size = 1F / (that.max - that.min).toFloat()
            val relative_n = (that.progress - that.min).toFloat() * div_size

            this.paint.strokeWidth = 1f
            this.paint.color = purple
            this.path.reset()

            canvas.drawRect(
                padding,
                0f,
                width - padding,
                height,
                this.paint
            )

            this.paint.color = color_map[ColorMap.Palette.Lines]
            this.paint.strokeWidth = 1F
            canvas.drawRoundRect(
                that.stroke_width,
                0F,
                width - that.stroke_width,
                height,
                padding,
                padding,
                this.paint
            )

            this.paint.color = color_map[ColorMap.Palette.Background]
            canvas.drawRoundRect(
                that.stroke_width * 2F,
                that.stroke_width,
                width - (that.stroke_width * 2F),
                height - that.stroke_width,
                padding,
                padding,
                this.paint
            )

            val handle_point = padding + ((width - (2 * padding)) * relative_n)

            this.paint.color = color_map[ColorMap.Palette.Lines]
            this.paint.strokeWidth = 1F
            canvas.drawOval(
                (width / 2F) - (padding / 4f),
                (height / 2F) - (padding / 4f),
                (width / 2F) + (padding / 4f),
                (height / 2F) + (padding / 4f),
                this.paint
            )
            canvas.drawLine(width / 2F, that.stroke_width * 4f, width / 2F, height - (that.stroke_width * 4f), paint)

            this.paint.color = color_map[ColorMap.Palette.Leaf]
            this.paint.strokeWidth = 1F
            this.path.addOval(
                handle_point - (padding / 2F),
                (height / 2F) - (padding / 2F),
                handle_point + (padding / 2F),
                (height / 2F) + (padding / 2F),
                Path.Direction.CW
            )
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
                    val padding = ((this.image_view.measuredHeight.toFloat()) / 2F) + (2F * this.stroke_width)
                    val pos_x = (max(padding, min(measured_max_width - padding, motionEvent.x)) - padding)
                    val use_width = (measured_max_width - (2F * padding))
                    val rel_x = (pos_x / use_width) + (.5F / (this.max - this.min).toFloat())

                    val value_x = max(this.min, min(this.max,((this.max - this.min).toFloat() * rel_x).toInt() + this.min))
                    this.set_progress(value_x)
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
