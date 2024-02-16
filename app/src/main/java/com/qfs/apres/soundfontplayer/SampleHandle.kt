package com.qfs.apres.soundfontplayer

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.tan

class SampleHandle(
    var data: ShortArray,
    var sample_rate: Int,
    var attenuation: Double = 0.0,
    val loop_points: Pair<Int, Int>?,
    var stereo_mode: Int,

    var volume_envelope: VolumeEnvelope,
    var modulation_envelope: ModulationEnvelope,
    var modulation_lfo: LFO,

    var max_values: Array<Float> = Array<Float>(0) { 0F },
    var pitch_shift: Double = 1.0,
    var filter_cutoff: Double = 13500.0,
    var pan: Double = 0.0,
) {
    data class VolumeEnvelope(
        var sample_rate: Int,
        var delay: Double = 0.0,
        var attack: Double = 0.0,
        var hold: Double = 0.0,
        var decay: Double = 0.0,
        var release: Double = 0.0,
        var sustain_attenuation: Double = 1.0
    ) {
        var frames_delay: Int = 0
        var frames_attack: Int = 0
        var frames_hold: Int = 0
        var frames_decay: Int = 0
        var frames_release: Int = 0

        init {
            this.set_sample_rate(this.sample_rate)
        }

        fun set_sample_rate(sample_rate: Int) {
            this.sample_rate = sample_rate
            this.frames_delay = (this.sample_rate.toDouble() * this.delay).toInt()
            this.frames_attack = (this.sample_rate.toDouble() * this.attack).toInt()
            this.frames_hold = (this.sample_rate.toDouble() * this.hold).toInt()
            this.frames_decay = (this.sample_rate.toDouble() * this.decay).toInt()
            this.frames_release = (this.sample_rate.toDouble() * this.release).toInt()
        }
    }

    class ModulationEnvelope(
        var sample_rate: Int,
        var delay: Double = 0.0,
        var attack: Double = 0.0,
        var hold: Double = 0.0,
        var decay: Double = 0.0,
        var release: Double = 0.0,
        var sustain_attenuation: Double = 0.0
    ) {
        var frames_delay: Int = 0
        var frames_attack: Int = 0
        var frames_hold: Int = 0
        var frames_decay: Int = 0
        var frames_release: Int = 0

        init {
            this.set_sample_rate(this.sample_rate)
        }

        fun set_sample_rate(sample_rate: Int) {
            this.sample_rate = sample_rate
            this.frames_delay = (this.sample_rate.toDouble() * this.delay).toInt()
            this.frames_attack = (this.sample_rate.toDouble() * this.attack).toInt()
            this.frames_hold = (this.sample_rate.toDouble() * this.hold).toInt()
            this.frames_decay = (this.sample_rate.toDouble() * this.decay).toInt()
            this.frames_release = (this.sample_rate.toDouble() * this.release).toInt()
        }
    }

    class LFO(
        var sample_rate: Int,
        val frequency: Double,
        val delay: Double,
        val pitch: Int,
        val filter: Int,
        val volume: Double
    ) {
        val wave_length = sample_rate.toDouble() / this.frequency
        val frames_delay = (this.sample_rate.toDouble() * this.delay)

        fun get_frame(i: Int): Double {
            return if (i < this.frames_delay) {
                0.0
            } else {
                val x = (i - this.frames_delay)
                (abs((x % this.wave_length) - this.wave_length) - (this.wave_length / 2)) / this.wave_length
            }
        }
    }


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
        volume_envelope = original.volume_envelope,
        modulation_envelope = original.modulation_envelope,
        modulation_lfo = original.modulation_lfo,
        max_values = original.max_values,
        pitch_shift = original.pitch_shift,
        filter_cutoff = original.filter_cutoff,
        pan = original.pan
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
        if (this.release_frame != null && this.working_frame >= this.release_frame!! + this.volume_envelope.frames_release) {
            this.is_dead = true
            return
        }

        val adj_frame = if (this.release_frame == null) {
            if (this.loop_points == null || frame < this.loop_points.second) {
                frame
            } else {
                val loop_size = (this.loop_points.second - this.loop_points.first)
                val loops = ((frame - this.loop_points.first) / loop_size)
                val loop_remainder = (frame - this.loop_points.first) % loop_size
                this.loop_points.first + (loops * loop_size) + loop_remainder
            }
        } else {
            if (this.loop_points == null || this.release_frame!! < this.loop_points.second) {
                min(frame, this.release_frame!! + this.volume_envelope.frames_release)
            } else {
                val loop_size = (this.loop_points.second - this.loop_points.first)
                val loops = ((frame - this.loop_points.first) / loop_size)
                val loop_remainder = (frame - this.loop_points.first) % loop_size

               min(this.loop_points.first + (loops * loop_size) + loop_remainder, this.loop_points.second + this.volume_envelope.frames_release)
            }
        }

        if (adj_frame < this.data_buffer.size) {
            this.data_buffer.position(adj_frame)
            this.is_dead = false
        } else {
            this.is_dead = true
        }
    }

    fun get_release_duration(): Int {
        return this.volume_envelope.frames_release
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
            if (this.working_frame < this.volume_envelope.frames_attack) {
                val r = (this.working_frame).toDouble() / this.volume_envelope.frames_attack.toDouble()
                frame_factor *= r
            } else if (this.working_frame - this.volume_envelope.frames_attack < this.volume_envelope.frames_hold) {
                // pass
            } else if (this.volume_envelope.sustain_attenuation < 1.0) {
                frame_factor *= if (this.working_frame - this.volume_envelope.frames_attack - this.volume_envelope.frames_hold < this.volume_envelope.frames_decay) {
                    val r = 1.0 - ((this.working_frame - this.volume_envelope.frames_hold - this.volume_envelope.frames_attack).toDouble() / this.volume_envelope.frames_decay.toDouble())
                    this.volume_envelope.sustain_attenuation + (r * (1.0 - this.volume_envelope.sustain_attenuation))
                } else {
                    this.volume_envelope.sustain_attenuation
                }
            }
        }

        if (! is_pressed) {
            val current_position_release = (this.working_frame - this.release_frame!!)
            if (current_position_release < this.volume_envelope.frames_release) {
                frame_factor *= 1.0 - (current_position_release.toDouble() / this.volume_envelope.frames_release.toDouble())
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

        if (this.modulation_lfo.delay <= this.working_frame) {
            if (this.modulation_lfo.volume != 0.0) {
                frame_factor *= this.modulation_lfo.volume.pow(
                    this.modulation_lfo.get_frame(this.working_frame)
                )
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

