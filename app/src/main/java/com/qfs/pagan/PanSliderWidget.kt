package com.qfs.pagan

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
        fun get_main_activity(): MainActivity {
            var context = this@PanSliderWidget.context
            while (context !is MainActivity) {
                context = (context as ContextThemeWrapper).baseContext
            }
            return context
        }

        override fun onDraw(canvas: Canvas) {
            val that = this@PanSliderWidget
            val relative_n = (that.progress - that.min).toFloat() / ((that.max + 1) - that.min).toFloat()
            val paint = Paint()
            val color_map = this.get_main_activity().view_model.color_map

            paint.setColor(color_map.get(ColorMap.Palette.Leaf))
            paint.strokeWidth = 1f
            val left_height = canvas.height * if (relative_n <= .5F) {
                1F
            } else {
                (1F - relative_n) / .5f
            }

            val right_height = canvas.height * if (relative_n >= .5f ) {
                1F
            } else {
                relative_n / .5F
            }

            val offset = canvas.width / ((that.max + 1) - that.min).toFloat()

            val main_path = Path()
            main_path.moveTo(offset / 2F, (canvas.height - left_height))
            main_path.lineTo(canvas.width.toFloat() / 2F, 0F)
            main_path.lineTo(canvas.width.toFloat() - (offset / 2F), (canvas.height - right_height))
            main_path.lineTo(canvas.width.toFloat() - (offset / 2F), canvas.height.toFloat())
            main_path.lineTo(canvas.width.toFloat() / 2F, canvas.height.toFloat())
            main_path.lineTo(offset / 2F, canvas.height.toFloat())
            main_path.lineTo(offset / 2F, (canvas.height - left_height))
            canvas.drawPath(main_path, paint)

            val canvsx = (canvas.width * relative_n)
            val nob_width = offset

            paint.strokeWidth = 6f
            paint.setColor(
                 color_map.get(ColorMap.Palette.LeafText)
            )

            canvas.drawLine(
                canvas.width / 2F,
                0F,
                canvas.width / 2F,
                canvas.height.toFloat(),
                paint
            )

            paint.strokeWidth = 3f

            val position_path = Path()
            position_path.moveTo(
                 canvsx - ((nob_width - offset) / 2F),
                canvas.height.toFloat()
            )
            position_path.lineTo(
                canvsx + (offset / 2F),
                canvas.height.toFloat() / 2F
            )
            position_path.lineTo(
                canvsx + offset + ((nob_width - offset) / 2F),
                canvas.height.toFloat()
            )
            position_path.lineTo(
                canvsx - ((nob_width - offset) / 2F),
                canvas.height.toFloat()
            )
            canvas.drawPath(position_path, paint)


            //paint.setColor(color_map.get(ColorMap.Palette.LeafText))

            // paint.setColor(color_map.get(ColorMap.Palette.LeafSelected))

            // paint.strokeWidth = 1f
            // paint.strokeWidth = 10f
            // canvas.drawOval(
            //     canvsx - ((nob_width - offset) / 2F),
            //     0f,
            //     canvsx + offset + ((nob_width - offset) / 2F),
            //     canvas.height.toFloat(),
            //     paint
            // )

            super.onDraw(canvas)
        }
    }
    var bitmap: Bitmap? = null
    var canvas: Canvas? = null

    init {
        this.orientation = HORIZONTAL
        this.addView(this.image_view)
        //this.image_view.layoutParams.height = MATCH_PARENT
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
                    println("$value_x, ${motionEvent.x}")
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
