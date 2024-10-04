package com.qfs.apres.soundfontplayer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class PitchedBuffer(val data: ShortArray, var pitch: Float, known_max: Int? = null, range: IntRange? = null, var is_loop: Boolean = false) {
    class PitchedBufferOverflow(): Exception()
    val max: Int
    var forced_points: MutableList<Int> = mutableListOf()
    var prev_get: Short? = null
    var weight: Boolean = false
    var _range = range ?: 0 until data.size
    var size: Int = ((this._range.last - this._range.first).toFloat() / this.pitch).roundToInt()

    private var virtual_position: Int = 0
    var pitch_adjustment: Float = 1F

    init {
        this.max = known_max ?: max(abs(data.min().toInt()), data.max().toInt())
    }

    fun is_overflowing(): Boolean {
        return (this.virtual_position.toFloat() * this.get_calculated_pitch()).toInt() - this._range.first >= this._range.last
    }

    fun get_calculated_pitch(): Float {
        return this.pitch * this.pitch_adjustment
    }

    fun repitch(new_pitch_adjustment: Float) {
        this.pitch_adjustment = new_pitch_adjustment
        this.size = ((this._range.last - this._range.first).toFloat() / this.pitch).roundToInt()
    }

    fun reset_pitch() {
        this.repitch(1F)
    }

    fun position(): Int {
        return this.virtual_position
    }

    fun position(index: Int) {
        this.weight = true
        this.virtual_position = index
    }

    private fun _get_real_frame(i: Int): Short {
        var range_size = this._range.last - this._range.first
        var adj_i = this._range.first + if (is_loop) {
            i % range_size
        } else if (i >= range_size) {
            throw PitchedBufferOverflow()
        }  else {
            i
        }

        return this.data[adj_i]
    }

    fun get(ignore_loop: Boolean = false): Short {
        val pitch = this.get_calculated_pitch()
        val position = ((this.virtual_position).toFloat() * pitch).toInt()
        var output = if (pitch < 1F) {
            this._get_real_frame(position)
        } else {
            val position_b = ((this.virtual_position + 1).toFloat() * pitch).toInt()

            var tmp = 0F
            var x = 0
            for (i in position until position_b) {
                tmp += try {
                    this._get_real_frame(i)
                } catch (e: PitchedBufferOverflow) {
                    break
                }
                x += 1
            }

            if (x != 0) {
                tmp /= x.toFloat()
            }
            tmp.toInt().toShort()
        }

        this.virtual_position += 1
        if (!ignore_loop && this.is_loop) {
            this.virtual_position = this.virtual_position % this.size
        }

        if (this.weight && this.prev_get != null) {
            output = ((this.prev_get!!.toInt() + output.toInt()) / 2).toShort()
            this.weight = false
        }
        this.prev_get = output
        return output
    }
}
