package com.qfs.apres.soundfontplayer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PitchedBuffer(val data: ShortArray, var pitch: Float, known_max: Int? = null, known_size: Int? = null) {
    val data_size = data.size
    val max: Int
    var size: Int

    private var virtual_position: Int = 0
    private val original_pitch: Float = this.pitch

    init {
        this.max = known_max ?: max(abs(data.min().toInt()), data.max().toInt())
        this.size = known_size ?: (this.data.size.toFloat() / this.pitch).toInt()
    }

    fun repitch(new_pitch: Float) {
        val old_pitch = this.pitch
        val next_pitch = this.original_pitch * new_pitch
        if (old_pitch == next_pitch) {
            return 
        }

        this.pitch = this.original_pitch * new_pitch
        this.size = (this.data_size.toFloat() / this.pitch).toInt()
        val new_position = (this.virtual_position.toFloat() * this.pitch / old_pitch).toInt()
        this.virtual_position = min(this.size - 1, new_position)
    }

    fun reset_pitch() {
        this.pitch = this.original_pitch
        this.size = (this.data_size.toFloat() / this.pitch).toInt()
    }

    fun position(): Int {
        return this.virtual_position
    }

    fun position(index: Int) {
        this.virtual_position = index
    }

    fun get(): Short {
        return this.data[((this.virtual_position++).toFloat() * this.pitch).toInt()]
    }

}
