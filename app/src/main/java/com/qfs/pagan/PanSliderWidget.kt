package com.qfs.pagan

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
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

    private var _in_transition = false
    var max = 1
    var min = -1
    var progress: Int = ((this.max - this.min) / 2) + this.min
    var on_change_listener: OnSeekBarChangeListener? = null
    private val _image_view: ImageView = object: androidx.appcompat.widget.AppCompatImageView(context, attrs) {
        val paint = Paint()
        val path = Path()
        val relative_handle_point: Float

        init {
            val that = this@PanSliderWidget
            this.relative_handle_point = (that.progress - that.min).toFloat() / (that.max - that.min).toFloat()
        }

        override fun onDraw(canvas: Canvas) {
            val that = this@PanSliderWidget
            val width = this.width.toFloat()
            val height = this.height.toFloat()
            val padding = (height / 2f) + (2F * that.stroke_width)

            val purple = ContextCompat.getColor(this.context, R.color.primary_text)
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

            this.paint.strokeWidth = 1F
            this.paint.color = ContextCompat.getColor(this.context, R.color.primary)
            canvas.drawRoundRect(
                that.stroke_width,
                0F,
                width - that.stroke_width,
                height,
                padding,
                padding,
                this.paint
            )

            val handle_point = padding + ((width - (2 * padding)) * relative_n)

            this.paint.strokeWidth = 1F
            this.paint.color = ContextCompat.getColor(this.context, R.color.main_background)
            canvas.drawOval(
                (width / 2F) - (padding / 3f),
                (height / 2F) - (padding / 3f),
                (width / 2F) + (padding / 3f),
                (height / 2F) + (padding / 3f),
                this.paint
            )
            canvas.drawLine(width / 2F, that.stroke_width * 4f, width / 2F, height - (that.stroke_width * 4f), this.paint)

            this.paint.strokeWidth = 1F
            this.paint.color = purple
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
        this.addView(this._image_view)
        this._image_view.layoutParams.width = MATCH_PARENT
        this._image_view.layoutParams.height = MATCH_PARENT
        this._image_view.setPadding(0,0,0,0)

        this._image_view.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    this.on_change_listener?.on_touch_start(this)
                    this._in_transition = true
                }
            }
            when (motionEvent.action) {
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_UP -> {
                    val measured_max_width = this._image_view.measuredWidth.toFloat()
                    val padding = ((this._image_view.measuredHeight.toFloat()) / 2F) + (2F * this.stroke_width)
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
                    this._in_transition = false
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
        this._image_view.invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.set_progress(this.progress, true)
    }

}
