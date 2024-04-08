package com.qfs.pagan

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.widget.LinearLayoutCompat

class ControlWidgetVolume(context: Context, attrs: AttributeSet? = null): LinearLayoutCompat(context, attrs), ControlWidget {
    private val slider = PaganSeekBar(context, attrs)
    private val input = RangedNumberInput(context, attrs)
    private val min = 0
    private val max = 128

    init {
        this.orientation = HORIZONTAL

        this.slider.max = this.max
        this.slider.min = this.min

        this.input.set_range(this.min, this.max)

        var lockout = false
        this.input.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) {
                if (lockout || p0.toString().isEmpty()) {
                    return
                }
                lockout = true
                this@ControlWidgetVolume.slider.progress = p0.toString().toInt()
                lockout = false
            }
        })

        this.slider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (lockout) {
                    return
                }
                lockout = true
                this@ControlWidgetVolume.input.set_value(p1)
                lockout = false
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) { }
        })


        this.addView(this.input)
        this.addView(this.slider)

        this.input.layoutParams.width = WRAP_CONTENT
        this.input.layoutParams.height = MATCH_PARENT

        this.slider.layoutParams.width = 0
        this.slider.layoutParams.height = MATCH_PARENT
        (this.slider.layoutParams as LinearLayout.LayoutParams).weight = 1f
    }

    override fun get_value(): Float {
        return this.slider.progress.toFloat()
    }
}