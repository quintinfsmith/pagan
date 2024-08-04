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
    var minor_offset: Int = 0

    private var virtual_position: Int = 0
    var pitch_adjustment: Float = 1F

    init {
        this.max = known_max ?: max(abs(data.min().toInt()), data.max().toInt())
    }

    fun set_minor_offset(offset: Int) {
        this.minor_offset = offset
    }

    fun is_overflowing(): Boolean {
        return (this.virtual_position.toFloat() * this.get_calculated_pitch()).toInt() + this.minor_offset >= this.data.size
    }

    fun get_calculated_pitch(): Float {
        return this.pitch * this.pitch_adjustment
    }

    fun repitch(new_pitch_adjustment: Float) {
        this.pitch_adjustment = new_pitch_adjustment
        this.size = (this.data.size.toFloat() / this.get_calculated_pitch()).toInt()
        this.minor_offset = 0
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

    fun get(): Short {
        val pitch = this.get_calculated_pitch()
        val position = ((this.virtual_position++).toFloat() * pitch).toInt() + this.minor_offset
        return this.data[min(position, this.data.size - 1)]
    }
}
