package com.qfs.apres.soundfontplayer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PitchedBuffer(val data: ShortArray, var pitch: Float, known_max: Int? = null, range: IntRange? = null, var is_loop: Boolean = false) {
    class PitchedBufferOverflow(): Exception()
    val max: Int
    var _range = range ?: 0 until data.size
    var size: Int = ((this._range.last + 1 - this._range.first).toFloat() / this.pitch).roundToInt()

    private var virtual_position: Int = 0
    var pitch_adjustment: Float = 1F

    init {
        this.max = known_max ?: max(abs(data.min().toInt()), data.max().toInt())
    }

    fun is_overflowing(): Boolean {
        return (this.virtual_position.toFloat() * this.get_calculated_pitch()).toInt() - this._range.first > this._range.last
    }

    fun get_calculated_pitch(): Float {
        return this.pitch * this.pitch_adjustment
    }

    fun repitch(new_pitch_adjustment: Float) {
        this.pitch_adjustment = new_pitch_adjustment
        this.size = ((this._range.last + 1 - this._range.first).toFloat() / this.pitch).roundToInt()
    }

    fun reset_pitch() {
        this.repitch(1F)
    }

    fun position(): Int {
        return this.virtual_position
    }

    fun position(index: Int) {
        this.virtual_position = index
    }

    fun position_subtract_buffer(virtual_position: Int, buffer_to_subtract: PitchedBuffer) {
        val to_sub = ((buffer_to_subtract._range.last + 1 - buffer_to_subtract._range.first).toFloat() / buffer_to_subtract.pitch).toInt()
        this.virtual_position = virtual_position - to_sub
    }

    private fun _get_real_frame(i: Float): Short {
        var range_size = this._range.last + 1 - this._range.first
        var adj_i = min(this._range.last, this._range.first + if (this.is_loop) {
            i.toInt() % range_size
        } else if (i >= range_size) {
            throw PitchedBufferOverflow()
        } else {
            i.toInt()
        })
        return this.data[adj_i]
    }

    fun get(): Float {
        val pitch = this.get_calculated_pitch()
        val position = (this.virtual_position++).toFloat() * pitch
        var output = this._get_real_frame(position)

        return output.toFloat() / Short.MAX_VALUE.toFloat()
    }
}
