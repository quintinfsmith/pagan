package com.qfs.apres.soundfontplayer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PitchedBuffer(val data: ShortArray, var pitch: Float, known_max: Int? = null, range: IntRange? = null, var is_loop: Boolean = false) {
    class PitchedBufferOverflow : Exception()
    val ptr: Long

    val max: Float
        get() = this.get_max(this.ptr)

    val size: Int
        get() = this.get_virtual_size(this.ptr)

    var position: Int
        get() = this.get_virtual_position(this.ptr)
        set(value: Int) = this.set_virtual_position(this.ptr, value)

    //var _range = range ?: 0 until data.size

    private var virtual_position: Int = 0
    var pitch_adjustment: Float = 1F


    external fun create(data: ShortArray, data_size: Int, pitch: Float, max_known: Boolean, max: Float, start: Int, end: Int, is_loop: Boolean): Long
    external fun get_max(ptr: Long): Float
    external fun get_virtual_size(ptr: Long): Int
    external fun is_overflowing_inner(ptr: Long): Boolean
    external fun repitch_inner(ptr: Long, new_pitch_adjustment: Float)
    external fun get_virtual_position(ptr: Long): Int
    external fun set_virtual_position(ptr: Long, new_position: Int)
    external fun get(ptr: Long): Float

    init {
        this.ptr = PitchedBuffer.create(
            this.data,
            this.data.size,
            this.pitch,
            known_max != null,
            known_max ?: max(abs(data.min().toInt()), data.max().toInt()),
            range?.first ?: 0,
            range?.last ?: data.size,
            this.is_loop
        )
    }

    fun is_overflowing(): Boolean {
        return this.is_overflowing_inner(this.ptr)
    }

    fun repitch(new_pitch_adjustment: Float) {
        this.repitch_inner(this.ptr, new_pitch_adjustment)
    }

    fun reset_pitch() {
        this.repitch(1F)
    }

    private fun _get_real_frame(i: Float): Short {
        var range_size = this._range.last + 1 - this._range.first
        var adj_i = min(this._range.last, this._range.first + if (this.is_loop) {
            i.toInt() % range_size
        } else if (i >= range_size) {
            throw PitchedBufferOverflow()
        } else {
            i.toInt()
        })
        return this.data[adj_i]
    }

    fun get(): Float {
        val pitch = this.get_calculated_pitch()
        val position = (this.virtual_position++).toFloat() * pitch
        var output = this._get_real_frame(position)

        return output.toFloat() / Short.MAX_VALUE.toFloat()
    }
}
