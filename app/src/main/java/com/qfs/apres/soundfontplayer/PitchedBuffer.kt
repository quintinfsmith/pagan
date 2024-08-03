package com.qfs.apres.soundfontplayer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.log
import kotlin.math.roundToInt

class PitchedBuffer(val data: ShortArray, var pitch: Float, known_max: Int? = null) {
    val max: Int
    val size: Int = data.size

    private var virtual_position: Int = 0
    var pitch_adjustment: Float = 1F
    var pitched_size: Int = (this.data.size.toFloat() * this.pitch / this.pitch_adjustment).roundToInt()

    init {
        this.max = known_max ?: max(abs(data.min().toInt()), data.max().toInt())
    }

    fun is_overflowing(): Boolean {
        return this.virtual_position >= this.pitched_size
    }

    fun repitch(new_pitch_adjustment: Float) {
        this.pitch_adjustment = new_pitch_adjustment
        this.pitched_size = (this.data.size * (this.pitch / this.pitch_adjustment)).toInt()
    }

    fun reset_pitch() {
        this.pitch_adjustment = 1F
        this.pitched_size = (this.data.size * this.pitch).toInt()
    }

    fun position(): Int {
        return this.virtual_position
    }

    fun position(index: Int) {
        this.virtual_position = index
    }

    fun pitched_position(): Int {
        return ((this.virtual_position).toFloat() * (this.pitch / this.pitch_adjustment)).toInt()
    }

    fun pitched_position(index: Int) {
        this.virtual_position = (index.toFloat() / (this.pitch / this.pitch_adjustment)).toInt()
    }

    fun get(): Short {
        val output = this.data[this.pitched_position()]
        this.virtual_position++
        return output
    }

}
