package com.qfs.pagan.apres.SoundFontPlayer

import com.qfs.pagan.apres.riffreader.toUInt
import kotlin.math.max
import kotlin.math.min

class SampleHandle(
    var data: ByteArray,
    val loop_points: Pair<Int, Int>?,
    var stereo_mode: Int,
    var delay_frames: Int = 0,
    var attack_byte_count: Int = 0,
    var hold_byte_count: Int = 0,
    var decay_byte_count: Int = 0,
    var release_mask: Array<Double>,
    var maximum_map: Array<Int>
) {

    constructor(original: SampleHandle): this(
        original.data,
        original.loop_points,
        original.stereo_mode,
        original.delay_frames,
        original.attack_byte_count,
        original.hold_byte_count,
        original.decay_byte_count,
        original.release_mask,
        original.maximum_map
    )

    var is_pressed = true
    private var is_dead = false
    private var current_position: Int = 0
    private var current_attack_position: Int = 0
    private var current_hold_position: Int = 0
    private var current_decay_position: Int = 0
    var current_delay_position: Int = 0
    var decay_position: Int? = null
    var sustain_volume: Int = 0 // TODO
    private var current_release_position: Int = 0
    var current_volume: Double = 0.5
    private var bytes_called: Int = 0 // Will not loop like current_position
    // Kludge to handle high tempo songs
    private val minimum_duration: Int = (AudioTrackHandle.sample_rate * .3).toInt()

    private fun get_max_in_range(x: Int, size: Int): Int {
        var index = min(
            this.maximum_map.size - 1,
            x * this.maximum_map.size / (this.data.size / 2)
        )
        val mapped_size =  size * this.maximum_map.size / (this.data.size / 2)
        var output = 0
        for (i in 0 until mapped_size) {
            output = max(output, this.maximum_map[index])
            index = (index + 1) % this.maximum_map.size
        }
        return output
    }

    fun get_next_max(buffer_size: Int): Int {
        return (this.get_max_in_range(this.current_position / 2, buffer_size).toDouble() * this.current_volume).toInt()
    }

    fun get_next_frame(): Short? {
        if (this.is_dead) {
            return null
        }
        //if (this.current_delay_position < this.delay_frames) {
        //    var output = 0.toShort()
        //    this.current_delay_position += 1
        //    return output
        //}

        if (this.current_position > this.data.size - 2) {
            this.is_dead = true
            return null
        }

        val a = toUInt(this.data[this.current_position])
        val b = toUInt(this.data[this.current_position + 1]) * 256
        var frame: Short = (a + b).toShort()

        frame = (frame.toDouble() * this.current_volume).toInt().toShort()

        this.current_position += 2
        this.bytes_called += 2
        if (this.current_attack_position < this.attack_byte_count) {
            this.current_attack_position += 2
        } else if (this.current_hold_position < this.hold_byte_count) {
            this.current_hold_position += 2
        } else if (this.current_decay_position < this.decay_byte_count) {
            this.current_decay_position += 2
        }

        if (! this.is_pressed && this.bytes_called >= this.minimum_duration - this.release_mask.size) {
        //if (! this.is_pressed) {
            if (this.current_release_position < this.release_mask.size) {
                frame = (frame * this.release_mask[this.current_release_position]).toInt().toShort()
                this.current_release_position += 1
            } else {
                this.is_dead = true
                return null
            }
        } else if (this.loop_points != null) {
            if (this.current_position >= this.loop_points.second * 2) {
                this.current_position = this.loop_points.first * 2
            }
        }

        return frame
    }

    fun release_note() {
        this.is_pressed = false
    }
    fun kill_note() {
        this.is_dead = true
    }
}

