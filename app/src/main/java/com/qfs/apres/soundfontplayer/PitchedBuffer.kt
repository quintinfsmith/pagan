package com.qfs.apres.soundfontplayer

import android.util.Log
import java.nio.ShortBuffer

class PitchedBuffer(data: ShortArray, val pitch: Float) {
    val buffer = ShortBuffer.wrap(data)
    val size = (data.size.toFloat() / this.pitch).toInt()
    fun position(): Int {
        return (this.buffer.position() / this.pitch).toInt()
    }

    fun position(index: Int) {
        this.buffer.position((index.toFloat() * this.pitch).toInt())
    }

    fun get(): Short {
        val next_position = this.position() + 1
        var value = this.buffer.get()
        try {
            while (next_position > this.position()) {
                value = this.buffer.get()
            }
        } catch (e: Exception) {
            Log.e("AAA", "V: $next_position | ")
            throw e
        }
        return value
    }
}