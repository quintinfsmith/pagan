package com.qfs.apres.soundfontplayer

import java.nio.ShortBuffer

class PitchedBuffer(data: ShortArray, var pitch: Double) {
    private val buffer: ShortBuffer = ShortBuffer.wrap(data)
    val data_size = data.size
    var size = (data.size.toDouble() / this.pitch).toInt()
    private var cached_value: Short? = null
    private var cached_position = 0
    private var virtual_position: Int = 0

    init {
        this.cached_position = 0
        this.virtual_position = 0
        if (this.pitch < 1.0) {
            this.cached_value = this.buffer.get()
        }
    }

    fun position(): Int {
        return this.virtual_position
    }

    fun position(index: Int) {
        this.virtual_position = index

        val pos = (index * this.pitch).toInt()
        this.buffer.position(pos)
        if (this.pitch < 1.0) {
            this.cached_value = this.buffer.get()
            this.buffer.position(pos)
            this.cached_position = pos
        }
    }

    fun get(): Short {
        return if (this.pitch >= 1.0) {
            this.get_high()
        } else {
            this.get_low()
        }
    }

    private fun get_high(): Short {
        val values = mutableListOf<Short>()
        while (this.buffer.position() / this.pitch < this.position()) {
            values.add(this.buffer.get())
        }
        this.virtual_position += 1

        return values.average().toInt().toShort()
    }

    private fun get_low(): Short {
        val pitched_position = this.virtual_position * this.pitch
        if (pitched_position > this.cached_position) {
            this.cached_value = this.buffer.get()
            this.cached_position = this.buffer.position()
        }

        val next_value = if (this.cached_position == this.data_size) {
            this.cached_value!!
        } else {
            this.buffer.get()
        }
        this.buffer.position(this.cached_position)

        this.virtual_position += 1

        val weight = pitched_position - pitched_position.toInt()
        val output = this.cached_value!! + ((next_value - this.cached_value!!) * weight).toInt().toShort()

        return output.toShort()
    }
}