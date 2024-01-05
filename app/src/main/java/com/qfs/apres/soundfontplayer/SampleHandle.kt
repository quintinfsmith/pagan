package com.qfs.apres.soundfontplayer

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.tan

class SampleHandle(
    var data: ShortArray,
    var sample_rate: Int,
    var attenuation: Float = 0.0F,
    val loop_points: Pair<Int, Int>?,
    var stereo_mode: Int,
    var delay_frames: Int = 0,
    var attack_frame_count: Double = 0.0,
    var hold_frame_count: Int = 0,
    var decay_frame_count: Double = 0.0,
    var release_size: Float, // Is actually integer, but is only ever used in calculations as Float
    var max_values: Array<Float> = Array<Float>(0) { 0F },
    var pitch_shift: Float = 1F,
    var filter_cutoff: Double = 13500.0,
    var pan: Double = 0.0,
    var sustain_attenuation: Float,
) {

    companion object {
        var uuid_gen = 0
        val MAXIMUM_VOLUME = .8F
    }
    var uuid: Int = SampleHandle.uuid_gen++

    constructor(original: SampleHandle): this(
        original.data,
        original.sample_rate,
        original.attenuation,
        original.loop_points,
        original.stereo_mode,
        original.delay_frames,
        original.attack_frame_count,
        original.hold_frame_count,
        original.decay_frame_count,
        original.release_size,
        original.max_values,
        original.pitch_shift,
        original.filter_cutoff,
        original.pan,
        original.sustain_attenuation
    )

    private val lpf_factor: Double

    var is_pressed = true
    var is_dead = false
    private var current_attack_position: Double = 0.0
    private var current_hold_position: Int = 0
    private var current_decay_position: Double = 0.0

    private var current_release_position: Float = 0F // Is actually integer, but is only ever used in calculations as Float
    var current_volume: Double = 0.5
    var data_buffer = PitchedBuffer(this.data, this.pitch_shift)
    var current_delay_position: Int = 0
    // TODO: Unimplimented
    // var release_delay: Int? = null
    // var remove_delay: Int? = null

    var lpf_previous: Double = 0.0

    init {
        val tmp_tan = tan(PI * this.filter_cutoff / this.sample_rate.toDouble())
        this.lpf_factor = (tmp_tan - 1) / (tmp_tan + 1)
    }

    fun get_next_frame(): Int? {
        if (this.is_dead) {
            return null
        }

        if (this.current_delay_position < this.delay_frames) {
            this.current_delay_position += 1
            return 0
        }

        if (this.data_buffer.position() >= this.data_buffer.size) {
            this.is_dead = true
            return null
        }
        var frame = this.data_buffer.get().toDouble()

        if (this.is_pressed) {
            if (this.current_attack_position < this.attack_frame_count) {
                val r = (this.current_attack_position / this.attack_frame_count)
                frame *= r
                this.current_attack_position += 1
            } else if (this.current_hold_position < this.hold_frame_count) {
                this.current_hold_position += 1
            } else if (this.sustain_attenuation < 1F) {
                val r = min(1.0, (this.current_decay_position / this.decay_frame_count))
                val factor = 1.0 - r + (this.sustain_attenuation * r)
                frame *= factor
                this.current_decay_position += 1
            }
        }

        if (! this.is_pressed) {
            if (this.current_release_position < this.release_size) {
                var r = (this.current_release_position / this.release_size)
                frame *= (1F - r)
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

        // TODO: low pass filter. I can't get this to work atm
        //if (this.filter_cutoff <= this.sample_rate / 2.0) {
        //    var input = frame / Double.MAX_VALUE
        //    val allpass_value = (this.lpf_factor * input) + this.lpf_previous
        //    this.lpf_previous = input - (this.lpf_factor * allpass_value)
        //    frame *= (input + allpass_value) / 2.0
        //}

        return (frame * this.attenuation * this.current_volume).toInt()
    }

    fun release_note() {
        this.is_pressed = false
    }
}

