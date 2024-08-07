package com.qfs.apres.soundfontplayer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.log
import kotlin.math.roundToInt

class PitchedBuffer(val data: ShortArray, var pitch: Float, known_max: Int? = null) {
    val max: Int
    var size: Int = (data.size.toFloat() / this.pitch).roundToInt()
    var forced_points: MutableList<Int> = mutableListOf()
    var prev_get: Short? = null
    var weight: Boolean = false

    private var virtual_position: Int = 0
    var pitch_adjustment: Float = 1F

    init {
        this.max = known_max ?: max(abs(data.min().toInt()), data.max().toInt())
    }

    fun is_overflowing(): Boolean {
        return (this.virtual_position.toFloat() * this.get_calculated_pitch()).toInt() >= this.data.size
    }

    fun get_calculated_pitch(): Float {
        return this.pitch * this.pitch_adjustment
    }

    fun repitch(new_pitch_adjustment: Float) {
        this.pitch_adjustment = new_pitch_adjustment
        this.size = (this.data.size.toFloat() / this.get_calculated_pitch()).toInt()
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

    fun get(): Short {
        val pitch = this.get_calculated_pitch()
        var output = if (pitch < 1F) {
            val position = ((this.virtual_position++).toFloat() * pitch).toInt()
            this.data[min(position, this.data.size - 1)]
        } else {
            val position_a = ((this.virtual_position++).toFloat() * pitch).toInt()
            val position_b = (this.virtual_position.toFloat() * pitch).toInt()
            var tmp = 0F
            for (i in position_a until position_b) {
                tmp += this.data[min(i, this.data.size - 1)].toFloat()
            }
            tmp /= (position_b - position_a).toFloat()
            tmp.toInt().toShort()
        }

        if (this.weight && this.prev_get != null) {
            output = ((this.prev_get!!.toInt() + output.toInt()) / 2).toShort()
            this.weight = false
        }
        this.prev_get = output
        return output
    }
}
