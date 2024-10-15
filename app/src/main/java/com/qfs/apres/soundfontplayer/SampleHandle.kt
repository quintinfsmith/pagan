package com.qfs.apres.soundfontplayer

import com.qfs.apres.soundfont.Generator.Operation
import com.qfs.apres.soundfont.Modulator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class SampleHandle(
    var data: ShortArray,
    var sample_rate: Int,
    var initial_attenuation: Float = 0F,
    val loop_points: Pair<Int, Int>?,
    var stereo_mode: Int,

    var volume_envelope: VolumeEnvelope,
    var modulation_envelope: ModulationEnvelope,
    var modulation_lfo: LFO?,

    var pitch_shift: Float = 1F,
    var filter_cutoff: Float = 13500F,
    var pan: Float = 0F,
    var volume: Float = 1F,
    data_buffers: Array<PitchedBuffer>? = null,
    var modulators: HashMap<Operation, Set<Modulator>> = hashMapOf()
    //var note_on_event: MIDIEvent
) {
    var RC = 1f / (this.filter_cutoff * 2f * PI.toFloat())
    val smoothing_factor: Float
    var pitch_adjustment: Float = 1F

    var previous_frame = 0f
    var uuid: Int = SampleHandle.uuid_gen++

    var working_frame: Int = 0
    var release_frame: Int? = null
    var kill_frame: Int? = null
    var is_dead = false
    var _active_buffer = 0
    var _data_buffers: Array<PitchedBuffer> = data_buffers ?: if (this.loop_points != null) {
        arrayOf<PitchedBuffer>(
            PitchedBuffer(
                data = this.data,
                pitch = this.pitch_shift,
                range = 0 until this.loop_points.first,
                is_loop = false
            ),
            PitchedBuffer(
                data = this.data,
                pitch = this.pitch_shift,
                range = this.loop_points.first .. this.loop_points.second,
                is_loop = true
            ),
            PitchedBuffer(
                data = this.data,
                pitch = this.pitch_shift,
                range = this.loop_points.second + 1 until this.data.size,
                is_loop = false
            )
        )
    } else {
        arrayOf(
            PitchedBuffer(
                data = this.data,
                pitch = this.pitch_shift
            )
        )
    }

    init {

        // TODO: Handle non-continuous modulators
        //for ((key, modulator) in this.modulators) {
        //    if (!modulator.source_operator.continuous) {
        //        when (key) {
        //            Operation.FilterCutoff -> {
        //            }
        //            else -> {}
        //        }
        //    }
        //}

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
                data_buffers = Array(original._data_buffers.size) { i: Int ->
                    var buffer = original._data_buffers[i]
                    // constructing this way allows us to skip calculating max
                    val new_buffer = PitchedBuffer(
                        data = buffer.data,
                        pitch = buffer.pitch,
                        known_max = buffer.max,
                        range = buffer._range,
                        is_loop = buffer.is_loop,
                    )
                    new_buffer.position(buffer.position())
                    new_buffer
                },
                modulators = original.modulators,
                //note_on_event = original.note_on_event
            )

            output._active_buffer = original._active_buffer
            output.release_frame = original.release_frame
            output.kill_frame = original.kill_frame
            output.repitch(original.pitch_adjustment)

            return output
        }
    }


    fun max_frame_value(): Int {
        var output = 0
        for (buffer in this._data_buffers) {
            output = max(output, buffer.max)
        }
        return output
    }

    fun set_release_frame(frame: Int) {
        this.release_frame = frame
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

        val loop_points = this.loop_points
        val release_frame = this.release_frame
        this.is_dead = try {
            if (release_frame == null || release_frame > frame) {
                if (loop_points == null || frame < this._data_buffers[0].size) {
                    this._data_buffers[0].position(frame)
                    this._active_buffer = 0
                } else {
                    this._data_buffers[1].position((frame - this._data_buffers[0].size))
                    this._active_buffer = 1
                }
            } else if (loop_points != null && loop_points.first < loop_points.second) {
                if (frame < this._data_buffers[0].size) {
                    this._data_buffers[0].position(frame)
                    this._active_buffer = 0
                } else if (frame < this._data_buffers[1].size) {
                    this._data_buffers[1].position(frame - this._data_buffers[0].size)
                    this._active_buffer = 1
                } else {
                    val remainder = frame - this.release_frame!!
                    val loop_size = (loop_points.second - loop_points.first)
                    if (remainder < loop_size) {
                        this._data_buffers[1].position(remainder)
                        this._active_buffer = 1
                    } else {
                        val loop_count = (this.release_frame!! - loop_points.first) / loop_size
                        this._data_buffers[2].position(frame - this._data_buffers[0].size - loop_count * this._data_buffers[1].size)
                        this._active_buffer = 2
                    }
                }
            } else {
                this._data_buffers[0].position(frame)
                this._active_buffer = 0
            }
            false
        } catch (e: PitchedBuffer.PitchedBufferOverflow) {
            true
        }
    }

    fun get_release_duration(): Int {
        return this.volume_envelope.frames_release
    }

    private fun _get_active_data_buffer(): PitchedBuffer {
        return this._data_buffers[this._active_buffer]
    }

    fun get_next_frame(): Int? {
        if (this.is_dead) {
            return null
        }

        var frame_factor = 1F
        val is_pressed = this.release_frame == null || this.working_frame < this.release_frame!!

        if (this.working_frame < this.volume_envelope.frames_delay) {
            this.working_frame += 1
            this.previous_frame = 0F
            return 0
        } else if (this.working_frame - this.volume_envelope.frames_delay < this.volume_envelope.frames_attack) {
            val r = (this.working_frame - this.volume_envelope.frames_delay).toFloat() / this.volume_envelope.frames_attack.toFloat()
            frame_factor /= 10F.pow(r * this.initial_attenuation)
        } else if (this.working_frame - this.volume_envelope.frames_attack - this.volume_envelope.frames_delay < this.volume_envelope.frames_hold) {
            frame_factor /= 10F.pow(this.initial_attenuation)
        } else if (this.volume_envelope.sustain_attenuation < 1F) {
            frame_factor /= 10F.pow(this.initial_attenuation)
            val relative_frame = this.working_frame - this.volume_envelope.frames_delay - this.volume_envelope.frames_attack - this.volume_envelope.frames_hold
            frame_factor /= if (relative_frame < this.volume_envelope.frames_decay) {
                val r = 1F - (relative_frame.toFloat() / this.volume_envelope.frames_decay.toFloat())
                10F.pow(this.volume_envelope.sustain_attenuation * r)
            } else {
                10F.pow(this.volume_envelope.sustain_attenuation)
            }
        }

        if (this._get_active_data_buffer().is_overflowing()) {
            if (!is_pressed || this.loop_points == null) {
                if (this._active_buffer < this._data_buffers.size - 1) {
                    this._active_buffer += 1
                } else {
                    this.is_dead = true
                    return null
                }
            } else {
               if (this._active_buffer == 0) {
                   this._active_buffer += 1
                   this._get_active_data_buffer().position(0)
               }
            }
        }

        if (!is_pressed) {
            val release_frame_count = min(
                this.volume_envelope.frames_release,
                (Array(this._data_buffers.size) { this._data_buffers[it].size }.sum()) - this.release_frame!!
            )

            val current_position_release = this.working_frame - this.release_frame!!
            if (current_position_release < release_frame_count) {
                frame_factor *= 1F - (current_position_release.toFloat() / release_frame_count.toFloat())
            } else {
                this.is_dead = true
                return null
            }
        }

        // TODO: Needs testing. I think this may end up slowing too much in some cases, but i'll test it better before releasing
        //val modulation_lfo = this.modulation_lfo
        //if (modulation_lfo != null && modulation_lfo.delay <= this.working_frame) {
        //    val lfo_frame = modulation_lfo.get_frame(this.working_frame)
        //    if (lfo_frame != null) {
        //        if (modulation_lfo.volume != 0F) {
        //            frame_factor *= 10F.pow(modulation_lfo.volume * ((lfo_frame + 1F) / 2F))
        //        }

        //        // TODO: This is mostly functional but still getting clicking because of imperfect re-calculation of the loop points
        //        //if (modulation_lfo.pitch != 1F) {
        //        //    val diff = modulation_lfo.pitch - 1F
        //        //    val new_pitch = 1F + ((lfo_frame + 1F) * diff / 2F)
        //        //    this.repitch(new_pitch)
        //        //}
        //    }
        //}

        this.working_frame += 1

        var frame_value = try {
            this._get_active_data_buffer().get().toFloat()
        } catch (e: PitchedBuffer.PitchedBufferOverflow) {
            return null
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
        for (buffer in this._data_buffers) {
            buffer.repitch(adjustment)
        }
    }
}

