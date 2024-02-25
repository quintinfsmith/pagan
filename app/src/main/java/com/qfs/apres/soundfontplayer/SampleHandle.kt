package com.qfs.apres.soundfontplayer

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.tan

class SampleHandle(
    var data: ShortArray,
    var sample_rate: Int,
    var initial_attenuation: Double = 0.0,
    val loop_points: Pair<Int, Int>?,
    var stereo_mode: Int,

    var volume_envelope: VolumeEnvelope,
    var modulation_envelope: ModulationEnvelope,
    var modulation_lfo: LFO,

    var pitch_shift: Double = 1.0,
    var filter_cutoff: Double = 13500.0,
    var pan: Double = 0.0,
    var volume: Double = 1.0,
    var linked_handle_count: Int = 1
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
        val pitch: Double,
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
        val HANDLE_VOLUME = .2
        var uuid_gen = 0
    }

    var uuid: Int = SampleHandle.uuid_gen++
    private val lpf_factor: Double

    var working_frame: Int = 0
    var release_frame: Int? = null
    var is_dead = false
    var data_buffer: PitchedBuffer
    val implicit_volume_ratio: Double = 1.0 / this.linked_handle_count.toDouble()

    // TODO: Unimplimented
    // var release_delay: Int? = null
    // var remove_delay: Int? = null
    //var lpf_previous: Double = 0.0

    constructor(original: SampleHandle): this(
        data = original.data,
        sample_rate = original.sample_rate,
        initial_attenuation = original.initial_attenuation,
        loop_points = original.loop_points,
        stereo_mode = original.stereo_mode,
        volume_envelope = original.volume_envelope,
        modulation_envelope = original.modulation_envelope,
        modulation_lfo = original.modulation_lfo,
        pitch_shift = original.pitch_shift,
        filter_cutoff = original.filter_cutoff,
        pan = original.pan,
        volume = original.volume,
        linked_handle_count = original.linked_handle_count
    )

    init {
        val tmp_tan = tan(PI * this.filter_cutoff / this.sample_rate.toDouble())
        this.lpf_factor = (tmp_tan - 1) / (tmp_tan + 1)
        this.data_buffer = PitchedBuffer(this.data, this.pitch_shift)
    }

    fun max_frame_value(): Int {
        return this.data_buffer.max
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
        } else if (this.loop_points != null) {
            if (frame < this.loop_points.second) {
                frame
            } else {
                val loop_size = (this.loop_points.second - this.loop_points.first)
                val loop_remainder = (frame - this.loop_points.first) % loop_size
                this.loop_points.first + loop_remainder
            }
        } else {
            frame
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

        var frame_factor = this.implicit_volume_ratio
        val is_pressed = this.release_frame == null || this.working_frame < this.release_frame!!

        if (this.working_frame < this.volume_envelope.frames_attack) {
            val r = (this.working_frame).toDouble() / this.volume_envelope.frames_attack.toDouble()
            frame_factor *= r * this.initial_attenuation
        } else if (this.working_frame - this.volume_envelope.frames_attack < this.volume_envelope.frames_hold) {
            frame_factor *= this.initial_attenuation
        } else if (this.volume_envelope.sustain_attenuation < 1.0) {
            frame_factor *= this.initial_attenuation
            frame_factor *= if (this.working_frame - this.volume_envelope.frames_attack - this.volume_envelope.frames_hold < this.volume_envelope.frames_decay) {
                val r = 1.0 - ((this.working_frame - this.volume_envelope.frames_hold - this.volume_envelope.frames_attack).toDouble() / this.volume_envelope.frames_decay.toDouble())
                (r * (this.initial_attenuation - this.volume_envelope.sustain_attenuation)) + this.volume_envelope.sustain_attenuation
            } else {
                this.volume_envelope.sustain_attenuation
            }
        }

        if (!is_pressed) {
            val current_position_release = (this.working_frame - this.release_frame!!)
            if (current_position_release < this.volume_envelope.frames_release) {
                frame_factor *= 1.0 - (current_position_release.toDouble() / this.volume_envelope.frames_release.toDouble())
            } else {
                this.is_dead = true
                return null
            }
        }

        if (this.loop_points != null) {
            val offset = this.data_buffer.position() - this.loop_points.second
            if (offset >= 0) {
                this.data_buffer.position(this.loop_points.first + offset)
            }
        }

        if (this.modulation_lfo.delay <= this.working_frame) {
            val lfo_frame = this.modulation_lfo.get_frame(this.working_frame)
            if (this.modulation_lfo.volume != 0.0) {
                frame_factor *= this.modulation_lfo.volume.pow(lfo_frame)
            }
            if (this.modulation_lfo.pitch != 1.0) {
                this.data_buffer.repitch(this.modulation_lfo.pitch.pow(lfo_frame))
            }
        }

        // TODO: low pass filter. I can't get this to work atm
        // if (this.filter_cutoff <= this.sample_rate / 2.0) {
        //    var input = frame / Double.MAX_VALUE
        //    val allpass_value = (this.lpf_factor * input) + this.lpf_previous
        //    this.lpf_previous = input - (this.lpf_factor * allpass_value)
        //    frame *= (input + allpass_value) / 2.0
        // }

        this.working_frame += 1


        return try {
            (this.data_buffer.get().toDouble() * frame_factor * (this.volume * SampleHandle.HANDLE_VOLUME)).toInt()
        } catch (e: ArrayIndexOutOfBoundsException) {
            this.is_dead = true
            null
        }
    }

    fun release_note() {
        this.set_release_frame(this.working_frame)
    }

    fun is_pressed(): Boolean {
        return this.release_frame == null || this.release_frame!! < this.working_frame
    }
    fun get_duration(): Int? {
        return if (this.release_frame == null) {
            null
        } else {
            this.release_frame!! + this.get_release_duration()
        }
    }

}

