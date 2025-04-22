package com.qfs.apres.soundfontplayer

import com.qfs.apres.soundfont.Generator.Operation
import com.qfs.apres.soundfont.Modulator
import com.qfs.apres.soundfont.SampleType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class SampleHandle(val ptr: Long) {
    constructor(
        data: ShortArray,
        sample_rate: Int,
        initial_attenuation: Float = 0F,
        loop_points: Pair<Int, Int>?,
        stereo_mode: Int,

        volume_envelope: VolumeEnvelope,

        pitch_shift: Float = 1F,
        filter_cutoff: Float = 13500F,
        pan: Float = 0F,
        volume_profile: ProfileBuffer? = null,
        pan_profile: ProfileBuffer? = null,
        data_buffers: Array<PitchedBuffer>? = null,

        // TODO: Modulations
        //modulation_envelope: ModulationEnvelope,
        //modulation_lfo: LFO?,
        //modulators: HashMap<Operation, Set<Modulator>> = hashMapOf()
    ): this(
        create(
            data,
            sample_rate,
            initial_attenuation,
            loop_points != null,
            loop_points?.first ?: 0,
            loop_points?.second ?: 0,
            stereo_mode,
            volume_envelope.ptr,
            pitch_shift,
            filter_cutoff,
            pan,
            volume_profile?.ptr ?: 0,
            pan_profile?.ptr ?: 0,
            data_buffers

            //modulation_envelope,
            //modulation_lfo,
            //modulators
        )
    )

    companion object {
        external fun create(
            data: ShortArray,
            sample_rate: Int,
            initial_attenuation: Float,
            is_loop: Boolean,
            loop_start: Int,
            loop_end: Int,
            stereo_mode: Int,
            volume_envelope_ptr: Long,
            pitch_shift: Float,
            filter_cutoff: Float,
            pan: Float,
            volume_profile_ptr: Long,
            pan_profile_ptr: Long,
        ): Long
    }


    class ProfileBuffer(val frames: Array<Pair<Int, Pair<Float, Float>>>, val start_frame: Int, skip_initial_set: Boolean = false) {
        var current_frame: Int = 0
        var current_index: Int = 0
        var current_value: Float = 0f
        var next_frame_trigger: Int = -1

        init {
            if (!skip_initial_set) {
                this.set_frame(0)
            }
        }

        fun get_next(): Float {
            val (frame, working_data) = this.frames[this.current_index]

            if (frame == this.current_frame) {
                this.current_value = working_data.first
            } else {
                this.current_value += working_data.second
            }

            val output = this.current_value

            this._move_to_next_frame()

            return output
        }

        private fun _move_to_next_frame() {
            this.current_frame += 1
            val current_frame = this.current_frame
            if (current_frame == this.next_frame_trigger) {
                if (this.current_index == this.frames.size - 1) {
                    this.next_frame_trigger = -1
                } else {
                    this.next_frame_trigger = this.frames[this.current_index++].first
                }
            }
        }

        fun set_frame(frame: Int) {
            val original_frame = this.current_frame

            this.current_frame = frame + this.start_frame

            if (original_frame == this.current_frame) {
                return
            } else if (original_frame < this.current_frame) {
                while (this.current_index < this.frames.size - 1) {
                    if (this.frames[this.current_index + 1].first <= this.current_frame) {
                        this.current_index += 1
                    } else {
                        break
                    }
                }
            } else {
                while (this.current_index > 0 && this.frames[this.current_index].first > this.current_frame) {
                    this.current_index -= 1
                }
            }

            this.next_frame_trigger = if (this.current_index < this.frames.size - 1) {
                this.frames[this.current_index + 1].first
            } else {
                -1
            }

            val working_frame = this.current_frame - 1
            var frame_data = this.frames[this.current_index]
            this.current_value = frame_data.second.first
            if (frame_data.second.second != 0F) {
                this.current_value += (working_frame - frame_data.first).toFloat() * frame_data.second.second
            }
        }

        fun copy(): ProfileBuffer {
            return ProfileBuffer(
                this.frames,
                this.start_frame
            )
        }
    }

    class VolumeEnvelope(val ptr: Long) {
        constructor(
            sample_rate: Int,
            delay: Float = 0F,
            attack: Float = 0F,
            hold: Float = 0F,
            decay: Float = 0F,
            release: Float = 0F,
            sustain_attenuation: Float = 0F
        ): this(
            create(
                sample_rate,
                delay,
                attack,
                hold,
                decay,
                release,
                sustain_attenuation
            )
        )

        companion object {
            external fun create(
                sample_rate: Int,
                delay: Float,
                attack: Float,
                hold: Float,
                decay: Float,
                release: Float,
                sustain_attenuation: Float
            ): Long
        }


        //sample_rate: Int,
        //delay: Float = 0F,
        //attack: Float = 0F,
        //hold: Float = 0F,
        //decay: Float = 0F,
        //release: Float = 0F,
        //sustain_attenuation: Float = 0F
        var frames_delay: Int = 0
        var frames_attack: Int = 0
        var frames_hold: Int = 0
        var frames_decay: Int = 0
        var frames_release: Int = 0

        external fun set_sample_rate_jni(ptr: Long, rate: Int)
        fun set_sample_rate(sample_rate: Int) {
            this.set_sample_rate_jni(this.ptr, sample_rate)
        }
    }

    data class ModulationEnvelope(
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

    external fun copy_jni(ptr: Long): Long
    fun copy(): SampleHandle {
        return SampleHandle(this.copy_jni(this.ptr))
    }

    external fun set_release_frame_jni(ptr: Long, frame: Int)
    fun set_release_frame(frame: Int) {
        this.set_release_frame_jni(this.ptr, frame)
    }

    external fun set_working_frame_jni(ptr: Long, frame: Int)
    fun set_working_frame(frame: Int) {
        this.set_working_frame_jni(this.ptr, frame)
    }

    external fun get_release_duration_jni(ptr: Long): Int
    fun get_release_duration(): Int {
        return this.get_release_duration_jni(ptr)
    }

    external fun get_next_balance_jni(ptr: Long): FloatArray
    fun get_next_balance(): Pair<Float, Float> {
        val array = this.get_next_balance_jni(this.ptr)
        return Pair(
            array[0],
            array[1]
        )
    }

    external fun get_next_frame_jni(ptr: Long): FloatArray
    fun get_next_frame(): Pair<Float, Float>? {
        val frame_array = this.get_next_frame_jni(this.ptr)
        return if (frame_array[2] == 0F) {
            null
        } else {
            Pair(
                frame_array[0],
                frame_array[1]
            )
        }
    }

    external fun release_note_jni(ptr: Long)
    fun release_note() {
        this.release_note(this.ptr)
    }

    external fun set_kill_frame_jni(ptr: Long, frame: Int)
    fun set_kill_frame(f: Int) {
        this.set_kill_frame_jni(this.ptr, f)
    }


    external fun repitch_jni(ptr: Long, new_pitch: Float)
    fun repitch(adjustment: Float) {
        this.repitch_jni(this.ptr, adjustment)
    }

    // Need a destroy funciton since PitchedBuffer needs one
    external fun destroy_jni(ptr: Long)
    fun destroy() {
        this.destroy_jni(this.ptr)
    }
}

