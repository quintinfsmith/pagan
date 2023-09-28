package com.qfs.apres.soundfontplayer

import android.util.Log
import java.lang.Math.PI
import java.lang.Math.tan
import java.nio.ShortBuffer
import kotlin.math.roundToInt

class SampleHandle(
    var data: ShortArray,
    var attenuation: Float = 0.0F,
    val loop_points: Pair<Int, Int>?,
    var stereo_mode: Int,
    var delay_frames: Int = 0,
    var attack_frame_count: Int = 0,
    var hold_frame_count: Int = 0,
    var decay_frame_count: Int = 0,
    var release_mask: Array<Double>,
    var max_values: Array<Float> = Array<Float>(0) { 0F },
    var pitch_shift: Float = 1F,
    var lfo_data: ShortArray?,
    var filter_cutoff: Int? = null,
    var pan: Double = 0.0
) {
    companion object {
        val MAXIMUM_VOLUME = .8F
    }

    constructor(original: SampleHandle): this(
        original.data,
        original.attenuation,
        original.loop_points,
        original.stereo_mode,
        original.delay_frames,
        original.attack_frame_count,
        original.hold_frame_count,
        original.decay_frame_count,
        original.release_mask,
        original.max_values,
        original.pitch_shift,
        original.lfo_data,
        original.filter_cutoff,
        original.pan
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
    var data_buffer = PitchedBuffer(this.data, this.pitch_shift)
    var lfo_buffer: ShortBuffer? = if (this.lfo_data == null) { null } else { ShortBuffer.wrap(this.lfo_data) }
    var lpf_previous: Double = 0.0


    fun get_max_value(): Float {
        var i = this.data_buffer.position() * this.max_values.size / this.data_buffer.size
        return this.max_values[i] * this.current_volume.toFloat()
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

        if (this.data_buffer.position() >= this.data_buffer.size) {
            this.is_dead = true
            return null
        }
        var frame = (this.data_buffer.get().toDouble() * this.attenuation * this.current_volume).toInt().toShort()
        var lfo_frame = this.lfo_buffer?.get() ?: 0.toShort()
        if (this.lfo_buffer != null && this.lfo_buffer!!.position() >= this.lfo_data!!.size) {
            this.lfo_buffer!!.position(0)
        }
        frame = (frame + lfo_frame).toShort()

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
            val offset = this.data_buffer.position() - this.loop_points.second
            if (offset >= 0) {
                this.data_buffer.position(this.loop_points.first + offset)
            }
        }

        // low pass filter
        if (this.filter_cutoff != null) {
            val tan_val = tan(PI * this.filter_cutoff!!.toFloat() / AudioTrackHandle.sample_rate.toFloat())
            val lpf_tmp = frame.toDouble()
            val a = ((tan_val - 1) / (tan_val + 1))
            frame = (a * frame.toDouble() + this.lpf_previous).roundToInt().toShort()
            this.lpf_previous = lpf_tmp - (a * frame.toDouble())
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

