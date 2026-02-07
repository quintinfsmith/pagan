/*
 * Apres, A Midi & Soundfont library
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.apres.soundfontplayer

import android.util.Log
import com.qfs.apres.soundfont2.SampleData
import kotlin.math.abs

class SampleHandle(var ptr: Long) {
    var filter_cutoff: Float? = null
    constructor(
        data: SampleData,
        sample_rate: Int,
        initial_attenuation: Float = 0F,
        loop_points: Pair<Int, Int>?,
        stereo_mode: Int,
        volume_envelope: VolumeEnvelope,
        pitch_shift: Float = 1F,
        filter_cutoff: Float? = null,
        pan: Float = 0F,
        vibrato_frequency: Float = 0f,
        vibrato_delay: Float = 0f,
        vibrato_pitch: Float = 0f,

        // TODO: Modulations
        //modulation_envelope: ModulationEnvelope,
        //modulation_lfo: LFO?,
        //modulators: HashMap<Operation, Set<Modulator>> = hashMapOf()
    ): this(
        SampleHandle.create(
            data.ptr,
            sample_rate,
            initial_attenuation,
            loop_points?.first ?: -1,
            loop_points?.second ?: -1,
            stereo_mode,
            volume_envelope.ptr,
            pitch_shift,
            filter_cutoff ?: 13500F,
            pan,
            vibrato_frequency,
            vibrato_delay,
            vibrato_pitch
            //modulation_envelope,
            //modulation_lfo,
            //modulators
        )
    ) {
        this.filter_cutoff = filter_cutoff
    }

    companion object {
        external fun create(
            data_ptr: Long,
            sample_rate: Int,
            initial_attenuation: Float,
            loop_start: Int,
            loop_end: Int,
            stereo_mode: Int,
            volume_envelope_ptr: Long,
            pitch_shift: Float,
            filter_cutoff: Float,
            pan: Float,
            vibrato_frequency: Float,
            vibrato_delay: Float,
            vibrato_pitch: Float
        ): Long
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
            VolumeEnvelope.create(sample_rate, delay, attack, hold, decay, release, sustain_attenuation)
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
            set(v: Float) = this.set_release(this.ptr, v)

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
        val wave_length = this.sample_rate.toFloat() / this.frequency
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
        set(f) = this.set_release_frame_jni(this.ptr, f ?: -1)

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

    external fun get_working_frame_jni(ptr: Long): Int
    val working_frame: Int
        get() = this.get_working_frame_jni(this.ptr)

    external fun is_dead_jni(ptr: Long): Boolean
    val is_dead: Boolean
        get() = this.is_dead_jni(this.ptr)

    external fun copy_jni(ptr: Long): Long
    fun copy(): SampleHandle {
        val new_handle = SampleHandle(this.copy_jni(this.ptr))
        new_handle.filter_cutoff = this.filter_cutoff
        return new_handle
    }

    external fun set_working_frame_jni(ptr: Long, frame: Int)
    fun set_working_frame(frame: Int) {
        this.set_working_frame_jni(this.ptr, frame)
    }

    external fun get_release_duration_jni(ptr: Long): Int
    fun get_release_duration(): Int {
        return this.get_release_duration_jni(this.ptr)
    }

    external fun get_next_frames_jni(ptr: Long, size: Int, left_padding: Int): FloatArray
    fun get_next_frames(left_padding: Int, size: Int): FloatArray {
        return this.get_next_frames_jni(this.ptr, size, left_padding)
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
        if (this.ptr.toInt() != 0) {
            this.destroy_jni(this.ptr)
        } else {
            Log.e("MEMORY", "Attempting to destroy destroyed SampleHandle")
        }
        this.ptr = 0
    }
}

