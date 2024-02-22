package com.qfs.apres.soundfontplayer

import java.lang.Math.abs
import kotlin.math.max
import kotlin.math.min

class PitchedBuffer(val data: ShortArray, var pitch: Double) {
    val data_size = data.size
    val max = max(abs(data.min().toInt()), data.max().toInt())
    var size = (data.size.toDouble() / this.pitch).toInt()

    private var virtual_position: Int = 0
    private val original_pitch: Double = this.pitch

    fun repitch(new_pitch: Double) {
        this.pitch = this.original_pitch * new_pitch
        this.size = (this.data_size.toDouble() / this.pitch).toInt()
        this.virtual_position = min(this.virtual_position, this.size - 1)
    }

    fun reset_pitch() {
        this.pitch = this.original_pitch
        this.size = (this.data_size.toDouble() / this.pitch).toInt()
    }

    fun position(): Int {
        return this.virtual_position
    }

    fun position(index: Int) {
        this.virtual_position = index
    }

    fun get(): Short {
        return this.data[((this.virtual_position++).toDouble() * this.pitch).toInt()]
    }

}