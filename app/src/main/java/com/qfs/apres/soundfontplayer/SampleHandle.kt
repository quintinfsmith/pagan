package com.qfs.apres.soundfontplayer

import kotlin.math.PI
import kotlin.math.tan

class SampleHandle(
    var data: ShortArray,
    var sample_rate: Int,
    var attenuation: Double = 0.0,
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

    var working_frame: Int = 0
    var release_frame: Int? = null
    var is_dead = false
    var current_volume: Double = 0.5
    var data_buffer: PitchedBuffer

    // TODO: Unimplimented
    // var release_delay: Int? = null
    // var remove_delay: Int? = null
    //var lpf_previous: Double = 0.0


    init {
        val tmp_tan = tan(PI * this.filter_cutoff / this.sample_rate.toDouble())
        this.lpf_factor = (tmp_tan - 1) / (tmp_tan + 1)
        this.data_buffer = PitchedBuffer(this.data, this.pitch_shift)
    }

    fun set_release_frame(frame: Int) {
        this.release_frame = frame
    }

    fun set_working_frame(frame: Int) {
        this.working_frame = frame
        if (this.release_frame != null && this.working_frame >= this.release_frame!! + this.frame_count_release) {
            this.is_dead = true
            return
        }

        // TODO: Improve this. This is slow and my brain doesn't wont to let me see the algebra here right now
        var tmp_frame = 0
        for (f in 0 until frame) {
            if (this.release_frame == null || this.release_frame!! > f) {
                if (this.loop_points == null || tmp_frame < this.loop_points!!.second) {
                    tmp_frame += 1
                } else if (tmp_frame >= this.loop_points!!.second) {
                    tmp_frame = this.loop_points!!.first
                }
            } else if (tmp_frame < this.data_buffer.size) {
                tmp_frame += 1
            }
        }

        if (tmp_frame < this.data_buffer.size) {
            this.data_buffer.position(tmp_frame)
            this.is_dead = false
        } else {
            this.is_dead = true
        }
    }

    fun get_release_duration(): Int {
        return this.frame_count_release
    }

    fun get_next_frame(): Int? {
        if (this.is_dead) {
            return null
        }

        if (this.data_buffer.position() >= this.data_buffer.size) {
            this.is_dead = true
            return null
        }

        var frame_factor = this.attenuation * this.current_volume
        val is_pressed = this.release_frame == null || this.working_frame < this.release_frame!!

        if (is_pressed) {
            if (this.working_frame < this.frame_count_attack) {
                val r = (this.working_frame).toDouble() / this.frame_count_attack.toDouble()
                frame_factor *= r
            } else if (this.working_frame - this.frame_count_attack < this.frame_count_hold) {
                // pass
            } else if (this.sustain_attenuation < 1.0 && this.working_frame - this.frame_count_attack - this.frame_count_hold < this.frame_count_decay) {
                val r = ((this.working_frame - this.frame_count_hold - this.frame_count_attack).toDouble()  / this.frame_count_decay.toDouble())
                frame_factor *= (1.0 - this.sustain_attenuation) * r
            }
        }

        if (! is_pressed) {
            val current_position_release = (this.working_frame - this.release_frame!!)
            if (current_position_release < this.frame_count_release) {
                frame_factor *= 1.0 - (current_position_release.toDouble() / this.frame_count_release.toDouble())
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

        this.working_frame += 1

        return (this.data_buffer.get().toDouble() * frame_factor).toInt()
    }

    fun release_note() {
        this.set_release_frame(this.working_frame)
    }

    fun is_pressed(): Boolean {
        return this.release_frame == null || this.release_frame!! < this.working_frame
    }
}

