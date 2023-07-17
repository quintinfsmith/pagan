package com.qfs.apres.SoundFontPlayer

import com.qfs.apres.InstrumentSample
import com.qfs.apres.NoteOn
import com.qfs.apres.Preset
import com.qfs.apres.PresetInstrument
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow

class SampleHandleGenerator {
    // Hash ignores velocity since velocity isn't baked into sample data
    data class MapKey(
        var note: Int,
        var sample: Int,
        var instrument: Int,
        var preset: Int
    )

    private var sample_data_map = HashMap<MapKey, SampleHandle>()

    fun get(event: NoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): SampleHandle {
        val mapkey = MapKey(event.note, sample.hashCode(), instrument.hashCode(), preset.hashCode())
        if (!sample_data_map.contains(mapkey)) {
            this.sample_data_map[mapkey] = this.generate_new(event, sample, instrument, preset)
        }
        return SampleHandle(this.sample_data_map[mapkey]!!)
    }

    fun cache_new(event: NoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset) {
        val mapkey = MapKey(event.note, sample.hashCode(), instrument.hashCode(), preset.hashCode())
        if (!sample_data_map.contains(mapkey)) {
            this.sample_data_map[mapkey] = this.generate_new(event, sample, instrument, preset)
        }
    }

    private fun generate_new(event: NoteOn, sample: InstrumentSample, instrument: PresetInstrument, preset: Preset): SampleHandle {
        var pitch_shift = 1F
        val original_note = sample.root_key ?: sample.sample!!.originalPitch
        if (original_note != 255) {
            val original_pitch = 2F.pow(original_note.toFloat() / 12F)
            val tuning_cent = (sample.tuning_cent ?: instrument.tuning_cent ?: preset.global_zone?.tuning_cent ?: 0).toFloat()
            val tuning_semi = (sample.tuning_semi ?: instrument.tuning_semi ?: preset.global_zone?.tuning_semi ?: 0).toFloat()
            val requiredPitch = 2F.pow((event.note.toFloat() + (tuning_semi + (tuning_cent / 1200))) / 12F)
            pitch_shift = requiredPitch / original_pitch
        }

        if (sample.sample!!.sampleRate != AudioTrackHandle.sample_rate) {
            pitch_shift *= (sample.sample!!.sampleRate.toFloat() / AudioTrackHandle.sample_rate.toFloat())
        }

        val data = this.resample(sample.sample!!.data!!, pitch_shift)

        var attenuation: Double = preset.global_zone?.attenuation
            ?: instrument.instrument?.global_sample?.attenuation
            ?: instrument.attenuation
            ?: sample.attenuation
            ?: 0.0

        val vol_env_delay: Double = preset.global_zone?.vol_env_delay
            ?: instrument.instrument?.global_sample?.vol_env_delay
            ?: instrument.vol_env_delay
            ?: sample.vol_env_delay
            ?: 0.0
        val vol_env_attack: Double = preset.global_zone?.vol_env_attack
            ?: instrument.instrument?.global_sample?.vol_env_attack
            ?: instrument.vol_env_attack
            ?: sample.vol_env_attack
            ?: 0.0

        val vol_env_hold: Double = preset.global_zone?.vol_env_hold
            ?: instrument.instrument?.global_sample?.vol_env_hold
            ?: instrument.vol_env_hold
            ?: sample.vol_env_hold
            ?: 0.0

        val vol_env_decay: Double = preset.global_zone?.vol_env_decay
            ?: instrument.instrument?.global_sample?.vol_env_decay
            ?: instrument.vol_env_decay
            ?: sample.vol_env_decay
            ?: 0.0

        val vol_env_release: Double = preset.global_zone?.vol_env_release
            ?: instrument.instrument?.global_sample?.vol_env_release
            ?: instrument.vol_env_release
            ?: sample.vol_env_release
            ?: 0.0

        val release_mask_size = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_release)).toInt()

        val max_values = mutableListOf<Short>()
        data.forEachIndexed { i: Int, frame: Short ->
            var d = i / (AudioTrackHandle.buffer_size / 3)
            while (max_values.size <= d) {
                max_values.add(0)
            }

            var abs_frame = abs(frame.toInt())
            if (abs_frame > max_values[d]) {
                max_values[d] = abs_frame.toShort()
            }
        }
        val max_values_floats = Array(max_values.size) {
            max_values[it].toFloat() / Short.MAX_VALUE.toFloat()
        }

        return SampleHandle(
            data = data,
            attenuation = (10.0).pow(attenuation / -20.0).toFloat(),
            stereo_mode = sample.sample!!.sampleType,
            loop_points = if (sample.sampleMode != null && sample.sampleMode!! and 1 == 1) {
                Pair(
                    (sample.sample!!.loopStart.toFloat() / pitch_shift).toInt(),
                    (sample.sample!!.loopEnd.toFloat() / pitch_shift).toInt()
                )
            } else {
                null
            },
            delay_frames = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_delay)).toInt(),
            attack_frame_count = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_attack )).toInt(),
            hold_frame_count = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_hold )).toInt(),
            decay_frame_count = ((AudioTrackHandle.sample_rate.toDouble() * vol_env_decay )).toInt(),
            release_mask = Array(release_mask_size) { i ->
                (release_mask_size - i - 1).toDouble() / release_mask_size.toDouble()
            },
            max_values = max_values_floats
        )
    }

    fun resample(sample_data: ShortArray, pitch_shift: Float): ShortArray {
        // TODO: This is VERY Niave. Look into actual resampling algorithms
        var new_size = (sample_data.size / pitch_shift).toInt()
        return ShortArray(new_size) { i: Int ->
            var i_offset = (i.toFloat() * pitch_shift).toInt()
            sample_data[i_offset]
        }

    }

    fun clear_cache() {
        this.sample_data_map.clear()
    }
}
