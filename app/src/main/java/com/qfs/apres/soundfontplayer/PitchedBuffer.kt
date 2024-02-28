package com.qfs.apres.soundfontplayer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PitchedBuffer(val data: ShortArray, var pitch: Float) {
    val data_size = data.size
    val max = max(abs(data.min().toInt()), data.max().toInt())
    var size = (data.size.toFloat() / this.pitch).toInt()

    private var virtual_position: Int = 0
    private val original_pitch: Float = this.pitch

    fun repitch(new_pitch: Float) {
        this.pitch = this.original_pitch * new_pitch
        this.size = (this.data_size.toFloat() / this.pitch).toInt()
        this.virtual_position = min(this.virtual_position, this.size - 1)
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