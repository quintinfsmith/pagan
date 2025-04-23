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
        pan_profile: ProfileBuffer? = null
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


    class ProfileBuffer(val ptr: Long) {
        constructor(frames: Array<Pair<Int, Pair<Float, Float>>>, start_frame: Int, skip_initial_set: Boolean = false): this(
            intermediary_create(frames, start_frame, skip_initial_set)
        )

        var current_frame: Int = 0
        var current_index: Int = 0
        var current_value: Float = 0f
        var next_frame_trigger: Int = -1

        companion object {
            fun intermediary_create(frames: Array<Pair<Int, Pair<Float, Float>>>, start_frame: Int, skip_initial_set: Boolean): Long {
                return create(
                    IntArray(frames.size) { i: Int -> frames[i].first },
                    FloatArray(frames.size) { i: Int -> frames[i].second.first },
                    FloatArray(frames.size) {i: Int -> frames[i].second.second },
                    start_frame,
                    skip_initial_set
                )
            }

            external fun create(
                frame_indices: IntArray,
                values: FloatArray,
                increments: FloatArray,
                start_frame: Int,
                skip_initial_set: Boolean
            ): Long
        }

        external fun copy_jni(ptr: Long): Long
        fun copy(): ProfileBuffer {
            return ProfileBuffer(this.copy_jni(this.ptr))
        }

        external fun destroy_jni(ptr: Long)
        fun destroy() {
            this.destroy_jni(this.ptr)
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
            create(sample_rate, delay, attack, hold, decay, release, sustain_attenuation)
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

        external fun get_frames_release(ptr: Long): Int
        external fun set_frames_release(ptr: Long, f: Int)
        var frames_release: Int
            get() = this.get_frames_release(this.ptr)
            set(f: Int) = this.set_frames_release(this.ptr, f)

        external fun get_release(ptr: Long): Float
        external fun set_release(ptr: Long, release: Float)
        var release: Float
            get() = this.get_release(this.ptr)
            set(v: Float) = set_release(ptr, v)

    external fun destroy_jni(ptr: Long)
        fun destroy() {
            this.destroy_jni(this.ptr)
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
        //external fun destroy_jni(ptr: Long)
        //fun destroy() {
        //    this.destroy_jni(this.ptr)
        //}
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

    external fun get_uuid_jni(ptr: Long): Int
    val uuid: Int
        get() = this.get_uuid_jni(this.ptr)

    external fun get_release_frame_jni(ptr: Long): Int
    external fun set_release_frame_jni(ptr: Long, frame: Int)

    var release_frame: Int?
        get() = this.get_release_frame()
        set(f: Int?) = this.set_release_frame_jni(this.ptr, f ?: 0)

    private fun get_release_frame(): Int? {
        val f = this.get_release_frame_jni(this.ptr)
        return if (f == -1) {
            null
        } else {
            f
        }
    }

    external fun get_volume_envelope_ptr(ptr: Long): Long
    val volume_envelope: VolumeEnvelope
        get() = this.get_volume_envelope()
    fun get_volume_envelope(): VolumeEnvelope {
        return VolumeEnvelope(this.get_volume_envelope_ptr(this.ptr))
    }

    external fun get_volume_profile_ptr(ptr: Long): Long
    external fun set_volume_profile_ptr(handle: Long, new_ptr: Long)
    var volume_profile: ProfileBuffer
        get() = this.get_volume_profile()
        set(new_buffer: ProfileBuffer) = this.set_volume_profile_ptr(this.ptr, new_buffer.ptr)
    fun get_volume_profile(): ProfileBuffer {
        return ProfileBuffer(
            this.get_volume_profile_ptr(this.ptr)
        )
    }

    external fun get_pan_profile_ptr(ptr: Long): Long
    external fun set_pan_profile_ptr(handle: Long, new_ptr: Long)
    var pan_profile: ProfileBuffer
        get() = this.get_pan_profile()
        set(new_buffer: ProfileBuffer) = this.set_pan_profile_ptr(this.ptr, new_buffer.ptr)
    fun get_pan_profile(): ProfileBuffer {
        return ProfileBuffer(
            this.get_pan_profile_ptr(this.ptr)
        )
    }

    external fun get_working_frame_jni(ptr: Long): Int
    val working_frame: Int
        get() = this.get_working_frame_jni(this.ptr)

    external fun get_smoothing_factor_jni(ptr: Long): Float
    val smoothing_factor: Float
        get() = this.get_smoothing_factor_jni(this.ptr)

    external fun is_dead_jni(ptr: Long): Boolean
    val is_dead: Boolean
        get() = this.is_dead_jni(this.ptr)

    external fun copy_jni(ptr: Long): Long
    fun copy(): SampleHandle {
        println("COPYING ${this.ptr}")
        return SampleHandle(this.copy_jni(this.ptr))
    }

    external fun set_working_frame_jni(ptr: Long, frame: Int)
    fun set_working_frame(frame: Int) {
        this.set_working_frame_jni(this.ptr, frame)
    }

    external fun get_release_duration_jni(ptr: Long): Int
    fun get_release_duration(): Int {
        return this.get_release_duration_jni(ptr)
    }

    external fun get_next_balance_jni(ptr: Long): Float
    fun get_next_balance(): Pair<Float, Float> {
        val v = this.get_next_balance_jni(this.ptr)
        // TODO
        return Pair(1F, 1F)
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
        this.release_note_jni(this.ptr)
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

