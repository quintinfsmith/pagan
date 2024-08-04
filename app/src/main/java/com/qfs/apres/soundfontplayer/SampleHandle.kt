package com.qfs.apres.soundfontplayer

import com.qfs.apres.soundfont.Modulator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.pow
import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToInt

class SampleHandle(
    var data: ShortArray,
    var sample_rate: Int,
    var initial_attenuation: Float = 0F,
    val loop_points: Pair<Int, Int>?,
    var stereo_mode: Int,

    var volume_envelope: VolumeEnvelope,
    var modulation_envelope: ModulationEnvelope,
    var modulation_lfo: LFO,

    var pitch_shift: Float = 1F,
    var filter_cutoff: Float = 13500F,
    var pan: Float = 0F,
    var volume: Float = 1F,
    var linked_handle_count: Int = 1,
    var data_buffer: PitchedBuffer = PitchedBuffer(data, pitch_shift),
    var modulators: Set<Modulator> = setOf()
) {
    var RC = 1f / (this.filter_cutoff * 2f * PI.toFloat())
    val smoothing_factor: Float
    var pitched_loop_points: Pair<Int, Int>? = null
    var pitch_adjustment: Float = 1F

    var previous_frame = 0f
    init {
        val dt =  (1f / this.sample_rate.toFloat())
        this.smoothing_factor = dt / (this.RC + dt)
        this.repitch(1F)
    }


    data class VolumeEnvelope(
        var sample_rate: Int,
        var delay: Float = 0F,
        var attack: Float = 0F,
        var hold: Float = 0F,
        var decay: Float = 0F,
        var release: Float = 0F,
        var sustain_attenuation: Float = 1F
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
            this.frames_delay = (this.sample_rate.toFloat() * this.delay).toInt()
            this.frames_attack = (this.sample_rate.toFloat() * this.attack).toInt()
            this.frames_hold = (this.sample_rate.toFloat() * this.hold).toInt()
            this.frames_decay = (this.sample_rate.toFloat() * this.decay).toInt()
            this.frames_release = (this.sample_rate.toFloat() * this.release).toInt()
        }
    }

    class ModulationEnvelope(
        var sample_rate: Int,
        var delay: Float = 0F,
        var attack: Float = 0F,
        var hold: Float = 0F,
        var decay: Float = 0F,
        var release: Float = 0F,
        var sustain_attenuation: Float = 0F
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
            this.frames_delay = (this.sample_rate.toFloat() * this.delay).toInt()
            this.frames_attack = (this.sample_rate.toFloat() * this.attack).toInt()
            this.frames_hold = (this.sample_rate.toFloat() * this.hold).toInt()
            this.frames_decay = (this.sample_rate.toFloat() * this.decay).toInt()
            this.frames_release = (this.sample_rate.toFloat() * this.release).toInt()
        }
    }

    class LFO(
        var sample_rate: Int,
        val frequency: Float,
        val delay: Float,
        val pitch: Float,
        val filter: Int,
        val volume: Float
    ) {
        val wave_length = sample_rate.toFloat() / this.frequency
        val frames_delay = 0 // (this.sample_rate.toFloat() * this.delay).toInt()

        fun get_frame(i: Int): Float? {
            return if (i < this.frames_delay) {
                null
            } else {
                val x = (i - this.frames_delay).toFloat()
                val divisor = this.wave_length / 4F
                abs((((x + divisor) % this.wave_length) / divisor) - 2F) - 1F
                //sin(x.toFloat() * 2F * PI.toFloat() / this.wave_length).toFloat()
            }
        }
    }

    companion object {
        var uuid_gen = 0
        fun copy(original: SampleHandle): SampleHandle {
            val output = SampleHandle(
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
                linked_handle_count = original.linked_handle_count,
                data_buffer = PitchedBuffer(
                    original.data,
                    original.pitch_shift,
                    original.data_buffer.max
                ), // constructing this way allows us to skip calculating max
                modulators = original.modulators
            )

            output.release_frame = original.release_frame
            output.kill_frame = original.kill_frame
            output.repitch(original.pitch_adjustment)

            return output
        }
    }

    var uuid: Int = SampleHandle.uuid_gen++

    var working_frame: Int = 0
    var release_frame: Int? = null
    var kill_frame: Int? = null
    var is_dead = false

    fun max_frame_value(): Int {
        return this.data_buffer.max
    }

    fun set_release_frame(frame: Int) {
        this.release_frame = frame
    }

    private fun calc_adj_frame(frame: Int): Int {
        val loop_points = this.pitched_loop_points
        return if (this.release_frame == null) {
            if (loop_points == null || frame < loop_points.second) {
                frame
            } else {
                val loop_size = (loop_points.second - loop_points.first)
                val loops = ((frame - loop_points.first) / loop_size)
                val loop_remainder = (frame - loop_points.first) % loop_size
                loop_points.first + (loops * loop_size) + loop_remainder
            }
        } else if (loop_points != null && loop_points.first < loop_points.second) {
            if (frame < loop_points.second) {
                frame
            } else {
                val loop_size = (loop_points.second - loop_points.first)
                val loop_remainder = (frame - loop_points.first) % loop_size
                loop_points.first + loop_remainder
            }
        } else {
            frame
        }
    }

    fun set_working_frame(frame: Int) {
        this.working_frame = frame
        if (this.kill_frame != null && this.working_frame >= this.kill_frame!!) {
            this.is_dead = true
            return
        }
        if (this.release_frame != null && this.working_frame >= this.release_frame!! + this.volume_envelope.frames_release) {
            this.is_dead = true
            return
        }
        val adj_frame = this.calc_adj_frame(frame)

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

        var frame_factor = 1F / this.linked_handle_count
        val is_pressed = this.release_frame == null || this.working_frame < this.release_frame!!

        if (this.working_frame < this.volume_envelope.frames_attack) {
            val r = (this.working_frame).toFloat() / this.volume_envelope.frames_attack.toFloat()
            frame_factor /= 10F.pow(r * this.initial_attenuation)
        } else if (this.working_frame - this.volume_envelope.frames_attack < this.volume_envelope.frames_hold) {
            frame_factor /= 10F.pow(this.initial_attenuation)
        } else if (this.volume_envelope.sustain_attenuation < 1F) {
            frame_factor /= 10F.pow(this.initial_attenuation)
            val relative_frame = this.working_frame - this.volume_envelope.frames_attack - this.volume_envelope.frames_hold
            frame_factor /= if (relative_frame < this.volume_envelope.frames_decay) {
                val r = 1F - (relative_frame.toFloat() / this.volume_envelope.frames_decay.toFloat())
                10F.pow(this.volume_envelope.sustain_attenuation * r)
            } else {
                10F.pow(this.volume_envelope.sustain_attenuation)
            }
        }

        if (!is_pressed) {
            if (this.data_buffer.is_overflowing()) {
                this.is_dead = true
                return null
            }

            val release_frame_count = min(
                this.volume_envelope.frames_release,
                this.data_buffer.size - this.release_frame!!
            )

            val current_position_release = this.working_frame - this.release_frame!!

            if (current_position_release < release_frame_count) {
                frame_factor *= 1F - (current_position_release.toFloat() / release_frame_count.toFloat())
            } else {
                this.is_dead = true
                return null
            }

        } else if (this.pitched_loop_points != null) {
            val offset = this.data_buffer.position() - this.pitched_loop_points!!.second
            if (offset >= 0) {
                this.data_buffer.position(this.pitched_loop_points!!.first + offset)
            }
        }

        if (this.modulation_lfo.delay <= this.working_frame) {
            val lfo_frame = this.modulation_lfo.get_frame(this.working_frame) 
            if (lfo_frame != null) {
                if (this.modulation_lfo.volume != 0F) {
                    frame_factor *= 10F.pow(this.modulation_lfo.volume * ((lfo_frame + 1F) / 2F))
                }

                // TODO: This is mostly functional but still getting clicking because of imperfect re-calculation of the loop points
                //if (this.modulation_lfo.pitch != 1F) {
                //    val diff = modulation_lfo.pitch - 1F
                //    val new_pitch = 1F + ((lfo_frame + 1F) * diff / 2F)
                //    this.repitch(new_pitch)
                //}
            }
        }

        this.working_frame += 1

        var frame_value = try {
            this.data_buffer.get().toFloat()
        } catch (e: ArrayIndexOutOfBoundsException) {
            this.is_dead = true
            return null
        }

        // Low Pass Filtering
        frame_value = this.previous_frame + (this.smoothing_factor * (frame_value - this.previous_frame))

        this.previous_frame = frame_value
        return (frame_value * frame_factor * this.volume).toInt()
    }

    fun release_note() {
        this.set_release_frame(this.working_frame)
    }

    fun set_kill_frame(f: Int) {
        this.kill_frame = f
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

    fun repitch(adjustment: Float) {
        this.data_buffer.repitch(adjustment)
        if (this.loop_points == null) {
            return
        }

        val new_pitch = this.pitch_shift * adjustment

        this.pitched_loop_points = Pair(
            (this.loop_points.first.toFloat() / new_pitch).toInt(),
            (this.loop_points.second.toFloat() / new_pitch).toInt()
        )

        /*
         * NOTE: sometimes the pitching causes the loop points to be unatainable, so add an offset when that occurs to
         * try to alay misalignments (eg loop point @14, pitch x3. mod = 4)
         */

        val closest_int: Int = (this.pitched_loop_points!!.second * new_pitch).toInt()
        this.data_buffer.set_minor_offset(this.loop_points.second - closest_int)
    }
}

