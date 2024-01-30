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
    var frame_count_delay: Int = 0,
    var frame_count_attack: Int = 0,
    var frame_count_hold: Int = 0,
    var frame_count_decay: Int = 0,
    var frame_count_release: Int = 0,
    var max_values: Array<Float> = Array<Float>(0) { 0F },
    var pitch_shift: Double = 1.0,
    var filter_cutoff: Double = 13500.0,
    var pan: Double = 0.0,
    var sustain_attenuation: Double = 1.0,
) {
    companion object {
        var uuid_gen = 0
        val MAXIMUM_VOLUME = .8F
    }

    var uuid: Int = SampleHandle.uuid_gen++

    constructor(original: SampleHandle): this(
        data = original.data,
        sample_rate = original.sample_rate,
        attenuation = original.attenuation,
        loop_points = original.loop_points,
        stereo_mode = original.stereo_mode,
        frame_count_delay = original.frame_count_delay,
        frame_count_attack = original.frame_count_attack,
        frame_count_hold = original.frame_count_hold,
        frame_count_decay = original.frame_count_decay,
        frame_count_release = original.frame_count_release,
        max_values = original.max_values,
        pitch_shift = original.pitch_shift,
        filter_cutoff = original.filter_cutoff,
        pan = original.pan,
        sustain_attenuation = original.sustain_attenuation
    )

    private val lpf_factor: Double

    var is_pressed = true
    var is_dead = false
    var current_volume: Double = 0.5
    var data_buffer: PitchedBuffer

    private var current_position_attack: Double = 0.0
    private var current_position_hold: Double = 0.0
    private var current_position_decay: Double = 0.0
    private var current_position_release: Double = 0.0
    private var current_position_delay: Double = 0.0

    private var increment_attack: Double = 1.0 / this.frame_count_attack
    private var increment_decay: Double = 1.0 / this.frame_count_decay
    private var increment_release: Double = 1.0 / this.frame_count_release


    // TODO: Unimplimented
    // var release_delay: Int? = null
    // var remove_delay: Int? = null
    //var lpf_previous: Double = 0.0

    init {
        val tmp_tan = tan(PI * this.filter_cutoff / this.sample_rate.toDouble())
        this.lpf_factor = (tmp_tan - 1) / (tmp_tan + 1)
        this.data_buffer = PitchedBuffer(this.data, this.pitch_shift)
    }

    fun get_next_frame(): Int? {
        if (this.is_dead) {
            return null
        }

        if (this.current_position_delay < this.frame_count_delay) {
            this.current_position_delay += 1
            return 0
        }

        if (this.data_buffer.position() >= this.data_buffer.size) {
            this.is_dead = true
            return null
        }

        var frame_factor = this.attenuation * this.current_volume

        if (this.is_pressed) {
            if (this.current_position_attack < 1.0) {
                frame_factor *= this.current_position_attack
                this.current_position_attack += this.increment_attack
            } else if (this.current_position_hold < this.frame_count_hold) {
                this.current_position_hold += 1.0
            } else if (this.sustain_attenuation < 1.0) {
                val r = min(1.0, this.current_position_decay)
                frame_factor *= (1.0 - r) + (this.sustain_attenuation * r)
                this.current_position_decay += this.increment_decay
            }
        }

        if (! this.is_pressed) {
            if (this.current_position_release < 1.0) {
                frame_factor *= 1.0 - this.current_position_release
                this.current_position_release += this.increment_release
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

        return (this.data_buffer.get().toDouble() * frame_factor).toInt()
    }

    fun release_note() {
        this.is_pressed = false
    }
}

