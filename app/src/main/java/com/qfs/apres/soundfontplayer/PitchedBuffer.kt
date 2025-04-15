package com.qfs.apres.soundfontplayer

import kotlin.math.abs
import kotlin.math.max

class PitchedBuffer(var ptr: Long) {
    constructor(data: ShortArray, pitch: Float, known_max: Int? = null, range: IntRange? = null, is_loop: Boolean = false): this(
        create(
            data,
            data.size,
            pitch,
            known_max ?: max(abs(data.min().toInt()), data.max().toInt()),
            range?.first ?: 0,
            range?.last ?: data.size,
            is_loop
        )
    )

    companion object {
        init {
            System.loadLibrary("pagan")
        }
        external fun create(data: ShortArray, data_size: Int, pitch: Float, max: Int, start: Int, end: Int, is_loop: Boolean): Long
    }

    class PitchedBufferOverflow : Exception()

    val max: Int
        get() = get_max(this.ptr)

    val size: Int
        get() = get_virtual_size(this.ptr)

    val position: Int
        get() = get_virtual_position(this.ptr)

    //var _range = range ?: 0 until data.size

    private var virtual_position: Int = 0
    var pitch_adjustment: Float = 1F

    external fun get_max(ptr: Long): Int
    external fun get_range_inner(ptr: Long, output: Array<Int>)
    external fun get_virtual_size(ptr: Long): Int
    external fun is_overflowing_inner(ptr: Long): Boolean
    external fun is_loop(ptr: Long): Boolean
    external fun repitch_inner(ptr: Long, new_pitch_adjustment: Float)
    external fun get_virtual_position(ptr: Long): Int
    external fun set_virtual_position(ptr: Long, new_position: Int)
    external fun get_inner(ptr: Long): Float
    external fun copy_inner(ptr: Long): Long

    fun get_range(): IntRange {
        var array = Array<Int>(2) { 0 }
        get_range_inner(this.ptr, array)
        // TODO: Double check '..' or 'until'
        return array[0] .. array[1]
    }

    fun is_overflowing(): Boolean {
        return is_overflowing_inner(this.ptr)
    }

    fun repitch(new_pitch_adjustment: Float) {
        repitch_inner(this.ptr, new_pitch_adjustment)
    }

    fun reset_pitch() {
        this.repitch(1F)
    }

    fun set_position(value: Int) {
        set_virtual_position(this.ptr, value)
    }

    fun get(): Float {
        return get_inner(this.ptr)
    }

    fun copy(): PitchedBuffer {
        return PitchedBuffer(copy_inner(this.ptr))
    }
}