package com.qfs.apres.SoundFontPlayer

import com.qfs.apres.riffreader.toUInt
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.max
import kotlin.math.min

class SampleHandle(
    var data: ShortArray,
    val loop_points: Pair<Int, Int>?,
    var stereo_mode: Int,
    var delay_frames: Int = 0,
    var attack_frame_count: Int = 0,
    var hold_frame_count: Int = 0,
    var decay_frame_count: Int = 0,
    var release_mask: Array<Double>,
    var maximum_map: Array<Int>
) {
    companion object {
        val MAXIMUM_VOLUME = .6F
    }

    constructor(original: SampleHandle): this(
        original.data,
        original.loop_points,
        original.stereo_mode,
        original.delay_frames,
        original.attack_frame_count,
        original.hold_frame_count,
        original.decay_frame_count,
        original.release_mask,
        original.maximum_map
    )

    var is_pressed = true
    private var is_dead = false
    private var current_attack_position: Int = 0
    private var current_hold_position: Int = 0
    private var current_decay_position: Int = 0

//    var current_delay_position: Int = 0
    var decay_position: Int? = null
    var sustain_volume: Int = 0 // TODO
    private var current_release_position: Int = 0
    var current_volume: Double = 0.5
    private var shorts_called: Int = 0 // running total
    var release_delay: Int? = null
    var remove_delay: Int? = null
    var data_buffer = ShortBuffer.wrap(data)


    fun get_next_frame(): Short? {
        if (this.is_dead) {
            return null
        }

        //if (this.current_delay_position < this.delay_frames) {
        //    var output = 0.toShort()
        //    this.current_delay_position += 1
        //    return output
        //}

        if (this.data_buffer.position() >= this.data.size) {
            this.is_dead = true
            return null
        }

        var frame = (this.data_buffer.get().toDouble() * this.current_volume).toInt().toShort()

        this.shorts_called += 1
        if (this.current_attack_position < this.attack_frame_count) {
            this.current_attack_position += 1
        } else if (this.current_hold_position < this.hold_frame_count) {
            this.current_hold_position += 1
        } else if (this.current_decay_position < this.decay_frame_count) {
            this.current_decay_position += 1
        }

        if (! this.is_pressed) {
            if (this.current_release_position < this.release_mask.size) {
                frame = (frame * this.release_mask[this.current_release_position]).toInt().toShort()
                this.current_release_position += 1
            } else {
                this.is_dead = true
                return null
            }
        } else if (this.loop_points != null) {
            if (this.data_buffer.position() >= this.loop_points.second) {
                this.data_buffer.position(this.loop_points.first)
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

