package com.qfs.apres.soundfontplayer

import android.util.Log
import java.nio.BufferUnderflowException
import java.nio.ShortBuffer
import kotlin.math.abs
import kotlin.math.min

class PitchedBuffer(data: ShortArray, val pitch: Float) {
    val buffer = ShortBuffer.wrap(data)
    private val buffer_size = data.size
    val size = (data.size.toFloat() / this.pitch).toInt()
    var cached_value: Short? = null
    var cached_position = 0
    var virtual_position: Int = 0
    init {
        this.cached_position = 0
        this.virtual_position = 0
        if (this.pitch < 1F) {
            this.cached_value = this.buffer.get()
        }
    }

    fun position(): Int {
        return this.virtual_position
    }

    fun position(index: Int) {
        this.virtual_position = index

        val pos = min((index.toFloat() * this.pitch).toInt(), this.size - 1)
        this.buffer.position(pos)
        if (this.pitch < 1F) {
            this.cached_value = this.buffer.get()
            this.buffer.position(pos)
            this.cached_position = pos
        }
    }

    fun get(): Short {
        return if (this.pitch >= 1F) {
            this.get_high()
        } else {
            this.get_low()
        }
    }

    private fun get_high(): Short {
        var value: Short = 0
        while (this.buffer.position() / this.pitch < this.position()) {
            value = this.buffer.get()
        }
        this.virtual_position += 1

        return value
    }
    private fun get_low(): Short {
        val pitched_position = this.virtual_position * this.pitch
        if (pitched_position > this.cached_position) {
            this.cached_value = this.buffer.get()
            this.cached_position = this.buffer.position()
        }

        val next_position = if (this.cached_position < this.buffer_size - 1) {
            this.cached_position + 1
        } else {
            this.buffer.position(0)
            0
        }

        val next_value = this.buffer.get()
        this.buffer.position(this.cached_position)

        this.virtual_position += 1

        val weight = pitched_position - pitched_position.toInt()
        val output = this.cached_value!! + ((next_value - this.cached_value!!) * weight).toInt().toShort()

        return output.toShort()
    }

}