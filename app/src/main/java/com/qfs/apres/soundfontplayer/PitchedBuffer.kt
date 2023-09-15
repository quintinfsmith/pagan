package com.qfs.apres.soundfontplayer

import android.util.Log
import java.nio.BufferUnderflowException
import java.nio.ShortBuffer
import kotlin.math.roundToInt

class PitchedBuffer(data: ShortArray, val pitch: Float) {
    val buffer = ShortBuffer.wrap(data)
    val size = (data.size.toFloat() / this.pitch).toInt()
    var cached_value: Short? = null
    var virtual_position: Int = 0
    init {
        if (this.pitch < 1F) {
            this.cached_value = this.buffer.get()
            this.buffer.position(0)
        }
    }

    fun position(): Int {
        return if (this.pitch >= 1F) {
            (this.buffer.position() / this.pitch).toInt()
        } else {
            this.virtual_position!!
        }
    }

    fun position(index: Int) {
        var pos = (index.toFloat() * this.pitch).toInt()
        if (this.pitch < 1F) {
            this.buffer.position(pos)
            this.cached_value = this.buffer.get()
            this.virtual_position = index
        }
        this.buffer.position(pos)
    }

    fun get(): Short {
        return if (this.pitch >= 1F) {
            val next_position = this.position() + 1
            var value = this.buffer.get()
            while (next_position > this.position()) {
                value = this.buffer.get()
            }
            value
        } else {
            val output = this.cached_value!!
            if (this.virtual_position + 1 < this.size && ((this.virtual_position + 1) * this.pitch).roundToInt() != (this.virtual_position * this.pitch).roundToInt()) {
                this.cached_value = this.buffer.get()
            }

            this.virtual_position += 1

            output
        }
    }
}